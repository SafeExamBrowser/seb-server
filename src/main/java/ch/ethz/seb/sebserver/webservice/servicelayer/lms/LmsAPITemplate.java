/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;

/** Defines an LMS API access template to build SEB Server LMS integration.
 *
 * A LMS integration consists of two main parts so far:
 * - The course API to search and request course data from LMS as well as resolve some LMS account details for a given
 * examineeId
 * - The SEB restriction API to apply SEB restriction data to the LMS to restrict a certain course for SEB
 *
 * A LmsAPITemplate is been constructed within a LmsSetup that defines the LMS setup data that is needed to connect to
 * a specific LMS instance of implemented type.
 *
 * The enum LmsSetup.LmsType defines the supported LMS types and for each type the supported API part(s).
 *
 * SEB Server uses the test functions that are defined for each LMS API part to test API access for a certain LMS
 * instance respectively the underling LMSSetup. Concrete implementations can do various tests to check full
 * or partial API Access and can flag missing or wrong LMSSetup attributes with the resulting LmsSetupTestResult.
 *
 * SEB Server than uses an instance of this template to communicate with the an LMS. */
public interface LmsAPITemplate {

    /** Get the underling LMSSetup configuration for this LmsAPITemplate
     *
     * @return the underling LMSSetup configuration for this LmsAPITemplate */
    LmsSetup lmsSetup();

    /** Performs a test for the underling LmsSetup configuration and checks if the
     * LMS and the course API of the LMS can be accessed or if there are some difficulties,
     * missing configuration data or connection/authentication errors.
     *
     * @return LmsSetupTestResult instance with the test result report */
    LmsSetupTestResult testCourseAccessAPI();

    /** Performs a test for the underling LmsSetup configuration and checks if the
     * LMS and the course restriction API of the LMS can be accessed or if there are some difficulties,
     * missing configuration data or connection/authentication errors.
     *
     * @return LmsSetupTestResult instance with the test result report */
    LmsSetupTestResult testCourseRestrictionAPI();

    /** Get an unsorted List of filtered QuizData from the LMS course/quiz API
     *
     * @param filterMap the FilterMap to get a filtered result. For possible filter attributes
     *            see documentation on QuizData
     * @return Result of an unsorted List of filtered QuizData from the LMS course/quiz API
     *         or refer to an error when happened */
    Result<List<QuizData>> getQuizzes(FilterMap filterMap);

    /** Get all QuizData for the set of QuizData identifiers from LMS API in a collection
     * of Result. If particular Quiz cannot be loaded because of errors or deletion,
     * the Result will have an error reference.
     *
     * @param ids the Set of Quiz identifiers to get the QuizData for
     * @return Collection of all QuizData from the given id set */
    Collection<Result<QuizData>> getQuizzes(Set<String> ids);

    /** Get the quiz data with specified identifier.
     *
     * Default implementation: Uses getQuizzes(Set<String> ids) and returns the first matching or an error.
     *
     * @param id the quiz data identifier
     * @return Result refer to the quiz data or to an error when happened */
    default Result<QuizData> getQuiz(final String id) {
        if (StringUtils.isBlank(id)) {
            return Result.ofError(new RuntimeException("missing model id"));
        }

        return getQuizzes(new HashSet<>(Arrays.asList(id)))
                .stream()
                .findFirst()
                .orElse(Result.ofError(new ResourceNotFoundException(EntityType.EXAM, id)));
    }

    /** Get all QuizData for the set of QuizData-identifiers (ids) from the LMS defined within the
     * underling LMSSetup, in a collection of Results.
     *
     * If there is caching involved this function shall try to get the data from the cache first.
     *
     * NOTE: This function depends on the specific LMS implementation and on whether caching the quiz data
     * makes sense or not. Following strategy is recommended:
     * Looks first in the cache if the whole set of QuizData can be get from the cache.
     * If all quizzes are cached, returns all from cache.
     * If one or more quiz is not in the cache, requests all quizzes from the API and refreshes the cache
     *
     * @param ids the Set of Quiz identifiers to get the QuizData for
     * @return Collection of all QuizData from the given id set */
    Collection<Result<QuizData>> getQuizzesFromCache(Set<String> ids);

    /** Convert an anonymous or temporary examineeUserId, sent by the SEB Client on LMS login,
     * to LMS examinee account details by requesting them on the LMS API with the given examineeUserId
     *
     * @param examineeUserId the examinee user identifier derived from SEB Client
     * @return a Result refer to the ExamineeAccountDetails instance or to an error when happened or not supported */
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

    /** Get SEB restriction data form LMS within a SEBRestrictionData instance. The available restriction details
     * depends on the type of LMS but shall at least contains the config-key(s) and the browser-exam-key(s).
     *
     * @param exam the exam to get the SEB restriction data for
     * @return Result refer to the SEBRestrictionData instance or to an ResourceNotFoundException if the restriction is
     *         missing or to another exception on unexpected error case */
    Result<SEBRestriction> getSEBClientRestriction(Exam exam);

    /** Applies SEB Client restrictions to the LMS with the given attributes.
     *
     * @param externalExamId The exam/course identifier from LMS side (Exam.externalId)
     * @param sebRestrictionData containing all data for SEB Client restriction to apply to the LMS
     * @return Result refer to the given SEBRestrictionData if restriction was successful or to an error if not */
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
