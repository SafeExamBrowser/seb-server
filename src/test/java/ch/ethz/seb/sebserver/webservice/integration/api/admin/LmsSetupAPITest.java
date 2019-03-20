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

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class LmsSetupAPITest extends AdministrationAPIIntegrationTester {

    @Test
    public void testCreateModifyActivateDelete() throws Exception {
        // create new LmsSetup with seb-admin
        LmsSetup lmsSetup = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(Domain.LMS_SETUP.ATTR_INSTITUTION_ID, "1")
                .withAttribute(Domain.LMS_SETUP.ATTR_NAME, "new LmsSetup 1")
                .withAttribute(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        assertNotNull(lmsSetup);
        assertNotNull(lmsSetup.id);
        assertTrue(lmsSetup.institutionId.longValue() == 1);
        assertEquals("new LmsSetup 1", lmsSetup.name);
        assertTrue(LmsType.MOCKUP == lmsSetup.lmsType);
        assertFalse(lmsSetup.active);

        // set lms server and credentials
        final LmsSetup modified = new LmsSetup(
                lmsSetup.id,
                lmsSetup.institutionId,
                lmsSetup.name,
                lmsSetup.lmsType,
                "lms1Name",
                "lms1Secret",
                "https://www.lms1.com",
                null,
                null);

        lmsSetup = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.PUT)
                .withBodyJson(modified)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        assertNotNull(lmsSetup);
        assertNotNull(lmsSetup.id);
        assertTrue(lmsSetup.institutionId.longValue() == 1);
        assertEquals("new LmsSetup 1", lmsSetup.name);
        assertTrue(LmsType.MOCKUP == lmsSetup.lmsType);
        assertEquals("lms1Name", lmsSetup.lmsAuthName);
        // secrets, once set are not exposed
        assertEquals(null, lmsSetup.lmsAuthSecret);
        assertFalse(lmsSetup.active);

        // activate
        EntityProcessingReport report = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath("/").withPath(String.valueOf(lmsSetup.id)).withPath("/active")
                .withMethod(HttpMethod.POST)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityProcessingReport>() {
                });

        assertNotNull(report);
        assertNotNull(report.source);
        assertTrue(report.source.size() == 1);
        assertEquals(String.valueOf(lmsSetup.id), report.source.iterator().next().modelId);
        assertEquals("[]", report.dependencies.toString());
        assertEquals("[]", report.errors.toString());

        // get
        lmsSetup = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT).withPath("/")
                .withPath(String.valueOf(lmsSetup.id))
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        assertNotNull(lmsSetup);
        assertTrue(lmsSetup.active);

        // deactivate
        report = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath("/").withPath(String.valueOf(lmsSetup.id)).withPath("/inactive")
                .withMethod(HttpMethod.POST)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityProcessingReport>() {
                });

        assertNotNull(report);
        assertNotNull(report.source);
        assertTrue(report.source.size() == 1);
        assertEquals(String.valueOf(lmsSetup.id), report.source.iterator().next().modelId);
        assertEquals("[]", report.dependencies.toString());
        assertEquals("[]", report.errors.toString());

        lmsSetup = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT).withPath("/")
                .withPath(String.valueOf(lmsSetup.id))
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        assertNotNull(lmsSetup);
        assertFalse(lmsSetup.active);

        // delete
        report = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath("/").withPath(String.valueOf(lmsSetup.id))
                .withMethod(HttpMethod.DELETE)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityProcessingReport>() {
                });

        assertNotNull(report);
        assertNotNull(report.source);
        assertTrue(report.source.size() == 1);
        assertEquals(String.valueOf(lmsSetup.id), report.source.iterator().next().modelId);
        assertEquals("[]", report.dependencies.toString());
        assertEquals("[]", report.errors.toString());

        // get
        final List<APIMessage> error = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT).withPath("/")
                .withPath(String.valueOf(lmsSetup.id))
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.NOT_FOUND)
                .getAsObject(new TypeReference<List<APIMessage>>() {
                });

        assertNotNull(error);
        assertTrue(error.size() > 0);
        assertEquals("Resource LMS_SETUP with ID: 1 not found", error.get(0).details);
    }

    @Test
    public void testValidationOnCreate() throws Exception {
        // create new LmsSetup with seb-admin
        final List<APIMessage> errors = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(Domain.LMS_SETUP.ATTR_INSTITUTION_ID, "1")
                .withAttribute(Domain.LMS_SETUP.ATTR_NAME, "new LmsSetup 1")
                .getAsObject(new TypeReference<List<APIMessage>>() {
                });

        assertNotNull(errors);
        assertTrue(errors.size() == 1);
        assertEquals("Field validation error", errors.get(0).systemMessage);
    }

    @Test
    public void getForIds() throws Exception {
        // create some new LmsSetup with seb-admin
        final LmsSetup lmsSetup1 = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute("name", "new LmsSetup 1")
                .withAttribute(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });
        final LmsSetup lmsSetup2 = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute("name", "new LmsSetup 2")
                .withAttribute(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        final Collection<LmsSetup> lmsSetups = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath(API.LIST_PATH_SEGMENT)
                .withAttribute(API.PARAM_MODEL_ID_LIST, lmsSetup1.id + "," + lmsSetup2.id)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Collection<LmsSetup>>() {
                });

        assertNotNull(lmsSetups);
        assertTrue(lmsSetups.size() == 2);
    }

    @Test
    public void getNames() throws Exception {
        // create some new LmsSetup with seb-admin
        new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute("name", "new LmsSetup 1")
                .withAttribute(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });
        new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute("name", "new LmsSetup 2")
                .withAttribute(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        final Collection<EntityName> lmsSetupNames = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath(API.NAMES_PATH_SEGMENT)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Collection<EntityName>>() {
                });

        assertNotNull(lmsSetupNames);
        assertTrue(lmsSetupNames.size() == 2);
    }

    @Test
    public void getById() throws Exception {
        final Long id1 = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(Domain.LMS_SETUP.ATTR_INSTITUTION_ID, "1")
                .withAttribute("name", "new LmsSetup 1")
                .withAttribute(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                }).id;

        final Long id2 = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution2Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(Domain.LMS_SETUP.ATTR_INSTITUTION_ID, "2")
                .withAttribute("name", "new LmsSetup 2")
                .withAttribute(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                }).id;

        LmsSetup lmsSetup = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath(String.valueOf(id1))
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        assertNotNull(lmsSetup);
        assertTrue(lmsSetup.id.longValue() == id1.longValue());

        // a seb-admin is also able to get lms setup that is not the own institution
        lmsSetup = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath(String.valueOf(id2))
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        assertNotNull(lmsSetup);
        assertTrue(lmsSetup.id.longValue() == id2.longValue());

        // but a institutional-admin is not able to get lms setup that is on another institution
        new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath(String.valueOf(id2))
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .getAsString();
    }

}
