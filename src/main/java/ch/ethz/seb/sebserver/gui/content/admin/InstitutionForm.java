/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.admin;

import org.eclipse.swt.widgets.Composite;
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
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.DefaultPageLayout;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.ActivateInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.DeactivateInstitution;
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

    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.institution.form.title");
    private static final LocTextKey NEW_TITLE_TEXT_KEY =
            new LocTextKey("sebserver.institution.form.title.new");

    private static final LocTextKey FORM_LOGO_IMAGE_TEXT_KEY =
            new LocTextKey("sebserver.institution.form.logoImage");
    private static final LocTextKey FORM_URL_SUFFIX_TEXT_KEY =
            new LocTextKey("sebserver.institution.form.urlSuffix");
    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.institution.form.name");

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;

    protected InstitutionForm(final PageService pageService) {

        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.currentUser = pageService.getCurrentUser();
    }

    @Override
    public void compose(final PageContext pageContext) {

        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final EntityKey entityKey = pageContext.getEntityKey();
        final boolean isNew = entityKey == null;
        // get data or create new. Handle error if happen
        final Institution institution = (isNew)
                ? Institution.createNew()
                : this.restService
                        .getBuilder(GetInstitution.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .onError(error -> pageContext.notifyLoadError(EntityType.INSTITUTION, error))
                        .getOrThrow();

        final EntityGrantCheck instGrant = this.currentUser.entityGrantCheck(institution);
        final boolean writeGrant = instGrant.w();
        final boolean modifyGrant = instGrant.m();
        final boolean isReadonly = pageContext.isReadonly();

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(institution.getEntityKey());

        // the default page layout with interactive title
        final LocTextKey titleKey = isNew
                ? NEW_TITLE_TEXT_KEY
                : TITLE_TEXT_KEY;
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        // The Institution form
        final FormHandle<Institution> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content))
                .readonly(isReadonly)
                .putStaticValueIf(() -> !isNew,
                        Domain.INSTITUTION.ATTR_ID,
                        institution.getModelId())
                .addField(FormBuilder.text(
                        Domain.INSTITUTION.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        institution.name)
                        .mandatory(!isReadonly))
                .addField(FormBuilder.text(
                        Domain.INSTITUTION.ATTR_URL_SUFFIX,
                        FORM_URL_SUFFIX_TEXT_KEY,
                        institution.urlSuffix))
                .addField(FormBuilder.imageUpload(
                        Domain.INSTITUTION.ATTR_LOGO_IMAGE,
                        FORM_LOGO_IMAGE_TEXT_KEY,
                        institution.logoImage)
                        .withMaxWidth(DefaultPageLayout.LOGO_IMAGE_MAX_WIDTH)
                        .withMaxHeight(DefaultPageLayout.LOGO_IMAGE_MAX_HEIGHT))
                .buildFor((isNew)
                        ? this.restService.getRestCall(NewInstitution.class)
                        : this.restService.getRestCall(SaveInstitution.class));

        // propagate content actions to action-pane
        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.INSTITUTION_NEW)
                .publishIf(() -> writeGrant && isReadonly)

                .newAction(ActionDefinition.INSTITUTION_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && isReadonly)

                .newAction(ActionDefinition.INSTITUTION_DEACTIVATE)
                .withEntityKey(entityKey)
                .withSimpleRestCall(this.restService, DeactivateInstitution.class)
                .withConfirm(this.pageService.confirmDeactivation(institution))
                .publishIf(() -> writeGrant && isReadonly && institution.isActive())

                .newAction(ActionDefinition.INSTITUTION_ACTIVATE)
                .withEntityKey(entityKey)
                .withSimpleRestCall(this.restService, ActivateInstitution.class)
                .publishIf(() -> writeGrant && isReadonly && !institution.isActive())

                .newAction(ActionDefinition.INSTITUTION_SAVE)
                .withEntityKey(entityKey)
                .withExec(formHandle::processFormSave)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !isReadonly)

                .newAction(ActionDefinition.INSTITUTION_SAVE_AND_ACTIVATE)
                .withEntityKey(entityKey)
                .withExec(formHandle::saveAndActivate)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !isReadonly && !institution.isActive())

                .newAction(ActionDefinition.INSTITUTION_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !isReadonly);
    }

}
