/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.tomcat.util.buf.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeleteIndicator;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicators;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveExam;
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

    private final PageService pageService;
    private final ResourceService resourceService;

    private final static LocTextKey listTitleKey =
            new LocTextKey("sebserver.exam.indicator.list.title");
    private final static LocTextKey typeColumnKey =
            new LocTextKey("sebserver.exam.indicator.list.column.type");
    private final static LocTextKey nameColumnKey =
            new LocTextKey("sebserver.exam.indicator.list.column.name");
    private final static LocTextKey thresholdColumnKey =
            new LocTextKey("sebserver.exam.indicator.list.column.thresholds");
    private final static LocTextKey emptySelectionTextKey =
            new LocTextKey("sebserver.exam.indicator.list.pleaseSelect");

    protected ExamForm(
            final PageService pageService,
            final ResourceService resourceService) {

        this.pageService = pageService;
        this.resourceService = resourceService;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final RestService restService = this.resourceService.getRestService();
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final I18nSupport i18nSupport = this.resourceService.getI18nSupport();

        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        final boolean readonly = pageContext.isReadonly();
        final boolean importFromQuizData = BooleanUtils.toBoolean(
                pageContext.getAttribute(AttributeKeys.IMPORT_FROM_QUIZZ_DATA));

        // get or create model data
        final Exam exam = (importFromQuizData
                ? createExamFromQuizData(entityKey, parentEntityKey, restService)
                : getExistingExam(entityKey, restService))
                        .get(pageContext::notifyError);

        if (exam == null) {
            log.error(
                    "Failed to get Exam. "
                            + "Error was notified to the User. "
                            + "See previous logs for more infomation");
            return;
        }

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(exam.getEntityKey());

        // the default page layout with title
        final LocTextKey titleKey = new LocTextKey(
                importFromQuizData
                        ? "sebserver.exam.form.title.import"
                        : "sebserver.exam.form.title");
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        final BooleanSupplier isNew = () -> importFromQuizData;
        final BooleanSupplier isNotNew = () -> !isNew.getAsBoolean();
        final EntityGrantCheck userGrantCheck = currentUser.entityGrantCheck(exam);
        final boolean modifyGrant = userGrantCheck.m();
        final ExamStatus examStatus = exam.getStatus();
        final boolean editable = examStatus == ExamStatus.UP_COMING;

        // The Exam form
        final FormHandle<Exam> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content), 4)
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

                .addField(FormBuilder.singleSelection(
                        Domain.EXAM.ATTR_LMS_SETUP_ID,
                        "sebserver.exam.form.lmssetup",
                        String.valueOf(exam.lmsSetupId),
                        this.resourceService::lmsSetupResource)
                        .readonly(true))
                .addField(FormBuilder.text(
                        Domain.EXAM.ATTR_EXTERNAL_ID,
                        "sebserver.exam.form.quizid",
                        exam.externalId)
                        .readonly(true))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_NAME,
                        "sebserver.exam.form.name",
                        exam.name)
                        .readonly(true))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_DESCRIPTION,
                        "sebserver.exam.form.description",
                        exam.description)
                        .asArea()
                        .readonly(true))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_START_TIME,
                        "sebserver.exam.form.starttime",
                        i18nSupport.formatDisplayDate(exam.startTime))
                        .readonly(true))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_END_TIME,
                        "sebserver.exam.form.endtime",
                        i18nSupport.formatDisplayDate(exam.endTime))
                        .readonly(true))
                .addField(FormBuilder.singleSelection(
                        Domain.EXAM.ATTR_TYPE,
                        "sebserver.exam.form.type",
                        String.valueOf(exam.type),
                        this.resourceService::examTypeResources))
                .addField(FormBuilder.text(
                        Exam.ATTR_STATUS,
                        "sebserver.exam.form.status",
                        i18nSupport.getText(new LocTextKey("sebserver.exam.status." + examStatus.name())))
                        .readonly(true))
                .addFieldIf(
                        isNotNew,
                        () -> FormBuilder.multiComboSelection(
                                Domain.EXAM.ATTR_SUPPORTER,
                                "sebserver.exam.form.supporter",
                                StringUtils.join(exam.supporter, Constants.LIST_SEPARATOR_CHAR),
                                this.resourceService::examSupporterResources))

                .buildFor(importFromQuizData
                        ? restService.getRestCall(ImportAsExam.class)
                        : restService.getRestCall(SaveExam.class));

        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(formContext
                .clearEntityKeys()
                .removeAttribute(AttributeKeys.IMPORT_FROM_QUIZZ_DATA));
        // propagate content actions to action-pane
        actionBuilder

                .newAction(ActionDefinition.EXAM_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && readonly && editable)

                .newAction(ActionDefinition.EXAM_SAVE)
                .withExec(formHandle::processFormSave)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !readonly && modifyGrant)

                .newAction(ActionDefinition.EXAM_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withAttribute(AttributeKeys.IMPORT_FROM_QUIZZ_DATA, String.valueOf(importFromQuizData))
                .withExec(this::cancelModify)
                .publishIf(() -> !readonly);

//                .newAction(ActionDefinition.EXAM_DEACTIVATE)
//                .withEntityKey(entityKey)
//                .withSimpleRestCall(restService, DeactivateExam.class)
//                .withConfirm(PageUtils.confirmDeactivation(exam, restService))
//                .publishIf(() -> writeGrant && readonly && exam.isActive())
//
//                .newAction(ActionDefinition.EXAM_ACTIVATE)
//                .withEntityKey(entityKey)
//                .withSimpleRestCall(restService, ActivateExam.class)
//                .publishIf(() -> writeGrant && readonly && !exam.isActive());

        // additional data in read-only view
        if (readonly) {

            // List of Indicators
            widgetFactory.labelLocalized(
                    content,
                    CustomVariant.TEXT_H3,
                    listTitleKey);

            final EntityTable<Indicator> indicatorTable =
                    this.pageService.entityTableBuilder(restService.getRestCall(GetIndicators.class))
                            .withEmptyMessage(new LocTextKey("sebserver.exam.indicator.list.empty"))
                            .withPaging(5)
                            .hideNavigation()
                            .withColumn(new ColumnDefinition<>(
                                    Domain.INDICATOR.ATTR_NAME,
                                    nameColumnKey,
                                    Indicator::getName,
                                    false))
                            .withColumn(new ColumnDefinition<>(
                                    Domain.INDICATOR.ATTR_TYPE,
                                    typeColumnKey,
                                    this::indicatorTypeName,
                                    false))
                            .withColumn(new ColumnDefinition<>(
                                    Domain.THRESHOLD.REFERENCE_NAME,
                                    thresholdColumnKey,
                                    ExamForm::thresholdsValue,
                                    false))
                            .withDefaultActionIf(
                                    () -> editable,
                                    () -> actionBuilder
                                            .newAction(ActionDefinition.EXAM_INDICATOR_MODIFY_FROM_LIST)
                                            .withParentEntityKey(entityKey)
                                            .create())

                            .compose(content);

            actionBuilder

                    .newAction(ActionDefinition.EXAM_INDICATOR_NEW)
                    .withParentEntityKey(entityKey)
                    .publishIf(() -> modifyGrant && editable)

                    .newAction(ActionDefinition.EXAM_INDICATOR_MODIFY_FROM_LIST)
                    .withParentEntityKey(entityKey)
                    .withSelect(indicatorTable::getSelection, PageAction::applySingleSelection, emptySelectionTextKey)
                    .publishIf(() -> modifyGrant && indicatorTable.hasAnyContent() && editable)

                    .newAction(ActionDefinition.EXAM_INDICATOR_DELETE_FROM_LIST)
                    .withEntityKey(entityKey)
                    .withSelect(indicatorTable::getSelection, this::deleteSelectedIndicator, emptySelectionTextKey)
                    .publishIf(() -> modifyGrant && indicatorTable.hasAnyContent() && editable);

            // TODO List of attached SEB Configurations

        }

    }

    private PageAction deleteSelectedIndicator(final PageAction action) {
        final EntityKey indicatorKey = action.getSingleSelection();
        this.resourceService.getRestService()
                .getBuilder(DeleteIndicator.class)
                .withURIVariable(API.PARAM_MODEL_ID, indicatorKey.modelId)
                .call();
        return action;
    }

    private Result<Exam> getExistingExam(final EntityKey entityKey, final RestService restService) {
        return restService.getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call();
    }

    private Result<Exam> createExamFromQuizData(
            final EntityKey entityKey,
            final EntityKey parentEntityKey,
            final RestService restService) {

        return restService.getBuilder(GetQuizData.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .withQueryParam(QuizData.QUIZ_ATTR_LMS_SETUP_ID, parentEntityKey.modelId)
                .call()
                .map(quizzData -> new Exam(quizzData));
    }

    private String indicatorTypeName(final Indicator indicator) {
        if (indicator.type == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.resourceService.getI18nSupport()
                .getText("sebserver.exam.indicator.type." + indicator.type.name());
    }

    private static String thresholdsValue(final Indicator indicator) {
        if (indicator.thresholds.isEmpty()) {
            return Constants.EMPTY_NOTE;
        }

        return indicator.thresholds
                .stream()
                .reduce(
                        new StringBuilder(),
                        (sb, threshold) -> sb.append(threshold.value).append(":").append(threshold.color).append("|"),
                        (sb1, sb2) -> sb1.append(sb2))
                .toString();
    }

    private PageAction cancelModify(final PageAction action) {
        final boolean importFromQuizData = BooleanUtils.toBoolean(
                action.pageContext().getAttribute(AttributeKeys.IMPORT_FROM_QUIZZ_DATA));
        if (importFromQuizData) {
            final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(action.pageContext());
            final PageAction activityHomeAction = actionBuilder
                    .newAction(ActionDefinition.QUIZ_DISCOVERY_VIEW_LIST)
                    .create();
            this.pageService.firePageEvent(new ActionEvent(activityHomeAction), action.pageContext());
            return activityHomeAction;
        }

        this.pageService.onEmptyEntityKeyGoTo(action, ActionDefinition.EXAM_VIEW_LIST);
        return action;
    }

}
