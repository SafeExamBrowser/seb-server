/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;


public enum ClientIndicatorType {
    UNKNOWN(0),
    LAST_PING(1),
    ERROR_LOG_COUNT(2),
    WARN_LOG_COUNT(3),
    INFO_LOG_COUNT(4),
    WLAN_STATUS(5),
    BATTERY_STATUS(5),
    

    ;

    public final int id;

    ClientIndicatorType(final int id) {
        this.id = id;
    }

    public static ClientIndicatorType byId(final int id) {
        for (final ClientIndicatorType status : ClientIndicatorType.values()) {
            if (status.id == id) {
                return status;
            }
        }

        return UNKNOWN;
    }
}
