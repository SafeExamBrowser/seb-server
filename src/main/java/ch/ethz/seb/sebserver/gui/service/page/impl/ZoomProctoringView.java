/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
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
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringFeature;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.GuiServiceInfo;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService.ProctoringWindowData;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Component
@GuiProfile
public class ZoomProctoringView extends AbstractProctoringView {

    private static final Logger log = LoggerFactory.getLogger(ZoomProctoringView.class);

    public ZoomProctoringView(
            final PageService pageService,
            final GuiServiceInfo guiServiceInfo,
            @Value("${sebserver.gui.remote.proctoring.entrypoint:/remote-proctoring}") final String remoteProctoringEndpoint,
            @Value("${sebserver.gui.remote.proctoring.api-servler.endpoint:/remote-view-servlet}") final String remoteProctoringViewServletEndpoint) {

        super(pageService, guiServiceInfo, remoteProctoringEndpoint, remoteProctoringViewServletEndpoint);
    }

    @Override
    public ProctoringServerType serverType() {
        return ProctoringServerType.ZOOM;
    }

    @Override
    public void compose(final PageContext pageContext) {

        final ProctoringWindowData proctoringWindowData = ProctoringGUIService.getCurrentProctoringWindowData();
        final Composite parent = pageContext.getParent();
        final Composite content = new Composite(parent, SWT.NONE | SWT.NO_SCROLL);
        final GridLayout gridLayout = new GridLayout();
        final ProctoringGUIService proctoringGUIService = this.pageService
                .getCurrentUser()
                .getProctoringGUIService();
        final ProctoringServiceSettings proctoringSettings = this.pageService
                .getRestService()
                .getBuilder(GetExamProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, proctoringWindowData.examId)
                .call()
                .onError(error -> log.error("Failed to get ProctoringServiceSettings", error))
                .getOr(null);

        content.setLayout(gridLayout);
        final GridData headerCell = new GridData(SWT.FILL, SWT.FILL, true, true);
        content.setLayoutData(headerCell);

        final Label title = this.pageService
                .getWidgetFactory()
                .label(content, proctoringWindowData.connectionData.subject);
        title.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));

        parent.addListener(SWT.Dispose, event -> closeRoom(proctoringGUIService, proctoringWindowData));

        final String url = this.guiServiceInfo
                .getExternalServerURIBuilder()
                .toUriString()
                + this.remoteProctoringEndpoint
                + this.remoteProctoringViewServletEndpoint
                + Constants.SLASH;

        if (log.isDebugEnabled()) {
            log.debug("Open proctoring Servlet in IFrame with URL: {}", url);
        }

        final Browser browser = new Browser(content, SWT.NONE | SWT.NO_SCROLL);
        browser.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        browser.setLayoutData(gridData);
        browser.setUrl(url);
        browser.setBackground(new Color(parent.getDisplay(), 100, 100, 100));

        final Composite footer = new Composite(content, SWT.NONE | SWT.NO_SCROLL);
        footer.setLayout(new RowLayout());
        final GridData footerLayout = new GridData(SWT.CENTER, SWT.BOTTOM, true, false);
        footer.setLayoutData(footerLayout);

        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();

        final Button closeAction = widgetFactory.buttonLocalized(footer, CLOSE_WINDOW_TEXT_KEY);
        closeAction.setLayoutData(new RowData());
        closeAction.addListener(SWT.Selection, event -> closeRoom(proctoringGUIService, proctoringWindowData));

        final BroadcastActionState broadcastActionState = new BroadcastActionState();
        if (proctoringSettings.enabledFeatures.contains(ProctoringFeature.BROADCAST)) {
//            final Button broadcastAudioAction = widgetFactory.buttonLocalized(footer, BROADCAST_AUDIO_ON_TEXT_KEY);
//            broadcastAudioAction.setLayoutData(new RowData());
//            broadcastAudioAction.addListener(SWT.Selection, event -> toggleBroadcastAudio(
//                    proctoringWindowData.examId,
//                    proctoringWindowData.connectionData.roomName,
//                    broadcastAudioAction));
//            broadcastAudioAction.setData(BroadcastActionState.KEY_NAME, broadcastActionState);

            final Button broadcastVideoAction = widgetFactory.buttonLocalized(footer, BROADCAST_VIDEO_ON_TEXT_KEY);
            broadcastVideoAction.setLayoutData(new RowData());
            broadcastVideoAction.addListener(SWT.Selection, event -> toggleBroadcastVideo(
                    proctoringWindowData.examId,
                    proctoringWindowData.connectionData.roomName,
                    broadcastVideoAction,
                    null));
            broadcastVideoAction.setData(BroadcastActionState.KEY_NAME, broadcastActionState);
        }
        if (proctoringSettings.enabledFeatures.contains(ProctoringFeature.ENABLE_CHAT)) {
            final Button chatAction = widgetFactory.buttonLocalized(footer, CHAT_ON_TEXT_KEY);
            chatAction.setLayoutData(new RowData());
            chatAction.addListener(SWT.Selection, event -> toggleChat(
                    proctoringWindowData.examId,
                    proctoringWindowData.connectionData.roomName,
                    chatAction));
            chatAction.setData(BroadcastActionState.KEY_NAME, broadcastActionState);
        }
    }

}
