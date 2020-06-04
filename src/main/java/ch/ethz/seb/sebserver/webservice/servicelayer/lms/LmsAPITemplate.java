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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;

/** Defines the interface to an LMS within a specified LMSSetup configuration.
 * There is one concrete implementations for every supported type of LMS like
 * Open edX or Moodle
 *
 * A LmsAPITemplate defines at least the core API access to query courses and quizzes from the LMS
 * Later a concrete LmsAPITemplate may also implement some special features regarding to the type
 * of the LMS */
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

    /** Get a Result of an unsorted List of filtered QuizData from the LMS course/quiz API
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
    default Collection<Result<QuizData>> getQuizzes(final Set<String> ids) {
        return getQuizzes(new FilterMap())
                .getOrElse(Collections::emptyList)
                .stream()
                .filter(quiz -> ids.contains(quiz.id))
                .map(Result::of)
                .collect(Collectors.toList());
    }

    /** Get all QuizData for the set of QuizData identifiers from LMS API in a collection
     * of Result. If particular Quiz cannot be loaded because of errors or deletion,
     * the Result will have an error reference.
     *
     * NOTE: This method looks first in the cache for all given ids.
     * If all quizzes are cached, returns all from cache.
     * If one quiz is not in the cache, requests all quizzes from the API and refreshes the cache
     *
     * @param ids the Set of Quiz identifiers to get the QuizData for
     * @return Collection of all QuizData from the given id set */
    Collection<Result<QuizData>> getQuizzesFromCache(Set<String> ids);

    default Result<QuizData> getQuiz(final String id) {
        if (StringUtils.isBlank(id)) {
            return Result.ofError(new RuntimeException("missing model id"));
        }

        return getQuizzes(new HashSet<>(Arrays.asList(id)))
                .stream()
                .findFirst()
                .orElse(Result.ofError(new ResourceNotFoundException(EntityType.EXAM, id)));
    }

    Result<QuizData> getQuizFromCache(String id);

    // TODO this can be used in a future release to resolve examinee's account detail information by an
    //      examinee identifier received by on SEB-Client connection.
    //Result<ExamineeAccountDetails> getExamineeAccountDetails(String examineeUserId);

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

    /** Get SEB restriction data form LMS within a SEBRestrictionData instance if available
     * or a ResourceNotFoundException if not yet available or restricted
     *
     * @param exam the exam to get the SEB restriction data for
     * @return Result refer to the SEBRestrictionData instance or to an ResourceNotFoundException if restriction is
     *         missing or to another exception on unexpected error case */
    Result<SEBRestriction> getSEBClientRestriction(Exam exam);

    /** Applies a SEB Client restriction within the LMS with the given attributes.
     *
     * @param externalExamId The exam identifier from LMS side (Exam.externalId)
     * @param sebRestrictionData containing all data for SEB Client restriction
     * @return Result refer to the given SEBRestrictionData if restriction was successful or to an error if not */
    Result<SEBRestriction> applySEBClientRestriction(
            String externalExamId,
            SEBRestriction sebRestrictionData);

    /** Releases an already applied SEB Client restriction within the LMS for a given Exam.
     * This completely removes the SEB Client restriction on LMS side.
     *
     * @param exam the Exam to release the restriction for
     * @return Result refer to the given Exam if successful or to an error if not */
    Result<Exam> releaseSEBClientRestriction(Exam exam);

}
