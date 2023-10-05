/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
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

import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.SEBServerAuthorizationContext;

@Component
@GuiProfile
public class ScreenProctoringServlet extends HttpServlet {

    private static final long serialVersionUID = 4147410676185956971L;

    @Override
    protected void doGet(
            final HttpServletRequest req,
            final HttpServletResponse resp) throws ServletException, IOException {

        final HttpSession httpSession = req.getSession();
        final ServletContext servletContext = httpSession.getServletContext();
        final WebApplicationContext webApplicationContext = WebApplicationContextUtils
                .getRequiredWebApplicationContext(servletContext);

        final UserInfo user = isAuthenticated(httpSession, webApplicationContext);

        // TODO https://stackoverflow.com/questions/46582/response-redirect-with-post-instead-of-get

        final String hello = "Hello";

//        StringBuilder sb = new StringBuilder();
//        sb.Append("<html>");
//        sb.AppendFormat(@"<body onload='document.forms[""form""].submit()'>");
//        sb.AppendFormat("<form name='form' action='{0}' method='post'>",postbackUrl);
//        sb.AppendFormat("<input type='hidden' name='id' value='{0}'>", id);
//        // Other params go here
//        sb.Append("</form>");
//        sb.Append("</body>");
//        sb.Append("</html>");

        resp.getOutputStream().println(hello);

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
