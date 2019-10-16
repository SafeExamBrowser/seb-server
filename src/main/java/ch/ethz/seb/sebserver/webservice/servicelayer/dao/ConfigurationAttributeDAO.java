/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ConfigurationAttributeDAO extends EntityDAO<ConfigurationAttribute, ConfigurationAttribute> {

    /** Use this to get all ConfigurationAttribute that are root attributes and no child
     * attributes (has no parent reference).
     *
     * @return Collection of all ConfigurationAttribute that are root attributes */
    Result<Collection<ConfigurationAttribute>> getAllRootAttributes();

    Result<Collection<ConfigurationAttribute>> allChildAttributes(final Long parentId);

}
