/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;

public interface IndicatorValue extends IndicatorValueHolder {

    @JsonProperty(SimpleIndicatorValue.ATTR_INDICATOR_TYPE)
    IndicatorType getType();

}
