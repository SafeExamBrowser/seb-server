/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.apache.tomcat.util.buf.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
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
import ch.ethz.seb.sebserver.gui.form.PageFormService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageUtils;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitution;
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

    private static final Logger log = LoggerFactory.getLogger(UserAccountForm.class);

    private final PageFormService pageFormService;
    private final RestService restService;
    private final CurrentUser currentUser;

    protected UserAccountForm(
            final PageFormService pageFormService,
            final RestService restService,
            final CurrentUser currentUser) {

        this.pageFormService = pageFormService;
        this.restService = restService;
        this.currentUser = currentUser;
    }

    @Override
    public void compose(final PageContext pageContext) {

        final UserInfo user = this.currentUser.get();
        final WidgetFactory widgetFactory = this.pageFormService.getWidgetFactory();
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        final BooleanSupplier isNew = () -> entityKey == null;
        final BooleanSupplier isNotNew = () -> !isNew.getAsBoolean();
        final BooleanSupplier isSEBAdmin = () -> user.hasRole(UserRole.SEB_SERVER_ADMIN);
        final boolean readonly = pageContext.isReadonly();
        // get data or create new. handle error if happen
        final UserAccount userAccount = isNew.getAsBoolean()
                ? new UserMod(
                        UUID.randomUUID().toString(),
                        (parentEntityKey != null)
                                ? Long.valueOf(parentEntityKey.modelId)
                                : user.institutionId)
                : this.restService
                        .getBuilder(GetUserAccount.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .get(pageContext::notifyError);

        if (userAccount == null) {
            log.error(
                    "Failed to get UserAccount. "
                            + "Error was notified to the User. "
                            + "See previous logs for more infomation");
            return;
        }

        final boolean ownAccount = user.uuid.equals(userAccount.getModelId());
        final EntityGrantCheck userGrantCheck = this.currentUser.entityGrantCheck(userAccount);
        final boolean writeGrant = userGrantCheck.w();
        final boolean modifyGrant = userGrantCheck.m();
        // modifying an UserAccount is not possible if the root institution is inactive
        final boolean istitutionActive = this.restService.getBuilder(GetInstitution.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(userAccount.getInstitutionId()))
                .call()
                .map(inst -> inst.active)
                .getOr(false);

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(userAccount.getEntityKey());

        if (log.isDebugEnabled()) {
            log.debug("UserAccount Form for user {}", userAccount.getName());
        }

        // the default page layout with title
        final LocTextKey titleKey = new LocTextKey(
                isNotNew.getAsBoolean()
                        ? "sebserver.useraccount.form.title"
                        : "sebserver.useraccount.form.title.new",
                userAccount.getUsername());
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        // The UserAccount form
        final FormHandle<UserInfo> formHandle = this.pageFormService.getBuilder(
                formContext.copyOf(content), 4)
                .readonly(readonly)
                .putStaticValueIf(isNotNew,
                        Domain.USER.ATTR_UUID,
                        userAccount.getModelId())
                .putStaticValueIf(isNotNew,
                        Domain.USER.ATTR_INSTITUTION_ID,
                        String.valueOf(userAccount.getInstitutionId()))
                .addField(FormBuilder.singleSelection(
                        Domain.USER.ATTR_INSTITUTION_ID,
                        "sebserver.useraccount.form.institution",
                        String.valueOf(userAccount.getInstitutionId()),
                        () -> PageUtils.getInstitutionSelectionResource(this.restService))
                        .withCondition(isSEBAdmin)
                        .readonlyIf(isNotNew))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_NAME,
                        "sebserver.useraccount.form.name",
                        userAccount.getName()))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_USERNAME,
                        "sebserver.useraccount.form.username",
                        userAccount.getUsername()))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_EMAIL,
                        "sebserver.useraccount.form.mail",
                        userAccount.getEmail()))
                .addField(FormBuilder.singleSelection(
                        Domain.USER.ATTR_LANGUAGE,
                        "sebserver.useraccount.form.language",
                        userAccount.getLanguage().getLanguage(),
                        widgetFactory.getI18nSupport().localizedLanguageResources()))
                .addField(FormBuilder.singleSelection(
                        Domain.USER.ATTR_TIMEZONE,
                        "sebserver.useraccount.form.timezone",
                        userAccount.getTimeZone().getID(),
                        widgetFactory.getI18nSupport().localizedTimeZoneResources()))
                .addField(FormBuilder.multiSelection(
                        USER_ROLE.REFERENCE_NAME,
                        "sebserver.useraccount.form.roles",
                        StringUtils.join(userAccount.getRoles(), Constants.LIST_SEPARATOR_CHAR),
                        widgetFactory.getI18nSupport().localizedUserRoleResources())
                        .withCondition(() -> modifyGrant)
                        .visibleIf(writeGrant))
                .addField(FormBuilder.text(
                        PasswordChange.ATTR_NAME_NEW_PASSWORD,
                        "sebserver.useraccount.form.password")
                        .asPasswordField()
                        .withCondition(isNew))
                .addField(FormBuilder.text(
                        PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD,
                        "sebserver.useraccount.form.password.confirm")
                        .asPasswordField()
                        .withCondition(isNew))
                .buildFor((entityKey == null)
                        ? this.restService.getRestCall(NewUserAccount.class)
                        : this.restService.getRestCall(SaveUserAccount.class));

        // propagate content actions to action-pane

        formContext.clearEntityKeys()

                .createAction(ActionDefinition.USER_ACCOUNT_NEW)
                .publishIf(() -> writeGrant && readonly && istitutionActive)

                .createAction(ActionDefinition.USER_ACCOUNT_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && readonly && istitutionActive)

                .createAction(ActionDefinition.USER_ACCOUNT_CHANGE_PASSOWRD)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && readonly && istitutionActive && userAccount.isActive())

                .createAction(ActionDefinition.USER_ACCOUNT_DEACTIVATE)
                .withExec(Action.activation(this.restService, false))
                .withConfirm(PageUtils.confirmDeactivation(userAccount, this.restService))
                .publishIf(() -> writeGrant && readonly && istitutionActive && userAccount.isActive())

                .createAction(ActionDefinition.USER_ACCOUNT_ACTIVATE)
                .withExec(Action.activation(this.restService, true))
                .publishIf(() -> writeGrant && readonly && istitutionActive && !userAccount.isActive())

                .createAction(ActionDefinition.USER_ACCOUNT_SAVE)
                .withExec(action -> {
                    final Action postChanges = formHandle.postChanges(action);
                    if (ownAccount) {
                        this.currentUser.refresh();
                        pageContext.forwardToMainPage();
                    }
                    return postChanges;
                })
                .publishIf(() -> !readonly)

                .createAction(ActionDefinition.USER_ACCOUNT_CANCEL_MODIFY)
                .withExec(Action::onEmptyEntityKeyGoToActivityHome)
                .withConfirm("sebserver.overall.action.modify.cancel.confirm")
                .publishIf(() -> !readonly);
    }

}
