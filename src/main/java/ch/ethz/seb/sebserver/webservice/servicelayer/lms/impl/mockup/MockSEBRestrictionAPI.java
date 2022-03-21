/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.mockup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.NoSEBRestrictionException;

public class MockSEBRestrictionAPI implements SEBRestrictionAPI {

    private static final Logger log = LoggerFactory.getLogger(MockSEBRestrictionAPI.class);

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        return LmsSetupTestResult.ofQuizRestrictionAPIError(LmsType.MOCKUP, "unsupported");
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {
        log.info("Apply SEB Client restriction for Exam: {}", exam);
        return Result.ofError(new NoSEBRestrictionException());
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(
            final String externalExamId,
            final SEBRestriction sebRestrictionData) {

        log.info("Apply SEB Client restriction: {}", sebRestrictionData);
        return Result.of(sebRestrictionData);
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {
        log.info("Release SEB Client restriction for Exam: {}", exam);
        return Result.of(exam);
    }

}
