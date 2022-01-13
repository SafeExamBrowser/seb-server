/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;

/** A client indicator is a indicator value holder for a specific Indicator
 * on a running client connection.
 * A client indicator can be used to verify a indicator value at a specific time of or
 * a client indicator can be used for in memory caching of the current value of the
 * indicator for a defined client connection. */
public interface ClientIndicator extends IndicatorValue {

    /** This is used to initialize a ClientIndicator.
     *
     * @param indicatorDefinition The indicator definition that defines type and thresholds of the indicator
     * @param connectionId the connection identifier to that this ClientIndicator is associated to
     * @param active indicates whether the connection is still an a active state or not
     * @param cachingEnabled defines whether indicator value caching is enabled or not. */
    void init(Indicator indicatorDefinition, Long connectionId, boolean active, boolean cachingEnabled);

    /** get the indicator type */
    @JsonIgnore
    IndicatorType getType();

    /** Get the exam identifier of the client connection of this ClientIndicator
     *
     * @return the exam identifier of the client connection of this ClientIndicator */
    Long examId();

    /** Get the client connection identifier to that this ClientIndicator is associated to
     *
     * @return the client connection identifier to that this ClientIndicator is associated to */
    Long connectionId();

    /** This is mostly internally used to compute the value for a certain time in the past or for the present time
     * If caching is not enabled this is called on every value read access otherwise it is called only if the
     * the value is not cached already.
     *
     * @param timestamp The time on that the indicator value shall be computed.
     * @return The computed indicator value on certain time */
    double computeValueAt(long timestamp);

    /** Get a set of EventTypes where this ClientIndicator is interested in
     *
     * @return a set of EventTypes where this ClientIndicator is interested in */
    Set<EventType> observedEvents();

    /** This gets called on a value change e.g.: when a ClientEvent was received.
     * NOTE: that this is called only on the same machine (server-instance) on that the ClientEvent was received.
     *
     * @param event The ClientEvent instance */
    void notifyValueChange(ClientEvent event);

    /** This gets called on a value change e.g.: when a ClientEvent was received.
     * NOTE: that this is called only on the same machine (server-instance) on that the ClientEvent was received.
     *
     * @param clientEventRecord The ClientEventRecord instance */
    void notifyValueChange(ClientEventRecord clientEventRecord);

    /** This indicates if the indicator indicates an incident. This is the case if the actual indicator value
     * is above or below the max or min value defined by the indicator threshold settings.
     *
     * @return true if this indicator indicates an incident */
    boolean hasIncident();

}
