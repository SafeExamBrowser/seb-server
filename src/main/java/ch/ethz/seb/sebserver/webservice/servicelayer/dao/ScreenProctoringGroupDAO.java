/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ScreenProctoringGroupDAO {

    /** Indicates if there is already any screen proctoring service in use for the specified exam
     *
     * @param examId the exam identifier
     * @return Result refer to the indication or to an error when happened. */
    Result<Boolean> isServiceInUse(Long examId);

    /** The the group with given identifier (PK).
     *
     * @param pk the group record primary key
     * @return Result refer to the group or to an error when happened */
    Result<ScreenProctoringGroup> getScreenProctoringGroup(Long pk);

    /** The the group with given uuid. The uuid is the reference to the group on SPS Service
     *
     * @param uuid the groups record uuid
     * @return Result refer to the group or to an error when happened */
    Result<ScreenProctoringGroup> getScreenProctoringGroup(String uuid);

    /** Get the group record with specified name for a given exam.
     *
     * @param examId the exam identifier
     * @param groupName the name of the group
     * @return Result refer to the group record or to an error when happened */
    Result<ScreenProctoringGroup> getGroupByName(Long examId, String groupName);

    /** Get all collecting group that exists for a given exam.
     *
     * @param examId the exam identifier
     * @return Result refer to the collection of all collecting group for the given exam or to an error when
     *         happened */
    Result<Collection<ScreenProctoringGroup>> getCollectingGroups(Long examId);

    /** This reserves a place in a collecting group on a given exam.
     * Creates a new collecting group record depending on the groups maxSize and on how many connection
     * already have been collected within the actual collecting room.
     *
     * @param examId the exam identifier
     * @param maxSize the maximum size of connection collected in one collecting group. Size of 0 means no limit.
     * @return Result refer to the collecting group record of place or to an error when happened*/
    Result<ScreenProctoringGroup> reservePlaceInCollectingGroup(Long examId, int maxSize);

    Result<ScreenProctoringGroup> releasePlaceInCollectingGroup(Long examId, Long groupId);

    /** This creates a new ScreenProctoringGroup with the given group data.
     * Note that examId and uuid and name are mandatory. The size is ignored and initially set to 0
     *
     * @param group the ScreenProctoringGroup data
     * @return Result refer to the new group or to an error when happened */
    Result<ScreenProctoringGroup> createNewGroup(ScreenProctoringGroup group);

    /** Delete the group record with given id.
     *
     * @param pk the group identifier (PK)
     * @return Result refer to the entity key of the former group record or to an error when happened */
    Result<EntityKey> deleteRoom(Long pk);

    /** Delete all groups records for a given exam.
     *
     * @param examId the exam identifier
     * @return Result refer to a collection of entity keys for all delete group records or to an error when happened */
    Result<Collection<EntityKey>> deleteGroups(Long examId);

    void updateGroupSize(String groupUUID, Integer activeCount, Integer totalCount);

    void resetAllForExam(Long examId);

    boolean hasActiveGroups();
}
