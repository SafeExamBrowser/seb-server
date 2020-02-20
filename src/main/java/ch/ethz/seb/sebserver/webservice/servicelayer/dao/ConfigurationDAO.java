/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ConfigurationDAO extends EntityDAO<Configuration, Configuration> {

    /** NOTE: Create is not supported as an API for Configuration.
     * A Configuration get automatically created by the creation of the ConfigurationNode
     * or on save a point in history */
    @Override
    default Result<Configuration> createNew(final Configuration data) {
        throw new UnsupportedOperationException(
                "Create is not supported for Configuration. A Configuration get automatically created by save a point in history");
    }

    /** NOTE: Delete is not supported as an API for Configuration.
     * A Configuration get automatically deleted by the deletion of the ConfigurationNode */
    @Override
    default Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        throw new UnsupportedOperationException(
                "Delete is not supported for Configuration. They get automatically deleted by deleting a configuration node");
    }

    /** Saves the current follow-up Configuration of the ConfigurationNode of given id
     * as a point in history and creates new new follow-up Configuration.
     *
     * @param configurationNodeId the identifier of the ConfigurationNode to create a new history entry form current
     *            follow-up
     * @return the new follow-up Configuration model */
    Result<Configuration> saveToHistory(Long configurationNodeId);

    /** Can be used to reset the current follow-up configuration back to the last saved version in the history
     *
     * @param configurationNodeId ConfigurationNode identifier to apply the undo on
     * @return the current and reset follow-up version */
    Result<Configuration> undo(Long configurationNodeId);

    /** Restores the attribute values to the default values that have been set for the specified configuration
     * on initialization. This are the base default values if the configuration has no template or the default
     * values from the template if there is one assigned to the configuration.
     *
     * In fact. this just gets the initial configuration values and reset the current values with that one
     *
     * @param configurationNodeId the ConfigurationNode identifier
     * @return the Configuration instance for which the attribute values have been reset */
    Result<Configuration> restoreToDefaultValues(final Long configurationNodeId);

    /** Restores the attribute values to the default values that have been set for the specified configuration
     * on initialization. This are the base default values if the configuration has no template or the default
     * values from the template if there is one assigned to the configuration.
     *
     * In fact. this just gets the initial configuration values and reset the current values with that one
     *
     * @param configuration the Configuration that defines the ConfigurationNode identifier
     * @return the Configuration instance for which the attribute values have been reset */
    default Result<Configuration> restoreToDefaultValues(final Configuration configuration) {
        if (configuration == null) {
            return Result.ofError(new NullPointerException("configuration"));
        }
        return restoreToDefaultValues(configuration.configurationNodeId);
    }

    /** Restores the current follow-up Configuration to the values of a given Configuration
     * in the history of the specified ConfigurationNode.
     *
     * @param configurationNodeId the ConfigurationNode identifier
     * @param configId the identifier of historical Configuration that defines the restore point and values
     * @return the follow-up Configuration with restored values */
    Result<Configuration> restoreToVersion(Long configurationNodeId, Long configId);

    /** Use this to get the follow-up configuration for a specified configuration node.
     *
     * @param configNodeId ConfigurationNode identifier to get the current follow-up configuration from
     * @return the current follow-up configuration */
    Result<Configuration> getFollowupConfiguration(Long configNodeId);

    /** Use this to get the follow-up configuration for a specified configuration node.
     *
     * @param configurationNode ConfigurationNode to get the current follow-up configuration from
     * @return the current follow-up configuration */
    default Result<Configuration> getFollowupConfiguration(final ConfigurationNode configurationNode) {
        if (configurationNode == null) {
            return Result.ofError(new NullPointerException("configurationNode"));
        }
        return getFollowupConfiguration(configurationNode.id);
    }

    /** Use this to get the last version of a configuration that is not the follow-up.
     *
     * @param configNodeId ConfigurationNode identifier to get the last version of configuration from
     * @return the last version of configuration */
    Result<Configuration> getConfigurationLastStableVersion(Long configNodeId);

}
