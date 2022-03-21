/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface SEBRestrictionAPI {

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
