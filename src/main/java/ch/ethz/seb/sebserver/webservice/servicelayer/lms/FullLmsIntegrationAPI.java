/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService.IntegrationData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService.ExamData;

public interface FullLmsIntegrationAPI {

    boolean fullIntegrationActive();

    /** Performs a test for the underling {@link LmsSetup } configuration and checks if the
     * LMS and the full LMS integration API of the LMS can be accessed or if there are some difficulties,
     * missing API functions
     *
     * @return {@link LmsSetupTestResult } instance with the test result report */
    LmsSetupTestResult testFullIntegrationAPI();

    Result<IntegrationData> applyConnectionDetails(IntegrationData data);

    Result<ExamData> applyExamData(ExamData examData);
    
    Result<String> deleteConnectionDetails();

    Result<QuizData> getQuizDataForRemoteImport(String examData);


}
