/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.io.OutputStream;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface FullLmsIntegrationService {

    Result<LmsAPITemplate> getLmsAPITemplate(String lmsUUID);

    Result<Void> refreshAccessToken(String lmsUUID);

    Result<Void> applyFullLmsIntegration(Long lmsSetupId);

    Result<Void> deleteFullLmsIntegration(Long lmsSetupId);

    Result<Map<String, String>> getExamTemplateSelection();

    Result<Exam> importExam(
            String lmsUUID,
            String courseId,
            String quizId,
            String examTemplateId,
            String quitPassword,
            String quitLink);

    Result<EntityKey> deleteExam(
            String lmsUUID,
            String courseId,
            String quizId);

    Result<Void> streamConnectionConfiguration(
            String lmsUUID,
            String courseId,
            String quizId,
            OutputStream out);
}
