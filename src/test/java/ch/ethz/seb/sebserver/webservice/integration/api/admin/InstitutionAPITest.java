/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.model.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.webservice.weblayer.api.RestAPI;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class InstitutionAPITest extends AdministrationAPIIntegrationTester {

    @Test
    public void getInstitutions() throws Exception {
        Page<Institution> institutions = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(RestAPI.ENDPOINT_INSTITUTION)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<Institution>>() {
                });

        assertNotNull(institutions);
        assertTrue(institutions.content.size() == 3);
        assertContainsInstitution("Institution1", institutions.content);
        assertContainsInstitution("Institution3", institutions.content);

        institutions = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(RestAPI.ENDPOINT_INSTITUTION)
                .withAttribute("active", "true")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<Institution>>() {
                });

        assertNotNull(institutions);
        assertTrue(institutions.content.size() == 2);
        assertContainsInstitution("Institution1", institutions.content);
        assertContainsInstitution("Institution2", institutions.content);

        institutions = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(RestAPI.ENDPOINT_INSTITUTION)
                .withAttribute("active", "false")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<Institution>>() {
                });

        assertNotNull(institutions);
        assertTrue(institutions.content.size() == 1);
        assertContainsInstitution("Institution3", institutions.content);

        // institutional admin sees only his institution
        institutions = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(RestAPI.ENDPOINT_INSTITUTION)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<Institution>>() {
                });

        assertNotNull(institutions);
        assertTrue(institutions.content.size() == 1);
        assertContainsInstitution("Institution1", institutions.content);

        final APIMessage errorMessage = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(RestAPI.ENDPOINT_INSTITUTION)
                .withAttribute("institution", "2") // try to hack
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .getAsObject(new TypeReference<APIMessage>() {
                });

        assertNotNull(errorMessage);
        assertEquals("1001", errorMessage.messageCode);
    }

    @Test
    public void getInstitutionById() throws Exception {
        Institution institution = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(RestAPI.ENDPOINT_INSTITUTION + "/1")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertTrue(institution.id.longValue() == 1);

        // a seb-admin is also able to get an institution that is not the one he self belongs to
        institution = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(RestAPI.ENDPOINT_INSTITUTION + "/2")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertTrue(institution.id.longValue() == 2);

        // but a institutional-admin is not able to get an institution that is not the one he self belongs to
        new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(RestAPI.ENDPOINT_INSTITUTION + "/2")
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .getAsString();
    }

    @Test
    public void createNewInstitution() throws Exception {
        // create new institution with seb-admin
        final Institution institution = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(RestAPI.ENDPOINT_INSTITUTION)
                .withMethod(HttpMethod.POST)
                .withAttribute("name", "new institution")
                .withAttribute("urlSuffix", "new_inst")
                .withAttribute("active", "false")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertNotNull(institution.id);
        assertEquals("new institution", institution.name);

        // an institutional admin should not be allowed to create a new institution
        APIMessage errorMessage = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(RestAPI.ENDPOINT_INSTITUTION)
                .withMethod(HttpMethod.POST)
                .withAttribute("name", "new institution")
                .withAttribute("urlSuffix", "new_inst")
                .withAttribute("active", "false")
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .getAsObject(new TypeReference<APIMessage>() {
                });

        // and predefined id should not be possible
        errorMessage = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(RestAPI.ENDPOINT_INSTITUTION)
                .withMethod(HttpMethod.POST)
                .withAttribute("id", "123")
                .withAttribute("name", "new institution")
                .withAttribute("urlSuffix", "new_inst")
                .withAttribute("active", "false")
                .withExpectedStatus(HttpStatus.BAD_REQUEST)
                .getAsObject(new TypeReference<APIMessage>() {
                });

        assertNotNull(errorMessage);
        assertEquals("1010", errorMessage.messageCode);
    }

//    @Test
//    public void createActivateModifyDeactivateAndDeleteInstitution() throws Exception  {
//        final Institution institution = new RestAPITestHelper()
//                .withAccessToken(getSebAdminAccess())
//                .withPath(RestAPI.ENDPOINT_INSTITUTION + "/create")
//                .withMethod(HttpMethod.PUT)
//                .withBodyJson(new Institution(null, ))
//                .withExpectedStatus(HttpStatus.OK)
//
//                .getAsObject(new TypeReference<Institution>() {
//                });
//    }

    static void assertContainsInstitution(final String name, final Collection<Institution> institutions) {
        assert institutions != null;
        assert institutions.stream()
                .filter(inst -> inst.name.equals(name))
                .findFirst()
                .isPresent();
    }
}
