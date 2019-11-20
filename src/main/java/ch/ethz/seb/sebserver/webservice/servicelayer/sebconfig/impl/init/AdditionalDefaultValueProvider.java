/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.init;

import java.util.Collection;
import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;

public interface AdditionalDefaultValueProvider {

    Collection<ConfigurationValue> getAdditionalDefaultValues(
            final Long institutionId,
            final Long configurationId,
            final Function<String, ConfigurationAttribute> attributeResolver);

}
