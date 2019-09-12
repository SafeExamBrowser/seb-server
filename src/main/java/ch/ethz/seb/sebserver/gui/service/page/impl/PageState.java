/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import ch.ethz.seb.sebserver.gui.service.page.Activity;
import ch.ethz.seb.sebserver.gui.service.page.PageStateDefinition;
import ch.ethz.seb.sebserver.gui.service.page.PageStateDefinition.Type;

public final class PageState {

    public final PageStateDefinition definition;
    public final PageAction gotoAction;

    PageState(final PageStateDefinition definition, final PageAction goToAction) {
        super();
        this.definition = definition;
        this.gotoAction = goToAction;
    }

    public PageStateDefinition getDefinition() {
        return this.definition;
    }

    public PageAction getGotoAction() {
        return this.gotoAction;
    }

    public Type type() {
        if (this.definition == null) {
            return null;
        }

        return this.definition.type();
    }

    public Activity activityAnchor() {
        if (this.definition == null) {
            return null;
        }

        return this.definition.activityAnchor();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("PageState [definition=");
        builder.append(this.definition);
        builder.append(", gotoAction=");
        builder.append(this.gotoAction);
        builder.append("]");
        return builder.toString();
    }

}
