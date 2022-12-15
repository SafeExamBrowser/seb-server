/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.CourseAccessAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionAPI;

public class LmsAPITemplateAdapter implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(LmsAPITemplateAdapter.class);

    private final CourseAccessAPI courseAccessAPI;
    private final SEBRestrictionAPI sebRestrictionAPI;
    private final APITemplateDataSupplier apiTemplateDataSupplier;

    /** CircuitBreaker for protected lmsTestRequest */
    private final CircuitBreaker<LmsSetupTestResult> lmsTestRequest;
    /** CircuitBreaker for protected quiz and course data requests */
    private final CircuitBreaker<Collection<QuizData>> quizzesRequest;
    /** CircuitBreaker for protected quiz and course data requests */
    private final CircuitBreaker<QuizData> quizRequest;
    /** CircuitBreaker for protected chapter data requests */
    private final CircuitBreaker<Chapters> chaptersRequest;
    /** CircuitBreaker for protected examinee account details requests */
    private final CircuitBreaker<ExamineeAccountDetails> accountDetailRequest;

    private final CircuitBreaker<SEBRestriction> restrictionRequest;
    private final CircuitBreaker<Exam> releaseRestrictionRequest;

    public LmsAPITemplateAdapter(
            final AsyncService asyncService,
            final Environment environment,
            final APITemplateDataSupplier apiTemplateDataSupplier,
            final CourseAccessAPI courseAccessAPI,
            final SEBRestrictionAPI sebRestrictionAPI) {

        this.courseAccessAPI = courseAccessAPI;
        this.sebRestrictionAPI = sebRestrictionAPI;
        this.apiTemplateDataSupplier = apiTemplateDataSupplier;

        this.lmsTestRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.lmsTestRequest.attempts",
                        Integer.class,
                        2),
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
                        1),
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
                        1),
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
                        1),
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
                        1),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.sebrestriction.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.sebrestriction.timeToRecover",
                        Long.class,
                        0L));

        this.releaseRestrictionRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.sebrestriction.attempts",
                        Integer.class,
                        2),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.sebrestriction.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.sebrestriction.timeToRecover",
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
    public LmsSetupTestResult testCourseAccessAPI() {
        if (this.courseAccessAPI != null) {
            if (log.isDebugEnabled()) {
                log.debug("Test Course Access API for LMSSetup: {}", lmsSetup());
            }

            return this.lmsTestRequest.protectedRun(() -> this.courseAccessAPI.testCourseAccessAPI())
                    .onError(error -> log.error(
                            "Failed to run protectedQuizzesRequest: {}",
                            error.getMessage()))
                    .getOrThrow();
        }

        return LmsSetupTestResult.ofAPINotSupported(getType());
    }

    @Override
    @Deprecated
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {

        if (this.courseAccessAPI == null) {
            return Result
                    .ofError(new UnsupportedOperationException("Course API Not Supported For: " + getType().name()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Get quizzes for LMSSetup: {}", lmsSetup());
        }

        return this.courseAccessAPI
                .getQuizzes(filterMap)
                .onError(error -> log.error(
                        "Failed to run protectedQuizzesRequest: {}",
                        error.getMessage()));
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

        if (log.isDebugEnabled()) {
            log.debug("Test course restriction API for LMSSetup: {}", lmsSetup());
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
                .onError(error -> {
                    if (error instanceof NoSEBRestrictionException) {
                        return;
                    }
                    log.error("Failed to get SEB restrictions: {}", error.getMessage());
                })
                .getOrThrow());
    }

    @Override
    public boolean hasSEBClientRestriction(final Exam exam) {
        final Result<SEBRestriction> sebClientRestriction = getSEBClientRestriction(exam);
        if (sebClientRestriction.hasError()) {
            return false;
        }

        return this.sebRestrictionAPI.hasSEBClientRestriction(sebClientRestriction.get());
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

        return this.releaseRestrictionRequest.protectedRun(() -> this.sebRestrictionAPI
                .releaseSEBClientRestriction(exam)
                .onError(error -> log.error(
                        "Failed to release SEB restrictions: {}",
                        error.getMessage()))
                .getOrThrow());
    }

}
