/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public class FieldValidationError {

    public final String messageCode;
    public final String domainName;
    public final String fieldName;
    public final String errorType;
    public final Collection<String> attributes;

    public FieldValidationError(final APIMessage apiMessage) {
        this(
                apiMessage.messageCode,
                apiMessage.attributes.toArray(new String[0]));
    }

    public FieldValidationError(
            final String messageCode,
            final String[] attributes) {

        this.messageCode = messageCode;
        this.attributes = Utils.immutableCollectionOf(attributes);

        this.domainName = (attributes != null && attributes.length > 0) ? attributes[0] : null;
        this.fieldName = (attributes != null && attributes.length > 1) ? attributes[1] : null;
        this.errorType = (attributes != null && attributes.length > 2) ? attributes[2] : null;
    }

    public String[] getAttributes() {
        if (this.attributes == null) {
            return new String[0];
        }

        return this.attributes.toArray(new String[0]);
    }

}
