/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session.proctoring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.GuiServiceInfo;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.content.action.ActionPane;
import ch.ethz.seb.sebserver.gui.content.monitoring.ProctorRoomConnectionsPopup;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionActivationEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetCollectingRooms;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetProctorRoomConnection;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.IsTownhallRoomAvailable;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.NotifyProctoringRoomOpened;

@Lazy
@Component
@GuiProfile
public class MonitoringProctoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringProctoringService.class);

    public static final LocTextKey OPEN_TOWNHALL_ERROR =
            new LocTextKey("sebserver.exam.proctoring.townhall.open.error");
    public static final LocTextKey CLOSE_TOWNHALL_ERROR =
            new LocTextKey("sebserver.exam.proctoring.townhall.close.error");
    public static final LocTextKey OPEN_ONE_ERROR =
            new LocTextKey("sebserver.exam.proctoring.one.open.error");
    public static final LocTextKey CLOSE_ONE_ERROR =
            new LocTextKey("sebserver.exam.proctoring.one.close.error");
    public static final LocTextKey OPEN_COLLECTING_ERROR =
            new LocTextKey("sebserver.exam.proctoring.collecting.open.error");
    public static final LocTextKey CLOSE_COLLECTING_ERROR =
            new LocTextKey("sebserver.exam.proctoring.collecting.close.error");

    private static final LocTextKey EXAM_ROOM_NAME =
            new LocTextKey("sebserver.monitoring.exam.proctoring.room.all.name");

    private static final String OPEN_ROOM_SCRIPT_RES =
            "classpath:ch/ethz/seb/sebserver/gui/service/session/proctoring/openRoomScript.js";

    private final PageService pageService;
    private final GuiServiceInfo guiServiceInfo;
    private final ProctorRoomConnectionsPopup proctorRoomConnectionsPopup;
    private final JSONMapper jsonMapper;
    private final Resource openRoomScriptRes;
    private final String remoteProctoringEndpoint;

    public MonitoringProctoringService(
            final PageService pageService,
            final GuiServiceInfo guiServiceInfo,
            final ProctorRoomConnectionsPopup proctorRoomConnectionsPopup,
            final JSONMapper jsonMapper,
            @Value(OPEN_ROOM_SCRIPT_RES) final Resource openRoomScript,
            @Value("${sebserver.gui.remote.proctoring.entrypoint:/remote-proctoring}") final String remoteProctoringEndpoint) {

        this.pageService = pageService;
        this.guiServiceInfo = guiServiceInfo;
        this.proctorRoomConnectionsPopup = proctorRoomConnectionsPopup;
        this.jsonMapper = jsonMapper;
        this.openRoomScriptRes = openRoomScript;
        this.remoteProctoringEndpoint = remoteProctoringEndpoint;
    }

    public boolean isTownhallRoomActive(final String examModelId) {
        return !BooleanUtils.toBoolean(this.pageService
                .getRestService()
                .getBuilder(IsTownhallRoomAvailable.class)
                .withURIVariable(API.PARAM_MODEL_ID, examModelId)
                .call()
                .getOr(Constants.FALSE_STRING));
    }

    public PageAction toggleTownhallRoom(
            final ProctoringGUIService proctoringGUIService,
            final ProctoringServiceSettings proctoringSettings,
            final PageAction action) {

        if (isTownhallRoomActive(action.getEntityKey().modelId)) {
            if (closeTownhallRoom(proctoringGUIService, action)) {
                this.pageService.firePageEvent(
                        new ActionActivationEvent(
                                true,
                                new Tuple<>(
                                        ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                                        ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM)),
                        action.pageContext());
            } else {
                action.pageContext().notifyError(CLOSE_TOWNHALL_ERROR, null);
            }

        } else {
            if (openTownhallRoom(proctoringGUIService, proctoringSettings, action)) {
                this.pageService.firePageEvent(
                        new ActionActivationEvent(
                                true,
                                new Tuple<>(
                                        ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                                        ActionDefinition.MONITOR_EXAM_CLOSE_TOWNHALL_PROCTOR_ROOM)),
                        action.pageContext());
            } else {
                action.pageContext().notifyError(OPEN_TOWNHALL_ERROR, null);
            }
        }
        return action;
    }

    public void initCollectingRoomActions(
            final PageContext pageContext,
            final PageActionBuilder actionBuilder,
            final ProctoringServiceSettings proctoringSettings,
            final ProctoringGUIService proctoringGUIService) {

        proctoringGUIService.clearCollectingRoomActionState();
        final EntityKey entityKey = pageContext.getEntityKey();
        final Collection<RemoteProctoringRoom> collectingRooms = this.pageService
                .getRestService()
                .getBuilder(GetCollectingRooms.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .onError(error -> log.error("Failed to get collecting room data:", error))
                .getOr(Collections.emptyList());

        updateCollectingRoomActions(
                collectingRooms,
                pageContext,
                actionBuilder,
                proctoringSettings,
                proctoringGUIService);
    }

    public void updateCollectingRoomActions(
            final Collection<RemoteProctoringRoom> collectingRooms,
            final PageContext pageContext,
            final PageActionBuilder actionBuilder,
            final ProctoringServiceSettings proctoringSettings,
            final ProctoringGUIService proctoringGUIService) {

        final EntityKey entityKey = pageContext.getEntityKey();
        final I18nSupport i18nSupport = this.pageService.getI18nSupport();

        collectingRooms
                .stream()
                .forEach(room -> {
                    if (proctoringGUIService.collectingRoomActionActive(room.name)) {
                        // update action
                        final TreeItem treeItem = proctoringGUIService.getCollectingRoomActionItem(room.name);
                        proctoringGUIService.registerCollectingRoomAction(room, treeItem);
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
                                        .withExec(_action -> openExamProctoringRoom(
                                                proctoringGUIService,
                                                proctoringSettings,
                                                room,
                                                _action))
                                        .withNameAttributes(
                                                room.subject,
                                                room.roomSize,
                                                proctoringSettings.collectingRoomSize)
                                        .noEventPropagation()
                                        .create();

                        this.pageService.publishAction(
                                action,
                                _treeItem -> proctoringGUIService.registerCollectingRoomAction(
                                        room,
                                        _treeItem,
                                        collectingRoom -> showCollectingRoomPopup(pageContext, entityKey,
                                                collectingRoom)));

                        processProctorRoomActionActivation(
                                proctoringGUIService.getCollectingRoomActionItem(room.name),
                                room, pageContext);
                    }
                });

        updateTownhallButton(proctoringGUIService, pageContext);
    }

    private void showCollectingRoomPopup(
            final PageContext pageContext,
            final EntityKey entityKey,
            final RemoteProctoringRoom collectingRoom) {

        final String additionalRoomData = collectingRoom.getAdditionalRoomData();
        String joinURL = null;
        try {
            final Map<String, String> roomData = this.jsonMapper.readValue(
                    additionalRoomData,
                    new TypeReference<Map<String, String>>() {
                    });
            joinURL = roomData.get("start_url");
        } catch (final Exception e) {
            log.error("Failed to get proctoring start URL: ", e);
        }
        final PageContext pc = pageContext.copy()
                .clearAttributes()
                .withEntityKey(
                        new EntityKey(collectingRoom.name,
                                EntityType.REMOTE_PROCTORING_ROOM))
                .withParentEntityKey(entityKey)
                .withAttribute(
                        ProctorRoomConnectionsPopup.PAGE_ATTR_JOIN_LINK,
                        joinURL);
        this.proctorRoomConnectionsPopup.show(pc, collectingRoom.subject);
    }

    private PageAction openExamProctoringRoom(
            final ProctoringGUIService proctoringGUIService,
            final ProctoringServiceSettings proctoringSettings,
            final RemoteProctoringRoom room,
            final PageAction action) {

        if (!proctoringGUIService.isCollectingRoomEnabled(room.name)) {
            return action;
        }

        final ProctoringRoomConnection proctoringConnectionData = this.pageService
                .getRestService()
                .getBuilder(GetProctorRoomConnection.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(proctoringSettings.examId))
                .withQueryParam(ProctoringRoomConnection.ATTR_ROOM_NAME, room.name)
                .withQueryParam(ProctoringRoomConnection.ATTR_SUBJECT, Utils.encodeFormURL_UTF_8(room.subject))
                .call()
                .getOrThrow();

        ProctoringGUIService.setCurrentProctoringWindowData(
                String.valueOf(proctoringSettings.examId),
                proctoringConnectionData);

        final String zoomStartLink = extractZoomStartLink(room.additionalRoomData);
        if (proctoringSettings.useZoomAppClientForCollectingRoom &&
                StringUtils.isNotBlank(zoomStartLink)) {

            final String script = String.format(
                    getOpenRoomScriptTemplate(),
                    room.name,
                    800,
                    1200,
                    room.name,
                    zoomStartLink,
                    "");

            RWT.getClient()
                    .getService(JavaScriptExecutor.class)
                    .execute(script);

        } else {

            final String script = String.format(
                    getOpenRoomScriptTemplate(),
                    room.name,
                    800,
                    1200,
                    room.name,
                    this.guiServiceInfo.getExternalServerURIBuilder().toUriString(),
                    this.remoteProctoringEndpoint);

            RWT.getClient()
                    .getService(JavaScriptExecutor.class)
                    .execute(script);

            final boolean newWindow = this.pageService.getCurrentUser()
                    .getProctoringGUIService()
                    .registerProctoringWindow(String.valueOf(room.examId), room.name, room.name);

            if (newWindow) {
                this.pageService.getRestService()
                        .getBuilder(NotifyProctoringRoomOpened.class)
                        .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(proctoringSettings.examId))
                        .withQueryParam(ProctoringRoomConnection.ATTR_ROOM_NAME, room.name)
                        .call()
                        .onError(error -> log.error("Failed to notify proctoring room opened: ", error));
            }
        }

        return action;
    }

    public PageAction openOneToOneRoom(
            final PageAction action,
            final ClientConnectionData connectionData,
            final ProctoringGUIService proctoringGUIService) {

        try {

            final String connectionToken = connectionData.clientConnection.connectionToken;
            final String examId = action.getEntityKey().modelId;

            if (!proctoringGUIService.hasWindow(connectionToken)) {
                final ProctoringRoomConnection proctoringConnectionData = proctoringGUIService
                        .openBreakOutRoom(
                                examId,
                                connectionToken,
                                connectionData.clientConnection.userSessionId,
                                Arrays.asList(connectionToken))
                        .onError(error -> log.error(
                                "Failed to open single proctoring room for connection {} {}",
                                connectionToken,
                                error.getMessage()))
                        .getOr(null);

                ProctoringGUIService.setCurrentProctoringWindowData(
                        examId,
                        connectionToken,
                        proctoringConnectionData);
            }

            final JavaScriptExecutor javaScriptExecutor = RWT.getClient().getService(JavaScriptExecutor.class);
            final String script = String.format(
                    getOpenRoomScriptTemplate(),
                    connectionData.clientConnection.userSessionId,
                    800,
                    1200,
                    connectionData.clientConnection.userSessionId,
                    this.guiServiceInfo.getExternalServerURIBuilder().toUriString(),
                    this.remoteProctoringEndpoint);
            javaScriptExecutor.execute(script);

        } catch (final Exception e) {
            log.error("Failed to open popup for one to one room: ", e);
            action.pageContext().notifyError(OPEN_ONE_ERROR, e);
        }

        return action;
    }

    private boolean openTownhallRoom(
            final ProctoringGUIService proctoringGUIService,
            final ProctoringServiceSettings proctoringSettings,
            final PageAction action) {

        try {
            final EntityKey examId = action.getEntityKey();

            final String endpoint = this.remoteProctoringEndpoint;
            final String joinURL = this.guiServiceInfo.getExternalServerURIBuilder().toUriString();
            if (proctoringGUIService.getTownhallWindowName(examId.modelId) == null) {
                final ProctoringRoomConnection proctoringConnectionData = proctoringGUIService
                        .openTownhallRoom(
                                examId.modelId,
                                this.pageService.getI18nSupport().getText(EXAM_ROOM_NAME))
                        .onError(error -> log.error(
                                "Failed to open all collecting room for exam {} {}", examId.modelId,
                                error.getMessage()))
                        .getOrThrow();
                ProctoringGUIService.setCurrentProctoringWindowData(
                        examId.modelId,
                        proctoringConnectionData.roomName,
                        proctoringConnectionData);
            }

            final String windowName = proctoringGUIService.getTownhallWindowName(examId.modelId);
            final JavaScriptExecutor javaScriptExecutor = RWT.getClient().getService(JavaScriptExecutor.class);
            final String script = String.format(
                    getOpenRoomScriptTemplate(),
                    windowName,
                    800,
                    1200,
                    "Town-Hall",
                    joinURL,
                    endpoint);
            javaScriptExecutor.execute(script);

        } catch (final Exception e) {
            log.error("Failed to open popup for town-hall room: ", e);
            return false;
        }

        return true;
    }

    private boolean closeTownhallRoom(
            final ProctoringGUIService proctoringGUIService,
            final PageAction action) {

        final String examId = action.getEntityKey().modelId;
        try {

            this.pageService
                    .getCurrentUser()
                    .getProctoringGUIService()
                    .closeRoomWindow(proctoringGUIService.getTownhallWindowName(examId));

        } catch (final Exception e) {
            log.error("Failed to close proctoring town-hall room for exam: {}", examId);
            return false;
        }

        return true;
    }

    private void updateTownhallButton(
            final ProctoringGUIService proctoringGUIService,
            final PageContext pageContext) {
        final EntityKey entityKey = pageContext.getEntityKey();

        if (isTownhallRoomActive(entityKey.modelId)) {
            final boolean townhallRoomFromThisUser = proctoringGUIService
                    .getTownhallWindowName(entityKey.modelId) != null;
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
                                new Tuple<>(
                                        ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                                        ActionDefinition.MONITOR_EXAM_CLOSE_TOWNHALL_PROCTOR_ROOM)),
                        pageContext);
            }
        } else {
            this.pageService.firePageEvent(
                    new ActionActivationEvent(
                            proctoringGUIService.getNumberOfProctoringParticipants() > 0,
                            new Tuple<>(
                                    ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                                    ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM)),
                    pageContext);
        }
    }

    private void processProctorRoomActionActivation(
            final TreeItem treeItem,
            final RemoteProctoringRoom room,
            final PageContext pageContext) {

        try {

            final boolean active = room.roomSize > 0 /* && !room.isOpen SEBSERV-236 */;
            final Display display = pageContext.getRoot().getDisplay();
            final PageAction action = (PageAction) treeItem.getData(ActionPane.ACTION_EVENT_CALL_KEY);
            final Image image = active
                    ? action.definition.icon.getImage(display)
                    : action.definition.icon.getGreyedImage(display);
            treeItem.setImage(image);
            treeItem.setForeground(active ? null : new Color(display, Constants.GREY_DISABLED));

        } catch (final Exception e) {
            log.warn("Failed to set Proctor-Room-Activation: ", e.getMessage());
        }
    }

    private String getOpenRoomScriptTemplate() {
        try {
            return IOUtils.toString(this.openRoomScriptRes.getInputStream());
        } catch (final Exception e) {
            log.error("Failed to load open proctoring room script template", e);
            return "ERROR: " + e.getLocalizedMessage();
        }
    }

    private String extractZoomStartLink(final String additioalRoomAttributesJson) {
        try {
            final Map<String, String> data =
                    this.jsonMapper.readValue(
                            additioalRoomAttributesJson,
                            new TypeReference<Map<String, String>>() {
                            });
            return data.get("start_url");
        } catch (final Exception e) {
            log.error("Failed to extract Zoom start link: ", e);
            return null;
        }
    }

}
