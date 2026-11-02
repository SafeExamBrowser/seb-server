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

import ch.ethz.seb.sebserver.gui.api.RestCall;
import ch.ethz.seb.sebserver.gui.api.RestServiceImpl;
import ch.ethz.seb.sebserver.gui.api.auth.OAuth2AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.api.institution.GetInstitution;
import org.junit.Test;
import org.springframework.test.context.jdbc.Sql;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.util.Result;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class GetInstitutionTest extends GuiIntegrationTest {

    @Test
    public void testRestServiceInit() {
        final OAuth2AuthorizationContextHolder authorizationContextHolder = login("admin", "admin");
        final Collection<RestCall<?>> calls = new ArrayList<>();
        calls.add(new GetInstitution());

        final RestServiceImpl restService = new RestServiceImpl(authorizationContextHolder, new JSONMapper(), calls);

        final Result<Institution> call = restService.getBuilder(GetInstitution.class)
                .withURIVariable(API.PARAM_MODEL_ID, "2")
                .call();

        assertNotNull(call);
        assertFalse(call.hasError());
        final Institution institution = call.get();
        assertEquals("Institution2", institution.name);
    }

}
