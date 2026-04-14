/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCreationInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationNodeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;

public interface ConfigurationNodeDAO extends
        EntityDAO<ConfigurationNode, ConfigurationNode>,
        BulkActionSupportDAO<ConfigurationNode> {

    String TEMPORARY_TEMPLATE_PREFIX = "[TEMPORARY_TEMPLATE]";

    /** Use this to create a copy from an existing configuration.
     *
     * @param institutionId the institution identifier of the existing configuration
     * @param newOwner the owner of the created copy
     * @param copyInfo the ConfigCreationInfo containing additional copy information
     * @return Result refer to the configuration copy root node or to an error if happened */
    Result<ConfigurationNode> createCopy(
            Long institutionId,
            String newOwner,
            ConfigCreationInfo copyInfo);

    /** Used to update the name and description of a Configuration Template that is used by an Exam Template
     *
     * @param configTemplateId The Configuration Template identifier
     * @param name the name to update
     *
     * @param description the description to update
     * @return Result refer to the updated ConfigurationNode or to an error when happened */
    Result<ConfigurationNode> updateConfigurationTemplate(
            Long configTemplateId,
            String name,
            String description);

    /** Get all ConfigurationNodeRecords that has a TEMPORARY_TEMPLATE_PREFIX prefix
     *
     * @return Result refer to th e list of ConfigurationNodeRecords or to an error when happened */
    Result<Collection<ConfigurationNodeRecord>> getAllTemporary();
}
