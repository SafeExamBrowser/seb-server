/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest.api.useraccount;

import java.util.Set;

import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.RestCall;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Lazy
@Component
@GuiProfile
public class GetUserDependencies extends RestCall<Set<EntityDependency>> {

    public GetUserDependencies() {
        super(new TypeKey<>(
                CallType.GET_DEPENDENCIES,
                EntityType.USER,
                new TypeReference<Set<EntityDependency>>() {
                }),
                HttpMethod.GET,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.USER_ACCOUNT_ENDPOINT + API.MODEL_ID_VAR_PATH_SEGMENT + API.DEPENDENCY_PATH_SEGMENT);
    }

}
