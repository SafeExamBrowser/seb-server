/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProctoringGroupMonitoringData", description = "Monitoring data for a proctoring group")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProctoringGroupMonitoringData(
        @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_UUID) String uuid,
        @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_NAME) String name,
        @JsonProperty(Domain.SCREEN_PROCTORING_GROUP.ATTR_SIZE) Integer size) {
}
