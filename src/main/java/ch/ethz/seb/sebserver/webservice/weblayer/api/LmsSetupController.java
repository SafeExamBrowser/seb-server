/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;


import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import org.mybatis.dynamic.sql.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.LmsSetupChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.LMS_SETUP_ENDPOINT)
public class LmsSetupController extends ActivatableEntityController<LmsSetup, LmsSetup> {

    private static final Logger log = LoggerFactory.getLogger(LmsSetupController.class);

    private final LmsAPIService lmsAPIService;
    private final LmsTestService lmsTestService;
    private final LmsLiveCycleService lmsLiveCycleService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Cryptor cryptor;

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
            final ScreenProctoringService screenProctoringService,
            final LmsLiveCycleService lmsLiveCycleService,
            final ApplicationEventPublisher applicationEventPublisher,
            final Cryptor cryptor) {

        super(authorization,
                bulkActionService,
                lmsSetupDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.lmsAPIService = lmsAPIService;
        this.lmsTestService = lmsTestService;
        this.lmsLiveCycleService = lmsLiveCycleService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.cryptor = cryptor;
    }

    @Override
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public LmsSetup getBy(@PathVariable final String modelId) {

        LmsSetup lmsSetup = this.entityDAO
                .byModelId(modelId)
                .flatMap(this::checkReadAccess)
                .getOrThrow();
        
        return new LmsSetup(
            lmsSetup.id,
            lmsSetup.institutionId,
            lmsSetup.name,
            lmsSetup.lmsType,
            lmsSetup.lmsAuthName,
            (lmsSetup.lmsAuthSecret != null) 
                    ? String.valueOf(cryptor.decrypt(lmsSetup.lmsAuthSecret).getOr(lmsSetup.lmsAuthSecret))
                    : null,
            lmsSetup.lmsApiUrl,
                (lmsSetup.lmsRestApiToken != null)
                        ? String.valueOf(cryptor.decrypt(lmsSetup.lmsRestApiToken).getOr(lmsSetup.lmsRestApiToken))
                        : null,
            lmsSetup.proxyHost,
            lmsSetup.proxyPort,
            lmsSetup.proxyAuthUsername,
                (lmsSetup.proxyAuthSecret != null)
                        ? String.valueOf(cryptor.decrypt(lmsSetup.proxyAuthSecret).getOr(lmsSetup.proxyAuthSecret))
                        : null,
            lmsSetup.active,
            lmsSetup.updateTime,
            lmsSetup.connectionId ,
            lmsSetup.integrationActive
        );
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return LmsSetupRecordDynamicSqlSupport.lmsSetupRecord;
    }

    @RequestMapping(
            path = API.LMS_SETUP_TEST_PATH_SEGMENT + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
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
                            new LmsSetupTestResult.LmsSetupError(ErrorType.TEMPLATE_CREATION, error.getMessage()));
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
    protected Result<LmsSetup> validForSave(final LmsSetup entity) {
        return super
                .validForSave(entity)
                .map(lmsSetup -> {
                    final LmsSetup existingLMSSetup = this.entityDAO.byPK(lmsSetup.id).getOrThrow();
                    if (existingLMSSetup.isActive()) {
                        // apply ad hoc test with new settings. If not valid deny save
                        LmsSetup adHocLMSSetup = new LmsSetup(
                                null,
                                existingLMSSetup.institutionId,
                                lmsSetup.name,
                                existingLMSSetup.lmsType,
                                lmsSetup.lmsAuthName,
                                lmsSetup.lmsAuthSecret,
                                lmsSetup.lmsApiUrl,
                                lmsSetup.lmsRestApiToken,
                                lmsSetup.proxyHost,
                                lmsSetup.proxyPort,
                                lmsSetup.proxyAuthUsername,
                                lmsSetup.proxyAuthSecret,
                                false,
                                null,
                                existingLMSSetup.connectionId,
                                existingLMSSetup.integrationActive
                        );
                        final LmsSetupTestResult result = this.lmsTestService.testAdHoc(adHocLMSSetup);
                        if (result.hasAnyError()) {
                            log.warn("Deny active LMS Setup save because of connection errors. LMS Setup {}, Errors: {}", adHocLMSSetup, result);
                            if (!result.missingLMSSetupAttribute.isEmpty()) {
                                throw new APIMessageException(result.missingLMSSetupAttribute);
                            } else if (result.hasError(ErrorType.TOKEN_REQUEST)) {
                                throw new APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of("Failed to gain access to LMS"));
                            } else {
                                throw new APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of(result.errors.toString()));
                            }
                        }
                    }

                    return lmsSetup;
                });
    }

    @Override
    protected Result<LmsSetup> notifySaved(final LmsSetup entity, final Activatable.ActivationAction activation) {
        // NOTE: When this save notification is called, the save (or activation/deactivation) has already happened
        //       In the case of deactivation this means the involved exams are already marked as inactive
        //       We need to handle activation and deactivation in different order especially if the full integration
        //       is involved since we have to make sure that the LMS integration is deleted after all
        //       post processes that still need the integration, has been processed. And on activation, we need
        //       the full integration activated first before all post processes that need it.
        //
        //       Therefore, we differentiate the cases here and for save-only case we use the usual Event publisher.
        if (activation == Activatable.ActivationAction.ACTIVATE) {
            lmsLiveCycleService
                    .processPostLmsActivation(entity.id)
                    .onErrorHandle(error -> new APIMessage.APIMessageException(
                            APIMessage.ErrorMessage.UNEXPECTED.of(
                                    error,
                                    "Failed to deactivate LMS Setup due to unexpected error"))
                    )
                    .getOrThrow();
        } else if (activation == Activatable.ActivationAction.DEACTIVATE) {
            lmsLiveCycleService
                    .processPostLmsDeactivation(entity.id)
                    .onErrorHandle(error -> new APIMessage.APIMessageException(
                            APIMessage.ErrorMessage.UNEXPECTED.of(
                                    error,
                                    "Failed to deactivate LMS Setup due to unexpected error"))
                    )
                    .getOrThrow();
        }

        // Finally publish the event for all generic listeners that are interested in it
        this.applicationEventPublisher.publishEvent(new LmsSetupChangeEvent(entity, activation));

        return super.notifySaved(entity, activation);
    }

    @Override
    protected Result<LmsSetup> validForDelete(final LmsSetup entity) {
        return Result.tryCatch(() -> {
            if (entity.isActive()) {
                throw new APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of(
                        "The Assessment Tool must be inactive bevor deletion. Please deactivate it first."));
            }
            return entity;
        });
    }
}
