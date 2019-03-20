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

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.GetQuizzes;
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
public class QuizDiscoveryList implements TemplateComposer {

    private final WidgetFactory widgetFactory;
    private final ResourceService resourceService;
    private final int pageSize;

    private final static LocTextKey emptySelectionText =
            new LocTextKey("sebserver.quizdiscovery.info.pleaseSelect");
    private final static LocTextKey columnTitleLmsSetup =
            new LocTextKey("sebserver.quizdiscovery.list.column.lmssetup");
    private final static LocTextKey columnTitleName =
            new LocTextKey("sebserver.quizdiscovery.list.column.name");

    protected QuizDiscoveryList(
            final WidgetFactory widgetFactory,
            final ResourceService resourceService,
            @Value("${sebserver.gui.list.page.size}") final Integer pageSize) {

        this.widgetFactory = widgetFactory;
        this.resourceService = resourceService;
        this.pageSize = (pageSize != null) ? pageSize : 20;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final RestService restService = this.resourceService.getRestService();
        final I18nSupport i18nSupport = this.resourceService.getI18nSupport();

        // content page layout with title
        final Composite content = this.widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey("sebserver.quizdiscovery.list.title"));

        // table
        final EntityTable<QuizData> table =
                this.widgetFactory.entityTableBuilder(restService.getRestCall(GetQuizzes.class))
                        .withEmptyMessage(new LocTextKey("sebserver.quizdiscovery.list.empty"))
                        .withPaging(this.pageSize)
                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_LMS_SETUP_ID,
                                columnTitleLmsSetup,
                                quizDataLmsSetupNameFunction(this.resourceService),
                                new TableFilterAttribute(
                                        CriteriaType.SINGLE_SELECTION,
                                        LmsSetup.FILTER_ATTR_LMS_SETUP,
                                        this.resourceService::lmsSetupResource),
                                false))
                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_NAME,
                                columnTitleName,
                                quizData -> quizData.name,
                                new TableFilterAttribute(CriteriaType.TEXT, QuizData.FILTER_ATTR_NAME),
                                true))
                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_START_TIME,
                                new LocTextKey(
                                        "sebserver.quizdiscovery.list.column.starttime",
                                        i18nSupport.getUsersTimeZoneTitleSuffix()),
                                quizData -> quizData.startTime,
                                new TableFilterAttribute(CriteriaType.DATE, QuizData.FILTER_ATTR_START_TIME),
                                true))
                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_END_TIME,
                                new LocTextKey(
                                        "sebserver.quizdiscovery.list.column.endtime",
                                        i18nSupport.getUsersTimeZoneTitleSuffix()),
                                quizData -> quizData.endTime,
                                true))
                        .compose(content);

        // propagate content actions to action-pane
        final GrantCheck lmsSetupGrant = currentUser.grantCheck(EntityType.LMS_SETUP);
        final GrantCheck examGrant = currentUser.grantCheck(EntityType.EXAM);
        pageContext.clearEntityKeys()

                .createAction(ActionDefinition.LMS_SETUP_NEW)
                .publishIf(lmsSetupGrant::iw)

                .createAction(ActionDefinition.QUIZ_DISCOVERY_EXAM_IMPORT)
                .withSelect(
                        table::getSelection,
                        action -> this.importQuizData(action, table),
                        emptySelectionText)
                .publishIf(() -> examGrant.im() && table.hasAnyContent());
    }

    private static Function<QuizData, String> quizDataLmsSetupNameFunction(final ResourceService resourceService) {
        return quizzData -> resourceService.getLmsSetupNameFunction()
                .apply(String.valueOf(quizzData.lmsSetupId));
    }

    private Action importQuizData(final Action action, final EntityTable<QuizData> table) {
        final QuizData selectedROWData = table.getSelectedROWData();

        return action
                .withEntityKey(action.getSingleSelection())
                .withParentEntityKey(new EntityKey(selectedROWData.lmsSetupId, EntityType.LMS_SETUP))
                .withAttribute(AttributeKeys.IMPORT_FROM_QUIZZ_DATA, "true");
    }

}
