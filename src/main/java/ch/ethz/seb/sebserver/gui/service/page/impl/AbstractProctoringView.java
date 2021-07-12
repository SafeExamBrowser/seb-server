/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import org.eclipse.swt.widgets.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gui.GuiServiceInfo;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.RemoteProctoringView;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.SendProctoringReconfigurationAttributes;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService.ProctoringWindowData;

public abstract class AbstractProctoringView implements RemoteProctoringView {

    private static final Logger log = LoggerFactory.getLogger(AbstractProctoringView.class);

    protected final PageService pageService;
    protected final GuiServiceInfo guiServiceInfo;
    protected final String remoteProctoringEndpoint;
    protected final String remoteProctoringViewServletEndpoint;

    protected AbstractProctoringView(
            final PageService pageService,
            final GuiServiceInfo guiServiceInfo,
            final String remoteProctoringEndpoint,
            final String remoteProctoringViewServletEndpoint) {

        this.pageService = pageService;
        this.guiServiceInfo = guiServiceInfo;
        this.remoteProctoringEndpoint = remoteProctoringEndpoint;
        this.remoteProctoringViewServletEndpoint = remoteProctoringViewServletEndpoint;
    }

    protected void sendReconfigurationAttributes(
            final String examId,
            final String roomName,
            final BroadcastActionState state) {

        this.pageService.getRestService().getBuilder(SendProctoringReconfigurationAttributes.class)
                .withURIVariable(API.PARAM_MODEL_ID, examId)
                .withFormParam(Domain.REMOTE_PROCTORING_ROOM.ATTR_ID, roomName)
                .withFormParam(

                        API.EXAM_PROCTORING_ATTR_RECEIVE_AUDIO,
                        state.audio ? Constants.TRUE_STRING : Constants.FALSE_STRING)
                .withFormParam(
                        API.EXAM_PROCTORING_ATTR_RECEIVE_VIDEO,
                        state.video ? Constants.TRUE_STRING : Constants.FALSE_STRING)
                .withFormParam(
                        API.EXAM_PROCTORING_ATTR_ALLOW_CHAT,
                        state.chat ? Constants.TRUE_STRING : Constants.FALSE_STRING)
                .call()
                .onError(error -> log.error("Failed to send broadcast attributes to clients in room: {} cause: {}",
                        roomName,
                        error.getMessage()));

    }

    protected void toggleBroadcastAudio(
            final String examId,
            final String roomName,
            final Button broadcastAction) {

        final BroadcastActionState state =
                (BroadcastActionState) broadcastAction.getData(BroadcastActionState.KEY_NAME);

        this.pageService.getPolyglotPageService().injectI18n(
                broadcastAction,
                state.audio ? BROADCAST_AUDIO_ON_TEXT_KEY : BROADCAST_AUDIO_OFF_TEXT_KEY);

        state.audio = !state.audio;
        sendReconfigurationAttributes(examId, roomName, state);
    }

    protected void toggleBroadcastVideo(
            final String examId,
            final String roomName,
            final Button videoAction,
            final Button audioAction) {

        final BroadcastActionState state =
                (BroadcastActionState) videoAction.getData(BroadcastActionState.KEY_NAME);

        if (audioAction != null) {
            this.pageService.getPolyglotPageService().injectI18n(
                    audioAction,
                    state.video ? BROADCAST_AUDIO_ON_TEXT_KEY : BROADCAST_AUDIO_OFF_TEXT_KEY);
        }
        if (videoAction != null) {
            this.pageService.getPolyglotPageService().injectI18n(
                    videoAction,
                    state.video ? BROADCAST_VIDEO_ON_TEXT_KEY : BROADCAST_VIDEO_OFF_TEXT_KEY);
        }

        state.video = !state.video;
        state.audio = state.video;
        sendReconfigurationAttributes(examId, roomName, state);
    }

    protected void toggleChat(
            final String examId,
            final String roomName,
            final Button broadcastAction) {

        final BroadcastActionState state =
                (BroadcastActionState) broadcastAction.getData(BroadcastActionState.KEY_NAME);

        this.pageService.getPolyglotPageService().injectI18n(
                broadcastAction,
                state.chat ? CHAT_ON_TEXT_KEY : CHAT_OFF_TEXT_KEY);

        state.chat = !state.chat;
        sendReconfigurationAttributes(examId, roomName, state);
    }

    protected void closeRoom(
            final ProctoringGUIService proctoringGUIService,
            final ProctoringWindowData proctoringWindowData) {

        try {
            proctoringGUIService.closeRoomWindow(proctoringWindowData.windowName);
        } catch (final Exception e) {
            log.error("Failed to close proctoring window properly: ", e);
        }
    }

    static final class BroadcastActionState {
        public static final String KEY_NAME = "BroadcastActionState";
        boolean audio = false;
        boolean video = false;
        boolean chat = false;
    }

}
