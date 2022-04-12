/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.content.exam.ExamList;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetFinishedExamPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class FinishedExamList implements TemplateComposer {

    private static final LocTextKey PAGE_TITLE_KEY =
            new LocTextKey("sebserver.finished.exam.list.title");
    private final static LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.finished.exam.info.pleaseSelect");
    private final static LocTextKey COLUMN_TITLE_NAME_KEY =
            new LocTextKey("sebserver.finished.exam.list.column.name");
    private final static LocTextKey COLUMN_TITLE_TYPE_KEY =
            new LocTextKey("sebserver.finished.exam.list.column.type");
    private final static LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.finished.exam.list.empty");

    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, QuizData.FILTER_ATTR_NAME);
    private final TableFilterAttribute typeFilter;

    private final PageService pageService;
    private final ResourceService resourceService;
    private final int pageSize;

    protected FinishedExamList(
            final PageService pageService,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.pageSize = pageSize;

        this.typeFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Exam.FILTER_ATTR_TYPE,
                this.resourceService::examTypeResources);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final RestService restService = this.resourceService.getRestService();
        final I18nSupport i18nSupport = this.resourceService.getI18nSupport();

        // content page layout with title
        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                PAGE_TITLE_KEY);

        final PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(pageContext.clearEntityKeys());

        // table
        final EntityTable<Exam> table =
                this.pageService.entityTableBuilder(restService.getRestCall(GetFinishedExamPage.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withRowDecorator(ExamList.decorateOnExamConsistency(this.pageService))
                        .withDefaultSort(QuizData.QUIZ_ATTR_NAME)

                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_NAME,
                                COLUMN_TITLE_NAME_KEY,
                                Exam::getName)
                                        .withFilter(this.nameFilter)
                                        .sortable())

                        .withColumn(new ColumnDefinition<Exam>(
                                Domain.EXAM.ATTR_TYPE,
                                COLUMN_TITLE_TYPE_KEY,
                                this.resourceService::localizedExamTypeName)
                                        .withFilter(this.typeFilter)
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_START_TIME,
                                new LocTextKey(
                                        "sebserver.finished.exam.list.column.startTime",
                                        i18nSupport.getUsersTimeZoneTitleSuffix()),
                                Exam::getStartTime)
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_END_TIME,
                                new LocTextKey(
                                        "sebserver.finished.exam.list.column.endTime",
                                        i18nSupport.getUsersTimeZoneTitleSuffix()),
                                Exam::getEndTime)
                                        .sortable())

                        .withDefaultAction(actionBuilder
                                .newAction(ActionDefinition.VIEW_FINISHED_EXAM_FROM_LIST)
                                .create())

                        .withSelectionListener(this.pageService.getSelectionPublisher(
                                pageContext,
                                ActionDefinition.VIEW_FINISHED_EXAM_FROM_LIST))

                        .compose(pageContext.copyOf(content));

        actionBuilder

                .newAction(ActionDefinition.VIEW_FINISHED_EXAM_FROM_LIST)
                .withSelect(
                        table::getMultiSelection,
                        PageAction::applySingleSelectionAsEntityKey,
                        EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> currentUser.get().hasRole(UserRole.EXAM_SUPPORTER), false);

    }

}
