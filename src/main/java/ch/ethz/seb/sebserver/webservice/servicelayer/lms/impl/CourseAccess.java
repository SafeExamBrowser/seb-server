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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.async.MemoizingCircuitBreaker;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;

public abstract class CourseAccess {

    protected final MemoizingCircuitBreaker<List<QuizData>> allQuizzesSupplier;

    protected CourseAccess(final AsyncService asyncService) {
        this.allQuizzesSupplier = asyncService.createMemoizingCircuitBreaker(
                allQuizzesSupplier(),
                3,
                Constants.MINUTE_IN_MILLIS,
                Constants.MINUTE_IN_MILLIS,
                true,
                Constants.HOUR_IN_MILLIS);
    }

    public Result<QuizData> getQuizFromCache(final String id) {
        return Result.tryCatch(() -> this.allQuizzesSupplier
                .getChached()
                .stream()
                .filter(qd -> id.equals(qd.id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No cached quiz: " + id)));
    }

    public Result<Collection<Result<QuizData>>> getQuizzesFromCache(final Set<String> ids) {
        return Result.tryCatch(() -> {
            final List<QuizData> cached = this.allQuizzesSupplier.getChached();
            if (cached == null) {
                throw new RuntimeException("No cached quizzes");
            }

            final Map<String, QuizData> cacheMapping = cached
                    .stream()
                    .collect(Collectors.toMap(q -> q.id, Function.identity()));

            if (!cacheMapping.keySet().containsAll(ids)) {
                throw new RuntimeException("Not all requested quizzes cached");
            }

            return ids
                    .stream()
                    .map(id -> {
                        final QuizData q = cacheMapping.get(id);
                        return (q == null)
                                ? Result.<QuizData> ofError(new NoSuchElementException("Quiz with id: " + id))
                                : Result.of(q);
                    })
                    .collect(Collectors.toList());
        });
    }

    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        return this.allQuizzesSupplier.get()
                .map(LmsAPIService.quizzesFilterFunction(filterMap));
    }

    protected abstract Supplier<List<QuizData>> allQuizzesSupplier();

}
