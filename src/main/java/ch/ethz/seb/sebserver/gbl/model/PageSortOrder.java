/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

/** Defines page sort order ASCENDING or DESCENDING and the corresponding SEB Server API parameter encoding
 *  for the sort parameter. */
public enum PageSortOrder {
    ASCENDING,
    DESCENDING;

    static String DESCENDING_PREFIX = "-";

    /** Use this to encode a given sort parameter name for SEB Server API encoding.
     *  This adds just a '-' as prefix to the sort parameter value if DESCENDING
     *
     * @param sort The sort parameter value
     * @return The encoded sort parameter value */
    public String encode(final String sort) {
        return (this == DESCENDING) ? DESCENDING_PREFIX + sort : sort;
    }

    /** Use this to decode a given sort parameter name for SEB Server API encoding.
     *  This removes just a prefixing '-' from sort parameter value if DESCENDING
     *
     * @param sort The sort parameter value
     * @return The encoded sort parameter value */
    public static String decode(final String sort) {
        return (sort != null && sort.startsWith(DESCENDING_PREFIX))
                ? sort.substring(1)
                : sort;
    }

    /** Use this to get the sort order from a  SEB Server API encoded sort parameter value.
     *
     * @param encoded SEB Server API encoded sort parameter value
     * @return The sort order from a  SEB Server API encoded sort parameter value. */
    public static PageSortOrder getSortOrder(final String encoded) {
        return (encoded != null && encoded.startsWith(DESCENDING_PREFIX))
                ? PageSortOrder.DESCENDING
                : PageSortOrder.ASCENDING;
    }
}