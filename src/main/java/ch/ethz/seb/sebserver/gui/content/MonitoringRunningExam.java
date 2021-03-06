/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.async.AsyncRunner;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.GuiServiceInfo;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.content.action.ActionPane;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionActivationEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushContext;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicators;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetClientConnectionDataList;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetProcotringRooms;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetProctorRoomConnectionData;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetTownhallRoom;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.IsTownhallRoomAvailable;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.session.ClientConnectionTable;
import ch.ethz.seb.sebserver.gui.service.session.InstructionProcessor;
import ch.ethz.seb.sebserver.gui.service.session.ProctoringGUIService;
import ch.ethz.seb.sebserver.gui.widget.Message;

@Lazy
@Component
@GuiProfile
public class MonitoringRunningExam implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(MonitoringRunningExam.class);

 // @formatter:off
    static final String OPEN_ROOM_SCRIPT =
            "try {\n" +
            "var existingWin = window.open('', '%s', 'height=%s,width=%s,location=no,scrollbars=yes,status=no,menubar=0,toolbar=no,titlebar=no,dialog=no');\n" +
            "if(existingWin.location.href === 'about:blank'){\n" +
            "    existingWin.location.href = '%s%s';\n" +
            "    existingWin.focus();\n" +
            "} else {\n" +
            "    existingWin.focus();\n" +
            "}" +
            "}\n" +
            "catch(err) {\n" +
            "    alert(\"Unexpected Javascript Error happened: \" + err);\n"+
            "}";
    // @formatter:on

    private static final String SHOW_CONNECTION_ACTION_APPLIED = "SHOW_CONNECTION_ACTION_APPLIED";
    private static final LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.emptySelection");
    private static final LocTextKey EMPTY_ACTIVE_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.emptySelection.active");
    private static final LocTextKey CONFIRM_QUIT_SELECTED =
            new LocTextKey("sebserver.monitoring.exam.connection.action.instruction.quit.selected.confirm");
    private static final LocTextKey CONFIRM_QUIT_ALL =
            new LocTextKey("sebserver.monitoring.exam.connection.action.instruction.quit.all.confirm");
    private static final LocTextKey CONFIRM_DISABLE_SELECTED =
            new LocTextKey("sebserver.monitoring.exam.connection.action.instruction.disable.selected.confirm");
    private static final LocTextKey EXAM_ROOM_NAME =
            new LocTextKey("sebserver.monitoring.exam.proctoring.room.all.name");

    private final ServerPushService serverPushService;
    private final PageService pageService;
    private final RestService restService;
    private final ResourceService resourceService;
    private final AsyncRunner asyncRunner;
    private final InstructionProcessor instructionProcessor;
    private final GuiServiceInfo guiServiceInfo;
    private final MonitoringExamSearchPopup monitoringExamSearchPopup;
    private final ProctorRoomConnectionsPopup proctorRoomConnectionsPopup;
    private final long pollInterval;
    private final long proctoringRoomUpdateInterval;
    private final String remoteProctoringEndpoint;

    protected MonitoringRunningExam(
            final ServerPushService serverPushService,
            final PageService pageService,
            final AsyncRunner asyncRunner,
            final InstructionProcessor instructionProcessor,
            final GuiServiceInfo guiServiceInfo,
            final MonitoringExamSearchPopup monitoringExamSearchPopup,
            final ProctorRoomConnectionsPopup proctorRoomConnectionsPopup,
            @Value("${sebserver.gui.webservice.poll-interval:1000}") final long pollInterval,
            @Value("${sebserver.gui.remote.proctoring.entrypoint:/remote-proctoring}") final String remoteProctoringEndpoint,
            @Value("${sebserver.gui.remote.proctoring.rooms.update.poll-interval:5000}") final long proctoringRoomUpdateInterval) {

        this.serverPushService = serverPushService;
        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.resourceService = pageService.getResourceService();
        this.asyncRunner = asyncRunner;
        this.instructionProcessor = instructionProcessor;
        this.guiServiceInfo = guiServiceInfo;
        this.pollInterval = pollInterval;
        this.monitoringExamSearchPopup = monitoringExamSearchPopup;
        this.remoteProctoringEndpoint = remoteProctoringEndpoint;
        this.proctorRoomConnectionsPopup = proctorRoomConnectionsPopup;
        this.proctoringRoomUpdateInterval = proctoringRoomUpdateInterval;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final RestService restService = this.resourceService.getRestService();
        final EntityKey entityKey = pageContext.getEntityKey();
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final Exam exam = restService.getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .getOrThrow();

        final Collection<Indicator> indicators = restService.getBuilder(GetIndicators.class)
                .withQueryParam(Indicator.FILTER_ATTR_EXAM_ID, entityKey.modelId)
                .call()
                .getOrThrow();

        final Composite content = this.pageService.getWidgetFactory().defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey("sebserver.monitoring.exam", exam.name));

        final Composite tablePane = new Composite(content, SWT.NONE);
        tablePane.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.heightHint = 100;
        tablePane.setLayoutData(gridData);

        final PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(pageContext.clearEntityKeys());

        final RestCall<Collection<ClientConnectionData>>.RestCallBuilder restCall =
                restService.getBuilder(GetClientConnectionDataList.class)
                        .withURIVariable(API.PARAM_PARENT_MODEL_ID, exam.getModelId());

        final ClientConnectionTable clientTable = new ClientConnectionTable(
                this.pageService,
                tablePane,
                this.asyncRunner,
                exam,
                indicators,
                restCall);

        clientTable
                .withDefaultAction(
                        actionBuilder
                                .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION)
                                .withParentEntityKey(entityKey)
                                .create(),
                        this.pageService)
                .withSelectionListener(this.pageService.getSelectionPublisher(
                        pageContext,
                        ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION,
                        ActionDefinition.MONITOR_EXAM_QUIT_SELECTED,
                        ActionDefinition.MONITOR_EXAM_DISABLE_SELECTED_CONNECTION,
                        ActionDefinition.MONITOR_EXAM_NEW_PROCTOR_ROOM));

        this.serverPushService.runServerPush(
                new ServerPushContext(
                        content,
                        Utils.truePredicate(),
                        createServerPushUpdateErrorHandler(this.pageService, pageContext)),
                this.pollInterval,
                context -> clientTable.updateValues(),
                updateTableGUI(clientTable));

        final BooleanSupplier isExamSupporter = () -> currentUser.get().hasRole(UserRole.EXAM_SUPPORTER);

        actionBuilder

                .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION)
                .withParentEntityKey(entityKey)
                .withExec(pageAction -> {
                    final Tuple<String> singleSelection = clientTable.getSingleSelection();
                    if (singleSelection == null) {
                        throw new PageMessageException(EMPTY_SELECTION_TEXT_KEY);
                    }

                    final PageAction copyOfPageAction = PageAction.copyOf(pageAction);
                    copyOfPageAction.withEntityKey(new EntityKey(
                            singleSelection._1,
                            EntityType.CLIENT_CONNECTION));
                    copyOfPageAction.withAttribute(
                            Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN,
                            singleSelection._2);

                    return copyOfPageAction;
                })
                .publishIf(isExamSupporter, false)

                .newAction(ActionDefinition.MONITOR_EXAM_QUIT_ALL)
                .withEntityKey(entityKey)
                .withConfirm(() -> CONFIRM_QUIT_ALL)
                .withExec(action -> this.quitSEBClients(action, clientTable, true))
                .noEventPropagation()
                .publishIf(isExamSupporter)

                .newAction(ActionDefinition.MONITORING_EXAM_SEARCH_CONNECTIONS)
                .withEntityKey(entityKey)
                .withExec(this::openSearchPopup)
                .noEventPropagation()
                .publishIf(isExamSupporter)

                .newAction(ActionDefinition.MONITOR_EXAM_QUIT_SELECTED)
                .withEntityKey(entityKey)
                .withConfirm(() -> CONFIRM_QUIT_SELECTED)
                .withSelect(
                        () -> this.selectionForQuitInstruction(clientTable),
                        action -> this.quitSEBClients(action, clientTable, false),
                        EMPTY_ACTIVE_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(isExamSupporter, false)

                .newAction(ActionDefinition.MONITOR_EXAM_DISABLE_SELECTED_CONNECTION)
                .withEntityKey(entityKey)
                .withConfirm(() -> CONFIRM_DISABLE_SELECTED)
                .withSelect(
                        clientTable::getSelection,
                        action -> this.disableSEBClients(action, clientTable, false),
                        EMPTY_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(isExamSupporter, false);

        if (isExamSupporter.getAsBoolean()) {
            addFilterActions(actionBuilder, clientTable, isExamSupporter);
            addProctoringActions(
                    currentUser.getProctoringGUIService(),
                    pageContext,
                    content,
                    actionBuilder);
        }
    }

    private void addProctoringActions(
            final ProctoringGUIService proctoringGUIService,
            final PageContext pageContext,
            final Composite parent,
            final PageActionBuilder actionBuilder) {

        final EntityKey entityKey = pageContext.getEntityKey();
        final ProctoringSettings proctoringSettings = this.restService
                .getBuilder(GetProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .getOr(null);

        if (proctoringSettings != null && proctoringSettings.enableProctoring) {

            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM)
                    .withEntityKey(entityKey)
                    .withExec(action -> this.toggleTownhallRoom(proctoringGUIService, action))
                    .noEventPropagation()
                    .publish();

            if (isTownhallRoomActive(entityKey.modelId)) {
                this.pageService.firePageEvent(
                        new ActionActivationEvent(
                                true,
                                new Tuple<>(
                                        ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                                        ActionDefinition.MONITOR_EXAM_CLOSE_TOWNHALL_PROCTOR_ROOM)),
                        pageContext);
            }

            final Map<String, Pair<RemoteProctoringRoom, TreeItem>> availableRooms = new HashMap<>();
            updateRoomActions(
                    pageContext,
                    availableRooms,
                    actionBuilder,
                    proctoringSettings,
                    proctoringGUIService);
            this.serverPushService.runServerPush(
                    new ServerPushContext(
                            parent,
                            Utils.truePredicate(),
                            createServerPushUpdateErrorHandler(this.pageService, pageContext)),
                    this.proctoringRoomUpdateInterval,
                    context -> updateRoomActions(
                            pageContext,
                            availableRooms,
                            actionBuilder,
                            proctoringSettings,
                            proctoringGUIService));
        }
    }

    private void addFilterActions(
            final PageActionBuilder actionBuilder,
            final ClientConnectionTable clientTable,
            final BooleanSupplier isExamSupporter) {

        addClosedFilterAction(actionBuilder, clientTable);
        addRequestedFilterAction(actionBuilder, clientTable);
        addDisabledFilterAction(actionBuilder, clientTable);
    }

    private void addDisabledFilterAction(
            final PageActionBuilder actionBuilder,
            final ClientConnectionTable clientTable) {

        if (clientTable.isStatusHidden(ConnectionStatus.DISABLED)) {
            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_SHOW_DISABLED_CONNECTION)
                    .withExec(showStateViewAction(clientTable, ConnectionStatus.DISABLED))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_HIDE_DISABLED_CONNECTION)
                                    .withExec(hideStateViewAction(clientTable, ConnectionStatus.DISABLED))
                                    .noEventPropagation()
                                    .create())
                    .publish();
        } else {
            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_HIDE_DISABLED_CONNECTION)
                    .withExec(hideStateViewAction(clientTable, ConnectionStatus.DISABLED))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_SHOW_DISABLED_CONNECTION)
                                    .withExec(showStateViewAction(clientTable, ConnectionStatus.DISABLED))
                                    .noEventPropagation()
                                    .create())
                    .publish();
        }
    }

    private void addRequestedFilterAction(
            final PageActionBuilder actionBuilder,
            final ClientConnectionTable clientTable) {

        if (clientTable.isStatusHidden(ConnectionStatus.CONNECTION_REQUESTED)) {
            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_SHOW_REQUESTED_CONNECTION)
                    .withExec(showStateViewAction(clientTable, ConnectionStatus.CONNECTION_REQUESTED))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_HIDE_REQUESTED_CONNECTION)
                                    .withExec(
                                            hideStateViewAction(clientTable, ConnectionStatus.CONNECTION_REQUESTED))
                                    .noEventPropagation()
                                    .create())
                    .publish();
        } else {
            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_HIDE_REQUESTED_CONNECTION)
                    .withExec(hideStateViewAction(clientTable, ConnectionStatus.CONNECTION_REQUESTED))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_SHOW_REQUESTED_CONNECTION)
                                    .withExec(
                                            showStateViewAction(clientTable, ConnectionStatus.CONNECTION_REQUESTED))
                                    .noEventPropagation()
                                    .create())
                    .publish();
        }
    }

    private void addClosedFilterAction(
            final PageActionBuilder actionBuilder,
            final ClientConnectionTable clientTable) {

        if (clientTable.isStatusHidden(ConnectionStatus.CLOSED)) {
            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_SHOW_CLOSED_CONNECTION)
                    .withExec(showStateViewAction(clientTable, ConnectionStatus.CLOSED))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_HIDE_CLOSED_CONNECTION)
                                    .withExec(hideStateViewAction(clientTable, ConnectionStatus.CLOSED))
                                    .noEventPropagation()
                                    .create())
                    .publish();
        } else {
            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_HIDE_CLOSED_CONNECTION)
                    .withExec(hideStateViewAction(clientTable, ConnectionStatus.CLOSED))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_SHOW_CLOSED_CONNECTION)
                                    .withExec(showStateViewAction(clientTable, ConnectionStatus.CLOSED))
                                    .noEventPropagation()
                                    .create())
                    .publish();
        }
    }

    private boolean isTownhallRoomActive(final String examModelId) {
        return !BooleanUtils.toBoolean(this.pageService
                .getRestService()
                .getBuilder(IsTownhallRoomAvailable.class)
                .withURIVariable(API.PARAM_MODEL_ID, examModelId)
                .call()
                .getOr(Constants.FALSE_STRING));
    }

    private PageAction openSearchPopup(final PageAction action) {
        this.monitoringExamSearchPopup.show(action.pageContext());
        return action;
    }

    private PageAction toggleTownhallRoom(
            final ProctoringGUIService proctoringGUIService,
            final PageAction action) {

        if (isTownhallRoomActive(action.getEntityKey().modelId)) {
            closeTownhallRoom(proctoringGUIService, action);
            this.pageService.firePageEvent(
                    new ActionActivationEvent(
                            true,
                            new Tuple<>(
                                    ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                                    ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM)),
                    action.pageContext());
            return action;
        } else {
            openTownhallRoom(proctoringGUIService, action);
            this.pageService.firePageEvent(
                    new ActionActivationEvent(
                            true,
                            new Tuple<>(
                                    ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                                    ActionDefinition.MONITOR_EXAM_CLOSE_TOWNHALL_PROCTOR_ROOM)),
                    action.pageContext());
            return action;
        }
    }

    private PageAction openTownhallRoom(
            final ProctoringGUIService proctoringGUIService,
            final PageAction action) {

        try {
            final EntityKey examId = action.getEntityKey();
            String activeAllRoomName = proctoringGUIService.getTownhallRoom(examId.modelId);

            if (activeAllRoomName == null) {
                final SEBProctoringConnection proctoringConnectionData = proctoringGUIService
                        .registerTownhallRoom(
                                examId.modelId,
                                this.pageService.getI18nSupport().getText(EXAM_ROOM_NAME))
                        .onError(error -> log.error(
                                "Failed to open all collecting room for exam {} {}", examId.modelId,
                                error.getMessage()))
                        .getOrThrow();
                ProctoringGUIService.setCurrentProctoringWindowData(
                        examId.modelId,
                        proctoringConnectionData);
                activeAllRoomName = proctoringConnectionData.roomName;
            }

            final JavaScriptExecutor javaScriptExecutor = RWT.getClient().getService(JavaScriptExecutor.class);
            final String script = String.format(
                    OPEN_ROOM_SCRIPT,
                    activeAllRoomName,
                    800,
                    1200,
                    this.guiServiceInfo.getExternalServerURIBuilder().toUriString(),
                    this.remoteProctoringEndpoint);
            javaScriptExecutor.execute(script);
            proctoringGUIService.registerProctoringWindow(activeAllRoomName);

        } catch (final Exception e) {
            log.error("Failed to open popup for townhall room: ", e);
        }
        return action;
    }

    private PageAction closeTownhallRoom(
            final ProctoringGUIService proctoringGUIService,
            final PageAction action) {

        final String examId = action.getEntityKey().modelId;
        final RemoteProctoringRoom townhall = this.pageService.getRestService()
                .getBuilder(GetTownhallRoom.class)
                .withURIVariable(API.PARAM_MODEL_ID, examId)
                .call()
                .getOr(null);

        if (townhall == null || townhall.id == null) {
            log.warn(
                    "Close townhall action cannot get active townhall form webservice for exam: {}",
                    examId);
            return action;
        }

        try {
            proctoringGUIService.closeRoom(townhall.name);
        } catch (final Exception e) {
            log.error("Failed to close proctoring townhall room for exam: {}", examId);
        }
        return action;
    }

    private void updateTownhallButton(
            final ProctoringGUIService proctoringGUIService,
            final PageContext pageContext) {
        final EntityKey entityKey = pageContext.getEntityKey();

        if (isTownhallRoomActive(entityKey.modelId)) {
            final boolean townhallRoomFromThisUser = proctoringGUIService
                    .getTownhallRoom(entityKey.modelId) != null;
            if (townhallRoomFromThisUser) {
                this.pageService.firePageEvent(
                        new ActionActivationEvent(
                                true,
                                new Tuple<>(
                                        ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                                        ActionDefinition.MONITOR_EXAM_CLOSE_TOWNHALL_PROCTOR_ROOM)),
                        pageContext);
            } else {
                this.pageService.firePageEvent(
                        new ActionActivationEvent(
                                false,
                                ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                                ActionDefinition.MONITOR_EXAM_CLOSE_TOWNHALL_PROCTOR_ROOM),
                        pageContext);
            }
        } else {
            this.pageService.firePageEvent(
                    new ActionActivationEvent(
                            true,
                            new Tuple<>(
                                    ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                                    ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM),
                            ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                            ActionDefinition.MONITOR_EXAM_CLOSE_TOWNHALL_PROCTOR_ROOM),
                    pageContext);
        }
    }

    private void updateRoomActions(
            final PageContext pageContext,
            final Map<String, Pair<RemoteProctoringRoom, TreeItem>> rooms,
            final PageActionBuilder actionBuilder,
            final ProctoringSettings proctoringSettings,
            final ProctoringGUIService proctoringGUIService) {

        final EntityKey entityKey = pageContext.getEntityKey();
        updateTownhallButton(proctoringGUIService, pageContext);
        final I18nSupport i18nSupport = this.pageService.getI18nSupport();
        this.pageService.getRestService().getBuilder(GetProcotringRooms.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .onError(error -> log.error("Failed to update proctoring rooms on GUI {}", error.getMessage()))
                .getOr(Collections.emptyList())
                .stream()
                .forEach(room -> {
                    if (rooms.containsKey(room.name)) {
                        // update action
                        final TreeItem treeItem = rooms.get(room.name).b;
                        rooms.put(room.name, new Pair<>(room, treeItem));
                        treeItem.setText(i18nSupport.getText(new LocTextKey(
                                ActionDefinition.MONITOR_EXAM_VIEW_PROCTOR_ROOM.title.name,
                                room.subject,
                                room.roomSize,
                                proctoringSettings.collectingRoomSize)));
                        processProctorRoomActionActivation(treeItem, room, pageContext);
                    } else {
                        // create new action
                        final PageAction action =
                                actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_VIEW_PROCTOR_ROOM)
                                        .withEntityKey(entityKey)
                                        .withExec(_action -> {
                                            final int actualRoomSize = getActualRoomSize(room, rooms);
                                            if (actualRoomSize <= 0) {
                                                return _action;
                                            }
                                            return showExamProctoringRoom(proctoringSettings, room, _action);
                                        })
                                        .withNameAttributes(
                                                room.subject,
                                                room.roomSize,
                                                proctoringSettings.collectingRoomSize)
                                        .noEventPropagation()
                                        .create();

                        this.pageService.publishAction(
                                action,
                                _treeItem -> rooms.put(room.name, new Pair<>(room, _treeItem)));
                        addRoomConnectionsPopupListener(pageContext, rooms);
                        processProctorRoomActionActivation(rooms.get(room.name).b, room, pageContext);
                    }
                });
    }

    private void processProctorRoomActionActivation(
            final TreeItem treeItem,
            final RemoteProctoringRoom room,
            final PageContext pageContext) {

        try {
            final Display display = pageContext.getRoot().getDisplay();
            final PageAction action = (PageAction) treeItem.getData(ActionPane.ACTION_EVENT_CALL_KEY);
            final Image image = room.roomSize > 0
                    ? action.definition.icon.getImage(display)
                    : action.definition.icon.getGreyedImage(display);
            treeItem.setImage(image);
            treeItem.setForeground(room.roomSize > 0 ? null : new Color(display, Constants.GREY_DISABLED));
        } catch (final Exception e) {
            log.warn("Failed to set Proctor-Room-Activation: ", e.getMessage());
        }
    }

    private void addRoomConnectionsPopupListener(
            final PageContext pageContext,
            final Map<String, Pair<RemoteProctoringRoom, TreeItem>> rooms) {

        if (!rooms.isEmpty()) {
            final EntityKey entityKey = pageContext.getEntityKey();
            final TreeItem treeItem = rooms.values().iterator().next().b;
            final Tree tree = treeItem.getParent();
            if (tree.getData(SHOW_CONNECTION_ACTION_APPLIED) == null) {
                tree.addListener(SWT.Selection, event -> {
                    final TreeItem item = (TreeItem) event.item;
                    item.getParent().deselectAll();
                    if (event.button == 3) {
                        rooms.entrySet()
                                .stream()
                                .filter(e -> e.getValue().b.equals(item))
                                .findFirst()
                                .ifPresent(e -> {
                                    final RemoteProctoringRoom room = e.getValue().a;
                                    if (room.roomSize > 0) {
                                        final PageContext pc = pageContext.copy()
                                                .clearAttributes()
                                                .withEntityKey(new EntityKey(room.name,
                                                        EntityType.REMOTE_PROCTORING_ROOM))
                                                .withParentEntityKey(entityKey);
                                        this.proctorRoomConnectionsPopup.show(pc, room.subject);
                                    }
                                });
                    }
                });
                tree.setData(SHOW_CONNECTION_ACTION_APPLIED, true);
            }
        }
    }

    private int getActualRoomSize(
            final RemoteProctoringRoom room,
            final Map<String, Pair<RemoteProctoringRoom, TreeItem>> rooms) {

        return rooms.get(room.name).a.roomSize;
    }

    private PageAction showExamProctoringRoom(
            final ProctoringSettings proctoringSettings,
            final RemoteProctoringRoom room,
            final PageAction action) {

        final SEBProctoringConnection proctoringConnectionData = this.pageService
                .getRestService()
                .getBuilder(GetProctorRoomConnectionData.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(proctoringSettings.examId))
                .withQueryParam(SEBProctoringConnection.ATTR_ROOM_NAME, room.name)
                .withQueryParam(SEBProctoringConnection.ATTR_SUBJECT, Utils.encodeFormURL_UTF_8(room.subject))
                .call()
                .getOrThrow();

        ProctoringGUIService.setCurrentProctoringWindowData(
                String.valueOf(proctoringSettings.examId),
                proctoringConnectionData);

        final String script = String.format(
                OPEN_ROOM_SCRIPT,
                room.name,
                800,
                1200,
                this.guiServiceInfo.getExternalServerURIBuilder().toUriString(),
                this.remoteProctoringEndpoint);

        RWT.getClient()
                .getService(JavaScriptExecutor.class)
                .execute(script);

        this.pageService.getCurrentUser()
                .getProctoringGUIService()
                .registerProctoringWindow(room.name);

        return action;
    }

    private static Function<PageAction, PageAction> showStateViewAction(
            final ClientConnectionTable clientTable,
            final ConnectionStatus status) {

        return action -> {
            clientTable.showStatus(status);
            clientTable.removeSelection();
            return action;
        };
    }

    private static Function<PageAction, PageAction> hideStateViewAction(
            final ClientConnectionTable clientTable,
            final ConnectionStatus status) {

        return action -> {
            clientTable.hideStatus(status);
            clientTable.removeSelection();
            return action;
        };
    }

    private Set<EntityKey> selectionForQuitInstruction(final ClientConnectionTable clientTable) {
        final Set<String> connectionTokens = clientTable.getConnectionTokens(
                ClientConnection.getStatusPredicate(ConnectionStatus.ACTIVE),
                true);
        if (connectionTokens == null || connectionTokens.isEmpty()) {
            return Collections.emptySet();
        }

        return clientTable.getSelection();
    }

    private PageAction quitSEBClients(
            final PageAction action,
            final ClientConnectionTable clientTable,
            final boolean all) {

        this.instructionProcessor.propagateSEBQuitInstruction(
                clientTable.getExam().id,
                statesPredicate -> clientTable.getConnectionTokens(
                        statesPredicate,
                        !all),
                action.pageContext());

        clientTable.removeSelection();
        clientTable.forceUpdateAll();
        return action;
    }

    private PageAction disableSEBClients(
            final PageAction action,
            final ClientConnectionTable clientTable,
            final boolean all) {

        this.instructionProcessor.disableConnection(
                clientTable.getExam().id,
                statesPredicate -> clientTable.getConnectionTokens(
                        statesPredicate,
                        !all),
                action.pageContext());

        clientTable.removeSelection();
        clientTable.forceUpdateAll();
        return action;
    }

    private Consumer<ServerPushContext> updateTableGUI(final ClientConnectionTable clientTable) {
        return context -> {
            if (!context.isDisposed()) {
                try {
                    clientTable.updateGUI();
                    context.layout();
                } catch (final Exception e) {
                    if (log.isWarnEnabled()) {
                        log.warn("Unexpected error while trying to update GUI: ", e);
                    }
                }
            }
        };
    }

    static final Function<Exception, Boolean> createServerPushUpdateErrorHandler(
            final PageService pageService,
            final PageContext pageContext) {

        return error -> {
            log.error("Fialed to update server push: {}", error.getMessage());
            try {
                pageService.getCurrentUser().get();
            } catch (final Exception e) {
                log.error("Failed to verify current user after server push error: {}", e.getMessage());
                log.info("Force logout and session cleanup...");
                pageContext.forwardToLoginPage();
                final MessageBox logoutSuccess = new Message(
                        pageContext.getShell(),
                        pageService.getI18nSupport().getText("sebserver.logout"),
                        Utils.formatLineBreaks(
                                pageService.getI18nSupport().getText("sebserver.logout.invalid-session.message")),
                        SWT.ICON_INFORMATION,
                        pageService.getI18nSupport());
                logoutSuccess.open(null);
                return true;
            }
            return false;
        };
    }

}
