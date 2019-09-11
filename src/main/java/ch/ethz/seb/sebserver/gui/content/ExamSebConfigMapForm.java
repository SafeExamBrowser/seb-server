/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.NewExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNode;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ExamSebConfigMapForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(ExamSebConfigMapForm.class);

    private static final LocTextKey NEW_CONFIG_MAPPING_TILE_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.title.new");
    private static final LocTextKey CONFIG_MAPPING_TILE_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.title");
    private static final LocTextKey CONFIG_MAPPING_NAME_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.name");
    private static final LocTextKey FORM_DESCRIPTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.description");
    private static final LocTextKey FORM_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.status");
    private static final LocTextKey FORM_ENCRYPT_SECRET_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.encryptSecret");
    private static final LocTextKey FORM_CONFIRM_ENCRYPT_SECRET_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.encryptSecret.confirm");

    private final PageService pageService;
    private final ResourceService resourceService;

    protected ExamSebConfigMapForm(
            final PageService pageService,
            final ResourceService resourceService) {

        this.pageService = pageService;
        this.resourceService = resourceService;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final RestService restService = this.resourceService.getRestService();
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();

        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        final boolean isNew = entityKey == null;
        final boolean isReadonly = pageContext.isReadonly();

        final Exam exam = (isNew)
                ? restService
                        .getBuilder(GetExam.class)
                        .withURIVariable(API.PARAM_MODEL_ID, parentEntityKey.modelId)
                        .call()
                        .get(pageContext::notifyError)
                : null;

        // get data or create new. Handle error if happen
        final ExamConfigurationMap examConfigurationMap = (isNew)
                ? ExamConfigurationMap.createNew(exam)
                : restService
                        .getBuilder(GetExamConfigMapping.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .get(pageContext::notifyError);

        if (examConfigurationMap == null) {
            log.error("Failed to get ExamConfigurationMap. "
                    + "Error is notified to the User. "
                    + "See previous logs for more infomation");
            return;
        }

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(examConfigurationMap.getEntityKey());

        // the default page layout
        final LocTextKey titleKey = (isNew)
                ? NEW_CONFIG_MAPPING_TILE_TEXT_KEY
                : CONFIG_MAPPING_TILE_TEXT_KEY;
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        final FormHandle<ExamConfigurationMap> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content), 4)
                .readonly(isReadonly)
                .putStaticValueIf(() -> !isNew,
                        Domain.EXAM_CONFIGURATION_MAP.ATTR_ID,
                        examConfigurationMap.getModelId())
                .putStaticValue(
                        Domain.EXAM_CONFIGURATION_MAP.ATTR_INSTITUTION_ID,
                        String.valueOf(examConfigurationMap.getInstitutionId()))
                .putStaticValue(
                        Domain.EXAM_CONFIGURATION_MAP.ATTR_EXAM_ID,
                        String.valueOf(examConfigurationMap.examId))

                .addField(FormBuilder.singleSelection(
                        Domain.EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID,
                        CONFIG_MAPPING_NAME_TEXT_KEY,
                        String.valueOf(examConfigurationMap.configurationNodeId),
                        this.resourceService::examConfigurationSelectionResources)
                        .withSelectionListener(this::updateFormValuesFromConfigSelection))

                .addField(FormBuilder.text(
                        Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                        FORM_DESCRIPTION_TEXT_KEY,
                        examConfigurationMap.configDescription)
                        .asArea()
                        .readonly(true))

                .addField(FormBuilder.text(
                        Domain.CONFIGURATION_NODE.ATTR_STATUS,
                        FORM_STATUS_TEXT_KEY,
                        this.resourceService.localizedExamConfigStatusName(examConfigurationMap))
                        .readonly(true))

                .addField(FormBuilder.text(
                        Domain.EXAM_CONFIGURATION_MAP.ATTR_ENCRYPT_SECRET,
                        FORM_ENCRYPT_SECRET_TEXT_KEY)
                        .asPasswordField())
                .addField(FormBuilder.text(
                        ExamConfigurationMap.ATTR_CONFIRM_ENCRYPT_SECRET,
                        FORM_CONFIRM_ENCRYPT_SECRET_TEXT_KEY)
                        .asPasswordField())

                .buildFor((isNew)
                        ? restService.getRestCall(NewExamConfigMapping.class)
                        : restService.getRestCall(SaveExamConfigMapping.class));

        // propagate content actions to action-pane
        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.EXAM_CONFIGURATION_SAVE)
                .withEntityKey(parentEntityKey)
                .withExec(formHandle::processFormSave)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !isReadonly)

                .newAction(ActionDefinition.EXAM_CONFIGURATION_CANCEL_MODIFY)
                .withEntityKey(parentEntityKey)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !isReadonly);
    }

    private void updateFormValuesFromConfigSelection(final Form form) {
        final String configId = form.getFieldValue(Domain.EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID);
        if (StringUtils.isBlank(configId)) {
            form.setFieldValue(Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION, null);
            form.setFieldValue(Domain.CONFIGURATION_NODE.ATTR_STATUS, null);
        } else {
            try {

                final ConfigurationNode configuration = this.resourceService
                        .getRestService()
                        .getBuilder(GetExamConfigNode.class)
                        .withURIVariable(API.PARAM_MODEL_ID, configId)
                        .call()
                        .getOrThrow();

                form.setFieldValue(
                        Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                        configuration.description);
                form.setFieldValue(
                        Domain.CONFIGURATION_NODE.ATTR_STATUS,
                        this.resourceService.localizedExamConfigStatusName(configuration));

            } catch (final Exception e) {
                log.error("Failed to update form values from SEB Configuration selection", e);
                form.setFieldValue(Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION, null);
                form.setFieldValue(Domain.CONFIGURATION_NODE.ATTR_STATUS, null);
            }
        }
    }

}
