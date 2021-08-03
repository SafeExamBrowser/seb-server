/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Set;

import org.eclipse.swt.graphics.RGB;
import org.joda.time.DateTimeUtils;
import org.junit.Test;

public class UtilsTest {

    @Test
    public void testImmutableCollectionOf() {
        final Collection<String> r1 = Utils.immutableCollectionOf((String[]) null);
        final Collection<String> r2 = Utils.immutableCollectionOf((String) null);
        final Collection<String> r3 = Utils.immutableCollectionOf(null, null);
        final Collection<String> r4 = Utils.immutableCollectionOf("ONE", "TWO");

        assertEquals("[]", r1.toString());
        assertEquals("[null]", r2.toString());
        assertEquals("[null, null]", r3.toString());
        assertEquals("[ONE, TWO]", r4.toString());
    }

    @Test
    public void testImmutableSetOf() {
        final Set<String> r1 = Utils.immutableSetOf((String[]) null);
        final Set<String> r2 = Utils.immutableSetOf((String) null);
        final Set<String> r3 = Utils.immutableSetOf(null, null);
        final Set<String> r4 = Utils.immutableSetOf("ONE", "TWO");
        final Set<String> r5 = Utils.immutableSetOf("ONE", "TWO", "ONE");

        assertEquals("[]", r1.toString());
        assertEquals("[null]", r2.toString());
        assertEquals("[null]", r3.toString());
        assertEquals("[ONE, TWO]", r4.toString());
        assertEquals("[ONE, TWO]", r5.toString());
    }

    @Test
    public void testParseRGB() {
        String colorString = "FFFFFF";
        assertEquals(
                "RGB {255, 255, 255}",
                Utils.parseRGB(colorString).toString());

        colorString = "FFaa34";
        assertEquals(
                "RGB {255, 170, 52}",
                Utils.parseRGB(colorString).toString());
    }

    @Test
    public void testParseColorString() {
        final RGB color = new RGB(255, 255, 255);
        assertEquals("ffffff", Utils.parseColorString(color));

    }

    @Test
    public void testTimestamp() {
        final long currentTimeMillis = DateTimeUtils.currentTimeMillis();
        final long millisecondsNow = Utils.getMillisecondsNow();
        System.out.println("************* currentTimeMillis: " + currentTimeMillis);
        System.out.println("************* millisecondsNow: " + millisecondsNow);
        System.out.println("************* div: " + (millisecondsNow - currentTimeMillis));
    }

}
