/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session.proctoring;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService.ProctoringWindowData;

@Lazy
@Service
@GuiProfile
public class ZoomWindowScriptResolver implements ProctoringWindowScriptResolver {

    private static final Logger log = LoggerFactory.getLogger(ZoomWindowScriptResolver.class);

    static final String RES_PATH =
            "classpath:ch/ethz/seb/sebserver/gui/service/session/proctoring/zoomWindow.html";

    private static final String ATTR_SUBJECT = "ATTR_SUBJECT";
    private static final String ATTR_API_KEY = "ATTR_API_KEY";
    private static final String ATTR_ACCESS_TOKEN = "ATTR_ACCESS_TOKEN";
    private static final String ATTR_ROOM_KEY = "ATTR_ROOM_KEY";
    private static final String ATTR_ROOM_NAME = "ATTR_ROOM_NAME";
    private static final String ATTR_HOST = "ATTR_HOST";
    private static final String ATTR_USER_NAME = "ATTR_USER_NAME";

    private final Resource resourceFile;

    public ZoomWindowScriptResolver(
            @Value(RES_PATH) final Resource resourceFile) {

        this.resourceFile = resourceFile;
    }

    @Override
    public boolean applies(final ProctoringWindowData data) {
        try {
            return data.connectionData.proctoringServerType == ProctoringServerType.ZOOM;
        } catch (final Exception e) {
            log.error("Failed to verify responsibility. Cause: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProctoringWindowScript(final ProctoringWindowData data) {
        final Map<String, String> args = new HashMap<>();
        args.put(ATTR_HOST, data.connectionData.serverHost);
        args.put(ATTR_ROOM_NAME, data.connectionData.meetingId);
        args.put(ATTR_ACCESS_TOKEN, String.valueOf(data.connectionData.accessToken));
        args.put(ATTR_API_KEY, String.valueOf(data.connectionData.apiKey));
        if (StringUtils.isNotBlank(data.connectionData.roomKey)) {
            args.put(ATTR_ROOM_KEY, String.valueOf(data.connectionData.roomKey));
        } else {
            args.put(ATTR_ROOM_KEY, "");
        }
        args.put(ATTR_SUBJECT, data.connectionData.subject);
        args.put(ATTR_USER_NAME, data.connectionData.userName);

        final String htmlWindow = getHTMLWindow();
        final String replace = new StringSubstitutor(
                args,
                Constants.DYN_HTML_ATTR_OPEN,
                Constants.DYN_HTML_ATTR_CLOSE)
                        .replace(htmlWindow);
        return replace;
    }

    private String getHTMLWindow() {
        try {
            return IOUtils.toString(this.resourceFile.getInputStream());
        } catch (final Exception e) {
            log.error("Failed to load Jitsi Meet window resources", e);
            return "ERROR: " + e.getLocalizedMessage();
        }
    }

}
