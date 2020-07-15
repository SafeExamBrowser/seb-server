/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientEvent implements Entity, IndicatorValueHolder {

    /** Adapt SEB API to SEB_SEB_Server API -> timestamp == clientTime */
    public static final String ATTR_TIMESTAMP = "timestamp";

    public static final String FILTER_ATTR_CONNECTION_ID = Domain.CLIENT_EVENT.ATTR_CLIENT_CONNECTION_ID;
    public static final String FILTER_ATTR_TYPE = Domain.CLIENT_EVENT.ATTR_TYPE;

    public static final String FILTER_ATTR_CLIENT_TIME_FROM = "clientTimeForm";
    public static final String FILTER_ATTR_CLIENT_TIME_TO = "clientTimeTo";
    public static final String FILTER_ATTR_CLIENT_TIME_FROM_TO = "clientTimeFromTo";

    public static final String FILTER_ATTR_SERVER_TIME_FROM = "serverTimeForm";
    public static final String FILTER_ATTR_SERVER_TIME_TO = "serverTimeTo";
    public static final String FILTER_ATTR_SERVER_TIME_FROM_TO = "serverTimeFromTo";

    public static final String FILTER_ATTR_TEXT = Domain.CLIENT_EVENT.ATTR_TEXT;

    public enum EventType {
        UNKNOWN(0),
        DEBUG_LOG(1),
        INFO_LOG(2),
        WARN_LOG(3),
        ERROR_LOG(4),
        LAST_PING(5)

        ;

        public final int id;

        EventType(final int id) {
            this.id = id;
        }

        public static EventType byId(final int id) {
            for (final EventType status : EventType.values()) {
                if (status.id == id) {
                    return status;
                }
            }

            return UNKNOWN;
        }
    }

    @JsonProperty(Domain.CLIENT_EVENT.ATTR_ID)
    public final Long id;

    @JsonProperty(Domain.CLIENT_EVENT.ATTR_CLIENT_CONNECTION_ID)
    public final Long connectionId;

    @JsonProperty(Domain.CLIENT_EVENT.ATTR_TYPE)
    public final EventType eventType;

    @JsonProperty(ATTR_TIMESTAMP)
    public final Long clientTime;

    @JsonProperty(Domain.CLIENT_EVENT.ATTR_SERVER_TIME)
    public final Long serverTime;

    @JsonProperty(Domain.CLIENT_EVENT.ATTR_NUMERIC_VALUE)
    public final Double numValue;

    @JsonProperty(Domain.CLIENT_EVENT.ATTR_TEXT)
    public final String text;

    @JsonCreator
    public ClientEvent(
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_ID) final Long id,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_CLIENT_CONNECTION_ID) final Long connectionId,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_TYPE) final EventType eventType,
            @JsonProperty(ATTR_TIMESTAMP) final Long clientTime,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_SERVER_TIME) final Long serverTime,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_NUMERIC_VALUE) final Double numValue,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_TEXT) final String text) {

        this.id = id;
        this.connectionId = connectionId;
        this.eventType = eventType;
        this.clientTime = (clientTime != null) ? clientTime : 0;
        this.serverTime = (serverTime != null) ? serverTime : Utils.getMillisecondsNow();
        this.numValue = numValue;
        this.text = Utils.truncateText(text, 512);
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CLIENT_EVENT;
    }

    @Override
    public String getName() {
        return getModelId();
    }

    public Long getId() {
        return this.id;
    }

    public Long getConnectionId() {
        return this.connectionId;
    }

    public EventType getEventType() {
        return this.eventType;
    }

    public Long getClientTime() {
        return this.clientTime;
    }

    public Long getServerTime() {
        return this.serverTime;
    }

    public Double getNumValue() {
        return this.numValue;
    }

    @Override
    public double getValue() {
        return this.numValue != null
                ? this.numValue
                : Double.NaN;
    }

    public String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ClientEvent [id=");
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

    public static ClientEventRecord toRecord(
            final ClientEvent event,
            final Long connectionId) {

        return new ClientEventRecord(
                event.id,
                connectionId,
                event.eventType.id,
                event.clientTime,
                event.serverTime,
                (event.numValue != null) ? new BigDecimal(event.numValue) : null,
                event.text);
    }
}
