/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

public class ReplTest {

    @Test
    @Ignore
    public void testDateFormatting() {
        final String datestring = DateTime.now(DateTimeZone.UTC).toString("yyyy-MM-dd'T'HH:mm:ss");
        assertEquals("", datestring);
    }

    @Test
    @Ignore
    public void testGenPwd() {
        final CharSequence meetingPwd = UUID.randomUUID().toString().subSequence(0, 9);
        assertEquals("", meetingPwd);
    }

}
