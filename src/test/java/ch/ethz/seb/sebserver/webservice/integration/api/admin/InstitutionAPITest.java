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
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.SEBServerRestEndpoints;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class InstitutionAPITest extends AdministrationAPIIntegrationTester {

    @Test
    public void getInstitutions() throws Exception {
        Page<Institution> institutions = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<Institution>>() {
                });

        assertNotNull(institutions);
        assertTrue(institutions.content.size() == 3);
        assertContainsInstitution("Institution1", institutions.content);
        assertContainsInstitution("Institution3", institutions.content);

        institutions = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
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
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
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
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<Institution>>() {
                });

        assertNotNull(institutions);
        assertTrue(institutions.content.size() == 1);
        assertContainsInstitution("Institution1", institutions.content);

        // Institutional admin tries to get data from other institution
        final List<APIMessage> errorMessage = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
                .withAttribute("institutionId", "2") // try to hack
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .getAsObject(new TypeReference<List<APIMessage>>() {
                });

        assertNotNull(errorMessage);
        assertTrue(errorMessage.size() > 0);
        assertEquals("1001", errorMessage.get(0).messageCode);
    }

    @Test
    public void getInstitutionById() throws Exception {
        Institution institution = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION + "/1")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertTrue(institution.id.longValue() == 1);

        // a seb-admin is also able to get an institution that is not the one he self belongs to
        institution = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION + "/2")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertTrue(institution.id.longValue() == 2);

        // but a institutional-admin is not able to get an institution that is not the one he self belongs to
        new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION + "/2")
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .getAsString();
    }

    @Test
    public void createNewInstitution() throws Exception {
        // create new institution with seb-admin
        Institution institution = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
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
        List<APIMessage> errorMessage = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
                .withMethod(HttpMethod.POST)
                .withAttribute("name", "new institution")
                .withAttribute("urlSuffix", "new_inst")
                .withAttribute("active", "false")
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .getAsObject(new TypeReference<List<APIMessage>>() {
                });

        // and name for institution must be unique
        errorMessage = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
                .withMethod(HttpMethod.POST)
                .withAttribute("name", "new institution")
                .withAttribute("urlSuffix", "new_inst")
                .withAttribute("active", "false")
                .withExpectedStatus(HttpStatus.BAD_REQUEST)
                .getAsObject(new TypeReference<List<APIMessage>>() {
                });

        assertNotNull(errorMessage);
        assertTrue(errorMessage.size() > 0);
        assertEquals("1010", errorMessage.get(0).messageCode);

        // and predefined id should be ignored
        institution = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
                .withMethod(HttpMethod.POST)
                .withAttribute("id", "123")
                .withAttribute("name", "newer institution")
                .withAttribute("urlSuffix", "new_inst")
                .withAttribute("active", "false")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertEquals("newer institution", institution.name);
    }

    @Test
    public void createActivateModifyDeactivateAndDeleteInstitution() throws Exception {
        // create new institution with seb-admin
        final String sebAdminAccess = getSebAdminAccess();
        Institution institution = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
                .withMethod(HttpMethod.POST)
                .withAttribute("name", "testInstitution")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertEquals("testInstitution", institution.name);
        assertFalse(institution.active);

        // get
        institution = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION).withPath("/")
                .withPath(String.valueOf(institution.id))
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertEquals("testInstitution", institution.name);
        assertEquals(null, institution.urlSuffix);
        assertFalse(institution.active);

        // modify
        institution = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION).withPath("/")
                .withPath(String.valueOf(institution.id))
                .withMethod(HttpMethod.PUT)
                .withBodyJson(new Institution(null, "testInstitution", "testSuffix", null, null))
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertEquals("testInstitution", institution.name);
        assertEquals("testSuffix", institution.urlSuffix);
        assertFalse(institution.active);

        // activate
        EntityProcessingReport report = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
                .withPath("/").withPath(String.valueOf(institution.id)).withPath("/active")
                .withMethod(HttpMethod.POST)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityProcessingReport>() {
                });

        assertNotNull(report);
        assertEquals("EntityProcessingReport "
                + "[source=[EntityKey [modelId=4, entityType=INSTITUTION]], "
                + "dependencies=[], "
                + "errors=[]]",
                report.toString());
        // get
        institution = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION).withPath("/")
                .withPath(String.valueOf(institution.id))
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertTrue(institution.active);

        // deactivate
        report = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
                .withPath("/").withPath(String.valueOf(institution.id)).withPath("/inactive")
                .withMethod(HttpMethod.POST)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityProcessingReport>() {
                });

        assertNotNull(report);
        assertEquals("EntityProcessingReport "
                + "[source=[EntityKey [modelId=4, entityType=INSTITUTION]], "
                + "dependencies=[], "
                + "errors=[]]",
                report.toString());
        // get
        institution = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION).withPath("/")
                .withPath(String.valueOf(institution.id))
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertFalse(institution.active);

        // delete
        report = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION)
                .withPath("/").withPath(String.valueOf(institution.id))
                .withMethod(HttpMethod.DELETE)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityProcessingReport>() {
                });

        assertNotNull(report);
        assertEquals("EntityProcessingReport "
                + "[source=[EntityKey [modelId=4, entityType=INSTITUTION]], "
                + "dependencies=[], "
                + "errors=[]]",
                report.toString());

        // get
        final List<APIMessage> error = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(SEBServerRestEndpoints.ENDPOINT_INSTITUTION).withPath("/")
                .withPath(String.valueOf(institution.id))
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.NOT_FOUND)
                .getAsObject(new TypeReference<List<APIMessage>>() {
                });

        assertNotNull(error);
        assertTrue(error.size() > 0);
        assertEquals("Resource INSTITUTION with ID: 4 not found", error.get(0).details);
    }

    static void assertContainsInstitution(final String name, final Collection<Institution> institutions) {
        assert institutions != null;
        assert institutions.stream()
                .filter(inst -> inst.name.equals(name))
                .findFirst()
                .isPresent();
    }
}
