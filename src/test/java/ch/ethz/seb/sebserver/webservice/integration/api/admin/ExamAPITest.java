/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class ExamAPITest extends AdministrationAPIIntegrationTester {

    @Test
    public void testModify() throws Exception {
        final String sebAdminAccess = getSebAdminAccess();
        final Exam exam = ExamImportTest.createImportedExamFromLmsSetupMock(
                this,
                sebAdminAccess,
                sebAdminAccess,
                "LmsSetupMock",
                "quiz2",
                ExamType.MANAGED,
                "user5");

        assertNotNull(exam);
        assertEquals("quiz2", exam.getExternalId());
        assertEquals(ExamType.MANAGED, exam.getType());
        assertFalse(exam.getSupporter().isEmpty());

        // add ExamSupporter
        final Exam newExam = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_ADMINISTRATION_ENDPOINT)
                .withMethod(HttpMethod.PUT)
                .withBodyJson(new Exam(
                        exam.id,
                        exam.institutionId,
                        exam.lmsSetupId,
                        exam.externalId,
                        exam.name,
                        exam.description,
                        exam.startTime,
                        exam.endTime,
                        exam.startURL,
                        exam.type,
                        exam.owner,
                        Arrays.asList("user5"),
                        null,
                        false,
                        null,
                        true,
                        null, null, null, null))
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Exam>() {
                });

        assertFalse(newExam.getSupporter().isEmpty());
        assertTrue(newExam.getSupporter().size() == 1);
        assertEquals("user5", newExam.getSupporter().iterator().next());

        // try to add a user as exam supporter with no exam support role should not be possible
        final List<APIMessage> error = new RestAPITestHelper()
                .withAccessToken(sebAdminAccess)
                .withPath(API.EXAM_ADMINISTRATION_ENDPOINT)
                .withMethod(HttpMethod.PUT)
                .withBodyJson(new Exam(
                        exam.id,
                        exam.institutionId,
                        exam.lmsSetupId,
                        exam.externalId,
                        exam.name,
                        exam.description,
                        exam.startTime,
                        exam.endTime,
                        exam.startURL,
                        exam.type,
                        exam.owner,
                        Arrays.asList("user2"),
                        null,
                        false,
                        null,
                        true,
                        null, null, null, null))
                .withExpectedStatus(HttpStatus.BAD_REQUEST)
                .getAsObject(new TypeReference<List<APIMessage>>() {
                });

        assertNotNull(error);
        assertTrue(error.size() == 1);
        final APIMessage error1 = error.iterator().next();
        assertEquals("[exam, supporter, grantDenied, user2]", String.valueOf(error1.attributes));
    }

}
