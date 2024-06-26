/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.Collection;
import java.util.Set;

import ch.ethz.seb.sebserver.webservice.servicelayer.lms.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService.IntegrationData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService.ExamData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

public class LmsAPITemplateAdapter implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(LmsAPITemplateAdapter.class);

    private static final int DEFAULT_ATTEMPTS = 1;

    private final CourseAccessAPI courseAccessAPI;
    private final SEBRestrictionAPI sebRestrictionAPI;

    private final FullLmsIntegrationAPI lmsIntegrationAPI;
    private final APITemplateDataSupplier apiTemplateDataSupplier;

    /** CircuitBreaker for protected lmsTestRequest */
    private final CircuitBreaker<LmsSetupTestResult> lmsTestRequest;
    /** CircuitBreaker for protected quiz and course data requests */
    private final CircuitBreaker<Collection<QuizData>> quizzesRequest;
    /** CircuitBreaker for protected quiz and course data requests */
    private final CircuitBreaker<QuizData> quizRequest;
    /** CircuitBreaker for protected quiz and course data requests */
    private final CircuitBreaker<QuizData> quizRecoverRequest;
    /** CircuitBreaker for protected chapter data requests */
    private final CircuitBreaker<Chapters> chaptersRequest;
    /** CircuitBreaker for protected examinee account details requests */
    private final CircuitBreaker<ExamineeAccountDetails> accountDetailRequest;

    private final CircuitBreaker<SEBRestriction> restrictionRequest;
    private final CircuitBreaker<Exam> examRequest;
    private final CircuitBreaker<IntegrationData> lmsAccessRequest;
    private final CircuitBreaker<ExamData> applyExamDataRequest;
    private final CircuitBreaker<String> deleteLmsAccessRequest;

    public LmsAPITemplateAdapter(
            final AsyncService asyncService,
            final Environment environment,
            final APITemplateDataSupplier apiTemplateDataSupplier,
            final CourseAccessAPI courseAccessAPI,
            final SEBRestrictionAPI sebRestrictionAPI,
            final FullLmsIntegrationAPI lmsIntegrationAPI) {

        this.courseAccessAPI = courseAccessAPI;
        this.sebRestrictionAPI = sebRestrictionAPI;
        this.apiTemplateDataSupplier = apiTemplateDataSupplier;
        this.lmsIntegrationAPI = lmsIntegrationAPI;

        this.lmsTestRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.lmsTestRequest.attempts",
                        Integer.class,
                        DEFAULT_ATTEMPTS),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.lmsTestRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 20),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.lmsTestRequest.timeToRecover",
                        Long.class,
                        0L));

        lmsAccessRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.lmsAccessRequest.attempts",
                        Integer.class,
                        DEFAULT_ATTEMPTS),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.lmsAccessRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 20),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.lmsAccessRequest.timeToRecover",
                        Long.class,
                        0L));

        applyExamDataRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.applyExamDataRequest.attempts",
                        Integer.class,
                        DEFAULT_ATTEMPTS),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.applyExamDataRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 20),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.applyExamDataRequest.timeToRecover",
                        Long.class,
                        0L));

        deleteLmsAccessRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.lmsTestRequest.attempts",
                        Integer.class,
                        DEFAULT_ATTEMPTS),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.lmsTestRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 20),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.lmsTestRequest.timeToRecover",
                        Long.class,
                        0L));

        this.quizzesRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.attempts",
                        Integer.class,
                        DEFAULT_ATTEMPTS),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.timeToRecover",
                        Long.class,
                        0L));

        this.quizRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.attempts",
                        Integer.class,
                        DEFAULT_ATTEMPTS),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.timeToRecover",
                        Long.class,
                        0L));

        this.quizRecoverRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.attempts",
                        Integer.class,
                        DEFAULT_ATTEMPTS),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.timeToRecover",
                        Long.class,
                        0L));

        this.chaptersRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.chaptersRequest.attempts",
                        Integer.class,
                        DEFAULT_ATTEMPTS),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.chaptersRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.chaptersRequest.timeToRecover",
                        Long.class,
                        0L));

        this.accountDetailRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.accountDetailRequest.attempts",
                        Integer.class,
                        2),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.accountDetailRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.accountDetailRequest.timeToRecover",
                        Long.class,
                        0L));

        this.restrictionRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.sebrestriction.attempts",
                        Integer.class,
                        DEFAULT_ATTEMPTS),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.sebrestriction.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.sebrestriction.timeToRecover",
                        Long.class,
                        0L));

        this.examRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.examRequest.attempts",
                        Integer.class,
                        DEFAULT_ATTEMPTS),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.examRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.examRequest.timeToRecover",
                        Long.class,
                        0L));
    }

    @Override
    public LmsType getType() {
        return this.lmsSetup().getLmsType();
    }

    @Override
    public LmsSetup lmsSetup() {
        return this.apiTemplateDataSupplier.getLmsSetup();
    }

    @Override
    public void checkCourseAPIAccess() {
        this.lmsTestRequest
                .protectedRun(() -> {
                    final LmsSetupTestResult testCourseAccessAPI = this.courseAccessAPI.testCourseAccessAPI();
                    if (!testCourseAccessAPI.isOk()) {
                        throw new RuntimeException("No course API Access: " + testCourseAccessAPI);
                    }
                    return testCourseAccessAPI;
                }).getOrThrow();
    }

    @Override
    public String getCourseIdFromExam(final Exam exam) {
        return this.courseAccessAPI.getCourseIdFromExam(exam);
    }

    @Override
    public String getQuizIdFromExam(final Exam exam) {
        return this.courseAccessAPI.getQuizIdFromExam(exam);
    }

    @Override
    public LmsSetupTestResult testCourseAccessAPI() {
        if (this.courseAccessAPI != null) {
            if (log.isDebugEnabled()) {
                log.debug("Test Course Access API for LMSSetup: {}", lmsSetup());
            }

            return this.lmsTestRequest.protectedRun(this.courseAccessAPI::testCourseAccessAPI)
                    .onError(error -> log.error(
                            "Failed to run protectedQuizzesRequest: {}",
                            error.getMessage()))
                    .getOrThrow();
        }

        return LmsSetupTestResult.ofAPINotSupported(getType());
    }

    @Override
    public void fetchQuizzes(final FilterMap filterMap, final AsyncQuizFetchBuffer asyncQuizFetchBuffer) {
        if (this.courseAccessAPI == null) {
            asyncQuizFetchBuffer.finish(new UnsupportedOperationException(
                    "Course API Not Supported For: " + getType().name()));
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Get quizzes for LMSSetup: {}", lmsSetup());
        }

        this.courseAccessAPI.fetchQuizzes(filterMap, asyncQuizFetchBuffer);
        asyncQuizFetchBuffer.finish();
    }

    @Override
    public Result<Collection<QuizData>> getQuizzes(final Set<String> ids) {

        if (this.courseAccessAPI == null) {
            return Result
                    .ofError(new UnsupportedOperationException("Course API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Get quizzes {} for LMSSetup: {}", ids, lmsSetup());
        }

        return this.quizzesRequest.protectedRun(() -> this.courseAccessAPI
                .getQuizzes(ids)
                .onError(error -> log.error(
                        "Failed to run protectedQuizzesRequest: {}",
                        error.getMessage()))
                .getOrThrow());
    }

    @Override
    public Result<QuizData> getQuiz(final String id) {

        if (this.courseAccessAPI == null) {
            return Result
                    .ofError(new UnsupportedOperationException("Course API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Get quiz {} for LMSSetup: {}", id, lmsSetup());
        }

        return this.quizRequest.protectedRun(() -> this.courseAccessAPI
                .getQuiz(id)
                .onError(error -> log.error(
                        "Failed to run protectedQuizRequest: {}",
                        error.getMessage()))
                .getOrThrow());
    }

    @Override
    public Result<QuizData> tryRecoverQuizForExam(final Exam exam) {

        if (this.courseAccessAPI == null) {
            return Result
                    .ofError(new UnsupportedOperationException("Course API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Try to recover quiz for exam {} for LMSSetup: {}", exam, lmsSetup());
        }

        return this.quizRecoverRequest.protectedRun(() -> this.courseAccessAPI
                .tryRecoverQuizForExam(exam)
                .getOrThrow());
    }

    @Override
    public void clearCourseCache() {
        if (this.courseAccessAPI != null) {

            if (log.isDebugEnabled()) {
                log.debug("Clear course cache for LMSSetup: {}", lmsSetup());
            }

            this.courseAccessAPI.clearCourseCache();
        }
    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeUserId) {

        if (this.courseAccessAPI == null) {
            return Result
                    .ofError(new UnsupportedOperationException("Course API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Get examinee details {} for LMSSetup: {}", examineeUserId, lmsSetup());
        }

        return this.accountDetailRequest.protectedRun(() -> this.courseAccessAPI
                .getExamineeAccountDetails(examineeUserId)
                .onError(error -> log.error(
                        "Unexpected error while trying to get examinee account details: {}",
                        error.getMessage()))
                .getOrThrow());
    }

    @Override
    public String getExamineeName(final String examineeUserId) {

        if (this.courseAccessAPI == null) {
            throw new UnsupportedOperationException("Course API Not Supported For: " + getType().name());
        }

        if (log.isDebugEnabled()) {
            log.debug("Get examinee name {} for LMSSetup: {}", examineeUserId, lmsSetup());
        }

        return this.courseAccessAPI.getExamineeName(examineeUserId);
    }

    @Override
    public Result<Chapters> getCourseChapters(final String courseId) {

        if (this.courseAccessAPI == null) {
            return Result
                    .ofError(new UnsupportedOperationException("Course API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Get course chapters {} for LMSSetup: {}", courseId, lmsSetup());
        }

        return this.chaptersRequest.protectedRun(() -> this.courseAccessAPI
                .getCourseChapters(courseId)
                .onError(error -> log.error(
                        "Failed to run getCourseChapters: {}",
                        error.getMessage()))
                .getOrThrow());
    }

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        if (this.sebRestrictionAPI != null) {
            return this.sebRestrictionAPI.testCourseRestrictionAPI();
        }

        return LmsSetupTestResult.ofAPINotSupported(getType());
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {

        if (this.sebRestrictionAPI == null) {
            return Result.ofError(
                    new UnsupportedOperationException("SEB Restriction API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Get course restriction: {} for LMSSetup: {}", exam.externalId, lmsSetup());
        }

        return this.restrictionRequest.protectedRun(() -> this.sebRestrictionAPI
                .getSEBClientRestriction(exam)
                .getOrThrow());
    }

    @Override
    public boolean hasSEBClientRestriction(final Exam exam) {
        return this.sebRestrictionAPI.hasSEBClientRestriction(getSEBClientRestriction(exam).getOrThrow());
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(
            final Exam exam,
            final SEBRestriction sebRestrictionData) {

        if (this.sebRestrictionAPI == null) {
            return Result.ofError(
                    new UnsupportedOperationException("SEB Restriction API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Apply course restriction: {} for LMSSetup: {}", exam, lmsSetup());
        }

        return this.restrictionRequest.protectedRun(() -> this.sebRestrictionAPI
                .applySEBClientRestriction(exam, sebRestrictionData)
                .onError(error -> log.error(
                        "Failed to apply SEB restrictions: {}",
                        error.getMessage()))
                .getOrThrow());
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {

        if (this.sebRestrictionAPI == null) {
            return Result.ofError(
                    new UnsupportedOperationException("SEB Restriction API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Release course restriction: {} for LMSSetup: {}", exam.externalId, lmsSetup());
        }

        final Result<Exam> protectedRun = this.examRequest.protectedRun(() -> this.sebRestrictionAPI
                .releaseSEBClientRestriction(exam)
                .onError(error -> log.error(
                        "Failed to release SEB restrictions: {}",
                        error.getMessage()))
                .getOrThrow());

        if (protectedRun.hasError()) {
            final Throwable cause = protectedRun.getError().getCause();
            if (cause != null && cause.getMessage().contains("LMS Warnings")) {
                return Result.ofRuntimeError(cause.getMessage());
            }
        }
        return protectedRun;
    }

    @Override
    public boolean fullIntegrationActive() {
        return this.lmsIntegrationAPI.fullIntegrationActive();
    }

    @Override
    public LmsSetupTestResult testFullIntegrationAPI() {
        if (this.lmsIntegrationAPI != null) {
            return this.lmsIntegrationAPI.testFullIntegrationAPI();
        }

        return LmsSetupTestResult.ofAPINotSupported(getType());
    }

    @Override
    public Result<IntegrationData> applyConnectionDetails(final IntegrationData data) {
        if (this.lmsIntegrationAPI == null) {
            return Result.ofError(
                    new UnsupportedOperationException("LMS Integration API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Create LMS connection details for LMSSetup: {}", lmsSetup());
        }

        return this.lmsAccessRequest.protectedRun(() -> this.lmsIntegrationAPI
                .applyConnectionDetails(data)
                .getOrThrow());
    }

    @Override
    public Result<ExamData> applyExamData(final ExamData examData) {
        if (this.lmsIntegrationAPI == null) {
            return Result.ofError(
                    new UnsupportedOperationException("LMS Integration API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Apply exam data: {} for LMSSetup: {}", examData, lmsSetup());
        }

        return this.applyExamDataRequest.protectedRun(() -> this.lmsIntegrationAPI
                .applyExamData(examData)
                .getOrThrow());
    }

    @Override
    public Result<String> deleteConnectionDetails() {
        if (this.lmsIntegrationAPI == null) {
            return Result.ofError(
                    new UnsupportedOperationException("LMS Integration API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Delete LMS connection details for LMSSetup: {}", lmsSetup());
        }

        return this.deleteLmsAccessRequest.protectedRun(() -> this.lmsIntegrationAPI.deleteConnectionDetails()
                .onError(error -> log.error(
                        "Failed to run protected deleteConnectionDetails: {}",
                        error.getMessage()))
                .getOrThrow());
    }

    @Override
    public Result<QuizData> getQuizDataForRemoteImport(final String examData) {
        if (this.lmsIntegrationAPI == null) {
            return Result.ofError(
                    new UnsupportedOperationException("LMS Integration API Not Supported For: " + getType().name()));
        }

        return this.lmsIntegrationAPI.getQuizDataForRemoteImport(examData);
    }

}
