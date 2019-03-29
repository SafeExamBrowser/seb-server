/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.Function;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageAction;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExams;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ExamList implements TemplateComposer {

    private final WidgetFactory widgetFactory;
    private final ResourceService resourceService;
    private final int pageSize;

    private final static LocTextKey emptySelectionTextKey =
            new LocTextKey("sebserver.exam.info.pleaseSelect");
    private final static LocTextKey columnTitleLmsSetupKey =
            new LocTextKey("sebserver.exam.list.column.lmssetup");
    private final static LocTextKey columnTitleNameKey =
            new LocTextKey("sebserver.exam.list.column.name");
    private final static LocTextKey columnTitleTypeKey =
            new LocTextKey("sebserver.exam.list.column.type");

    private final TableFilterAttribute institutionFilter;
    private final TableFilterAttribute lmsFilter;
    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, QuizData.FILTER_ATTR_NAME);
    private final TableFilterAttribute startTimeFilter =
            new TableFilterAttribute(CriteriaType.DATE, QuizData.FILTER_ATTR_START_TIME);

    protected ExamList(
            final WidgetFactory widgetFactory,
            final ResourceService resourceService,
            @Value("${sebserver.gui.list.page.size}") final Integer pageSize) {

        this.widgetFactory = widgetFactory;
        this.resourceService = resourceService;
        this.pageSize = (pageSize != null) ? pageSize : 20;

        this.institutionFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Entity.FILTER_ATTR_INSTITUTION,
                this.resourceService::institutionResource);

        this.lmsFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                LmsSetup.FILTER_ATTR_LMS_SETUP,
                this.resourceService::lmsSetupResource);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final RestService restService = this.resourceService.getRestService();
        final I18nSupport i18nSupport = this.resourceService.getI18nSupport();

        // content page layout with title
        final Composite content = this.widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey("sebserver.exam.list.title"));

        final boolean isSEBAdmin = currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);

        // table
        final EntityTable<Exam> table =
                this.widgetFactory.entityTableBuilder(restService.getRestCall(GetExams.class))
                        .withEmptyMessage(new LocTextKey("sebserver.exam.list.empty"))
                        .withPaging(this.pageSize)
                        .withColumnIf(() -> isSEBAdmin,
                                new ColumnDefinition<>(
                                        Domain.EXAM.ATTR_INSTITUTION_ID,
                                        new LocTextKey("sebserver.exam.list.column.institution"),
                                        examInstitutionNameFunction(this.resourceService),
                                        this.institutionFilter,
                                        false))
                        .withColumn(new ColumnDefinition<>(
                                Domain.EXAM.ATTR_LMS_SETUP_ID,
                                columnTitleLmsSetupKey,
                                examLmsSetupNameFunction(this.resourceService),
                                this.lmsFilter,
                                false))
                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_NAME,
                                columnTitleNameKey,
                                Exam::getName,
                                this.nameFilter,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_START_TIME,
                                new LocTextKey(
                                        "sebserver.exam.list.column.starttime",
                                        i18nSupport.getUsersTimeZoneTitleSuffix()),
                                Exam::getStartTime,
                                this.startTimeFilter,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.EXAM.ATTR_TYPE,
                                columnTitleTypeKey,
                                this::examTypeName,
                                true))
                        .withDefaultAction(pageContext
                                .clearEntityKeys()
                                .createAction(ActionDefinition.EXAM_VIEW_FROM_LIST))
                        .compose(content);

        // propagate content actions to action-pane
        final GrantCheck userGrant = currentUser.grantCheck(EntityType.EXAM);
        pageContext.clearEntityKeys()

                .createAction(ActionDefinition.EXAM_IMPORT)
                .publishIf(userGrant::im)

                .createAction(ActionDefinition.EXAM_VIEW_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, emptySelectionTextKey)
                .publishIf(table::hasAnyContent)

                .createAction(ActionDefinition.EXAM_MODIFY_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, emptySelectionTextKey)
                .publishIf(() -> userGrant.im() && table.hasAnyContent());

    }

    private static Function<Exam, String> examInstitutionNameFunction(final ResourceService resourceService) {
        return exam -> resourceService.getInstitutionNameFunction()
                .apply(String.valueOf(exam.institutionId));
    }

    private static Function<Exam, String> examLmsSetupNameFunction(final ResourceService resourceService) {
        return exam -> resourceService.getLmsSetupNameFunction()
                .apply(String.valueOf(exam.lmsSetupId));
    }

    private String examTypeName(final Exam exam) {
        if (exam.type == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.resourceService.getI18nSupport()
                .getText("sebserver.exam.type." + exam.type.name());
    }

}
