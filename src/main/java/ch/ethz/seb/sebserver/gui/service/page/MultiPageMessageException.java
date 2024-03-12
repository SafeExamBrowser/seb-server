/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;

public class MultiPageMessageException extends PageMessageException {

    private static final long serialVersionUID = -501071029069528332L;

    private final Collection<Exception> errors;

    public MultiPageMessageException(final LocTextKey message, final Collection<Exception> errors) {
        super(message, (errors != null && !errors.isEmpty()) ? errors.iterator().next() : null);
        this.errors = Utils.immutableCollectionOf(errors);
    }

    public Collection<Exception> getErrors() {
        return this.errors;
    }

}
