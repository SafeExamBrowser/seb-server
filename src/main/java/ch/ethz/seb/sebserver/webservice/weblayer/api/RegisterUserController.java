/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import ch.ethz.seb.sebserver.gbl.model.user.*;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.TooManyRequests;
import ch.ethz.seb.sebserver.gbl.model.Domain.USER_ROLE;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;
import io.github.bucket4j.local.LocalBucket;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.REGISTER_ENDPOINT)
public class RegisterUserController {

    private final UserActivityLogDAO userActivityLogDAO;
    private final UserDAO userDAO;
    private final BeanValidationService beanValidationService;
    private final ScreenProctoringService screenProctoringService;
    private final LocalBucket requestRateLimitBucket;
    private final LocalBucket createRateLimitBucket;
    private final boolean registeringEnabled;
    private final boolean autoActivation;

    protected RegisterUserController(
            final InstitutionDAO institutionDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final UserDAO userDAO,
            final BeanValidationService beanValidationService,
            final RateLimitService rateLimitService,
            final WebserviceInfo webserviceInfo,
            final ScreenProctoringService screenProctoringService,
            @Qualifier(WebSecurityConfig.USER_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder userPasswordEncoder) {
        
        final Map<String, Boolean> features = webserviceInfo.configuredFeatures();
        this.userActivityLogDAO = userActivityLogDAO;
        this.userDAO = userDAO;
        this.beanValidationService = beanValidationService;
        this.registeringEnabled = BooleanUtils.isTrue(features.get(UserFeatures.Feature.ADMIN_USER_ACCOUNT_SELF_REGISTERING.featureName));
        this.autoActivation = BooleanUtils.isTrue(features.get(UserFeatures.Feature.ADMIN_USER_ACCOUNT_SELF_REGISTERING_AUTO_ACTIVATION.featureName));
        this.requestRateLimitBucket = rateLimitService.createRequestLimitBucker();
        this.createRateLimitBucket = rateLimitService.createCreationLimitBucker();
        this.screenProctoringService = screenProctoringService;
    }

    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UserInfo registerNewUser(
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request) {

        if (!registeringEnabled) {
            throw new RuntimeException("Registering is not enabled from backend!");
        }

        if (!this.requestRateLimitBucket.tryConsume(1)) {
            throw new TooManyRequests();
        }

        final POSTMapper postMap = new POSTMapper(allRequestParams, request.getQueryString())
                .putIfAbsent(USER_ROLE.REFERENCE_NAME, UserRole.EXAM_SUPPORTER.name());
        final UserMod userMod = new UserMod(null, postMap);

        return this.beanValidationService.validateBean(userMod)
                .map(userAccount -> {

                    final Collection<APIMessage> errors = new ArrayList<>();
                    if (!userAccount.newPasswordMatch()) {
                        errors.add(APIMessage.fieldValidationError(
                                new FieldError(
                                        "passwordChange",
                                        PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD,
                                        "user:confirmNewPassword:password.mismatch")));
                    }

                    if (!errors.isEmpty()) {
                        throw new APIMessageException(errors);
                    }

                    if (!this.createRateLimitBucket.tryConsume(1)) {
                        throw new TooManyRequests(TooManyRequests.Code.REGISTRATION);
                    }

                    return userAccount;
                })
                .flatMap(this.userDAO::createNew)
                .flatMap(account -> this.userDAO.setActive(account, autoActivation))
                .flatMap(this.userActivityLogDAO::logRegisterAccount)
                .flatMap(account -> this.userDAO.byModelId(account.getModelId()))
                .flatMap(this::createSPSAccount)
                .getOrThrow();

    }
    
    private Result<UserInfo> createSPSAccount(final UserInfo userInfo) {
        return Result.tryCatch(() -> {
            this.screenProctoringService.synchronizeSPSUser(userInfo.uuid);
            return userInfo;
        });
    }

}
