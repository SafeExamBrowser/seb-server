/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.GuiServiceInfo;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.RemoteProctoringView;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.SendProctoringBroadcastOffInstruction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.SendProctoringBroadcastOnInstruction;
import ch.ethz.seb.sebserver.gui.service.session.ProctoringGUIService;
import ch.ethz.seb.sebserver.gui.service.session.ProctoringGUIService.ProctoringWindowData;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Component
@GuiProfile
public class JitsiMeetProctoringView implements RemoteProctoringView {

    private static final LocTextKey CLOSE_WINDOW_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.close");
    private static final LocTextKey BROADCAST_AUDIO_ON_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcaston.audio");
    private static final LocTextKey BROADCAST_AUDIO_OFF_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcastoff.audio");
    private static final LocTextKey BROADCAST_VIDEO_ON_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcaston.video");
    private static final LocTextKey BROADCAST_VIDEO_OFF_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcastoff.video");
    private static final LocTextKey CHAT_ON_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcaston.chat");
    private static final LocTextKey CHAT_OFF_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.action.broadcastoff.chat");

    private final PageService pageService;
    private final GuiServiceInfo guiServiceInfo;
    private final String remoteProctoringEndpoint;
    private final String remoteProctoringViewServletEndpoint;

    public JitsiMeetProctoringView(
            final PageService pageService,
            final GuiServiceInfo guiServiceInfo,
            @Value("${sebserver.gui.remote.proctoring.entrypoint:/remote-proctoring}") final String remoteProctoringEndpoint,
            @Value("${sebserver.gui.remote.proctoring.api-servler.endpoint:/remote-view-servlet}") final String remoteProctoringViewServletEndpoint) {

        this.pageService = pageService;
        this.guiServiceInfo = guiServiceInfo;
        this.remoteProctoringEndpoint = remoteProctoringEndpoint;
        this.remoteProctoringViewServletEndpoint = remoteProctoringViewServletEndpoint;
    }

    @Override
    public void compose(final PageContext pageContext) {

        final ProctoringWindowData proctoringWindowData = ProctoringGUIService.getCurrentProctoringWindowData();

        final Composite parent = pageContext.getParent();

        final Composite content = new Composite(parent, SWT.NONE | SWT.NO_SCROLL);
        final GridLayout gridLayout = new GridLayout();

        content.setLayout(gridLayout);
        final GridData headerCell = new GridData(SWT.FILL, SWT.FILL, true, true);
        content.setLayoutData(headerCell);

        parent.addListener(SWT.Dispose, event -> this.pageService
                .getCurrentUser()
                .getProctoringGUIService()
                .closeRoom(proctoringWindowData.connectionData.roomName));

        final String url = this.guiServiceInfo.getExternalServerURIBuilder().toUriString()
                + this.remoteProctoringEndpoint + this.remoteProctoringViewServletEndpoint + "/";
        final Browser browser = new Browser(content, SWT.NONE | SWT.NO_SCROLL);
        browser.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        browser.setLayoutData(gridData);
        browser.setUrl(url);
        //browser.layout();
        browser.setBackground(new Color(parent.getDisplay(), 100, 100, 100));

        final Composite footer = new Composite(content, SWT.NONE | SWT.NO_SCROLL);
        footer.setLayout(new RowLayout());
        final GridData footerLayout = new GridData(SWT.CENTER, SWT.BOTTOM, true, false);
        footerLayout.heightHint = 40;
        footer.setLayoutData(footerLayout);

        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();

        final Button closeAction = widgetFactory.buttonLocalized(footer, CLOSE_WINDOW_TEXT_KEY);
        closeAction.setLayoutData(new RowData(150, 30));
        closeAction.addListener(SWT.Selection, event -> this.pageService
                .getCurrentUser()
                .getProctoringGUIService()
                .closeRoom(proctoringWindowData.connectionData.roomName));

        final BroadcastActionState broadcastActionState = new BroadcastActionState();

        final Button broadcastAudioAction = widgetFactory.buttonLocalized(footer, BROADCAST_AUDIO_ON_TEXT_KEY);
        broadcastAudioAction.setLayoutData(new RowData(150, 30));
        broadcastAudioAction.addListener(SWT.Selection, event -> toggleBroadcastAudio(
                proctoringWindowData.examId,
                proctoringWindowData.connectionData.roomName,
                broadcastAudioAction));
        broadcastAudioAction.setData(BroadcastActionState.KEY_NAME, broadcastActionState);

        final Button broadcastVideoAction = widgetFactory.buttonLocalized(footer, BROADCAST_VIDEO_ON_TEXT_KEY);
        broadcastVideoAction.setLayoutData(new RowData(150, 30));
        broadcastVideoAction.addListener(SWT.Selection, event -> toggleBroadcastVideo(
                proctoringWindowData.examId,
                proctoringWindowData.connectionData.roomName,
                broadcastVideoAction,
                broadcastAudioAction));
        broadcastVideoAction.setData(BroadcastActionState.KEY_NAME, broadcastActionState);

        final Button chatAction = widgetFactory.buttonLocalized(footer, CHAT_ON_TEXT_KEY);
        chatAction.setLayoutData(new RowData(150, 30));
        chatAction.addListener(SWT.Selection, event -> toggleChat(
                proctoringWindowData.examId,
                proctoringWindowData.connectionData.roomName,
                chatAction));
        chatAction.setData(BroadcastActionState.KEY_NAME, broadcastActionState);

    }

