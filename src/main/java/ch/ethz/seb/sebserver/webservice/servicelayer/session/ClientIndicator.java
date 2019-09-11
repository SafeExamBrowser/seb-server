/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValueHolder;

/** A client indicator is a indicator value holder for a specific Indicator
 * on a running client connection.
 * A client indicator can be used to verify a indicator value at a specific time of or
 * a client indicator can be used for in memory caching of the current value of the
 * indicator for a defined client connection. */
public interface ClientIndicator extends IndicatorValue {

    void init(Indicator indicatorDefinition, Long connectionId, boolean cachingEnabled);

    Long examId();

    Long connectionId();

    double computeValueAt(long timestamp);

    Set<EventType> observedEvents();

    void notifyValueChange(IndicatorValueHolder indicatorValueHolder);

}
