/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.monitoring;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroupData.ClientGroupType;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroupData.ClientOS;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;

@Lazy
@Component
public class ClientOSGroupMatcher implements ClientGroupConnectionMatcher {

    @Override
    public ClientGroupType matcherType() {
        return ClientGroupType.CLIENT_OS;
    }

    @Override
    public boolean isInGroup(final ClientConnection clientConnection, final ClientGroup group) {
        if (group == null
                || group.type != ClientGroupType.CLIENT_OS
                || clientConnection == null
                || clientConnection.info == null) {

            return false;
        }

        try {
            final ClientOS osType = ClientOS.valueOf(group.getData());
            return clientConnection.info.contains(osType.queryString1) ||
                    (osType.queryString2 != null && clientConnection.info.contains(osType.queryString2));
        } catch (final Exception e) {
            return false;
        }
    }
}
