/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Tuple3Test {

    @Test
    public void test() {
        final Tuple3<String> candidate = new Tuple3<>("1", "2", "3");
        assertEquals("(1, 2, 3)", candidate.toString());
        assertEquals("1", candidate.get_1());
        assertEquals("2", candidate.get_2());
        assertEquals("3", candidate.get_3());
    }

}
