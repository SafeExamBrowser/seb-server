/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.Mockito;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnectionData;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;

public class ExamJITSIProctoringServiceTest {

    @Test
    public void testCreateProctoringURL() {
        final Cryptor cryptorMock = Mockito.mock(Cryptor.class);
        Mockito.when(cryptorMock.decrypt(Mockito.any())).thenReturn("fbvgeghergrgrthrehreg123");
        final ExamJITSIProctoringService examJITSIProctoringService =
                new ExamJITSIProctoringService(null, null, cryptorMock);
        final SEBProctoringConnectionData data = examJITSIProctoringService.createProctoringConnectionData(
                ProctoringServerType.JITSI_MEET,
                "connectionToken",
                "https://seb-jitsi.example.ch",
                "test-app",
                "fbvgeghergrgrthrehreg123",
                "Test Name",
                "test-client",
                "SomeRoom",
                "Subject",
                1609459200L)
                .getOrThrow();

        assertNotNull(data);
        assertEquals(
                "https://seb-jitsi.example.ch",
                data.serverURL);
        assertEquals(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZXh0Ijp7InVzZXIiOnsibmFtZSI6IlRlc3QgTmFtZSJ9fSwiaXNzIjoidGVzdC1hcHAiLCJhdWQiOiJ0ZXN0LWNsaWVudCIsInN1YiI6InNlYi1qaXRzaS5leGFtcGxlLmNoIiwicm9vbSI6IlNvbWVSb29tIiwiZXhwIjoxNjA5NDU5MjAwfQ.4ovqUkG6jrLvkDEZNdhbtFI_DFLDFsM2eBJHhcYq7a4",
                data.accessToken);

    }

}
