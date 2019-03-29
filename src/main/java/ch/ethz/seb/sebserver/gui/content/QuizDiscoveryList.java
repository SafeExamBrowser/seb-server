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
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.PageFormService;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.PageAction;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
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

    // localized text keys
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.list.title");
    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.list.empty");
    private final static LocTextKey EMPTY_SELECTION_TEXT =
            new LocTextKey("sebserver.quizdiscovery.info.pleaseSelect");
    private final static LocTextKey LMS_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.list.column.lmssetup");
    private final static LocTextKey NAME_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.list.column.name");
    private final static LocTextKey DETAILS_TITLE_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.quiz.details.title");

    // filter attribute models
    private final TableFilterAttribute lmsFilter;
    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, QuizData.FILTER_ATTR_NAME);
    private final TableFilterAttribute startTimeFilter =
            new TableFilterAttribute(CriteriaType.DATE, QuizData.FILTER_ATTR_START_TIME);

    // dependencies
    private final WidgetFactory widgetFactory;
    private final ResourceService resourceService;
    private final PageFormService pageFormService;
    private final int pageSize;

    protected QuizDiscoveryList(
            final PageFormService pageFormService,
            final WidgetFactory widgetFactory,
            final ResourceService resourceService,
            @Value("${sebserver.gui.list.page.size}") final Integer pageSize) {

        this.pageFormService = pageFormService;
        this.widgetFactory = widgetFactory;
        this.resourceService = resourceService;
        this.pageSize = (pageSize != null) ? pageSize : 20;

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
                TITLE_TEXT_KEY);

        // table
        final EntityTable<QuizData> table =
                this.widgetFactory.entityTableBuilder(restService.getRestCall(GetQuizzes.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_LMS_SETUP_ID,
                                LMS_TEXT_KEY,
                                quizDataLmsSetupNameFunction(this.resourceService),
                                this.lmsFilter,
                                false))
                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_NAME,
                                NAME_TEXT_KEY,
                                quizData -> quizData.name,
                                this.nameFilter,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_START_TIME,
                                new LocTextKey(
                                        "sebserver.quizdiscovery.list.column.starttime",
                                        i18nSupport.getUsersTimeZoneTitleSuffix()),
                                quizData -> quizData.startTime,
                                this.startTimeFilter,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_END_TIME,
                                new LocTextKey(
                                        "sebserver.quizdiscovery.list.column.endtime",
                                        i18nSupport.getUsersTimeZoneTitleSuffix()),
                                quizData -> quizData.endTime,
                                true))
                        .withDefaultAction(t -> pageContext
                                .clearEntityKeys()
                                .createAction(ActionDefinition.QUIZ_DISCOVERY_SHOW_DETAILS)
                                .withExec(action -> this.showDetails(action, t.getSelectedROWData()))
                                .noEventPropagation())
                        .compose(content);

        // propagate content actions to action-pane
        final GrantCheck lmsSetupGrant = currentUser.grantCheck(EntityType.LMS_SETUP);
        final GrantCheck examGrant = currentUser.grantCheck(EntityType.EXAM);
        pageContext.clearEntityKeys()

                .createAction(ActionDefinition.LMS_SETUP_NEW)
                .publishIf(lmsSetupGrant::iw)

                .createAction(ActionDefinition.QUIZ_DISCOVERY_SHOW_DETAILS)
                .withSelect(
                        table::getSelection,
                        action -> this.showDetails(action, table.getSelectedROWData()),
                        EMPTY_SELECTION_TEXT)
                .noEventPropagation()
                .publishIf(table::hasAnyContent)

                .createAction(ActionDefinition.QUIZ_DISCOVERY_EXAM_IMPORT)
                .withSelect(
                        table::getSelection,
                        action -> this.importQuizData(action, table),
                        EMPTY_SELECTION_TEXT)
                .publishIf(() -> examGrant.im() && table.hasAnyContent());
    }

    private static Function<QuizData, String> quizDataLmsSetupNameFunction(final ResourceService resourceService) {
        return quizzData -> resourceService.getLmsSetupNameFunction()
                .apply(String.valueOf(quizzData.lmsSetupId));
    }

    private PageAction importQuizData(final PageAction action, final EntityTable<QuizData> table) {
        final QuizData selectedROWData = table.getSelectedROWData();

        return action
                .withEntityKey(action.getSingleSelection())
                .withParentEntityKey(new EntityKey(selectedROWData.lmsSetupId, EntityType.LMS_SETUP))
                .withAttribute(AttributeKeys.IMPORT_FROM_QUIZZ_DATA, "true");
    }

    private PageAction showDetails(final PageAction action, final QuizData quizData) {
        action.getSingleSelection();

        final ModalInputDialog<Void> dialog = new ModalInputDialog<>(
                action.pageContext().getParent().getShell(),
                this.widgetFactory);

        dialog.open(
                DETAILS_TITLE_TEXT_KEY,
                action.pageContext(),
                pc -> createDetailsForm(quizData, pc));

        return action;
    }

    private void createDetailsForm(final QuizData quizData, final PageContext pc) {
        this.widgetFactory.labelSeparator(pc.getParent());
        this.pageFormService.getBuilder(pc, 4)
                .readonly(true)
                .addField(FormBuilder.singleSelection(
                        QuizData.QUIZ_ATTR_LMS_SETUP_ID,
                        "sebserver.quizdiscovery.quiz.details.lms",
                        String.valueOf(quizData.lmsSetupId),
                        () -> this.resourceService.lmsSetupResource()))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_NAME,
                        "sebserver.quizdiscovery.quiz.details.name",
                        quizData.name))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_DESCRIPTION,
                        "sebserver.quizdiscovery.quiz.details.description",
                        quizData.description)
                        .asArea())
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_START_TIME,
                        "sebserver.quizdiscovery.quiz.details.starttime",
                        this.widgetFactory.getI18nSupport().formatDisplayDate(quizData.startTime)))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_END_TIME,
                        "sebserver.quizdiscovery.quiz.details.endtime",
                        this.widgetFactory.getI18nSupport().formatDisplayDate(quizData.startTime)))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_START_URL,
                        "sebserver.quizdiscovery.quiz.details.url",
                        quizData.startURL))
                .build();
        this.widgetFactory.labelSeparator(pc.getParent());
    }

}
