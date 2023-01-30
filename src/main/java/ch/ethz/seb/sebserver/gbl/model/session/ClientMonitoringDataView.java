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
    public static final String ATTR_NOTIFICATION_FLAG = "nf";

    public static final int FLAG_MISSING_PING = 1;
    public static final int FLAG_PENDING_NOTIFICATION = 2;
    public static final int FLAG_GRANT_NOT_CHECKED = 4;
    public static final int FLAG_GRANT_DENIED = 8;
    public static final int FLAG_INVALID_SEB_VERSION = 16;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_ID)
    Long getId();

    @JsonProperty(ATTR_STATUS)
    ConnectionStatus getStatus();

    @JsonProperty(ATTR_INDICATOR_VALUES)
    Map<Long, String> getIndicatorValues();

    @JsonProperty(ATTR_NOTIFICATION_FLAG)
    Integer notificationFlag();

    default boolean isMissingPing() {
        final Integer notificationFlag = notificationFlag();
        return notificationFlag != null && (notificationFlag & FLAG_MISSING_PING) > 0;
    }

    default boolean isGrantChecked() {
        final Integer notificationFlag = notificationFlag();
        return notificationFlag != null && (notificationFlag & FLAG_GRANT_NOT_CHECKED) == 0;
    }

    default boolean isGrantDenied() {
        final Integer notificationFlag = notificationFlag();
        return notificationFlag != null && (notificationFlag & FLAG_GRANT_DENIED) > 0;
    }

    default boolean isPendingNotification() {
        final Integer notificationFlag = notificationFlag();
        return notificationFlag != null && (notificationFlag & FLAG_PENDING_NOTIFICATION) > 0;
    }

    public static Predicate<ClientMonitoringDataView> getStatusPredicate(final ConnectionStatus... status) {
        final EnumSet<ConnectionStatus> states = EnumSet.noneOf(ConnectionStatus.class);
        if (status != null) {
            Collections.addAll(states, status);
        }
        return connection -> states.contains(connection.getStatus());
    }

}
