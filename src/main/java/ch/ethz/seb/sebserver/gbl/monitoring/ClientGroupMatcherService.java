/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.monitoring;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroupData.ClientGroupType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;

@Lazy
@Service
public class ClientGroupMatcherService {

    private final Map<ClientGroupType, ClientGroupConnectionMatcher> matcher;

    public ClientGroupMatcherService(final Collection<ClientGroupConnectionMatcher> matcher) {
        this.matcher = matcher
                .stream()
                .collect(Collectors.toMap(
                        k -> k.matcherType(),
                        Function.identity()));
    }

    public boolean isInGroup(final ClientConnection clientConnection, final ClientGroup group) {
        if (!this.matcher.containsKey(group.type)) {
            return false;
        }

        return this.matcher.get(group.type).isInGroup(clientConnection, group);
    }

}
