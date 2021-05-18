/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
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

    /** Reset and cleanup the caches if there are some */
    void cleanup();

    /** Get the specified LmsSetup model by primary key
     *
     * @param id The identifier (PK) of the LmsSetup model
     * @return Result refer to the LmsSetup model for specified id or to an error if happened */
    Result<LmsSetup> getLmsSetup(Long id);

    /** Get the specified LmsSetup model by modelId
     *
     * @param modelId The identifier (PK) of the LmsSetup model
     * @return Result refer to the LmsSetup model for specified id or to an error if happened */
    default Result<LmsSetup> getLmsSetup(final String modelId) {
        return Result.tryCatch(() -> getLmsSetup(Long.parseLong(modelId))
                .getOrThrow());
    }

    /** Used to get a specified page of QuizData from all active LMS Setup of the current users
     * institution, filtered by the given FilterMap.
     *
     * @param pageNumber the page number from the QuizData list to get
     * @param pageSize the page size
     * @param sort the sort parameter
     * @param filterMap the FilterMap containing all filter criteria
     * @return the specified Page of QuizData form all active LMS Setups of the current users institution */
    Result<Page<QuizData>> requestQuizDataPage(
            final int pageNumber,
            final int pageSize,
            final String sort,
            final FilterMap filterMap);

    /** Get a LmsAPITemplate for specified LmsSetup configuration by model identifier.
     *
     * @param lmsSetupId the identifier of LmsSetup
     * @return LmsAPITemplate for specified LmsSetup configuration */
    Result<LmsAPITemplate> getLmsAPITemplate(String lmsSetupId);

    /** use this to the the specified LmsAPITemplate.
     *
     * @param template the LmsAPITemplate
     * @return LmsSetupTestResult containing list of errors if happened */
    LmsSetupTestResult test(LmsAPITemplate template);

    /** This can be used to test an LmsSetup connection parameter without saving or heaving
     * an already persistent version of an LmsSetup.
     *
     * @param lmsSetup the LmsSetup instance
     * @return LmsSetupTestResult containing list of errors if happened */
    LmsSetupTestResult testAdHoc(LmsSetup lmsSetup);

    /** Get a LmsAPITemplate for specified LmsSetup configuration by primary key
     *
     * @param lmsSetupId the primary key of the LmsSetup
     * @return LmsAPITemplate for specified LmsSetup */
    default Result<LmsAPITemplate> getLmsAPITemplate(final Long lmsSetupId) {
        if (lmsSetupId == null) {
            return Result.ofError(new IllegalArgumentException("lmsSetupId has null-reference"));
        }
        return getLmsAPITemplate(String.valueOf(lmsSetupId));
    }

    /** Closure that gives a Predicate to filter a QuizzData on the criteria given by a FilterMap.
     * Now supports name and startTime filtering
     *
     * @param filterMap the FilterMap containing the filter criteria
     * @return true if the given QuizzData passes the filter */
    static Predicate<QuizData> quizFilterPredicate(final FilterMap filterMap) {
        if (filterMap == null) {
            return q -> true;
        }
        final String name = filterMap.getQuizName();
        final DateTime from = filterMap.getQuizFromTime();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        return q -> {
            final boolean nameFilter = StringUtils.isBlank(name) || (q.name != null && q.name.contains(name));
            final boolean startTimeFilter =
                    from == null || (q.startTime != null && (q.startTime.isEqual(from) || q.startTime.isAfter(from)));
            final DateTime endTime = now.isAfter(from) ? now : from;
            final boolean fromTimeFilter = (endTime == null || q.endTime == null || endTime.isBefore(q.endTime));
            return nameFilter && (startTimeFilter || fromTimeFilter);
        };
    }

    /** Closure that gives a Function to gets a list of QuizData and used the quizFilterFunction to filter this list
     * on the criteria given by a FilterMap.
     *
     * @param filterMap the FilterMap containing the filter criteria
     * @return filtered list of QuizData */
    static Function<List<QuizData>, List<QuizData>> quizzesFilterFunction(final FilterMap filterMap) {
        return quizzes -> quizzes
                .stream()
                .filter(quizFilterPredicate(filterMap))
                .collect(Collectors.toList());
    }

    /** Closure that gives a Function to create a Page of QuizData from a given List of QuizData with the
     * attributes, pageNumber, pageSize and sort.
     *
     * NOTE: this is not sorting the QuizData list but uses the sortAttribute for the page creation
     *
     * @param sortAttribute the sort attribute for the new Page
     * @param pageNumber the number of the Page to build
     * @param pageSize the size of the Page to build
     * @return A Page of QuizData extracted form a given list of QuizData */
    static Function<List<QuizData>, Page<QuizData>> quizzesToPageFunction(
            final String sortAttribute,
            final int pageNumber,
            final int pageSize) {

        return quizzes -> {
            if (quizzes.isEmpty()) {
                return new Page<>(0, 1, sortAttribute, Collections.emptyList());
            }

            int start = (pageNumber - 1) * pageSize;
            int end = start + pageSize;
            if (end > quizzes.size()) {
                end = quizzes.size();
            }
            if (start >= end) {
                start = end - pageSize;
                if (start < 0) {
                    start = 0;
                }

                return new Page<>(
                        (quizzes.size() <= pageSize) ? 1 : quizzes.size() / pageSize + 1,
                        start / pageSize + 1,
                        sortAttribute,
                        quizzes.subList(start, end));
            }

            final int mod = quizzes.size() % pageSize;
            return new Page<>(
                    (quizzes.size() <= pageSize)
                            ? 1
                            : (mod > 0)
                                    ? quizzes.size() / pageSize + 1
                                    : quizzes.size() / pageSize,
                    pageNumber,
                    sortAttribute,
                    quizzes.subList(start, end));
        };
    }

    /** Closure that gives a Function to sort a List of QuizData by a certain sort criteria.
     * The sort criteria is the name of the QuizData attribute plus a leading '-' sign for
     * descending sort order indication.
     *
     * @param sort the sort criteria ( ['-']{attributeName} )
     * @return A Function to sort a List of QuizData by a certain sort criteria */
    static Function<List<QuizData>, List<QuizData>> quizzesSortFunction(final String sort) {
        return quizzes -> {
            quizzes.sort(QuizData.getComparator(sort));
            return quizzes;
        };
    }

}
