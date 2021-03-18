/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session.proctoring;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.util.buf.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.CloseProctoringRoom;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.OpenBreakOutRoom;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.OpenTownhallRoom;

public class ProctoringGUIService {

    private static final Logger log = LoggerFactory.getLogger(ProctoringGUIService.class);

    public static final String SESSION_ATTR_PROCTORING_DATA = "SESSION_ATTR_PROCTORING_DATA";
    private static final String CLOSE_ROOM_SCRIPT = "var existingWin = window.open('', '%s'); existingWin.close()";

    private final RestService restService;

    final Map<String, RoomData> openWindows = new HashMap<>();

    public ProctoringGUIService(final RestService restService) {
        this.restService = restService;
    }

    public void registerProctoringWindow(
            final String examId,
            final String windowName,
            final String roomName) {

        this.openWindows.put(windowName, new RoomData(roomName, examId));
    }

    public static ProctoringWindowData getCurrentProctoringWindowData() {
        return (ProctoringWindowData) RWT.getUISession()
                .getHttpSession()
                .getAttribute(SESSION_ATTR_PROCTORING_DATA);
    }

    public static void setCurrentProctoringWindowData(
            final String examId,
            final ProctoringRoomConnection data) {
        setCurrentProctoringWindowData(examId, data.roomName, data);
    }

    public static void setCurrentProctoringWindowData(
            final String examId,
            final String windowName,
            final ProctoringRoomConnection data) {

        RWT.getUISession().getHttpSession().setAttribute(
                SESSION_ATTR_PROCTORING_DATA,
                new ProctoringWindowData(examId, windowName, data));
    }

    public boolean hasWindow(final String windowName) {
        return this.openWindows.containsKey(windowName);
    }

    public Result<ProctoringRoomConnection> openBreakOutRoom(
            final String examId,
            final String windowName,
            final String subject,
            final Collection<String> connectionTokens) {

        return this.restService.getBuilder(OpenBreakOutRoom.class)
                .withURIVariable(API.PARAM_MODEL_ID, examId)
                .withFormParam(ProctoringRoomConnection.ATTR_SUBJECT, subject)
                .withFormParam(
                        API.EXAM_API_SEB_CONNECTION_TOKEN,
                        StringUtils.join(connectionTokens, Constants.LIST_SEPARATOR_CHAR))
                .call()
                .map(connection -> {
                    this.registerProctoringWindow(examId, windowName, connection.roomName);
                    return connection;
                });
    }

    public Result<ProctoringRoomConnection> openTownhallRoom(
            final String examId,
            final String windowName,
            final String subject) {

        return this.restService.getBuilder(OpenTownhallRoom.class)
                .withURIVariable(API.PARAM_MODEL_ID, examId)
                .withFormParam(ProctoringRoomConnection.ATTR_SUBJECT, subject)
                .call()
                .map(connection -> {
                    this.openWindows.put(windowName, new RoomData(connection.roomName, examId));
                    return connection;
                });
    }

    public void closeRoomWindow(final String windowName) {
        final RoomData roomData = this.openWindows.get(windowName);
        if (roomData != null) {
            this.restService.getBuilder(CloseProctoringRoom.class)
                    .withURIVariable(API.PARAM_MODEL_ID, roomData.examId)
                    .withFormParam(ProctoringRoomConnection.ATTR_ROOM_NAME, roomData.roomName)
                    .call()
                    .onError(error -> log.error("Failed to close break-out room: {} {}",
                            roomData.roomName,
                            error.getMessage()));
            closeWindow(windowName);
        } else {
            log.warn("No proctoring room window with name: {} found for closing.", windowName);
        }
    }

    public void clear() {
        if (!this.openWindows.isEmpty()) {
            this.openWindows
                    .entrySet()
                    .stream()
                    .forEach(entry -> closeRoomWindow(entry.getKey()));

            this.openWindows.clear();
        }
    }

    private void closeWindow(final String windowName) {
        try {
            this.openWindows.remove(windowName);
            RWT.getClient()
                    .getService(JavaScriptExecutor.class)
                    .execute(String.format(CLOSE_ROOM_SCRIPT, windowName));

        } catch (final Exception e) {
            log.info("Failed to close opened proctoring window: {}", windowName);
        }
    }

    private static final class RoomData {
        final String roomName;
        final String examId;

        public RoomData(final String roomName, final String examId) {
            this.roomName = roomName;
            this.examId = examId;
        }
    }

    public static class ProctoringWindowData {
        public final String windowName;
        public final String examId;
        public final ProctoringRoomConnection connectionData;

        protected ProctoringWindowData(
                final String examId,
                final String windowName,
                final ProctoringRoomConnection connectionData) {
            this.windowName = windowName;
            this.examId = examId;
            this.connectionData = connectionData;
        }
    }

}
