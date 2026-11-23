/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.Test;
import org.mockito.InOrder;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;

public class ExamMonitoringV3ServiceImplTest {

    private static final Long EXAM_ID = 1L;

    private final ExamSessionCacheService examSessionCacheService = mock(ExamSessionCacheService.class);
    private final ExamAdminService examAdminService = mock(ExamAdminService.class);
    private final ClientConnectionDAO clientConnectionDAO = mock(ClientConnectionDAO.class);
    private final ClientGroupDAO clientGroupDAO = mock(ClientGroupDAO.class);
    private final ExamSessionService examSessionService = mock(ExamSessionService.class);

    private final ExamMonitoringV3ServiceImpl candidate = new ExamMonitoringV3ServiceImpl(
            this.examSessionCacheService,
            this.examAdminService,
            this.clientConnectionDAO,
            this.clientGroupDAO,
            this.examSessionService);

    private final Exam exam = new Exam(EXAM_ID, 1L, 1L, "101:1:c1:i1",
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, Boolean.FALSE, null);

    @Test
    public void overviewSyncsClientConnectionsBeforeReadingCache() {
        when(this.clientConnectionDAO.getConnectionTokens(EXAM_ID)).thenReturn(Result.of(Collections.emptyList()));
        when(this.clientGroupDAO.allForExam(EXAM_ID)).thenReturn(Result.of(Collections.emptyList()));

        this.candidate.getExamMonitoringOverviewData(this.exam);

        final InOrder inOrder = inOrder(this.examSessionService, this.clientConnectionDAO);
        inOrder.verify(this.examSessionService).updateClientConnections(EXAM_ID);
        inOrder.verify(this.clientConnectionDAO).getConnectionTokens(EXAM_ID);
    }

    @Test
    public void connectionListSyncsClientConnectionsBeforeReadingCache() {
        when(this.clientConnectionDAO.getConnectionTokens(EXAM_ID)).thenReturn(Result.of(Collections.emptyList()));

        this.candidate.getFullMonitoringPageData(this.exam, Utils.truePredicate());

        final InOrder inOrder = inOrder(this.examSessionService, this.clientConnectionDAO);
        inOrder.verify(this.examSessionService).updateClientConnections(EXAM_ID);
        inOrder.verify(this.clientConnectionDAO).getConnectionTokens(EXAM_ID);
    }
}
