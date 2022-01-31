/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientConnectionData {

    public static final String ATTR_CLIENT_CONNECTION = "cData";
    public static final String ATTR_INDICATOR_VALUE = "iValues";
    public static final String ATTR_MISSING_PING = "missing";
    public static final String ATTR_PENDING_NOTIFICATION = "notification";

    @JsonProperty(ATTR_CLIENT_CONNECTION)
    public final ClientConnection clientConnection;
    @JsonProperty(ATTR_INDICATOR_VALUE)
    public final List<? extends IndicatorValue> indicatorValues;

    public final Boolean missingPing;
    public final Boolean pendingNotification;

    @JsonCreator
    public ClientConnectionData(
            @JsonProperty(ATTR_MISSING_PING) final Boolean missingPing,
            @JsonProperty(ATTR_PENDING_NOTIFICATION) final Boolean pendingNotification,
            @JsonProperty(ATTR_CLIENT_CONNECTION) final ClientConnection clientConnection,
            @JsonProperty(ATTR_INDICATOR_VALUE) final Collection<? extends SimpleIndicatorValue> indicatorValues) {

        this.missingPing = missingPing;
        this.pendingNotification = pendingNotification;
        this.clientConnection = clientConnection;
        this.indicatorValues = Utils.immutableListOf(indicatorValues);
    }

    protected ClientConnectionData(
            final ClientConnection clientConnection,
            final List<? extends IndicatorValue> indicatorValues) {

        this.missingPing = null;
        this.pendingNotification = Boolean.FALSE;
        this.clientConnection = clientConnection;
        this.indicatorValues = Utils.immutableListOf(indicatorValues);
    }

    @JsonProperty(ATTR_MISSING_PING)
    public Boolean getMissingPing() {
        return this.missingPing;
    }

    @JsonProperty(ATTR_PENDING_NOTIFICATION)
    public Boolean pendingNotification() {
        return this.pendingNotification;
    }

    @JsonIgnore
    public Long getConnectionId() {
        return this.clientConnection.id;
    }

    @JsonIgnore
    public boolean hasAnyIncident() {
        return this.missingPing || this.pendingNotification;
    }

    public ClientConnection getClientConnection() {
        return this.clientConnection;
    }

    public Collection<? extends IndicatorValue> getIndicatorValues() {
        return this.indicatorValues;
    }

    public boolean dataEquals(final ClientConnectionData other) {
        if (!this.clientConnection.dataEquals(other.clientConnection)) {
            return false;
        }

        if (this.indicatorValues.size() != other.indicatorValues.size()) {
            return false;
        }

        final Iterator<? extends IndicatorValue> i1 = this.indicatorValues.iterator();
        final Iterator<? extends IndicatorValue> i2 = other.indicatorValues.iterator();
        while (i1.hasNext()) {
            final IndicatorValue iv1 = i1.next();
            final IndicatorValue iv2 = i2.next();
            if (!iv1.dataEquals(iv2)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ClientConnectionData [clientConnection=");
        builder.append(this.clientConnection);
        builder.append(", indicatorValues=");
        builder.append(this.indicatorValues);
        builder.append("]");
        return builder.toString();
    }

}
