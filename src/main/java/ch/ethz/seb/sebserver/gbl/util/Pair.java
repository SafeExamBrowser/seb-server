/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

/** Simple data class that defines a pair (A and B) of same or different types. */
public class Pair<A, B> {

    /** The A instance of the pair */
    public final A a;
    /** The B instance of the pair */
    public final B b;

    public Pair(final A a, final B b) {
        super();
        this.a = a;
        this.b = b;
    }

}
