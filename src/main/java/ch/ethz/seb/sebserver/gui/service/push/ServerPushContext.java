/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.push;

import java.util.function.Predicate;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/** ServerPushContext defines the state of a server push session.
 *
 * @author anhefti */
public final class ServerPushContext {

    private final Composite anchor;
    private final Predicate<ServerPushContext> runAgain;

    public ServerPushContext(final Composite anchor) {
        this(anchor, context -> false);
    }

    public ServerPushContext(
            final Composite anchor,
            final Predicate<ServerPushContext> runAgain) {

        this.anchor = anchor;
        this.runAgain = runAgain;
    }

    public boolean runAgain() {
        return this.runAgain.test(this);
    }

    public boolean isDisposed() {
        return this.anchor.isDisposed();
    }

    public Display getDisplay() {
        return this.anchor.getDisplay();
    }

    public Composite getAnchor() {
        return this.anchor;
    }

    public void layout() {
        this.anchor.pack();
        this.anchor.layout();
        this.anchor.getParent().layout(true, true);
    }

}
