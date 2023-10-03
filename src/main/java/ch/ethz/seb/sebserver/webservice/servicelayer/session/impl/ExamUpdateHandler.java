/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.Features;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamFinishedEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamResetEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamStartedEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamUpdateTask;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;

@Lazy
@Service
@WebServiceProfile
class ExamUpdateHandler implements ExamUpdateTask {

    private static final Logger log = LoggerFactory.getLogger(ExamUpdateHandler.class);

    private final ExamDAO examDAO;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final SEBRestrictionService sebRestrictionService;
    private final LmsAPIService lmsAPIService;
    private final ScreenProctoringService screenProctoringService;
    private final String updatePrefix;
    private final Long examTimePrefix;
    private final Long examTimeSuffix;
    private final boolean tryRecoverExam;
    private final int recoverAttempts;

    public ExamUpdateHandler(
            final ExamDAO examDAO,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ApplicationEventPublisher applicationEventPublisher,
            final SEBRestrictionService sebRestrictionService,
            final LmsAPIService lmsAPIService,
            final WebserviceInfo webserviceInfo,
            final ScreenProctoringService screenProctoringService,
            @Value("${sebserver.webservice.api.exam.time-prefix:3600000}") final Long examTimePrefix,
            @Value("${sebserver.webservice.api.exam.time-suffix:3600000}") final Long examTimeSuffix,
            @Value("${sebserver.webservice.api.exam.tryrecover:true}") final boolean tryRecoverExam,
            @Value("${sebserver.webservice.api.exam.recoverattempts:3}") final int recoverAttempts) {

        this.examDAO = examDAO;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.applicationEventPublisher = applicationEventPublisher;
        this.sebRestrictionService = sebRestrictionService;
        this.lmsAPIService = lmsAPIService;
        this.screenProctoringService = screenProctoringService;
        this.updatePrefix = webserviceInfo.getLocalHostAddress()
                + "_" + webserviceInfo.getServerPort() + "_";
        this.examTimePrefix = examTimePrefix;
        this.examTimeSuffix = examTimeSuffix;
        this.tryRecoverExam = tryRecoverExam;
        this.recoverAttempts = recoverAttempts;
    }

    public SEBRestrictionService getSEBRestrictionService() {
        return this.sebRestrictionService;
    }

    String createUpdateId() {
        return this.updatePrefix + Utils.getMillisecondsNow();
    }

    @Override
    public int examUpdateTaskProcessingOrder() {
        return 0;
    }

    @Override
    public void processExamUpdateTask() {

        final String updateId = this.createUpdateId();

        if (log.isDebugEnabled()) {
            log.debug("Run exam update task with Id: {}", updateId);
        }

        controlExamLMSUpdate();
        controlExamState(updateId);
        this.examDAO.releaseAgedLocks();
    }

