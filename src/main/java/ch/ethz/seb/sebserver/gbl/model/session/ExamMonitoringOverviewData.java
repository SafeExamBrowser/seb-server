/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Collection;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExamMonitoringOverviewData(
        @JsonProperty("clientStates") ClientStatesData clientStates,
        @JsonProperty("clientGroups") Collection<ClientGroup> clientGroups,
        @JsonProperty("indicators") Map<String, Integer> indicators,
        @JsonProperty("notifications") Map<String, Integer> notifications
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
    
    public record ClientGroup(
            @JsonProperty(Domain.CLIENT_GROUP.ATTR_ID) Long id,
            @JsonProperty(Domain.CLIENT_GROUP.ATTR_NAME) String name,
            @JsonProperty("clientAmount") Integer clientAmount,
            @JsonProperty("spsGroupUUID") String spsGroupUUID,
            @JsonProperty("type") String type,
            @JsonProperty("typeValue") String typeValue) {
    }
}
