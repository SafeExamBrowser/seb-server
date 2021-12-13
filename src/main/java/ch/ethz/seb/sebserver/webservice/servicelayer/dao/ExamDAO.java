/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import org.springframework.cache.annotation.CacheEvict;

import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;

/** Concrete EntityDAO interface of Exam entities */
public interface ExamDAO extends ActivatableEntityDAO<Exam, Exam>, BulkActionSupportDAO<Exam> {

    /** Get a GrantEntity for the exam of specified id (PK)
     * This is actually a Exam instance but with no course data loaded.
     *
     * @param id The id of the exam (PK)
     * @return Result referring to the GrantEntity of the exam or to an error when happened */
    Result<GrantEntity> examGrantEntityByPK(final Long id);

    /** Get an Exam GrantEntity by a given ClientConnection id.
     * This is actually a Exam instance but with no course data loaded.
     *
     * @param connectionId the connection identifier
     * @return a Result containing the Exam GrantEntity by a given ClientConnection id or refer to an error if
     *         happened */
    Result<GrantEntity> examGrantEntityByClientConnection(Long connectionId);

    /** Get all active Exams for a given institution.
     *
     * @param institutionId the identifier of the institution
     * @return Result refer to a collection of all active exams of the given institution or refer to an error if
     *         happened */
    Result<Collection<Long>> allIdsOfInstitution(Long institutionId);

    /** Get all active and running Exams for a given institution.
     *
     * @param institutionId the identifier of the institution
     * @return Result refer to a collection of all active and running exams of the given institution or refer to an
     *         error if
     *         happened */
    Result<Collection<Long>> allIdsOfRunning(final Long institutionId);

    /** Get all institution ids for that a specified exam for given quiz id already exists
     *
     * @param quizId The quiz or external identifier of the exam (LMS)
     * @return Result refer to a collection of primary keys of the institutions or to an error when happened */
    Result<Collection<Long>> allInstitutionIdsByQuizId(String quizId);

    /** Updates the exam status for specified exam
     *
     * @param examId The exam identifier
     * @param status the exam status to update to
     * @param updateId the update identifier to check update write lock
     * @return Result refer to updated Exam or to an error if happened */
    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_RUNNING_EXAM,
            key = "#examId")
    Result<Exam> updateState(Long examId, ExamStatus status, String updateId);

    /** Saves the Exam and updates the running exam cache. */
    @Override
    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_RUNNING_EXAM,
            key = "#exam.id")
    Result<Exam> save(Exam exam);

    /** Use this to get identifiers of all exams in a specified state for a specified institution.
     *
     * @param institutionId the institution identifier. May be null for all institutions
     * @param status the ExamStatus
     * @return Result refer to collection of exam identifiers or to an error if happened */
    Result<Collection<Long>> getExamIdsForStatus(Long institutionId, ExamStatus status);

    /** This is used to get all Exams to check if they have to set into running state in the meanwhile.
     * Gets all exams in the upcoming status for run-check
     *
     * @return Result refer to a collection of exams or to an error if happened */
    Result<Collection<Exam>> allForRunCheck();

    /** This is used to get all Exams to check if they have to set into finished state in the meanwhile.
     * Gets all exams in the running status for end-check
     *
     * @return Result refer to a collection of exams or to an error if happened */
    Result<Collection<Exam>> allForEndCheck();

    /** Get a collection of all currently running exam identifiers
     *
     * @return collection of all currently running exam identifiers */
    Result<Collection<Long>> allRunningExamIds();

    /** This is used to place an internal (write)lock for the specified exam.
     * The exam will be marked as locked on the persistence level to prevent other running web-service instances
     * to write concurrently to the specified exam while it is been updated by an internal batch process.
     *
     * @param examId the exam identifier
     * @param updateId an update identifier
     * @return Result refer to the specified exam or to an error if happened */
    Result<Exam> placeLock(Long examId, String updateId);

    /** This is used to release an internal (write)lock for the specified exam.
     * The exam will be marked as not locked on the persistence level.
     *
     * @param examId the exam identifier
     * @param updateId an update identifier
     * @return Result refer to the specified exam or to an error if happened */
    Result<Exam> releaseLock(Long examId, String updateId);

    /** This is used to force release an internal (write)lock for the specified exam.
     * The exam will be marked as not locked on the persistence level even if it is currently locked by another process
     *
     * @param examId the exam identifier
     * @return Result refer to the specified exam or to an error if happened */
    Result<Long> forceUnlock(Long examId);

    /** Used to force unlock all locked exams for a specified updateId
     *
     * @param updateId the update identifier
     * @return list of identifiers of unlocked exams */
    Result<Collection<Long>> forceUnlockAll(String updateId);

    /** Indicates if the exam with specified identifier has an internal write lock.
     *
     * @param examId the exam identifier
     * @return Result refer to the lock-check-result or to an error if happened */
    Result<Boolean> isLocked(Long examId);

    /** This checks if there are write locks that are out of date and release such. */
    void releaseAgedLocks();

    /** Use this to check if the exam with the specified identifier is up to date
     *
     * @param examId the exam identifier
     * @param updateId the update identifier of the exam
     * @return Result refer to the up-to-date result or to an error if happened */
    Result<Boolean> upToDate(Long examId, String updateId);

    /** This is used to set the seb-restriction flag for a specified exam.
     *
     * @param examId the exam identifier
     * @param sebRestriction the seb-restriction flag value
     * @return Result refer to the updated Exam or to an error if happened */
    Result<Exam> setSEBRestriction(Long examId, boolean sebRestriction);

}
