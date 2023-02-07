/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamConfigurationValueService;

@Lazy
@Service
@WebServiceProfile
public class ExamConfigurationValueServiceImpl implements ExamConfigurationValueService {

    private static final Logger log = LoggerFactory.getLogger(ExamConfigurationValueServiceImpl.class);

    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final ConfigurationDAO configurationDAO;
    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ConfigurationValueDAO configurationValueDAO;
    private final Cryptor cryptor;

    public ExamConfigurationValueServiceImpl(
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final ConfigurationDAO configurationDAO,
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ConfigurationValueDAO configurationValueDAO,
            final Cryptor cryptor) {

        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.configurationDAO = configurationDAO;
        this.configurationAttributeDAO = configurationAttributeDAO;
        this.configurationValueDAO = configurationValueDAO;
        this.cryptor = cryptor;
    }

    @Override
    public String getMappedDefaultConfigAttributeValue(
            final Long examId,
            final String configAttributeName,
            final String defaultValue) {

        try {

            final Long configId = this.examConfigurationMapDAO
                    .getDefaultConfigurationNode(examId)
                    .flatMap(nodeId -> this.configurationDAO.getConfigurationLastStableVersion(nodeId))
                    .map(config -> config.id)
                    .onError(error -> log.warn("Failed to get default Exam Config for exam: {} cause: {}",
                            examId, error.getMessage()))
                    .getOr(null);

            if (configId == null) {
                return defaultValue;
            }

            final Long attrId = this.configurationAttributeDAO
                    .getAttributeIdByName(configAttributeName)
                    .onError(error -> log.error("Failed to get attribute id with name: {} for exam: {}",
                            configAttributeName, examId, error))
                    .getOr(null);

            return this.configurationValueDAO
                    .getConfigAttributeValue(configId, attrId)
                    .onError(error -> log.warn(
                            "Failed to get exam config attribute: {} {} error: {}",
                            examId,
                            configAttributeName,
                            error.getMessage()))
                    .getOr(defaultValue);

        } catch (final Exception e) {
            if (defaultValue == null) {
                log.error("Unexpected error while trying to extract SEB settings attribute value:", e);
            }
            return defaultValue;
        }
    }

    @Override
    public String getQuitSecret(final Long examId) {
        try {

            final String quitSecretEncrypted = getMappedDefaultConfigAttributeValue(
                    examId,
                    CONFIG_ATTR_NAME_QUIT_SECRET);

            if (StringUtils.isNotEmpty(quitSecretEncrypted)) {
                try {

                    return this.cryptor
                            .decrypt(quitSecretEncrypted)
                            .getOrThrow()
                            .toString();

                } catch (final Exception e) {
                    log.error("Failed to decrypt quitSecret: ", e);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to get SEB restriction with quit secret: ", e);
        }

        return StringUtils.EMPTY;
    }

    @Override
    public String getQuitLink(final Long examId) {
        try {

            final String quitLink = getMappedDefaultConfigAttributeValue(
                    examId,
                    CONFIG_ATTR_NAME_QUIT_LINK);

            return (quitLink != null) ? quitLink : StringUtils.EMPTY;

        } catch (final Exception e) {
            log.error("Failed to get SEB restriction with quit link: ", e);
            return StringUtils.EMPTY;
        }
    }

    @Override
    public String getAllowedSEBVersion(final Long examId) {
        try {

            return getMappedDefaultConfigAttributeValue(
                    examId,
                    CONFIG_ATTR_NAME_ALLOWED_SEB_VERSION,
                    StringUtils.EMPTY);

        } catch (final Exception e) {
            return null;
        }
    }

}
