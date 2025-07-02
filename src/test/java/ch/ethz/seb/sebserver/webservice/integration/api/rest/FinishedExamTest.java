/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.RestServiceImpl;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.session.GetFinishedExamClientConnection;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.session.GetFinishedExamClientConnectionPage;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.session.GetFinishedExamPage;
import org.junit.Test;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class FinishedExamTest extends GuiIntegrationTest {

    @Test
    public void finishedExamsTest() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "admin",
                "admin",
                new GetFinishedExamPage(),
                new GetFinishedExamClientConnectionPage(),
                new GetFinishedExamClientConnection());

        // get finished exams page:
        final Page<Exam> finishedExams = restService
                .getBuilder(GetFinishedExamPage.class)
                .call()
                .getOrThrow();

        assertNotNull(finishedExams);
        assertFalse(finishedExams.content.isEmpty());
    }

}
