/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface OrientationDAO extends EntityDAO<Orientation, Orientation> {

    /** Use this to delete all Orientation of a defined template.
     *
     * @param templateId the template identifier (PK)
     * @return Collection of all EntityKey of Orientations that has been deleted */
    Result<Collection<EntityKey>> deleteAllOfTemplate(Long templateId);

    Result<ConfigurationNode> copyDefaultOrientationsForTemplate(
            ConfigurationNode node,
            Map<Long, Long> viewMapping);

    Result<Collection<Orientation>> getAllOfTemplate(Long templateId);

    Result<Orientation> getAttributeOfTemplate(Long templateId, Long attributeId);

}
