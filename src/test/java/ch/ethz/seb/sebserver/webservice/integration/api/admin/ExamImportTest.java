/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class ExamImportTest extends AdministrationAPIIntegrationTester {

    @Test
    public void testImportFromQuizz() throws Exception {
        // create new active LmsSetup Mock with seb-admin
        final LmsSetup lmsSetup1 = QuizDataTest.createLmsSetupMockWith(
                this,
                getSebAdminAccess(),
                "new LmsSetup 1",
                true);

        // import Exam from quiz1
        final Exam exam = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.EXAM_ADMINISTRATION_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(QuizData.QUIZ_ATTR_LMS_SETUP_ID, lmsSetup1.getModelId())
                .withAttribute(QuizData.QUIZ_ATTR_ID, "quiz1")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Exam>() {
                });

        assertNotNull(exam);
        assertEquals("quiz1", exam.getExternalId());
        assertEquals(lmsSetup1.id, exam.getLmsSetupId());
    }

}
