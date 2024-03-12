/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.event;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;

public class ActionActivationEvent implements PageEvent {

    public final boolean activation;
    public final Collection<ActionDefinition> actions;
    public final Tuple<ActionDefinition> decoration;

    public ActionActivationEvent(final boolean activation, final ActionDefinition... actions) {
        this.activation = activation;
        this.actions = Utils.immutableCollectionOf(actions);
        this.decoration = null;
    }

    public ActionActivationEvent(final boolean activation, final Collection<ActionDefinition> actions) {
        this.activation = activation;
        this.actions = Utils.immutableCollectionOf(actions);
        this.decoration = null;
    }

    public ActionActivationEvent(
            final boolean activation,
            final Tuple<ActionDefinition> decoration,
            final ActionDefinition... actions) {

        this.activation = activation;
        this.actions = Utils.immutableCollectionOf(actions);
        this.decoration = decoration;
    }

}
