/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;

public class PageMessageException extends RuntimeException {

    private static final long serialVersionUID = -6967378384991469166L;

    private final LocTextKey textKey;

    public PageMessageException(final String message, final Throwable cause) {
        super(message, cause);
        this.textKey = new LocTextKey(message);
    }

    public PageMessageException(final String message) {
        super(message);
        this.textKey = new LocTextKey(message);
    }

    public PageMessageException(final LocTextKey message, final Throwable cause) {
        super(message.name, cause);
        this.textKey = message;
    }

    public PageMessageException(final LocTextKey message) {
        super(message.name);
        this.textKey = message;
    }

    public LocTextKey getMessageKey() {
        return this.textKey;
    }

}
