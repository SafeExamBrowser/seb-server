/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import java.util.Objects;

/** A tuple of three elements of the same type */
public class Tuple3<T> extends Tuple<T> {

    /** The third element of the tuple */
    public final T _3;

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        final Tuple3<?> tuple3 = (Tuple3<?>) o;
        return Objects.equals(this._3, tuple3._3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this._3);
    }

    public Tuple3(final T _1, final T _2, final T _3) {
        super(_1, _2);
        this._3 = _3;
    }

    public T get_3() {
        return this._3;
    }

    @Override
    public String toString() {
        return "(" + this._1 + ", " + this._2 + ", " + this._3 + ")";
    }
}
