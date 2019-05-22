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

    Result<Configuration> undo(Long configurationNodeId);

    /** Restores the current follow-up Configuration to the values of a given Configuration
     * in the history of the specified ConfigurationNode.
     *
     * @param configurationNodeId the ConfigurationNode identifier
     * @param configId the identifier of historical Configuration that defines the restore point and values
     * @return the follow-up Configuration with restored values */
    Result<Configuration> restoreToVersion(Long configurationNodeId, Long configId);

    Result<Configuration> getFollowupConfiguration(String configNodeId);

}
