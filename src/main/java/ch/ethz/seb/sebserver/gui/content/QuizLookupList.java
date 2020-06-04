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
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.eclipse.swt.widgets.Composite;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.CheckExamImported;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.GetQuizPage;
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
public class QuizLookupList implements TemplateComposer {

    // localized text keys

    private static final LocTextKey NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION =
            new LocTextKey("sebserver.quizdiscovery.list.action.no.modify.privilege");
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.list.title");
    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.list.empty");
    private final static LocTextKey EMPTY_SELECTION_TEXT =
            new LocTextKey("sebserver.quizdiscovery.info.pleaseSelect");
    private final static LocTextKey INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.list.column.institution");
    private final static LocTextKey LMS_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.list.column.lmssetup");
    private final static LocTextKey NAME_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.list.column.name");
    private final static LocTextKey START_TIME_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.list.column.starttime");
    private final static LocTextKey END_TIME_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.list.column.endtime");
    private final static LocTextKey DETAILS_TITLE_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.quiz.details.title");
    private static final LocTextKey QUIZ_DETAILS_URL_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.quiz.details.url");
    private static final LocTextKey QUIZ_DETAILS_INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.quiz.details.institution");
    private static final LocTextKey QUIZ_DETAILS_LMS_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.quiz.details.lmssetup");
    private static final LocTextKey QUIZ_DETAILS_NAME_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.quiz.details.name");
    private static final LocTextKey QUIZ_DETAILS_DESCRIPTION_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.quiz.details.description");
    private static final LocTextKey QUIZ_DETAILS_START_TIME_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.quiz.details.starttime");
    private static final LocTextKey QUIZ_DETAILS_END_TIME_TEXT_KEY =
            new LocTextKey("sebserver.quizdiscovery.quiz.details.endtime");
    private final static LocTextKey NO_IMPORT_OF_OUT_DATED_QUIZ =
            new LocTextKey("sebserver.quizdiscovery.quiz.import.out.dated");
    private final static LocTextKey TEXT_KEY_CONFIRM_EXISTING =
            new LocTextKey("sebserver.quizdiscovery.quiz.import.existing.confirm");

    private final static String TEXT_KEY_ADDITIONAL_ATTR_PREFIX =
            "sebserver.quizdiscovery.quiz.details.additional.";

