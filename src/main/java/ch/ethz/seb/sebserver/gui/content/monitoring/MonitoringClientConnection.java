/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringFeature;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionActivationEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushContext;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.push.UpdateErrorHandler;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicators;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.GetExtendedClientEventPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.ConfirmPendingClientNotification;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetClientConnectionData;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetPendingClientNotifications;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.session.ClientConnectionDetails;
import ch.ethz.seb.sebserver.gui.service.session.InstructionProcessor;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.MonitoringProctoringService;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class MonitoringClientConnection implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(MonitoringClientConnection.class);

    private static final LocTextKey PAGE_TITLE_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.title");

    private static final LocTextKey NOTIFICATION_LIST_TITLE_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.notificationlist.title");
    private static final LocTextKey NOTIFICATION_LIST_TITLE_TOOLTIP_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.notificationlist.title.tooltip");
    private static final LocTextKey NOTIFICATION_LIST_CONFIRM_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.action.confirm.notification.text");
    private static final LocTextKey NOTIFICATION_LIST_NO_SELECTION_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.notificationlist.pleaseSelect");
    private static final LocTextKey NOTIFICATION_LIST_COLUMN_TYPE_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.notificationlist.type");

    private static final LocTextKey CONFIRM_QUIT =
            new LocTextKey("sebserver.monitoring.exam.connection.action.instruction.quit.confirm");
    private static final LocTextKey CONFIRM_OPEN_SINGLE_ROOM =
            new LocTextKey("sebserver.monitoring.exam.connection.action.singleroom.confirm");

    private static final LocTextKey EVENT_LIST_TITLE_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.eventlist.title");
    private static final LocTextKey EVENT_LIST_TITLE_TOOLTIP_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.eventlist.title.tooltip");
    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.eventlist.empty");
    private static final LocTextKey LIST_COLUMN_TYPE_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.eventlist.type");

    private static final LocTextKey LIST_COLUMN_CLIENT_TIME_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.eventlist.clienttime");
    private static final LocTextKey LIST_COLUMN_SERVER_TIME_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.eventlist.servertime");
    private static final LocTextKey LIST_COLUMN_VALUE_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.eventlist.value");
    private static final LocTextKey LIST_COLUMN_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.eventlist.text");

    private final ServerPushService serverPushService;
    private final PageService pageService;
    private final ResourceService resourceService;
    private final I18nSupport i18nSupport;
    private final InstructionProcessor instructionProcessor;
    private final SEBClientEventDetailsPopup sebClientLogDetailsPopup;
    private final MonitoringProctoringService monitoringProctoringService;
    private final long pollInterval;
    private final int pageSize;

    private final TableFilterAttribute typeFilter;
    private final TableFilterAttribute textFilter =
            new TableFilterAttribute(CriteriaType.TEXT, ClientEvent.FILTER_ATTR_TEXT);

    protected MonitoringClientConnection(
            final ServerPushService serverPushService,
            final PageService pageService,
            final InstructionProcessor instructionProcessor,
            final SEBClientEventDetailsPopup sebClientLogDetailsPopup,
            final MonitoringProctoringService monitoringProctoringService,
            @Value("${sebserver.gui.webservice.poll-interval:500}") final long pollInterval,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.serverPushService = serverPushService;
        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.i18nSupport = this.resourceService.getI18nSupport();
        this.instructionProcessor = instructionProcessor;
        this.monitoringProctoringService = monitoringProctoringService;
        this.pollInterval = pollInterval;
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
        final String connectionToken = pageContext.getAttribute(Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN);

        if (connectionToken == null) {
            pageContext.notifyUnexpectedError(new IllegalAccessException("connectionToken has null reference"));
            return;
        }

        // content page layout with title
        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                PAGE_TITLE_KEY);

        final Exam exam = restService.getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, parentEntityKey.modelId)
                .call()
                .onError(error -> pageContext.notifyLoadError(EntityType.EXAM, error))
                .getOrThrow();
        final UserInfo user = currentUser.get();
        final boolean supporting = user.hasRole(UserRole.EXAM_SUPPORTER) &&
                exam.supporter.contains(user.uuid);
        final BooleanSupplier isExamSupporter = () -> supporting || user.hasRole(UserRole.EXAM_ADMIN);

        final Collection<Indicator> indicators = restService.getBuilder(GetIndicators.class)
                .withQueryParam(Indicator.FILTER_ATTR_EXAM_ID, parentEntityKey.modelId)
                .call()
                .getOrThrow();

        final RestCall<ClientConnectionData>.RestCallBuilder getConnectionData =
                restService.getBuilder(GetClientConnectionData.class)
                        .withURIVariable(API.PARAM_PARENT_MODEL_ID, exam.getModelId())
                        .withURIVariable(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);

        final ClientConnectionData connectionData = getConnectionData
                .call()
                .getOrThrow();

        final ClientConnectionDetails clientConnectionDetails = new ClientConnectionDetails(
                this.pageService,
                pageContext.copyOf(content),
                exam,
                getConnectionData,
                indicators);

        // NOTIFICATIONS
        Supplier<EntityTable<ClientNotification>> _notificationTableSupplier = () -> null;
        if (connectionData.clientConnection.status.clientActiveStatus) {
            final PageService.PageActionBuilder notificationActionBuilder = this.pageService
                    .pageActionBuilder(
                            pageContext
                                    .clearAttributes()
                                    .clearEntityKeys());

            widgetFactory.addFormSubContextHeader(
                    content,
                    NOTIFICATION_LIST_TITLE_KEY,
                    NOTIFICATION_LIST_TITLE_TOOLTIP_KEY);

            final EntityTable<ClientNotification> notificationTable = this.pageService.remoteListTableBuilder(
                    restService.getRestCall(GetPendingClientNotifications.class),
                    EntityType.CLIENT_EVENT)
                    .withRestCallAdapter(builder -> builder.withURIVariable(
                            API.PARAM_PARENT_MODEL_ID,
                            parentEntityKey.modelId)
                            .withURIVariable(
                                    API.EXAM_API_SEB_CONNECTION_TOKEN,
                                    connectionToken))
                    .withPaging(-1)
                    .withColumn(new ColumnDefinition<ClientNotification>(
                            ClientNotification.ATTR_NOTIFICATION_TYPE,
                            NOTIFICATION_LIST_COLUMN_TYPE_KEY,
                            this.resourceService::getNotificationTypeName)
                                    .sortable()
                                    .widthProportion(2))

                    .withColumn(new ColumnDefinition<ClientNotification>(
                            Domain.CLIENT_EVENT.ATTR_TEXT,
                            LIST_COLUMN_TEXT_KEY,
                            ClientEvent::getText)
                                    .sortable()
                                    .withCellTooltip()
                                    .widthProportion(4))
                    .withColumn(new ColumnDefinition<ClientNotification>(
                            Domain.CLIENT_EVENT.ATTR_SERVER_TIME,
                            new LocTextKey(LIST_COLUMN_SERVER_TIME_KEY.name,
                                    this.i18nSupport.getUsersTimeZoneTitleSuffix()),
                            this::getServerTime)
                                    .sortable()
                                    .widthProportion(1))
                    .withDefaultAction(t -> notificationActionBuilder
                            .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION_CONFIRM_NOTIFICATION)
                            .withParentEntityKey(parentEntityKey)
                            .withConfirm(() -> NOTIFICATION_LIST_CONFIRM_TEXT_KEY)
                            .withExec(action -> this.confirmNotification(action, connectionData, t))
                            .noEventPropagation()
                            .create())
                    .withSelectionListener(this.pageService.getSelectionPublisher(
                            pageContext,
                            ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION_CONFIRM_NOTIFICATION))
                    .compose(pageContext.copyOf(content));

            notificationActionBuilder
                    .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION_CONFIRM_NOTIFICATION)
                    .withParentEntityKey(parentEntityKey)
                    .withConfirm(() -> NOTIFICATION_LIST_CONFIRM_TEXT_KEY)
                    .withSelect(
                            () -> notificationTable.getMultiSelection(),
                            action -> this.confirmNotification(action, connectionData, notificationTable),

                            NOTIFICATION_LIST_NO_SELECTION_KEY)
                    .noEventPropagation()
                    .publishIf(isExamSupporter, false);

            _notificationTableSupplier = () -> notificationTable;
        }

        final Supplier<EntityTable<ClientNotification>> notificationTableSupplier = _notificationTableSupplier;
        // server push update
        this.serverPushService.runServerPush(
                new ServerPushContext(
                        content,
                        Utils.truePredicate(),
                        new UpdateErrorHandler(this.pageService, pageContext)),
                this.pollInterval,
                context -> clientConnectionDetails.updateData(),
                context -> clientConnectionDetails.updateGUI(notificationTableSupplier, pageContext));

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
                        "seb-client-" + connectionToken,
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
                .newAction(ActionDefinition.MONITOR_EXAM_BACK_TO_OVERVIEW)
                .withEntityKey(parentEntityKey)
                .publishIf(isExamSupporter)

                .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION_QUIT)
                .withConfirm(() -> CONFIRM_QUIT)
                .withExec(action -> {
                    this.instructionProcessor.propagateSEBQuitInstruction(
                            exam.id,
                            connectionToken,
                            pageContext);
                    return action;
                })
                .noEventPropagation()
                .publishIf(() -> isExamSupporter.getAsBoolean() &&
                        connectionData.clientConnection.status.clientActiveStatus);

        if (connectionData.clientConnection.status == ConnectionStatus.ACTIVE) {
            final ProctoringServiceSettings proctoringSettings = restService
                    .getBuilder(GetExamProctoringSettings.class)
                    .withURIVariable(API.PARAM_MODEL_ID, parentEntityKey.modelId)
                    .call()
                    .onError(error -> log.error("Failed to get ProctoringServiceSettings", error))
                    .getOr(null);

            if (proctoringSettings != null && proctoringSettings.enableProctoring) {
                final ProctoringGUIService proctoringGUIService = this.resourceService
                        .getCurrentUser()
                        .getProctoringGUIService();

                if (proctoringSettings.enabledFeatures.contains(ProctoringFeature.ONE_TO_ONE)) {
                    actionBuilder
                            .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION_PROCTORING)
                            .withEntityKey(parentEntityKey)
                            .withConfirm(() -> CONFIRM_OPEN_SINGLE_ROOM)
                            .withExec(action -> this.monitoringProctoringService.openOneToOneRoom(
                                    action,
                                    connectionData,
                                    proctoringGUIService))
                            .noEventPropagation()
                            .publish();
                }

                clientConnectionDetails.setStatusChangeListener(ccd -> {
                    this.pageService.firePageEvent(
                            new ActionActivationEvent(
                                    ccd.clientConnection.status == ConnectionStatus.ACTIVE,
                                    ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION_QUIT,
                                    ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION_PROCTORING,
                                    ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION_EXAM_ROOM_PROCTORING),
                            pageContext);
                });
            }
        }
    }

    private PageAction confirmNotification(
            final PageAction pageAction,
            final ClientConnectionData connectionData,
            final EntityTable<ClientNotification> table) {

        final EntityKey entityKey = table.getSingleSelection();
        final EntityKey parentEntityKey = pageAction.getParentEntityKey();

        if (entityKey == null) {
            pageAction.pageContext().publishInfo(NOTIFICATION_LIST_NO_SELECTION_KEY);
            return pageAction;
        }

        this.pageService.getRestService()
                .getBuilder(ConfirmPendingClientNotification.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, parentEntityKey.modelId)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .withURIVariable(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionData.clientConnection.connectionToken)
                .call()
                .getOrThrow();

        table.reset();
        return pageAction;
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
