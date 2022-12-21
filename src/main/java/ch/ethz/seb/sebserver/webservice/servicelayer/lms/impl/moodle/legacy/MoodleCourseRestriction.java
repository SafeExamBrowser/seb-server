/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionAPI;

/** Dummy Implementation */
public class MoodleCourseRestriction implements SEBRestrictionAPI {

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        return LmsSetupTestResult.ofQuizAccessAPIError(LmsType.MOODLE, "SEB restriction not supported");
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {
        return Result.ofError(new UnsupportedOperationException("SEB restriction not supported"));
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(final Exam exam, final SEBRestriction sebRestrictionData) {
        return Result.ofError(new UnsupportedOperationException("SEB restriction not supported"));
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {
        return Result.ofError(new UnsupportedOperationException("SEB restriction not supported"));
    }

}
