/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

public enum PageSortOrder {
    ASCENDING,
    DESCENDING;

    static String DESCENDING_PREFIX = "-";

    public String encode(final String sort) {
        return (this == DESCENDING) ? DESCENDING_PREFIX + sort : sort;
    }

    public static String decode(final String sort) {
        return (sort != null && sort.startsWith(DESCENDING_PREFIX))
                ? sort.substring(1)
                : sort;
    }

    public static PageSortOrder getSortOrder(final String encoded) {
        return (encoded != null && encoded.startsWith(DESCENDING_PREFIX))
                ? PageSortOrder.DESCENDING
                : PageSortOrder.ASCENDING;
    }
}