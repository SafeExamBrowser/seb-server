/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import static ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeValueType.*;

/** Enumeration of known SEB configuration attribute types */
public enum AttributeType {
    /** Single read-only label */
    LABEL(NONE),
    /** Single lined text value */
    TEXT_FIELD(TEXT),
    /** Password (Base 16 encoded SHA256)
     * Displayed as two text input fields (confirm) */
    PASSWORD_FIELD(TEXT),
    /** Multiple lined text value */
    TEXT_AREA(TEXT),
    /** Check Box or boolean type */
    CHECKBOX(TEXT),

    SLIDER(TEXT),

    /** Integer number type */
    INTEGER(TEXT),
    /** Decimal number type */
    DECIMAL(TEXT),
    /** Single selection type (Drop-down) */
    SINGLE_SELECTION(TEXT),

    COMBO_SELECTION(TEXT),
    /** Radio selection type (like single selection but with check-boxes) */
    RADIO_SELECTION(TEXT),
    /** Multiple selection type */
    MULTI_SELECTION(LIST),
    /** Multiple selection with checkbox type */
    MULTI_CHECKBOX_SELECTION(LIST),

    FILE_UPLOAD(BASE64_BINARY),

    /** Table type is a list of a composite of single types */
    TABLE(COMPOSITE_LIST),

    INLINE_TABLE(COMPOSITE_LIST),

    COMPOSITE_TABLE(COMPOSITE);

    public final AttributeValueType attributeValueType;

    AttributeType(final AttributeValueType attributeValueType) {
        this.attributeValueType = attributeValueType;
    }
}
