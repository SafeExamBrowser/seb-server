/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Lazy
@Component
@GuiProfile
public class CrossOriginIsolationFilter implements Filter {

    private static final String SAME_ORIGIN = "same-origin";
    private static final String REQUIRE_CORP = "require-corp";
    private static final String CROSS_ORIGIN_OPENER_POLICY = "Cross-Origin-Opener-Policy";
    private static final String CROSS_ORIGIN_EMBEDDER_POLICY = "Cross-Origin-Embedder-Policy";

    private final String adminEndpoint;
    private final String examEndpoint;

    public CrossOriginIsolationFilter(
            @Value("${sebserver.webservice.api.exam.endpoint:/exam-api}") final String examEndpoint,
            @Value("${sebserver.webservice.api.admin.endpoint:/admin-api/v1}") final String adminEndpoint) {

        this.adminEndpoint = adminEndpoint;
        this.examEndpoint = examEndpoint;
    }

    @Override
    public void doFilter(
            final ServletRequest request,
            final ServletResponse response,
            final FilterChain chain) throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        processResponse((HttpServletRequest) request, (HttpServletResponse) response);
        chain.doFilter(request, response);
    }

    protected void processResponse(final HttpServletRequest request, final HttpServletResponse response) {
        final String url = request.getRequestURI();

        if (url.startsWith(this.adminEndpoint) || url.startsWith(this.examEndpoint)) {
            return;
        }

        response.setHeader(CROSS_ORIGIN_EMBEDDER_POLICY, REQUIRE_CORP);
        response.setHeader(CROSS_ORIGIN_OPENER_POLICY, SAME_ORIGIN);
    }
}
