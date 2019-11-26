/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.BooleanSupplier;

import org.apache.tomcat.util.buf.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.Privilege;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.USER_ROLE;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserAccount;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageUtils;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.ActivateUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.DeactivateUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.NewUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.SaveUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class UserAccountForm implements TemplateComposer {

    static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.title");
    static final LocTextKey NEW_TITLE_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.title.new");

    static final LocTextKey FORM_PASSWORD_CONFIRM_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.password.confirm");
    static final LocTextKey FORM_PASSWORD_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.password");
    static final LocTextKey FORM_ROLES_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.roles");
    static final LocTextKey FORM_TIMEZONE_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.timezone");
    static final LocTextKey FORM_MAIL_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.mail");
    static final LocTextKey FORM_USERNAME_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.username");
    static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.name");
    static final LocTextKey FORM_INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.institution");

    private final PageService pageService;
    private final ResourceService resourceService;

    protected UserAccountForm(
            final PageService pageService,
            final ResourceService resourceService) {

        this.pageService = pageService;
        this.resourceService = resourceService;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final RestService restService = this.resourceService.getRestService();
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();

        final UserInfo user = currentUser.get();
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        final boolean readonly = pageContext.isReadonly();

        final BooleanSupplier isNew = () -> entityKey == null;
        final BooleanSupplier isNotNew = () -> !isNew.getAsBoolean();
        final BooleanSupplier isSEBAdmin = () -> user.hasRole(UserRole.SEB_SERVER_ADMIN);

        // get data or create new. handle error if happen
        final UserAccount userAccount = isNew.getAsBoolean()
                ? UserMod.createNew((parentEntityKey != null)
                        ? Long.valueOf(parentEntityKey.modelId)
                        : user.institutionId)
                : restService
                        .getBuilder(GetUserAccount.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .onError(error -> pageContext.notifyLoadError(EntityType.USER, error))
                        .getOrThrow();

        final boolean roleBasedEditGrant = Privilege.hasRoleBasedUserAccountEditGrant(userAccount, currentUser.get());
        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(userAccount.getEntityKey());
        final boolean ownAccount = user.uuid.equals(userAccount.getModelId());
        final EntityGrantCheck userGrantCheck = currentUser.entityGrantCheck(userAccount);

        final boolean writeGrant = roleBasedEditGrant && userGrantCheck.w();
        final boolean modifyGrant = roleBasedEditGrant && userGrantCheck.m();
        final boolean institutionalWriteGrant = currentUser.hasInstitutionalPrivilege(
                PrivilegeType.WRITE,
                EntityType.USER);

        final boolean institutionActive = restService.getBuilder(GetInstitution.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(userAccount.getInstitutionId()))
                .call()
                .map(inst -> inst.active)
                .getOr(false);

        // the default page layout with title
        final LocTextKey titleKey = isNotNew.getAsBoolean()
                ? TITLE_TEXT_KEY
                : NEW_TITLE_TEXT_KEY;
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        // The UserAccount form
        final FormHandle<UserInfo> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content), 4)
                .readonly(readonly)
                .putStaticValueIf(isNotNew,
                        Domain.USER.ATTR_UUID,
                        userAccount.getModelId())
                .putStaticValueIf(isNotNew,
                        Domain.USER.ATTR_INSTITUTION_ID,
                        String.valueOf(userAccount.getInstitutionId()))
                .putStaticValue(
                        Domain.USER.ATTR_LANGUAGE,
                        "en")
                .addFieldIf(
                        isSEBAdmin,
                        () -> FormBuilder.singleSelection(
                                Domain.USER.ATTR_INSTITUTION_ID,
                                FORM_INSTITUTION_TEXT_KEY,
                                String.valueOf(userAccount.getInstitutionId()),
                                () -> this.resourceService.institutionResource())
                                .readonlyIf(isNotNew))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        userAccount.getName()))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_USERNAME,
                        FORM_USERNAME_TEXT_KEY,
                        userAccount.getUsername()))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_EMAIL,
                        FORM_MAIL_TEXT_KEY,
                        userAccount.getEmail()))
//                .addField(FormBuilder.singleSelection(
//                        Domain.USER.ATTR_LANGUAGE,
//                        "sebserver.useraccount.form.language",
//                        userAccount.getLanguage().getLanguage(),
//                        this.resourceService::languageResources)
//                        .readonly(true))
                .addField(FormBuilder.singleSelection(
                        Domain.USER.ATTR_TIMEZONE,
                        FORM_TIMEZONE_TEXT_KEY,
                        userAccount.getTimeZone().getID(),
                        this.resourceService::timeZoneResources))
                .addFieldIf(
                        () -> modifyGrant,
                        () -> FormBuilder.multiSelection(
                                USER_ROLE.REFERENCE_NAME,
                                FORM_ROLES_TEXT_KEY,
                                StringUtils.join(userAccount.getRoles(), Constants.LIST_SEPARATOR_CHAR),
                                this.resourceService::userRoleResources)
                                .visibleIf(writeGrant))
                .addFieldIf(
                        isNew,
                        () -> FormBuilder.text(
                                PasswordChange.ATTR_NAME_NEW_PASSWORD,
                                FORM_PASSWORD_TEXT_KEY)
                                .asPasswordField())
                .addFieldIf(
                        isNew,
                        () -> FormBuilder.text(
                                PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD,
                                FORM_PASSWORD_CONFIRM_TEXT_KEY)
                                .asPasswordField())
                .buildFor((entityKey == null)
                        ? restService.getRestCall(NewUserAccount.class)
                        : restService.getRestCall(SaveUserAccount.class));

        // propagate content actions to action-pane
        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.USER_ACCOUNT_NEW)
                .publishIf(() -> institutionalWriteGrant && readonly && institutionActive)

                .newAction(ActionDefinition.USER_ACCOUNT_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && readonly && institutionActive)

                .newAction(ActionDefinition.USER_ACCOUNT_CHANGE_PASSOWRD)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && readonly && institutionActive && userAccount.isActive())

                .newAction(ActionDefinition.USER_ACCOUNT_DEACTIVATE)
                .withEntityKey(entityKey)
                .withSimpleRestCall(restService, DeactivateUserAccount.class)
                .withConfirm(PageUtils.confirmDeactivation(userAccount, restService))
                .publishIf(() -> writeGrant && readonly && institutionActive && userAccount.isActive())

                .newAction(ActionDefinition.USER_ACCOUNT_ACTIVATE)
                .withEntityKey(entityKey)
                .withSimpleRestCall(restService, ActivateUserAccount.class)
                .publishIf(() -> writeGrant && readonly && institutionActive && !userAccount.isActive())

                .newAction(ActionDefinition.USER_ACCOUNT_SAVE)
                .withEntityKey(entityKey)
                .withExec(action -> {
                    return formHandle.handleFormPost(formHandle.doAPIPost()
                            .map(userInfo -> {
                                if (ownAccount) {
                                    currentUser.refresh(userInfo);
                                    pageContext.forwardToMainPage();
                                }
                                return userInfo;
                            }),
                            action);
                })
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !readonly)

                .newAction(ActionDefinition.USER_ACCOUNT_CANCEL_MODIFY)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !readonly);
    }

}