    private void toggleBroadcastAudio(final String examId, final String roomName, final Button broadcastAction) {
        final BroadcastActionState state =
                (BroadcastActionState) broadcastAction.getData(BroadcastActionState.KEY_NAME);
        if (state.audio) {
            this.pageService.getPolyglotPageService().injectI18n(broadcastAction, BROADCAST_AUDIO_ON_TEXT_KEY);
            this.pageService.getRestService().getBuilder(SendProctoringBroadcastOffInstruction.class)
                    .withURIVariable(API.PARAM_MODEL_ID, examId)
                    .withFormParam(Domain.REMOTE_PROCTORING_ROOM.ATTR_ID, roomName)
                    .withFormParam(
                            ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_RECEIVE_AUDIO,
                            Constants.TRUE_STRING)
                    .call()
                    .getOrThrow();
        } else {
            this.pageService.getPolyglotPageService().injectI18n(broadcastAction, BROADCAST_AUDIO_OFF_TEXT_KEY);
            this.pageService.getRestService().getBuilder(SendProctoringBroadcastOnInstruction.class)
                    .withURIVariable(API.PARAM_MODEL_ID, examId)
                    .withFormParam(Domain.REMOTE_PROCTORING_ROOM.ATTR_ID, roomName)
                    .withFormParam(
                            ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_RECEIVE_AUDIO,
                            Constants.TRUE_STRING)
                    .call()
                    .getOrThrow();
        }
        state.audio = !state.audio;
    }

    private void toggleBroadcastVideo(
            final String examId,
            final String roomName,
            final Button videoAction,
            final Button audioAction) {
        final BroadcastActionState state =
                (BroadcastActionState) videoAction.getData(BroadcastActionState.KEY_NAME);
        if (state.video) {
            this.pageService.getPolyglotPageService().injectI18n(audioAction, BROADCAST_AUDIO_ON_TEXT_KEY);
            this.pageService.getPolyglotPageService().injectI18n(videoAction, BROADCAST_VIDEO_ON_TEXT_KEY);

            this.pageService.getRestService().getBuilder(SendProctoringBroadcastOffInstruction.class)
                    .withURIVariable(API.PARAM_MODEL_ID, examId)
                    .withFormParam(Domain.REMOTE_PROCTORING_ROOM.ATTR_ID, roomName)
                    .withFormParam(
                            ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_RECEIVE_AUDIO,
                            Constants.TRUE_STRING)
                    .withFormParam(
                            ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_RECEIVE_VIDEO,
                            Constants.TRUE_STRING)
                    .call()
                    .getOrThrow();
        } else {
            this.pageService.getPolyglotPageService().injectI18n(audioAction, BROADCAST_AUDIO_OFF_TEXT_KEY);
            this.pageService.getPolyglotPageService().injectI18n(videoAction, BROADCAST_VIDEO_OFF_TEXT_KEY);
            this.pageService.getRestService().getBuilder(SendProctoringBroadcastOnInstruction.class)
                    .withURIVariable(API.PARAM_MODEL_ID, examId)
                    .withFormParam(Domain.REMOTE_PROCTORING_ROOM.ATTR_ID, roomName)
                    .withFormParam(
                            ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_RECEIVE_AUDIO,
                            Constants.TRUE_STRING)
                    .withFormParam(
                            ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_RECEIVE_VIDEO,
                            Constants.TRUE_STRING)
                    .call()
                    .getOrThrow();
        }
        state.video = !state.video;
        state.audio = state.video;
    }

    private void toggleChat(final String examId, final String roomName, final Button broadcastAction) {
        final BroadcastActionState state =
                (BroadcastActionState) broadcastAction.getData(BroadcastActionState.KEY_NAME);
        if (state.chat) {
            this.pageService.getPolyglotPageService().injectI18n(broadcastAction, CHAT_ON_TEXT_KEY);
            this.pageService.getRestService().getBuilder(SendProctoringBroadcastOffInstruction.class)
                    .withURIVariable(API.PARAM_MODEL_ID, examId)
                    .withFormParam(Domain.REMOTE_PROCTORING_ROOM.ATTR_ID, roomName)
                    .withFormParam(
                            ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_ALLOW_CHAT,
                            Constants.TRUE_STRING)
                    .call()
                    .getOrThrow();
        } else {
            this.pageService.getPolyglotPageService().injectI18n(broadcastAction, CHAT_OFF_TEXT_KEY);
            this.pageService.getRestService().getBuilder(SendProctoringBroadcastOnInstruction.class)
                    .withURIVariable(API.PARAM_MODEL_ID, examId)
                    .withFormParam(Domain.REMOTE_PROCTORING_ROOM.ATTR_ID, roomName)
                    .withFormParam(
                            ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_RECONFIGURE_SETTINGS.JITSI_ALLOW_CHAT,
                            Constants.TRUE_STRING)
                    .call()
                    .getOrThrow();
        }
        state.chat = !state.chat;
    }

    @Override
    public ProctoringServerType serverType() {
        return ProctoringServerType.JITSI_MEET;
    }

    private static class BroadcastActionState {
        public static final String KEY_NAME = "BroadcastActionState";
        boolean audio = false;
        boolean video = false;
        boolean chat = false;
    }

}
