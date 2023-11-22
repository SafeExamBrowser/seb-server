/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface SEBClientSessionService extends ExamUpdateTask, SessionUpdateTask {

    @Override
    default int examUpdateTaskProcessingOrder() {
        return 10;
    }

    @Override
    default void processExamUpdateTask() {
        cleanupInstructions();
    }

    @Override
    default int sessionUpdateTaskProcessingOrder() {
        return 0;
    }

    @Override
    default void processSessionUpdateTask() {
        updatePingEvents();
        updateASKGrants();
    }

    /** Used to check current cached ping times of all running connections and
     * if a ping time is overflowing, creating a ping overflow event or if an
     * overflowed ping is back to normal, a ping back to normal event. */
    void updatePingEvents();

    /** Used to update the app signature key grants of all active SEB connections that miss a grant */
    void updateASKGrants();

    /** Used to cleanup old instructions from the persistent storage */
    void cleanupInstructions();

    /** Notify a ping for a certain client connection.
     *
     * @param connectionToken the connection token
     * @param pingNumber the ping number
     * @param instructionConfirm instruction confirm sent by the SEB client or null
     * @return SEB instruction if available */
    String notifyPing(String connectionToken, int pingNumber, String instructionConfirm);

    /** Notify a SEB client event for live indication and storing to database.
     *
     * @param connectionToken the connection token
     * @param jsonBody The SEB client event JSON data */
    void notifyClientEvent(String connectionToken, String jsonBody);

    /** This is used to confirm SEB instructions that must be confirmed by the SEB client.
     *
     * @param connectionToken The SEB client connection token
     * @param instructionConfirm the instruction confirm identifier */
    void confirmInstructionDone(String connectionToken, String instructionConfirm);

    /** Use this to get the get the specific indicator values for a given client connection.
     *
     * @param clientConnection The client connection values
     * @return Result refer to ClientConnectionData instance containing the given clientConnection plus the indicator
     *         values or to an error when happened */
    Result<ClientConnectionData> getIndicatorValues(final ClientConnection clientConnection);

}
