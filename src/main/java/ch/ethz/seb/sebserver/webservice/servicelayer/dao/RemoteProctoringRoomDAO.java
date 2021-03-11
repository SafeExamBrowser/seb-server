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

public interface RemoteProctoringRoomDAO {

    Result<Collection<RemoteProctoringRoom>> getCollectingRoomsForExam(Long examId);

    Result<RemoteProctoringRoom> getRoom(Long roomId);

    Result<RemoteProctoringRoom> getRoom(Long examId, String roomName);

    Result<String> getRoomName(Long roomId);

    Result<RemoteProctoringRoom> createTownhallRoom(Long examId, NewRoom room);

    Result<RemoteProctoringRoom> getTownhallRoom(Long examId);

    Result<EntityKey> deleteTownhallRoom(Long examId);

    Result<RemoteProctoringRoom> createBreakOutRoom(Long examId, NewRoom room, String connectionTokens);

    Result<EntityKey> deleteRoom(Long roomId);

    Result<Collection<EntityKey>> deleteRooms(Long examId);

    Result<RemoteProctoringRoom> reservePlaceInCollectingRoom(
            Long examId,
            int roomMaxSize,
            Function<Long, Result<NewRoom>> newRoomFunction);

    Result<RemoteProctoringRoom> releasePlaceInCollectingRoom(final Long examId, Long roomId);

}
