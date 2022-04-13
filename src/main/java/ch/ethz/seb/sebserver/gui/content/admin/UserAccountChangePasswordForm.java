/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.admin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.ChangePassword;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.widget.Message;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
/** The form to change an User-Account password.
 * If the current user is the owner of the User-Account the password is required and must
 * match the users current password.
 * If the current user is an administrator that has to reset another users password the
 * password that is also required must match the administrators current password. **/
public class UserAccountChangePasswordForm implements TemplateComposer {

    private static final String FORM_TITLE_KEY = "sebserver.useraccount.form.pwchange.title";
    private static final LocTextKey FORM_PASSWORD_NEW_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.password.new");
    private static final LocTextKey FORM_PASSWORD_NEW_CONFIRM_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.password.new.confirm");
    private static final LocTextKey FORM_PASSWORD_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.password");
    private static final String PASSWORD_CHANGE_INFO_KEY =
            "sebserver.useraccount.form.password.info";

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final I18nSupport i18nSupport;

    protected UserAccountChangePasswordForm(final PageService pageService) {

        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.currentUser = pageService.getCurrentUser();
        this.i18nSupport = pageService.getI18nSupport();
    }

    @Override
    public void compose(final PageContext pageContext) {

        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final EntityKey entityKey = pageContext.getEntityKey();

        final UserInfo userInfo = this.restService
                .getBuilder(GetUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .onError(error -> pageContext.notifyLoadError(EntityType.USER, error))
                .getOrThrow();

        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey(FORM_TITLE_KEY, userInfo.username));

        widgetFactory.labelLocalized(
                content,
                WidgetFactory.CustomVariant.SUBTITLE,
                new LocTextKey(PASSWORD_CHANGE_INFO_KEY, userInfo.username));

        final boolean ownAccount = this.currentUser.get().uuid.equals(entityKey.getModelId());

        // The Password Change form
        final FormHandle<UserInfo> formHandle = this.pageService.formBuilder(pageContext.copyOf(content))
                .readonly(false)
                .putStaticValueIf(() -> entityKey != null,
                        Domain.USER.ATTR_UUID,
                        entityKey.getModelId())
                .addField(FormBuilder.text(
                        PasswordChange.ATTR_NAME_PASSWORD,
                        FORM_PASSWORD_TEXT_KEY)
                        .asPasswordField()
                        .mandatory())
                .addField(FormBuilder.text(
                        PasswordChange.ATTR_NAME_NEW_PASSWORD,
                        FORM_PASSWORD_NEW_TEXT_KEY)
                        .asPasswordField()
                        .mandatory())
                .addFieldIf(
                        () -> entityKey != null,
                        () -> FormBuilder.text(
                                PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD,
                                FORM_PASSWORD_NEW_CONFIRM_TEXT_KEY)
                                .asPasswordField()
                                .mandatory())
                .buildFor(this.restService.getRestCall(ChangePassword.class));

        this.pageService.pageActionBuilder(pageContext)

                .newAction(ActionDefinition.USER_ACCOUNT_CHANGE_PASSWORD_SAVE)
                .withExec(action -> {
                    final PageAction saveAction = formHandle.processFormSave(action);
                    if (ownAccount) {
                        // NOTE: in this case the user changed the password of the own account
                        //       this should cause an logout with specified message that password change
                        //       was successful and the pointing the need of re login with the new password
                        this.pageService.logout(pageContext);
                        final MessageBox error = new Message(
                                pageContext.getShell(),
                                this.i18nSupport.getText("sebserver.login.password.change"),
                                this.i18nSupport.getText("sebserver.login.password.change.success"),
                                SWT.ICON_INFORMATION,
                                this.i18nSupport);
                        error.open(null);
                    }
                    return saveAction;
                })
                .ignoreMoveAwayFromEdit()
                .publish()

                .newAction(ActionDefinition.USER_ACCOUNT_CANCEL_MODIFY)
                .withExec(this.pageService.backToCurrentFunction())
                .publish();
    }

}
