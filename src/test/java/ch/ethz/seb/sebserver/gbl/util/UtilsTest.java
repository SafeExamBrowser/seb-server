/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

public class UtilsTest {

    @Test
    public void testGetSingle() {
        final Collection<String> singleCollection = Utils.immutableCollectionOf("ONE");
        final Collection<String> collection = Utils.immutableCollectionOf("ONE", "TWO");

        final Result<String> r1 = Utils.getSingle(null);
        final Result<String> r2 = Utils.getSingle(Collections.emptyList());
        final Result<String> r3 = Utils.getSingle(singleCollection);
        final Result<String> r4 = Utils.getSingle(collection);

        assertTrue(r1.hasError());
        assertTrue(r2.hasError());
        assertFalse(r3.hasError());
        assertTrue(r4.hasError());

        assertEquals("ONE", r3.get());
        assertEquals("Collection has no or more then one element. Expected is exaclty one. Size: null",
                r1.getError().getMessage());
        assertEquals("Collection has no or more then one element. Expected is exaclty one. Size: 2",
                r4.getError().getMessage());
    }

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
