/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;

public interface IndicatorValue extends IndicatorValueHolder {

    public static final String ATTR_INDICATOR_ID = "id";
    public static final String ATTR_INDICATOR_VALUE = "val";
    public static final String ATTR_INDICATOR_TYPE = "type";

    @JsonProperty(SimpleIndicatorValue.ATTR_INDICATOR_ID)
    Long getIndicatorId();

    /** Use this to get the display value of the value of given IndicatorValue.
     * Since the internal value is a double this gets the correct display value for the IndicatorType
     *
     * @param indicatorValue The indicator value instance
     * @return the display value of the given IndicatorValue */
    static String getDisplayValue(final IndicatorValue indicatorValue, final IndicatorType type) {
        if (Double.isNaN(indicatorValue.getValue())) {
            return Constants.EMPTY_NOTE;
        }
        if (type.integerValue) {
            return String.valueOf((long) indicatorValue.getValue());
        } else {
            return String.valueOf(indicatorValue.getValue());
        }
    }

    default boolean dataEquals(final IndicatorValue other) {
        final Long i1 = getIndicatorId();
        final Long i2 = other.getIndicatorId();
        if (i1 != null && i2 != null) {
            if (i1.longValue() != i2.longValue() || Math.abs(this.getValue() - other.getValue()) > 0.1) {
                return false;
            }
        }
        return true;
    }

}
