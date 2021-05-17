/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.AbstractCachedCourseAccess;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.AbstractCourseAccess;

/** Defines an LMS API access template to build SEB Server LMS integration.
 * </p>
 * A LMS integration consists of two main parts so far:
 * </p>
 *
 * <pre>
 * - The course API to search and request course data from LMS as well as resolve some
 *   LMS account details for a given examineeId.
 * - The SEB restriction API to apply SEB restriction data to the LMS to restrict a
 *   certain course for SEB.
 * </pre>
 * </p>
 *
 * <b>Course API</b></br>
 * All course API requests of this template shall not block and return as fast as possible
 * with the best result it can provide for the time on that the request was made.
 * </p>
 * Each request to a remote LMS shall be executed within a protected call such that the
 * request don't block the API call as well as do not attack the remote LMS with endless
 * requests on failure.</br>
 * Therefore the abstract class {@link AbstractCourseAccess} defines protected calls
 * for different API calls by using {@link CircuitBreaker}. documentation on the class for
 * more information.
 * </p>
 * Since the course API requests course data from potentially thousands of existing and
 * active courses, the course API can implement some short time caches if needed.</br>
 * The abstract class {@link AbstractCachedCourseAccess} defines such a short time
 * cache for all implementing classes using EH-Cache. See documentation on the class for
 * more information.
 * </p>
 * <b>SEB restriction API</b></br>
 * For this API we need no caching since this is mostly about pushing data to the LMS for the LMS
 * to use. But this calls sahl also be protected within some kind of circuit breaker pattern to
 * avoid blocking on long latency.
 * </p>
 * </p>
 * A {@link LmsAPITemplate } will be constructed within the application with a {@link LmsSetup } instances.
 * The application constructs a {@link LmsAPITemplate } for each type of LMS setup when needed or requested and
 * there is not already a cached template or the cached template is out of date.</br>
 * The {@link LmsSetup } defines the data that is needed to connect to a specific LMS instance of implemented type
 * and is wrapped within a {@link LmsAPITemplate } instance that lives as long as there are no changes to the
 * {@link LmsSetup and the {@link LmsSetup } that is wrapped within the {@link LmsAPITemplate } is up to date.
 * <p>
 * The enum {@link LmsSetup.LmsType } defines the supported LMS types and for each type the supported API part(s).
 * <p>
 * The application uses the test functions that are defined for each LMS API part to test API access for a certain LMS
 * instance respectively the underling {@link LmsSetup }. Concrete implementations can do various tests to check full
 * or partial API Access and can flag missing or wrong {@link LmsSetup } attributes with the resulting
 * {@link LmsSetupTestResult }.</br>
 * SEB Server than uses an instance of this template to communicate with the an LMS. */
public interface LmsAPITemplate {

    /** Get the LMS type of the concrete template implementation
     *
     * @return the LMS type of the concrete template implementation */
    LmsSetup.LmsType getType();

    /** Get the underling {@link LmsSetup } configuration for this LmsAPITemplate
     *
     * @return the underling {@link LmsSetup } configuration for this LmsAPITemplate */
    LmsSetup lmsSetup();

    // *******************************************************************
    // **** Course API functions *****************************************

    /** Performs a test for the underling {@link LmsSetup } configuration and checks if the
     * LMS and the course API of the LMS can be accessed or if there are some difficulties,
     * missing configuration data or connection/authentication errors.
     *
     * @return {@link LmsSetupTestResult } instance with the test result report */
    LmsSetupTestResult testCourseAccessAPI();

    /** Get an unsorted List of filtered {@link QuizData } from the LMS course/quiz API
     *
     * @param filterMap the {@link FilterMap } to get a filtered result. Possible filter attributes are:
     *
     *            <pre>
     *      {@link QuizData.FILTER_ATTR_QUIZ_NAME } The quiz name filter text (exclude all names that do not contain the given text)
     *      {@link QuizData.FILTER_ATTR_START_TIME } The quiz start time (exclude all quizzes that starts before)
     *            </pre>
     *
     * @return Result of an unsorted List of filtered {@link QuizData } from the LMS course/quiz API
     *         or refer to an error when happened */
    Result<List<QuizData>> getQuizzes(FilterMap filterMap);

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

    /** Clears the underling caches if there are some for a particular implementation. */
    void clearCache();

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
    String getExamineeName(String examineeUserId);

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

    // ****************************************************************************
    // **** SEB restriction API functions *****************************************

    /** Performs a test for the underling {@link LmsSetup } configuration and checks if the
     * LMS and the course restriction API of the LMS can be accessed or if there are some difficulties,
     * missing configuration data or connection/authentication errors.
     *
     * @return {@link LmsSetupTestResult } instance with the test result report */
    LmsSetupTestResult testCourseRestrictionAPI();

    /** Get SEB restriction data form LMS within a {@link SEBRestrictionData } instance. The available restriction
     * details
     * depends on the type of LMS but shall at least contains the config-key(s) and the browser-exam-key(s).
     *
     * @param exam the exam to get the SEB restriction data for
     * @return Result refer to the {@link SEBRestrictionData } instance or to an ResourceNotFoundException if the
     *         restriction is
     *         missing or to another exception on unexpected error case */
    Result<SEBRestriction> getSEBClientRestriction(Exam exam);

    /** Applies SEB Client restrictions to the LMS with the given attributes.
     *
     * @param externalExamId The exam/course identifier from LMS side (Exam.externalId)
     * @param sebRestrictionData containing all data for SEB Client restriction to apply to the LMS
     * @return Result refer to the given {@link SEBRestrictionData } if restriction was successful or to an error if
     *         not */
    Result<SEBRestriction> applySEBClientRestriction(
            String externalExamId,
            SEBRestriction sebRestrictionData);

    /** Releases an already applied SEB Client restriction within the LMS for a given Exam.
     * This completely removes the SEB Client restriction on LMS side.
     *
     * @param exam the Exam to release the restriction for.
     * @return Result refer to the given Exam if successful or to an error if not */
    Result<Exam> releaseSEBClientRestriction(Exam exam);

}
