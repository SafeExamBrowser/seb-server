/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

/** Defines the LMS API access service interface with all functionality needed to access
 * a LMS API within a given LmsSetup configuration.
 *
 * There are LmsAPITemplate implementations for each type of supported LMS that are managed
 * in reference to a LmsSetup configuration within this service. This means actually that
 * this service caches requested LmsAPITemplate (that holds the LMS API connection) as long
 * as there is no change in the underling LmsSetup configuration. If the LmsSetup configuration
 * changes this service will be notifies about the change and release the related LmsAPITemplate from cache. */
public interface LmsAPIService {

    Result<Page<QuizData>> requestQuizDataPage(
            final int pageNumber,
            final int pageSize,
            final String sort,
            final FilterMap filterMap);

    /** Get a LmsAPITemplate for specified LmsSetup configuration.
     *
     * @param lmsSetupId the identifier of LmsSetup
     * @return LmsAPITemplate for specified LmsSetup configuration */
    Result<LmsAPITemplate> getLmsAPITemplate(String lmsSetupId);

    default Result<LmsAPITemplate> getLmsAPITemplate(final Long lmsSetupId) {
        if (lmsSetupId == null) {
            return Result.ofError(new IllegalArgumentException("lmsSetupId has null-reference"));
        }
        return getLmsAPITemplate(String.valueOf(lmsSetupId));
    }

    public static Predicate<QuizData> quizzeFilterFunction(final FilterMap filterMap) {
        final String name = filterMap.getName();
        final DateTime from = filterMap.getQuizFromTime();
        return q -> (StringUtils.isBlank(name) || (q.name != null && q.name.contains(name)))
                && (from == null) || (q.startTime != null && q.startTime.isBefore(from));
    }

    public static Function<List<QuizData>, List<QuizData>> quizzesFilterFunction(final FilterMap filterMap) {
        filterMap.getName();
        return quizzes -> quizzes
                .stream()
                .filter(quizzeFilterFunction(filterMap))
                .collect(Collectors.toList());
    }

    public static Function<List<QuizData>, Page<QuizData>> quizzesToPageFunction(
            final String sort,
            final int pageNumber,
            final int pageSize) {

        return quizzes -> {
            final int start = pageNumber * pageSize;
            int end = start + pageSize;
            if (end > quizzes.size() - 1) {
                end = quizzes.size() - 1;
            }

            return new Page<>(quizzes.size() / pageSize, pageNumber, sort, quizzes.subList(start, end));
        };
    }

    public static Function<List<QuizData>, List<QuizData>> quizzesSortFunction(final String sort) {
        return quizzes -> {
            quizzes.sort(QuizData.getComparator(sort));
            return quizzes;
        };
    }

}
