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
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNode;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.NewExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class SebExamConfigPropForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(SebExamConfigPropForm.class);

    private static final LocTextKey FORM_TITLE_NEW =
            new LocTextKey("sebserver.examconfig.form.title.new");
    private static final LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.examconfig.form.title");
    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.form.name");
    private static final LocTextKey FORM_DESCRIPTION_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.form.description");
    private static final LocTextKey FORM_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.form.status");

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;

    protected SebExamConfigPropForm(
            final PageService pageService,
            final RestService restService,
            final CurrentUser currentUser) {

        this.pageService = pageService;
        this.restService = restService;
        this.currentUser = currentUser;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final ResourceService resourceService = this.pageService.getResourceService();

        final UserInfo user = this.currentUser.get();
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();

        final boolean isNew = entityKey == null;

        // get data or create new. Handle error if happen
        final ConfigurationNode examConfig = (isNew)
                ? ConfigurationNode.createNewExamConfig((parentEntityKey != null)
                        ? Long.valueOf(parentEntityKey.modelId)
                        : user.institutionId)
                : this.restService
                        .getBuilder(GetExamConfigNode.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .get(pageContext::notifyError);

        if (examConfig == null) {
            log.error("Failed to get ConfigurationNode. "
                    + "Error was notified to the User. "
                    + "See previous logs for more infomation");
            return;
        }

        final EntityGrantCheck entityGrant = this.currentUser.entityGrantCheck(examConfig);
        final boolean writeGrant = entityGrant.w();
        final boolean modifyGrant = entityGrant.m();
        final boolean isReadonly = pageContext.isReadonly();

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(examConfig.getEntityKey());

        // the default page layout with interactive title
        final LocTextKey titleKey = (isNew)
                ? FORM_TITLE_NEW
                : FORM_TITLE;
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        // The SebClientConfig form
        final FormHandle<ConfigurationNode> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content), 4)
                .readonly(isReadonly)
                .putStaticValueIf(() -> !isNew,
                        Domain.CONFIGURATION_NODE.ATTR_ID,
                        examConfig.getModelId())
                .putStaticValue(
                        Domain.CONFIGURATION_NODE.ATTR_INSTITUTION_ID,
                        String.valueOf(examConfig.getInstitutionId()))
                .putStaticValue(
                        Domain.CONFIGURATION_NODE.ATTR_TYPE,
                        ConfigurationType.EXAM_CONFIG.name())
                .addField(FormBuilder.text(
                        Domain.CONFIGURATION_NODE.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        examConfig.name))
                .addField(FormBuilder.text(
                        Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                        FORM_DESCRIPTION_TEXT_KEY,
                        examConfig.description)
                        .asArea())
                .addField(FormBuilder.singleSelection(
                        Domain.CONFIGURATION_NODE.ATTR_STATUS,
                        FORM_STATUS_TEXT_KEY,
                        examConfig.status.name(),
                        resourceService::examConfigStatusResources))
                .buildFor((isNew)
                        ? this.restService.getRestCall(NewExamConfig.class)
                        : this.restService.getRestCall(SaveExamConfig.class));

        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_NEW)
                .publishIf(() -> writeGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_PROP_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_SAVE)
                .withEntityKey(entityKey)
                .withExec(formHandle::processFormSave)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withExec(action -> this.pageService.onEmptyEntityKeyGoTo(
                        action,
                        ActionDefinition.SEB_EXAM_CONFIG_LIST))
                .publishIf(() -> !isReadonly);

    }

}
