/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
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
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.RevokeTokenEndpoint;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.USER_ACCOUNT_ENDPOINT)
public class UserAccountController extends ActivatableEntityController<UserInfo, UserMod> {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserDAO userDAO;

    public UserAccountController(
            final UserDAO userDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BulkActionService bulkActionService,
            final ApplicationEventPublisher applicationEventPublisher,
            final BeanValidationService beanValidationService) {

        super(authorization,
                bulkActionService,
                userDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);
        this.applicationEventPublisher = applicationEventPublisher;
        this.userDAO = userDAO;
    }

    @RequestMapping(path = "/me", method = RequestMethod.GET)
    public UserInfo loggedInUser() {
        return this.authorization
                .getUserService()
                .getCurrentUser()
                .getUserInfo();
    }

    @Override
    protected Class<UserMod> modifiedDataType() {
        return UserMod.class;
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return UserRecordDynamicSqlSupport.userRecord;
    }

    @Override
    protected UserMod createNew(final POSTMapper postParams) {
        return new UserMod(null, postParams);
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.PASSWORD_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UserInfo changePassword(
            @PathVariable final String modelId,
            @Valid @RequestBody final PasswordChange passwordChange) {

        if (!passwordChange.newPasswordMatch()) {
            throw new APIMessageException(ErrorMessage.PASSWORD_MISMATCH);
        }

        return this.userDAO.byModelId(modelId)
                .flatMap(this.authorization::checkWrite)
                .flatMap(e -> this.userDAO.changePassword(modelId, passwordChange.getNewPassword()))
                .flatMap(this::revokeAccessToken)
                .flatMap(e -> this.userActivityLogDAO.log(ActivityType.MODIFY, e))

                .getOrThrow();
    }

    private Result<UserInfo> revokeAccessToken(final UserInfo userInfo) {
        return Result.tryCatch(() -> {
            this.applicationEventPublisher.publishEvent(
                    new RevokeTokenEndpoint.RevokeTokenEvent(userInfo, userInfo.username));
            return userInfo;
        });
    }

}
