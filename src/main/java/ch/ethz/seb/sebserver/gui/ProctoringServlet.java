/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnectionData;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.SEBServerAuthorizationContext;

@Component
@GuiProfile
public class ProctoringServlet extends HttpServlet {

    private static final long serialVersionUID = 3475978419653411800L;

    public static final String SESSION_ATTR_PROCTORING_DATA = "SESSION_ATTR_PROCTORING_DATA";

    // @formatter:off
    private static final String HTML =
            "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "    <title></title>" +
                    "    <script src='https://%s/external_api.js'></script>" +
                    "</head>" +
                    "" +
                    "<body>" +
                    "<div id=\"proctoring\"></div> " +
                    "</body>" +
                    "<script>" +
                    "    const options = {\n" +
                    "        parentNode: document.querySelector('#proctoring'),\n" +
                    "        roomName: '%s',\n" +
//                    "        width: window.innerWidth,\n" +
                    "        height: window.innerHeight,\n" +
                    "        jwt: '%s',\n" +
                    "        configOverwrite: { startAudioOnly: false, startWithAudioMuted: true, startWithVideoMuted: true, disable1On1Mode: true },\n" +
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
                    "    const meetAPI = new JitsiMeetExternalAPI(\"%s\", options);\n" +
                    "    meetAPI.executeCommand('subject', '%s');\n" +
                    "</script>" +
                    "</html>";
    // @formatter:on

    @Override
    protected void doGet(
            final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, IOException {

        final HttpSession httpSession = req.getSession();
        final ServletContext servletContext = httpSession.getServletContext();
        final WebApplicationContext webApplicationContext = WebApplicationContextUtils
                .getRequiredWebApplicationContext(servletContext);

        final boolean authenticated = isAuthenticated(httpSession, webApplicationContext);
        if (!authenticated) {
            resp.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        final SEBProctoringConnectionData proctoringConnectionData =
                (SEBProctoringConnectionData) httpSession.getAttribute(SESSION_ATTR_PROCTORING_DATA);

        final String script = String.format(
                HTML,
                proctoringConnectionData.serverHost,
                proctoringConnectionData.roomName,
                proctoringConnectionData.accessToken,
                proctoringConnectionData.serverHost,
                proctoringConnectionData.subject);
        resp.getOutputStream().println(script);

    }

    private boolean isAuthenticated(
            final HttpSession httpSession,
            final WebApplicationContext webApplicationContext) {

        final AuthorizationContextHolder authorizationContextHolder = webApplicationContext
                .getBean(AuthorizationContextHolder.class);
        final SEBServerAuthorizationContext authorizationContext = authorizationContextHolder
                .getAuthorizationContext(httpSession);
        return authorizationContext.isValid() && authorizationContext.isLoggedIn();
    }

}