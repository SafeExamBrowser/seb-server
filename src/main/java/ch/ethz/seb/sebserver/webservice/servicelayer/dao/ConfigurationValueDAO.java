/*
 * Copyright (c) 2019 ETH Zürich, IT Services
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
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBSettingsView;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ConfigurationValueDAO extends EntityDAO<ConfigurationValue, ConfigurationValue> {

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
     * <p> 
     * NOTE: The inner list (List<ConfigurationValue>) that contains the values of a row, can contain null values
     *       when the attribute values for the specific column in the row doesn't exist due to new invented 
     *       attributes for example.
     *
     * @param institutionId the institution identifier
     * @param configurationId the configuration identifier
     * @param attributeId the table attribute to get all values for
     * @param fixMissingValues indicates whether this call should fix missing SEB Settings values or not.
     * @return an ordered list of all ConfigurationValue of a table attribute on a specified configuration */
    Result<List<List<ConfigurationValue>>> getOrderedTableValues(
            Long institutionId,
            Long configurationId,
            Long attributeId,
            boolean fixMissingValues);

    int getTableSize(Long configurationId, Long attributeId);

    /** Use this to save all values of a table attribute.
     *
     * @param value the ConfigurationTableValues instance containing all actual table attribute and value information
     * @return the saved table values of the attribute and configuration */
    Result<ConfigurationTableValues> saveTableValues(ConfigurationTableValues value);

    /** Use this to (re)set the default value(s) for a configuration attributes of a given configuration entry.
     * This uses also the ExamConfigInitService to initialize table values
     *
     * @param institutionId the institution identifier of the configuration
     * @param configurationId the configuration identifier
     * @param attributeId the configuration attribute identifier
     * @return Result refer to a set of all keys of default values or to an error if happened */
    Result<Set<EntityKey>> setDefaultValues(
            Long institutionId,
            Long configurationId,
            Long attributeId);

    /** Use this to get a specific SEB Settings attribute value.
     *
     * @param configId the configuration identifier for the value
     * @param attrId the attribute identifier
     * @return the String value of the SEB setting attribute */
    Result<String> getConfigAttributeValue(Long configId, Long attrId);

    Result<Collection<ConfigurationValue>> getConfigAttributeValues(Long configId, Set<Long> attrIds);

    /** This applies the ignore SEB Service policy as described in Issue SEBWIN-464 on the given configuration
     *
     * @param configurationId The configuration identifier*/
    void applyIgnoreSEBService(Long institutionId, Long configurationId);

    /** Saves the given hashed quit password as value for the given configuration
     *
     * @param configurationId The configuration identifier
     * @param pwd The hashed quit password
     * @return Result refer to void or to an error when happened*/
    Result<Void> saveQuitPassword(Long configurationId, String pwd);

    Result<ConfigurationValue> saveForce(ConfigurationValue configurationValue);

    
}
