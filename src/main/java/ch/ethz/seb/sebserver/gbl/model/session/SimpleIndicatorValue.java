/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;

public final class SimpleIndicatorValue implements IndicatorValue {

    public static final String ATTR_INDICATOR_VALUE = "indicatorValue";
    public static final String ATTR_INDICATOR_TYPE = "indicatorType";

    @JsonProperty(ATTR_INDICATOR_TYPE)
    public final IndicatorType type;
    @JsonProperty(ATTR_INDICATOR_VALUE)
    public final double value;

    @JsonCreator
    public SimpleIndicatorValue(
            @JsonProperty(ATTR_INDICATOR_TYPE) final IndicatorType type,
            @JsonProperty(ATTR_INDICATOR_VALUE) final double value) {

        this.type = type;
        this.value = value;
    }

    @Override
    public IndicatorType getType() {
        return this.type;
    }

    @Override
    public double getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("IndicatorValue [type=");
        builder.append(this.type);
        builder.append(", value=");
        builder.append(this.value);
        builder.append("]");
        return builder.toString();
    }
}