    Result<Set<String>> updateExamFromLMS(final Long lmsSetupId, final Map<String, Exam> exams) {

        return Result.tryCatch(() -> {
            final Set<String> failedOrMissing = new HashSet<>(exams.keySet());
            final String updateId = this.createUpdateId();

            // test overall LMS access
            try {
                this.lmsAPIService
                        .getLmsAPITemplate(lmsSetupId)
                        .getOrThrow()
                        .checkCourseAPIAccess();
            } catch (final Exception e) {
                log.warn("No LMS access, mark all exams of the LMS as not connected to LMS");
                if (!failedOrMissing.isEmpty()) {
                    failedOrMissing
                            .stream()
                            .forEach(quizId -> {
                                try {
                                    final Exam exam = exams.get(quizId);
                                    if (exam.lmsAvailable == null || exam.isLmsAvailable()) {
                                        this.examDAO.markLMSAvailability(quizId, false, updateId);
                                    }
                                } catch (final Exception ee) {
                                    log.error("Failed to mark exam: {} as not connected to LMS", quizId, ee);
                                }
                            });
                }
                return failedOrMissing;
            }

            this.lmsAPIService
                    .getLmsAPITemplate(lmsSetupId)
                    .map(template -> {
                        // TODO flush only involved courses from cache!
                        template.clearCourseCache();
                        return template;
                    })
                    .flatMap(template -> template.getQuizzes(new HashSet<>(exams.keySet())))
                    .onError(error -> log.warn(
                            "Failed to get quizzes from LMS Setup: {} cause: {}",
                            lmsSetupId,
                            error.getMessage()))
                    .getOr(Collections.emptyList())
                    .stream()
                    .forEach(quiz -> {

                        try {
                            final Exam exam = getExamForQuizWithMoodleSpecialCase(exams, quiz);

                            if (exam == null) {
                                log.warn("Failed to find map exam to fetched quiz-data: {}", quiz);
                                return;
                            }

                            if (hasChanges(exam, quiz)) {

                                final Result<QuizData> updateQuizData = this.examDAO
                                        .updateQuizData(exam.id, quiz, updateId);

                                if (updateQuizData.hasError()) {
                                    log.error("Failed to update quiz data for exam: {}", quiz,
                                            updateQuizData.getError());
                                } else {
                                    if (!exam.isLmsAvailable()) {
                                        this.examDAO.markLMSAvailability(quiz.id, true, updateId);
                                        // delete attempts attribute
                                        this.additionalAttributesDAO.delete(
                                                EntityType.EXAM,
                                                exam.id,
                                                Exam.ADDITIONAL_ATTR_QUIZ_RECOVER_ATTEMPTS);
                                    }
                                    failedOrMissing.remove(quiz.id);
                                    log.info("Updated quiz data for exam: {}", updateQuizData.get());
                                }

                                // also update the exam on screen proctoring service if exam has screen proctoring enabled
                                this.screenProctoringService
                                        .updateExamOnScreenProctoingService(exam.id)
                                        .onError(error -> log
                                                .error("Failed to update exam changes for screen proctoring"));

                            } else {
                                if (!exam.isLmsAvailable()) {
                                    this.examDAO.markLMSAvailability(quiz.id, true, updateId);
                                }
                                failedOrMissing.remove(quiz.id);
                            }
                        } catch (final Exception e) {
                            log.error("Unexpected error while trying to update quiz data for exam: {}", quiz, e);
                        }
                    });

            if (!failedOrMissing.isEmpty() && this.tryRecoverExam) {
                new HashSet<>(failedOrMissing).stream()
                        .forEach(quizId -> tryRecoverQuizData(quizId, lmsSetupId, exams, updateId)
                                .onSuccess(quizData -> failedOrMissing.remove(quizId)));
            }

            return failedOrMissing;
        });
    }

    @EventListener(ExamUpdateEvent.class)
    void updateRunning(final ExamUpdateEvent event) {
        this.examDAO
                .byPK(event.examId)
                .onSuccess(exam -> updateState(
                        exam,
                        DateTime.now(DateTimeZone.UTC),
                        this.examTimePrefix,
                        this.examTimeSuffix,
                        this.createUpdateId()));
    }

    void updateState(
            final Exam exam,
            final DateTime now,
            final long leadTime,
            final long followupTime,
            final String updateId) {

        try {
            // Include leadTime and followupTime
            final DateTime startTimeThreshold = now.plus(leadTime);
            final DateTime endTimeThreshold = now.minus(leadTime);

            if (log.isDebugEnabled()) {
                log.debug("Check exam update for startTimeThreshold: {}, endTimeThreshold {}, exam: {}",
                        startTimeThreshold,
                        endTimeThreshold,
                        exam);
            }

            if (exam.status == ExamStatus.ARCHIVED) {
                log.warn("Exam in unexpected state for status update. Skip update. Exam: {}", exam);
                return;
            }

            if (exam.status != ExamStatus.RUNNING && withinTimeframe(
                    exam.startTime,
                    startTimeThreshold,
                    exam.endTime,
                    endTimeThreshold)) {

                if (withinTimeframe(exam.startTime, startTimeThreshold, exam.endTime, endTimeThreshold)) {
                    setRunning(exam, updateId)
                            .onError(error -> log.error("Failed to update exam to running state: {}",
                                    exam,
                                    error));
                    return;
                }
            }

            if (exam.status != ExamStatus.FINISHED &&
                    exam.endTime != null &&
                    endTimeThreshold.isAfter(exam.endTime)) {
                setFinished(exam, updateId)
                        .onError(error -> log.error("Failed to update exam to finished state: {}",
                                exam,
                                error));
                return;
            }

            if (exam.status != ExamStatus.UP_COMING &&
                    exam.startTime != null &&
                    startTimeThreshold.isBefore(exam.startTime)) {
                setUpcoming(exam, updateId)
                        .onError(error -> log.error("Failed to update exam to up-coming state: {}",
                                exam,
                                error));
            }
        } catch (final Exception e) {
            log.error("Unexpected error while trying to update exam state for exam: {}", exam, e);
        }
    }

