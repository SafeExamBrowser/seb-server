/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringStaticClientData;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;

@Lazy
@Component
@GuiProfile
public class GetMonitoringStaticClientData extends RestCall<MonitoringStaticClientData> {

    public GetMonitoringStaticClientData() {
        super(new TypeKey<>(
                CallType.GET_LIST,
                null,
                new TypeReference<MonitoringStaticClientData>() {
                }),
                HttpMethod.POST,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.EXAM_MONITORING_ENDPOINT
                        + API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                        + API.EXAM_MONITORING_STATIC_CLIENT_DATA);
    }

}
