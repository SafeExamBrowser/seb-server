/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.WebserviceURIService;

@Lazy
@Component
final class InstitutionalAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(InstitutionalAuthenticationEntryPoint.class);

    private final String guiEntryPoint;
    private final WebserviceURIService webserviceURIService;
    private final ClientHttpRequestFactory clientHttpRequestFactory;

    protected InstitutionalAuthenticationEntryPoint(
            @Value("${sebserver.gui.entrypoint}") final String guiEntryPoint,
            final WebserviceURIService webserviceURIService,
            final ClientHttpRequestFactory clientHttpRequestFactory) {

        this.guiEntryPoint = guiEntryPoint;
        this.webserviceURIService = webserviceURIService;
        this.clientHttpRequestFactory = clientHttpRequestFactory;
    }

    @Override
    public void commence(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException authException) throws IOException, ServletException {

        final String requestURI = request.getRequestURI();

        log.info("No default gui entrypoint requested: {}", requestURI);

        final String logoImageBase64 = requestLogoImage(requestURI);
        if (StringUtils.isNotBlank(logoImageBase64)) {
            request.getSession().setAttribute(API.PARAM_LOGO_IMAGE, logoImageBase64);
        } else {
            request.getSession().removeAttribute(API.PARAM_LOGO_IMAGE);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }

        final RequestDispatcher dispatcher = request.getServletContext()
                .getRequestDispatcher(this.guiEntryPoint);
        dispatcher.forward(request, response);
    }

    private String requestLogoImage(final String requestURI) {
        log.debug("Trying to verify insitution from requested entrypoint url: {}", requestURI);

        final String instPrefix = requestURI.replaceAll("/", "");
        if (StringUtils.isBlank(instPrefix)) {
            return null;
        }

        try {

            final RestTemplate restTemplate = new RestTemplate();
            restTemplate.setRequestFactory(this.clientHttpRequestFactory);

            final ResponseEntity<String> exchange = restTemplate
                    .exchange(
                            this.webserviceURIService.getURIBuilder()
                                    .path(API.INFO_ENDPOINT + API.INSTITUTIONAL_LOGO_PATH)
                                    .toUriString(),
                            HttpMethod.GET,
                            HttpEntity.EMPTY,
                            String.class,
                            instPrefix);

            if (exchange.getStatusCodeValue() == HttpStatus.OK.value()) {
                return exchange.getBody();
            } else {
                log.error("Failed to verify insitution from requested entrypoint url: {}, response: {}", requestURI,
                        exchange);
            }
        } catch (final Exception e) {
            log.error("Failed to verify insitution from requested entrypoint url: {}", requestURI, e);
        }

        return null;
    }
}