/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamConfigurationValueService;

@Service
public class ExamConfigurationValueServiceImpl implements ExamConfigurationValueService {

    private static final Logger log = LoggerFactory.getLogger(ExamConfigurationValueServiceImpl.class);

    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final ConfigurationDAO configurationDAO;
    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ConfigurationValueDAO configurationValueDAO;

    public ExamConfigurationValueServiceImpl(
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final ConfigurationDAO configurationDAO,
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ConfigurationValueDAO configurationValueDAO) {

        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.configurationDAO = configurationDAO;
        this.configurationAttributeDAO = configurationAttributeDAO;
        this.configurationValueDAO = configurationValueDAO;
    }

    @Override
    public String getMappedDefaultConfigAttributeValue(final Long examId, final String configAttributeName) {
        try {

            final Long configId = this.examConfigurationMapDAO
                    .getDefaultConfigurationNode(examId)
                    .flatMap(nodeId -> this.configurationDAO.getConfigurationLastStableVersion(nodeId))
                    .map(config -> config.id)
                    .getOrThrow();

            final Long attrId = this.configurationAttributeDAO
                    .getAttributeIdByName(configAttributeName)
                    .onError(error -> log.error("Failed to get attribute id with name: {} for exam: {}",
                            configAttributeName, examId, error))
                    .getOr(null);

            return this.configurationValueDAO
                    .getConfigAttributeValue(configId, attrId)
                    .getOrThrow();

        } catch (final Exception e) {
            log.error("Unexpected error while trying to extract SEB settings attribute value:", e);
            return null;
        }
    }

}
