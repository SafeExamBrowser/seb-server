/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.configs;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.content.exam.ExamList;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamConfigMappingNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamConfigMappingsPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.DeleteExamConfiguration;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ExportConfigKey;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNode;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.NewExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class SEBExamConfigForm implements TemplateComposer {

    static final LocTextKey FORM_TITLE_NEW =
            new LocTextKey("sebserver.examconfig.form.title.new");
    static final LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.examconfig.form.title");
    static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.form.name");
    static final LocTextKey FORM_DESCRIPTION_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.form.description");
    static final LocTextKey FORM_HISTORY_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.form.with-history");
    static final LocTextKey FORM_TEMPLATE_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.form.template");
    static final LocTextKey FORM_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.form.status");
    static final LocTextKey FORM_IMPORT_SETTINGS_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.action.import-settings");
    static final LocTextKey FORM_IMPORT_CONFIG_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.action.import-config");
    static final LocTextKey FORM_IMPORT_SELECT_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.action.import-file-select");
    static final LocTextKey FORM_IMPORT_PASSWORD_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.action.import-file-password");
    static final LocTextKey CONFIG_KEY_TITLE_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.form.config-key.title");
    static final LocTextKey FORM_IMPORT_CONFIRM_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.action.import-config.confirm");
    static final LocTextKey FORM_ATTACHED_EXAMS_TITLE_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.form.attached-to");
    static final LocTextKey FORM_ATTACHED_EXAMS_TITLE_TOOLTIP_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.form.attached-to" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);

    static final LocTextKey SAVE_CONFIRM_STATE_CHANGE_WHILE_ATTACHED =
            new LocTextKey("sebserver.examconfig.action.state-change.confirm");
    static final LocTextKey FIELD_VAL_UNIQUE_NAME =
            new LocTextKey("sebserver.form.validation.fieldError.name.notunique");
    static final LocTextKey FIELD_VAL_NOT_NULL_KEY =
            new LocTextKey("sebserver.form.validation.fieldError.notNull");
    static final LocTextKey FORM_IMPORT_ERROR_TITLE =
            new LocTextKey("sebserver.error.unexpected");
    static final LocTextKey FORM_IMPORT_ERROR_FILE_SELECTION =
            new LocTextKey("sebserver.examconfig.message.error.file");
    static final LocTextKey CONFIRM_DELETE =
            new LocTextKey("sebserver.examconfig.message.confirm.delete");
    static final LocTextKey DELETE_CONFIRM_TITLE =
            new LocTextKey("sebserver.dialog.confirm.title");
    private final static LocTextKey DELETE_ERROR_CONSISTENCY =
            new LocTextKey("sebserver.examconfig.message.consistency.error");
    private final static LocTextKey DELETE_ERROR_DEPENDENCY =
            new LocTextKey("sebserver.examconfig.message.delete.partialerror");
    private final static LocTextKey DELETE_CONFIRM =
            new LocTextKey("sebserver.examconfig.message.delete.confirm");

    private final PageService pageService;
    private final RestService restService;
    private final SEBExamConfigCreationPopup sebExamConfigCreationPopup;
    private final CurrentUser currentUser;

    protected SEBExamConfigForm(
            final PageService pageService,
            final SEBExamConfigCreationPopup sebExamConfigCreationPopup) {

        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.sebExamConfigCreationPopup = sebExamConfigCreationPopup;
        this.currentUser = pageService.getCurrentUser();
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
                ? ConfigurationNode.createNewExamConfig(user.institutionId)
                : this.restService
                        .getBuilder(GetExamConfigNode.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .onError(error -> pageContext.notifyLoadError(EntityType.CONFIGURATION_NODE, error))
                        .getOrThrow();

        final EntityGrantCheck entityGrant = this.currentUser.entityGrantCheck(examConfig);
        final boolean writeGrant = entityGrant.w();
        final boolean modifyGrant = entityGrant.m();
        final boolean isReadonly = pageContext.isReadonly();
        final boolean isAttachedToExam = !isNew && this.restService
                .getBuilder(GetExamConfigMappingNames.class)
                .withQueryParam(ExamConfigurationMap.FILTER_ATTR_CONFIG_ID, examConfig.getModelId())
                .call()
                .map(names -> names != null && !names.isEmpty())
                .getOr(Boolean.FALSE);

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(examConfig.getEntityKey());

        // the default page layout with interactive title
        final LocTextKey titleKey = (isNew)
                ? FORM_TITLE_NEW
                : FORM_TITLE;
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        final List<Tuple<String>> examConfigTemplateResources = resourceService.getExamConfigTemplateResources();
        final FormHandle<ConfigurationNode> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content))
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
                .addFieldIf(
                        () -> !examConfigTemplateResources.isEmpty(),
                        () -> FormBuilder.singleSelection(
                                Domain.CONFIGURATION_NODE.ATTR_TEMPLATE_ID,
                                FORM_TEMPLATE_TEXT_KEY,
                                (parentEntityKey != null)
                                        ? parentEntityKey.modelId
                                        : String.valueOf(examConfig.templateId),
                                resourceService::getExamConfigTemplateResources)
                                .readonly(!isNew))
                .addField(FormBuilder.text(
                        Domain.CONFIGURATION_NODE.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        examConfig.name)
                        .mandatory(!isReadonly))
                .addField(FormBuilder.text(
                        Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                        FORM_DESCRIPTION_TEXT_KEY,
                        examConfig.description)
                        .asArea())

                .addField(FormBuilder.singleSelection(
                        Domain.CONFIGURATION_NODE.ATTR_STATUS,
                        FORM_STATUS_TEXT_KEY,
                        examConfig.status.name(),
                        () -> resourceService.examConfigStatusResources(isAttachedToExam))
                        .withEmptyCellSeparation(!isReadonly))
                .buildFor((isNew)
                        ? this.restService.getRestCall(NewExamConfig.class)
                        : this.restService.getRestCall(SaveExamConfig.class));

        final PageContext actionContext = formContext.clearEntityKeys();
        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(actionContext);
        actionBuilder

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_NEW)
                .publishIf(() -> writeGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_PROP_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_DELETE)
                .withEntityKey(entityKey)
                .withConfirm(() -> CONFIRM_DELETE)
                .withExec(this::deleteConfiguration)
                .publishIf(() -> writeGrant && examConfig.status != ConfigurationStatus.IN_USE && isReadonly)

                .newAction((!modifyGrant || examConfig.status == ConfigurationStatus.IN_USE)
                        ? ActionDefinition.SEB_EXAM_CONFIG_VIEW
                        : ActionDefinition.SEB_EXAM_CONFIG_MODIFY)
                .withEntityKey(entityKey)
                .withAttribute(PageContext.AttributeKeys.READ_ONLY, String.valueOf(!modifyGrant))
                .publishIf(() -> isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_COPY_CONFIG)
                .withEntityKey(entityKey)
                .withExec(this.sebExamConfigCreationPopup.configCreationFunction(
                        actionContext
                                .withEntityKey(entityKey)
                                .withAttribute(
                                        PageContext.AttributeKeys.COPY_AS_TEMPLATE,
                                        Constants.FALSE_STRING)
                                .withAttribute(
                                        PageContext.AttributeKeys.CREATE_FROM_TEMPLATE,
                                        Constants.FALSE_STRING)))
                .noEventPropagation()
                .publishIf(() -> modifyGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_COPY_CONFIG_AS_TEMPLATE)
                .withEntityKey(entityKey)
                .withExec(this.sebExamConfigCreationPopup.configCreationFunction(
                        pageContext.withAttribute(
                                PageContext.AttributeKeys.COPY_AS_TEMPLATE,
                                Constants.TRUE_STRING)))
                .noEventPropagation()
                .publishIf(() -> modifyGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_GET_CONFIG_KEY)
                .withEntityKey(entityKey)
                .withExec(SEBExamConfigForm.getConfigKeyFunction(this.pageService))
                .noEventPropagation()
                .publishIf(() -> modifyGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_PROP_SAVE)
                .withEntityKey(entityKey)
                .withExec(formHandle::processFormSave)
                .ignoreMoveAwayFromEdit()
                .withConfirm(() -> stateChangeConfirm(isAttachedToExam, formHandle))
                .publishIf(() -> !isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_PROP_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !isReadonly);

        if (isAttachedToExam && isReadonly) {

            widgetFactory.addFormSubContextHeader(
                    content,
                    FORM_ATTACHED_EXAMS_TITLE_TEXT_KEY,
                    FORM_ATTACHED_EXAMS_TITLE_TOOLTIP_TEXT_KEY);

            final EntityTable<ExamConfigurationMap> table =
                    this.pageService.entityTableBuilder(this.restService.getRestCall(GetExamConfigMappingsPage.class))
                            .withRestCallAdapter(restCall -> restCall.withQueryParam(
                                    ExamConfigurationMap.FILTER_ATTR_CONFIG_ID, examConfig.getModelId()))
                            .withPaging(1)
                            .hideNavigation()
                            .withRowDecorator(ExamList.decorateOnExamMapConsistency(this.pageService))

                            .withColumn(new ColumnDefinition<>(
                                    QuizData.QUIZ_ATTR_NAME,
                                    ExamList.COLUMN_TITLE_NAME_KEY,
                                    ExamConfigurationMap::getExamName))

                            .withColumn(new ColumnDefinition<>(
                                    QuizData.QUIZ_ATTR_START_TIME,
                                    new LocTextKey(
                                            ExamList.EXAM_LIST_COLUMN_START_TIME,
                                            this.pageService.getI18nSupport().getUsersTimeZoneTitleSuffix()),
                                    ExamConfigurationMap::getExamStartTime))

                            .withColumn(new ColumnDefinition<>(
                                    Domain.EXAM.ATTR_TYPE,
                                    ExamList.COLUMN_TITLE_TYPE_KEY,
                                    resourceService::localizedExamTypeName))

                            .withDefaultAction(this::showExamAction)

                            .withSelectionListener(this.pageService.getSelectionPublisher(
                                    pageContext,
                                    ActionDefinition.EXAM_VIEW_FROM_LIST))

                            .compose(pageContext.copyOf(content));

            actionBuilder

                    .newAction(ActionDefinition.EXAM_VIEW_FROM_LIST)
                    .withExec(pageAction -> {
                        final ExamConfigurationMap selectedExamMapping = getSelectedExamMapping(table);
                        return pageAction.withEntityKey(
                                new EntityKey(selectedExamMapping.examId, EntityType.EXAM));
                    })
                    .publish(false);
        }
    }

    private PageAction deleteConfiguration(final PageAction action) {
        final ConfigurationNode configNode = this.restService
                .getBuilder(GetExamConfigNode.class)
                .withURIVariable(API.PARAM_MODEL_ID, action.getEntityKey().modelId)
                .call()
                .getOrThrow();

        final Result<EntityProcessingReport> call = this.restService
                .getBuilder(DeleteExamConfiguration.class)
                .withURIVariable(API.PARAM_MODEL_ID, action.getEntityKey().modelId)
                .call();

        final PageContext pageContext = action.pageContext();

        if (call.hasError()) {
            final Exception error = call.getError();
            if (error instanceof RestCallError) {
                final APIMessage message = ((RestCallError) error)
                        .getAPIMessages()
                        .stream()
                        .findFirst()
                        .orElse(null);
                if (message != null && ErrorMessage.INTEGRITY_VALIDATION.isOf(message)) {
                    pageContext.publishPageMessage(new PageMessageException(DELETE_ERROR_CONSISTENCY));
                    return action;
                }
            }
        }

        final EntityProcessingReport report = call.getOrThrow();
        final String configName = configNode.toName().name;
        if (report.getErrors().isEmpty()) {
            pageContext.publishPageMessage(DELETE_CONFIRM_TITLE, new LocTextKey(DELETE_CONFIRM.name, configName));
        } else {
            pageContext.publishPageMessage(
                    DELETE_CONFIRM_TITLE,
                    new LocTextKey(DELETE_ERROR_DEPENDENCY.name, configName,
                            report.getErrors().iterator().next().getErrorMessage().systemMessage));
        }

        return this.pageService.pageActionBuilder(pageContext)
                .newAction(ActionDefinition.SEB_EXAM_CONFIG_LIST)
                .create();
    }

    private PageAction showExamAction(final EntityTable<ExamConfigurationMap> table) {
        return this.pageService.pageActionBuilder(table.getPageContext())
                .newAction(ActionDefinition.EXAM_VIEW_FROM_LIST)
                .withSelectionSupplier(() -> {
                    final ExamConfigurationMap selectedROWData = getSelectedExamMapping(table);
                    return new HashSet<>(
                            Collections.singletonList(new EntityKey(selectedROWData.examId, EntityType.EXAM)));
                })
                .withExec(PageAction::applySingleSelectionAsEntityKey)
                .create();
    }

    private ExamConfigurationMap getSelectedExamMapping(final EntityTable<ExamConfigurationMap> table) {
        final ExamConfigurationMap selectedROWData = table.getSingleSelectedROWData();

        if (selectedROWData == null) {
            throw new PageMessageException(ExamList.EMPTY_SELECTION_TEXT_KEY);
        }
        return selectedROWData;
    }

    private LocTextKey stateChangeConfirm(
            final boolean isAttachedToExam,
            final FormHandle<ConfigurationNode> formHandle) {

        if (isAttachedToExam) {
            final String fieldValue = formHandle
                    .getForm()
                    .getFieldValue(Domain.CONFIGURATION_NODE.ATTR_STATUS);

            if (fieldValue != null) {
                final ConfigurationStatus state = ConfigurationStatus.valueOf(fieldValue);
                if (state != ConfigurationStatus.IN_USE) {
                    return SAVE_CONFIRM_STATE_CHANGE_WHILE_ATTACHED;
                }
            }
        }

        return null;
    }

    public static Function<PageAction, PageAction> getConfigKeyFunction(final PageService pageService) {
        final RestService restService = pageService.getResourceService().getRestService();
        return action -> {
            final ConfigKey configKey = restService.getBuilder(ExportConfigKey.class)
                    .withURIVariable(API.PARAM_MODEL_ID, action.getEntityKey().modelId)
                    .call()
                    .getOrThrow();

            final WidgetFactory widgetFactory = pageService.getWidgetFactory();
            final ModalInputDialog<Void> dialog = new ModalInputDialog<>(
                    action.pageContext().getParent().getShell(),
                    widgetFactory);

            dialog.setDialogWidth(500);

            dialog.open(
                    CONFIG_KEY_TITLE_TEXT_KEY,
                    action.pageContext(),
                    pc -> {
                        final Composite content = widgetFactory.defaultPageLayout(
                                pc.getParent());

                        widgetFactory.labelLocalized(
                                content,
                                CustomVariant.TEXT_H3,
                                CONFIG_KEY_TITLE_TEXT_KEY);

                        final Text text = new Text(content, SWT.NONE);
                        text.setEditable(false);
                        text.setText(configKey.key);
                    });
            return action;
        };
    }

}
