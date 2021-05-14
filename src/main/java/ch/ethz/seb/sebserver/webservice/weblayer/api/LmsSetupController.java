/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import javax.validation.Valid;

import org.mybatis.dynamic.sql.SqlTable;
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
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.LMS_SETUP_ENDPOINT)
public class LmsSetupController extends ActivatableEntityController<LmsSetup, LmsSetup> {

    private final LmsAPIService lmsAPIService;
    final ApplicationEventPublisher applicationEventPublisher;

    public LmsSetupController(
            final LmsSetupDAO lmsSetupDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final BulkActionService bulkActionService,
            final LmsAPIService lmsAPIService,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ApplicationEventPublisher applicationEventPublisher) {

        super(authorization,
                bulkActionService,
                lmsSetupDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.lmsAPIService = lmsAPIService;
        this.applicationEventPublisher = applicationEventPublisher;
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
                PrivilegeType.MODIFY,
                EntityType.LMS_SETUP,
                institutionId);

        final LmsSetupTestResult result = this.lmsAPIService.getLmsAPITemplate(modelId)
                .map(this.lmsAPIService::test)
                .getOrThrow();

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

        final LmsSetupTestResult result = this.lmsAPIService.testAdHoc(lmsSetup);
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

}
