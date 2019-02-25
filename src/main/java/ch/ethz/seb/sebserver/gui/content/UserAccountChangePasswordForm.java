/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.content.action.UserAccountActions;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.form.PageFormService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.ChangePassword;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccount;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class UserAccountChangePasswordForm implements TemplateComposer {

    private final PageFormService pageFormService;
    private final RestService restService;

    protected UserAccountChangePasswordForm(
            final PageFormService pageFormService,
            final RestService restService) {

        this.pageFormService = pageFormService;
        this.restService = restService;
    }

    @Override
    public void compose(final PageContext pageContext) {

        final WidgetFactory widgetFactory = this.pageFormService.getWidgetFactory();
        final EntityKey entityKey = pageContext.getEntityKey();

        final UserInfo userInfo = this.restService
                .getBuilder(GetUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .get(pageContext::notifyError);

        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey("sebserver.useraccount.form.pwchange.title", userInfo.username));

        // The Password Change form
        final FormHandle<UserInfo> formHandle = this.pageFormService.getBuilder(
                pageContext.copyOf(content), 4)
                .readonly(pageContext.isReadonly())
                .putStaticValueIf(() -> entityKey != null,
                        Domain.USER.ATTR_ID,
                        entityKey.getModelId())
                .addField(FormBuilder.text(
                        PasswordChange.ATTR_NAME_OLD_PASSWORD,
                        "sebserver.useraccount.form.institution.password.old")
                        .asPasswordField())
                .addField(FormBuilder.text(
                        PasswordChange.ATTR_NAME_NEW_PASSWORD,
                        "sebserver.useraccount.form.institution.password.new")
                        .asPasswordField())
                .addField(FormBuilder.text(
                        PasswordChange.ATTR_NAME_RETYPED_NEW_PASSWORD,
                        "sebserver.useraccount.form.institution.password.retyped")
                        .asPasswordField()
                        .withCondition(() -> entityKey != null))
                .buildFor(this.restService.getRestCall(ChangePassword.class));

        pageContext.createAction(ActionDefinition.USER_ACCOUNT_CHANGE_PASSOWRD_SAVE)
                .withExec(formHandle::postChanges)
                .publish()
                .createAction(ActionDefinition.USER_ACCOUNT_CANCEL_MODIFY)
                .withExec(UserAccountActions::cancelEditUserAccount)
                .withConfirm("sebserver.overall.action.modify.cancel.confirm")
                .publish();
    }

}
