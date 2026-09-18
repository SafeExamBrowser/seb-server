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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
    
//    @Test
//    public void testSystemTime() {
//        DateTime now = DateTime.now().withZone(DateTimeZone.UTC);
//        long now1 = Utils.getMillisecondsNow();
//        DateTime now2 = new DateTime(Long.valueOf(String.valueOf(now1)), DateTimeZone.UTC);
//        Utils.toDateTimeUTCUnix(System.currentTimeMillis());
//         
//        System.out.println("******************* now Date Time " + now);
//        System.out.println("******************* now System    " + now2);
//    }

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

    @Test
    public void decryptDevData() {
        final MockEnvironment mockEnvironment = new MockEnvironment();
        mockEnvironment.setProperty("sebserver.webservice.internalSecret", "somePW");
        final String encrypted = "562057aa14d31efa78d494be6a298b8c22ec0860f1e76ab3f59d42e1f5822766604c2141087a9c0cadda99aeb55e962038a0c1bc5ef8dbbc0969b975a8591cf512b7034d408c36e9803307d52e44e305bf13f89a12f3fbda5590b281282d5fee93af2f36cf42f40417abc3f97320797ebb3207a18775c6202334bcd0137001c3e301dc53b356bbfd39c4a1d1957f1e55de5ee13fa57b80a18398d13c25e656673406b4643c9b16efb70203c311279f20fcd8005083b30d31606087e7f771cad5cfa9dbd074260eb8004e57db293016cad5d45560dd9b57f02804daf2b9a5cc6084bcfb4bd890fc6cc0114be77818cb43447c71f2330f54c04253ebe2223fdae16d1e9a48c8c9cccda8890bdac59239b49cae1fc3e9d6671ed7bec61d3b8e03471ab27cf14ca2f003f7fe6253e70a833e84e4a2912a678fab3ba672f85ddc2420c9791de05f8db774ccabbd4db4831f4b076ff113c397e7f8a76629d8440000cab437";
        final Cryptor cryptor = new Cryptor(mockEnvironment);
        final Result<CharSequence> decrypted = cryptor.decrypt(encrypted);
        assertFalse(decrypted.hasError());
        System.out.println("decrypted:");
        System.out.println(decrypted.get());
    }

    @Test
    public void encryptDevData() {
        final MockEnvironment mockEnvironment = new MockEnvironment();
        mockEnvironment.setProperty("sebserver.webservice.internalSecret", "somePW");
        final String text = "{\"id\":28,\"enableScreenProctoring\":true,\"spsServiceURL\":\"http://localhost:8090\",\"spsAPIKey\":\"sebserverClient\",\"spsAPISecret\":\"somePW\",\"spsAccountId\":\"SEBServerAPIAccount\",\"spsAccountPassword\":\"admin\",\"spsCollectingStrategy\":\"EXAM\",\"spsSEBGroupsSelection\":\"\",\"bundled\":true,\"changeStrategyConfirm\":false}";
        final Cryptor cryptor = new Cryptor(mockEnvironment);
        final Result<CharSequence> encrypt = cryptor.encrypt(text);
        assertFalse(encrypt.hasError());
        System.out.println("encrypted:");
        System.out.println(encrypt.get());
    }


}
