/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;

public interface ClientMonitoringDataView {

    public static final String ATTR_STATUS = "st";
    public static final String ATTR_CONNECTION_TOKEN = "tk";
    public static final String ATTR_EXAM_USER_SESSION_ID = "si";
    public static final String ATTR_INFO = "in";
    public static final String ATTR_INDICATOR_VALUES = "iv";
    public static final String ATTR_CLIENT_GROUPS = "cg";
    public static final String ATTR_MISSING_PING = "mp";
    public static final String ATTR_PENDING_NOTIFICATION = "pn";

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_ID)
    Long getId();

    @JsonProperty(ATTR_STATUS)
    ConnectionStatus getStatus();

    @JsonProperty(ATTR_INDICATOR_VALUES)
    Map<Long, String> getIndicatorValues();

    @JsonProperty(ATTR_MISSING_PING)
    boolean isMissingPing();

    @JsonProperty(ATTR_PENDING_NOTIFICATION)
    boolean isPendingNotification();

    public static Predicate<ClientMonitoringDataView> getStatusPredicate(final ConnectionStatus... status) {
        final EnumSet<ConnectionStatus> states = EnumSet.noneOf(ConnectionStatus.class);
        if (status != null) {
            Collections.addAll(states, status);
        }
        return connection -> states.contains(connection.getStatus());
    }

}
