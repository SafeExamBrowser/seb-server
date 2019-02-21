/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.content;

import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.UserAccount;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.form.PageFormService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageUtils;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.page.action.UserAccountActions;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.NewUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.SaveUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory;

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

        if (log.isDebugEnabled()) {
            log.debug("Compose User Account Form within PageContext: {}", pageContext);
        }

        final WidgetFactory widgetFactory = this.pageFormService.getWidgetFactory();
        final EntityKey entityKey = pageContext.getEntityKey();
        final BooleanSupplier isNew = () -> entityKey == null;
        final BooleanSupplier isNotNew = () -> !isNew.getAsBoolean();
        final BooleanSupplier isSEBAdmin = () -> this.currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);

        // get data or create new. handle error if happen
        final UserAccount userAccount = isNew.getAsBoolean()
                ? new UserMod(
                        UUID.randomUUID().toString(),
                        this.currentUser.get().institutionId)
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

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext;
        pageContext.withEntityKey(userAccount.getEntityKey());

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
                .readonly(pageContext.isReadonly())
                .putStaticValueIf(isNotNew,
                        Domain.USER.ATTR_UUID,
                        userAccount.getModelId())
                .putStaticValueIf(isNew,
                        Domain.USER.ATTR_TIMEZONE,
                        userAccount.getTimeZone().getID())
                .addField(FormBuilder.singleSelection(
                        Domain.USER.ATTR_INSTITUTION_ID,
                        "sebserver.useraccount.form.institution",
                        String.valueOf(userAccount.getInstitutionId()),
                        () -> PageUtils.getInstitutionSelectionResource(this.restService))
                        .withCondition(isSEBAdmin))
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
                        Domain.USER.ATTR_LOCALE,
                        "sebserver.useraccount.form.language",
                        userAccount.getTimeZone().getID(),
                        () -> widgetFactory.getI18nSupport().getLanguageResources())
                        .withLocalizationSupplied())
                .addField(FormBuilder.singleSelection(
                        Domain.USER.ATTR_TIMEZONE,
                        "sebserver.useraccount.form.timezone",
                        userAccount.getTimeZone().getID(),
                        () -> widgetFactory.getI18nSupport().getTimeZoneResources())
                        .withLocalizationSupplied())
                // TODO add role selection (create multi selector)
                .addField(FormBuilder.text(
                        UserMod.ATTR_NAME_NEW_PASSWORD,
                        "sebserver.useraccount.form.password",
                        null)
                        .asPasswordField()
                        .withCondition(isNew))
                .addField(FormBuilder.text(
                        UserMod.ATTR_NAME_RETYPED_NEW_PASSWORD,
                        "sebserver.useraccount.form.password.retyped",
                        null)
                        .asPasswordField()
                        .withCondition(isNew))
                .buildFor((entityKey == null)
                        ? this.restService.getRestCall(NewUserAccount.class)
                        : this.restService.getRestCall(SaveUserAccount.class),
                        UserAccountActions.postSaveAdapter(pageContext));

        // propagate content actions to action-pane
        final boolean writeGrant = this.currentUser.hasPrivilege(PrivilegeType.WRITE, userAccount);
        final boolean modifyGrant = this.currentUser.hasPrivilege(PrivilegeType.MODIFY, userAccount);
        if (pageContext.isReadonly()) {
            formContext.createAction(ActionDefinition.USER_ACCOUNT_NEW)
                    .withExec(UserAccountActions::newUserAccount)
                    .publishIf(() -> writeGrant);
            formContext.createAction(ActionDefinition.USER_ACCOUNT_MODIFY)
                    .withExec(UserAccountActions::editUserAccount)
                    .publishIf(() -> modifyGrant);

            if (!userAccount.isActive()) {
                formContext.createAction(ActionDefinition.USER_ACCOUNT_ACTIVATE)
                        .withExec(UserAccountActions::activateUserAccount)
                        .publishIf(() -> modifyGrant);
            } else {
                formContext.createAction(ActionDefinition.USER_ACCOUNT_DEACTIVATE)
                        .withExec(UserAccountActions::deactivateUserAccount)
                        .withConfirm(PageUtils.confirmDeactivation(userAccount, this.restService))
                        .publishIf(() -> modifyGrant);
            }

        } else {
            formContext.createAction(ActionDefinition.USER_ACCOUNT_SAVE)
                    .withExec(formHandle::postChanges)
                    .publish()
                    .createAction(ActionDefinition.USER_ACCOUNT_CANCEL_MODIFY)
                    .withExec(UserAccountActions::cancelEditUserAccount)
                    .withConfirm("sebserver.overall.action.modify.cancel.confirm")
                    .publish();
        }
    }

}
