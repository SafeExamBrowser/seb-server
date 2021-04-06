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

import org.apache.commons.lang3.StringUtils;
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
public class ZoomWindowScriptResolver implements ProctoringWindowScriptResolver {

    private static final Logger log = LoggerFactory.getLogger(ZoomWindowScriptResolver.class);

    private static final String ATTR_SUBJECT = "_subject_";
    private static final String ATTR_API_KEY = "_apiKey_";
    private static final String ATTR_ACCESS_TOKEN = "_accessToken_";
    private static final String ATTR_ROOM_KEY = "_roomKey_";
    private static final String ATTR_ROOM_NAME = "_roomName_";
    private static final String ATTR_HOST = "_host_";
    private static final String ATTR_USER_NAME = "_username_";

 // @formatter:off
    private static final String ZOOM_WINDOW_HTML =
            "<html>\n"
            + "    <head>\n"
            + "        <meta charset=\"utf-8\" />\n"
            + "        <link type=\"text/css\" rel=\"stylesheet\" href=\"https://source.zoom.us/1.9.0/css/bootstrap.css\" />\n"
            + "        <link type=\"text/css\" rel=\"stylesheet\" href=\"https://source.zoom.us/1.9.0/css/react-select.css\" />\n"
            + "    </head>\n"
            + "    <body>\n"
            + "        <script src=\"https://source.zoom.us/1.9.0/lib/vendor/react.min.js\"></script>\n"
            + "        <script src=\"https://source.zoom.us/1.9.0/lib/vendor/react-dom.min.js\"></script>\n"
            + "        <script src=\"https://source.zoom.us/1.9.0/lib/vendor/redux.min.js\"></script>\n"
            + "        <script src=\"https://source.zoom.us/1.9.0/lib/vendor/redux-thunk.min.js\"></script>\n"
            + "        <script src=\"https://source.zoom.us/1.9.0/lib/vendor/jquery.min.js\"></script>\n"
            + "        <script src=\"https://source.zoom.us/1.9.0/lib/vendor/lodash.min.js\"></script>\n"
            + "        <script src=\"https://source.zoom.us/zoom-meeting-1.9.0.min.js\"></script>\n"
            + "        <script src=\"https://cdnjs.cloudflare.com/ajax/libs/crypto-js/3.1.9/crypto-js.min.js\"></script>\n"
            + "        <script type=\"text/javascript\">\n"
            + "\n"
            + "            console.log(\"Checking system requirements...\");\n"
            + "            console.log(JSON.stringify(ZoomMtg.checkSystemRequirements()));\n"
            + "\n"
            + "            console.log(\"Initializing Zoom...\");\n"
            + "            ZoomMtg.setZoomJSLib('https://source.zoom.us/1.9.0/lib', '/av');\n"
            + "            ZoomMtg.preLoadWasm();\n"
            + "            ZoomMtg.prepareJssdk();\n"
            + "\n"
            + "            const API_KEY = \"%%_" + ATTR_API_KEY + "_%%\";\n"
            + "            const config = {\n"
            + "                meetingNumber: %%_" + ATTR_ROOM_NAME + "_%%,\n"
            + "                leaveUrl: '%%_" + ATTR_HOST + "_%%',\n"
            + "                userName: '%%_" + ATTR_USER_NAME + "_%%',\n"
            + "                passWord: '%%_" + ATTR_ROOM_KEY + "_%%',\n"
            + "                role: 1 // 1 for host; 0 for attendee\n"
            + "            };\n"
            + "\n"
            + "            const signature = '%%_" + ATTR_ACCESS_TOKEN + "_%%';\n"
            + "\n"
            + "            console.log(\"Initializing meeting...\");\n"
            + "\n"
            + "            // See documentation: https://zoom.github.io/sample-app-web/ZoomMtg.html#init\n"
            + "            ZoomMtg.init({\n"
            + "                debug: true, //optional\n"
            + "                leaveUrl: config.leaveUrl, //required\n"
            + "                // webEndpoint: 'PSO web domain', // PSO option\n"
            + "                showMeetingHeader: true, //option\n"
            + "                disableInvite: false, //optional\n"
            + "                disableCallOut: false, //optional\n"
            + "                disableRecord: false, //optional\n"
            + "                disableJoinAudio: false, //optional\n"
            + "                audioPanelAlwaysOpen: true, //optional\n"
            + "                showPureSharingContent: false, //optional\n"
            + "                isSupportAV: true, //optional,\n"
            + "                isSupportChat: true, //optional,\n"
            + "                isSupportQA: true, //optional,\n"
            + "                isSupportCC: true, //optional,\n"
            + "                screenShare: true, //optional,\n"
            + "                rwcBackup: '', //optional,\n"
            + "                videoDrag: true, //optional,\n"
            + "                sharingMode: 'both', //optional,\n"
            + "                videoHeader: true, //optional,\n"
            + "                isLockBottom: true, // optional,\n"
            + "                isSupportNonverbal: true, // optional,\n"
            + "                isShowJoiningErrorDialog: true, // optional,\n"
            + "                inviteUrlFormat: '', // optional\n"
            + "                loginWindow: {  // optional,\n"
            + "                    width: 400,\n"
            + "                    height: 380\n"
            + "                },\n"
            + "                meetingInfo: [ // optional\n"
            + "                   'topic',\n"
            + "                   'host',\n"
            + "                   'mn',\n"
            + "                   'pwd',\n"
            + "                   'invite',\n"
            + "                   'participant',\n"
            + "                   'dc'\n"
            + "                ],\n"
            + "                disableVoIP: false, // optional\n"
            + "                disableReport: false, // optional\n"
            + "                error: function (res) {\n"
            + "                    console.warn(\"INIT ERROR\")\n"
            + "                    console.log(res)\n"
            + "                },\n"
            + "                success: function () {\n"
            + "                   console.log(\"INIT SUCCESS\")\n"
            + "                    ZoomMtg.join({\n"
            + "                        signature: signature,\n"
            + "                        apiKey: API_KEY,\n"
            + "                        meetingNumber: config.meetingNumber,\n"
            + "                        userName: config.userName,\n"
            + "                        passWord: config.passWord,\n"
            + "                        success(res) {\n"
            + "                             console.log(\"JOIN SUCCESS\")\n"
            + "                        },\n"
            + "                        error(res) {\n"
            + "                            console.warn(\"JOIN ERROR\")\n"
            + "                            console.log(res)\n"
            + "                        }\n"
            + "                    })\n"
            + "                }\n"
            + "            })\n"
            + "        </script>\n"
            + "    </body>\n"
            + "</html>";
    // @formatter:on

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

        return new StringSubstitutor(args, "%%_", "_%%")
                .replace(ZOOM_WINDOW_HTML);
    }

}
