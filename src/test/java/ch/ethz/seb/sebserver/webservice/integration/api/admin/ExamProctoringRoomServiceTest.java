/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringRoomService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class ExamProctoringRoomServiceTest extends AdministrationAPIIntegrationTester {

    private static final String CONNECTION_TOKEN_1 = "connection_token1";
    private static final String CONNECTION_TOKEN_2 = "connection_token2";

    @Autowired
    private ExamProctoringRoomService examProctoringRoomService;
    @Autowired
    private ExamSessionService examSessionService;
    @Autowired
    private ExamAdminService examAdminService;
    @Autowired
    private ClientConnectionDAO clientConnectionDAO;

    @Test
    @Order(1)
    public void test01_checkExamRunning() {
        final Result<Collection<Exam>> runningExamsForInstitution =
                this.examSessionService.getRunningExamsForInstitution(1L);
        assertFalse(runningExamsForInstitution.hasError());
        final Collection<Exam> collection = runningExamsForInstitution.get();
        assertFalse(collection.isEmpty());
        final Exam exam = collection.stream().filter(e -> e.id == 2L).findAny().orElse(null);
        assertNotNull(exam);
        assertEquals("Demo Quiz 6 (MOCKUP)", exam.name);
        assertEquals("2", String.valueOf(exam.id));
    }

    @Test
    @Order(2)
    public void test02_setProctoringServiceSettings() {
        this.examAdminService.saveProctoringServiceSettings(
                2L,
                new ProctoringServiceSettings(
                        2L, true, ProctoringServerType.JITSI_MEET, "", 1, null, false,
                        "app-key", "app.secret", "sdk-key", "sdk.secret", false));

        assertTrue(this.examAdminService.isProctoringEnabled(2L).get());
    }

    @Test
    @Order(3)
    public void test03_addClientConnection() {
        final Result<ClientConnection> createNew = this.clientConnectionDAO.createNew(new ClientConnection(
                null,
                1L,
                2L,
                ConnectionStatus.CONNECTION_REQUESTED,
                CONNECTION_TOKEN_1,
                "",
                "",
                false,
                "",
                null));
        assertFalse(createNew.hasError());
    }

}
