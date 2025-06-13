/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import static ch.ethz.seb.sebserver.gbl.model.user.UserFeatures.Feature.EXAM_NO_LMS;
import static ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys.NEW_EXAM_NO_LMS;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.model.user.UserFeatures;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.CheckExamConsistency;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class ExamList implements TemplateComposer {

    public static final String EXAM_LIST_COLUMN_START_TIME =
            "sebserver.exam.list.column.starttime";
    public static final LocTextKey PAGE_TITLE_KEY =
            new LocTextKey("sebserver.exam.list.title");
    public static final LocTextKey NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION =
            new LocTextKey("sebserver.exam.list.action.no.modify.privilege");
    public final static LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.info.pleaseSelect");
    public final static LocTextKey COLUMN_TITLE_INSTITUTION_KEY =
            new LocTextKey("sebserver.exam.list.column.institution");
    public final static LocTextKey COLUMN_TITLE_LMS_KEY =
            new LocTextKey("sebserver.exam.list.column.lmssetup");
    public final static LocTextKey COLUMN_TITLE_NAME_KEY =
            new LocTextKey("sebserver.exam.list.column.name");
    public final static LocTextKey COLUMN_TITLE_STATE_KEY =
            new LocTextKey("sebserver.exam.list.column.state");
    public final static LocTextKey COLUMN_TITLE_TYPE_KEY =
            new LocTextKey("sebserver.exam.list.column.type");
    public final static LocTextKey NO_MODIFY_OF_OUT_DATED_EXAMS =
            new LocTextKey("sebserver.exam.list.modify.out.dated");
    public final static LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.exam.list.empty");

    private final TableFilterAttribute institutionFilter;
    private final TableFilterAttribute lmsFilter;
    private final TableFilterAttribute nameFilter = new TableFilterAttribute(
            CriteriaType.TEXT,
            Domain.EXAM.ATTR_QUIZ_NAME,
            Utils.createFilterTooltipKey(COLUMN_TITLE_NAME_KEY));
    private final TableFilterAttribute stateFilter;
    private final TableFilterAttribute typeFilter;

    private final PageService pageService;
    private final ResourceService resourceService;
    private final int pageSize;

    private final ExamBatchArchivePopup examBatchArchivePopup;
    private final ExamBatchDeletePopup examBatchDeletePopup;

    protected ExamList(
            final PageService pageService,
            final ExamBatchArchivePopup examBatchArchivePopup,
            final ExamBatchDeletePopup examBatchDeletePopup,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.examBatchArchivePopup = examBatchArchivePopup;
        this.examBatchDeletePopup = examBatchDeletePopup;
        this.pageSize = pageSize;

        this.institutionFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Entity.FILTER_ATTR_INSTITUTION,
                this.resourceService::institutionResource,
                Utils.createFilterTooltipKey(COLUMN_TITLE_INSTITUTION_KEY));

        this.lmsFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                LmsSetup.FILTER_ATTR_LMS_SETUP,
                this.resourceService::lmsSetupResource,
                Utils.createFilterTooltipKey(COLUMN_TITLE_LMS_KEY));

        this.stateFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Exam.FILTER_ATTR_STATUS,
                this.resourceService::localizedExamStatusSelection,
                Utils.createFilterTooltipKey(COLUMN_TITLE_STATE_KEY));

        this.typeFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Exam.FILTER_ATTR_TYPE,
                this.resourceService::examTypeResources,
                Utils.createFilterTooltipKey(COLUMN_TITLE_TYPE_KEY));
    }

    @Override
    public void compose(final PageContext pageContext) {
    
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final RestService restService = this.resourceService.getRestService();
        final I18nSupport i18nSupport = this.resourceService.getI18nSupport();
        final boolean teacherOnly = currentUser.isOnlyTeacher();

        // content page layout with title
        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                PAGE_TITLE_KEY);

        final PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(pageContext.clearEntityKeys());

        final BooleanSupplier isSEBAdmin =
                () -> currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);
        
        // table
        final EntityTable<Exam> table =
                this.pageService.entityTableBuilder(restService.getRestCall(GetExamPage.class))
                        .withMultiSelection()
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withRowDecorator(decorateOnExamConsistency(this.pageService))
                        .withStaticFilter(Exam.FILTER_ATTR_ACTIVE, Constants.TRUE_STRING)
                        .withDefaultSort(Domain.EXAM.ATTR_QUIZ_NAME)

                        .withColumnIf(
                                () -> isSEBAdmin.getAsBoolean()
                                        && currentUser.isFeatureEnabled(UserFeatures.Feature.ADMIN_INSTITUTION)
                                        && !pageService.isLightSetup(),
                                () -> new ColumnDefinition<Exam>(
                                        Domain.EXAM.ATTR_INSTITUTION_ID,
                                        COLUMN_TITLE_INSTITUTION_KEY,
                                        exam -> this.resourceService.getInstitutionNameFunction()
                                                .apply(String.valueOf(exam.getInstitutionId())))
                                                        .withFilter(this.institutionFilter)
                                                        .sortable())

                        .withColumnIf(() -> !teacherOnly,
                                () -> new ColumnDefinition<>(
                                Domain.EXAM.ATTR_LMS_SETUP_ID,
                                COLUMN_TITLE_LMS_KEY,
                                examLmsSetupNameFunction(this.resourceService))
                                        .withFilter(this.lmsFilter)
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                Domain.EXAM.ATTR_QUIZ_NAME,
                                COLUMN_TITLE_NAME_KEY,
                                Exam::getName)
                                        .withFilter(this.nameFilter)
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                Domain.EXAM.ATTR_QUIZ_START_TIME,
                                new LocTextKey(
                                        EXAM_LIST_COLUMN_START_TIME,
                                        i18nSupport.getUsersTimeZoneTitleSuffix()),
                                Exam::getStartTime)
                                        .withFilter(new TableFilterAttribute(
                                                CriteriaType.DATE,
                                                Domain.EXAM.ATTR_QUIZ_START_TIME,
                                                Utils.toDateTimeUTC(Utils.getMillisecondsNow())
                                                        .minusYears(1)
                                                        .toString(),
                                                new LocTextKey("sebserver.exam.list.column.starttime.filter.tooltip")))
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                Domain.EXAM.ATTR_STATUS,
                                COLUMN_TITLE_STATE_KEY,
                                this.resourceService::localizedExamStatusName)
                                        .withFilter(this.stateFilter)
                                        .sortable())

                        .withColumn(new ColumnDefinition<Exam>(
                                Domain.EXAM.ATTR_TYPE,
                                COLUMN_TITLE_TYPE_KEY,
                                this.resourceService::localizedExamTypeName)
                                        .withFilter(this.typeFilter)
                                        .sortable())

                        .withDefaultAction(actionBuilder
                                .newAction(ActionDefinition.EXAM_VIEW_FROM_LIST)
                                .create())

                        .withSelectionListener(this.pageService.getSelectionPublisher(
                                pageContext,
                                ActionDefinition.EXAM_VIEW_FROM_LIST,
                                ActionDefinition.EXAM_MODIFY_FROM_LIST,
                                ActionDefinition.EXAM_LIST_BULK_ARCHIVE,
                                ActionDefinition.EXAM_LIST_BULK_DELETE))

                        .compose(pageContext.copyOf(content));

        // propagate content actions to action-pane
        final GrantCheck userGrant = currentUser.grantCheck(EntityType.EXAM);
        actionBuilder
                .newAction(ActionDefinition.EXAM_VIEW_FROM_LIST)
                .withSelect(table::getMultiSelection, PageAction::applySingleSelectionAsEntityKey,
                        EMPTY_SELECTION_TEXT_KEY)
                .publish(false)

                .newAction(ActionDefinition.EXAM_MODIFY_FROM_LIST)
                .withSelect(
                        table.getGrantedSelection(currentUser, NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION),
                        action -> modifyExam(action, table),
                        EMPTY_SELECTION_TEXT_KEY)
                .publishIf(userGrant::im, false)

                .newAction(ActionDefinition.EXAM_LIST_BULK_ARCHIVE)
                .withSelect(
                        table::getMultiSelection,
                        this.examBatchArchivePopup.popupCreationFunction(pageContext),
                        EMPTY_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(userGrant::im, false)

                .newAction(ActionDefinition.EXAM_LIST_BULK_DELETE)
                .withSelect(
                        table::getMultiSelection,
                        this.examBatchDeletePopup.popupCreationFunction(pageContext),
                        EMPTY_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(userGrant::iw, false)

                .newAction(ActionDefinition.EXAM_LIST_HIDE_MISSING)
                .withExec(action -> hideMissingExams(action, table))
                .noEventPropagation()
                .withSwitchAction(
                        actionBuilder.newAction(ActionDefinition.EXAM_LIST_SHOW_MISSING)
                                .withExec(action -> showMissingExams(action, table))
                                .noEventPropagation()
                                .create())
                .publish()

                .newAction(ActionDefinition.EXAM_NEW)
                .withAttribute(NEW_EXAM_NO_LMS, Constants.TRUE_STRING)
                .publishIf(() -> userGrant.iw() && currentUser.isFeatureEnabled(EXAM_NO_LMS))
        ;
    }

    private PageAction showMissingExams(final PageAction action, final EntityTable<Exam> table) {
        table.setStaticFilter(Exam.FILTER_ATTR_HIDE_MISSING, Constants.FALSE_STRING);
        table.applyFilter();
        return action;
    }

    private PageAction hideMissingExams(final PageAction action, final EntityTable<Exam> table) {
        table.setStaticFilter(Exam.FILTER_ATTR_HIDE_MISSING, Constants.TRUE_STRING);
        table.applyFilter();
        return action;
    }

    static PageAction modifyExam(final PageAction action, final EntityTable<Exam> table) {
        final Exam exam = table.getSingleSelectedROWData();

        if (exam == null) {
            throw new PageMessageException(EMPTY_SELECTION_TEXT_KEY);
        }

        if (exam.endTime != null) {
            final DateTime now = DateTime.now(DateTimeZone.UTC);
            if (exam.endTime.isBefore(now)) {
                throw new PageMessageException(NO_MODIFY_OF_OUT_DATED_EXAMS);
            }
        }

        return action.withEntityKey(action.getSingleSelection());
    }

    public static BiConsumer<TableItem, ExamConfigurationMap> decorateOnExamMapConsistency(
            final PageService pageService) {

        return (item, examMap) -> pageService.getRestService().getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(examMap.examId))
                .call()
                .ifPresent(exam -> decorateOnExamConsistency(item, exam, pageService));
    }

    public static BiConsumer<TableItem, Exam> decorateOnExamConsistency(final PageService pageService) {
        return (item, exam) -> decorateOnExamConsistency(item, exam, pageService);
    }

    static void decorateOnExamConsistency(
            final TableItem item,
            final Exam exam,
            final PageService pageService) {

        if (exam.lmsSetupId != null && BooleanUtils.isFalse(exam.isLmsAvailable())) {
            item.setData(RWT.CUSTOM_VARIANT, CustomVariant.DISABLED.key);
            return;
        }

        if (exam.getStatus() != ExamStatus.RUNNING) {
            return;
        }

        pageService.getRestService().getBuilder(CheckExamConsistency.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call()
                .ifPresent(warnings -> {
                    if (warnings != null && !warnings.isEmpty()) {
                        item.setData(RWT.CUSTOM_VARIANT, CustomVariant.WARNING.key);
                    }
                });
    }

    public static Function<Exam, String> examLmsSetupNameFunction(final ResourceService resourceService) {
        return exam -> resourceService.getLmsSetupNameFunction()
                .apply(String.valueOf(exam.lmsSetupId));
    }

}
