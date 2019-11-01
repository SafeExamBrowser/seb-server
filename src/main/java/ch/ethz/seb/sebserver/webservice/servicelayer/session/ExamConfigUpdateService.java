/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ExamConfigUpdateService {

    /** Used to process a SEB Exam Configuration change that can also effect some
     * running exams that as the specified configuration attached.
     *
     * This deals with the whole functionality the underling data-structure provides. So
     * it assumes a N to M relationship between a SEB Exam Configuration and an Exam even
     * if this may currently not be possible in case of implemented restrictions.
     *
     * First of all a consistency check is applied that checks if there is no running Exam
     * involved that has currently active SEB Client connection. Active SEB Client connections are
     * established connections that are not yet closed and connection attempts that are older the a
     * defined time interval.
     *
     * After this check passed, the system places an update-lock on each Exam that is involved on the
     * data-base level and commit this immediately so that this can prevent new SEB Client connection
     * attempts to be allowed.
     *
     * After placing update-locks the fist check is done again to ensure there where no new SEB Client
     * connection attempts in the meantime. If there where, the procedure will stop and rollback all
     * changes so far.
     *
     * If everything is locked the changes to the SEB Exam Configuration will be saved to the data-base
     * and all involved caches will be flushed to ensure the changed will take effect on next request.
     *
     * The last step is to update the SEB restriction on the LMS for every involved exam if it is running
     * and the feature is switched on. If something goes wrong during the update for an exam here, no
     * rollback of the entire procedure is applied. Instead the error is logged and the update can be
     * later triggered manually by an administrator.
     *
     * If there is any other error during the procedure the changes are rolled back and a force release of
     * the update-locks is applied to ensure all involved Exams are not locked anymore.
     *
     * @param configurationNodeId the SEB Configuration node identifier
     * @return Result refer to a list of involved and updated Exam identifiers */
    Result<Collection<Long>> processSEBExamConfigurationChange(Long configurationNodeId);

    Result<Long> processSEBExamConfigurationAttachmentChange(Long examId);

    /** Use this to force a release of update-locks for all Exams that has the specified
     * SEB Exam Configuration attached.
     *
     * @param configurationId The configuration that belongs to the affected SEB Exam configuration node */
    void forceReleaseUpdateLocks(Long configurationId);

    /** Use this to force a release of update-locks for all Exams that has the specified
     * SEB Exam Configuration attached.
     *
     * @param examIds list of Exam identifiers to apply the force release to */
    Collection<Result<Long>> forceReleaseUpdateLocks(Collection<Long> examIds);

    /** Use this to check if for a specified SEB Exam configuration no running Exam
     * is involved that has currently active SEB Client connections. And get a list
     * of identifiers of all involved Exams that are affected by a change of the given configuration.
     *
     * Active SEB Client connections are established connections that are not yet closed and connection
     * attempts that are older the a defined time interval.
     *
     * @param configurationNodeId the identifier of the SEB Exam configuration
     * @return Result refer to a list of identifiers of involved Exams or refer to an error on integrity violation or
     *         processing error */
    Result<Collection<Long>> checkRunningExamIntegrity(final Long configurationNodeId);

    /** Use this to check if a specified Exam has currently active SEB Client connections.
     *
     * Active SEB Client connections are established connections that are not yet closed and
     * connection attempts that are older the a defined time interval.
     *
     * @param examId The Exam identifier
     * @return true if the given Exam has currently no active client connection, false otherwise. */
    boolean hasActiveSebClientConnections(final Long examId);

    /** Used to apply SEB Client restriction within the LMS API for a specified Exam, if
     * the exam has activated the SEB Client restriction
     *
     * @param exam the Exam instance
     * @return Result refer to the Exam instance or to an error if happened */
    Result<Exam> applySebClientRestriction(Exam exam);

    /** Used to update SEB Client restriction within the LMS API for a specified Exam, if
     * the exam has activated the SEB Client restriction and the Exam is already running.
     *
     * @param exam the Exam instance
     * @return Result refer to the Exam instance or to an error if happened */
    Result<Exam> updateSebClientRestriction(Exam exam);

    /** Used to release SEB Client restriction within the LMS API for a specified Exam.
     *
     * @param exam the Exam instance
     * @return Result refer to the Exam instance or to an error if happened */
    Result<Exam> releaseSebClientRestriction(Exam exam);

}
