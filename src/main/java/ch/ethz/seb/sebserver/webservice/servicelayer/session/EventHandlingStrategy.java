/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.function.Consumer;

import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;

/** A exam session SEB client event handling strategy implements a certain strategy to
 * store ClientEvent that are coming in within the specified endpoint in height frequency. */
public interface EventHandlingStrategy extends Consumer<ClientEvent> {

    String EVENT_CONSUMER_STRATEGY_CONFIG_PROPERTY_KEY = "sebserver.webservice.api.exam.event-handling-strategy";
    String EVENT_CONSUMER_STRATEGY_SINGLE_EVENT_STORE = "SINGLE_EVENT_STORE_STRATEGY";
    String EVENT_CONSUMER_STRATEGY_ASYNC_BATCH_STORE = "ASYNC_BATCH_STORE_STRATEGY";

    /** This enables a certain EventHandlingStrategy to be the executing and EventHandlingStrategy
     * and will be re-initialized on server restart */
    void enable();

}
