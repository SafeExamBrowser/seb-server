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
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class ExamImportTest extends AdministrationAPIIntegrationTester {

    @Test
    public void testImportFromQuiz() throws Exception {
        // create new active LmsSetup Mock with seb-admin
        final LmsSetup lmsSetup1 = QuizDataTest.createLmsSetupMock(
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
        assertEquals(ExamType.UNDEFINED, exam.getType());

        final Exam exam2 = createImportedExamFromLmsSetupMock(
                this,
                getSebAdminAccess(),
                getSebAdminAccess(),
                "LmsSetupMock",
                "quiz2",
                ExamType.MANAGED);

        assertNotNull(exam2);
        assertEquals("quiz2", exam2.getExternalId());
        assertEquals(ExamType.MANAGED, exam2.getType());
    }

    @Test
    public void testImportFromQuizWithInstitutionalAdmin2() throws Exception {
        // Institutional administrators are able to create LMS Setups but not to import quizzes
        // as Exams. Only Exam administrators should be allowed to import Exams from quizzes
        try {
            createImportedExamFromLmsSetupMock(
                    this,
                    getAdminInstitution2Access(),
                    getAdminInstitution2Access(),
                    "LmsSetupMock",
                    "quiz2",
                    ExamType.MANAGED);
            fail("AssertionError expected here");
        } catch (final AssertionError ae) {
            assertEquals("Response status expected:<200> but was:<403>", ae.getMessage());
        }

        // Only Exam administrators should be allowed to import Exams from quizzes
        final Exam exam2 = createImportedExamFromLmsSetupMock(
                this,
                getAdminInstitution2Access(),
                getExamAdmin1(), // this exam administrator is on Institution 2
                "LmsSetupMock2",
                "quiz2",
                ExamType.MANAGED);

        assertNotNull(exam2);
        assertEquals("quiz2", exam2.getExternalId());
        assertEquals(ExamType.MANAGED, exam2.getType());
    }

    @Test
    public void testImportFromQuizWithInstitutional() throws Exception {
        // and creation between different institutions should also not be possible
        try {
            createImportedExamFromLmsSetupMock(
                    this,
                    getAdminInstitution1Access(),
                    getExamAdmin1(), // this exam administrator is on Institution 2
                    "LmsSetupMock",
                    "quiz2",
                    ExamType.MANAGED);
            fail("AssertionError expected here");
        } catch (final AssertionError ae) {
            assertEquals("Response status expected:<200> but was:<403>", ae.getMessage());
        }
    }

    public static final Exam createImportedExamFromLmsSetupMock(
            final AdministrationAPIIntegrationTester tester,
            final String tokenForLmsSetupCreation,
            final String tokenForExamImport,
            final String lmsSetupName,
            final String importQuizName,
            final ExamType examType) throws Exception {

        // create new active LmsSetup Mock with seb-admin
        final LmsSetup lmsSetup1 = QuizDataTest.createLmsSetupMock(
                tester,
                tokenForLmsSetupCreation,
                lmsSetupName,
                true);

        // import Exam from quiz1
        return tester.restAPITestHelper()
                .withAccessToken(tokenForExamImport)
                .withPath(API.EXAM_ADMINISTRATION_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute(QuizData.QUIZ_ATTR_LMS_SETUP_ID, lmsSetup1.getModelId())
                .withAttribute(QuizData.QUIZ_ATTR_ID, importQuizName)
                .withAttribute(Domain.EXAM.ATTR_TYPE, examType.name())
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Exam>() {
                });
    }

}
