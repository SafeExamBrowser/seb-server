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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class LmsSetupAPITest extends AdministrationAPIIntegrationTester {

    @Autowired
    private LmsAPIService lmsAPIService;

    @Before
    public void init() {
        this.lmsAPIService.cleanup();
    }

    @After
    public void cleanup() {
        this.lmsAPIService.cleanup();
    }

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
                null,
                null,
                null,
                null,
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
                .withPath(String.valueOf(lmsSetup.id)).withPath("/active")
                .withMethod(HttpMethod.POST)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityProcessingReport>() {
                });

        assertNotNull(report);
        assertNotNull(report.source);
        assertTrue(report.source.size() == 1);
        assertEquals(String.valueOf(lmsSetup.id), report.source.iterator().next().modelId);
        assertEquals("[EntityKey [modelId=1, entityType=LMS_SETUP]]", report.results.toString());
        assertEquals("[]", report.errors.toString());

        // get
        lmsSetup = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
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
                .withPath(String.valueOf(lmsSetup.id)).withPath("/inactive")
                .withMethod(HttpMethod.POST)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityProcessingReport>() {
                });

        assertNotNull(report);
        assertNotNull(report.source);
        assertTrue(report.source.size() == 1);
        assertEquals(String.valueOf(lmsSetup.id), report.source.iterator().next().modelId);
        assertEquals("[EntityKey [modelId=1, entityType=LMS_SETUP]]", report.results.toString());
        assertEquals("[]", report.errors.toString());

        lmsSetup = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
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
                .withPath(String.valueOf(lmsSetup.id))
                .withMethod(HttpMethod.DELETE)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityProcessingReport>() {
                });

        assertNotNull(report);
        assertNotNull(report.source);
        assertTrue(report.source.size() == 1);
        assertEquals(String.valueOf(lmsSetup.id), report.source.iterator().next().modelId);
        assertEquals("[EntityKey [modelId=1, entityType=LMS_SETUP]]", report.results.toString());
        assertEquals("[]", report.errors.toString());

        // get
        final List<APIMessage> error = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
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

    @Test
    public void testInstituionalView() throws Exception {
        // create new LmsSetup Mock with seb-admin
        final LmsSetup lmsSetup1 = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(Domain.LMS_SETUP.ATTR_NAME, "new LmsSetup 1")
                .withAttribute(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        // create new LmsSetup Mock with institutional 2 admin
        final LmsSetup lmsSetup2 = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution2Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(Domain.LMS_SETUP.ATTR_NAME, "new LmsSetup 1")
                .withAttribute(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        // get with institutional 1 admin, expected to see only the one created by seb-admin
        Page<LmsSetup> lmsSetups = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<LmsSetup>>() {
                });

        assertNotNull(lmsSetups);
        assertNotNull(lmsSetups.content);
        assertTrue(lmsSetups.content.size() == 1);
        LmsSetup lmsSetup = lmsSetups.content.get(0);
        assertEquals(lmsSetup1.id, lmsSetup.id);

        // get with institutional 2 admin, expected to see only the one self created
        lmsSetups = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution2Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<LmsSetup>>() {
                });

        assertNotNull(lmsSetups);
        assertNotNull(lmsSetups.content);
        assertTrue(lmsSetups.content.size() == 1);
        lmsSetup = lmsSetups.content.get(0);
        assertEquals(lmsSetup2.id, lmsSetup.id);
    }

    @Test
    public void testLmsSetupConnectionTest() throws Exception {
        // create new LmsSetup Mock with seb-admin
        LmsSetup lmsSetup = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(Domain.LMS_SETUP.ATTR_NAME, "new LmsSetup 1")
                .withAttribute(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        // test LMS connection should fail because there is no server set yet
        List<APIMessage> errors = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath(API.LMS_SETUP_TEST_PATH_SEGMENT)
                .withPath(lmsSetup.getModelId())
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.BAD_REQUEST)
                .getAsObject(new TypeReference<List<APIMessage>>() {
                });

        assertNotNull(errors);
        assertTrue(errors.size() == 3);

        // save (wrong) LMS server and credentials
        lmsSetup = new LmsSetup(
                lmsSetup.id,
                lmsSetup.institutionId,
                lmsSetup.name,
                lmsSetup.lmsType,
                "lms1Name",
                null, // no secret
                "https://www.lms1.com",
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        lmsSetup = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.PUT)
                .withBodyJson(lmsSetup)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        // test LMS connection again should fail because there is no secret set
        errors = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath(API.LMS_SETUP_TEST_PATH_SEGMENT)
                .withPath(lmsSetup.getModelId())
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.BAD_REQUEST)
                .getAsObject(new TypeReference<List<APIMessage>>() {
                });

        assertNotNull(errors);
        assertTrue(errors.size() == 1);
        assertEquals("[lmsSetup, lmsClientsecret, notNull]", String.valueOf(errors.get(0).attributes));

        // save correct LMS server and credentials
        lmsSetup = new LmsSetup(
                lmsSetup.id,
                lmsSetup.institutionId,
                lmsSetup.name,
                lmsSetup.lmsType,
                "lms1Name",
                "someSecret",
                "https://www.lms1.com",
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        lmsSetup = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.PUT)
                .withBodyJson(lmsSetup)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        final LmsSetupTestResult testResult = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath(API.LMS_SETUP_TEST_PATH_SEGMENT)
                .withPath(lmsSetup.getModelId())
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetupTestResult>() {
                });

        assertNotNull(testResult);
        assertTrue(testResult.isQuizAccessOk());
    }

}
