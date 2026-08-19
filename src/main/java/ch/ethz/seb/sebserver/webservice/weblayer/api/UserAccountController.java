/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.authorization.Privilege;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.*;
import ch.ethz.seb.sebserver.gbl.model.user.GuiAbilities;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserAccount;
import ch.ethz.seb.sebserver.gbl.model.user.UserFeatures;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.FeatureService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.UserCacheService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;
import jakarta.servlet.http.HttpServletRequest;
import org.mybatis.dynamic.sql.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.USER_ACCOUNT_ENDPOINT)
@Tag(name = "UserAccount", description = "User account administration and current-user endpoints.")
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_ADMIN_API)
public class UserAccountController extends ActivatableEntityController<UserInfo, UserMod> {

    private static final Logger log = LoggerFactory.getLogger(UserAccountController.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserDAO userDAO;
    private final PasswordEncoder userPasswordEncoder;
    private final ScreenProctoringService screenProctoringService;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final WebserviceInfo webserviceInfo;
    private final OAuth2AuthorizationService oAuth2AuthorizationService;
    private final FeatureService featureService;
    private final UserCacheService userCacheService;

    public UserAccountController(
            final UserDAO userDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BulkActionService bulkActionService,
            final ApplicationEventPublisher applicationEventPublisher,
            final BeanValidationService beanValidationService,
            final ScreenProctoringService screenProctoringService,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final WebserviceInfo webserviceInfo,
            final FeatureService featureService,
            final PasswordEncoder userPasswordEncoder,
            final OAuth2AuthorizationService oAuth2AuthorizationService,
            final UserCacheService userCacheService) {

        super(authorization,
                bulkActionService,
                userDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);
        this.applicationEventPublisher = applicationEventPublisher;
        this.userDAO = userDAO;
        this.userPasswordEncoder = userPasswordEncoder;
        this.screenProctoringService = screenProctoringService;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.webserviceInfo = webserviceInfo;
        this.featureService = featureService;
        this.oAuth2AuthorizationService = oAuth2AuthorizationService;
        this.userCacheService = userCacheService;
    }

    @Operation(
            operationId = "getCurrentUserAccount",
            summary = "Get the current user account")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Current user account.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserInfo.class)))
    })
    @RequestMapping(
            path = API.CURRENT_USER_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UserInfo loggedInUser() {
        return this.authorization
                .getUserService()
                .getCurrentUser()
                .getUserInfo();
    }

    @Operation(hidden = true)
    @RequestMapping(
            path = API.CURRENT_USER_PATH_SEGMENT + API.FEATURES_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UserFeatures getCurrentUserFeatures() {
        return this.featureService.getCurrentUserFeatures().getOrThrow();
    }

    @Operation(
            operationId = "getCurrentUserGuiAbilities",
            summary = "Get the effective GUI abilities of the current user",
            description = "GUI components and actions merged over the current user's roles. "
                    + "Presentation configuration for the GUI only; the webservice authorizes every request independently.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "The effective GUI abilities of the current user.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GuiAbilities.class)))
    })
    @RequestMapping(
            path = API.CURRENT_USER_PATH_SEGMENT + API.GUI_ABILITIES_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public GuiAbilities getCurrentUserGuiAbilities() {
        return GuiAbilitiesDefinition.abilitiesFor(
                this.authorization
                        .getUserService()
                        .getCurrentUser()
                        .getUserInfo()
                        .getUserRoles());
    }

    @Operation(hidden = true)
    @RequestMapping(path = API.LOGIN_PATH_SEGMENT, method = RequestMethod.POST)
    public void login() {
        this.userActivityLogDAO.logLogin(this.authorization
                .getUserService()
                .getCurrentUser()
                .getUserInfo());
    }

    @Operation(
            operationId = "logUserAccountLogout",
            summary = "Record current user logout",
            description = "Attempts to revoke the current bearer token and records a logout activity.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout activity recorded.")
    })
    @RequestMapping(path = API.LOGOUT_PATH_SEGMENT, method = RequestMethod.POST)
    public void logout(@Parameter(hidden = true) final HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                String tokenValue = authHeader.replace("Bearer", "").trim();
                OAuth2Authorization byToken = oAuth2AuthorizationService.findByToken(tokenValue, OAuth2TokenType.ACCESS_TOKEN);
                oAuth2AuthorizationService.remove(byToken);
            }
        } catch (Exception e) {
            log.warn("Failed to revoke access token: {}", e.getMessage());
        }

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
    protected Result<UserInfo> notifyCreated(UserInfo entity) {
        return super
                .notifyCreated(entity)
                .flatMap(account -> this.userDAO.setActive(account, true))
                .map(this::synchronizeUserWithSPS);
    }

    @Override
    protected Result<UserInfo> validForSave(final UserInfo userInfo) {
        return super.validForSave(userInfo)
                .flatMap(this::additionalConsistencyChecks);
    }

    @Override
    protected Result<UserInfo> notifySaved(
            final UserInfo entity, 
            final Activatable.ActivationAction activationAction) {

        final Result<UserInfo> userInfoResult = super.notifySaved(entity, activationAction);
        this.userCacheService.evictServerUserByName(entity.username);
        this.synchronizeUserWithSPS(entity);
        return userInfoResult;
    }

    @Override
    protected Result<Pair<UserInfo, EntityProcessingReport>> notifyDeleted(final Pair<UserInfo, EntityProcessingReport> pair) {
        final Result<Pair<UserInfo, EntityProcessingReport>> result = super.notifyDeleted(pair);
        this.screenProctoringService.deleteSPSUser(pair.a.uuid);
        return result;
    }

    @Operation(
            operationId = "getUserAccountSupervisors",
            summary = "Get active supporter user names",
            description = "Returns active EXAM_SUPPORTER user account names for the given institution.",
            parameters = {
                    @Parameter(name = API.PARAM_INSTITUTION_ID, description = "Institution identifier. Defaults to the current user's institution.")
            })
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Active supporter user names.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = EntityName.class))))
    })
    @RequestMapping(
            path = API.USER_ACCOUNT_ENDPOINT_SUPPORTER,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<EntityName> getSupporterNames(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {
        
        authorization.check(
                PrivilegeType.READ, 
                EntityType.USER, 
                institutionId);

        final FilterMap filterMap = new FilterMap();
        filterMap.putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));
        
        return super.getAll(filterMap)
                .map(all -> all.stream()
                        .filter(u -> Objects.equals(u.institutionId, institutionId) && 
                                u.hasAnyRole(UserRole.EXAM_SUPPORTER) && 
                                u.isActive())
                        .map(UserInfo::toName)
                        .toList())
                .getOrThrow();
    }

    @Operation(
            operationId = "changeUserAccountPassword",
            summary = "Change a user account password",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PasswordChange.class))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User account after password change.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserInfo.class)))
    })
    @RequestMapping(
            path = API.PASSWORD_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UserInfo changePassword(@Valid @RequestBody final PasswordChange passwordChange) {

        final String modelId = passwordChange.getModelId();
        return this.userDAO.byModelId(modelId)
                .flatMap(this.authorization::checkModify)
                .map(ui -> checkPasswordChange(ui, passwordChange))
                .flatMap(e -> this.userDAO.changePassword(modelId, passwordChange.getNewPassword()))
               // .flatMap(this::revokeAccessToken)
                .flatMap(e -> this.userActivityLogDAO.log(UserLogActivityType.PASSWORD_CHANGE, e))
                .map(this::synchronizeUserWithSPS)
                .map(this::removeInitialAdminPasswordFromDB)
                .getOrThrow();
    }

    // NOTE: there is no revoke token possibility anymore since 3.0
//    private Result<UserInfo> revokeAccessToken(final UserInfo userInfo) {
//        return Result.tryCatch(() -> {
//            this.applicationEventPublisher.publishEvent(
//                    new RevokeTokenEndpoint.RevokeTokenEvent(userInfo, userInfo.username));
//            return userInfo;
//        });
//    }

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

            // check if this is a teacher only account. Teacher only accounts cannot be edited
            if (userInfo.getModelId() != null) {
                userDAO
                        .byModelId(userInfo.getModelId())
                        .ifPresent( savedAccount -> {
                                if (savedAccount.isOnlyTeacher()) {
                                    throw new APIConstraintViolationException(
                                            "Teacher accounts are managed by other systems and can therefore not be changes");
                                }
                        });
            }
            
            final SEBServerUser currentUser = this.authorization.getUserService().getCurrentUser();
            final EnumSet<UserRole> rolesOfCurrentUser = currentUser.getUserRoles();
            final EnumSet<UserRole> userRolesOfAccount = userInfo.getUserRoles();

            // check of institution of UserInfo is active, otherwise save is not valid
            if (!this.beanValidationService
                    .isActive(new EntityKey(userInfo.getInstitutionId(), EntityType.INSTITUTION))) {
                throw new APIConstraintViolationException(
                        "User within an inactive institution cannot be created nor modified");
            }

            // check if the current User has the role based right to save the User Account
            // role based right in this context means that for example an Institutional Administrator that
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

    private UserInfo synchronizeUserWithSPS(final UserInfo userInfo) {
        screenProctoringService.synchronizeSPSUser(userInfo.uuid);
        return userInfo;
    }

    private UserInfo removeInitialAdminPasswordFromDB(final UserInfo userInfo){
        if(!this.webserviceInfo.isLightSetup()){
            return userInfo;
        }

        try{
            this.additionalAttributesDAO.delete(EntityType.USER, Constants.LIGHT_ADMIN_USER_ID, Domain.USER.ATTR_USERNAME);
            this.additionalAttributesDAO.delete(EntityType.USER, Constants.LIGHT_ADMIN_USER_ID, Domain.USER.ATTR_PASSWORD);

        }catch(final Exception e){
            log.error("Unable to delete initial admin credentials from the additional attributes table: ", e);
        }

        return userInfo;
    }
}
