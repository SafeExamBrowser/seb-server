/*
 * Copyright (c) 2019 ETH Zürich, IT Services
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

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConnectionConfigurationChangeEvent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SebClientConfigRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConnectionConfigurationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@Tag(name = "SEB Client Configuration", description = "SEB client configuration management")
@RestController
@EnableAsync
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_CLIENT_CONFIG_ENDPOINT)
public class SEBClientConfigController extends ActivatableEntityController<SEBClientConfig, SEBClientConfig> {

    private final ConnectionConfigurationService sebConnectionConfigurationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Cryptor cryptor;

    public SEBClientConfigController(
            final SEBClientConfigDAO sebClientConfigDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final BulkActionService bulkActionService,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ConnectionConfigurationService sebConnectionConfigurationService,
            final ApplicationEventPublisher applicationEventPublisher, 
            final Cryptor cryptor) {

        super(authorization,
                bulkActionService,
                sebClientConfigDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.sebConnectionConfigurationService = sebConnectionConfigurationService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.cryptor = cryptor;
    }

    @Operation(operationId = "getSEBClientConfig", summary = "Get a single SEB client configuration by model ID")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not found")
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBClientConfig getBy(@Parameter(description = "The model identifier") @PathVariable final String modelId) {

        final SEBClientConfig config = this.entityDAO
                .byModelId(modelId)
                .flatMap(this::checkReadAccess)
                .getOrThrow();

        final CharSequence fallbackPassword = config.fallbackPassword != null 
                ? cryptor.decrypt(config.fallbackPassword).getOr(config.fallbackPassword) 
                : null;
        final CharSequence quitPassword = config.quitPassword != null
                ? cryptor.decrypt(config.quitPassword).getOr(config.quitPassword)
                : null;
        final CharSequence encryptSecret = config.encryptSecret != null
                ? cryptor.decrypt(config.encryptSecret).getOr(config.encryptSecret)
                : null;
        
        return new SEBClientConfig(
                config.id,
                config.institutionId,
                config.name,
                config.configPurpose,
                config.sebServerPingTime,
                config.vdiType,
                config.vdiExecutable,
                config.vdiPath,
                config.vdiArguments,
                config.fallback,
                config.fallbackStartURL,
                config.fallbackTimeout,
                config.fallbackAttempts,
                config.fallbackAttemptInterval,
                fallbackPassword,
                config.fallbackPasswordConfirm,
                quitPassword,
                config.quitPasswordConfirm,
                config.date,
                encryptSecret,
                config.encryptSecretConfirm,
                config.encryptCertificateAlias,
                config.encryptCertificateAsym,
                config.active,
                config.lastUpdateTime,
                config.lastUpdateUser,
                config.selectedExams);
    }

    @Operation(operationId = "getSEBClientConfigCredentials", summary = "Get the client credentials for a SEB client configuration")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not found")
    @RequestMapping(
            path = API.SEB_CLIENT_CONFIG_CREDENTIALS_PATH_SEGMENT + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ClientCredentials getClientCredentials(
            @Parameter(description = "The model identifier") @PathVariable final String modelId,
            @Parameter(description = "The institution identifier. Defaults to the current user's institution") @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        checkReadPrivilege(institutionId);

        return this.entityDAO.byModelId(modelId)
                .flatMap(this.authorization::checkWrite)
                .flatMap(config -> ((SEBClientConfigDAO) this.entityDAO).getSEBClientCredentials(modelId))
                .getOrThrow();
    }

    @Operation(operationId = "exportSEBClientConfig", summary = "Download and export the SEB client configuration file")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not found")
    @RequestMapping(
            path = API.SEB_CLIENT_CONFIG_DOWNLOAD_PATH_SEGMENT + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadSEBConfig(
            @Parameter(description = "The model identifier") @PathVariable final String modelId,
            @Parameter(description = "The institution identifier. Defaults to the current user's institution") @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @Parameter(description = "The exam identifier") @RequestParam(name = EXAM.ATTR_ID, required = false) final Long examId,
            final HttpServletResponse response) throws IOException {

        checkReadPrivilege(institutionId);

        this.entityDAO.byModelId(modelId)
                .flatMap(this.authorization::checkWrite)
                .map(this.userActivityLogDAO::logExport);

        final ServletOutputStream outputStream = response.getOutputStream();
        final PipedOutputStream pout;
        final PipedInputStream pin;
        try {
            pout = new PipedOutputStream();
            pin = new PipedInputStream(pout);

            this.sebConnectionConfigurationService.exportSEBClientConfiguration(
                    pout,
                    modelId,
                    examId);

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
            this.sebConnectionConfigurationService.initialCheckAccess(entity);
            // notify all
            applicationEventPublisher.publishEvent(new ConnectionConfigurationChangeEvent(
                    entity.institutionId,
                    entity.id));
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
