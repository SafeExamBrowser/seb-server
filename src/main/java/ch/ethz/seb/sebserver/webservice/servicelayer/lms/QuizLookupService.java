/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

public interface QuizLookupService {

    boolean isLookupRunning();

    /** Used to get a specified page of QuizData from all active LMS Setup of the current users
     * institution, filtered by the given FilterMap.
     *
     * @param pageNumber the page number from the QuizData list to get
     * @param pageSize the page size
     * @param sort the sort parameter
     * @param filterMap the FilterMap containing all filter criteria
     * @return the specified Page of QuizData from all active LMS Setups of the current users institution */
    Result<Page<QuizData>> requestQuizDataPage(
            final int pageNumber,
            final int pageSize,
            final String sort,
            final FilterMap filterMap,
            Function<String, Result<LmsAPITemplate>> lmsAPITemplateSupplier);

    void clear();

    void clear(Long institutionId);

    default LookupResult emptyLookupResult() {
        return new LookupResult(Collections.emptyList(), true);
    }

    public final static class LookupResult {
        public final List<QuizData> quizData;
        public final boolean completed;
        public final long timestamp;

        public LookupResult(
                final List<QuizData> quizData,
                final boolean completed) {

            this.quizData = quizData;
            this.completed = completed;
            this.timestamp = Utils.getMillisecondsNow();
        }
    }

    /** Closure that gives a Function to create a Page of QuizData from a given List of QuizData with the
     * attributes, pageNumber, pageSize and sort.
     *
     * NOTE: this is not sorting the QuizData list but uses the sortAttribute for the page creation
     *
     * @param sortAttribute the sort attribute for the new Page
     * @param pageNumber the number of the Page to build
     * @param pageSize the size of the Page to build
     * @param complete indicates if the quiz lookup that uses this page function has been completed yet
     * @return A Page of QuizData extracted from a given list of QuizData */
    static Function<LookupResult, Page<QuizData>> quizzesToPageFunction(
            final String sortAttribute,
            final int pageNumber,
            final int pageSize) {

        return lookupResult -> {
            final List<QuizData> quizzes = lookupResult.quizData;
            if (quizzes.isEmpty()) {
                return new Page<>(0, 1, pageSize, sortAttribute, Collections.emptyList(), lookupResult.completed);
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
                        pageSize,
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
                    pageSize,
                    sortAttribute,
                    quizzes.subList(start, end),
                    lookupResult.completed);
        };
    }

    /** Closure that gives a Function to sort a List of QuizData by a certain sort criteria.
     * The sort criteria is the name of the QuizData attribute plus a leading '-' sign for
     * descending sort order indication.
     *
     * @param sort the sort criteria ( ['-']{attributeName} )
     * @return A Function to sort a List of QuizData by a certain sort criteria */
    static Function<LookupResult, LookupResult> quizzesSortFunction(final String sort) {
        return lookupResult -> {
            lookupResult.quizData.sort(QuizData.getComparator(sort));
            return lookupResult;
        };
    }

}
