/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

public interface CourseAccessAPI {

    Logger log = LoggerFactory.getLogger(CourseAccessAPI.class);

    /** Fetch status that indicates an asynchronous quiz data fetch status if the
     * concrete implementation has such. */
    public enum FetchStatus {
        ALL_FETCHED,
        ASYNC_FETCH_RUNNING,
        FETCH_ERROR
    }

    /** Performs a test for the underling {@link LmsSetup } configuration and checks if the
     * LMS and the course API of the LMS can be accessed or if there are some difficulties,
     * missing configuration data or connection/authentication errors.
     *
     * @return {@link LmsSetupTestResult } instance with the test result report */
    LmsSetupTestResult testCourseAccessAPI();

    /** To make a quick course API access check without report that just throws an exception if not available */
    default void checkCourseAPIAccess() {
        final LmsSetupTestResult testCourseAccessAPI = this.testCourseAccessAPI();
        if (!testCourseAccessAPI.isOk()) {
            throw new RuntimeException("No course API Access: " + testCourseAccessAPI);
        }
    }

    void fetchQuizzes(FilterMap filterMap, AsyncQuizFetchBuffer asyncQuizFetchBuffer);

    /** Get all {@link QuizData } for the set of {@link QuizData } identifiers from LMS API in a collection
     * of Result. If particular quizzes cannot be loaded because of errors or deletion,
     * the the referencing QuizData will not be in the resulting list and an error is logged.
     *
     * @param ids the Set of Quiz identifiers to get the {@link QuizData } for
     * @return Collection of all {@link QuizData } from the given id set */
    Result<Collection<QuizData>> getQuizzes(Set<String> ids);

    /** Get the quiz data with specified identifier.
     *
     * @param id the quiz data identifier
     * @return Result refer to the quiz data or to an error when happened */
    Result<QuizData> getQuiz(final String id);

    /** Tries to recover dangling exam that has lost its quiz data with the id mapping.
     *
     * @param exam The dangling exam to try to recover
     * @return Result referring to the recovered QuizData or to an error when happened */
    Result<QuizData> tryRecoverQuizForExam(Exam exam);

    /** Clears the underling caches if there are some for a particular implementation. */
    void clearCourseCache();

    /** Convert an anonymous or temporary examineeUserId, sent by the SEB Client on LMS login,
     * to LMS examinee account details by requesting them on the LMS API with the given examineeUserId
     *
     * @param examineeUserId the examinee user identifier derived from SEB Client
     * @return a Result refer to the {@link ExamineeAccountDetails } instance or to an error when happened or not
     *         supported */
    Result<ExamineeAccountDetails> getExamineeAccountDetails(String examineeUserId);

    /** Used to convert an anonymous or temporary examineeUserId, sent by the SEB Client on LMS login,
     * to a readable LMS examinee account name by requesting this on the LMS API with the given examineeUserId.
     *
     * If the underling concrete template implementation does not support this user name conversion,
     * the given examineeSessionId shall be returned.
     *
     * @param examineeUserId the examinee user identifier derived from SEB Client
     * @return a user account display name if supported or the given examineeSessionId if not. */
    String getExamineeName(final String examineeUserId);

    /** Used to get a list of chapters (display name and chapter-identifier) that can be used to
     * apply chapter-based SEB restriction for a specified course.
     *
     * The availability of this depends on the type of LMS and on installed plugins that supports this feature.
     * If this is not supported by the underling LMS a UnsupportedOperationException will be presented
     * within the Result.
     *
     * @param courseId The course identifier
     * @return Result referencing to the Chapters model for the given course or to an error when happened. */
    Result<Chapters> getCourseChapters(String courseId);

    /** This is used to buffer fetch results of asynchronous LMS quiz data fetch processes.
     * An asynchronous LMS quiz data fetch processes will buffer its fetch results within this buffer
     * during processing and a request can get already buffered results on a none-blocking manner.
     *
     * Use it like a Future but with the ability to get already fetched data. */
    static class AsyncQuizFetchBuffer {

        /** The buffer set where already fetched data is stored and can be get */
        public Set<QuizData> buffer = new HashSet<>();
        /** Indicates whether the asynchronous fetch is still running or has finished */
        public boolean finished = false;
        /** Indicates if the fetch is been canceled. Set this to true to cancel the asynchronous process */
        public boolean canceled = false;
        /** Reference to an error when the asynchronous fetch stopped with an error */
        public Exception error = null;

        public void finish() {
            this.finished = true;
        }

        public void finish(final Exception error) {
            this.error = error;
            finish();
        }

        public void cancel() {
            this.canceled = true;
        }

    }

}
