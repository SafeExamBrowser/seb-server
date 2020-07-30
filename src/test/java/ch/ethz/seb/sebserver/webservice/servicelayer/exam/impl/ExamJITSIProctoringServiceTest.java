/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExamJITSIProctoringServiceTest {

    @Test
    public void testCreateProctoringURL() {
        final ExamJITSIProctoringService examJITSIProctoringService = new ExamJITSIProctoringService(null);
        final String jwt = examJITSIProctoringService.createProctoringURL(
                "https://seb-jitsi.example.ch",
                "test-app",
                "fbvgeghergrgrthrehreg123",
                "Test Name",
                "test-client",
                "SomeRoom",
                1609459200L)
                .getOrThrow();

        assertEquals(
                "https://seb-jitsi.example.ch/SomeRoom?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZXh0Ijp7InVzZXIiOnsibmFtZSI6IlRlc3QgTmFtZSJ9fSwiaXNzIjoidGVzdC1hcHAiLCJhdWQiOiJ0ZXN0LWNsaWVudCIsInN1YiI6InNlYi1qaXRzaS5leGFtcGxlLmNoIiwicm9vbSI6IlNvbWVSb29tIiwiZXhwIjoxNjA5NDU5MjAwfQ.4ovqUkG6jrLvkDEZNdhbtFI_DFLDFsM2eBJHhcYq7a4",
                jwt);
    }

}
