/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.event;

import java.util.function.Consumer;

import org.eclipse.swt.widgets.TreeItem;

import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;

/** This action is used to publish an Action to the Action-Pane for a specified context.
 * The ActionPane is listening to this events and render specified actions on notify */
public class ActionPublishEvent implements PageEvent {

    public final boolean active;
    public final PageAction action;
    public final Consumer<TreeItem> actionConsumer;

    public ActionPublishEvent(final PageAction action, final boolean active) {
        this.action = action;
        this.active = active;
        this.actionConsumer = null;
    }

    public ActionPublishEvent(final PageAction action, final Consumer<TreeItem> actionConsumer) {
        this.action = action;
        this.active = true;
        this.actionConsumer = actionConsumer;
    }

}
