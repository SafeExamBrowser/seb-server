/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.activation;

import org.springframework.context.ApplicationEvent;

import ch.ethz.seb.sebserver.gbl.model.Entity;

public final class EntityActivationEvent extends ApplicationEvent {

    private static final long serialVersionUID = -6712364320755441148L;

    public final transient boolean activated;
    public final transient Entity entity;

    public EntityActivationEvent(final Entity source, final boolean activated) {
        super(source);
        this.entity = source;
        this.activated = activated;
    }

}
