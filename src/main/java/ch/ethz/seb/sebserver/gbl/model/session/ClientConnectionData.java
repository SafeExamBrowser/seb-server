/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientConnectionData {

    @JsonProperty
    public final ClientConnection clientConnection;
    @JsonProperty
    public final Collection<? extends IndicatorValue> indicatorValues;

    @JsonCreator
    protected ClientConnectionData(
            @JsonProperty final ClientConnection clientConnection,
            @JsonProperty final Collection<? extends IndicatorValue> indicatorValues) {

        this.clientConnection = clientConnection;
        this.indicatorValues = indicatorValues;
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
