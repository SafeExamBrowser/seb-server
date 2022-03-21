/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

/** A partial course access API implementation that uses CircuitBreaker to apply LMS
 * API requests in a protected environment.
 *
 * Extend this to implement a concrete course access API for a given type of LMS. */
public abstract class AbstractCourseAccess {

    private static final Logger log = LoggerFactory.getLogger(AbstractCourseAccess.class);

    /** CircuitBreaker for protected quiz and course data requests */
    protected final CircuitBreaker<List<QuizData>> allQuizzesRequest;
    /** CircuitBreaker for protected quiz and course data requests */
    protected final CircuitBreaker<Collection<QuizData>> quizzesRequest;
    /** CircuitBreaker for protected quiz and course data requests */
    protected final CircuitBreaker<QuizData> quizRequest;
    /** CircuitBreaker for protected chapter data requests */
    protected final CircuitBreaker<Chapters> chaptersRequest;
    /** CircuitBreaker for protected examinee account details requests */
    protected final CircuitBreaker<ExamineeAccountDetails> accountDetailRequest;

    protected AbstractCourseAccess(
            final AsyncService asyncService,
            final Environment environment) {

        this.allQuizzesRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.attempts",
                        Integer.class,
                        3),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.blockingTime",
                        Long.class,
                        Constants.MINUTE_IN_MILLIS),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.timeToRecover",
                        Long.class,
                        Constants.MINUTE_IN_MILLIS));

        this.quizzesRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.attempts",
                        Integer.class,
                        3),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.timeToRecover",
                        Long.class,
                        Constants.MINUTE_IN_MILLIS));

        this.quizRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.attempts",
                        Integer.class,
                        3),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.quizzesRequest.timeToRecover",
                        Long.class,
                        Constants.MINUTE_IN_MILLIS));

        this.chaptersRequest = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.chaptersRequest.attempts",
                        Integer.class,
                        3),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.chaptersRequest.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 10),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.chaptersRequest.timeToRecover",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 30));

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
                        Constants.SECOND_IN_MILLIS * 30));
    }

    public Result<List<QuizData>> protectedQuizzesRequest(final FilterMap filterMap) {
        return this.allQuizzesRequest.protectedRun(allQuizzesSupplier(filterMap))
                .onError(error -> log.error(
                        "Failed to run protectedQuizzesRequest: {}",
                        error.getMessage()));
    }

    public Result<Collection<QuizData>> protectedQuizzesRequest(final Set<String> ids) {
        return this.quizzesRequest.protectedRun(quizzesSupplier(ids))
                .onError(error -> log.error(
                        "Failed to run protectedQuizzesRequest: {}",
                        error.getMessage()));
    }

    public Result<QuizData> protectedQuizRequest(final String id) {
        return this.quizRequest.protectedRun(quizSupplier(id))
                .onError(error -> log.error(
                        "Failed to run protectedQuizRequest: {}",
                        error.getMessage()));
    }

    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeSessionId) {
        final Supplier<ExamineeAccountDetails> accountDetailsSupplier = accountDetailsSupplier(examineeSessionId);
        return this.accountDetailRequest.protectedRun(() -> {
            try {
                return accountDetailsSupplier.get();
            } catch (final Exception e) {
                log.error("Unexpected error while trying to get examinee account details: ", e);
                throw e;
            }
        });
    }

    /** Default implementation that uses getExamineeAccountDetails to geht the examinee name
     *
     * @param examineeSessionId
     * @return The examinee account name for the given examineeSessionId */
    public String getExamineeName(final String examineeSessionId) {
        return getExamineeAccountDetails(examineeSessionId)
                .map(ExamineeAccountDetails::getDisplayName)
                .onError(error -> log.warn("Failed to request user-name for ID: {}", error.getMessage(), error))
                .getOr(examineeSessionId);
    }

    public Result<Chapters> getCourseChapters(final String courseId) {
        return this.chaptersRequest.protectedRun(getCourseChaptersSupplier(courseId))
                .onError(error -> log.error(
                        "Failed to run getCourseChapters: {}",
                        error.getMessage()));
    }

    protected abstract Supplier<ExamineeAccountDetails> accountDetailsSupplier(final String examineeSessionId);

    /** Provides a supplier to supply request to use within the circuit breaker */
    protected abstract Supplier<List<QuizData>> allQuizzesSupplier(final FilterMap filterMap);

    /** Provides a supplier for the quiz data request to use within the circuit breaker */
    protected abstract Supplier<Collection<QuizData>> quizzesSupplier(final Set<String> ids);

    /** Provides a supplier for the quiz data request to use within the circuit breaker */
    protected abstract Supplier<QuizData> quizSupplier(final String id);

    /** Provides a supplier for the course chapter data request to use within the circuit breaker */
    protected abstract Supplier<Chapters> getCourseChaptersSupplier(final String courseId);

}
