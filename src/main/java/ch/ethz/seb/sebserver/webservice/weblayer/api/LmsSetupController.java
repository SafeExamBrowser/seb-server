/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + API.LMS_SETUP_ENDPOINT)
public class LmsSetupController extends ActivatableEntityController<LmsSetup, LmsSetup> {

    private final LmsAPIService lmsAPIService;

    public LmsSetupController(
            final LmsSetupDAO lmsSetupDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final BulkActionService bulkActionService,
            final LmsAPIService lmsAPIService,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService) {

        super(authorization,
                bulkActionService,
                lmsSetupDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.lmsAPIService = lmsAPIService;
    }

    @Override
    protected Class<LmsSetup> modifiedDataType() {
        return LmsSetup.class;
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return LmsSetupRecordDynamicSqlSupport.lmsSetupRecord;
    }

    @RequestMapping(
            path = API.SEB_CONFIG_EXPORT_PATH_SEGMENT + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE) // TODO check if this is the right format
    public void downloadSEBConfig(
            @PathVariable final Long modelId,
            final HttpServletResponse response) throws Exception {

        this.authorization.check(PrivilegeType.WRITE, EntityType.LMS_SETUP);

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setStatus(HttpStatus.OK.value());

        final InputStream sebConfigFileIn = this.lmsAPIService
                .createSEBStartConfiguration(modelId)
                .getOrThrow();

        IOUtils.copyLarge(sebConfigFileIn, response.getOutputStream());
        response.flushBuffer();
    }

    @RequestMapping(
            path = API.LMS_SETUP_TEST_PATH_SEGMENT + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public LmsSetupTestResult connectionReport(@PathVariable final Long modelId) {

        this.authorization.check(PrivilegeType.MODIFY, EntityType.LMS_SETUP);

        return this.lmsAPIService.createLmsAPITemplate(modelId)
                .map(template -> template.testLmsSetup())
                .getOrThrow();
    }

    @Override
    protected LmsSetup createNew(final POSTMapper postParams) {
        return new LmsSetup(null, postParams);
    }

//    @Override
//    protected Result<LmsSetup> validForCreate(final LmsSetup entity) {
//
//        final SEBClientAuth clientAuth = new SEBClientAuth(entity.sebAuthName, entity.sebAuthSecret);
//
//        final Result<LmsSetup> result = super.validForCreate(entity);
//        if (result.hasError()) {
//            final Throwable error = result.getError();
//            if (error instanceof BeanValidationException) {
//                final BeanValidationException beanValidationException = (BeanValidationException) error;
//                final Result<SEBClientAuth> validateSebAuth = this.beanValidationService.validateBean(clientAuth);
//                if (validateSebAuth.hasError()) {
//                    final Throwable sebAuthError = validateSebAuth.getError();
//                    if (sebAuthError instanceof BeanValidationException) {
//                        final BindingResult bindingResult = beanValidationException.getBindingResult();
//                        bindingResult.addAllErrors(((BeanValidationException) sebAuthError).getBindingResult());
//                        return Result.ofError(new BeanValidationException(bindingResult));
//                    } else {
//                        return validateSebAuth
//                                .map(ce -> entity);
//                    }
//                }
//            }
//            return result;
//        } else {
//            return this.beanValidationService.validateBean(clientAuth)
//                    .map(ca -> entity);
//        }
//    }
//
//    @Override
//    protected Result<LmsSetup> validForSave(final LmsSetup entity) {
//        return super.validForSave(entity)
//                .map(setup -> {
//
//                    if (StringUtils.isNoneBlank(entity.sebAuthName)
//                            || StringUtils.isNoneBlank(entity.sebAuthSecret)) {
//
//                        throw new IllegalAPIArgumentException(
//                                "SEB Client Authentication cannot be changed after creation");
//                    }
//                    return setup;
//                });
//    }

    public static final class SEBClientAuth {

        @NotNull(message = "lmsSetup:sebClientname:notNull")
        @Size(min = 3, max = 255, message = "lmsSetup:sebClientname:size:{min}:{max}:${validatedValue}")
        public final String sebAuthName;

        @NotNull(message = "lmsSetup:sebClientsecret:notNull")
        @Size(min = 8, max = 255, message = "lmsSetup:sebClientsecret:size:{min}:{max}:${validatedValue}")
        public final String sebAuthSecret;

        protected SEBClientAuth(final String authName, final String authSecret) {

            this.sebAuthName = authName;
            this.sebAuthSecret = authSecret;
        }
    }

}
