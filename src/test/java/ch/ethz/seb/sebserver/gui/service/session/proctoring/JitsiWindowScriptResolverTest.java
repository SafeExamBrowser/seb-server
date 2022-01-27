/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session.proctoring;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService.ProctoringWindowData;

public class JitsiWindowScriptResolverTest {

    @Test
    public void testJitsiWindowScriptResolver() {
        final DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader();
        final Resource resource = defaultResourceLoader.getResource(JitsiWindowScriptResolver.RES_PATH);
        final JitsiWindowScriptResolver jitsiWindowScriptResolver = new JitsiWindowScriptResolver(resource);

        final ProctoringWindowData proctoringWindowData = new ProctoringWindowData(
                "0",
                "Test_Window",
                new ProctoringRoomConnection(
                        ProctoringServerType.JITSI_MEET,
                        "CONNECTION_TOKEN",
                        "HOST",
                        "URL",
                        "ROOM",
                        "SUBJECT",
                        "ACCESS_TOKEN",
                        null,
                        "API_KEY",
                        "ROOM_KEY",
                        "MEETING_ID",
                        "USER_NAME",
                        null));

        final ProctoringWindowData proctoringWindowDataOther = new ProctoringWindowData(
                "0",
                "Test_Window",
                new ProctoringRoomConnection(
                        ProctoringServerType.ZOOM,
                        "CONNECTION_TOKEN",
                        "HOST",
                        "URL",
                        "ROOM",
                        "SUBJECT",
                        "ACCESS_TOKEN",
                        null,
                        "API_KEY",
                        "ROOM_KEY",
                        "MEETING_ID",
                        "USER_NAME",
                        null));

        assertFalse(jitsiWindowScriptResolver.applies(proctoringWindowDataOther));
        assertTrue(jitsiWindowScriptResolver.applies(proctoringWindowData));

        final String proctoringWindowScript = jitsiWindowScriptResolver
                .getProctoringWindowScript(proctoringWindowData);

        assertEquals(
                "<!DOCTYPE html>\r\n"
                        + "<html>\r\n"
                        + "    <head>\r\n"
                        + "    <title></title>\r\n"
                        + "    <script src='https://HOST/external_api.js'></script>\r\n"
                        + "</head>\r\n"
                        + "<body>\r\n"
                        + "<div id='proctoring'></div>\r\n"
                        + "</body>\r\n"
                        + "<script>\r\n"
                        + "\r\n"
                        + "    const options = {\r\n"
                        + "        parentNode: document.querySelector('#proctoring'),\r\n"
                        + "        roomName: 'ROOM',\r\n"
                        + "        // width: window.innerWidth,\r\n"
                        + "        height: window.innerHeight - 4,\r\n"
                        + "        jwt: 'ACCESS_TOKEN',\r\n"
                        + "        configOverwrite: { startAudioOnly: false, startWithAudioMuted: true, startWithVideoMuted: false, disable1On1Mode: true },\r\n"
                        + "        interfaceConfigOverwrite: { \r\n"
                        + "            TOOLBAR_BUTTONS: [\r\n"
                        + "                'microphone', 'camera',\r\n"
                        + "                'fodeviceselection', 'profile', 'chat', 'recording',\r\n"
                        + "                'livestreaming', 'settings',\r\n"
                        + "                'videoquality', 'filmstrip', 'feedback',\r\n"
                        + "                'tileview', 'help', 'mute-everyone', 'security'\r\n"
                        + "            ],\r\n"
                        + "            SHOW_WATERMARK_FOR_GUESTS: false,\r\n"
                        + "            RECENT_LIST_ENABLED: false,\r\n"
                        + "            HIDE_INVITE_MORE_HEADER: true,\r\n"
                        + "            DISABLE_RINGING: true,\r\n"
                        + "            DISABLE_PRESENCE_STATUS: true,\r\n"
                        + "            DISABLE_JOIN_LEAVE_NOTIFICATIONS: true,\r\n"
                        + "            GENERATE_ROOMNAMES_ON_WELCOME_PAGE: false,\r\n"
                        + "            MOBILE_APP_PROMO: false,\r\n"
                        + "            SHOW_JITSI_WATERMARK: false,\r\n"
                        + "            DISABLE_PRESENCE_STATUS: true,\r\n"
                        + "            DISABLE_RINGING: true,\r\n"
                        + "            DISABLE_VIDEO_BACKGROUND: false,\r\n"
                        + "            filmStripOnly: false\r\n"
                        + "        }\r\n"
                        + "    }\r\n"
                        + "    \r\n"
                        + "    const meetAPI = new JitsiMeetExternalAPI('HOST', options);\r\n"
                        + "    meetAPI.executeCommand('subject', 'SUBJECT');\r\n"
                        + "    meetAPI.on('videoConferenceJoined', (event) => {\r\n"
                        + "        meetAPI.executeCommand('setLargeVideoParticipant', event.id);\r\n"
                        + "    });\r\n"
                        + "</script>\r\n"
                        + "</html>",
                proctoringWindowScript);
    }

}
