/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.mockup;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService.IntegrationData;

public class MockupFullIntegration implements FullLmsIntegrationAPI {

    @Override
    public LmsSetupTestResult testFullIntegrationAPI() {
        return LmsSetupTestResult.ofAPINotSupported(LmsSetup.LmsType.MOODLE_PLUGIN);
    }

    @Override
    public Result<IntegrationData> applyConnectionDetails(final IntegrationData data) {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<FullLmsIntegrationService.ExamData> applyExamData(final FullLmsIntegrationService.ExamData examData) {
        return Result.ofRuntimeError("Not Supported");
    }

    @Override
    public Result<Exam> applyConnectionConfiguration(final Exam exam, final byte[] configData) {
        return Result.ofRuntimeError("Not Supported");
    }

    @Override
    public Result<String> deleteConnectionDetails() {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<QuizData> getQuizDataForRemoteImport(final String examData) {
        return Result.ofRuntimeError("Not Supported");
    }
}
