/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.integration;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestServiceImpl;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.OAuth2AuthorizationContextHolder;

public class RestServiceTest extends GuiIntegrationTest {

    @Test
    public void testRestServiceInit() {
        final OAuth2AuthorizationContextHolder authorizationContextHolder = login("admin", "admin");
        final Collection<RestCall<?>> calls = new ArrayList<>();
        calls.add(new RestServiceTest.GetInstitution());

        final RestServiceImpl restService = new RestServiceImpl(authorizationContextHolder, new JSONMapper(), calls);

        final Result<Institution> call = restService.getBuilder(RestServiceTest.GetInstitution.class)
                .withURIVariable(API.PARAM_MODEL_ID, "2")
                .call();

        assertNotNull(call);
        assertFalse(call.hasError());
        final Institution institution = call.get();
        assertEquals("Institution2", institution.name);
    }

    public static class GetInstitution extends RestCall<Institution> {

        public GetInstitution() {
            super(new TypeKey<>(
                    CallType.GET_SINGLE,
                    EntityType.INSTITUTION,
                    new TypeReference<Institution>() {
                    }),
                    HttpMethod.GET,
                    MediaType.APPLICATION_FORM_URLENCODED,
                    API.INSTITUTION_ENDPOINT + API.MODEL_ID_VAR_PATH_SEGMENT);
        }

    }

}
