/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.event;

import ch.ethz.seb.sebserver.gui.service.page.action.Action;

/** This Event is used to propagate a user-action to the GUI system.
 * Potentially every component can listen to an Event and react on the user-action */
public final class ActionEvent implements PageEvent {

    public final Action action;
    public final boolean activity;

    public ActionEvent(final Action action, final boolean activity) {
        super();
        this.action = action;
        this.activity = activity;
    }

}
