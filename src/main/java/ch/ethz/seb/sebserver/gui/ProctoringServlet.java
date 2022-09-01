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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.SEBServerAuthorizationContext;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService.ProctoringWindowData;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringWindowScriptResolver;

@Component
@GuiProfile
public class ProctoringServlet extends HttpServlet {

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

        final boolean authenticated = isAuthenticated(httpSession, webApplicationContext);
        if (!authenticated) {
            resp.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

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
            RAPConfiguration.setCORS(resp);
            resp.getOutputStream().println(script);
        }
    }

    @Override
    protected void doOptions(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

        RAPConfiguration.setCORS(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
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