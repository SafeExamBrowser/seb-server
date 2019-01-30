/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.i18n;

import java.util.Arrays;

public class LocTextKey {

    public final String name;
    public final Object[] args;

    public LocTextKey(final String name) {
        this.name = name;
        this.args = null;
    }

    public LocTextKey(final String name, final Object... args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
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
        final LocTextKey other = (LocTextKey) obj;
        if (this.name == null) {
            if (other.name != null)
                return false;
        } else if (!this.name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "LocTextKey [name=" + this.name + ", args=" + Arrays.toString(this.args) + "]";
    }

}
