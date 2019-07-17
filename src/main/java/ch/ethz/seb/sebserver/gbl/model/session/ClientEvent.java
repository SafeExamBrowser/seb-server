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
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ClientEvent implements Entity, IndicatorValueHolder {

    public static final String FILTER_ATTR_CONECTION_ID = Domain.CLIENT_EVENT.ATTR_CONNECTION_ID;
    public static final String FILTER_ATTR_TYPE = Domain.CLIENT_EVENT.ATTR_TYPE;
    public static final String FILTER_ATTR_FROM_DATE = "fromDate";

    public static enum EventType {
        UNKNOWN(0),
        DEBUG_LOG(1),
        INFO_LOG(2),
        WARN_LOG(3),
        ERROR_LOG(4),
        LAST_PING(5)

        ;

        public final int id;

        private EventType(final int id) {
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

    @JsonProperty(Domain.CLIENT_EVENT.ATTR_CONNECTION_ID)
    public final Long connectionId;

    @JsonProperty(Domain.CLIENT_EVENT.ATTR_TYPE)
    public final EventType eventType;

    @JsonProperty(Domain.CLIENT_EVENT.ATTR_TIMESTAMP)
    public final Long timestamp;

    @JsonProperty(Domain.CLIENT_EVENT.ATTR_NUMERIC_VALUE)
    public final Double numValue;

    @JsonProperty(Domain.CLIENT_EVENT.ATTR_TEXT)
    public final String text;

    @JsonCreator
    public ClientEvent(
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_ID) final Long id,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_CONNECTION_ID) final Long connectionId,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_TYPE) final EventType eventType,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_TIMESTAMP) final Long timestamp,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_NUMERIC_VALUE) final Double numValue,
            @JsonProperty(Domain.CLIENT_EVENT.ATTR_TEXT) final String text) {

        this.id = id;
        this.connectionId = connectionId;
        this.eventType = eventType;
        this.timestamp = (timestamp != null) ? timestamp : System.currentTimeMillis();
        this.numValue = numValue;
        this.text = text;
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

    public Long getTimestamp() {
        return this.timestamp;
    }

    public Double getNumValue() {
        return this.numValue;
    }

    @Override
    public double getValue() {
        return this.numValue != null
                ? this.numValue.doubleValue()
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
        builder.append(", timestamp=");
        builder.append(this.timestamp);
        builder.append(", numValue=");
        builder.append(this.numValue);
        builder.append(", text=");
        builder.append(this.text);
        builder.append("]");
        return builder.toString();
    }

    public static final ClientEventRecord toRecord(
            final ClientEvent event,
            final Long connectionId) {

        return new ClientEventRecord(
                event.id,
                connectionId,
                event.eventType.id,
                event.timestamp,
                (event.numValue != null) ? new BigDecimal(event.numValue) : null,
                event.text);
    }
}
