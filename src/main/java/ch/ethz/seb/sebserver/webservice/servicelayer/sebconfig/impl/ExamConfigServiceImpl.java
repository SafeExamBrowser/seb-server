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
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationFormat;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationValueValidator;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionService.Strategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ZipService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.SEBConfigEncryptionServiceImpl.EncryptionContext;

@Lazy
@Service
@WebServiceProfile
public class ExamConfigServiceImpl implements ExamConfigService {

    private static final Logger log = LoggerFactory.getLogger(ExamConfigServiceImpl.class);

    private final ExamConfigIO examConfigIO;
    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final Collection<ConfigurationValueValidator> validators;
    private final ClientCredentialService clientCredentialService;
    private final ZipService zipService;
    private final SEBConfigEncryptionService sebConfigEncryptionService;

    protected ExamConfigServiceImpl(
            final ExamConfigIO examConfigIO,
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final Collection<ConfigurationValueValidator> validators,
            final ClientCredentialService clientCredentialService,
            final ZipService zipService,
            final SEBConfigEncryptionService sebConfigEncryptionService) {

        this.examConfigIO = examConfigIO;
        this.configurationAttributeDAO = configurationAttributeDAO;
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
        return this.examConfigurationMapDAO.getDefaultConfigurationNode(examId);
    }

    public Result<Long> getUserConfigurationIdForExam(final Long examId, final String userId) {
        return this.examConfigurationMapDAO.getUserConfigurationNodeId(examId, userId);
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
                .getConfigPasswordCipher(examId, configurationNodeId)
                .getOr(null);

        if (StringUtils.isNotBlank(passwordCipher)) {

            if (log.isDebugEnabled()) {
                log.debug("*** SEB exam configuration with password based encryption");
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

        if (true) {
            PipedOutputStream pout;
            PipedInputStream pin;
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
    public Result<Collection<String>> generateConfigKeys(final Long institutionId, final Long examId) {
        return this.examConfigurationMapDAO.getConfigurationNodeIds(examId)
                .map(ids -> ids
                        .stream()
                        .map(id -> generateConfigKey(institutionId, id)
                                .getOrThrow())
                        .collect(Collectors.toList()));
    }

    @Override
    public Result<Configuration> importFromSEBFile(
            final Configuration config,
            final InputStream input,
            final CharSequence password) {

        return Result.tryCatch(() -> {

            Future<Exception> streamDecrypted = null;
            InputStream cryptIn = null;
            PipedInputStream plainIn = null;
            PipedOutputStream cryptOut = null;
            InputStream unzippedIn = null;
            try {

                cryptIn = this.examConfigIO.unzip(input);
                plainIn = new PipedInputStream();
                cryptOut = new PipedOutputStream(plainIn);

                // decrypt
                streamDecrypted = this.sebConfigEncryptionService.streamDecrypted(
                        cryptOut,
                        cryptIn,
                        EncryptionContext.contextOf(password));

                // if zipped, unzip attach unzip stream first
                unzippedIn = this.examConfigIO.unzip(plainIn);

                // parse XML and import
                this.examConfigIO.importPlainXML(
                        unzippedIn,
                        config.institutionId,
                        config.id);

                return config;

            } catch (final Exception e) {
                log.error("Unexpected error while trying to import SEB Exam Configuration: ", e);

                if (streamDecrypted != null) {
                    final Exception exception = streamDecrypted.get();
                    if (exception != null && exception instanceof APIMessageException) {
                        throw exception;
                    }
                }

                throw new RuntimeException("Failed to import SEB configuration. Cause is: " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(cryptIn);
                IOUtils.closeQuietly(plainIn);
                IOUtils.closeQuietly(cryptOut);
                IOUtils.closeQuietly(unzippedIn);
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
