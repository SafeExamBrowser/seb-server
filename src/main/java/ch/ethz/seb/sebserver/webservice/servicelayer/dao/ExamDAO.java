/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.cache.annotation.CacheEvict;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;

/** Concrete EntityDAO interface of Exam entities */
public interface ExamDAO extends ActivatableEntityDAO<Exam, Exam>, BulkActionSupportDAO<Exam> {

    /** Use this to find an imported Exam with external id with like match
     *
     * @param internalQuizIdLike like match string that will be applied to like SQL match
     * @return Result refer to Exam that matches the query. If more then one matches, error is reported.*/
    Result<Exam> byExternalIdLike(String internalQuizIdLike);

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
     *         error if happened */
    Result<Collection<Long>> allIdsOfRunning(final Long institutionId);

    /** Get all active and running exam id's for exams that are enabled for SEB screen proctoring.
     *
     * @return Result refer to a collection of all matching exam id's or to an error if happened */
    Result<Collection<Long>> allIdsOfRunningWithScreenProctoringEnabled();

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

    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_RUNNING_EXAM,
            key = "#examId")
    String getAppSignatureKeySalt(Long examId);

    /** Saves the Exam and updates the running exam cache. */
    @Override
    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_RUNNING_EXAM,
            key = "#exam.id")
    Result<Exam> save(Exam exam);

    /** Use this to get identifiers of all exams in a specified state for a specified institution.
     *
     * @param filterMap FilterMap with other filter criteria
     * @param status the list of ExamStatus
     * @return Result refer to collection of exam identifiers or to an error if happened */
    Result<Collection<Exam>> getExamsForStatus(
            final FilterMap filterMap,
            final Predicate<Exam> predicate,
            final ExamStatus... status);

    /** Gets all for active and none archived exams within the system, independently from institution and LMSSetup.
     *
     * @return Result refer to all exams for LMS update or to an error when happened */
    Result<Collection<Exam>> allForLMSUpdate();

    /** Gets all Exams of all active LMS Setups form a given LMS Setup Id list.
     * 
     * @param lmsId List of LMS Setup ids
     * @return Result refer to all Exams of all active LMS Setups form a given LMS Setup Id list or to an error when happened
     */
    Result<Collection<Exam>> allActiveForLMSSetup(Collection<Long> lmsId);

    /** Get all Exams form LMS Setup with a given id.
     * 
     * @param lmsSetupId The LMS Setup id
     * @return Result refer to all Exam if the LMS Setup or to an error when happened
     */
    Result<Collection<Exam>> allForLMSSetup(Long lmsSetupId);

    /** This is used to get all Exams that potentially needs a state change.
     * Checks if the stored running time frame of the exam is not in sync with the current state and return
     * all exams for this is the case.
     * Adding also leadTime before and followupTime after the specified running time frame of the exam for
     * this check.
     *
     * @param leadTime Time period in milliseconds that is added to now-time-point to check the start time of the exam
     * @param followupTime Time period in milliseconds that is subtracted from now-time-point check the end time of the
     *            exam
     * @return Result refer to a collection of exams or to an error if happened */
    Result<Collection<Exam>> allThatNeedsStatusUpdate(long leadTime, long followupTime);

    /** Quickly checks if an Exam is running or not (Sate RUNNING or TEST_RUN)
     *
     * @param examId The identifier of the exam to check
     * @return true if the exam is in a running state */
    boolean isRunning(Long examId);

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
     * @return Result refer to the specified exam identifier or to an error if happened */
    Result<Long> placeLock(Long examId, String updateId);

    default Result<Exam> placeLock(final Exam exam, final String updateId) {
        return placeLock(exam.id, updateId)
                .map(id -> exam);
    }

    /** This is used to release an internal (write)lock for the specified exam.
     * The exam will be marked as not locked on the persistence level.
     *
     * @param examId the exam identifier
     * @param updateId an update identifier
     * @return Result refer to the specified exam identifier or to an error if happened */
    Result<Long> releaseLock(Long examId, String updateId);

    default Result<Exam> releaseLock(final Exam exam, final String updateId) {
        return releaseLock(exam.id, updateId)
                .map(id -> exam);
    }

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

    /** Use this to check if the exam is up-to-date
     *
     * @param exam the exam to check if it is in sync with the persistent or if there is a newer version
     * @return Result refer to the up-to-date result or to an error if happened */
    boolean upToDate(Exam exam);

    /** Marks the specified exam as updated (sets the last modified date to now)
     * to notify exam content has changed.
     * This is automatically done also with normal save but not always an entire
     * save is needed. In these cases this can be used to notify a exam content change.
     *
     * @param examId The exam identifier */
    void setModified(Long examId);

    /** This is used to set the seb-restriction flag for a specified exam.
     *
     * @param examId the exam identifier
     * @param sebRestriction the seb-restriction flag value
     * @return Result refer to the updated Exam or to an error if happened */
    Result<Exam> setSEBRestriction(Long examId, boolean sebRestriction);

    /** This deletes the exam template reference of all exams that has a given
     * template reference.
     *
     * @param examTemplateId The exam template reference identifier
     * @return Result refer to the collection of entity keys of all involved exams or to an error when happened */
    Result<Collection<EntityKey>> deleteTemplateReferences(Long examTemplateId);

    /** This is used by the internal update process to update the quiz data for the specified exam.
     * This shall only be called if there are changes to the quiz data of the exam since this also
     * refreshes the running exam cache.
     *
     * @param examId the exam identifier
     * @param quizData The quiz data to update
     * @param updateId The update identifier given by the update task
     * @return Result refer to the given QuizData or to an error when happened */
    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_RUNNING_EXAM,
            key = "#examId")
    Result<QuizData> updateQuizData(Long examId, QuizData quizData, String updateId);

    /** Marks the given exam by setting the last-update-time to current time.
     * This provokes a cache update on distributed setup.
     * Additionally, this flushes the local cache immediately.
     *
     * @param examId the Exam identifier */
    @CacheEvict(
            cacheNames = {
                    ExamSessionCacheService.CACHE_NAME_RUNNING_EXAM,
                    ExamSessionCacheService.CACHE_NAME_SEB_CONFIG_EXAM },
            key = "#examId")
    void markUpdate(Long examId);

    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_RUNNING_EXAM,
            key = "#exam.id")
    Result<Exam> applySupporter(Exam exam, String userUUID);

    /** This is used by the internal update process to mark exams for which the LMS related data availability
     *
     * @param externalQuizId The exams external UUID or quiz id of the exam to mark
     * @param available The LMS availability flag to set
     * @param updateId The update identifier given by the update task */
    void markLMSAvailability(final String externalQuizId, final boolean available, final String updateId);

    void updateQuitPassword(Exam exam, String quitPassword);
    
    void saveBrowserExamKeys(Long examId, String bek);

    void updateSupporterAccounts(Long examId, List<String> supporterUUIDs);

    /** This gets the number of exams that references a given Supporter or Teacher account UUID
     * 
     * @param uuid Supporter/Teacher account UUID
     * @return number of exams that has a supporter reference for given UUID
     */
    int numOfExamsReferencingSupporter(String uuid);

    Result<String> getImportedQuizUUIDs(Long lmsSetupId);
}