    private boolean withinTimeframe(
            final DateTime startTime,
            final DateTime startTimeThreshold,
            final DateTime endTime,
            final DateTime endTimeThreshold) {

        if (startTime == null && endTime == null) {
            return true;
        }

        if (startTime == null && endTime.isAfter(endTimeThreshold)) {
            return true;
        }

        if (endTime == null && startTime.isBefore(startTimeThreshold)) {
            return true;
        }

        return (startTime.isBefore(startTimeThreshold) && endTime.isAfter(endTimeThreshold));
    }

    Result<Exam> setUpcoming(final Exam exam, final String updateId) {
        if (log.isDebugEnabled()) {
            log.debug("Update exam as up-coming: {}", exam);
        }

        return this.examDAO
                .placeLock(exam.id, updateId)
                .flatMap(e -> this.examDAO.updateState(exam.id, ExamStatus.UP_COMING, updateId))
                .map(e -> {
                    this.examDAO
                            .releaseLock(e, updateId)
                            .onError(error -> this.examDAO
                                    .forceUnlock(exam.id)
                                    .onError(unlockError -> log.error(
                                            "Failed to force unlock update look for exam: {}",
                                            exam.id)));
                    return e;
                })
                .map(e -> {
                    this.applicationEventPublisher.publishEvent(new ExamResetEvent(exam));
                    return exam;
                });
    }

    Result<Exam> setRunning(final Exam exam, final String updateId) {
        if (log.isDebugEnabled()) {
            log.debug("Update exam as running: {}", exam);
        }

        return this.examDAO
                .placeLock(exam.id, updateId)
                .flatMap(e -> this.examDAO.updateState(exam.id, ExamStatus.RUNNING, updateId))
                .map(e -> {
                    this.examDAO
                            .releaseLock(e, updateId)
                            .onError(error -> this.examDAO
                                    .forceUnlock(exam.id)
                                    .onError(unlockError -> log.error(
                                            "Failed to force unlock update look for exam: {}",
                                            exam.id)));
                    return e;
                })
                .map(e -> {
                    this.applicationEventPublisher.publishEvent(new ExamStartedEvent(exam));
                    return exam;
                });
    }

    Result<Exam> setFinished(final Exam exam, final String updateId) {
        if (log.isDebugEnabled()) {
            log.debug("Update exam as finished: {}", exam);
        }

        return this.examDAO
                .placeLock(exam.id, updateId)
                .flatMap(e -> this.examDAO.updateState(exam.id, ExamStatus.FINISHED, updateId))
                .map(e -> {
                    this.examDAO
                            .releaseLock(e, updateId)
                            .onError(error -> this.examDAO
                                    .forceUnlock(exam.id)
                                    .onError(unlockError -> log.error(
                                            "Failed to force unlock update look for exam: {}",
                                            exam.id)));
                    return e;
                })
                .map(e -> {
                    this.applicationEventPublisher.publishEvent(new ExamFinishedEvent(exam));
                    return exam;
                });
    }

