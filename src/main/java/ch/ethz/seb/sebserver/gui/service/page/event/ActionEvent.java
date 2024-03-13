/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.event;

import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;

/** This Event is used to propagate a user-action to the GUI system.
 * Potentially every component can listen to an Event and react on the user-action */
public final class ActionEvent implements PageEvent {

    public final PageAction action;

    public ActionEvent(final PageAction action) {
        super();
        this.action = action;
    }

}
