/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import ch.ethz.seb.sebserver.gbl.model.Entity;

/** Overall interface for client group data used either for template or real client groups */
public interface ClientGroupData extends Entity {

    String ATTR_IP_RANGE_START = "ipRangeStart";
    String ATTR_IP_RANGE_END = "ipRangeEnd";
    String ATTR_CLIENT_OS = "clientOS";
    String ATTR_NAME_RANGE_START_LETTER = "nameRangeStartLetter";
    String ATTR_NAME_RANGE_END_LETTER = "nameRangeEndLetter";

    /** All known and implemented client group types */
    enum ClientGroupType {
        NONE,
        IP_V4_RANGE,
        CLIENT_OS,
        NAME_ALPHABETICAL_RANGE
    }

    /** All known and implemented SEB OS types */
    enum ClientOS {
        NONE(null),
        WINDOWS("Windows"),
        MAC_OS("macOS"),
        I_OS("iOS"),
        IPAD_OS("iPadOS"),
        I_OS_OR_IPAD_OS("iOS", "iPadOS");

        public final String queryString1;
        public final String queryString2;

        ClientOS(final String queryString1) {
            this.queryString1 = queryString1;
            this.queryString2 = null;
        }

        ClientOS(final String queryString1, final String queryString2) {
            this.queryString1 = queryString1;
            this.queryString2 = queryString2;
        }
    }

    Long getId();

    ClientGroupType getType();

    String getColor();

    String getIcon();

    String getIpRangeStart();

    String getIpRangeEnd();
    
    ClientOS getClientOS();

    String getNameRangeStartLetter();

    String getNameRangeEndLetter();
    
}
