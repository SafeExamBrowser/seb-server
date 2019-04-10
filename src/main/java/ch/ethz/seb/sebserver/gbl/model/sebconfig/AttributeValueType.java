/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

/** Enumeration of known SEB configuration attribute value types */
public enum AttributeValueType {
    /** Not defined or unknown */
    NONE,
    /** Short text (255 chars) */
    TEXT,
    /** Large texts MEDIUMTEXT */
    LARGE_TEXT,
    /** Base 64 encoded binary data */
    BASE64_BINARY,
    /** A list of single values of the same type */
    LIST,
    /** A composite of different typed values like a Map or Dictionary */
    COMPOSITE,
    /** A list of composites of the same type like a Table */
    COMPOSITE_LIST
}
