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

    /** Use this to get the type of indicator this value was computed from.
     *
     * @return the type of indicator this value was computed from. */
    @JsonProperty(SimpleIndicatorValue.ATTR_INDICATOR_TYPE)
    IndicatorType getType();

    /** Use this to get the display value of the value of given IndicatorValue.
     * Since the internal value is a double this gets the correct display value for the InticatorType
     *
     * @param indicatorValue
     * @return the display value of the given IndicatorValue */
    public static String getDisplayValue(final IndicatorValue indicatorValue) {
        if (indicatorValue.getType().integerValue) {
            return String.valueOf((int) indicatorValue.getValue());
        } else {
            return String.valueOf(indicatorValue.getValue());
        }
    }

}
