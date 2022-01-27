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

public class ZoomWindowScriptResolverTest {

    @Test
    public void testZoomWindowScriptResolver() {
        final DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader();
        final Resource resource = defaultResourceLoader.getResource(ZoomWindowScriptResolver.RES_PATH);
        final ZoomWindowScriptResolver zoomWindowScriptResolver = new ZoomWindowScriptResolver(resource, "1.9.8");

        final ProctoringWindowData proctoringWindowDataZoom = new ProctoringWindowData(
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
                        "SDK_TOKEN",
                        "API_KEY",
                        "ROOM_KEY",
                        "MEETING_ID",
                        "USER_NAME",
                        null));

        final ProctoringWindowData proctoringWindowDataOther = new ProctoringWindowData(
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
                        "SDK_TOKEN",
                        "API_KEY",
                        "ROOM_KEY",
                        "MEETING_ID",
                        "USER_NAME",
                        null));

        assertFalse(zoomWindowScriptResolver.applies(proctoringWindowDataOther));
        assertTrue(zoomWindowScriptResolver.applies(proctoringWindowDataZoom));

        final String proctoringWindowScript = zoomWindowScriptResolver
                .getProctoringWindowScript(proctoringWindowDataZoom);

        assertEquals(
                "<html>\r\n"
                        + "    <head>\r\n"
                        + "        <meta charset='utf-8' />\r\n"
                        + "        <link type='text/css' rel='stylesheet' href='https://source.zoom.us/1.9.8/css/bootstrap.css' />\r\n"
                        + "        <link type='text/css' rel='stylesheet' href='https://source.zoom.us/1.9.8/css/react-select.css' />\r\n"
                        + "    </head>\r\n"
                        + "    <body>\r\n"
                        + "        <script src='https://source.zoom.us/1.9.8/lib/vendor/react.min.js'></script>\r\n"
                        + "        <script src='https://source.zoom.us/1.9.8/lib/vendor/react-dom.min.js'></script>\r\n"
                        + "        <script src='https://source.zoom.us/1.9.8/lib/vendor/redux.min.js'></script>\r\n"
                        + "        <script src='https://source.zoom.us/1.9.8/lib/vendor/redux-thunk.min.js'></script>\r\n"
                        + "        <script src='https://source.zoom.us/1.9.8/lib/vendor/lodash.min.js'></script>\r\n"
                        + "        <script src='https://source.zoom.us/zoom-meeting-1.9.8.min.js'></script>\r\n"
                        + "        <script src='https://cdnjs.cloudflare.com/ajax/libs/crypto-js/3.1.9/crypto-js.min.js'></script>\r\n"
                        + "        <script type='text/javascript'>\r\n"
                        + "\r\n"
                        + "            console.log('Checking system requirements...');\r\n"
                        + "            console.log(JSON.stringify(ZoomMtg.checkSystemRequirements()));\r\n"
                        + "\r\n"
                        + "            console.log('Initializing Zoom...');\r\n"
                        + "            ZoomMtg.setZoomJSLib('https://source.zoom.us/1.9.8/lib', '/av');\r\n"
                        + "            ZoomMtg.preLoadWasm();\r\n"
                        + "            ZoomMtg.prepareJssdk();\r\n"
                        + "\r\n"
                        + "            const API_KEY = 'ROOM_KEY';\r\n"
                        + "            const config = {\r\n"
                        + "                meetingNumber: MEETING_ID,\r\n"
                        + "                leaveUrl: 'HOST',\r\n"
                        + "                userName: 'USER_NAME',\r\n"
                        + "                passWord: 'API_KEY',\r\n"
                        + "                role: 1 // 1 for host; 0 for attendee\r\n"
                        + "            };\r\n"
                        + "\r\n"
                        + "            const signature = 'ACCESS_TOKEN';\r\n"
                        + "            console.log('Initializing meeting...');\r\n"
                        + "\r\n"
                        + "            // See documentation: https://zoom.github.io/sample-app-web/ZoomMtg.html#init\r\n"
                        + "            ZoomMtg.init({\r\n"
                        + "                debug: true, //optional\r\n"
                        + "                leaveUrl: config.leaveUrl, //required\r\n"
                        + "                // webEndpoint: 'PSO web domain', // PSO option\r\n"
                        + "                showMeetingHeader: true, //option\r\n"
                        + "                disableInvite: false, //optional\r\n"
                        + "                disableCallOut: false, //optional\r\n"
                        + "                disableRecord: false, //optional\r\n"
                        + "                disableJoinAudio: false, //optional\r\n"
                        + "                audioPanelAlwaysOpen: true, //optional\r\n"
                        + "                showPureSharingContent: false, //optional\r\n"
                        + "                isSupportAV: true, //optional,\r\n"
                        + "                isSupportChat: true, //optional,\r\n"
                        + "                isSupportQA: true, //optional,\r\n"
                        + "                isSupportCC: true, //optional,\r\n"
                        + "                screenShare: true, //optional,\r\n"
                        + "                rwcBackup: '', //optional,\r\n"
                        + "                videoDrag: true, //optional,\r\n"
                        + "                sharingMode: 'both', //optional,\r\n"
                        + "                videoHeader: true, //optional,\r\n"
                        + "                isLockBottom: true, // optional,\r\n"
                        + "                isSupportNonverbal: true, // optional,\r\n"
                        + "                isShowJoiningErrorDialog: true, // optional,\r\n"
                        + "                inviteUrlFormat: '', // optional\r\n"
                        + "                loginWindow: {  // optional,\r\n"
                        + "                    width: window.innerWidth - 5,\r\n"
                        + "                    height: window.innerHeight - 4\r\n"
                        + "                },\r\n"
                        + "                meetingInfo: [ // optional\r\n"
                        + "                   'topic',\r\n"
                        + "                   'host',\r\n"
                        + "                   'mn',\r\n"
                        + "                   'pwd',\r\n"
                        + "                   'invite',\r\n"
                        + "                   'participant',\r\n"
                        + "                   'dc'\r\n"
                        + "                ],\r\n"
                        + "                disableVoIP: false, // optional\r\n"
                        + "                disableReport: false, // optional\r\n"
                        + "                error: function (res) {\r\n"
                        + "                    console.warn('INIT ERROR')\r\n"
                        + "                    console.log(res)\r\n"
                        + "                },\r\n"
                        + "                success: function () {\r\n"
                        + "                   console.log('INIT SUCCESS')\r\n"
                        + "                    ZoomMtg.join({\r\n"
                        + "                        signature: signature,\r\n"
                        + "                        apiKey: API_KEY,\r\n"
                        + "                        meetingNumber: config.meetingNumber,\r\n"
                        + "                        userName: config.userName,\r\n"
                        + "                        passWord: config.passWord,\r\n"
                        + "                        success(res) {\r\n"
                        + "                             console.log('JOIN SUCCESS')\r\n"
                        + "                        },\r\n"
                        + "                        error(res) {\r\n"
                        + "                            console.warn('JOIN ERROR')\r\n"
                        + "                            console.log(res)\r\n"
                        + "                        }\r\n"
                        + "                    })\r\n"
                        + "                }\r\n"
                        + "            })\r\n"
                        + "            \r\n"
                        + "            window.addEventListener('unload', () => {\r\n"
                        + "                ZoomMtg.endMeeting({});\r\n"
                        + "            });\r\n"
                        + "        </script>\r\n"
                        + "    </body>\r\n"
                        + "</html>",
                proctoringWindowScript);
    }

}
