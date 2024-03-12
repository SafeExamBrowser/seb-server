/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroupTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.IndicatorTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.clientgroup.DeleteClientGroupTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.clientgroup.GetClientGroupTemplatePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.indicator.DeleteIndicatorTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.indicator.GetIndicatorTemplatePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.template.DeleteExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.template.GetExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.template.GetExamTemplateProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.template.NewExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.template.SaveExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.widget.ThresholdList;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ExamTemplateForm implements TemplateComposer {

    public static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.form.title");
    public static final LocTextKey TITLE_NEW_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.form.title.new");

    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.form.name");
    private static final LocTextKey FORM_DESCRIPTION_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.form.description");
    private static final LocTextKey FORM_DEFAULT_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.form.default");
    private static final LocTextKey FORM_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.form.examType");
    private static final LocTextKey FORM_CONFIG_TEMPLATE_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.form.examConfigTemplate");
    private static final LocTextKey FORM_SUPPORTER_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.form.supporter");

    private final static LocTextKey INDICATOR_LIST_TITLE_KEY =
            new LocTextKey("sebserver.examtemplate.indicator.list.title");
    private final static LocTextKey INDICATOR_LIST_TITLE_TOOLTIP_KEY =
            new LocTextKey("sebserver.examtemplate.indicator.list.title" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey INDICATOR_TYPE_COLUMN_KEY =
            new LocTextKey("sebserver.examtemplate.indicator.list.column.type");
    private final static LocTextKey INDICATOR_NAME_COLUMN_KEY =
            new LocTextKey("sebserver.examtemplate.indicator.list.column.name");
    private final static LocTextKey INDICATOR_THRESHOLD_COLUMN_KEY =
            new LocTextKey("sebserver.examtemplate.indicator.list.column.thresholds");
    private final static LocTextKey INDICATOR_EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.indicator.list.pleaseSelect");
    private static final LocTextKey INDICATOR_EMPTY_LIST_MESSAGE =
            new LocTextKey("sebserver.examtemplate.indicator.list.empty");

    private final static LocTextKey CLIENT_GROUP_LIST_TITLE_KEY =
            new LocTextKey("sebserver.examtemplate.clientgroup.list.title");
    private final static LocTextKey CLIENT_GROUP_LIST_TITLE_TOOLTIP_KEY =
            new LocTextKey("sebserver.examtemplate.clientgroup.list.title" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey CLIENT_GROUP_TYPE_COLUMN_KEY =
            new LocTextKey("sebserver.examtemplate.clientgroup.list.column.type");
    private final static LocTextKey CLIENT_GROUP_NAME_COLUMN_KEY =
            new LocTextKey("sebserver.examtemplate.clientgroup.list.column.name");
    private final static LocTextKey CLIENT_GROUP_COLOR_COLUMN_KEY =
            new LocTextKey("sebserver.examtemplate.clientgroup.list.column.color");
    private final static LocTextKey CLIENT_GROUP_DATA_COLUMN_KEY =
            new LocTextKey("sebserver.examtemplate.clientgroup.list.column.data");
    private final static LocTextKey CLIENT_GROUP_EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.clientgroup.list.pleaseSelect");
    private static final LocTextKey CLIENT_GROUP_EMPTY_LIST_MESSAGE =
            new LocTextKey("sebserver.examtemplate.clientgroup.list.empty");

    private static final LocTextKey EXAM_TEMPLATE_DELETE_CONFIRM =
            new LocTextKey("sebserver.examtemplate.form.action.delete.confirm");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final WidgetFactory widgetFactory;
    private final RestService restService;
    private final ProctoringSettingsPopup proctoringSettingsPopup;

    public ExamTemplateForm(
            final PageService pageService,
            final ProctoringSettingsPopup proctoringSettingsPopup) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.widgetFactory = pageService.getWidgetFactory();
        this.restService = pageService.getRestService();
        this.proctoringSettingsPopup = proctoringSettingsPopup;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final EntityKey entityKey = pageContext.getEntityKey();
        final boolean readonly = pageContext.isReadonly();
        final boolean isNew = entityKey == null;

        // get or create model data
        final ExamTemplate examTemplate = (isNew)
                ? ExamTemplate.createNew(currentUser.get().institutionId)
                : this.restService
                        .getBuilder(GetExamTemplate.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .onError(error -> pageContext.notifyLoadError(EntityType.EXAM_TEMPLATE, error))
                        .getOrThrow();

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(examTemplate.getEntityKey());

        // the default page layout with interactive title
        final LocTextKey titleKey = isNew
                ? TITLE_NEW_TEXT_KEY
                : TITLE_TEXT_KEY;
        final Composite content = this.widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        final List<Tuple<String>> examConfigTemplateResources = this.resourceService.getExamConfigTemplateResources();

        // The Exam Template form
        final FormHandle<ExamTemplate> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content))
                .readonly(readonly)
                .putStaticValueIf(() -> !isNew,
                        Domain.EXAM_TEMPLATE.ATTR_ID,
                        examTemplate.getModelId())
                .putStaticValueIf(() -> !isNew,
                        Domain.EXAM_TEMPLATE.ATTR_INSTITUTION_ID,
                        String.valueOf(examTemplate.getInstitutionId()))

                .addField(FormBuilder.text(
                        Domain.EXAM_TEMPLATE.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        examTemplate.name)
                        .mandatory(!readonly))

                .addField(FormBuilder.text(
                        Domain.EXAM_TEMPLATE.ATTR_DESCRIPTION,
                        FORM_DESCRIPTION_TEXT_KEY,
                        examTemplate.description)
                        .asArea())

                .addField(FormBuilder.checkbox(
                        Domain.EXAM_TEMPLATE.ATTR_INSTITUTIONAL_DEFAULT,
                        FORM_DEFAULT_TEXT_KEY,
                        String.valueOf(examTemplate.getInstitutionalDefault())))

                .addField(FormBuilder.singleSelection(
                        Domain.EXAM_TEMPLATE.ATTR_EXAM_TYPE,
                        FORM_TYPE_TEXT_KEY,
                        (examTemplate.examType != null) ? String.valueOf(examTemplate.examType)
                                : Exam.ExamType.UNDEFINED.name(),
                        this.resourceService::examTypeResources))

                .addFieldIf(
                        () -> !examConfigTemplateResources.isEmpty(),
                        () -> FormBuilder.singleSelection(
                                Domain.EXAM_TEMPLATE.ATTR_CONFIGURATION_TEMPLATE_ID,
                                FORM_CONFIG_TEMPLATE_TEXT_KEY,
                                String.valueOf(examTemplate.configTemplateId),
                                this.resourceService::getExamConfigTemplateResources))

                .addField(FormBuilder.multiComboSelection(
                        Domain.EXAM_TEMPLATE.ATTR_SUPPORTER,
                        FORM_SUPPORTER_TEXT_KEY,
                        StringUtils.join(examTemplate.supporter, Constants.LIST_SEPARATOR_CHAR),
                        this.resourceService::examSupporterResources))

                .buildFor((isNew)
                        ? this.restService.getRestCall(NewExamTemplate.class)
                        : this.restService.getRestCall(SaveExamTemplate.class));

        final boolean proctoringEnabled = !isNew && this.restService
                .getBuilder(GetExamTemplateProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .map(ProctoringServiceSettings::getEnableProctoring)
                .getOr(false);

        final EntityGrantCheck userGrantCheck = currentUser.entityGrantCheck(examTemplate);
        // propagate content actions to action-pane
        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.EXAM_TEMPLATE_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> userGrantCheck.m() && readonly)

                .newAction(ActionDefinition.EXAM_TEMPLATE_SAVE)
                .withEntityKey(entityKey)
                .withExec(formHandle::processFormSave)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !readonly)

                .newAction(ActionDefinition.EXAM_TEMPLATE_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !readonly)

                .newAction(ActionDefinition.EXAM_TEMPLATE_DELETE)
                .withEntityKey(entityKey)
                .withConfirm(() -> EXAM_TEMPLATE_DELETE_CONFIRM)
                .withExec(this::deleteExamTemplate)
                .publishIf(() -> userGrantCheck.w() && readonly)

                .newAction(ActionDefinition.EXAM_TEMPLATE_PROCTORING_ON)
                .withEntityKey(entityKey)
                .withExec(this.proctoringSettingsPopup.settingsFunction(this.pageService, userGrantCheck.m()))
                .noEventPropagation()
                .publishIf(() -> proctoringEnabled && readonly)

                .newAction(ActionDefinition.EXAM_TEMPLATE_PROCTORING_OFF)
                .withEntityKey(entityKey)
                .withExec(this.proctoringSettingsPopup.settingsFunction(this.pageService, userGrantCheck.m()))
                .noEventPropagation()
                .publishIf(() -> !proctoringEnabled && readonly);

        if (readonly) {

            // List of Indicators
            this.widgetFactory.addFormSubContextHeader(
                    content,
                    INDICATOR_LIST_TITLE_KEY,
                    INDICATOR_LIST_TITLE_TOOLTIP_KEY);

            final PageActionBuilder actionBuilder = this.pageService
                    .pageActionBuilder(pageContext.clearEntityKeys());

            final EntityTable<IndicatorTemplate> indicatorTable =
                    this.pageService
                            .entityTableBuilder(this.restService.getRestCall(GetIndicatorTemplatePage.class))
                            .withRestCallAdapter(builder -> builder.withURIVariable(
                                    API.PARAM_PARENT_MODEL_ID,
                                    entityKey.modelId))
                            .withEmptyMessage(INDICATOR_EMPTY_LIST_MESSAGE)
                            .withMarkup()
                            .withPaging(100)
                            .hideNavigation()
                            .withColumn(new ColumnDefinition<>(
                                    Domain.INDICATOR.ATTR_NAME,
                                    INDICATOR_NAME_COLUMN_KEY,
                                    IndicatorTemplate::getName)
                                            .widthProportion(2))
                            .withColumn(new ColumnDefinition<>(
                                    Domain.INDICATOR.ATTR_TYPE,
                                    INDICATOR_TYPE_COLUMN_KEY,
                                    this::indicatorTypeName)
                                            .widthProportion(1))
                            .withColumn(new ColumnDefinition<IndicatorTemplate>(
                                    Domain.THRESHOLD.REFERENCE_NAME,
                                    INDICATOR_THRESHOLD_COLUMN_KEY,
                                    it -> ThresholdList.thresholdsToHTML(it.thresholds, it.type))
                                            .asMarkup()
                                            .widthProportion(4))
                            .withDefaultActionIf(
                                    () -> userGrantCheck.m(),
                                    () -> actionBuilder
                                            .newAction(ActionDefinition.INDICATOR_TEMPLATE_MODIFY_FROM_LIST)
                                            .withParentEntityKey(entityKey)
                                            .create())

                            .withSelectionListener(this.pageService.getSelectionPublisher(
                                    pageContext,
                                    ActionDefinition.INDICATOR_TEMPLATE_MODIFY_FROM_LIST,
                                    ActionDefinition.INDICATOR_TEMPLATE_DELETE_FROM_LIST))

                            .compose(pageContext.copyOf(content));

            actionBuilder

                    .newAction(ActionDefinition.INDICATOR_TEMPLATE_MODIFY_FROM_LIST)
                    .withParentEntityKey(entityKey)
                    .withSelect(
                            indicatorTable::getMultiSelection,
                            PageAction::applySingleSelectionAsEntityKey,
                            INDICATOR_EMPTY_SELECTION_TEXT_KEY)
                    .publishIf(() -> userGrantCheck.m() && indicatorTable.hasAnyContent(), false)

                    .newAction(ActionDefinition.INDICATOR_TEMPLATE_DELETE_FROM_LIST)
                    .withEntityKey(entityKey)
                    .withSelect(
                            indicatorTable::getMultiSelection,
                            this::deleteSelectedIndicator,
                            INDICATOR_EMPTY_SELECTION_TEXT_KEY)
                    .publishIf(() -> userGrantCheck.m() && indicatorTable.hasAnyContent(), false)

                    .newAction(ActionDefinition.INDICATOR_TEMPLATE_NEW)
                    .withParentEntityKey(entityKey)
                    .publishIf(() -> userGrantCheck.m());

            // List of Client Groups
            this.widgetFactory.addFormSubContextHeader(
                    content,
                    CLIENT_GROUP_LIST_TITLE_KEY,
                    CLIENT_GROUP_LIST_TITLE_TOOLTIP_KEY);

            final EntityTable<ClientGroupTemplate> clientGroupTable =
                    this.pageService
                            .entityTableBuilder(this.restService.getRestCall(GetClientGroupTemplatePage.class))
                            .withRestCallAdapter(builder -> builder.withURIVariable(
                                    API.PARAM_PARENT_MODEL_ID,
                                    entityKey.modelId))
                            .withEmptyMessage(CLIENT_GROUP_EMPTY_LIST_MESSAGE)
                            .withMarkup()
                            .withPaging(100)
                            .hideNavigation()
                            .withColumn(new ColumnDefinition<>(
                                    Domain.CLIENT_GROUP.ATTR_NAME,
                                    CLIENT_GROUP_NAME_COLUMN_KEY,
                                    ClientGroupTemplate::getName)
                                            .widthProportion(2))
                            .withColumn(new ColumnDefinition<ClientGroupTemplate>(
                                    Domain.CLIENT_GROUP.ATTR_TYPE,
                                    CLIENT_GROUP_TYPE_COLUMN_KEY,
                                    cgt -> this.resourceService.clientGroupTypeName(cgt))
                                            .widthProportion(1))
                            .withColumn(new ColumnDefinition<ClientGroupTemplate>(
                                    Domain.CLIENT_GROUP.ATTR_COLOR,
                                    CLIENT_GROUP_COLOR_COLUMN_KEY,
                                    cgt -> WidgetFactory.getColorValueHTML(cgt))
                                            .asMarkup()
                                            .widthProportion(1))
                            .withColumn(new ColumnDefinition<ClientGroupTemplate>(
                                    Domain.CLIENT_GROUP.ATTR_DATA,
                                    CLIENT_GROUP_DATA_COLUMN_KEY,
                                    cgt -> this.widgetFactory.clientGroupDataToHTML(cgt))
                                            .asMarkup()
                                            .widthProportion(4))
                            .withDefaultActionIf(
                                    () -> userGrantCheck.m(),
                                    () -> actionBuilder
                                            .newAction(ActionDefinition.CLIENT_GROUP_TEMPLATE_MODIFY_FROM_LIST)
                                            .withParentEntityKey(entityKey)
                                            .create())

                            .withSelectionListener(this.pageService.getSelectionPublisher(
                                    pageContext,
                                    ActionDefinition.CLIENT_GROUP_TEMPLATE_MODIFY_FROM_LIST,
                                    ActionDefinition.CLIENT_GROUP_TEMPLATE_DELETE_FROM_LIST))

                            .compose(pageContext.copyOf(content));

            actionBuilder

                    .newAction(ActionDefinition.CLIENT_GROUP_TEMPLATE_MODIFY_FROM_LIST)
                    .withParentEntityKey(entityKey)
                    .withSelect(
                            clientGroupTable::getMultiSelection,
                            PageAction::applySingleSelectionAsEntityKey,
                            CLIENT_GROUP_EMPTY_SELECTION_TEXT_KEY)
                    .publishIf(() -> userGrantCheck.m() && clientGroupTable.hasAnyContent(), false)

                    .newAction(ActionDefinition.CLIENT_GROUP_TEMPLATE_DELETE_FROM_LIST)
                    .withEntityKey(entityKey)
                    .withSelect(
                            clientGroupTable::getMultiSelection,
                            this::deleteSelectedClientGroup,
                            CLIENT_GROUP_EMPTY_SELECTION_TEXT_KEY)
                    .publishIf(() -> userGrantCheck.m() && clientGroupTable.hasAnyContent(), false)

                    .newAction(ActionDefinition.CLIENT_GROUP_TEMPLATE_NEW)
                    .withParentEntityKey(entityKey)
                    .publishIf(() -> userGrantCheck.m());
        }
    }

    private PageAction deleteExamTemplate(final PageAction action) {
        this.pageService.getRestService().getBuilder(DeleteExamTemplate.class)
                .withURIVariable(API.PARAM_MODEL_ID, action.getEntityKey().modelId)
                .call()
                .onError(error -> action.pageContext().notifyUnexpectedError(error));
        return action;
    }

    private PageAction deleteSelectedIndicator(final PageAction action) {
        final EntityKey entityKey = action.getEntityKey();
        final EntityKey indicatorKey = action.getSingleSelection();
        this.resourceService.getRestService()
                .getBuilder(DeleteIndicatorTemplate.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, entityKey.modelId)
                .withURIVariable(API.PARAM_MODEL_ID, indicatorKey.modelId)
                .call();
        return action;
    }

    private PageAction deleteSelectedClientGroup(final PageAction action) {
        final EntityKey entityKey = action.getEntityKey();
        final EntityKey indicatorKey = action.getSingleSelection();
        this.resourceService.getRestService()
                .getBuilder(DeleteClientGroupTemplate.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, entityKey.modelId)
                .withURIVariable(API.PARAM_MODEL_ID, indicatorKey.modelId)
                .call();
        return action;
    }

    private String indicatorTypeName(final IndicatorTemplate indicator) {
        if (indicator.type == null) {
            return Constants.EMPTY_NOTE;
        }
        return this.resourceService.getI18nSupport()
                .getText(ResourceService.EXAM_INDICATOR_TYPE_PREFIX + indicator.type.name());
    }

}
