/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationValueValidator;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebExamConfigService;

@Lazy
@Service
@WebServiceProfile
public class SebExamConfigServiceImpl implements SebExamConfigService {

    private static final Logger log = LoggerFactory.getLogger(SebExamConfigServiceImpl.class);

    private final ExamConfigIO examConfigIO;
    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final Collection<ConfigurationValueValidator> validators;

    protected SebExamConfigServiceImpl(
            final ExamConfigIO examConfigIO,
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final Collection<ConfigurationValueValidator> validators) {

        this.examConfigIO = examConfigIO;
        this.configurationAttributeDAO = configurationAttributeDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.validators = validators;

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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void exportPlainXML(
            final OutputStream out,
            final Long institutionId,
            final Long configurationNodeId) {

        if (log.isDebugEnabled()) {
            log.debug("Start to stream plain text SEB clonfiguration data");
        }

        PipedOutputStream pout = null;
        PipedInputStream pin = null;
        try {
            pout = new PipedOutputStream();
            pin = new PipedInputStream(pout);

            this.examConfigIO.exportPlainXML(pout, institutionId, configurationNodeId);

            IOUtils.copyLarge(pin, out);

            pin.close();
            pout.flush();
            pout.close();

        } catch (final IOException e) {
            log.error("Error while stream plain text SEB clonfiguration data: ", e);
        } finally {
            try {
                if (pin != null)
                    pin.close();
            } catch (final IOException e1) {
                log.error("Failed to close PipedInputStream: ", e1);
            }
            try {
                if (pout != null)
                    pout.close();
            } catch (final IOException e1) {
                log.error("Failed to close PipedOutputStream: ", e1);
            }

            if (log.isDebugEnabled()) {
                log.debug("Finished to stream plain text SEB clonfiguration data");
            }
        }

    }

    @Override
    public Result<Long> getDefaultConfigurationIdForExam(final Long examId) {
        return this.examConfigurationMapDAO.getDefaultConfigurationForExam(examId);
    }

    @Override
    public Result<Long> getUserConfigurationIdForExam(final Long examId, final String userId) {
        return this.examConfigurationMapDAO.getUserConfigurationIdForExam(examId, userId);
    }

    @Override
    public void exportForExam(final OutputStream out, final Long configExamMappingId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void exportDefaultForExam(final OutputStream out, final Long examId) {
        // TODO Auto-generated method stub

    }

    @Override
    public String generateConfigKey(final Long configurationNodeId) {
        // TODO https://www.safeexambrowser.org/developer/seb-config-key.html
        throw new UnsupportedOperationException("TODO");
    }

}
