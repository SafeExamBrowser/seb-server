/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.EventHandlingStrategy;

@Lazy
@Service
@WebServiceProfile
public class EventHandlingStrategyFactory {

    private final EventHandlingStrategy eventHandlingStrategy;

    protected EventHandlingStrategyFactory(
            final ApplicationContext applicationContext,
            @Value(EventHandlingStrategy.EVENT_CONSUMER_STRATEGY_VALUE_PROPERTY) final String nameProperty) {

        this.eventHandlingStrategy = applicationContext.getBean(
                nameProperty,
                EventHandlingStrategy.class);
        this.eventHandlingStrategy.enable();
    }

    public EventHandlingStrategy get() {
        return this.eventHandlingStrategy;
    }

}
