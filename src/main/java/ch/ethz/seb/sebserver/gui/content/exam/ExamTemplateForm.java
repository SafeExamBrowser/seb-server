/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
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
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.IndicatorTemplate;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeleteExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeleteIndicatorTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicatorTemplatePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.NewExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
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

    private static final LocTextKey EXAM_TEMPLATE_DELETE_CONFIRM =
            new LocTextKey("sebserver.examtemplate.form.action.delete.confirm");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final WidgetFactory widgetFactory;
    private final RestService restService;

    public ExamTemplateForm(final PageService pageService) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.widgetFactory = pageService.getWidgetFactory();
        this.restService = pageService.getRestService();
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

        final GrantCheck userGrant = currentUser.grantCheck(EntityType.EXAM_TEMPLATE);
        // propagate content actions to action-pane
        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.EXAM_TEMPLATE_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> userGrant.im() && readonly)

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
                .publishIf(() -> userGrant.iw() && readonly)

        ;

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
                                    it -> ExamFormIndicators.thresholdsValue(it.thresholds, it.type))
                                            .asMarkup()
                                            .widthProportion(4))
                            .withDefaultActionIf(
                                    () -> userGrant.im(),
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
                            indicatorTable::getSelection,
                            PageAction::applySingleSelectionAsEntityKey,
                            INDICATOR_EMPTY_SELECTION_TEXT_KEY)
                    .publishIf(() -> userGrant.im() && indicatorTable.hasAnyContent(), false)

                    .newAction(ActionDefinition.INDICATOR_TEMPLATE_DELETE_FROM_LIST)
                    .withEntityKey(entityKey)
                    .withSelect(
                            indicatorTable::getSelection,
                            this::deleteSelectedIndicator,
                            INDICATOR_EMPTY_SELECTION_TEXT_KEY)
                    .publishIf(() -> userGrant.im() && indicatorTable.hasAnyContent(), false)

                    .newAction(ActionDefinition.INDICATOR_TEMPLATE_NEW)
                    .withParentEntityKey(entityKey)
                    .publishIf(() -> userGrant.im());
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

    private String indicatorTypeName(final IndicatorTemplate indicator) {
        if (indicator.type == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.resourceService.getI18nSupport()
                .getText(ResourceService.EXAM_INDICATOR_TYPE_PREFIX + indicator.type.name());
    }

}
