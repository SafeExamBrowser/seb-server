/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ConfigurationValueDAO extends EntityDAO<ConfigurationValue, ConfigurationValue> {

    /** NOTE: Deletion is not supported for ConfigurationValue.
     * A ConfigurationValue get automatically deleted on deletion of a Configuration */
    @Override
    default Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        throw new UnsupportedOperationException(
                "Deletion is not supported for ConfigurationValue. A ConfigurationValue get "
                        + "automatically deleted on deletion of a Configuration");
    }

    /** Use this to get all ConfigurationValue for a specific configuration and for a all
     * root attributes that are not child attributes.
     *
     * @param institutionId the institution identifier
     * @param configurationId the configuration identifier
     * @return all ConfigurationValue from given configuration and all root attributes */
    Result<Collection<ConfigurationValue>> allRootAttributeValues(
            Long institutionId,
            Long configurationId);

    /** Use this to get ConfigurationTableValues for a specified table attribute and configuration.
     *
     * @param institutionId the institution identifier
     * @param configurationId the configuration identifier
     * @param attributeId the table attribute to get all values for
     * @return ConfigurationTableValues containing all values of specified table attribute and configuration on the
     *         TableValue format */
    Result<ConfigurationTableValues> getTableValues(
            Long institutionId,
            Long configurationId,
            Long attributeId);

    /** Use this to get an ordered list of all ConfigurationValue of a table attribute on a specified configuration.
     * The list is ordered within the row/list index
     *
     * @param institutionId the institution identifier
     * @param configurationId the configuration identifier
     * @param attributeId the table attribute to get all values for
     * @return an ordered list of all ConfigurationValue of a table attribute on a specified configuration */
    Result<List<List<ConfigurationValue>>> getOrderedTableValues(
            Long institutionId,
            Long configurationId,
            Long attributeId);

    /** Use this to save all values of a table attribute.
     *
     * @param value the ConfigurationTableValues instance containing all actual table attribute and value information
     * @return the saved table values of the attribute and configuration */
    Result<ConfigurationTableValues> saveTableValues(ConfigurationTableValues value);

}
