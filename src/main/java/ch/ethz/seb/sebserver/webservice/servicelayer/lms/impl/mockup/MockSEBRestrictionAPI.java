/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.mockup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<Long, SEBRestriction> restrictionDB = new ConcurrentHashMap<>();

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        return LmsSetupTestResult.ofOkay(LmsType.MOCKUP);
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {
        log.info("Get SEB Client restriction for Exam: {}", exam);
        if (!restrictionDB.containsKey(exam.id)) {
            return Result.ofError(new NoSEBRestrictionException());
        } else {
            return Result.of(restrictionDB.get(exam.id));
        }
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(
            final Exam exam,
            final SEBRestriction sebRestrictionData) {

        log.info("Apply SEB Client restriction: {}", sebRestrictionData);
        restrictionDB.put(exam.id, sebRestrictionData);
        return Result.of(sebRestrictionData);
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {
        log.info("Release SEB Client restriction for Exam: {}", exam);
        if (restrictionDB.containsKey(exam.id)) {
            SEBRestriction sebRestriction = restrictionDB.get(exam.id);
            restrictionDB.put(
                    exam.id,
                    new SEBRestriction(
                            exam.id,
                            null,
                            null,
                            sebRestriction.additionalProperties,
                            sebRestriction.warningMessage));
        }
        return Result.of(exam);
    }

}
