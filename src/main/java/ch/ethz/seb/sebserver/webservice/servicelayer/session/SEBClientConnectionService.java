/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.security.Principal;
import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Service interface defining functionality to handle SEB client connections on running exams. */
public interface SEBClientConnectionService {

    /** Use this to get the underling ExamSessionService
     *
     * @return get the underling ExamSessionService */
    ExamSessionService getExamSessionService();

    /** If a SEB client connects to the SEB Server the first time for a exam session,
     * this is used to create a ClientConnection for this connection attempt.
     * So this starts the SEB Client - SEB Server handshake.
     * <p>
     * The examId is not mandatory here an can still be null if at this time
     * no exam was selected.
     * <p>
     * A connection-token to identify the connection is generated and stored within the
     * returned ClientConnection.
     *
     * @param principal the client connection Principal from REST controller interface
     * @param institutionId The institution identifier
     * @param clientAddress The clients remote IP address (optional)
     * @param sebVersion the name tag of the SEB version (optional)
     * @param sebOsName the operating system tag name where SEB runs on (optional)
     * @param sebMachineName the machine/device name where the SEB runs on (optional)
     * @param examId the exam identifier (optional)
     * @param clientId The client identifier sent by the SEB client (used to identify VDI client pair)
     * @return A Result refer to the newly created ClientConnection in state: CONNECTION_REQUESTED, or refer to an error
     *         if happened */
    Result<ClientConnection> createClientConnection(
            Principal principal,
            Long institutionId,
            String clientAddress,
            String sebVersion,
            String sebOsName,
            String sebMachineName,
            Long examId,
            String clientId);

    /** This updates an already existing ClientConnection with the given connectionToken.
     * <p>
     * If a clientAddress is given and it differs from the existing clientAddress and the there is
     * an exam mapping to an exam of type VDI, the given clientAddress is used to update the virtualClientAddress
     * of the ClientConnection.
     * <p>
     * If an examId is given this is used to update the ClientConnection
     * <p>
     * If a userSessionId is given this is used to update the ClientConnection
     *
     * @param connectionToken The connection-token that was given on ClientConnection creation and that identifies the
     *            connection
     * @param institutionId The institution identifier
     * @param clientAddress The clients remote IP address
     * @param sebVersion the name tag of the SEB version (optional)
     * @param sebOsName the operating system tag name where SEB runs on (optional)
     * @param sebMachineName the machine/device name where the SEB runs on (optional)
     * @param examId The exam identifier
     * @param userSessionId The user session identifier of the users http-session with the LMS
     * @param clientId The client identifier sent by the SEB client (used to identify VDI client pair)
     * @return A Result refer to the updated ClientConnection instance, or refer to an error if happened */
    Result<ClientConnection> updateClientConnection(
            String connectionToken,
            Long institutionId,
            Long examId,
            String clientAddress,
            String sebVersion,
            String sebOsName,
            String sebMachineName,
            String userSessionId,
            String clientId);

    /** This is used to establish a already created ClientConnection and set it to sate: ESTABLISHED
     * The connectionToken identifies the ClientConnection and the given clientAddress must match with
     * the clientAddress of the already created ClientConnection in state CONNECTION_REQUESTED.
     * <p>
     * This may not be the case for VDI exams. In case of VDI exams the different clientAddress is stored
     * in the virtualClientAddress field of the ClientConnection.
     * <p>
     * The examId may also be null here if the examId is already known within the existing ClientConnection.
     * If not, an error is thrown and send to the calling SEB Client.
     * <p>
     * If a userSessionId is provided within the establish request, this is also stored within the ClientConnection.
     *
     * @param connectionToken The connection-token that was given on ClientConnection creation and that identifies the
     *            connection
     * @param institutionId The institution identifier
     * @param examId The exam identifier (may be null of already known)
     * @param clientAddress The clients remote IP address
     * @param sebVersion the name tag of the SEB version (optional)
     * @param sebOsName the operating system tag name where SEB runs on (optional)
     * @param sebMachineName the machine/device name where the SEB runs on (optional)
     * @param userSessionId The user session identifier of the users http-session with the LMS
     * @param clientId The client identifier sent by the SEB client (used to identify VDI client pair)
     * @return A Result refer to the established ClientConnection instance, or refer to an error if happened */
    Result<ClientConnection> establishClientConnection(
            String connectionToken,
            Long institutionId,
            Long examId,
            String clientAddress,
            String sebVersion,
            String sebOsName,
            String sebMachineName,
            String userSessionId,
            String clientId);

    /** This is used to regular close an established ClientConnection from SEB Client side.
     * <p>
     * This will save the existing established ClientConnection in new CLOSED state and flush all caches.
     *
     * @param connectionToken The connection-token that was given on ClientConnection creation and that identifies the
     *            connection
     * @param institutionId institution identifier
     * @return A Result refer to the closed ClientConnection instance, or refer to an error if happened */
    Result<ClientConnection> closeConnection(
            String connectionToken,
            Long institutionId,
            String clientAddress);

    /** This is used to disable a undefined or requested ClientConnection attempt from the SEB Server side
     * <p>
     * This will save the existing ClientConnection that is in UNDEFINED or REQUESTED state, in new DISABLED state and
     * flush all caches.
     *
     * @param connectionToken The connection-token that was given on ClientConnection creation and that identifies the
     *            connection
     * @param institutionId institution identifier
     * @return A Result refer to the closed ClientConnection instance, or refer to an error if happened */
    Result<ClientConnection> disableConnection(String connectionToken, Long institutionId);

    /** This is used to disable multiple undefined or requested ClientConnection attempt from the SEB Server side
     * <p>
     * This will save the existing ClientConnections that are in UNDEFINED or REQUESTED state, in new DISABLED state and
     * flush caches.
     *
     * @param connectionTokens String array of connection tokens of connections to disable
     * @param institutionId institution identifier
     * @return A Result refer to a list of EntityKey of the closed ClientConnection instances, or refer to an error if
     *         happened */
    Result<Collection<EntityKey>> disableConnections(final String[] connectionTokens, final Long institutionId);

    /** Used to check current cached ping times of all running connections and
     * if a ping time is overflowing, creating a ping overflow event or if an
     * overflowed ping is back to normal, a ping back to normal event. */
    void updatePingEvents();

    /** Used to cleanup old instructions from the persistent storage */
    void cleanupInstructions();

    /** Notify a ping for a certain client connection.
     *
     * @param connectionToken the connection token
     * @param timestamp the ping time-stamp
     * @param pingNumber the ping number
     * @param instructionConfirm instruction confirm sent by the SEB client or null
     * @return SEB instruction if available */
    String notifyPing(String connectionToken, long timestamp, int pingNumber, String instructionConfirm);

    /** Notify a SEB client event for live indication and storing to database.
     *
     * @param connectionToken the connection token
     * @param event The SEB client event data */
    void notifyClientEvent(String connectionToken, final ClientEvent event);

    /** This is used to confirm SEB instructions that must be confirmed by the SEB client.
     *
     * @param connectionToken The SEB client connection token
     * @param instructionConfirm the instruction confirm identifier */
    void confirmInstructionDone(String connectionToken, String instructionConfirm);

}
