/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.monitoring;

import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroupData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class AlphabeticalNameRangeMatcher implements ClientGroupConnectionMatcher {
    @Override
    public ClientGroupData.ClientGroupType matcherType() {
        return ClientGroupData.ClientGroupType.NAME_ALPHABETICAL_RANGE;
    }

    @Override
    public boolean isInGroup(final ClientConnection clientConnection, final ClientGroup group) {
        if (StringUtils.isBlank(clientConnection.userSessionId)) {
            return false;
        }

        final String start = group.nameRangeStartLetter != null ? group.nameRangeStartLetter : "A";
        final String end = group.nameRangeStartLetter != null ? group.nameRangeStartLetter : "Z";
        
        return clientConnection.userSessionId.compareToIgnoreCase(start) >= 0 &&
                clientConnection.userSessionId.compareToIgnoreCase(end) <= 0;
    }
}
