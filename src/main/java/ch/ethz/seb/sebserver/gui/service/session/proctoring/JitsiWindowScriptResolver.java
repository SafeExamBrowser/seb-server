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

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService.ProctoringWindowData;

@Lazy
@Service
@GuiProfile
public class JitsiWindowScriptResolver implements ProctoringWindowScriptResolver {

    private static final Logger log = LoggerFactory.getLogger(JitsiWindowScriptResolver.class);

    private static final String ATTR_SUBJECT = "_subject_";
    private static final String ATTR_ACCESS_TOKEN = "_accessToken_";
    private static final String ATTR_ROOM_NAME = "_roomName_";
    private static final String ATTR_HOST = "_host_";

    // @formatter:off
    private static final String JITSI_WINDOW_HTML =
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "    <title></title>" +
            "    <script src='https://%%_" + ATTR_HOST + "_%%/external_api.js'></script>" +
            "</head>" +
            "" +
            "<body>" +
            "<div id=\"proctoring\"></div> " +
            "</body>" +
            "<script>" +
            "    const options = {\n" +
            "        parentNode: document.querySelector('#proctoring'),\n" +
            "        roomName: '%%_" + ATTR_ROOM_NAME + "_%%',\n" +
//                    "        width: window.innerWidth,\n" +
            "        height: window.innerHeight - 4,\n" +
            "        jwt: '%%_" + ATTR_ACCESS_TOKEN + "_%%',\n" +
            "        configOverwrite: { startAudioOnly: false, startWithAudioMuted: true, startWithVideoMuted: false, disable1On1Mode: true },\n" +
            "        interfaceConfigOverwrite: { " +
            "TOOLBAR_BUTTONS: [\r\n" +
            "        'microphone', 'camera',\r\n" +
            "        'fodeviceselection', 'profile', 'chat', 'recording',\r\n" +
            "        'livestreaming', 'settings',\r\n" +
            "        'videoquality', 'filmstrip', 'feedback',\r\n" +
            "        'tileview', 'help', 'mute-everyone', 'security'\r\n" +
            "    ],"
            + "SHOW_WATERMARK_FOR_GUESTS: false, "
            + "RECENT_LIST_ENABLED: false, "
            + "HIDE_INVITE_MORE_HEADER: true, "
            + "DISABLE_RINGING: true, "
            + "DISABLE_PRESENCE_STATUS: true, "
            + "DISABLE_JOIN_LEAVE_NOTIFICATIONS: true, "
            + "GENERATE_ROOMNAMES_ON_WELCOME_PAGE: false, "
            + "MOBILE_APP_PROMO: false, "
            + "SHOW_JITSI_WATERMARK: false, "
            + "DISABLE_PRESENCE_STATUS: true, "
            + "DISABLE_RINGING: true, "
            + "DISABLE_VIDEO_BACKGROUND: false, "
            + "filmStripOnly: false }\n" +
            "    }\n" +
            "    const meetAPI = new JitsiMeetExternalAPI(\"%%_" + ATTR_HOST + "_%%\", options);\n" +
            "    meetAPI.executeCommand('subject', '%%_" + ATTR_SUBJECT + "_%%');\n" +
            "</script>" +
            "</html>";
    // @formatter:on

    @Override
    public boolean applies(final ProctoringWindowData data) {
        try {
            return data.connectionData.proctoringServerType == ProctoringServerType.JITSI_MEET;
        } catch (final Exception e) {
            log.error("Failed to verify responsibility. Cause: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProctoringWindowScript(final ProctoringWindowData data) {
        final Map<String, String> args = new HashMap<>();
        args.put(ATTR_HOST, data.connectionData.serverHost);
        args.put(ATTR_ROOM_NAME, data.connectionData.roomName);
        args.put(ATTR_ACCESS_TOKEN, String.valueOf(data.connectionData.accessToken));
        args.put(ATTR_SUBJECT, data.connectionData.subject);

        return new StringSubstitutor(args, "%%_", "_%%")
                .replace(JITSI_WINDOW_HTML);
    }

}
