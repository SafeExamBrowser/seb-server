/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import java.util.Collection;
import java.util.function.BooleanSupplier;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicators;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.GetExtendedClientEventPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetFinishedExamClientConnection;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class FinishedExamClientConnection implements TemplateComposer {

    private static final LocTextKey PAGE_TITLE_KEY =
            new LocTextKey("sebserver.finished.exam.connection.title");

    private final static LocTextKey EXAM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.finished.connection.form.exam");
    private final static LocTextKey CONNECTION_ID_TEXT_KEY =
            new LocTextKey("sebserver.finished.connection.form.id");
    private final static LocTextKey CONNECTION_INFO_TEXT_KEY =
            new LocTextKey("sebserver.finished.connection.form.info");
    private final static LocTextKey CONNECTION_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.finished.connection.form.status");

    private static final LocTextKey EVENT_LIST_TITLE_KEY =
            new LocTextKey("sebserver.finished.exam.connection.eventlist.title");
    private static final LocTextKey EVENT_LIST_TITLE_TOOLTIP_KEY =
            new LocTextKey("sebserver.finished.exam.connection.eventlist.title.tooltip");
    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.finished.exam.connection.eventlist.empty");
    private static final LocTextKey LIST_COLUMN_TYPE_KEY =
            new LocTextKey("sebserver.finished.exam.connection.eventlist.type");

    private static final LocTextKey LIST_COLUMN_CLIENT_TIME_KEY =
            new LocTextKey("sebserver.finished.exam.connection.eventlist.clienttime");
    private static final LocTextKey LIST_COLUMN_SERVER_TIME_KEY =
            new LocTextKey("sebserver.finished.exam.connection.eventlist.servertime");
    private static final LocTextKey LIST_COLUMN_VALUE_KEY =
            new LocTextKey("sebserver.finished.exam.connection.eventlist.value");
    private static final LocTextKey LIST_COLUMN_TEXT_KEY =
            new LocTextKey("sebserver.finished.exam.connection.eventlist.text");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final I18nSupport i18nSupport;
    private final SEBClientEventDetailsPopup sebClientLogDetailsPopup;
    private final int pageSize;

    private final TableFilterAttribute typeFilter;
    private final TableFilterAttribute textFilter =
            new TableFilterAttribute(CriteriaType.TEXT, ClientEvent.FILTER_ATTR_TEXT);

    protected FinishedExamClientConnection(
            final PageService pageService,
            final SEBClientEventDetailsPopup sebClientLogDetailsPopup,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.i18nSupport = this.resourceService.getI18nSupport();
        this.sebClientLogDetailsPopup = sebClientLogDetailsPopup;
        this.pageSize = pageSize;

        this.typeFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Domain.CLIENT_EVENT.ATTR_TYPE,
                this.resourceService::clientEventTypeResources);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final RestService restService = this.resourceService.getRestService();
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        final EntityKey entityKey = pageContext.getEntityKey();

        // content page layout with title
        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                PAGE_TITLE_KEY);
        final Exam exam = restService
                .getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, parentEntityKey.modelId)
                .call()
                .onError(error -> pageContext.notifyLoadError(EntityType.EXAM, error))
                .getOrThrow();
        final UserInfo user = currentUser.get();
        final boolean supporting = user.hasRole(UserRole.EXAM_SUPPORTER) &&
                exam.supporter.contains(user.uuid);
        final BooleanSupplier isExamSupporter = () -> supporting || user.hasRole(UserRole.EXAM_ADMIN);
        final Collection<Indicator> indicators = restService
                .getBuilder(GetIndicators.class)
                .withQueryParam(Indicator.FILTER_ATTR_EXAM_ID, parentEntityKey.modelId)
                .call()
                .getOrThrow();
        final ClientConnectionData connectionData = restService
                .getBuilder(GetFinishedExamClientConnection.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .getOrThrow();

        final FormBuilder formBuilder = this.pageService.formBuilder(pageContext.copyOf(content))
                .readonly(true)
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_NAME,
                        EXAM_NAME_TEXT_KEY,
                        exam.getName()))
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                        CONNECTION_ID_TEXT_KEY,
                        connectionData.clientConnection.userSessionId))
                .addField(FormBuilder.text(
                        ClientConnection.ATTR_INFO,
                        CONNECTION_INFO_TEXT_KEY,
                        connectionData.clientConnection.info))
                .withDefaultSpanInput(3)
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_STATUS,
                        CONNECTION_STATUS_TEXT_KEY,
                        this.resourceService.localizedClientConnectionStatusName(
                                connectionData.clientConnection.status))
                        .asColorBox())
                .addEmptyCell();

        indicators.forEach(indicator -> {
            if (indicator.type == IndicatorType.LAST_PING || indicator.type == IndicatorType.NONE) {
                return;
            }
            formBuilder.addField(FormBuilder.text(
                    indicator.name,
                    new LocTextKey(indicator.name),
                    connectionData.indicatorValues
                            .stream()
                            .filter(indicatorValue -> indicatorValue.getIndicatorId().equals(indicator.id))
                            .findFirst()
                            .map(iv -> IndicatorValue.getDisplayValue(iv, indicator.type))
                            .orElse(Constants.EMPTY_NOTE))
                    .asColorBox()
                    .withDefaultLabel(indicator.name))
                    .addEmptyCell();
        });

        formBuilder.build();

        // CLIENT EVENTS
        final PageService.PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(
                        pageContext
                                .clearAttributes()
                                .clearEntityKeys());

        widgetFactory.addFormSubContextHeader(
                content,
                EVENT_LIST_TITLE_KEY,
                EVENT_LIST_TITLE_TOOLTIP_KEY);

        // client event table for this connection
        this.pageService
                .entityTableBuilder(
                        "seb-client-" + connectionData.getModelId(),
                        restService.getRestCall(GetExtendedClientEventPage.class))
                .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                .withPaging(this.pageSize)
                .withRestCallAdapter(restCallBuilder -> restCallBuilder.withQueryParam(
                        ClientEvent.FILTER_ATTR_CONNECTION_ID,
                        entityKey.modelId))

                .withColumn(new ColumnDefinition<ExtendedClientEvent>(
                        Domain.CLIENT_EVENT.ATTR_TYPE,
                        LIST_COLUMN_TYPE_KEY,
                        this.resourceService::getEventTypeName)
                                .withFilter(this.typeFilter)
                                .sortable()
                                .widthProportion(2))

                .withColumn(new ColumnDefinition<ExtendedClientEvent>(
                        Domain.CLIENT_EVENT.ATTR_TEXT,
                        LIST_COLUMN_TEXT_KEY,
                        ClientEvent::getText)
                                .withFilter(this.textFilter)
                                .sortable()
                                .withCellTooltip()
                                .widthProportion(4))

                .withColumn(new ColumnDefinition<ExtendedClientEvent>(
                        Domain.CLIENT_EVENT.ATTR_NUMERIC_VALUE,
                        LIST_COLUMN_VALUE_KEY,
                        ClientEvent::getValue)
                                .widthProportion(1))

                .withColumn(new ColumnDefinition<ExtendedClientEvent>(
                        Domain.CLIENT_EVENT.ATTR_CLIENT_TIME,
                        new LocTextKey(LIST_COLUMN_CLIENT_TIME_KEY.name,
                                this.i18nSupport.getUsersTimeZoneTitleSuffix()),
                        this::getClientTime)
                                .sortable()
                                .widthProportion(1))

                .withColumn(new ColumnDefinition<ExtendedClientEvent>(
                        Domain.CLIENT_EVENT.ATTR_SERVER_TIME,
                        new LocTextKey(LIST_COLUMN_SERVER_TIME_KEY.name,
                                this.i18nSupport.getUsersTimeZoneTitleSuffix()),
                        this::getServerTime)
                                .sortable()
                                .widthProportion(1))

                .withDefaultAction(t -> actionBuilder
                        .newAction(ActionDefinition.LOGS_SEB_CLIENT_SHOW_DETAILS)
                        .withExec(action -> this.sebClientLogDetailsPopup.showDetails(action,
                                t.getSingleSelectedROWData()))
                        .noEventPropagation()
                        .create())

                .compose(pageContext.copyOf(content));

        actionBuilder
                .newAction(ActionDefinition.FINISHED_EXAM_BACK_TO_OVERVIEW)
                .withEntityKey(parentEntityKey)
                .publishIf(isExamSupporter);
    }

    private String getClientTime(final ClientEvent event) {
        if (event == null || event.getClientTime() == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.i18nSupport
                .formatDisplayTime(Utils.toDateTimeUTC(event.getClientTime()));
    }

    private String getServerTime(final ClientEvent event) {
        if (event == null || event.getServerTime() == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.i18nSupport
                .formatDisplayTime(Utils.toDateTimeUTC(event.getServerTime()));
    }

}
