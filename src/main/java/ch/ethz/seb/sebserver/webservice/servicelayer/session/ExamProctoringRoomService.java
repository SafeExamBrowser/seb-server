/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.util.Result;

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

    /** Get a collection of all ClientConnection that are currently connected to a specified room.
     *
     * @param examId The exam identifier of the room
     * @param roomName The room name
     * @return Result refer to the resulting collection of ClientConnection or to an error when happened */
    Result<Collection<ClientConnection>> getRoomConnections(Long examId, String roomName);

    /** This is internally used to update client connections that are flagged for updating
     * the proctoring room assignment.
     * This attaches or detaches client connections from or to proctoring rooms of an exam in one batch.
     * New client connections that are coming in and are established only mark itself for
     * proctoring room update if proctoring is enabled for the specified exam. This batch processing
     * then makes the update synchronous to not create to to many rooms or several rooms with the same
     * name of an exam. */
    void updateProctoringCollectingRooms();

    /** This creates a town-hall room for a specific exam. The exam must be active and running
     * and there must be no other town-hall room already be active. An unique room name will be
     * created and returned.
     *
     * @param examId The exam identifier
     * @return Result referencing the created room name or to an error when happened */
    Result<RemoteProctoringRoom> createTownhallRoom(Long examId, String subject);

    Result<RemoteProctoringRoom> getTownhallRoomData(final Long examId);

    void disposeTownhallRoom(Long examId);

}
