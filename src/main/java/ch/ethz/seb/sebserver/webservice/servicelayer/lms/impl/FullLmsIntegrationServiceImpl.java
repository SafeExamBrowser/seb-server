/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.io.OutputStream;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class FullLmsIntegrationServiceImpl implements FullLmsIntegrationService {
    @Override
    public Result<LmsAPITemplate> getLmsAPITemplate(final String lmsUUID) {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Void> refreshAccessToken(final String lmsUUID) {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Void> applyFullLmsIntegration(final Long lmsSetupId) {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Void> deleteFullLmsIntegration(final Long lmsSetupId) {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Map<String, String>> getExamTemplateSelection() {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Exam> importExam(
            final String lmsUUID,
            final String courseId,
            final String quizId,
            final String examTemplateId,
            final String quitPassword,
            final String quitLink) {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<EntityKey> deleteExam(
            final String lmsUUID,
            final String courseId,
            final String quizId) {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Void> streamConnectionConfiguration(
            final String lmsUUID,
            final String courseId,
            final String quizId,
            final OutputStream out) {
        return Result.ofRuntimeError("TODO");
    }

    private Long findLMSSetup(final String lmsUUID) {
        // TODO
        return null;
    }
}
