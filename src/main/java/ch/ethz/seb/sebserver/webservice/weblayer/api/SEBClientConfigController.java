/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SebClientConfigRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ClientConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@EnableAsync
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_CLIENT_CONFIG_ENDPOINT)
public class SEBClientConfigController extends ActivatableEntityController<SEBClientConfig, SEBClientConfig> {

    private final ClientConfigService sebClientConfigService;

    public SEBClientConfigController(
            final SEBClientConfigDAO sebClientConfigDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final BulkActionService bulkActionService,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ClientConfigService sebClientConfigService) {

        super(authorization,
                bulkActionService,
                sebClientConfigDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.sebClientConfigService = sebClientConfigService;
    }

    @RequestMapping(
            path = API.SEB_CLIENT_CONFIG_DOWNLOAD_PATH_SEGMENT + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadSEBConfig(
            @PathVariable final String modelId,
            final HttpServletResponse response) throws IOException {

        this.entityDAO.byModelId(modelId)
                .flatMap(this.authorization::checkWrite)
                .map(this.userActivityLogDAO::logExport);

        final ServletOutputStream outputStream = response.getOutputStream();
        PipedOutputStream pout;
        PipedInputStream pin;
        try {
            pout = new PipedOutputStream();
            pin = new PipedInputStream(pout);

            this.sebClientConfigService.exportSEBClientConfiguration(
                    pout,
                    modelId);

            IOUtils.copyLarge(pin, outputStream);

            response.setStatus(HttpStatus.OK.value());

            outputStream.flush();

        } finally {
            outputStream.flush();
            outputStream.close();
        }
    }

    @Override
    protected SEBClientConfig createNew(final POSTMapper postParams) {

        final Long institutionId = postParams.getLong(
                Domain.SEB_CLIENT_CONFIGURATION.ATTR_INSTITUTION_ID);

        if (institutionId == null) {
            throw new APIConstraintViolationException("Institution identifier is missing");
        }

        postParams.putIfAbsent(
                Domain.SEB_CLIENT_CONFIGURATION.ATTR_DATE,
                DateTime.now(DateTimeZone.UTC).toString(Constants.DEFAULT_DATE_TIME_FORMAT));

        return new SEBClientConfig(institutionId, postParams);
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return SebClientConfigRecordDynamicSqlSupport.sebClientConfigRecord;
    }

    @Override
    protected Result<SEBClientConfig> validForCreate(final SEBClientConfig entity) {
        return super.validForCreate(entity)
                .map(this::checkPasswordMatch);
    }

    @Override
    protected Result<SEBClientConfig> validForSave(final SEBClientConfig entity) {
        return super.validForSave(entity)
                .map(this::checkPasswordMatch);
    }

    @Override
    protected Result<SEBClientConfig> notifySaved(final SEBClientConfig entity) {
        if (entity.isActive()) {
            // try to get access token for SEB client
            this.sebClientConfigService.initalCheckAccess(entity);
        }
        return super.notifySaved(entity);
    }

    private SEBClientConfig checkPasswordMatch(final SEBClientConfig entity) {
        final Collection<APIMessage> errors = new ArrayList<>();
        if (entity.hasEncryptionSecret() && !entity.encryptSecret.equals(entity.encryptSecretConfirm)) {
            errors.add(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.SEB_CLIENT_CONFIGURATION.TYPE_NAME,
                            PasswordChange.ATTR_NAME_PASSWORD,
                            "clientConfig:confirm_encrypt_secret:password.mismatch")));
        }

        if (entity.hasFallbackPassword() && !entity.fallbackPassword.equals(entity.fallbackPasswordConfirm)) {
            errors.add(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.SEB_CLIENT_CONFIGURATION.TYPE_NAME,
                            SEBClientConfig.ATTR_FALLBACK_PASSWORD_CONFIRM,
                            "clientConfig:sebServerFallbackPasswordHashConfirm:password.mismatch")));
        }

        if (entity.hasQuitPassword() && !entity.quitPassword.equals(entity.quitPasswordConfirm)) {
            errors.add(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.SEB_CLIENT_CONFIGURATION.TYPE_NAME,
                            SEBClientConfig.ATTR_QUIT_PASSWORD_CONFIRM,
                            "clientConfig:hashedQuitPasswordConfirm:password.mismatch")));
        }

        if (BooleanUtils.isTrue(entity.fallback) && StringUtils.isBlank(entity.fallbackStartURL)) {
            errors.add(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.SEB_CLIENT_CONFIGURATION.TYPE_NAME,
                            SEBClientConfig.ATTR_FALLBACK_START_URL,
                            "clientConfig:startURL:notNull")));
        }

        if (BooleanUtils.isTrue(entity.fallback) && entity.fallbackTimeout == null) {
            errors.add(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.SEB_CLIENT_CONFIGURATION.TYPE_NAME,
                            SEBClientConfig.ATTR_FALLBACK_TIMEOUT,
                            "clientConfig:sebServerFallbackTimeout:notNull")));
        }

        if (BooleanUtils.isTrue(entity.fallback) && entity.fallbackAttempts == null) {
            errors.add(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.SEB_CLIENT_CONFIGURATION.TYPE_NAME,
                            SEBClientConfig.ATTR_FALLBACK_ATTEMPTS,
                            "clientConfig:sebServerFallbackAttempts:notNull")));
        }

        if (BooleanUtils.isTrue(entity.fallback) && entity.fallbackAttemptInterval == null) {
            errors.add(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.SEB_CLIENT_CONFIGURATION.TYPE_NAME,
                            SEBClientConfig.ATTR_FALLBACK_ATTEMPT_INTERVAL,
                            "clientConfig:sebServerFallbackAttemptInterval:notNull")));
        }

        if (!errors.isEmpty()) {
            throw new APIMessage.APIMessageException(errors);
        }

        return entity;
    }

}
