/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ExamProctoringService {

    /** Get the proctoring server type of the specific implementation
     *
     * @return the proctoring service type of the specific implementation */
    ProctoringServerType getType();

    /** Use this to test the proctoring service settings against the remote proctoring server.
     *
     * @param examProctoring the settings to test
     * @return Result refer to true if the settings are correct and the proctoring server can be accessed. */
    Result<Boolean> testExamProctoring(final ProctoringServiceSettings examProctoring);

    /** Gets the room connection data for a certain room for the proctor.
     *
     * @param proctoringSettings the proctoring service settings
     * @param roomName the name of the room
     * @param subject the subject of the room
     * @return Result refer to the room connection data for the proctor to connect to specified room or to an error when
     *         happened */
    Result<ProctoringRoomConnection> getProctorRoomConnection(
            ProctoringServiceSettings proctoringSettings,
            String roomName,
            String subject);

    /** This instructs all sepcified SEB clients to join a defined room by creating a individual room access token
     * and join instruction for each client and put this instruction to the clients instruction queue.
     *
     * @param proctoringSettings The proctoring service settings
     * @param clientConnectionTokens A collection of SEB connection tokens. Only active SEB clients will get the
     *            instructions
     * @param roomName The name of the room to join
     * @param subject the subject of the room to join
     * @return Result refer to the room connection data for the proctor to join to room too or to an error when
     *         happened */
    Result<ProctoringRoomConnection> sendJoinRoomToClients(
            ProctoringServiceSettings proctoringSettings,
            Collection<String> clientConnectionTokens,
            String roomName,
            String subject);

    /** Sends instructions to join or rejoin the individual assigned collecting rooms of each involved SEB client.
     * Creates an individual join instruction for each involved client and put that to the clients instruction queue.
     *
     * INFO:
     * A collecting room is assigned to each SEB client connection while connecting to the SEB server and
     * each SEB client that has successfully connected to the SEB Server and is participating in an exam
     * with proctoring enabled, is assigned to a collecting room.
     *
     * @param proctoringSettings he proctoring service settings
     * @param clientConnectionTokens A collection of SEB connection tokens. Only active SEB clients will get the
     *            instructions
     * @return Empty Result that refers to an error when happened */
    Result<Void> sendJoinCollectingRoomToClients(
            ProctoringServiceSettings proctoringSettings,
            Collection<String> clientConnectionTokens);

    default String verifyRoomName(final String requestedRoomName, final String connectionToken) {
        if (StringUtils.isNotBlank(requestedRoomName)) {
            return requestedRoomName;
        }

        throw new RuntimeException("Test Why: " + connectionToken);
    }

}
