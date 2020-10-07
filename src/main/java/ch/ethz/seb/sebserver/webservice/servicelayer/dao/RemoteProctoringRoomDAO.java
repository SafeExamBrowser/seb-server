/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface RemoteProctoringRoomDAO {

    Result<Collection<RemoteProctoringRoom>> getRoomsForExam(Long examId);

    Result<RemoteProctoringRoom> createNewRoom(final Long examId, RemoteProctoringRoom room);

    Result<RemoteProctoringRoom> saveRoom(final Long examId, RemoteProctoringRoom room);

    Result<Collection<EntityKey>> deleteRooms(Long examId);

    Result<RemoteProctoringRoom> reservePlaceInRoom(Long examId, int roomMaxSize);

    Result<RemoteProctoringRoom> releasePlaceInRoom(final Long examId, Long roomId);

}
