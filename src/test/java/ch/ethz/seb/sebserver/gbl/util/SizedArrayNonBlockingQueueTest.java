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

public class SizedArrayNonBlockingQueueTest {

    @Test
    public void test() {
        final SizedArrayNonBlockingQueue<String> candidate = new SizedArrayNonBlockingQueue<>(3);

        candidate.add("1");
        assertEquals("[1]", candidate.toString());

        candidate.add("2");
        assertEquals("[1, 2]", candidate.toString());

        candidate.add("3");
        assertEquals("[1, 2, 3]", candidate.toString());

        candidate.add("4");
        assertEquals("[2, 3, 4]", candidate.toString());

    }

}
