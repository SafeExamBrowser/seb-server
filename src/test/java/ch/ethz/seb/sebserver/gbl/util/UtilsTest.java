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

}
