/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution;

import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;

@Lazy
@Component
@GuiProfile
public class GetInstitutionDependency extends RestCall<Set<EntityKey>> {

    protected GetInstitutionDependency() {
        super(
                new TypeReference<Set<EntityKey>>() {
                },
                HttpMethod.GET,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.INSTITUTION_ENDPOINT + API.MODEL_ID_VAR_PATH_SEGMENT + API.DEPENDENCY_PATH_SEGMENT);
    }

}
