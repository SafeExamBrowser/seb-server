/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.Map;
import java.util.function.Function;

import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.GetExtendedClientEventPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetClientConnection;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class SebClientLogs implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(SebClientLogs.class);

    private static final LocTextKey DETAILS_TITLE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.details.title");
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.title");
    private static final LocTextKey EMPTY_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.empty");

    private static final LocTextKey EXAM_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.column.exam");
    private static final LocTextKey CLIENT_SESSION_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.column.client-session");
    private static final LocTextKey TYPE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.column.type");
    private static final LocTextKey TIME_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.column.time");
    private static final LocTextKey VALUE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.column.value");

    private static final LocTextKey DETAILS_EVENT_TILE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.details.event.title");
    private static final LocTextKey DETAILS_CONNECTION_TILE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.details.connection.title");
    private static final LocTextKey DETAILS_EXAM_TILE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.details.exam.title");

    private static final LocTextKey FORM_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.type");
    private static final LocTextKey FORM_SERVERTIME_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.server-time");
    private static final LocTextKey FORM_CLIENTTIME_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.client-time");
    private static final LocTextKey FORM_VALUE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.value");
    private static final LocTextKey FORM_MESSAGE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.message");

    private static final LocTextKey FORM_SESSION_ID_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.connection.session-id");
    private static final LocTextKey FORM_ADDRESS_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.connection.address");
    private static final LocTextKey FORM_TOKEN_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.connection.token");
    private static final LocTextKey FORM_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.connection.status");

    private static final LocTextKey FORM_EXAM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.exam.name");
    private static final LocTextKey FORM_DESC_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.exam.description");
    private static final LocTextKey FORM_EXAM_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.exam.type");
    private static final LocTextKey FORM_START_TIME_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.exam.startTime");
    private static final LocTextKey FORM_END_TIME_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.form.column.exam.endTime");

    private final static LocTextKey EMPTY_SELECTION_TEXT =
            new LocTextKey("sebserver.seblogs.info.pleaseSelect");

    private final TableFilterAttribute examFilter;
    private final TableFilterAttribute clientSessionFilter;
    private final TableFilterAttribute eventTypeFilter;

    private final PageService pageService;
    private final ResourceService resourceService;
    private final RestService restService;
    private final I18nSupport i18nSupport;
    private final WidgetFactory widgetFactory;
    private final int pageSize;

    public SebClientLogs(
            final PageService pageService,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.restService = this.resourceService.getRestService();
        this.i18nSupport = this.resourceService.getI18nSupport();
        this.widgetFactory = pageService.getWidgetFactory();
        this.pageSize = pageSize;

        this.examFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                ExtendedClientEvent.FILTER_ATTRIBUTE_EXAM,
                this.resourceService::getExamLogSelectionResources);

        this.clientSessionFilter = new TableFilterAttribute(
                CriteriaType.TEXT,
                ClientConnection.FILTER_ATTR_SESSION_ID);

        this.eventTypeFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                ClientEvent.FILTER_ATTR_TYPE,
                this.resourceService::clientEventTypeResources);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        // content page layout with title
        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                TITLE_TEXT_KEY);

        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(
                pageContext
                        .clearEntityKeys()
                        .clearAttributes());

        // table
        final EntityTable<ExtendedClientEvent> table = this.pageService.entityTableBuilder(
                this.restService.getRestCall(GetExtendedClientEventPage.class))
                .withEmptyMessage(EMPTY_TEXT_KEY)
                .withPaging(this.pageSize)

                .withColumn(new ColumnDefinition<>(
                        Domain.CLIENT_CONNECTION.ATTR_EXAM_ID,
                        EXAM_TEXT_KEY,
                        examNameFunction())
                                .withFilter(this.examFilter)
                                .widthProportion(2))

                .withColumn(new ColumnDefinition<>(
                        Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                        CLIENT_SESSION_TEXT_KEY,
                        ExtendedClientEvent::getUserSessionId)
                                .withFilter(this.clientSessionFilter)
                                .sortable()
                                .widthProportion(2))

                .withColumn(new ColumnDefinition<ExtendedClientEvent>(
                        Domain.CLIENT_EVENT.TYPE_NAME,
                        TYPE_TEXT_KEY,
                        this.resourceService::getEventTypeName)
                                .withFilter(this.eventTypeFilter)
                                .sortable()
                                .widthProportion(1))

                .withColumn(new ColumnDefinition<>(
                        Domain.CLIENT_EVENT.ATTR_SERVER_TIME,
                        TIME_TEXT_KEY,
                        this::getEventTime)
                                .withFilter(new TableFilterAttribute(
                                        CriteriaType.DATE_TIME_RANGE,
                                        ClientEvent.FILTER_ATTR_SERVER_TIME_FROM_TO,
                                        Utils.toDateTimeUTC(Utils.getMillisecondsNow())
                                                .minusYears(1)
                                                .toString()))
                                .sortable()
                                .widthProportion(2))

                .withColumn(new ColumnDefinition<ExtendedClientEvent>(
                        Domain.CLIENT_EVENT.ATTR_NUMERIC_VALUE,
                        VALUE_TEXT_KEY,
                        clientEvent -> (clientEvent.numValue != null)
                                ? String.valueOf(clientEvent.numValue)
                                : Constants.EMPTY_NOTE)
                                        .widthProportion(1))

                .withDefaultAction(t -> actionBuilder
                        .newAction(ActionDefinition.LOGS_SEB_CLIENT_SHOW_DETAILS)
                        .withExec(action -> this.showDetails(action, t.getSingleSelectedROWData()))
                        .noEventPropagation()
                        .create())

                .compose(pageContext.copyOf(content));

        actionBuilder
                .newAction(ActionDefinition.LOGS_SEB_CLIENT_SHOW_DETAILS)
                .withSelect(
                        table::getSelection,
                        action -> this.showDetails(action, table.getSingleSelectedROWData()),
                        EMPTY_SELECTION_TEXT)
                .noEventPropagation()
                .publishIf(table::hasAnyContent);
    }

    private PageAction showDetails(final PageAction action, final ExtendedClientEvent clientEvent) {
        action.getSingleSelection();

        final ModalInputDialog<Void> dialog = new ModalInputDialog<>(
                action.pageContext().getParent().getShell(),
                this.widgetFactory);
        dialog.setDialogWidth(600);

        dialog.open(
                DETAILS_TITLE_TEXT_KEY,
                action.pageContext(),
                pc -> createDetailsForm(clientEvent, pc));

        return action;
    }

    private void createDetailsForm(final ExtendedClientEvent clientEvent, final PageContext pc) {

        final Composite parent = pc.getParent();
        final Composite content = this.widgetFactory.createPopupScrollComposite(parent);

        // Event Details Title
        this.widgetFactory.labelLocalized(
                content,
                CustomVariant.TEXT_H3,
                DETAILS_EVENT_TILE_TEXT_KEY);

        this.pageService.formBuilder(pc.copyOf(content))
                .withDefaultSpanInput(6)
                .withEmptyCellSeparation(false)
                .readonly(true)
                .addField(FormBuilder.text(
                        Domain.CLIENT_EVENT.TYPE_NAME,
                        FORM_TYPE_TEXT_KEY,
                        this.resourceService.getEventTypeName(clientEvent)))
                .addField(FormBuilder.text(
                        Domain.CLIENT_EVENT.ATTR_CLIENT_TIME,
                        FORM_CLIENTTIME_TEXT_KEY,
                        this.i18nSupport.formatDisplayDateTime(clientEvent.clientTime)))
                .addField(FormBuilder.text(
                        Domain.CLIENT_EVENT.ATTR_SERVER_TIME,
                        FORM_SERVERTIME_TEXT_KEY,
                        this.i18nSupport.formatDisplayDateTime(clientEvent.serverTime)))
                .addField(FormBuilder.text(
                        Domain.CLIENT_EVENT.ATTR_NUMERIC_VALUE,
                        FORM_VALUE_TEXT_KEY,
                        (clientEvent.numValue != null)
                                ? String.valueOf(clientEvent.numValue)
                                : Constants.EMPTY_NOTE))
                .addField(FormBuilder.text(
                        Domain.CLIENT_EVENT.ATTR_TEXT,
                        FORM_MESSAGE_TEXT_KEY,
                        clientEvent.text)
                        .asArea())
                .build();

        // SEB Client Connection Title
        this.widgetFactory.labelLocalized(
                content,
                CustomVariant.TEXT_H3,
                DETAILS_CONNECTION_TILE_TEXT_KEY);

        final ClientConnection connection = this.restService.getBuilder(GetClientConnection.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(clientEvent.connectionId))
                .call()
                .get(
                        error -> log.error("Failed to get ClientConnection for id {}", clientEvent.connectionId, error),
                        () -> ClientConnection.EMPTY_CLIENT_CONNECTION);

        this.pageService.formBuilder(pc.copyOf(content))
                .withDefaultSpanInput(6)
                .withEmptyCellSeparation(false)
                .readonly(true)
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                        FORM_SESSION_ID_TEXT_KEY,
                        connection.userSessionId))
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_CLIENT_ADDRESS,
                        FORM_ADDRESS_TEXT_KEY,
                        connection.clientAddress))
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN,
                        FORM_TOKEN_TEXT_KEY,
                        connection.connectionToken))
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_STATUS,
                        FORM_STATUS_TEXT_KEY,
                        this.resourceService.localizedClientConnectionStatusName(connection.status)))
                .build();

        // Exam Details Title
        this.widgetFactory.labelLocalized(
                content,
                CustomVariant.TEXT_H3,
                DETAILS_EXAM_TILE_TEXT_KEY);

        final Exam exam = this.restService.getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(clientEvent.examId))
                .call()
                .get(
                        error -> log.error("Failed to get Exam for id {}", clientEvent.examId, error),
                        () -> Exam.EMPTY_EXAM);

        this.pageService.formBuilder(pc.copyOf(content))
                .withDefaultSpanInput(6)
                .withEmptyCellSeparation(false)
                .readonly(true)
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_NAME,
                        FORM_EXAM_NAME_TEXT_KEY,
                        exam.name))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_DESCRIPTION,
                        FORM_DESC_TEXT_KEY,
                        exam.description))
                .addField(FormBuilder.text(
                        Domain.EXAM.ATTR_TYPE,
                        FORM_EXAM_TYPE_TEXT_KEY,
                        this.resourceService.localizedExamTypeName(exam)))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_START_TIME,
                        FORM_START_TIME_TEXT_KEY,
                        this.i18nSupport.formatDisplayDateTime(exam.startTime)))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_END_TIME,
                        FORM_END_TIME_TEXT_KEY,
                        this.i18nSupport.formatDisplayDateTime(exam.endTime)))
                .build();

    }

    private Function<ExtendedClientEvent, String> examNameFunction() {
        final Map<Long, String> examNameMapping = this.resourceService.getExamNameMapping();
        return event -> examNameMapping.get(event.examId);
    }

    private final String getEventTime(final ExtendedClientEvent event) {
        if (event == null || event.serverTime == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.i18nSupport
                .formatDisplayDateTime(Utils.toDateTimeUTC(event.serverTime));
    }

}
