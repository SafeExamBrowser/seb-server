/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.admin;

import java.util.Locale;
import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.tomcat.util.buf.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
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
    static final LocTextKey FORM_SURNAME_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.surname");
    static final LocTextKey FORM_INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.institution");
    static final LocTextKey FORM_CREATION_DATE_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.creationdate");
    static final LocTextKey FORM_LANG_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.language");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final UserAccountDeletePopup userAccountDeletePopup;
    private final boolean multilingual;

    protected UserAccountForm(
            final PageService pageService,
            final UserAccountDeletePopup userAccountDeletePopup,
            @Value("${sebserver.gui.multilingual:false}") final Boolean multilingual) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.userAccountDeletePopup = userAccountDeletePopup;
        this.multilingual = BooleanUtils.toBoolean(multilingual);
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
                        : currentUser.get().institutionId)
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

        final boolean institutionActive = userAccount.getInstitutionId() != null &&
                restService.getBuilder(GetInstitution.class)
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
                formContext.copyOf(content))
                .readonly(readonly)
                .putStaticValueIf(isNotNew,
                        Domain.USER.ATTR_UUID,
                        userAccount.getModelId())
                .putStaticValueIf(isNotNew,
                        Domain.USER.ATTR_INSTITUTION_ID,
                        String.valueOf(userAccount.getInstitutionId()))
                .putStaticValueIf(
                        () -> !this.multilingual,
                        Domain.USER.ATTR_LANGUAGE,
                        Locale.ENGLISH.getLanguage())
                .addFieldIf(
                        isSEBAdmin,
                        () -> FormBuilder.singleSelection(
                                Domain.USER.ATTR_INSTITUTION_ID,
                                FORM_INSTITUTION_TEXT_KEY,
                                String.valueOf(userAccount.getInstitutionId()),
                                this.resourceService::institutionResource)
                                .readonlyIf(isNotNew)
                                .mandatory(!readonly))
                .addFieldIf(
                        () -> readonly,
                        () -> FormBuilder.text(
                                Domain.USER.ATTR_CREATION_DATE,
                                FORM_CREATION_DATE_TEXT_KEY,
                                this.pageService.getI18nSupport().formatDisplayDate(userAccount.getCreationDate()))
                                .readonly(true))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        userAccount.getName())
                        .mandatory(!readonly))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_SURNAME,
                        FORM_SURNAME_TEXT_KEY,
                        userAccount.getSurname())
                        .mandatory(!readonly))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_USERNAME,
                        FORM_USERNAME_TEXT_KEY,
                        userAccount.getUsername())
                        .mandatory(!readonly))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_EMAIL,
                        FORM_MAIL_TEXT_KEY,
                        userAccount.getEmail()))
                .addFieldIf(
                        () -> this.multilingual,
                        () -> FormBuilder.singleSelection(
                                Domain.USER.ATTR_LANGUAGE,
                                FORM_LANG_TEXT_KEY,
                                userAccount.getLanguage().getLanguage(),
                                this.resourceService::languageResources))
                .addField(FormBuilder.singleSelection(
                        Domain.USER.ATTR_TIMEZONE,
                        FORM_TIMEZONE_TEXT_KEY,
                        userAccount.getTimeZone().getID(),
                        this.resourceService::timeZoneResources)
                        .mandatory(!readonly))
                .addFieldIf(
                        () -> modifyGrant,
                        () -> FormBuilder.multiCheckboxSelection(
                                USER_ROLE.REFERENCE_NAME,
                                FORM_ROLES_TEXT_KEY,
                                StringUtils.join(userAccount.getRoles(), Constants.LIST_SEPARATOR_CHAR),
                                this.resourceService::userRoleResources)
                                .visibleIf(writeGrant)
                                .mandatory(!readonly))
                .addFieldIf(
                        isNew,
                        () -> FormBuilder.text(
                                PasswordChange.ATTR_NAME_NEW_PASSWORD,
                                FORM_PASSWORD_TEXT_KEY)
                                .asPasswordField()
                                .mandatory(!readonly))
                .addFieldIf(
                        isNew,
                        () -> FormBuilder.text(
                                PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD,
                                FORM_PASSWORD_CONFIRM_TEXT_KEY)
                                .asPasswordField()
                                .mandatory(!readonly))
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

                .newAction(ActionDefinition.USER_ACCOUNT_CHANGE_PASSWORD)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && readonly && institutionActive && userAccount.isActive())

                .newAction(ActionDefinition.USER_ACCOUNT_DEACTIVATE)
                .withEntityKey(entityKey)
                .withSimpleRestCall(restService, DeactivateUserAccount.class)
                .withConfirm(this.pageService.confirmDeactivation(userAccount))
                .publishIf(() -> writeGrant && readonly && institutionActive && userAccount.isActive())

                .newAction(ActionDefinition.USER_ACCOUNT_ACTIVATE)
                .withEntityKey(entityKey)
                .withSimpleRestCall(restService, ActivateUserAccount.class)
                .publishIf(() -> writeGrant && readonly && institutionActive && !userAccount.isActive())

                .newAction(ActionDefinition.USER_ACCOUNT_DELETE)
                .withEntityKey(entityKey)
                .withExec(this.userAccountDeletePopup.deleteWizardFunction(pageContext))
                .publishIf(() -> writeGrant && readonly && institutionActive)

                .newAction(ActionDefinition.USER_ACCOUNT_SAVE)
                .withEntityKey(entityKey)
                .withExec(action -> formHandle.handleFormPost(formHandle.doAPIPost()
                        .map(userInfo -> {
                            if (ownAccount) {
                                currentUser.refresh(userInfo);
                            }
                            return userInfo;
                        }),
                        action))
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !readonly)

                .newAction(ActionDefinition.USER_ACCOUNT_SAVE_AND_ACTIVATE)
                .withEntityKey(entityKey)
                .withExec(formHandle::saveAndActivate)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !readonly && !ownAccount && !userAccount.isActive())

                .newAction(ActionDefinition.USER_ACCOUNT_CANCEL_MODIFY)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !readonly);
    }

}
