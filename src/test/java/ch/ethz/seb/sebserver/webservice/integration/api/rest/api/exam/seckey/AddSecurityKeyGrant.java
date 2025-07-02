/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest.api.exam.seckey;

import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.RestCall;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Lazy
@Component
@GuiProfile
public class AddSecurityKeyGrant extends RestCall<SecurityKey> {

    public AddSecurityKeyGrant() {
        super(new TypeKey<>(
                CallType.NEW,
                EntityType.SEB_SECURITY_KEY_REGISTRY,
                new TypeReference<SecurityKey>() {
                }),
                HttpMethod.POST,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.EXAM_ADMINISTRATION_ENDPOINT
                        + API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                        + API.EXAM_ADMINISTRATION_SEB_SECURITY_KEY_GRANTS_PATH_SEGMENT);
    }

}
