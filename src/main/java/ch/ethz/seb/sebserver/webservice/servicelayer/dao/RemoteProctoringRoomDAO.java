/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.NewRoom;

/** Data access for RemoteProctoringRoom domain objects. */
public interface RemoteProctoringRoomDAO {

    /** Indicates if there is already any proctoring service in use for the specified exam
     *
     * @param examId the exam identifier
     * @return Result refer to the indication or to an error when happened. */
    Result<Boolean> isServiceInUse(Long examId);

    /** Get all collecting room records that exists for a given exam.
     *
     * @param examId the exam identifier
     * @return Result refer to the collection of all collecting room records for the given exam or to an error when
     *         happened */
    Result<Collection<RemoteProctoringRoom>> getCollectingRooms(Long examId);

    /** Get all room records that exists for a given exam.
     *
     * @param examId the exam identifier
     * @return Result refer to the collection of all room records for the given exam or to an error when
     *         happened */
    Result<Collection<RemoteProctoringRoom>> getRooms(Long examId);

    /** The the room record with given identifier (PK).
     *
     * @param roomId the room record identifier
     * @return Result refer to the room record or to an error when happened */
    Result<RemoteProctoringRoom> getRoom(Long roomId);

    /** Get the room record with specified name for a given exam.
     *
     * @param examId the exam identifier
     * @param roomName the name of the room
     * @return Result refer to the room record or to an error when happened */
    Result<RemoteProctoringRoom> getRoom(Long examId, String roomName);

    /** Get the room name for the room with given identifier.
     *
     * @param roomId the room record identifier (PK)
     * @return Result refer to the rooms name or to an error when happened */
    Result<String> getRoomName(Long roomId);

    /** Create the town hall room for a given exam. Uses the given room data to create the record
     *
     * @param examId the exam identifier
     * @param room the room data to save to record
     * @return Result refer to the created room record or to an error when happened */
    Result<RemoteProctoringRoom> createTownhallRoom(Long examId, NewRoom room);

    boolean isTownhallRoomActive(Long examId);

    /** Get the town hall room record for a given exam if existing.
     *
     * @param examId the exam identifier
     * @return Result refer to the town-hall room record or to an error when happened. */
    Result<RemoteProctoringRoom> getTownhallRoom(Long examId);

    /** Delete the town-hall room record for a given exam.
     *
     * @param examId the exam identifier
     * @return Result refer to the entity key of the former town-hall room record or to an error when happened */
    Result<EntityKey> deleteTownhallRoom(Long examId);

    /** Create a break-out room for a given exam. Uses the given room data to create the record
     *
     * @param examId the exam identifier
     * @param room the room data to save to record
     * @param connectionTokens comma separated list of SEB client connection tokens that joins the new break-out room
     * @return Result refer to the created break-out room record or to an error when happened */
    Result<RemoteProctoringRoom> createBreakOutRoom(Long examId, NewRoom room, String connectionTokens);

    /** Delete the room record with given id.
     *
     * @param roomId the room identifier (PK)
     * @return Result refer to the entity key of the former room record or to an error when happened */
    Result<EntityKey> deleteRoom(Long roomId);

    /** Delete all room records for a given exam.
     *
     * @param examId the exam identifier
     * @return Result refer to a collection of entity keys for all delete room records or to an error when happened */
    Result<Collection<EntityKey>> deleteRooms(Long examId);

    /** This reserves a place in a collecting room on a given exam.
     * Creates a new collecting room record depending on the roomMaxSize and the how many connection
     * already have been collected within the actual collecting room.
     *
     * @param examId the exam identifier
     * @param roomMaxSize the maximum size of connection collected in one collecting room
     * @param newRoomFunction Function to create data for a new collecting room if needed.
     * @return Result refer to the collecting room record of place or to an error when happened */
    Result<RemoteProctoringRoom> reservePlaceInCollectingRoom(
            Long examId,
            int roomMaxSize,
            Function<Long, Result<NewRoom>> newRoomFunction);

    /** Releases a place in the actual collecting room for a given exam.
     *
     * @param examId the exam identifier
     * @param roomId the room record identifier (PK)
     * @return Result refer to the actual collecting room record or to an error when happened */
    Result<RemoteProctoringRoom> releasePlaceInCollectingRoom(Long examId, Long roomId);

    /** Get currently active break-out rooms for given connectionToken
     *
     * @param connectionTokens The connection token of the client connection
     * @return Result refer to active break-out rooms or to an error when happened */
    Result<Collection<RemoteProctoringRoom>> getBreakoutRooms(String connectionToken);

    /** Get a list of client connection tokens of connections that currently are in
     * break-out rooms, including the town-hall room
     *
     * @param examId The exam identifier of the connection
     * @return Result refer to active break-out rooms or to an error when happened */
    Result<Collection<String>> getConnectionsInBreakoutRooms(Long examId);

    /** Mark a specified collecting room as opened or closed by a proctor.
     *
     * @param roomId The collecting room identifier
     * @param isOpen mark open or not */
    void setCollectingRoomOpenFlag(Long roomId, boolean isOpen);

    /** Use this to update the current room size of for a proctoring collecting room
     * by its real number of attached SEB connections. This can be used on error case to
     * recover and set the re calc the number of participants in a room
     *
     * @param remoteProctoringRoomId The proctoring room identifier
     * @return The newly calculated number of participants in the room. */
    Result<Long> updateRoomSize(Long remoteProctoringRoomId);

}
