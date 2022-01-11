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

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;

public class ExamJITSIProctoringServiceTest {

    @Test
    public void testTokenPayload() throws InvalidKeyException, NoSuchAlgorithmException {
        final Cryptor cryptorMock = Mockito.mock(Cryptor.class);
        Mockito.when(cryptorMock.decrypt(Mockito.any())).thenReturn(Result.of("fbvgeghergrgrthrehreg123"));
        final JitsiProctoringService examJITSIProctoringService =
                new JitsiProctoringService(null, null, cryptorMock, null, new JSONMapper());

        String accessToken = examJITSIProctoringService.createPayload(
                "test-app",
                "Test Name",
                "test-client",
                "SomeRoom",
                1609459200L,
                "https://test.ch",
                false);

        assertEquals(
                "{\"context\":{\"user\":{\"name\":\"Test Name\"}},\"moderator\":false,\"aud\":\"test-client\",\"iss\":\"test-app\",\"sub\":\"https://test.ch\",\"room\":\"SomeRoom\",\"exp\":1609459200}",
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
                "{\"context\":{\"user\":{\"name\":\"Test Name\"}},\"moderator\":true,\"aud\":\"test-client\",\"iss\":\"test-app\",\"sub\":\"https://test.ch\",\"room\":\"SomeRoom\",\"exp\":1609459200}",
                accessToken);
    }

    @Test
    public void testCreateProctoringURL() {
        final Cryptor cryptorMock = Mockito.mock(Cryptor.class);
        Mockito.when(cryptorMock.decrypt(Mockito.any())).thenReturn(Result.of("fbvgeghergrgrthrehreg123"));
        final JitsiProctoringService examJITSIProctoringService =
                new JitsiProctoringService(null, null, cryptorMock, null, new JSONMapper());
        final ProctoringRoomConnection data = examJITSIProctoringService.createProctoringConnection(
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
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZXh0Ijp7InVzZXIiOnsibmFtZSI6IlRlc3QgTmFtZSJ9fSwibW9kZXJhdG9yIjp0cnVlLCJhdWQiOiJ0ZXN0LWNsaWVudCIsImlzcyI6InRlc3QtYXBwIiwic3ViIjoic2ViLWppdHNpLmV4YW1wbGUuY2giLCJyb29tIjoiU29tZVJvb20iLCJleHAiOjE2MDk0NTkyMDB9.47qoBCXG34ITeMmrwxlTmDcc6JLSVVF1HAOlcSkCvqw",
                data.accessToken);

    }

}
