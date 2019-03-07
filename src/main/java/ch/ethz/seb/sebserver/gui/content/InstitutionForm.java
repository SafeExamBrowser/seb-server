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
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.NewInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.SaveInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
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
        final boolean isNew = entityKey == null;
        // get data or create new. Handle error if happen
        final Institution institution = (entityKey == null)
                ? Institution.createNew()
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

        final EntityGrantCheck instGrant = this.currentUser.entityGrantCheck(institution);
        final boolean writeGrant = instGrant.w();
        final boolean modifyGrant = instGrant.m();
        final boolean userWriteGrant = this.currentUser.grantCheck(EntityType.USER).w();
        final boolean isReadonly = pageContext.isReadonly();

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(institution.getEntityKey());

        if (log.isDebugEnabled()) {
            log.debug("Institution Form for Institution {}", institution.name);
        }

        // the default page layout with interactive title
        final LocTextKey titleKey = new LocTextKey(
                (isNew)
                        ? "sebserver.institution.form.title.new"
                        : "sebserver.institution.form.title",
                institution.name);
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        // The Institution form
        final FormHandle<Institution> formHandle = this.pageFormService.getBuilder(
                formContext.copyOf(content), 4)
                .readonly(formContext.isReadonly())
                .putStaticValueIf(() -> !isNew,
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
                        .withCondition(() -> !isNew && modifyGrant))
                .buildFor((isNew)
                        ? this.restService.getRestCall(NewInstitution.class)
                        : this.restService.getRestCall(SaveInstitution.class));

        // propagate content actions to action-pane
        formContext.clearEntityKeys()

                .createAction(ActionDefinition.INSTITUTION_NEW)
                .publishIf(() -> writeGrant && isReadonly)

                .createAction(ActionDefinition.USER_ACCOUNT_NEW)
                .withParentEntityKey(entityKey)
                .publishIf(() -> userWriteGrant && isReadonly && institution.isActive())

                .createAction(ActionDefinition.INSTITUTION_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && isReadonly)

                .createAction(ActionDefinition.INSTITUTION_DEACTIVATE)
                .withEntityKey(entityKey)
                .withExec(this.restService::activation)
                .withConfirm(PageUtils.confirmDeactivation(institution, this.restService))
                .publishIf(() -> writeGrant && isReadonly && institution.isActive())

                .createAction(ActionDefinition.INSTITUTION_ACTIVATE)
                .withEntityKey(entityKey)
                .withExec(this.restService::activation)
                .publishIf(() -> writeGrant && isReadonly && !institution.isActive())

                .createAction(ActionDefinition.INSTITUTION_SAVE)
                .withExec(formHandle::postChanges)
                .publishIf(() -> !isReadonly)

                .createAction(ActionDefinition.INSTITUTION_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withExec(Action::onEmptyEntityKeyGoToActivityHome)
                .withConfirm("sebserver.overall.action.modify.cancel.confirm")
                .publishIf(() -> !isReadonly);
    }

}
