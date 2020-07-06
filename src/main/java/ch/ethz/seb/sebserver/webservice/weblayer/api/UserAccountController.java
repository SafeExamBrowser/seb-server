/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import javax.validation.Valid;

import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.authorization.Privilege;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserAccount;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.RevokeTokenEndpoint;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.USER_ACCOUNT_ENDPOINT)
public class UserAccountController extends ActivatableEntityController<UserInfo, UserMod> {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserDAO userDAO;
    private final PasswordEncoder userPasswordEncoder;

    public UserAccountController(
            final UserDAO userDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BulkActionService bulkActionService,
            final ApplicationEventPublisher applicationEventPublisher,
            final BeanValidationService beanValidationService,
            @Qualifier(WebSecurityConfig.USER_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder userPasswordEncoder) {

        super(authorization,
                bulkActionService,
                userDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);
        this.applicationEventPublisher = applicationEventPublisher;
        this.userDAO = userDAO;
        this.userPasswordEncoder = userPasswordEncoder;
    }

    @RequestMapping(path = API.CURRENT_USER_PATH_SEGMENT, method = RequestMethod.GET)
    public UserInfo loggedInUser() {
        return this.authorization
                .getUserService()
                .getCurrentUser()
                .getUserInfo();
    }

    @RequestMapping(path = API.LOGIN_PATH_SEGMENT, method = RequestMethod.POST)
    public void logLogin() {
        this.userActivityLogDAO.logLogin(this.authorization
                .getUserService()
                .getCurrentUser()
                .getUserInfo());
    }

    @RequestMapping(path = API.LOGOUT_PATH_SEGMENT, method = RequestMethod.POST)
    public void logLogout() {
        this.userActivityLogDAO.logLogout(this.authorization
                .getUserService()
                .getCurrentUser()
                .getUserInfo());
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return UserRecordDynamicSqlSupport.userRecord;
    }

    @Override
    protected UserMod createNew(final POSTMapper postParams) {
        return new UserMod(null, postParams);
    }

    @Override
    protected Result<UserInfo> checkModifyAccess(final UserInfo entity) {
        return super.checkModifyAccess(entity)
                .map(this::checkRoleBasedEditGrant);
    }

    private UserInfo checkRoleBasedEditGrant(final UserInfo userInfo) {
        final SEBServerUser currentUser = this.authorization.getUserService().getCurrentUser();
        if (Privilege.hasRoleBasedUserAccountEditGrant(userInfo, currentUser.getUserInfo())) {
            return userInfo;
        } else {
            throw new AccessDeniedException("No edit right grant for user: " + currentUser.getUsername());
        }
    }

    @Override
    protected Result<UserMod> validForCreate(final UserMod userInfo) {
        return super.validForCreate(userInfo)
                .flatMap(this::additionalConsistencyChecks)
                .flatMap(this::passwordMatch);
    }

    @Override
    protected Result<UserInfo> validForSave(final UserInfo userInfo) {
        return super.validForSave(userInfo)
                .flatMap(this::additionalConsistencyChecks);
    }

    @RequestMapping(
            path = API.PASSWORD_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public UserInfo changePassword(@Valid @RequestBody final PasswordChange passwordChange) {

        final String modelId = passwordChange.getModelId();
        return this.userDAO.byModelId(modelId)
                .flatMap(this.authorization::checkModify)
                .map(ui -> checkPasswordChange(ui, passwordChange))
                .flatMap(e -> this.userDAO.changePassword(modelId, passwordChange.getNewPassword()))
                .flatMap(this::revokeAccessToken)
                .flatMap(e -> this.userActivityLogDAO.log(UserLogActivityType.PASSWORD_CHANGE, e))
                .getOrThrow();
    }

    private Result<UserInfo> revokeAccessToken(final UserInfo userInfo) {
        return Result.tryCatch(() -> {
            this.applicationEventPublisher.publishEvent(
                    new RevokeTokenEndpoint.RevokeTokenEvent(userInfo, userInfo.username));
            return userInfo;
        });
    }

    private <T extends UserAccount> Result<UserMod> passwordMatch(final UserMod userInfo) {
        if (!userInfo.newPasswordMatch()) {
            throw new APIMessageException(APIMessage.fieldValidationError(
                    new FieldError(
                            "passwordChange",
                            PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD,
                            "user:confirmNewPassword:password.mismatch")));
        }

        return Result.of(userInfo);
    }

    /** Additional consistency checks that has to be checked before create and save actions */
    private <T extends UserAccount> Result<T> additionalConsistencyChecks(final T userInfo) {
        return Result.tryCatch(() -> {
            final SEBServerUser currentUser = this.authorization.getUserService().getCurrentUser();
            final EnumSet<UserRole> rolesOfCurrentUser = currentUser.getUserRoles();
            final EnumSet<UserRole> userRolesOfAccount = userInfo.getUserRoles();

            // check of institution of UserInfo is active. Otherwise save is not valid
            if (!this.beanValidationService
                    .isActive(new EntityKey(userInfo.getInstitutionId(), EntityType.INSTITUTION))) {
                throw new APIConstraintViolationException(
                        "User within an inactive institution cannot be created nor modified");
            }

            // check if the current User has the role based right to save the User Account
            // role based right in this context means that for example a Institutional Administrator that
            // has normally the right to edit a User Account of his own institution, don't has the right
            // to edit a User Account of his own institution with a higher role based rank, for example a
            // SEB Server Admin of the same Institution
            if (userRolesOfAccount.contains(UserRole.SEB_SERVER_ADMIN) &&
                    !rolesOfCurrentUser.contains(UserRole.SEB_SERVER_ADMIN)) {

                throw new APIConstraintViolationException(
                        "The current user cannot edit a User-Account of higher role passed rank: "
                                + UserRole.SEB_SERVER_ADMIN);
            }

            // check if there are only public UserRole set for current User
            final List<UserRole> publicRolesFor = UserRole.publicRolesForUser(currentUser.getUserInfo());
            final UserRole nonePublicRole = userRolesOfAccount
                    .stream()
                    .filter(role -> !publicRolesFor.contains(role))
                    .findFirst()
                    .orElse(null);

            if (nonePublicRole != null) {
                throw new APIConstraintViolationException(
                        "The current user has not the privilege to create a User-Account with none public role: "
                                + nonePublicRole);
            }

            return userInfo;
        });
    }

    private UserInfo checkPasswordChange(final UserInfo info, final PasswordChange passwordChange) {
        final SEBServerUser currentUser = this.userDAO.sebServerUserByUsername(this.authorization
                .getUserService()
                .getCurrentUser().getUsername())
                .getOrThrow();

        final Collection<APIMessage> errors = new ArrayList<>();

        if (!this.userPasswordEncoder.matches(passwordChange.getPassword(), currentUser.getPassword())) {

            errors.add(APIMessage.fieldValidationError(
                    new FieldError(
                            "passwordChange",
                            PasswordChange.ATTR_NAME_PASSWORD,
                            "user:password:password.wrong")));
        }

        if (!passwordChange.newPasswordMatch()) {

            errors.add(APIMessage.fieldValidationError(
                    new FieldError(
                            "passwordChange",
                            PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD,
                            "user:confirmNewPassword:password.mismatch")));
        }

        if (!errors.isEmpty()) {
            throw new APIMessageException(errors);
        }

        return info;

    }
}
