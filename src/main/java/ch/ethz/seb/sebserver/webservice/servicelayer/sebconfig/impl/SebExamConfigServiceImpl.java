/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentialService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationFormat;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationValueValidator;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService.Strategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebExamConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ZipService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.SebConfigEncryptionServiceImpl.EncryptionContext;

@Lazy
@Service
@WebServiceProfile
public class SebExamConfigServiceImpl implements SebExamConfigService {

    private static final Logger log = LoggerFactory.getLogger(SebExamConfigServiceImpl.class);

    private final ExamConfigIO examConfigIO;
    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ConfigurationDAO configurationDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final Collection<ConfigurationValueValidator> validators;
    private final ClientCredentialService clientCredentialService;
    private final ZipService zipService;
    private final SebConfigEncryptionService sebConfigEncryptionService;

    protected SebExamConfigServiceImpl(
            final ExamConfigIO examConfigIO,
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ConfigurationDAO configurationDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final Collection<ConfigurationValueValidator> validators,
            final ClientCredentialService clientCredentialService,
            final ZipService zipService,
            final SebConfigEncryptionService sebConfigEncryptionService) {

        this.examConfigIO = examConfigIO;
        this.configurationAttributeDAO = configurationAttributeDAO;
        this.configurationDAO = configurationDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.validators = validators;
        this.clientCredentialService = clientCredentialService;
        this.zipService = zipService;
        this.sebConfigEncryptionService = sebConfigEncryptionService;
    }

    @Override
    public void validate(final ConfigurationValue value) throws FieldValidationException {
        if (value == null) {
            log.warn("Validate called with null reference. Ignore this and skip validation");
            return;
        }

        final ConfigurationAttribute attribute = this.configurationAttributeDAO
                .byPK(value.attributeId)
                .getOrThrow();

        this.validators
                .stream()
                .filter(validator -> !validator.validate(value, attribute))
                .findFirst()
                .ifPresent(validator -> validator.throwValidationError(value, attribute));
    }

    @Override
    public void validate(final ConfigurationTableValues tableValue) throws FieldValidationException {
        final List<APIMessage> errors = tableValue.values.stream()
                .map(tv -> new ConfigurationValue(
                        null,
                        tableValue.institutionId,
                        tableValue.configurationId,
                        tv.attributeId,
                        tv.listIndex,
                        tv.value))
                .flatMap(cv -> {
                    try {
                        validate(cv);
                        return Stream.empty();
                    } catch (final FieldValidationException fve) {
                        return Stream.of(fve);
                    }
                })
                .map(fve -> fve.apiMessage)
                .collect(Collectors.toList());

        if (!errors.isEmpty()) {
            throw new APIMessageException(errors);
        }
    }

    @Override
    public void exportPlainXML(
            final OutputStream out,
            final Long institutionId,
            final Long configurationNodeId) {

        this.exportPlainOnly(ConfigurationFormat.XML, out, institutionId, configurationNodeId);
    }

    @Override
    public void exportPlainJSON(
            final OutputStream out,
            final Long institutionId,
            final Long configurationNodeId) {

        this.exportPlainOnly(ConfigurationFormat.JSON, out, institutionId, configurationNodeId);
    }

    public Result<Long> getDefaultConfigurationIdForExam(final Long examId) {
        return this.examConfigurationMapDAO.getDefaultConfigurationForExam(examId);
    }

    public Result<Long> getUserConfigurationIdForExam(final Long examId, final String userId) {
        return this.examConfigurationMapDAO.getUserConfigurationIdForExam(examId, userId);
    }

    @Override
    public Long exportForExam(
            final OutputStream out,
            final Long institutionId,
            final Long examId,
            final String userId) {

        final Long configurationNodeId = (StringUtils.isBlank(userId))
                ? getDefaultConfigurationIdForExam(examId)
                        .getOrThrow()
                : getUserConfigurationIdForExam(examId, userId)
                        .getOrThrow();

        return exportForExam(out, institutionId, examId, configurationNodeId);
    }

    @Override
    public Long exportForExam(
            final OutputStream out,
            final Long institutionId,
            final Long examId,
            final Long configurationNodeId) {

        final CharSequence passwordCipher = this.examConfigurationMapDAO
                .getConfigPasswortCipher(examId, configurationNodeId)
                .getOr(null);

        if (StringUtils.isNotBlank(passwordCipher)) {

            if (log.isDebugEnabled()) {
                log.debug("*** Seb exam configuration with password based encryption");
            }

            final CharSequence encryptionPasswordPlaintext = this.clientCredentialService
                    .decrypt(passwordCipher);

            PipedOutputStream plainOut = null;
            PipedInputStream zipIn = null;

            PipedOutputStream zipOut = null;
            PipedInputStream cryptIn = null;

            PipedOutputStream cryptOut = null;
            PipedInputStream in = null;

            try {

                plainOut = new PipedOutputStream();
                zipIn = new PipedInputStream(plainOut);

                zipOut = new PipedOutputStream();
                cryptIn = new PipedInputStream(zipOut);

                cryptOut = new PipedOutputStream();
                in = new PipedInputStream(cryptOut);

                // streaming...
                // export plain text
                this.examConfigIO.exportPlain(
                        ConfigurationFormat.XML,
                        plainOut,
                        institutionId,
                        configurationNodeId);
                // zip the plain text
                this.zipService.write(zipOut, zipIn);
                // encrypt the zipped plain text
                this.sebConfigEncryptionService.streamEncrypted(
                        cryptOut,
                        cryptIn,
                        EncryptionContext.contextOf(
                                Strategy.PASSWORD_PSWD,
                                encryptionPasswordPlaintext));

                // copy to output
                IOUtils.copyLarge(in, out);

            } catch (final Exception e) {
                log.error("Error while zip and encrypt seb exam config stream: ", e);
            } finally {
                IOUtils.closeQuietly(zipIn);
                IOUtils.closeQuietly(plainOut);
                IOUtils.closeQuietly(cryptIn);
                IOUtils.closeQuietly(zipOut);
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(cryptOut);
            }
        } else {
            // just export in plain text XML format
            this.exportPlainXML(out, institutionId, configurationNodeId);
        }

        return configurationNodeId;
    }

