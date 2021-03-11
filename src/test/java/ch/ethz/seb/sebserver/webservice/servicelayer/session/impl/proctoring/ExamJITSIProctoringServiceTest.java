/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;
import org.mockito.Mockito;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;

public class ExamJITSIProctoringServiceTest {

    @Test
    public void testTokenPayload() throws InvalidKeyException, NoSuchAlgorithmException {
        final Cryptor cryptorMock = Mockito.mock(Cryptor.class);
        Mockito.when(cryptorMock.decrypt(Mockito.any())).thenReturn("fbvgeghergrgrthrehreg123");
        final JitsiProctoringService examJITSIProctoringService =
                new JitsiProctoringService(null, null, cryptorMock, null);

        String accessToken = examJITSIProctoringService.createPayload(
                "test-app",
                "Test Name",
                "test-client",
                "SomeRoom",
                1609459200L,
                "https://test.ch",
                false);

        assertEquals(
                "{\"context\":{\"user\":{\"name\":\"Test Name\"}},\"iss\":\"test-app\",\"aud\":\"test-client\",\"sub\":\"https://test.ch\",\"room\":\"SomeRoom\",\"moderator\":false,\"exp\":1609459200}",
                accessToken);

        accessToken = examJITSIProctoringService.createPayload(
                "test-app",
                "Test Name",
                "test-client",
                "SomeRoom",
                1609459200L,
                "https://test.ch",
                true);

        assertEquals(
                "{\"context\":{\"user\":{\"name\":\"Test Name\"}},\"iss\":\"test-app\",\"aud\":\"test-client\",\"sub\":\"https://test.ch\",\"room\":\"SomeRoom\",\"moderator\":true,\"exp\":1609459200}",
                accessToken);
    }

    @Test
    public void testCreateProctoringURL() {
        final Cryptor cryptorMock = Mockito.mock(Cryptor.class);
        Mockito.when(cryptorMock.decrypt(Mockito.any())).thenReturn("fbvgeghergrgrthrehreg123");
        final JitsiProctoringService examJITSIProctoringService =
                new JitsiProctoringService(null, null, cryptorMock, null);
        final ProctoringRoomConnection data = examJITSIProctoringService.createProctoringConnection(
                ProctoringServerType.JITSI_MEET,
                "connectionToken",
                "https://seb-jitsi.example.ch",
                "test-app",
                "fbvgeghergrgrthrehreg123",
                "Test Name",
                "test-client",
                "SomeRoom",
                "Subject",
                1609459200L,
                true)
                .getOrThrow();

        assertNotNull(data);
        assertEquals(
                "https://seb-jitsi.example.ch",
                data.serverURL);

        assertEquals(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZXh0Ijp7InVzZXIiOnsibmFtZSI6IlRlc3QgTmFtZSJ9fSwiaXNzIjoidGVzdC1hcHAiLCJhdWQiOiJ0ZXN0LWNsaWVudCIsInN1YiI6InNlYi1qaXRzaS5leGFtcGxlLmNoIiwicm9vbSI6IlNvbWVSb29tIiwibW9kZXJhdG9yIjp0cnVlLCJleHAiOjE2MDk0NTkyMDB9.poOwfCsRjNqCizEQM3qFFWbjuX0bZLer3cqlbaPK9wc",
                data.accessToken);

    }

}
