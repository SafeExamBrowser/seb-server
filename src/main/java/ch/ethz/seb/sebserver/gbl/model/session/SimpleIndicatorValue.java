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

public final class SimpleIndicatorValue implements IndicatorValue {

    @JsonProperty(ATTR_INDICATOR_ID)
    public final Long indicatorId;
    @JsonProperty(ATTR_INDICATOR_VALUE)
    public final double value;

    @JsonCreator
    public SimpleIndicatorValue(
            @JsonProperty(ATTR_INDICATOR_ID) final Long indicatorId,
            @JsonProperty(ATTR_INDICATOR_VALUE) final double value) {

        this.indicatorId = indicatorId;
        this.value = value;
    }

    @Override
    public Long getIndicatorId() {
        return this.indicatorId;
    }

    @Override
    public double getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("SimpleIndicatorValue [indicatorId=");
        builder.append(this.indicatorId);
        builder.append(", value=");
        builder.append(this.value);
        builder.append("]");
        return builder.toString();
    }

}
