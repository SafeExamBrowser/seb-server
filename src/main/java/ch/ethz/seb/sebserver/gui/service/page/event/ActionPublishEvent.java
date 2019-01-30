/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.event;

import ch.ethz.seb.sebserver.gui.service.page.action.ActionDefinition;

public class ActionPublishEvent implements PageEvent {

    public final ActionDefinition actionDefinition;
    public final Runnable run;
    public final String confirmationMessage;
    public final String successMessage;

    public ActionPublishEvent(
            final ActionDefinition actionDefinition,
            final Runnable run) {

        this(actionDefinition, run, null, null);
    }

    public ActionPublishEvent(
            final ActionDefinition actionDefinition,
            final Runnable run,
            final String confirmationMessage) {

        this(actionDefinition, run, confirmationMessage, null);
    }

    public ActionPublishEvent(
            final ActionDefinition actionDefinition,
            final Runnable run,
            final String confirmationMessage,
            final String successMessage) {

        this.actionDefinition = actionDefinition;
        this.run = run;
        this.confirmationMessage = confirmationMessage;
        this.successMessage = successMessage;
    }

    @Override
    public String toString() {
        return "ActionPublishEvent [actionDefinition=" + this.actionDefinition + ", confirmationMessage="
                + this.confirmationMessage + ", successMessage=" + this.successMessage + "]";
    }

}
