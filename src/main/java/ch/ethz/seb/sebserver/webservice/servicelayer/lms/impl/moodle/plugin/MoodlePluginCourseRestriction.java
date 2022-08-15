/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionAPI;

public class MoodlePluginCourseRestriction implements SEBRestrictionAPI {

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(final String externalExamId,
            final SEBRestriction sebRestrictionData) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {
        // TODO Auto-generated method stub
        return null;
    }

}
