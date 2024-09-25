/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import java.util.Objects;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.util.Result;
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
                    .flatMap(this.configurationDAO::getConfigurationLastStableVersion)
                    .map(config -> config.id)
                    .getOr(null);

            if (configId == null) {
                return defaultValue;
            }

            return this.configurationValueDAO
                    .getConfigAttributeValue(configId, getAttributeId(configAttributeName))
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
    public String getQuitPassword(final Long examId) {
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
    public String getQuitPasswordFromConfigTemplate(final Long configTemplateId) {
        try {

            final Long configId = this.configurationDAO
                    .getFollowupConfigurationId(configTemplateId)
                    .getOrThrow();

            return this.configurationValueDAO
                    .getConfigAttributeValue(configId, getAttributeId(CONFIG_ATTR_NAME_QUIT_SECRET))
                    .getOrThrow();

        } catch (final Exception e) {
            log.error("Failed to get quit password from configuration template", e);
            return null;
        }
    }

    @Override
    public Result<Long> applyQuitPasswordToConfigs(final Long examId, final String quitSecret) {
        return Result.tryCatch(() -> {

            final String oldQuitPassword = this.getQuitPassword(examId);
            final String newQuitPassword = StringUtils.isNotBlank(quitSecret)
                    ? this.cryptor
                        .decrypt(quitSecret)
                        .getOr(quitSecret)
                        .toString()
                    : StringUtils.EMPTY;

            if (Objects.equals(oldQuitPassword, newQuitPassword)) {
                return examId;
            }

            return saveSEBAttributeValueToConfig(
                    examId, 
                    CONFIG_ATTR_NAME_QUIT_SECRET, 
                    StringUtils.isBlank(quitSecret) ? StringUtils.EMPTY : quitSecret);
        });
    }

    @Override
    public Result<Long> applyQuitURLToConfigs(final Long examId, final String quitLink) {
        return Result.tryCatch(() -> {

            final String oldQuitLink = this.getQuitLink(examId);
            if (Objects.equals(oldQuitLink, quitLink)) {
                return examId;
            }

            return saveSEBAttributeValueToConfig(examId, CONFIG_ATTR_NAME_QUIT_LINK, quitLink);
        });
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

    private Long getAttributeId(final String configAttributeName) {
        return this.configurationAttributeDAO
                .getAttributeIdByName(configAttributeName)
                .onError(error -> log.error("Failed to get attribute id with name: {}",
                        configAttributeName, error))
                .getOr(null);
    }

    private Long saveSEBAttributeValueToConfig(
            final Long examId,
            final String attrName,
            final String attrValue) {

        final Long configNodeId = this.examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .getOr(null);

        if (configNodeId == null) {
            log.info("No Exam Configuration found for exam {} to apply SEB Setting: {}", examId, attrName);
            return examId;
        }

        final Long attrId = getAttributeId(attrName);
        if (attrId == null) {
            return examId;
        }

        final Configuration lastStable = this.configurationDAO
                .getConfigurationLastStableVersion(configNodeId)
                .getOrThrow();
        final Long followupId = configurationDAO
                .getFollowupConfigurationId(configNodeId)
                .getOrThrow();

        // save to last sable version
        this.configurationValueDAO
                .saveForce(new ConfigurationValue(
                        null,
                        lastStable.institutionId,
                        lastStable.id,
                        attrId,
                        0,
                        attrValue
                ))
                .onError(err -> log.error(
                        "Failed to save SEB Setting: {} to config: {}",
                        attrName,
                        lastStable,
                        err));

        if (!Objects.equals(followupId, lastStable.id)) {
            // save also to followup version
            this.configurationValueDAO
                    .saveForce(new ConfigurationValue(
                            null,
                            lastStable.institutionId,
                            followupId,
                            attrId,
                            0,
                            attrValue
                    ))
                    .onError(err -> log.error(
                            "Failed to save SEB Setting: {} to config: {}",
                            attrName,
                            lastStable,
                            err));
        }

        return examId;
    }

}