    // filter attribute models
    private final TableFilterAttribute institutionFilter;
    private final TableFilterAttribute lmsFilter;
    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, QuizData.FILTER_ATTR_NAME);

    // dependencies
    private final WidgetFactory widgetFactory;
    private final ResourceService resourceService;
    private final PageService pageService;
    private final int pageSize;

    protected QuizLookupList(
            final PageService pageService,
            final ResourceService resourceService,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.widgetFactory = pageService.getWidgetFactory();
        this.resourceService = resourceService;
        this.pageSize = pageSize;

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
                TITLE_TEXT_KEY);

        final PageActionBuilder actionBuilder =
                this.pageService.pageActionBuilder(pageContext.clearEntityKeys());

        final BooleanSupplier isSEBAdmin =
                () -> currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);

        final Function<String, String> institutionNameFunction =
                this.resourceService.getInstitutionNameFunction();

        // table
        final EntityTable<QuizData> table =
                this.pageService.entityTableBuilder(restService.getRestCall(GetQuizPage.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)

                        .withColumnIf(
                                isSEBAdmin,
                                () -> new ColumnDefinition<QuizData>(
                                        QuizData.QUIZ_ATTR_INSTITUTION_ID,
                                        INSTITUTION_TEXT_KEY,
                                        quiz -> institutionNameFunction
                                                .apply(String.valueOf(quiz.institutionId)))
                                                        .withFilter(this.institutionFilter))

                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_LMS_SETUP_ID,
                                LMS_TEXT_KEY,
                                quizDataLmsSetupNameFunction(this.resourceService))
                                        .withFilter(this.lmsFilter)
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_NAME,
                                NAME_TEXT_KEY,
                                QuizData::getName)
                                        .withFilter(this.nameFilter)
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_START_TIME,
                                new LocTextKey(
                                        START_TIME_TEXT_KEY.name,
                                        i18nSupport.getUsersTimeZoneTitleSuffix()),
                                QuizData::getStartTime)
                                        .withFilter(new TableFilterAttribute(
                                                CriteriaType.DATE,
                                                QuizData.FILTER_ATTR_START_TIME,
                                                Utils.toDateTimeUTC(Utils.getMillisecondsNow())
                                                        .minusYears(1)
                                                        .toString()))
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                QuizData.QUIZ_ATTR_END_TIME,
                                new LocTextKey(
                                        END_TIME_TEXT_KEY.name,
                                        i18nSupport.getUsersTimeZoneTitleSuffix()),
                                QuizData::getEndTime)
                                        .sortable())

                        .withDefaultAction(t -> actionBuilder
                                .newAction(ActionDefinition.QUIZ_DISCOVERY_SHOW_DETAILS)
                                .withExec(action -> this.showDetails(
                                        action,
                                        t.getSingleSelectedROWData(),
                                        institutionNameFunction))
                                .noEventPropagation()
                                .create())

                        .withSelectionListener(this.pageService.getSelectionPublisher(
                                pageContext,
                                ActionDefinition.QUIZ_DISCOVERY_SHOW_DETAILS,
                                ActionDefinition.QUIZ_DISCOVERY_EXAM_IMPORT))

                        .compose(pageContext.copyOf(content));

        // propagate content actions to action-pane
        final GrantCheck examGrant = currentUser.grantCheck(EntityType.EXAM);
        actionBuilder
                .newAction(ActionDefinition.QUIZ_DISCOVERY_SHOW_DETAILS)
                .withSelect(
                        table::getSelection,
                        action -> this.showDetails(
                                action,
                                table.getSingleSelectedROWData(),
                                institutionNameFunction),
                        EMPTY_SELECTION_TEXT)
                .noEventPropagation()
                .publishIf(table::hasAnyContent, false)

                .newAction(ActionDefinition.QUIZ_DISCOVERY_EXAM_IMPORT)
                .withConfirm(importQuizConfirm(table, restService))
                .withSelect(
                        table.getGrantedSelection(currentUser, NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION),
                        action -> this.importQuizData(action, table),
                        EMPTY_SELECTION_TEXT)
                .publishIf(() -> examGrant.im() && table.hasAnyContent(), false);
    }

    private static Function<QuizData, String> quizDataLmsSetupNameFunction(final ResourceService resourceService) {
        return quizData -> resourceService.getLmsSetupNameFunction()
                .apply(String.valueOf(quizData.lmsSetupId));
    }

    private Function<PageAction, LocTextKey> importQuizConfirm(
            final EntityTable<QuizData> table,
            final RestService restService) {

        return action -> {
            action.getSingleSelection();
            final QuizData selectedROWData = table.getSingleSelectedROWData();

            final Collection<EntityKey> existingImports = restService.getBuilder(CheckExamImported.class)
                    .withURIVariable(API.PARAM_MODEL_ID, selectedROWData.id)
                    .call()
                    .getOrThrow();

            if (existingImports != null && !existingImports.isEmpty()) {
                return TEXT_KEY_CONFIRM_EXISTING;
            } else {
                return null;
            }
        };
    }

    private PageAction importQuizData(
            final PageAction action,
            final EntityTable<QuizData> table) {

        action.getSingleSelection();
        final QuizData selectedROWData = table.getSingleSelectedROWData();

        if (selectedROWData.endTime != null) {
            final DateTime now = DateTime.now(DateTimeZone.UTC);
            if (selectedROWData.endTime.isBefore(now)) {
                throw new PageMessageException(NO_IMPORT_OF_OUT_DATED_QUIZ);
            }
        }

        return action
                .withEntityKey(action.getSingleSelection())
                .withParentEntityKey(new EntityKey(selectedROWData.lmsSetupId, EntityType.LMS_SETUP))
                .withAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA, Constants.TRUE_STRING);
    }

    private PageAction showDetails(
            final PageAction action,
            final QuizData quizData,
            final Function<String, String> institutionNameFunction) {

        action.getSingleSelection();

        final ModalInputDialog<Void> dialog = new ModalInputDialog<Void>(
                action.pageContext().getParent().getShell(),
                this.widgetFactory)
                        .setLargeDialogWidth();

        dialog.open(
                DETAILS_TITLE_TEXT_KEY,
                action.pageContext(),
                pc -> createDetailsForm(quizData, pc, institutionNameFunction));

        return action;
    }

    private static final Collection<String> ADDITIONAL_HTML_ATTRIBUTES = Arrays.asList(
            "course_summary");

    private void createDetailsForm(
            final QuizData quizData,
            final PageContext pc,
            final Function<String, String> institutionNameFunction) {

        final Composite parent = pc.getParent();
        final Composite grid = this.widgetFactory.createPopupScrollComposite(parent);

        final FormBuilder formbuilder = this.pageService.formBuilder(pc.copyOf(grid))
                .withDefaultSpanInput(6)
                .withEmptyCellSeparation(false)
                .readonly(true)
                .addFieldIf(
                        () -> this.resourceService.getCurrentUser().get().hasRole(UserRole.SEB_SERVER_ADMIN),
                        () -> FormBuilder.text(
                                QuizData.QUIZ_ATTR_INSTITUTION_ID,
                                QUIZ_DETAILS_INSTITUTION_TEXT_KEY,
                                institutionNameFunction.apply(String.valueOf(quizData.institutionId))))
                .addField(FormBuilder.singleSelection(
                        QuizData.QUIZ_ATTR_LMS_SETUP_ID,
                        QUIZ_DETAILS_LMS_TEXT_KEY,
                        String.valueOf(quizData.lmsSetupId),
                        this.resourceService::lmsSetupResource))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_NAME,
                        QUIZ_DETAILS_NAME_TEXT_KEY,
                        quizData.name))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_DESCRIPTION,
                        QUIZ_DETAILS_DESCRIPTION_TEXT_KEY,
                        quizData.description)
                        .asHTML())
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_START_TIME,
                        QUIZ_DETAILS_START_TIME_TEXT_KEY,
                        this.widgetFactory.getI18nSupport().formatDisplayDateWithTimeZone(quizData.startTime)))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_END_TIME,
                        QUIZ_DETAILS_END_TIME_TEXT_KEY,
                        this.widgetFactory.getI18nSupport().formatDisplayDateWithTimeZone(quizData.endTime)))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_START_URL,
                        QUIZ_DETAILS_URL_TEXT_KEY,
                        quizData.startURL));

        if (!quizData.additionalAttributes.isEmpty()) {
            quizData.additionalAttributes
                    .forEach((key, value) -> {
                        LocTextKey titleKey = new LocTextKey(TEXT_KEY_ADDITIONAL_ATTR_PREFIX + key);
                        if (!this.pageService.getI18nSupport().hasText(titleKey)) {
                            titleKey = new LocTextKey(key);
                        }
                        formbuilder
                                .addField(FormBuilder.text(
                                        key,
                                        titleKey,
                                        toAdditionalValue(key, value))
                                        .asHTML(ADDITIONAL_HTML_ATTRIBUTES.contains(key)));
                    });
        }

        formbuilder.build();
    }

    private String toAdditionalValue(final String name, final String value) {
        if (QuizData.ATTR_ADDITIONAL_CREATION_TIME.equals(name)) {
            try {
                return this.pageService
                        .getI18nSupport()
                        .formatDisplayDate(Utils.toDateTimeUTCUnix(Long.parseLong(value)));
            } catch (final Exception e) {
                return value;
            }
        } else if (QuizData.ATTR_ADDITIONAL_TIME_LIMIT.equals(name)) {
            try {
                return this.pageService
                        .getI18nSupport()
                        .formatDisplayTime(Utils.toDateTimeUTCUnix(Long.parseLong(value)));
            } catch (final Exception e) {
                return value;
            }
        } else {
            return value;
        }
    }

}
