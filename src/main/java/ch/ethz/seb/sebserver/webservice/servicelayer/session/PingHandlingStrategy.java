/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

/** Defines a SEB Client connection ping handling strategy.
 * The strategy may be different in case of stand-alone or distributed SEB Server setup. */
public interface PingHandlingStrategy {

    /** Initializes the ping handling for a new SEB Connection.
     *
     * @param connectionId the SEB Connection identifier
     * @param connectionToken the SEB Connection token */
    void initForConnection(Long connectionId, String connectionToken);

    /** Used to notify a ping from a SEB Client connection.
     *
     * @param connectionToken the SEB Client connection token
     * @param timestamp the ping time-stamp
     * @param pingNumber the ping number */
    void notifyPing(final String connectionToken, final long timestamp, final int pingNumber);

}
