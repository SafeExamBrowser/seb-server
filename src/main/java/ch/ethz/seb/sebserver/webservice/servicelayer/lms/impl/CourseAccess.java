/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

public abstract class CourseAccess {

    private static final Logger log = LoggerFactory.getLogger(CourseAccess.class);

    public enum FetchStatus {
        ALL_FETCHED,
        ASYNC_FETCH_RUNNING,
        FETCH_ERROR
    }

    protected final CircuitBreaker<List<QuizData>> quizzesRequest;
    protected final CircuitBreaker<Chapters> chaptersRequest;
    protected final CircuitBreaker<ExamineeAccountDetails> accountDetailRequest;

    protected CourseAccess(
            final AsyncService asyncService,
            final Environment environment) {

        this.quizzesRequest = asyncService.createCircuitBreaker(
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
                        Constants.MINUTE_IN_MILLIS));

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
                        Constants.SECOND_IN_MILLIS * 10));
    }

    public Result<Collection<Result<QuizData>>> getQuizzesFromCache(final Set<String> ids) {
        return Result.tryCatch(() -> {
            final List<QuizData> cached = allQuizzesSupplier().getAllCached();
            final List<QuizData> available = (cached != null)
                    ? cached
                    : quizzesSupplier(ids).get();

            final Map<String, QuizData> quizMapping = available
                    .stream()
                    .collect(Collectors.toMap(q -> q.id, Function.identity()));

            if (!quizMapping.keySet().containsAll(ids)) {

                final Map<String, QuizData> collect = quizzesSupplier(ids).get()
                        .stream()
                        .collect(Collectors.toMap(qd -> qd.id, Function.identity()));
                if (collect != null) {
                    quizMapping.clear();
                    quizMapping.putAll(collect);
                }
            }

            return ids
                    .stream()
                    .map(id -> {
                        final QuizData q = quizMapping.get(id);
                        return (q == null)
                                ? Result.<QuizData> ofError(new NoSuchElementException("Quiz with id: " + id))
                                : Result.of(q);
                    })
                    .collect(Collectors.toList());
        });
    }

    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        return allQuizzesSupplier().getAll(filterMap);
    }

    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeSessionId) {
        return this.accountDetailRequest.protectedRun(accountDetailsSupplier(examineeSessionId));
    }

    public String getExamineeName(final String examineeSessionId) {
        return getExamineeAccountDetails(examineeSessionId)
                .map(ExamineeAccountDetails::getDisplayName)
                .onError(error -> log.warn("Failed to request user-name for ID: {}", error.getMessage(), error))
                .getOr(examineeSessionId);
    }

    protected Result<Chapters> getCourseChapters(final String courseId) {
        return this.chaptersRequest.protectedRun(getCourseChaptersSupplier(courseId));
    }

    /** NOTE: this returns a ExamineeAccountDetails with given examineeSessionId for default.
     * Override this if requesting account details is supported for specified LMS access.
     *
     * @param examineeSessionId
     * @return this returns a ExamineeAccountDetails with given examineeSessionId for default */
    protected Supplier<ExamineeAccountDetails> accountDetailsSupplier(final String examineeSessionId) {
        return () -> new ExamineeAccountDetails(
                examineeSessionId,
                examineeSessionId,
                examineeSessionId,
                examineeSessionId,
                Collections.emptyMap());
    }

    protected abstract Supplier<List<QuizData>> quizzesSupplier(final Set<String> ids);

    protected abstract AllQuizzesSupplier allQuizzesSupplier();

    protected abstract Supplier<Chapters> getCourseChaptersSupplier(final String courseId);

    protected abstract FetchStatus getFetchStatus();

    protected interface AllQuizzesSupplier {
        List<QuizData> getAllCached();

        Result<List<QuizData>> getAll(final FilterMap filterMap);
    }

}
