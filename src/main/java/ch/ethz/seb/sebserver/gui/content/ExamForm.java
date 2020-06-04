/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.Features;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.download.DownloadService;
import ch.ethz.seb.sebserver.gui.service.remote.download.SEBExamConfigDownload;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.CheckExamConsistency;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.CheckSEBRestriction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeleteExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeleteIndicator;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamConfigMappingsPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicatorPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.GetLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.GetQuizData;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.ImportAsExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class ExamForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(ExamForm.class);

    public static final LocTextKey EXAM_FORM_TITLE_KEY =
            new LocTextKey("sebserver.exam.form.title");
    public static final LocTextKey EXAM_FORM_TITLE_IMPORT_KEY =
            new LocTextKey("sebserver.exam.form.title.import");
    private static final LocTextKey CONFIG_EMPTY_LIST_MESSAGE =
            new LocTextKey("sebserver.exam.configuration.list.empty");
    private static final LocTextKey INDICATOR_EMPTY_LIST_MESSAGE =
            new LocTextKey("sebserver.exam.indicator.list.empty");
    private static final LocTextKey FORM_SUPPORTER_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.supporter");
    private static final LocTextKey FORM_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.status");
    private static final LocTextKey FORM_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.type");
    private static final LocTextKey FORM_END_TIME_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.endtime");
    private static final LocTextKey FORM_START_TIME_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.starttime");
    private static final LocTextKey FORM_DESCRIPTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.description");
    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.name");
    private static final LocTextKey FORM_QUIZ_ID_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.quizid");
    private static final LocTextKey FORM_QUIZ_URL_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.quizurl");
    private static final LocTextKey FORM_LMSSETUP_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.lmssetup");

    private final static LocTextKey CONFIG_LIST_TITLE_KEY =
            new LocTextKey("sebserver.exam.configuration.list.title");
    private final static LocTextKey CONFIG_LIST_TITLE_TOOLTIP_KEY =
            new LocTextKey("sebserver.exam.configuration.list.title" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey CONFIG_NAME_COLUMN_KEY =
            new LocTextKey("sebserver.exam.configuration.list.column.name");
    private final static LocTextKey CONFIG_DESCRIPTION_COLUMN_KEY =
            new LocTextKey("sebserver.exam.configuration.list.column.description");
    private final static LocTextKey CONFIG_STATUS_COLUMN_KEY =
            new LocTextKey("sebserver.exam.configuration.list.column.status");
    private final static LocTextKey CONFIG_EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.list.pleaseSelect");

    private final static LocTextKey INDICATOR_LIST_TITLE_KEY =
            new LocTextKey("sebserver.exam.indicator.list.title");
    private final static LocTextKey INDICATOR_LIST_TITLE_TOOLTIP_KEY =
            new LocTextKey("sebserver.exam.indicator.list.title" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey INDICATOR_TYPE_COLUMN_KEY =
            new LocTextKey("sebserver.exam.indicator.list.column.type");
    private final static LocTextKey INDICATOR_NAME_COLUMN_KEY =
            new LocTextKey("sebserver.exam.indicator.list.column.name");
    private final static LocTextKey INDICATOR_THRESHOLD_COLUMN_KEY =
            new LocTextKey("sebserver.exam.indicator.list.column.thresholds");
    private final static LocTextKey INDICATOR_EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.indicator.list.pleaseSelect");

    private final static LocTextKey CONSISTENCY_MESSAGE_TITLE =
            new LocTextKey("sebserver.exam.consistency.title");
    private final static LocTextKey CONSISTENCY_MESSAGE_MISSING_SUPPORTER =
            new LocTextKey("sebserver.exam.consistency.missing-supporter");
    private final static LocTextKey CONSISTENCY_MESSAGE_MISSING_INDICATOR =
            new LocTextKey("sebserver.exam.consistency.missing-indicator");
    private final static LocTextKey CONSISTENCY_MESSAGE_MISSING_CONFIG =
            new LocTextKey("sebserver.exam.consistency.missing-config");
    private final static LocTextKey CONSISTENCY_MESSAGE_MISSING_SEB_RESTRICTION =
            new LocTextKey("sebserver.exam.consistency.missing-seb-restriction");

    private final Map<String, LocTextKey> consistencyMessageMapping;

    private final static LocTextKey CONFIRM_MESSAGE_REMOVE_CONFIG =
            new LocTextKey("sebserver.exam.confirm.remove-config");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final DownloadService downloadService;
    private final String downloadFileName;
    private final WidgetFactory widgetFactory;
    private final RestService restService;

    protected ExamForm(
            final PageService pageService,
            final ResourceService resourceService,
            final DownloadService downloadService,
            @Value("${sebserver.gui.seb.exam.config.download.filename}") final String downloadFileName) {

        this.pageService = pageService;
        this.resourceService = resourceService;
        this.downloadService = downloadService;
        this.downloadFileName = downloadFileName;
        this.widgetFactory = pageService.getWidgetFactory();
        this.restService = this.resourceService.getRestService();

        this.consistencyMessageMapping = new HashMap<>();
        this.consistencyMessageMapping.put(
                APIMessage.ErrorMessage.EXAM_CONSISTENCY_VALIDATION_SUPPORTER.messageCode,
                CONSISTENCY_MESSAGE_MISSING_SUPPORTER);
        this.consistencyMessageMapping.put(
                APIMessage.ErrorMessage.EXAM_CONSISTENCY_VALIDATION_INDICATOR.messageCode,
                CONSISTENCY_MESSAGE_MISSING_INDICATOR);
        this.consistencyMessageMapping.put(
                APIMessage.ErrorMessage.EXAM_CONSISTENCY_VALIDATION_CONFIG.messageCode,
                CONSISTENCY_MESSAGE_MISSING_CONFIG);
        this.consistencyMessageMapping.put(
                APIMessage.ErrorMessage.EXAM_CONSISTENCY_VALIDATION_SEB_RESTRICTION.messageCode,
                CONSISTENCY_MESSAGE_MISSING_SEB_RESTRICTION);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = this.resourceService.getCurrentUser();

        final I18nSupport i18nSupport = this.resourceService.getI18nSupport();
        final EntityKey entityKey = pageContext.getEntityKey();
        final boolean readonly = pageContext.isReadonly();
        final boolean importFromQuizData = BooleanUtils.toBoolean(
                pageContext.getAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA));

        // get or create model data
        final Exam exam = (importFromQuizData
                ? createExamFromQuizData(pageContext)
                : getExistingExam(pageContext))
                        .onError(error -> pageContext.notifyLoadError(EntityType.EXAM, error))
                        .getOrThrow();

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(exam.getEntityKey());

        // check exam consistency and inform the user if needed
        Collection<APIMessage> warnings = null;
        if (readonly) {
            warnings = this.restService.getBuilder(CheckExamConsistency.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .getOr(Collections.emptyList());
            if (warnings != null && !warnings.isEmpty()) {
                showConsistencyChecks(warnings, formContext.getParent());
            }
        }

        // the default page layout with title
        final LocTextKey titleKey = importFromQuizData
                ? EXAM_FORM_TITLE_IMPORT_KEY
                : EXAM_FORM_TITLE_KEY;
        final Composite content = this.widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);
        if (warnings != null && !warnings.isEmpty()) {
            final GridData gridData = (GridData) content.getLayoutData();
            gridData.verticalIndent = 10;
        }

        final BooleanSupplier isNew = () -> importFromQuizData;
        final BooleanSupplier isNotNew = () -> !isNew.getAsBoolean();
        final EntityGrantCheck userGrantCheck = currentUser.entityGrantCheck(exam);
        final boolean modifyGrant = userGrantCheck.m();
        final ExamStatus examStatus = exam.getStatus();
        final boolean isExamRunning = examStatus == ExamStatus.RUNNING;
        final boolean editable = examStatus == ExamStatus.UP_COMING
                || examStatus == ExamStatus.RUNNING
                        && currentUser.get().hasRole(UserRole.EXAM_ADMIN);
        final boolean sebRestrictionAvailable = testSEBRestrictionAPI(exam);
        final boolean isRestricted = readonly && sebRestrictionAvailable && this.restService
                .getBuilder(CheckSEBRestriction.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call()
                .onError(e -> log.error("Unexpected error while trying to verify seb restriction settings: ", e))
                .getOr(false);

        // The Exam form
        final FormHandle<Exam> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content), 8)
                .withDefaultSpanLabel(1)
                .withDefaultSpanInput(4)
                .withDefaultSpanEmptyCell(3)
                .readonly(readonly)
                .putStaticValueIf(isNotNew,
                        Domain.EXAM.ATTR_ID,
                        exam.getModelId())
                .putStaticValue(
                        Domain.EXAM.ATTR_INSTITUTION_ID,
                        String.valueOf(exam.getInstitutionId()))
                .putStaticValueIf(isNotNew,
                        Domain.EXAM.ATTR_LMS_SETUP_ID,
                        String.valueOf(exam.lmsSetupId))
                .putStaticValueIf(isNew,
                        QuizData.QUIZ_ATTR_LMS_SETUP_ID,
                        String.valueOf(exam.lmsSetupId))
                .putStaticValueIf(isNotNew,
                        Domain.EXAM.ATTR_EXTERNAL_ID,
                        exam.externalId)
                .putStaticValueIf(isNew,
                        QuizData.QUIZ_ATTR_ID,
                        exam.externalId)

                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        exam.name)
                        .readonly(true)
                        .withInputSpan(3)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.singleSelection(
                        Domain.EXAM.ATTR_LMS_SETUP_ID,
                        FORM_LMSSETUP_TEXT_KEY,
                        String.valueOf(exam.lmsSetupId),
                        this.resourceService::lmsSetupResource)
                        .readonly(true)
                        .withInputSpan(3)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_START_TIME,
                        FORM_START_TIME_TEXT_KEY,
                        i18nSupport.formatDisplayDateWithTimeZone(exam.startTime))
                        .readonly(true)
                        .withInputSpan(3)
                        .withEmptyCellSeparation(false))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_END_TIME,
                        FORM_END_TIME_TEXT_KEY,
                        i18nSupport.formatDisplayDateWithTimeZone(exam.endTime))
                        .readonly(true)
                        .withInputSpan(3)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.text(
                        Domain.EXAM.ATTR_EXTERNAL_ID,
                        FORM_QUIZ_ID_TEXT_KEY,
                        exam.externalId)
                        .readonly(true)
                        .withEmptyCellSeparation(false))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_START_URL,
                        FORM_QUIZ_URL_TEXT_KEY,
                        exam.startURL)
                        .readonly(true)
                        .withInputSpan(7))

                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_DESCRIPTION,
                        FORM_DESCRIPTION_TEXT_KEY,
                        exam.description)
                        .asHTML()
                        .readonly(true)
                        .withInputSpan(6)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.text(
                        Domain.EXAM.ATTR_STATUS + "_display",
                        FORM_STATUS_TEXT_KEY,
                        i18nSupport.getText(new LocTextKey("sebserver.exam.status." + examStatus.name())))
                        .readonly(true)
                        .withLabelSpan(2)
                        .withInputSpan(4)
                        .withEmptyCellSpan(1))
                .addField(FormBuilder.singleSelection(
                        Domain.EXAM.ATTR_TYPE,
                        FORM_TYPE_TEXT_KEY,
                        (exam.type != null) ? String.valueOf(exam.type) : Exam.ExamType.UNDEFINED.name(),
                        this.resourceService::examTypeResources)
                        .withLabelSpan(2)
                        .withInputSpan(4)
                        .withEmptyCellSpan(2)
                        .mandatory(!readonly))

                .addField(FormBuilder.multiComboSelection(
                        Domain.EXAM.ATTR_SUPPORTER,
                        FORM_SUPPORTER_TEXT_KEY,
                        StringUtils.join(exam.supporter, Constants.LIST_SEPARATOR_CHAR),
                        this.resourceService::examSupporterResources)
                        .withLabelSpan(2)
                        .withInputSpan(4)
                        .withEmptyCellSpan(2)
                        .mandatory(!readonly))

                .buildFor(importFromQuizData
                        ? this.restService.getRestCall(ImportAsExam.class)
                        : this.restService.getRestCall(SaveExam.class));

        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(formContext
                .clearEntityKeys()
                .removeAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA));
        // propagate content actions to action-pane
        actionBuilder

                .newAction(ActionDefinition.EXAM_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && readonly && editable)

                .newAction(ActionDefinition.EXAM_SAVE)
                .withExec(action -> (importFromQuizData)
                        ? importExam(action, formHandle, sebRestrictionAvailable && exam.status == ExamStatus.RUNNING)
                        : formHandle.processFormSave(action))
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !readonly && modifyGrant)

                .newAction(ActionDefinition.EXAM_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA, String.valueOf(importFromQuizData))
                .withExec(this.cancelModifyFunction())
                .publishIf(() -> !readonly)

                .newAction(ActionDefinition.EXAM_MODIFY_SEB_RESTRICTION_DETAILS)
                .withEntityKey(entityKey)
                .withExec(ExamSEBRestrictionSettings.settingsFunction(this.pageService))
                .withAttribute(
                        ExamSEBRestrictionSettings.PAGE_CONTEXT_ATTR_LMS_TYPE,
                        this.restService.getBuilder(GetLmsSetup.class)
                                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(exam.lmsSetupId))
                                .call()
                                .getOrThrow().lmsType.name())
                .withAttribute(PageContext.AttributeKeys.FORCE_READ_ONLY, String.valueOf(!modifyGrant))
                .noEventPropagation()
                .publishIf(() -> sebRestrictionAvailable && readonly)

                .newAction(ActionDefinition.EXAM_ENABLE_SEB_RESTRICTION)
                .withEntityKey(entityKey)
                .withExec(action -> ExamSEBRestrictionSettings.setSEBRestriction(action, true, this.restService))
                .publishIf(() -> sebRestrictionAvailable && readonly && modifyGrant && !importFromQuizData
                        && BooleanUtils.isFalse(isRestricted))

                .newAction(ActionDefinition.EXAM_DISABLE_SEB_RESTRICTION)
                .withEntityKey(entityKey)
                .withExec(action -> ExamSEBRestrictionSettings.setSEBRestriction(action, false, this.restService))
                .publishIf(() -> sebRestrictionAvailable && readonly && modifyGrant && !importFromQuizData
                        && BooleanUtils.isTrue(isRestricted));

        // additional data in read-only view
        if (readonly && !importFromQuizData) {

            // List of SEB Configuration
            this.widgetFactory.addFormSubContextHeader(
                    content,
                    CONFIG_LIST_TITLE_KEY,
                    CONFIG_LIST_TITLE_TOOLTIP_KEY);

            final EntityTable<ExamConfigurationMap> configurationTable =
                    this.pageService.entityTableBuilder(this.restService.getRestCall(GetExamConfigMappingsPage.class))
                            .withRestCallAdapter(builder -> builder.withQueryParam(
                                    ExamConfigurationMap.FILTER_ATTR_EXAM_ID,
                                    entityKey.modelId))
                            .withEmptyMessage(CONFIG_EMPTY_LIST_MESSAGE)
                            .withPaging(1)
                            .hideNavigation()
                            .withColumn(new ColumnDefinition<>(
                                    Domain.CONFIGURATION_NODE.ATTR_NAME,
                                    CONFIG_NAME_COLUMN_KEY,
                                    ExamConfigurationMap::getConfigName)
                                            .widthProportion(2))
                            .withColumn(new ColumnDefinition<>(
                                    Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                                    CONFIG_DESCRIPTION_COLUMN_KEY,
                                    ExamConfigurationMap::getConfigDescription)
                                            .widthProportion(4))
                            .withColumn(new ColumnDefinition<ExamConfigurationMap>(
                                    Domain.CONFIGURATION_NODE.ATTR_STATUS,
                                    CONFIG_STATUS_COLUMN_KEY,
                                    this.resourceService::localizedExamConfigStatusName)
                                            .widthProportion(1))
                            .withDefaultActionIf(
                                    () -> modifyGrant,
                                    this::viewExamConfigPageAction)

                            .withSelectionListener(this.pageService.getSelectionPublisher(
                                    pageContext,
                                    ActionDefinition.EXAM_CONFIGURATION_EXAM_CONFIG_VIEW_PROP,
                                    ActionDefinition.EXAM_CONFIGURATION_DELETE_FROM_LIST,
                                    ActionDefinition.EXAM_CONFIGURATION_EXPORT,
                                    ActionDefinition.EXAM_CONFIGURATION_GET_CONFIG_KEY))

                            .compose(pageContext.copyOf(content));

            final EntityKey configMapKey = (configurationTable.hasAnyContent())
                    ? new EntityKey(
                            configurationTable.getFirstRowData().configurationNodeId,
                            EntityType.CONFIGURATION_NODE)
                    : null;

            actionBuilder

                    .newAction(ActionDefinition.EXAM_CONFIGURATION_NEW)
                    .withParentEntityKey(entityKey)
                    .withExec(ExamToConfigBindingForm.bindFunction(this.pageService))
                    .noEventPropagation()
                    .publishIf(() -> modifyGrant && editable && !configurationTable.hasAnyContent())

                    .newAction(ActionDefinition.EXAM_CONFIGURATION_EXAM_CONFIG_VIEW_PROP)
                    .withParentEntityKey(entityKey)
                    .withEntityKey(configMapKey)
                    .publishIf(() -> modifyGrant && configurationTable.hasAnyContent(), false)

                    .newAction(ActionDefinition.EXAM_CONFIGURATION_DELETE_FROM_LIST)
                    .withEntityKey(entityKey)
                    .withSelect(
                            getConfigMappingSelection(configurationTable),
                            this::deleteExamConfigMapping,
                            CONFIG_EMPTY_SELECTION_TEXT_KEY)
                    .withConfirm(() -> {
                        if (isExamRunning) {
                            return CONFIRM_MESSAGE_REMOVE_CONFIG;
                        }
                        return null;
                    })
                    .publishIf(() -> modifyGrant && configurationTable.hasAnyContent() && editable, false)

                    .newAction(ActionDefinition.EXAM_CONFIGURATION_EXPORT)
                    .withParentEntityKey(entityKey)
                    .withSelect(
                            getConfigSelection(configurationTable),
                            this::downloadExamConfigAction,
                            CONFIG_EMPTY_SELECTION_TEXT_KEY)
                    .noEventPropagation()
                    .publishIf(() -> userGrantCheck.r() && configurationTable.hasAnyContent(), false)

                    .newAction(ActionDefinition.EXAM_CONFIGURATION_GET_CONFIG_KEY)
                    .withSelect(
                            getConfigSelection(configurationTable),
                            this::getExamConfigKey,
                            CONFIG_EMPTY_SELECTION_TEXT_KEY)
                    .noEventPropagation()
                    .publishIf(() -> userGrantCheck.r() && configurationTable.hasAnyContent(), false);

            // List of Indicators
            this.widgetFactory.addFormSubContextHeader(
                    content,
                    INDICATOR_LIST_TITLE_KEY,
                    INDICATOR_LIST_TITLE_TOOLTIP_KEY);

            final EntityTable<Indicator> indicatorTable =
                    this.pageService.entityTableBuilder(this.restService.getRestCall(GetIndicatorPage.class))
                            .withRestCallAdapter(builder -> builder.withQueryParam(
                                    Indicator.FILTER_ATTR_EXAM_ID,
                                    entityKey.modelId))
                            .withEmptyMessage(INDICATOR_EMPTY_LIST_MESSAGE)
                            .withMarkup()
                            .withPaging(5)
                            .hideNavigation()
                            .withColumn(new ColumnDefinition<>(
                                    Domain.INDICATOR.ATTR_NAME,
                                    INDICATOR_NAME_COLUMN_KEY,
                                    Indicator::getName)
                                            .widthProportion(2))
                            .withColumn(new ColumnDefinition<>(
                                    Domain.INDICATOR.ATTR_TYPE,
                                    INDICATOR_TYPE_COLUMN_KEY,
                                    this::indicatorTypeName)
                                            .widthProportion(1))
                            .withColumn(new ColumnDefinition<>(
                                    Domain.THRESHOLD.REFERENCE_NAME,
                                    INDICATOR_THRESHOLD_COLUMN_KEY,
                                    ExamForm::thresholdsValue)
                                            .asMarkup()
                                            .widthProportion(4))
                            .withDefaultActionIf(
                                    () -> modifyGrant,
                                    () -> actionBuilder
                                            .newAction(ActionDefinition.EXAM_INDICATOR_MODIFY_FROM_LIST)
                                            .withParentEntityKey(entityKey)
                                            .create())

                            .withSelectionListener(this.pageService.getSelectionPublisher(
                                    pageContext,
                                    ActionDefinition.EXAM_INDICATOR_MODIFY_FROM_LIST,
                                    ActionDefinition.EXAM_INDICATOR_DELETE_FROM_LIST))

                            .compose(pageContext.copyOf(content));

            actionBuilder

                    .newAction(ActionDefinition.EXAM_INDICATOR_MODIFY_FROM_LIST)
                    .withParentEntityKey(entityKey)
                    .withSelect(
                            indicatorTable::getSelection,
                            PageAction::applySingleSelectionAsEntityKey,
                            INDICATOR_EMPTY_SELECTION_TEXT_KEY)
                    .publishIf(() -> modifyGrant && indicatorTable.hasAnyContent(), false)

                    .newAction(ActionDefinition.EXAM_INDICATOR_DELETE_FROM_LIST)
                    .withEntityKey(entityKey)
                    .withSelect(
                            indicatorTable::getSelection,
                            this::deleteSelectedIndicator,
                            INDICATOR_EMPTY_SELECTION_TEXT_KEY)
                    .publishIf(() -> modifyGrant && indicatorTable.hasAnyContent(), false)

                    .newAction(ActionDefinition.EXAM_INDICATOR_NEW)
                    .withParentEntityKey(entityKey)
                    .publishIf(() -> modifyGrant);
        }
    }

    private PageAction importExam(
            final PageAction action,
            final FormHandle<Exam> formHandle,
            final boolean applySEBRestriction) {

        // process normal save first
        final PageAction processFormSave = formHandle.processFormSave(action);

        // when okay and the exam sebRestriction is true
        if (applySEBRestriction) {
            ExamSEBRestrictionSettings.setSEBRestriction(
                    processFormSave,
                    true,
                    this.restService,
                    t -> log.error("Failed to initially restrict the course for SEB on LMS: {}", t.getMessage()));
        }

        return processFormSave;
    }

    private boolean testSEBRestrictionAPI(final Exam exam) {
        return this.restService.getBuilder(GetLmsSetup.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(exam.lmsSetupId))
                .call()
                .onError(t -> log.error("Failed to check SEB restriction API: ", t))
                .map(lmsSetup -> lmsSetup.lmsType.features.contains(Features.SEB_RESTRICTION))
                .getOr(false);
    }

    private void showConsistencyChecks(final Collection<APIMessage> result, final Composite parent) {
        if (result == null || result.isEmpty()) {
            return;
        }

        final Composite warningPanel = this.widgetFactory.createWarningPanel(parent);
        this.widgetFactory.labelLocalized(
                warningPanel,
                CustomVariant.TITLE_LABEL,
                CONSISTENCY_MESSAGE_TITLE);

        result
                .stream()
                .map(message -> this.consistencyMessageMapping.get(message.messageCode))
                .filter(Objects::nonNull)
                .forEach(message -> this.widgetFactory.labelLocalized(
                        warningPanel,
                        CustomVariant.MESSAGE,
                        message));
    }

    private PageAction viewExamConfigPageAction(final EntityTable<ExamConfigurationMap> table) {

        return this.pageService.pageActionBuilder(table.getPageContext()
                .clearEntityKeys()
                .removeAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA))
                .newAction(ActionDefinition.EXAM_CONFIGURATION_EXAM_CONFIG_VIEW_PROP)
                .withSelectionSupplier(() -> {
                    final ExamConfigurationMap selectedROWData = table.getSingleSelectedROWData();
                    final HashSet<EntityKey> result = new HashSet<>();
                    if (selectedROWData != null) {
                        result.add(new EntityKey(
                                selectedROWData.configurationNodeId,
                                EntityType.CONFIGURATION_NODE));
                    }
                    return result;
                })
                .create();
    }

    private PageAction downloadExamConfigAction(final PageAction action) {
        final UrlLauncher urlLauncher = RWT.getClient().getService(UrlLauncher.class);
        final EntityKey selection = action.getSingleSelection();
        if (selection != null) {
            final String downloadURL = this.downloadService.createDownloadURL(
                    selection.modelId,
                    action.pageContext().getParentEntityKey().modelId,
                    SEBExamConfigDownload.class,
                    this.downloadFileName);
            urlLauncher.openURL(downloadURL);
        }
        return action;
    }

    private Supplier<Set<EntityKey>> getConfigMappingSelection(
            final EntityTable<ExamConfigurationMap> configurationTable) {
        return () -> {
            final ExamConfigurationMap firstRowData = configurationTable.getFirstRowData();
            if (firstRowData == null) {
                return Collections.emptySet();
            } else {
                return new HashSet<>(Arrays.asList(firstRowData.getEntityKey()));
            }
        };
    }

    private Supplier<Set<EntityKey>> getConfigSelection(final EntityTable<ExamConfigurationMap> configurationTable) {
        return () -> {
            final ExamConfigurationMap firstRowData = configurationTable.getFirstRowData();
            if (firstRowData == null) {
                return Collections.emptySet();
            } else {
                return new HashSet<>(Arrays.asList(new EntityKey(
                        firstRowData.configurationNodeId,
                        EntityType.CONFIGURATION_NODE)));
            }
        };
    }

    private PageAction deleteSelectedIndicator(final PageAction action) {
        final EntityKey indicatorKey = action.getSingleSelection();
        this.resourceService.getRestService()
                .getBuilder(DeleteIndicator.class)
                .withURIVariable(API.PARAM_MODEL_ID, indicatorKey.modelId)
                .call();
        return action;
    }

    private PageAction getExamConfigKey(final PageAction action) {
        final EntityKey examConfigMappingKey = action.getSingleSelection();
        if (examConfigMappingKey != null) {
            action.withEntityKey(examConfigMappingKey);
            return SEBExamConfigForm
                    .getConfigKeyFunction(this.pageService)
                    .apply(action);
        }

        return action;
    }

    private PageAction deleteExamConfigMapping(final PageAction action) {
        final EntityKey examConfigMappingKey = action.getSingleSelection();
        this.resourceService.getRestService()
                .getBuilder(DeleteExamConfigMapping.class)
                .withURIVariable(API.PARAM_MODEL_ID, examConfigMappingKey.modelId)
                .call()
                .onError(error -> action.pageContext().notifyRemoveError(EntityType.EXAM_CONFIGURATION_MAP, error));
        return action;
    }

    private Result<Exam> getExistingExam(final PageContext pageContext) {
        final EntityKey entityKey = pageContext.getEntityKey();
        return this.restService.getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call();
    }

    private Result<Exam> createExamFromQuizData(final PageContext pageContext) {
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        return this.restService.getBuilder(GetQuizData.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .withQueryParam(QuizData.QUIZ_ATTR_LMS_SETUP_ID, parentEntityKey.modelId)
                .call()
                .map(Exam::new)
                .onError(error -> pageContext.notifyLoadError(EntityType.EXAM, error));
    }

    private String indicatorTypeName(final Indicator indicator) {
        if (indicator.type == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.resourceService.getI18nSupport()
                .getText(ResourceService.EXAM_INDICATOR_TYPE_PREFIX + indicator.type.name());
    }

    private static String thresholdsValue(final Indicator indicator) {
        if (indicator.thresholds.isEmpty()) {
            return Constants.EMPTY_NOTE;
        }

        final StringBuilder builder = indicator.thresholds
                .stream()
                .reduce(
                        new StringBuilder(),
                        (sb, threshold) -> sb
                                .append("<span style='padding: 2px 5px 2px 5px; background-color: #")
                                .append(threshold.color)
                                .append("; ")
                                .append((Utils.darkColor(Utils.parseRGB(threshold.color)))
                                        ? "color: #4a4a4a; "
                                        : "color: #FFFFFF;")
                                .append("'>")
                                .append(Indicator.getDisplayValue(indicator.type, threshold.value))
                                .append(" (")
                                .append(threshold.color)
                                .append(")")
                                .append("</span>")
                                .append(" | "),
                        StringBuilder::append);
        builder.delete(builder.length() - 3, builder.length() - 1);
        return builder.toString();
    }

    private Function<PageAction, PageAction> cancelModifyFunction() {
        final Function<PageAction, PageAction> backToCurrentFunction = this.pageService.backToCurrentFunction();
        return action -> {
            final boolean importFromQuizData = BooleanUtils.toBoolean(

                    action.pageContext().getAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA));
            if (importFromQuizData) {
                final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(action.pageContext());
                final PageAction activityHomeAction = actionBuilder
                        .newAction(ActionDefinition.QUIZ_DISCOVERY_VIEW_LIST)
                        .create();
                this.pageService.firePageEvent(new ActionEvent(activityHomeAction), action.pageContext());
                return activityHomeAction;
            }

            return backToCurrentFunction.apply(action);
        };
    }

}
