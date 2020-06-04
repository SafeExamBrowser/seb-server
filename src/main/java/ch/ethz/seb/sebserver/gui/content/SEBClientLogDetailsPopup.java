/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetClientConnection;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
@GuiProfile
public class SEBClientLogDetailsPopup {

    private static final Logger log = LoggerFactory.getLogger(SEBClientLogDetailsPopup.class);

    private static final LocTextKey DETAILS_TITLE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.details.title");
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

    private final PageService pageService;
    private final ResourceService resourceService;
    private final RestService restService;
    private final I18nSupport i18nSupport;
    private final WidgetFactory widgetFactory;

    public SEBClientLogDetailsPopup(
            final PageService pageService,
            final WidgetFactory widgetFactory) {

        this.pageService = pageService;
        this.widgetFactory = widgetFactory;
        this.resourceService = pageService.getResourceService();
        this.restService = pageService.getRestService();
        this.i18nSupport = pageService.getI18nSupport();
    }

    PageAction showDetails(final PageAction action, final ExtendedClientEvent clientEvent) {
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
                WidgetFactory.CustomVariant.TEXT_H3,
                DETAILS_EVENT_TILE_TEXT_KEY);

        PageContext formContext = pc.copyOf(content);

        this.pageService.formBuilder(formContext)
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
                        this.i18nSupport.formatDisplayDateTime(clientEvent.clientTime) + " " +
                                this.i18nSupport.getUsersTimeZoneTitleSuffix()))
                .addField(FormBuilder.text(
                        Domain.CLIENT_EVENT.ATTR_SERVER_TIME,
                        FORM_SERVERTIME_TEXT_KEY,
                        this.i18nSupport.formatDisplayDateTime(clientEvent.serverTime) + " " +
                                this.i18nSupport.getUsersTimeZoneTitleSuffix()))
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
                WidgetFactory.CustomVariant.TEXT_H3,
                DETAILS_CONNECTION_TILE_TEXT_KEY);

        final ClientConnection connection = this.restService.getBuilder(GetClientConnection.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(clientEvent.connectionId))
                .call()
                .get(
                        error -> log.error("Failed to get ClientConnection for id {}", clientEvent.connectionId, error),
                        () -> ClientConnection.EMPTY_CLIENT_CONNECTION);

        this.pageService.formBuilder(formContext)
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
                WidgetFactory.CustomVariant.TEXT_H3,
                DETAILS_EXAM_TILE_TEXT_KEY);

        final Exam exam = this.restService.getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(clientEvent.examId))
                .call()
                .get(
                        error -> log.error("Failed to get Exam for id {}", clientEvent.examId, error),
                        () -> Exam.EMPTY_EXAM);

        this.pageService.formBuilder(formContext)
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
                        this.i18nSupport.formatDisplayDateWithTimeZone(exam.startTime)))
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_END_TIME,
                        FORM_END_TIME_TEXT_KEY,
                        this.i18nSupport.formatDisplayDateWithTimeZone(exam.endTime)))
                .build();

    }
}
