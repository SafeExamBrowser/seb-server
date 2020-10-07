/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.tomcat.util.buf.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnectionData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.SEBClientsJoinProctorRoom;

public class ProctoringGUIService {

    private static final Logger log = LoggerFactory.getLogger(ProctoringGUIService.class);

    private static final String CLOSE_ROOM_SCRIPT = "var existingWin = window.open('', '%s'); existingWin.close()";

    private final AtomicInteger counter = new AtomicInteger(1);
    private final RestService restService;
    final Map<String, RoomConnectionData> rooms = new HashMap<>();

    final Set<String> openWindows = new HashSet<>();

    public ProctoringGUIService(final RestService restService) {
        this.restService = restService;
    }

    public String createNewRoomName() {
        return "Room-" + this.counter.getAndIncrement();
    }

    public void registerProctoringWindow(final String window) {
        this.openWindows.add(window);
    }

    public Set<String> roomNames() {
        return this.rooms.keySet();
    }

    public void registerNewProcotringRoom(
            final String examId,
            final String roomName,
            final Collection<String> connectionTokens) {

        final List<SEBProctoringConnectionData> connections =
                this.restService.getBuilder(SEBClientsJoinProctorRoom.class)
                        .withURIVariable(API.PARAM_MODEL_ID, examId)
                        .withFormParam(SEBProctoringConnectionData.ATTR_ROOM_NAME, roomName)
                        .withFormParam(
                                API.EXAM_API_SEB_CONNECTION_TOKEN,
                                StringUtils.join(connectionTokens, Constants.LIST_SEPARATOR_CHAR))
                        .call()
                        .getOrThrow();

        this.rooms.put(roomName, new RoomConnectionData(roomName, examId, connections));
        this.openWindows.add(roomName);

    }

    public void addConnectionsToRoom(
            final String examId,
            final String room,
            final Collection<String> connectionTokens) {

        if (this.rooms.containsKey(room)) {
            final RoomConnectionData roomConnectionData = this.rooms.get(room);
            if (!roomConnectionData.examId.equals(examId) || !roomConnectionData.roomName.equals(room)) {
                throw new IllegalArgumentException("Exam identifier mismatch");
            }
            final List<SEBProctoringConnectionData> newConnections =
                    this.restService.getBuilder(SEBClientsJoinProctorRoom.class)
                            .withURIVariable(API.PARAM_MODEL_ID, examId)
                            .withFormParam(SEBProctoringConnectionData.ATTR_ROOM_NAME, room)
                            .withFormParam(
                                    API.EXAM_API_SEB_CONNECTION_TOKEN,
                                    StringUtils.join(connectionTokens, Constants.LIST_SEPARATOR_CHAR))
                            .call()
                            .getOrThrow();
            roomConnectionData.connections.addAll(newConnections);
        }
    }

    public void removeConnectionsFromRoom(
            final String examId,
            final String room,
            final Collection<String> connectionTokens) {

    }

    public Result<List<SEBProctoringConnectionData>> closeRoom(final String name) {
        return Result.tryCatch(() -> {
            closeWindow(name);
            final RoomConnectionData roomConnectionData = this.rooms.remove(name);
            if (roomConnectionData != null) {
                // first send instruction to leave this room and join the personal room
                final String connectionsString = StringUtils.join(
                        roomConnectionData.connections
                                .stream()
                                .map(c -> c.connectionToken)
                                .collect(Collectors.toList()),
                        Constants.LIST_SEPARATOR_CHAR);

// NOTE: uncomment this if we need to send first a LEAVE instruction before sending the JOIN instruction for the single room
//                this.restService.getBuilder(LeaveProctorRoom.class)
//                        .withURIVariable(API.PARAM_MODEL_ID, roomConnectionData.examId)
//                        .withFormParam(SEBProctoringConnectionData.ATTR_ROOM_NAME, name)
//                        .withFormParam(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionsString)
//                        .call()
//                        .getOrThrow();

                return this.restService.getBuilder(SEBClientsJoinProctorRoom.class)
                        .withURIVariable(API.PARAM_MODEL_ID, roomConnectionData.examId)
                        .withFormParam(SEBProctoringConnectionData.ATTR_ROOM_NAME, name)
                        .withFormParam(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionsString)
                        .call()
                        .getOrThrow();

            }

            return Collections.emptyList();
        });
    }

    public void clear() {

        if (!this.rooms.isEmpty()) {
            this.rooms.keySet().stream().forEach(this::closeRoom);
            this.rooms.clear();
        }

        if (!this.openWindows.isEmpty()) {

            this.openWindows.stream()
                    .forEach(room -> closeWindow(room));
            this.openWindows.clear();
        }

    }

    private void closeWindow(final String room) {
        try {
            RWT.getClient().getService(JavaScriptExecutor.class)
                    .execute(String.format(CLOSE_ROOM_SCRIPT, room));
        } catch (final Exception e) {
            log.info("Failed to close opened proctoring window: {}", room);
        }
    }

    private static final class RoomConnectionData {
        final String roomName;
        final String examId;
        final Collection<SEBProctoringConnectionData> connections;

        protected RoomConnectionData(
                final String roomName,
                final String examId,
                final Collection<SEBProctoringConnectionData> connections) {

            this.roomName = roomName;
            this.examId = examId;
            this.connections = connections != null ? new ArrayList<>(connections) : new ArrayList<>();
        }

    }

}
