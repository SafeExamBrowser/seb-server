/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

/** A tuple of two elements of the same type */
public class Tuple<T> {

    /** The first element of the tuple */
    public final T _1;
    /** The second element of the tuple */
    public final T _2;

    public Tuple(final T _1, final T _2) {
        super();
        this._1 = _1;
        this._2 = _2;
    }

    public T get_1() {
        return this._1;
    }

    public T get_2() {
        return this._2;
    }

    @SuppressWarnings("unchecked")
    public <TT extends Tuple<T>> TT adaptTo(final Class<TT> type) {
        if (type.equals(this.getClass())) {
            return (TT) this;
        }

        throw new IllegalArgumentException("Type mismatch: " + this.getClass() + " to " + type);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this._1 == null) ? 0 : this._1.hashCode());
        result = prime * result + ((this._2 == null) ? 0 : this._2.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("rawtypes")
        final Tuple other = (Tuple) obj;
        if (this._1 == null) {
            if (other._1 != null)
                return false;
        } else if (!this._1.equals(other._1))
            return false;
        if (this._2 == null) {
            if (other._2 != null)
                return false;
        } else if (!this._2.equals(other._2))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "(" + this._1 + ", " + this._2 + ")";
    }

}
