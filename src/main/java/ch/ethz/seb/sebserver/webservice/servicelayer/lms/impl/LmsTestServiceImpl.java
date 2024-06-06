/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplateCacheService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class LmsTestServiceImpl implements LmsTestService {

    private static final Logger log = LoggerFactory.getLogger(LmsTestServiceImpl.class);
    private final LmsSetupDAO lmsSetupDAO;
    private final LmsAPITemplateCacheService lmsAPITemplateCacheService;
    private final FullLmsIntegrationService fullLmsIntegrationService;

    public LmsTestServiceImpl(
            final LmsSetupDAO lmsSetupDAO,
            final LmsAPITemplateCacheService lmsAPITemplateCacheService,
            final FullLmsIntegrationService fullLmsIntegrationService) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.lmsAPITemplateCacheService = lmsAPITemplateCacheService;
        this.fullLmsIntegrationService = fullLmsIntegrationService;
    }

    @Override
    public LmsSetupTestResult test(final LmsAPITemplate template) {
        final LmsSetupTestResult testCourseAccessAPI = template.testCourseAccessAPI();
        if (!testCourseAccessAPI.isOk()) {
            lmsAPITemplateCacheService.clearCache(template.lmsSetup().getModelId());
            return testCourseAccessAPI;
        }

        if (template.lmsSetup().getLmsType().features.contains(LmsSetup.Features.SEB_RESTRICTION)) {
            final LmsSetupTestResult lmsSetupTestResult = template.testCourseRestrictionAPI();
            if (!lmsSetupTestResult.isOk()) {
                lmsAPITemplateCacheService.clearCache(template.lmsSetup().getModelId());
                return lmsSetupTestResult;
            }
        }

        if (template.lmsSetup().getLmsType().features.contains(LmsSetup.Features.LMS_FULL_INTEGRATION)) {
            final Long lmsSetupId = template.lmsSetup().id;
            final LmsSetupTestResult lmsSetupTestResult = template.testFullIntegrationAPI();
            if (!lmsSetupTestResult.isOk()) {
                lmsAPITemplateCacheService.clearCache(template.lmsSetup().getModelId());
                this.lmsSetupDAO
                        .setIntegrationActive(lmsSetupId, false)
                        .onError(er -> log.error("Failed to mark LMS integration inactive", er));
                return lmsSetupTestResult;
            } else {

                final Result<FullLmsIntegrationService.IntegrationData> integrationDataResult = fullLmsIntegrationService
                        .applyFullLmsIntegration(template.lmsSetup().id);

                if (integrationDataResult.hasError()) {
                    return LmsSetupTestResult.ofFullIntegrationAPIError(
                            template.lmsSetup().lmsType,
                            "Failed to apply full LMS integration");
                }
            }
        }

        return LmsSetupTestResult.ofOkay(template.lmsSetup().getLmsType());
    }

    @Override
    public LmsSetupTestResult testAdHoc(final LmsSetup lmsSetup) {

        final Result<LmsAPITemplate> createLmsSetupTemplate = lmsAPITemplateCacheService
                .createInMemoryLmsAPITemplate(lmsSetup);
        if (createLmsSetupTemplate.hasError()) {
            return new LmsSetupTestResult(
                    lmsSetup.lmsType,
                    new LmsSetupTestResult.Error(LmsSetupTestResult.ErrorType.TEMPLATE_CREATION,
                            createLmsSetupTemplate.getError().getMessage()));

        }
        final LmsAPITemplate lmsSetupTemplate = createLmsSetupTemplate.get();

        final LmsSetupTestResult testCourseAccessAPI = lmsSetupTemplate.testCourseAccessAPI();
        if (!testCourseAccessAPI.isOk()) {
            return testCourseAccessAPI;
        }

        final LmsSetup.LmsType lmsType = lmsSetupTemplate.lmsSetup().getLmsType();
        if (lmsType.features.contains(LmsSetup.Features.SEB_RESTRICTION)) {
            final LmsSetupTestResult lmsSetupTestResult = lmsSetupTemplate.testCourseRestrictionAPI();
            if (!lmsSetupTestResult.isOk()) {
                return lmsSetupTestResult;
            }
        }

        if (lmsType.features.contains(LmsSetup.Features.LMS_FULL_INTEGRATION)) {
            final LmsSetupTestResult lmsSetupTestResult = lmsSetupTemplate.testFullIntegrationAPI();
            if (!lmsSetupTestResult.isOk()) {
                return lmsSetupTestResult;
            }
        }

        return LmsSetupTestResult.ofOkay(lmsSetupTemplate.lmsSetup().getLmsType());
    }
}
