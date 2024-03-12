/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.*;

import java.util.Objects;

import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

public class ReplTest {

//    @Test
//    @Ignore
//    public void testDateFormatting() {
//        final String datestring = DateTime.now(DateTimeZone.UTC).toString("yyyy-MM-dd'T'HH:mm:ss");
//        assertEquals("", datestring);
//    }
//
//    @Test
//    @Ignore
//    public void testGenPwd() {
//        final CharSequence meetingPwd = UUID.randomUUID().toString().subSequence(0, 9);
//        assertEquals("", meetingPwd);
//    }

//    @Test
//    public void testTimezone() {
//        assertEquals("", DateTimeZone.UTC.getID());
//    }

//    @Test
//    public void testPeriod() {
//        final Period period = new Period(
//                DateTime.now(DateTimeZone.UTC),
//                DateTime.now(DateTimeZone.UTC).plusDays(1));
//
//        final Interval interv = new Interval(
//                DateTime.now(DateTimeZone.UTC),
//                DateTime.now(DateTimeZone.UTC).plusDays(1));
//
//        assertEquals(Constants.DAY_IN_MIN, interv.toDurationMillis() / Constants.MINUTE_IN_MILLIS);
//    }

//    @Test
//    public void testBooleanMatch() {
//        assertTrue(Boolean.valueOf(false) == Boolean.valueOf(false));
//        assertTrue(new Boolean(false) == new Boolean(false));
//    }

    @Test
    public void testObjectEquals() {
        assertTrue(Objects.equals("", ""));
        assertTrue(Objects.equals(null, null));
        assertFalse(Objects.equals("", null));
    }

    @Test
    public void encryptSignatureKey() {
        final MockEnvironment mockEnvironment = new MockEnvironment();
        mockEnvironment.setProperty("sebserver.webservice.internalSecret", "37e57333-d3f8-46b3-a614-2576a21bcee5");
        final String signature =
                "d1g2S21aZXRRZ2VZcUppeDNXN1Z0dy42NTg1NzQ5ODI5MS4xNjIzODMxNDkxMTkxLjAuUVJOMFNMK056VHpocWE1aE1SbTZDeUxaL2VWZ0Vud0EzMkxnb1VRNjZPcz0";
        final Cryptor cryptor = new Cryptor(mockEnvironment);
        final Result<CharSequence> encrypt = cryptor.encrypt(signature);
        assertFalse(encrypt.hasError());
        final CharSequence charSequence = encrypt.get();
        assertNotNull(charSequence);
        assertEquals("334", String.valueOf(charSequence.length()));
//        assertEquals(
//                "4a455832bebb66925d43431d8e99f6a093368b63af75d83a229071a47507762617697a2f0caa8ecc5c3814a3543ceca9797d8d75592fb77b2e28102ab54e26497e911350219cc57f7a0644c4f0e25278899f29dec128918d4a2fc832cd3e2d0b131a69d8098be5ad26c01ef215f0f1709a3cd38986fde5bee56b8b9903d4bc916061f5fa634536a8e84c34de010166b1448bb85cb7e6641890745173155ea8ca23d71377eed0cd",
//                Utils.toString(charSequence));

    }

}
