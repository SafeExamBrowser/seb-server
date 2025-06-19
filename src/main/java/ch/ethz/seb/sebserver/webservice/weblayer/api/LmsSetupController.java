/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import javax.validation.Valid;

import java.util.concurrent.Executor;

import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsTestService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import org.mybatis.dynamic.sql.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult.ErrorType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.LmsSetupChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.LMS_SETUP_ENDPOINT)
public class LmsSetupController extends ActivatableEntityController<LmsSetup, LmsSetup> {

    private static final Logger log = LoggerFactory.getLogger(LmsSetupController.class);

    private final LmsAPIService lmsAPIService;
    private final LmsTestService lmsTestService;
    private final SEBRestrictionService sebRestrictionService;
    private final FullLmsIntegrationService fullLmsIntegrationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Executor executor;

    public LmsSetupController(
            final LmsSetupDAO lmsSetupDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final BulkActionService bulkActionService,
            final LmsAPIService lmsAPIService,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final LmsTestService lmsTestService,
            final SEBRestrictionService sebRestrictionService,
            final FullLmsIntegrationService fullLmsIntegrationService,
            final ApplicationEventPublisher applicationEventPublisher,
            @Qualifier(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME) final Executor executor) {

        super(authorization,
                bulkActionService,
                lmsSetupDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.lmsAPIService = lmsAPIService;
        this.lmsTestService = lmsTestService;
        this.sebRestrictionService = sebRestrictionService;
        this.fullLmsIntegrationService = fullLmsIntegrationService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.executor = executor;
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return LmsSetupRecordDynamicSqlSupport.lmsSetupRecord;
    }

    @RequestMapping(
            path = API.LMS_SETUP_TEST_PATH_SEGMENT + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public LmsSetupTestResult testLms(
            @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable final Long modelId) {

        this.authorization.check(
                PrivilegeType.READ,
                EntityType.LMS_SETUP,
                institutionId);

        final LmsSetupTestResult result = this.lmsAPIService
                .getLmsAPITemplate(modelId)
                .map(this.lmsTestService::test)
                .onErrorDo(error -> {
                    final LmsType lmsType = this.entityDAO.byPK(modelId).get().lmsType;
                    return new LmsSetupTestResult(
                            lmsType,
                            new LmsSetupTestResult.Error(ErrorType.TEMPLATE_CREATION, error.getMessage()));
                })
                .get();

        if (result.missingLMSSetupAttribute != null && !result.missingLMSSetupAttribute.isEmpty()) {
            throw new APIMessageException(result.missingLMSSetupAttribute);
        }

        return result;
    }

    @RequestMapping(
            path = API.LMS_SETUP_TEST_PATH_SEGMENT + API.LMS_SETUP_TEST_AD_HOC_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public LmsSetupTestResult testLmsAdHoc(@Valid @RequestBody final LmsSetup lmsSetup) {

        this.authorization.checkModify(lmsSetup);

        final LmsSetupTestResult result = this.lmsTestService.testAdHoc(lmsSetup);
        if (result.missingLMSSetupAttribute != null && !result.missingLMSSetupAttribute.isEmpty()) {
            throw new APIMessageException(result.missingLMSSetupAttribute);
        }

        return result;
    }

    @Override
    protected LmsSetup createNew(final POSTMapper postParams) {

        final Long institutionId = postParams.getLong(
                Domain.LMS_SETUP.ATTR_INSTITUTION_ID);

        if (institutionId == null) {
            throw new APIConstraintViolationException("Institution identifier is missing");
        }

        return new LmsSetup(null, postParams);
    }

    @Override
    protected Result<LmsSetup> notifySaved(final LmsSetup entity, final Activatable.ActivationAction activation) {
        // TODO this triggers a whole bunch of updates when LMS Setup is been activated/deactivated that
        //      currently leads to GUI timeout when there are a lot of exams
        //      Solution, run this in a background task (and store errors when possible)
        //      involved: SEB Restriction, LmsAPI Service, FullLMSIntegration, Screen Proctoring, Proctoring, 
        this.applicationEventPublisher.publishEvent(new LmsSetupChangeEvent(entity, activation));
//        executor.execute(() -> {
//            try {
//                this.applicationEventPublisher.publishEvent(new LmsSetupChangeEvent(entity, activation));
//            } catch (final Exception e) {
//                log.error("Failed on publish LmsSetupChangeEvent: ", e);
//            }
//        });
        
        return super.notifySaved(entity, activation);
    }

    @Override
    protected Result<LmsSetup> validForDelete(final LmsSetup entity) {
        return Result.tryCatch(() -> {
            // if there is a SEB Restriction involved, release all SEB Restriction for exams
            if (entity.lmsType.features.contains(LmsSetup.Features.SEB_RESTRICTION)) {
                sebRestrictionService
                        .releaseAllRestrictionsOf(entity)
                        .getOrThrow();
            }
            // if there is a full LMS integration involved, delete it first on LMS
            if (entity.lmsType.features.contains(LmsSetup.Features.LMS_FULL_INTEGRATION)) {
                fullLmsIntegrationService
                        .deleteFullLmsIntegration(entity.id)
                        .getOrThrow();
            }

            return entity;
        });
    }
}
