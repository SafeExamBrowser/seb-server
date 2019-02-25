/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.content.action.InstitutionActions;
import ch.ethz.seb.sebserver.gui.content.action.UserAccountActions;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.form.PageFormService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageUtils;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.NewInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.SaveInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class InstitutionForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(InstitutionForm.class);

    private final PageFormService pageFormService;
    private final RestService restService;
    private final CurrentUser currentUser;

    protected InstitutionForm(
            final PageFormService pageFormService,
            final RestService restService,
            final CurrentUser currentUser) {

        this.pageFormService = pageFormService;
        this.restService = restService;
        this.currentUser = currentUser;
    }

    @Override
    public void compose(final PageContext pageContext) {

        final WidgetFactory widgetFactory = this.pageFormService.getWidgetFactory();
        final EntityKey entityKey = pageContext.getEntityKey();

        // get data or create new. Handle error if happen
        final Institution institution = (entityKey == null)
                ? new Institution(null, null, null, null, false)
                : this.restService
                        .getBuilder(GetInstitution.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .get(pageContext::notifyError);

        if (institution == null) {
            log.error("Failed to get Institution. "
                    + "Error was notified to the User. "
                    + "See previous logs for more infomation");
            return;
        }

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext;
        pageContext.withEntityKey(institution.getEntityKey());

        if (log.isDebugEnabled()) {
            log.debug("Institution Form for Institution {}", institution.name);
        }

        // the default page layout with interactive title
        final LocTextKey titleKey = new LocTextKey(
                (entityKey != null)
                        ? "sebserver.institution.form.title"
                        : "sebserver.institution.form.title.new",
                institution.name);
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        // The Institution form
        final FormHandle<Institution> formHandle = this.pageFormService.getBuilder(
                formContext.copyOf(content), 4)
                .readonly(pageContext.isReadonly())
                .putStaticValueIf(() -> entityKey != null,
                        Domain.INSTITUTION.ATTR_ID,
                        institution.getModelId())
                .addField(FormBuilder.text(
                        Domain.INSTITUTION.ATTR_NAME,
                        "sebserver.institution.form.name",
                        institution.name))
                .addField(FormBuilder.text(
                        Domain.INSTITUTION.ATTR_URL_SUFFIX,
                        "sebserver.institution.form.urlSuffix",
                        institution.urlSuffix))
                .addField(FormBuilder.imageUpload(
                        Domain.INSTITUTION.ATTR_LOGO_IMAGE,
                        "sebserver.institution.form.logoImage",
                        institution.logoImage)
                        .withCondition(() -> entityKey != null))
                .buildFor((entityKey == null)
                        ? this.restService.getRestCall(NewInstitution.class)
                        : this.restService.getRestCall(SaveInstitution.class));

        // propagate content actions to action-pane
        final boolean writeGrant = this.currentUser.hasPrivilege(PrivilegeType.WRITE, institution);
        final boolean modifyGrant = this.currentUser.hasPrivilege(PrivilegeType.MODIFY, institution);
        if (pageContext.isReadonly()) {
            formContext.createAction(ActionDefinition.INSTITUTION_NEW)
                    .withAttribute(AttributeKeys.READ_ONLY, "false")
                    .publishIf(() -> writeGrant);
            formContext.createAction(ActionDefinition.INSTITUTION_MODIFY)
                    .withExec(InstitutionActions::editInstitution)
                    .publishIf(() -> modifyGrant);

            if (!institution.isActive()) {
                formContext.createAction(ActionDefinition.INSTITUTION_ACTIVATE)
                        .withExec(InstitutionActions::activateInstitution)
                        .publishIf(() -> modifyGrant);
            } else {
                formContext.createAction(ActionDefinition.INSTITUTION_DEACTIVATE)
                        .withExec(InstitutionActions::deactivateInstitution)
                        .withConfirm(PageUtils.confirmDeactivation(institution, this.restService))
                        .publishIf(() -> modifyGrant)
                        .withParentEntityKey(entityKey)
                        .createAction(ActionDefinition.USER_ACCOUNT_NEW)
                        .withExec(UserAccountActions::newUserAccount)
                        .withParentEntity(institution.getEntityKey())
                        .publishIf(() -> this.currentUser.hasPrivilege(PrivilegeType.WRITE, EntityType.USER));
            }

        } else {
            formContext.createAction(ActionDefinition.INSTITUTION_SAVE)
                    .withExec(formHandle::postChanges)
                    .publish()
                    .createAction(ActionDefinition.INSTITUTION_CANCEL_MODIFY)
                    .withExec(InstitutionActions::cancelEditInstitution)
                    .withConfirm("sebserver.overall.action.modify.cancel.confirm")
                    .publish();
        }
    }

}
