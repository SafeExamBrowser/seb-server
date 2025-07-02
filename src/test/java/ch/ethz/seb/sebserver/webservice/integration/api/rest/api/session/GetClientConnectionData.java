/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest.api.session;

import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.RestCall;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Lazy
@Component
@GuiProfile
public class GetClientConnectionData extends RestCall<ClientConnectionData> {

    public GetClientConnectionData() {
        super(new TypeKey<>(
                CallType.GET_SINGLE,
                EntityType.CLIENT_CONNECTION,
                new TypeReference<ClientConnectionData>() {
                }),
                HttpMethod.GET,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.EXAM_MONITORING_ENDPOINT +
                        API.PARENT_MODEL_ID_VAR_PATH_SEGMENT +
                        API.EXAM_MONITORING_SEB_CONNECTION_TOKEN_PATH_SEGMENT);
    }

}
