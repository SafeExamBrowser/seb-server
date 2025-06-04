/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExamMonitoringOverviewData(
        @JsonProperty("clientStates") ClientStatesData clientStates,
        @JsonProperty("clientGroups") Collection<ClientGroup> clientGroups,
        @JsonProperty("indicators") Indicators indicators,
        @JsonProperty("notifications") NotificationData notifications
) {

    public enum ClientStates {
        CONNECTION_REQUESTED, READY, ACTIVE, CLOSED, DISABLED, MISSING
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ClientStatesData {
        @JsonProperty("total") public int total = 0;
        @JsonProperty("CONNECTION_REQUESTED") public int CONNECTION_REQUESTED = 0;
        @JsonProperty("READY") public int READY = 0;
        @JsonProperty("ACTIVE") public int ACTIVE = 0;
        @JsonProperty("CLOSED") public int CLOSED = 0;
        @JsonProperty("DISABLED") public int DISABLED = 0;
        @JsonProperty("MISSING") public int MISSING = 0;

        public void calcTotal() {
            total = CONNECTION_REQUESTED + READY + ACTIVE + CLOSED + DISABLED + MISSING;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Indicators {
        @JsonProperty("WLAN_STATUS") public IndicatorData WLAN_STATUS;
        @JsonProperty("BATTERY_STATUS") public IndicatorData BATTERY_STATUS;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class IndicatorData {
        @JsonProperty("color") public String color;
        @JsonProperty("incident") public int incident;
        @JsonProperty("warning") public int warning;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class NotificationData {
        @JsonProperty("total") public int total = 0;
        @JsonProperty("LOCK_SCREEN") public int LOCK_SCREEN = 0;
        @JsonProperty("RAISE_HAND") public int RAISE_HAND = 0;
        
        public void calcTotal() {
            total = LOCK_SCREEN + RAISE_HAND;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ClientGroup {
        @JsonProperty(Domain.CLIENT_GROUP.ATTR_ID) public final Long id;
        @JsonProperty(Domain.CLIENT_GROUP.ATTR_NAME) public final String name;
        @JsonProperty("clientAmount") public int clientAmount;
        @JsonProperty("spsGroupUUID") public final String spsGroupUUID;
        @JsonProperty("type") public final String type;
        @JsonProperty("typeValue") public final String typeValue;

        public ClientGroup(
                @JsonProperty(Domain.CLIENT_GROUP.ATTR_ID) final Long id,
                @JsonProperty(Domain.CLIENT_GROUP.ATTR_NAME) final String name,
                @JsonProperty("spsGroupUUID") final String spsGroupUUID,
                @JsonProperty("type") final String type,
                @JsonProperty("typeValue") final String typeValue) {
            
            this.id = id;
            this.name = name;
            this.spsGroupUUID = spsGroupUUID;
            this.type = type;
            this.typeValue = typeValue;
            this.clientAmount = 0;
        }
    }
    
}
