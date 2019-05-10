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
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ConfigurationValueDAO extends EntityDAO<ConfigurationValue, ConfigurationValue> {

    /** NOTE: Deletion is not supported for ConfigurationValue.
     * A ConfigurationValue get automatically deleted on deletion of a Configuration */
    @Override
    default Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        throw new UnsupportedOperationException(
                "Deletion is not supported for ConfigurationValue. A ConfigurationValue get automatically deleted on deletion of a Configuration");
    }

    Result<ConfigurationTableValues> getTableValues(
            final Long institutionId,
            final Long configurationId,
            final Long attributeId);

    Result<ConfigurationTableValues> saveTableValues(ConfigurationTableValues value);

    Result<Collection<ConfigurationValue>> getTableRowValues(
            final Long institutionId,
            final Long configurationId,
            final Long attributeId,
            final Integer rowIndex);

}
