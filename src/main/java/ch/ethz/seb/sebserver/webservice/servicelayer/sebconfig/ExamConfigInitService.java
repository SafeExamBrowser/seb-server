/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.util.Collection;
import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;

/** Service to support and maintain initial default values for SEB exam configurations */
public interface ExamConfigInitService {

    /** Use this to get all additinal initial default values for a newly created SEB exam configuration
     * that are configured with the SEB Server configuration on SEB Server setup.
     *
     * @param institutionId The institution identifier
     * @param configurationId The configuration identifier
     * @param attributeResolver An attribute resolver function that gives an ConfigurationAttribute instance
     *            for the name of an attribute.
     * @return Collection of all ConfigurationValue that must be applied to an newly created SEB exam configuration. */
    Collection<ConfigurationValue> getAdditionalDefaultValues(
            Long institutionId,
            Long configurationId,
            final Function<String, ConfigurationAttribute> attributeResolver);

}
