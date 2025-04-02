/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.mockup;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService.IntegrationData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockupFullIntegration implements FullLmsIntegrationAPI {

    private static final Logger log = LoggerFactory.getLogger(MockupFullIntegration.class);

    @Override
    public boolean fullIntegrationActive() {
        return true;
    }

    @Override
    public LmsSetupTestResult testFullIntegrationAPI() {
        log.info("Test Full LMS Integration");
        return LmsSetupTestResult.ofOkay(LmsSetup.LmsType.MOCKUP);
    }

    @Override
    public Result<IntegrationData> applyConnectionDetails(final IntegrationData data) {
        return Result.tryCatch(() -> {
            
            log.info("Apply Connection Details: {}", data);
            
            // validation
            if (StringUtils.isBlank( data.id)) {
                throw new APIMessage.FieldValidationException("lmsFullIntegration:id", "id is mandatory");
            }
            if (StringUtils.isBlank( data.url)) {
                throw new APIMessage.FieldValidationException("lmsFullIntegration:url", "url is mandatory");
            }
            if (StringUtils.isBlank( data.access_token)) {
                throw new APIMessage.FieldValidationException("lmsFullIntegration:access_token", "access_token is mandatory");
            }

            return data;
        });
    }

    @Override
    public Result<String> applyExamData(final FullLmsIntegrationService.ExamData examData) {
        return Result.tryCatch(() -> {
            
            log.info("Apply Exam Data: {}", examData);
            
            if (StringUtils.isNotBlank(examData.next_course_id )) {
                // return test example if quizContextId to create download URL
                return "123";
            }
            return "0";
        });
        //return Result.ofRuntimeError("Not Supported");
    }

    @Override
    public Result<String> deleteConnectionDetails() {
        log.info("Delete Connection Details");
        return Result.of("0");
    }

    @Override
    public Result<QuizData> getQuizDataForRemoteImport(final String examData) {
        return Result.ofRuntimeError("Not Supported");
    }
}
