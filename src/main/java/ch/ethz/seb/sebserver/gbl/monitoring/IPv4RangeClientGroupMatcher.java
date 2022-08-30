/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroupData.ClientGroupType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@Lazy
@Component
public class IPv4RangeClientGroupMatcher implements ClientGroupConnectionMatcher {

    private static final Logger log = LoggerFactory.getLogger(IPv4RangeClientGroupMatcher.class);

    @Override
    public ClientGroupType matcherType() {
        return ClientGroupType.IP_V4_RANGE;
    }

    @Override
    public boolean isInGroup(final ClientConnection clientConnection, final ClientGroup group) {
        try {

            final long startIPAddress = Utils.ipToLong(group.ipRangeStart);
            final long endIPAddress = Utils.ipToLong(group.ipRangeEnd);
            final long inputIPAddress = Utils.ipToLong(clientConnection.clientAddress);

            return (inputIPAddress >= startIPAddress && inputIPAddress <= endIPAddress);
        } catch (final Exception e) {
            log.error("Failed to verify IP range for group: {} connection: {}", group, clientConnection, e);
            return false;
        }
    }

}
