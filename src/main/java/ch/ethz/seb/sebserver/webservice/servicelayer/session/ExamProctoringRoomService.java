/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Collection;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Defines functionality to deal with proctoring rooms in a generic way (independent from meeting service) */
public interface ExamProctoringRoomService {

    /** Get all existing default proctoring rooms of an exam.
     *
     * @param examId The exam identifier
     * @return Result refer to the list of default proctoring rooms for the specified Exam */
    Result<Collection<RemoteProctoringRoom>> getProctoringCollectingRooms(Long examId);

    /** Get a collection of all ClientConnection that are currently connected to a specified room.
     *
     * @param roomId The room identifier
     * @return Result refer to the resulting collection of ClientConnection or to an error when happened */
    Result<Collection<ClientConnection>> getRoomConnections(Long roomId);

    /** Get a collection of all ClientConnection that are currently connected to a specified collecting room.
     *
     * @param examId The exam identifier of the room
     * @param roomName The room name
     * @return Result refer to the resulting collection of ClientConnection or to an error when happened */
    Result<Collection<ClientConnection>> getCollectingRoomConnections(Long examId, String roomName);

    /** This is internally used to update client connections that are flagged for updating
     * the proctoring room assignment.
     * This attaches or detaches client connections from or to proctoring rooms of an exam in one batch.
     * New client connections that are coming in and are established only mark itself for
     * proctoring room update if proctoring is enabled for the specified exam. This batch processing
     * then makes the update synchronously to keep track on room creation and naming
     *
     * If for a specified exam the town-hall room is active incoming client connection are instructed to
     * join the town-hall room. If not, incoming client connection are instructed to join a collecting room. */
    void updateProctoringCollectingRooms();

    /** This is internally called when an exam ends.
     * Dispose all rooms of an exam and deletes the stored data of the rooms.
     * This is checking first if the exam has finished and if not, prevent the deletion of room data.
     *
     * @param exam the exam
     * @return Result refer to the given exam or to an error when happened */
    Result<Exam> disposeRoomsForExam(Exam exam);

    /** Indicates whether the town-hall for given exam is active or not
     *
     * @param examId the exam identifier
     * @return true if the town-hall for given exam is currently actice */
    boolean isTownhallRoomActive(final Long examId);

    /** This creates a town-hall room for a specific exam. The exam must be active and running
     * and there must be no other town-hall room already be active. An unique room name will be
     * created and returned.
     *
     * @param examId The exam identifier
     * @return Result referencing the created room name or to an error when happened */
    Result<ProctoringRoomConnection> openTownhallRoom(Long examId, String subject);

    /** Get the RemoteProctoringRoom data for the town-hall of specified exam.
     * If the town-hall is not active for the specified exam, returns the RemoteProctoringRoom.NULL_ROOM
     *
     * @param examId the exam identifier
     * @return Result refer to the RemoteProctoringRoom data or to an error when happened */
    Result<RemoteProctoringRoom> getTownhallRoomData(final Long examId);

    /** Used to close a active town-hall for given exam.
     *
     * @param examId The exam identifier
     * @return Result refer to the room key of the closed town-hall or to an error when happened. */
    Result<EntityKey> closeTownhallRoom(Long examId);

    /** Used to create a break out room for all active SEB clients given by the connectionTokens.
     * This first notifies the underling proctoring specific service layer on room creation that will create a room
     * on the meeting service if necessary. Then creating the room internally for holding data and tracking the new
     * break out room.
     * When the room is created sends a join instruction to all given SEB connections to join the room immediately.
     * Returns the ProctoringRoomConnection data that also defines the room connection for a proctor.
     *
     * @param examId The exam identifier
     * @param subject the subject for the new break out room
     * @param connectionTokens a comma separated list of SEB client connection tokens used to send the join instruction
     *            to
     * @return Result refer to ProctoringRoomConnection data or to an error when happened */
    Result<ProctoringRoomConnection> createBreakOutRoom(Long examId, String subject, String connectionTokens);

    /** Close the proctoring room with specified name.
     * This fist notifies the underling proctoring specific service layer on room closing that will
     * close the room on the meeting service if needed. Then send reset configuration instructions to all SEB
     * client connections that are attached to the room. Then closing the room internally if needed and
     * sending a join instruction to all invovled SEB clients to rejoin the collecting room.
     *
     * @param examId The exam identifier
     * @param roomName The room name
     * @return Result refer to an empty value or to an error when happened */
    Result<Void> closeProctoringRoom(Long examId, String roomName);

    /** Sends a reconfiguration instruction with defined attributes to all involved and active SEB client connections.
     *
     * @param examId The exam identifier
     * @param roomName The room name
     * @param attributes the reconfiguration attributes
     * @return Result refer to an empty value or to an error when happened */
    Result<Void> sendReconfigurationInstructions(Long examId, String roomName, Map<String, String> attributes);

    /** Notifies that a specified proctoring room has been opened by a proctor.
     *
     * This can be used to do instruct connection SEB clients of the room to do some initial actions,
     * sending join instruction for the room to the SEB clients for example.
     *
     * @param examId The exam identifier of the proctoring room
     * @param roomName The name of the proctoring room
     * @return Result refer to void or to an error when happened */
    Result<Void> notifyRoomOpened(Long examId, String roomName);

}
