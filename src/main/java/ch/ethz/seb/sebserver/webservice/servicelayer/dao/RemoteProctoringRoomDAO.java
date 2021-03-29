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

public interface RemoteProctoringRoomDAO {

    Result<Collection<RemoteProctoringRoom>> getCollectingRoomsForExam(Long examId);

    Result<RemoteProctoringRoom> getRoom(Long roomId);

    Result<String> getRoomName(Long roomId);

    boolean isTownhallRoomActive(Long examId);

    Result<RemoteProctoringRoom> getTownhallRoom(Long examId);

    Result<RemoteProctoringRoom> createTownhallRoom(Long examId, String subject);

    Result<RemoteProctoringRoom> saveRoom(final Long examId, RemoteProctoringRoom room);

    Result<EntityKey> deleteTownhallRoom(Long examId);

    Result<Collection<EntityKey>> deleteRooms(Long examId);

    Result<RemoteProctoringRoom> reservePlaceInCollectingRoom(
            Long examId,
            int roomMaxSize,
            Function<Long, String> newRoomNameFunction,
            Function<Long, String> newRommSubjectFunction);

    Result<RemoteProctoringRoom> releasePlaceInCollectingRoom(final Long examId, Long roomId);

}
