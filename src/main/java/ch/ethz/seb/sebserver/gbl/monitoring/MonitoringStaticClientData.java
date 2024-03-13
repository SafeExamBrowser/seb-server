/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.monitoring;

import java.util.Collection;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.session.ClientStaticData;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MonitoringStaticClientData {

    public static final String ATTR_STATIC_CONNECTION_DATA = "staticClientConnectionData";
    public static final String ATTR_DUPLICATIONS = "duplications";

    @JsonProperty(ATTR_STATIC_CONNECTION_DATA)
    public final Collection<ClientStaticData> staticClientConnectionData;
    @JsonProperty(ATTR_DUPLICATIONS)
    public final Set<Long> duplications;

    public MonitoringStaticClientData(
            @JsonProperty(ATTR_STATIC_CONNECTION_DATA) final Collection<ClientStaticData> staticClientConnectionData,
            @JsonProperty(ATTR_DUPLICATIONS) final Set<Long> duplications) {

        this.staticClientConnectionData = Utils.immutableCollectionOf(staticClientConnectionData);
        this.duplications = Utils.immutableSetOf(duplications);
    }

    public Collection<ClientStaticData> getStaticClientConnectionData() {
        return this.staticClientConnectionData;
    }

    public Set<Long> getDuplications() {
        return this.duplications;
    }

}
