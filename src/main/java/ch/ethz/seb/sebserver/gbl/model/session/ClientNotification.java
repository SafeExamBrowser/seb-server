/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.util.Pair;

public class ClientNotification extends ClientEvent {

    public static enum NotificationType {
        UNKNOWN(0, null),
        LOCK_SCREEN(1, "lockscreen"),
        RAISE_HAND(2, "raisehand");

        public final int id;
        public final String typeName;

        private NotificationType(final int id, final String typeName) {
            this.id = id;
            this.typeName = typeName;
        }

        public static NotificationType byId(final int id) {
            for (final NotificationType type : NotificationType.values()) {
                if (type.id == id) {
                    return type;
                }
            }

            return UNKNOWN;
        }

        public static NotificationType getNotificationType(final String text) {
            if (StringUtils.isBlank(text)) {
                return NotificationType.UNKNOWN;
            }
            return Arrays.asList(NotificationType.values())
                    .stream()
                    .filter(type -> type.typeName != null &&
                            text.startsWith(Constants.ANGLE_BRACE_OPEN + type.typeName + Constants.ANGLE_BRACE_CLOSE))
                    .findFirst()
                    .orElse(NotificationType.UNKNOWN);
        }
    }

    public static final String ATTR_NOTIFICATION_TYPE = "notificationType";

    @JsonProperty(ATTR_NOTIFICATION_TYPE)
    public final NotificationType notificationType;

    public ClientNotification(
            final Long id,
            final Long connectionId,
            final EventType eventType,
            final Long clientTime,
            final Long serverTime,
            final Double numValue,
            final String text) {

        super(id, connectionId, eventType, clientTime, serverTime, numValue, text);

        this.notificationType = NotificationType.getNotificationType(text);
    }

    @JsonCreator
    public ClientNotification(
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_ID) final Long id,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_CLIENT_CONNECTION_ID) final Long connectionId,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_TYPE) final EventType eventType,
            @JsonProperty(ATTR_TIMESTAMP) final Long clientTime,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_SERVER_TIME) final Long serverTime,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_NUMERIC_VALUE) final Double numValue,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_TEXT) final String text,
            @JsonProperty(ATTR_NOTIFICATION_TYPE) final NotificationType notificationType) {

        super(id, connectionId, eventType, clientTime, serverTime, numValue, text);
        this.notificationType = notificationType;
    }

    public NotificationType getNotificationType() {
        return this.notificationType;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CLIENT_NOTIFICATION;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ClientNotification [notificationType=");
        builder.append(this.notificationType);
        builder.append(", id=");
        builder.append(this.id);
        builder.append(", connectionId=");
        builder.append(this.connectionId);
        builder.append(", eventType=");
        builder.append(this.eventType);
        builder.append(", clientTime=");
        builder.append(this.clientTime);
        builder.append(", serverTime=");
        builder.append(this.serverTime);
        builder.append(", numValue=");
        builder.append(this.numValue);
        builder.append(", text=");
        builder.append(this.text);
        builder.append("]");
        return builder.toString();
    }

    public static Pair<NotificationType, String> extractTypeAndPlainText(final String text) {
        final NotificationType type = NotificationType.getNotificationType(text);
        if (type != NotificationType.UNKNOWN) {
            return new Pair<>(type,
                    text.replace(Constants.ANGLE_BRACE_OPEN + type.typeName + Constants.ANGLE_BRACE_CLOSE, ""));
        } else {
            return new Pair<>(type, text);
        }
    }

}
