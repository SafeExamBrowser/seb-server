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
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;

public class ClientConnectionData {

    @JsonProperty("clientConnection")
    public final ClientConnection clientConnection;
    @JsonProperty("indicatorValues")
    public final List<? extends IndicatorValue> indicatorValues;

    public final Boolean missingPing;

    @JsonCreator
    public ClientConnectionData(
            @JsonProperty("missingPing") final Boolean missingPing,
            @JsonProperty("clientConnection") final ClientConnection clientConnection,
            @JsonProperty("indicatorValues") final Collection<? extends SimpleIndicatorValue> indicatorValues) {

        this.missingPing = missingPing;
        this.clientConnection = clientConnection;
        this.indicatorValues = Utils.immutableListOf(indicatorValues);
    }

    protected ClientConnectionData(
            final ClientConnection clientConnection,
            final List<? extends IndicatorValue> indicatorValues) {

        this.missingPing = null;
        this.clientConnection = clientConnection;
        this.indicatorValues = Utils.immutableListOf(indicatorValues);
    }

    @JsonProperty("missingPing")
    public Boolean getMissingPing() {
        return this.missingPing;
    }

    @JsonIgnore
    public Long getConnectionId() {
        return this.clientConnection.id;
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
            if (iv1.getType() != iv2.getType() || Math.abs(iv1.getValue() - iv2.getValue()) > 0.1) {
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
