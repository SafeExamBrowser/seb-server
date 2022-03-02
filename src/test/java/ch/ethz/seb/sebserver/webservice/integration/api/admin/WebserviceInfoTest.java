/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.EntityName;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class WebserviceInfoTest extends AdministrationAPIIntegrationTester {

    @Test
    public void testGetLogo() throws Exception {
        String result = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.INFO_ENDPOINT + API.LOGO_PATH_SEGMENT)
                .withPath("/inst1")
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsString();

        assertEquals("", result);

        result = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.INFO_ENDPOINT + API.LOGO_PATH_SEGMENT)
                .withPath("/inst2")
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsString();

        assertEquals("AAA", result);
    }

    @Test
    public void test_getInstitutionInfo() throws Exception {

        Collection<EntityName> result = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.INFO_ENDPOINT + API.INFO_INST_PATH_SEGMENT)
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Collection<EntityName>>() {
                });

        assertNotNull(result);
        assertTrue(result.stream().filter(en -> "Institution1".equals(en.name)).findFirst().isPresent());
        assertTrue(result.stream().filter(en -> "Institution2".equals(en.name)).findFirst().isPresent());
        assertFalse(result.stream().filter(en -> "Institution3".equals(en.name)).findFirst().isPresent());

        result = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.INFO_ENDPOINT + API.INFO_INST_PATH_SEGMENT)
                .withPath("/inst2")
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Collection<EntityName>>() {
                });

        assertNotNull(result);
        assertFalse(result.stream().filter(en -> "Institution1".equals(en.name)).findFirst().isPresent());
        assertTrue(result.stream().filter(en -> "Institution2".equals(en.name)).findFirst().isPresent());
        assertFalse(result.stream().filter(en -> "Institution3".equals(en.name)).findFirst().isPresent());

    }

}