    @Override
    public Result<String> generateConfigKey(
            final Long institutionId,
            final Long configurationNodeId) {

        if (log.isDebugEnabled()) {
            log.debug("Start to stream plain JSON SEB Configuration data for Config-Key generation");
        }

        if (log.isTraceEnabled()) {
            PipedOutputStream pout = null;
            PipedInputStream pin = null;
            try {
                pout = new PipedOutputStream();
                pin = new PipedInputStream(pout);
                this.examConfigIO.exportPlain(
                        ConfigurationFormat.JSON,
                        pout,
                        institutionId,
                        configurationNodeId);

                final String json = IOUtils.toString(pin, "UTF-8");

                log.trace("SEB Configuration JSON to create Config-Key: {}", json);
            } catch (final Exception e) {
                log.error("Failed to trace SEB Configuration JSON: ", e);
            }
        }

        PipedOutputStream pout = null;
        PipedInputStream pin = null;
        try {
            pout = new PipedOutputStream();
            pin = new PipedInputStream(pout);

            this.examConfigIO.exportPlain(
                    ConfigurationFormat.JSON,
                    pout,
                    institutionId,
                    configurationNodeId);

            final String configKey = DigestUtils.sha256Hex(pin);

            return Result.of(configKey);

        } catch (final Exception e) {
            log.error("Error while stream plain JSON SEB Configuration data for Config-Key generation: ", e);
            return Result.ofError(e);
        } finally {
            try {
                if (pin != null) {
                    pin.close();
                }
            } catch (final IOException e1) {
                log.error("Failed to close PipedInputStream: ", e1);
            }
            try {
                if (pout != null) {
                    pout.close();
                }
            } catch (final IOException e1) {
                log.error("Failed to close PipedOutputStream: ", e1);
            }

            if (log.isDebugEnabled()) {
                log.debug("Finished to stream plain JSON SEB Configuration data for Config-Key generation");
            }
        }
    }

    @Override
    public Result<Configuration> importFromSEBFile(
            final Long configNodeId,
            final InputStream input,
            final CharSequence password) {

        return Result.tryCatch(() -> {

            final Configuration newConfig = this.configurationDAO
                    .saveToHistory(configNodeId)
                    .getOrThrow();

            Future<Exception> streamDecrypted = null;
            try {

                final InputStream cryptIn = this.examConfigIO.unzip(input);
                final PipedInputStream plainIn = new PipedInputStream();
                final PipedOutputStream cryptOut = new PipedOutputStream(plainIn);

                // decrypt
                streamDecrypted = this.sebConfigEncryptionService.streamDecrypted(
                        cryptOut,
                        cryptIn,
                        EncryptionContext.contextOf(password));

                // if zipped, unzip attach unzip stream first
                final InputStream _plainIn = this.examConfigIO.unzip(plainIn);

                // parse XML and import
                this.examConfigIO.importPlainXML(
                        _plainIn,
                        newConfig.institutionId,
                        newConfig.id);

                return newConfig;

            } catch (final Exception e) {
                log.error("Unexpected error while trying to import SEB Exam Configuration: ", e);
                log.debug("Make an undo on the ConfigurationNode to rollback the changes");
                this.configurationDAO
                        .undo(configNodeId)
                        .getOrThrow();

                if (streamDecrypted != null) {
                    final Exception exception = streamDecrypted.get();
                    if (exception != null) {
                        throw exception;
                    }
                }

                throw new RuntimeException("Failed to import SEB configuration. Cause is: " + e.getMessage());
            }
        });
    }

    private void exportPlainOnly(
            final ConfigurationFormat exportFormat,
            final OutputStream out,
            final Long institutionId,
            final Long configurationNodeId) {

        if (log.isDebugEnabled()) {
            log.debug("Start to stream plain text SEB Configuration data");
        }

        PipedOutputStream pout = null;
        PipedInputStream pin = null;
        try {
            pout = new PipedOutputStream();
            pin = new PipedInputStream(pout);

            this.examConfigIO.exportPlain(
                    exportFormat,
                    pout,
                    institutionId,
                    configurationNodeId);

            IOUtils.copyLarge(pin, out);

        } catch (final Exception e) {
            log.error("Error while stream plain text SEB Configuration export data: ", e);
        } finally {
            try {
                if (pin != null) {
                    pin.close();
                }
            } catch (final IOException e1) {
                log.error("Failed to close PipedInputStream: ", e1);
            }
            try {
                if (pout != null) {
                    pout.flush();
                    pout.close();
                }
            } catch (final IOException e1) {
                log.error("Failed to close PipedOutputStream: ", e1);
            }

            if (log.isDebugEnabled()) {
                log.debug("Finished to stream plain text SEB Configuration export data");
            }
        }
    }

}
