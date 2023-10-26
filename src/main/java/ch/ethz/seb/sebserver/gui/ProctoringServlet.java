/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.SEBServerAuthorizationContext;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService.ProctoringWindowData;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService.ScreenProctoringWindowData;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringWindowScriptResolver;

@Component
@GuiProfile
public class ProctoringServlet extends HttpServlet {

    public static final String SCREEN_PROCOTRING_FLAG_PARAM = "screenproctoring";

    private static final long serialVersionUID = 3475978419653411800L;
    private static final Logger log = LoggerFactory.getLogger(ProctoringServlet.class);

    private final Collection<ProctoringWindowScriptResolver> proctoringWindowScriptResolver;

    public ProctoringServlet(
            final Collection<ProctoringWindowScriptResolver> proctoringWindowScriptResolver) {

        this.proctoringWindowScriptResolver = proctoringWindowScriptResolver;
    }

    @Override
    protected void doGet(
            final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, IOException {

        final HttpSession httpSession = req.getSession();
        final ServletContext servletContext = httpSession.getServletContext();
        final WebApplicationContext webApplicationContext = WebApplicationContextUtils
                .getRequiredWebApplicationContext(servletContext);

        UserInfo user;
        try {
            user = isAuthenticated(httpSession, webApplicationContext);
        } catch (final Exception e) {
            resp.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        final String parameter = req.getParameter(SCREEN_PROCOTRING_FLAG_PARAM);
        if (BooleanUtils.toBoolean(parameter)) {
            openScreenProctoring(req, resp, user, httpSession);
        } else {
            openRemoteProctoring(resp, httpSession);
        }
    }

    private void openScreenProctoring(
            final HttpServletRequest req,
            final HttpServletResponse resp,
            final UserInfo user,
            final HttpSession httpSession) throws IOException {

        final ScreenProctoringWindowData data = (ScreenProctoringWindowData) httpSession
                .getAttribute(ProctoringGUIService.SESSION_ATTR_SCREEN_PROCTORING_DATA);

        // NOTE: POST on data.loginLocation seems not to work for automated login
        // TODO discuss with Nadim how to make a direct login POST on the GUI client
        //      maybe there is a way to expose /login endpoint for directly POST credentials for login.

        // https://stackoverflow.com/questions/46582/response-redirect-with-post-instead-of-get
        final StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<body onload='document.forms[\"form\"].submit()'>");
        sb.append("<form name='form' action='");
        sb.append(data.loginLocation).append("' method='post'>");
        sb.append("</input type='hidden' name='username' value='").append("super-admin").append("'>");
        sb.append("</input type='hidden' name='password' type='password' value='").append("admin").append("'>");
        sb.append("</form>");
        sb.append("</body>");
        sb.append("</html>");

        resp.getOutputStream().println(sb.toString());
    }

    private void openRemoteProctoring(
            final HttpServletResponse resp,
            final HttpSession httpSession) throws IOException {

        final ProctoringWindowData proctoringData =
                (ProctoringWindowData) httpSession
                        .getAttribute(ProctoringGUIService.SESSION_ATTR_PROCTORING_DATA);

        final String script = this.proctoringWindowScriptResolver.stream()
                .filter(resolver -> resolver.applies(proctoringData))
                .findFirst()
                .map(resolver -> resolver.getProctoringWindowScript(proctoringData))
                .orElse(null);

        if (script == null) {
            log.error("Failed to get proctoring window script for data: {}", proctoringData);
            resp.getOutputStream().println("Failed to get proctoring window script");
        } else {
            resp.getOutputStream().println(script);
        }
    }

    @Override
    protected void doOptions(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private UserInfo isAuthenticated(
            final HttpSession httpSession,
            final WebApplicationContext webApplicationContext) {

        final AuthorizationContextHolder authorizationContextHolder = webApplicationContext
                .getBean(AuthorizationContextHolder.class);
        final SEBServerAuthorizationContext authorizationContext = authorizationContextHolder
                .getAuthorizationContext(httpSession);
        if (!authorizationContext.isValid() || !authorizationContext.isLoggedIn()) {
            throw new RuntimeException("No authentication found");
        }

        return authorizationContext.getLoggedInUser().getOrThrow();
    }

}