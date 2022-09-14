/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import ch.ethz.seb.sebserver.gbl.model.Entity;

/** Overall interface for client group data used either for template or real client groups */
public interface ClientGroupData extends Entity {

    public static final String ATTR_IP_RANGE_START = "ipRangeStart";
    public static final String ATTR_IP_RANGE_END = "ipRangeEnd";
    public static final String ATTR_CLIENT_OS = "clientOS";

    /** All known and implemented client group types */
    public enum ClientGroupType {
        NONE,
        IP_V4_RANGE,
        CLIENT_OS
    }

    /** All known and implemented SEB OS types */
    public enum ClientOS {
        NONE(null),
        WINDOWS("Windows"),
        MAC_OS("macOS"),
        I_OS("iOS");

        public final String queryString;

        private ClientOS(final String queryString) {
            this.queryString = queryString;
        }

    }

    Long getId();

    ClientGroupType getType();

    String getColor();

    String getIcon();

    String getIpRangeStart();

    String getIpRangeEnd();

    ClientOS getClientOS();

}
