/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

@FunctionalInterface
public interface IndicatorValueHolder {

    @JsonProperty(SimpleIndicatorValue.ATTR_INDICATOR_VALUE)
    double getValue();

}
