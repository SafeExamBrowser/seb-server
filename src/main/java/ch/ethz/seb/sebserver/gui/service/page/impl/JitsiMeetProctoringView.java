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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnectionData;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.GuiServiceInfo;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.RemoteProctoringView;
import ch.ethz.seb.sebserver.gui.service.session.ProctoringGUIService;

@Component
@GuiProfile
public class JitsiMeetProctoringView implements RemoteProctoringView {

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
        final Composite parent = pageContext.getParent();

        parent.addListener(SWT.Dispose, event -> {
            final ProctoringGUIService proctoringGUIService = this.pageService
                    .getCurrentUser()
                    .getProctoringGUIService();
            final SEBProctoringConnectionData currentProctoringData = ProctoringGUIService
                    .getCurrentProctoringData();
            proctoringGUIService.closeRoom(currentProctoringData.roomName);
        });

        final String url = this.guiServiceInfo.getExternalServerURIBuilder().toUriString()
                + this.remoteProctoringEndpoint + this.remoteProctoringViewServletEndpoint + "/";
        final Browser browser = new Browser(parent, SWT.NONE);
        browser.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        browser.setLayoutData(gridData);
        browser.setUrl(url);
        browser.layout();

    }

    @Override
    public ProctoringServerType serverType() {
        return ProctoringServerType.JITSI_MEET;
    }

}
