/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;
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
     * LMS and the core API of the LMS can be accessed or if there are some difficulties,
     * missing configuration data or connection/authentication errors.
     *
     * @return LmsSetupTestResult instance with the test result report */
    LmsSetupTestResult testLmsSetup();

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
    Collection<Result<QuizData>> getQuizzes(Set<String> ids);

    default Result<QuizData> getQuiz(final String id) {
        if (StringUtils.isBlank(id)) {
            return Result.ofError(new RuntimeException("missing model id"));
        }

        return getQuizzes(new HashSet<>(Arrays.asList(id)))
                .stream()
                .findFirst()
                .orElse(Result.ofError(new ResourceNotFoundException(EntityType.EXAM, id)));
    }

    // TODO this can be used in a future release to resolve examinee's account detail information by an
    //      examinee identifier received by on SEB-Client connection.
    //Result<ExamineeAccountDetails> getExamineeAccountDetails(String examineeUserId);

    default List<APIMessage> attributeValidation(final ClientCredentials credentials) {

        final LmsSetup lmsSetup = lmsSetup();
        // validation of LmsSetup
        final List<APIMessage> missingAttrs = new ArrayList<>();
        if (StringUtils.isBlank(lmsSetup.lmsApiUrl)) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_URL,
                    "lmsSetup:lmsUrl:notNull"));
        }
        if (!credentials.hasClientId()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTNAME,
                    "lmsSetup:lmsClientname:notNull"));
        }
        if (!credentials.hasSecret()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTSECRET,
                    "lmsSetup:lmsClientsecret:notNull"));
        }
        return missingAttrs;
    }

}
