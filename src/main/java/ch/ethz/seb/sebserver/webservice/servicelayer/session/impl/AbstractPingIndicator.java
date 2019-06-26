/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;

public abstract class AbstractPingIndicator extends AbstractClientIndicator {

    private final Set<EventType> EMPTY_SET = Collections.unmodifiableSet(EnumSet.noneOf(EventType.class));

    protected int pingCount = 0;
    protected int pingNumber = 0;

    public void notifyPing(final long timestamp, final int pingNumber) {
        super.currentValue = timestamp;
        this.pingCount++;
        this.pingNumber = pingNumber;
    }

    @Override
    public double computeValueAt(final long timestamp) {
        return timestamp;
    }

    @Override
    public Set<EventType> observedEvents() {
        return this.EMPTY_SET;
    }

}
