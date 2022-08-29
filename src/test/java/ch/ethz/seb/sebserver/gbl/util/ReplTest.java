/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Objects;

import org.junit.Test;

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

}
