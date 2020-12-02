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

    public static final String ATTR_INDICATOR_ID = "indicatorId";
    public static final String ATTR_INDICATOR_VALUE = "indicatorValue";
    public static final String ATTR_INDICATOR_TYPE = "indicatorType";

    @JsonProperty(SimpleIndicatorValue.ATTR_INDICATOR_ID)
    Long getIndicatorId();

    /** Use this to get the type of indicator this value was computed from.
     *
     * @return the type of indicator this value was computed from. */
    @JsonProperty(SimpleIndicatorValue.ATTR_INDICATOR_TYPE)
    IndicatorType getType();

    /** Use this to get the display value of the value of given IndicatorValue.
     * Since the internal value is a double this gets the correct display value for the IndicatorType
     *
     * @param indicatorValue The indicator value instance
     * @return the display value of the given IndicatorValue */
    static String getDisplayValue(final IndicatorValue indicatorValue) {
        if (Double.isNaN(indicatorValue.getValue())) {
            return Constants.EMPTY_NOTE;
        }
        if (indicatorValue.getType().integerValue) {
            return String.valueOf((int) indicatorValue.getValue());
        } else {
            return String.valueOf(indicatorValue.getValue());
        }
    }

}
