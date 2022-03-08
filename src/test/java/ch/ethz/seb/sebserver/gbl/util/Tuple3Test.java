/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.Test;

public class Tuple3Test {

    @Test
    public void test3() {
        final Tuple3<String> candidate = new Tuple3<>("1", "2", "3");
        assertEquals("(1, 2, 3)", candidate.toString());
        assertEquals("1", candidate.get_1());
        assertEquals("2", candidate.get_2());
        assertEquals("3", candidate.get_3());

        final Tuple3<String> candidate1 = new Tuple3<>("1", "2", "3");
        final Tuple3<String> candidate2 = new Tuple3<>("4", "5", "6");
        assertEquals(candidate, candidate1);
        assertNotEquals(candidate1, candidate2);

        try {
            @SuppressWarnings("unchecked")
            final Tuple<String> tuple = candidate.adaptTo(Tuple.class);
            fail("Should fail here");
        } catch (final Exception e) {
            assertEquals(
                    "Type mismatch: class ch.ethz.seb.sebserver.gbl.util.Tuple3 to class ch.ethz.seb.sebserver.gbl.util.Tuple",
                    e.getMessage());
        }
    }

    @Test
    public void test2() {
        final Tuple<String> candidate = new Tuple<>("1", "2");
        assertEquals("(1, 2)", candidate.toString());
        assertEquals("1", candidate.get_1());
        assertEquals("2", candidate.get_2());

        final Tuple<String> candidate1 = new Tuple<>("1", "2");
        final Tuple<String> candidate2 = new Tuple3<>("4", "5", "6");
        assertEquals(candidate, candidate1);
        assertNotEquals(candidate1, candidate2);

        @SuppressWarnings("unchecked")
        final Tuple3<String> tuple = candidate2.adaptTo(Tuple3.class);
        assertEquals("(4, 5, 6)", tuple.toString());

    }

}