    private boolean hasChanges(final Exam exam, final QuizData quizData) {
        if (!Utils.isEqualsWithEmptyCheck(exam.name, quizData.name) ||
                !Objects.equals(exam.startTime, quizData.startTime) ||
                !Objects.equals(exam.endTime, quizData.endTime) ||
                !Utils.isEqualsWithEmptyCheckTruncated(exam.getDescription(), quizData.description) ||
                !Utils.isEqualsWithEmptyCheck(exam.getStartURL(), quizData.startURL) ||
                !Objects.equals(exam.externalId, quizData.id)) {

            if (!Utils.isEqualsWithEmptyCheck(exam.name, quizData.name)) {
                log.info("Update name difference from LMS. Exam: {}, QuizData: {}", exam.name, quizData.name);
            }
            if (!Objects.equals(exam.startTime, quizData.startTime)) {
                log.info("Update startTime difference from LMS. Exam: {}, QuizData: {}", exam.startTime,
                        quizData.startTime);
            }
            if (!Objects.equals(exam.endTime, quizData.endTime)) {
                log.info("Update endTime difference from LMS. Exam: {}, QuizData: {}", exam.endTime, quizData.endTime);
            }
            if (!Utils.isEqualsWithEmptyCheckTruncated(exam.getDescription(), quizData.description)) {
                log.info("Update description difference from LMS. Exam: {}", exam);
            }
            if (!Utils.isEqualsWithEmptyCheck(exam.getStartURL(), quizData.startURL)) {
                log.info("Update startURL difference from LMS. Exam:{}, QuizData: {}",
                        exam.getStartURL(),
                        quizData.startURL);
            }
            if (!Objects.equals(exam.externalId, quizData.id)) {
                log.info("Update quizId difference from LMS. Exam:{}, QuizData: {}",
                        exam.externalId,
                        quizData.id);
            }

            return true;
        }

        if (quizData.additionalAttributes != null && !quizData.additionalAttributes.isEmpty()) {
            for (final Map.Entry<String, String> attr : quizData.additionalAttributes.entrySet()) {
                final String currentAttrValue = exam.getAdditionalAttribute(attr.getKey());
                if (!Utils.isEqualsWithEmptyCheck(currentAttrValue, attr.getValue())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Update difference from LMS: attribute{}, currentValue: {}, lmsValue: {}",
                                attr.getKey(),
                                currentAttrValue,
                                attr.getValue());
                    }
                    return true;
                }
            }
        }

        return false;
    }

    private Result<QuizData> tryRecoverQuizData(
            final String quizId,
            final Long lmsSetupId,
            final Map<String, Exam> exams,
            final String updateId) {

        return Result.tryCatch(() -> {

            final LmsAPITemplate lmsTemplate = this.lmsAPIService
                    .getLmsAPITemplate(lmsSetupId)
                    .getOrThrow();

            final Exam exam = exams.get(quizId);
            if (!lmsTemplate.getType().features.contains(Features.COURSE_RECOVERY)) {
                if (exam.lmsAvailable == null || exam.isLmsAvailable()) {
                    this.examDAO.markLMSAvailability(quizId, false, updateId);
                }
                throw new UnsupportedOperationException("No Course Recovery");
            }

            final int attempts = Integer.parseInt(this.additionalAttributesDAO.getAdditionalAttribute(
                    EntityType.EXAM,
                    exam.id,
                    Exam.ADDITIONAL_ATTR_QUIZ_RECOVER_ATTEMPTS)
                    .map(AdditionalAttributeRecord::getValue)
                    .getOr("0"));

            if (attempts >= this.recoverAttempts) {
                if (log.isDebugEnabled()) {
                    log.debug("Skip recovering quiz due to too many attempts for exam: {}", exam.getModelId());
                    throw new RuntimeException("Recover attempts reached");
                }
            }

            log.info(
                    "Try to recover quiz data from LMS: {} quiz with internal identifier: {}",
                    lmsSetupId,
                    quizId);

            return this.lmsAPIService
                    .getLmsAPITemplate(lmsSetupId)
                    .flatMap(template -> template.tryRecoverQuizForExam(exam))
                    .onSuccess(recoveredQuizData -> recoverSuccess(updateId, exam, recoveredQuizData))
                    .onError(error -> recoverError(quizId, updateId, exam, attempts))
                    .getOrThrowRuntime("Not Available");
        });
    }

    private void recoverError(final String quizId, final String updateId, final Exam exam, final int attempts) {

        // increment attempts
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                exam.id,
                Exam.ADDITIONAL_ATTR_QUIZ_RECOVER_ATTEMPTS,
                String.valueOf(attempts + 1))
                .onError(error1 -> log.error("Failed to save new attempts: ", error1));

        if (exam.lmsAvailable == null || exam.isLmsAvailable()) {
            this.examDAO.markLMSAvailability(quizId, false, updateId);
        }
    }

    private void recoverSuccess(final String updateId, final Exam exam, final QuizData recoveredQuizData) {
        if (recoveredQuizData != null) {

            // save exam with new external id and quit data
            this.examDAO
                    .updateQuizData(exam.id, recoveredQuizData, updateId)
                    .onError(error -> log.error("Failed to save exam for recovered quiz data: ", error))
                    .onSuccess(qd -> log.info("Successfully recovered exam from quiz data, {}", qd))
                    .getOrThrow();

            // delete attempts attribute
            this.additionalAttributesDAO.delete(
                    EntityType.EXAM,
                    exam.id,
                    Exam.ADDITIONAL_ATTR_QUIZ_RECOVER_ATTEMPTS);
        }
    }

    private void controlExamLMSUpdate() {
        if (log.isTraceEnabled()) {
            log.trace("Start update exams from LMS");
        }

        try {

            // create mapping
            final Map<Long, Map<String, Exam>> examToLMSMapping = new HashMap<>();
            this.examDAO.allForLMSUpdate()
                    .onError(error -> log.error("Failed to update exams from LMS: ", error))
                    .getOr(Collections.emptyList())
                    .stream()
                    .forEach(exam -> {
                        final Map<String, Exam> examMap = (examToLMSMapping.computeIfAbsent(
                                exam.lmsSetupId,
                                lmsId -> new HashMap<>()));
                        examMap.put(exam.externalId, exam);
                    });

            // update per LMS Setup
            examToLMSMapping.entrySet()
                    .stream()
                    .forEach(updateEntry -> {
                        final Result<Set<String>> updateExamFromLMS = this.updateExamFromLMS(
                                updateEntry.getKey(),
                                updateEntry.getValue());

                        if (updateExamFromLMS.hasError()) {
                            log.error("Failed to update exams from LMS: ", updateExamFromLMS.getError());
                        } else {
                            final Set<String> failedExams = updateExamFromLMS.get();
                            if (!failedExams.isEmpty()) {
                                log.warn("Failed to update following exams from LMS: {}", failedExams);
                            }
                        }
                    });

        } catch (final Exception e) {
            log.error("Unexpected error while update exams from LMS: ", e);
        }
    }

    private void controlExamState(final String updateId) {
        if (log.isTraceEnabled()) {
            log.trace("Check starting exams: {}", updateId);
        }

        try {

            final DateTime now = DateTime.now(DateTimeZone.UTC);
            this.examDAO
                    .allThatNeedsStatusUpdate(this.examTimePrefix, this.examTimeSuffix)
                    .getOrThrow()
                    .stream()
                    .forEach(exam -> this.updateState(
                            exam,
                            now,
                            this.examTimePrefix,
                            this.examTimeSuffix,
                            updateId));

        } catch (final Exception e) {
            log.error("Unexpected error while trying to run exam state update task: ", e);
        }
    }

    /** NOTE: LMS binding for Moodle uses a composed quiz-identifier with also contains the course short name
     * for the reason to be able to re-identify a quiz when the main quiz-id changes in case of backup-restore on Moodle
     * But since course names also can change this function tries to find old Exam mappings for course-names hat has
     * changed */
    private Exam getExamForQuizWithMoodleSpecialCase(final Map<String, Exam> exams, final QuizData quiz) {
        Exam exam = exams.get(quiz.id);

        if (exam == null) {
            try {
                final LmsAPITemplate lms = this.lmsAPIService
                        .getLmsAPITemplate(quiz.lmsSetupId)
                        .getOrThrow();

                if (lms.getType() == LmsType.MOODLE || lms.getType() == LmsType.MOODLE_PLUGIN) {
                    final String quizId = MoodleUtils.getQuizId(quiz.id);
                    final Optional<String> find =
                            exams.keySet().stream().filter(key -> key.startsWith(quizId)).findFirst();
                    if (find.isPresent()) {
                        exam = exams.get(find.get());
                    }
                }
            } catch (final Exception e) {
                log.error("Failed to verify changed external Exam id from moodle course: {}", e.getMessage());
            }
        }

        return exam;
    }

}
