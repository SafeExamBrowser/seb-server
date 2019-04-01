/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class QuizDataTest extends AdministrationAPIIntegrationTester {

    @Test
    public void testGetInstitutionalQuizPage() throws Exception {
        // create new active LmsSetup Mock with seb-admin
        final LmsSetup lmsSetup1 = createLmsSetupMockWith(
                this,
                getSebAdminAccess(),
                "new LmsSetup 1",
                true);

        assertNotNull(lmsSetup1);
        assertTrue(lmsSetup1.isActive());

        // create new inactive LmsSetup Mock with institution 2 admin
        final LmsSetup lmsSetup2 = createLmsSetupMockWith(
                this,
                getAdminInstitution2Access(),
                "new LmsSetup 2",
                false);

        assertNotNull(lmsSetup2);
        assertFalse(lmsSetup2.isActive());

        // for the active LmsSetup we should get the quizzes page but only the quizzes from LmsSetup of seb-admin
        Page<QuizData> quizzes = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.QUIZ_DISCOVERY_ENDPOINT)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<QuizData>>() {
                });

        assertNotNull(quizzes);
        assertTrue(quizzes.content.size() == 5);

        // for the inactive LmsSetup we should'nt get any quizzes
        quizzes = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution2Access())
                .withPath(API.QUIZ_DISCOVERY_ENDPOINT)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<QuizData>>() {
                });

        assertNotNull(quizzes);
        assertTrue(quizzes.content.size() == 0);

        // activate / deactivate
        new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath(String.valueOf(lmsSetup1.id)).withPath("/inactive")
                .withMethod(HttpMethod.POST)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityProcessingReport>() {
                });
        new RestAPITestHelper()
                .withAccessToken(getAdminInstitution2Access())
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withPath(String.valueOf(lmsSetup2.id)).withPath("/active")
                .withMethod(HttpMethod.POST)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<EntityProcessingReport>() {
                });

        // now we should not get any quizzes for the seb-admin
        quizzes = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.QUIZ_DISCOVERY_ENDPOINT)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<QuizData>>() {
                });

        assertNotNull(quizzes);
        assertTrue(quizzes.content.size() == 0);

        // but for the now active lmsSetup2 we should get the quizzes
        quizzes = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution2Access())
                .withPath(API.QUIZ_DISCOVERY_ENDPOINT)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<QuizData>>() {
                });

        assertNotNull(quizzes);
        assertTrue(quizzes.content.size() == 5);
    }

    @Test
    public void testGetQuiz() throws Exception {
        // create new active LmsSetup Mock with seb-admin
        final LmsSetup lmsSetup = createLmsSetupMockWith(
                this,
                getSebAdminAccess(),
                "new LmsSetup 1",
                true);

        final QuizData quizData = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.QUIZ_DISCOVERY_ENDPOINT)
                .withPath("quiz1")
                .withAttribute(QuizData.QUIZ_ATTR_LMS_SETUP_ID, lmsSetup.getModelId())
                .withMethod(HttpMethod.GET)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<QuizData>() {
                });

        assertNotNull(quizData);
    }

    public static final LmsSetup createLmsSetupMockWith(
            final AdministrationAPIIntegrationTester tester,
            final String token,
            final String name,
            final boolean active) throws Exception {

        LmsSetup lmsSetup = tester.restAPITestHelper()
                .withAccessToken(token)
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(Domain.LMS_SETUP.ATTR_NAME, name)
                .withAttribute(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        lmsSetup = new LmsSetup(
                lmsSetup.id,
                lmsSetup.institutionId,
                lmsSetup.name,
                lmsSetup.lmsType,
                "lms1Name",
                "somePW",
                "https://www.lms1.com",
                null,
                null);

        lmsSetup = tester.restAPITestHelper()
                .withAccessToken(token)
                .withPath(API.LMS_SETUP_ENDPOINT)
                .withMethod(HttpMethod.PUT)
                .withBodyJson(lmsSetup)
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<LmsSetup>() {
                });

        if (active) {
            final EntityProcessingReport report = tester.restAPITestHelper()
                    .withAccessToken(token)
                    .withPath(API.LMS_SETUP_ENDPOINT)
                    .withPath(String.valueOf(lmsSetup.id)).withPath("/active")
                    .withMethod(HttpMethod.POST)
                    .withExpectedStatus(HttpStatus.OK)
                    .getAsObject(new TypeReference<EntityProcessingReport>() {
                    });
            assertTrue(report.errors.isEmpty());
            return tester.restAPITestHelper()
                    .withAccessToken(token)
                    .withPath(API.LMS_SETUP_ENDPOINT)
                    .withPath(String.valueOf(lmsSetup.id))
                    .withMethod(HttpMethod.GET)
                    .withExpectedStatus(HttpStatus.OK)
                    .getAsObject(new TypeReference<LmsSetup>() {
                    });
        }

        return lmsSetup;
    }
}
