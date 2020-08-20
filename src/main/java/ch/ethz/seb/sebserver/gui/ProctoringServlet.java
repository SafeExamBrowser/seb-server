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

import ch.ethz.seb.sebserver.gbl.model.exam.SEBClientProctoringConnectionData;
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
                    "    const options = {\r\n" +
                    "        parentNode: document.querySelector('#proctoring'),\r\n" +
                    "        roomName: '%s',\r\n" +
                    "        width: 600,\r\n" +
                    "        height: 400,\r\n" +
                    "        jwt: '%s'\r\n" +
                    "    }\r\n" +
                    "    meetAPI = new JitsiMeetExternalAPI(\"%s\", options);\r\n" +
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

        final SEBClientProctoringConnectionData proctoringConnectionData =
                (SEBClientProctoringConnectionData) httpSession.getAttribute(SESSION_ATTR_PROCTORING_DATA);

        final String accessToken = proctoringConnectionData.getAccessToken();
        final String roomName = proctoringConnectionData.roomName;
        final String server = "seb-jitsi.ethz.ch";
        final String script = String.format(HTML, server, roomName, accessToken, server);
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