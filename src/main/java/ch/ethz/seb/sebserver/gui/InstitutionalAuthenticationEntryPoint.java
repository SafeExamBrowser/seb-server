/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.WebserviceURIService;
import ch.ethz.seb.sebserver.gui.widget.ImageUploadSelection;

@Lazy
@Component
@GuiProfile
public final class InstitutionalAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String INST_SUFFIX_ATTRIBUTE = "endpointInstId";

    private static final Logger log = LoggerFactory.getLogger(InstitutionalAuthenticationEntryPoint.class);

    private final String guiEntryPoint;

    private final String remoteProctoringEndpoint;
    private final String defaultLogo;
    private final WebserviceURIService webserviceURIService;
    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;

    protected InstitutionalAuthenticationEntryPoint(
            @Value("${sebserver.gui.entrypoint}") final String guiEntryPoint,
            @Value("${sebserver.gui.remote.proctoring.entrypoint:/remote-proctoring}") final String remoteProctoringEndpoint,
            @Value("${sebserver.gui.defaultLogo:" + Constants.NO_NAME + "}") final String defaultLogoFileName,
            final WebserviceURIService webserviceURIService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final ResourceLoader resourceLoader) {

        this.guiEntryPoint = guiEntryPoint;
        this.remoteProctoringEndpoint = remoteProctoringEndpoint;
        this.webserviceURIService = webserviceURIService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;

        String _defaultLogo;
        if (!Constants.NO_NAME.equals(defaultLogoFileName)) {
            try {

                final String extension = ImageUploadSelection.SUPPORTED_IMAGE_FILES.stream()
                        .filter(defaultLogoFileName::endsWith)
                        .findFirst()
                        .orElse(null);

                if (extension == null) {
                    throw new IllegalArgumentException("Image of type: " + defaultLogoFileName + " not supported");
                }

                final Resource resource = resourceLoader.getResource(defaultLogoFileName);
                final Reader reader = new InputStreamReader(
                        new Base64InputStream(resource.getInputStream(), true),
                        StandardCharsets.UTF_8);

                _defaultLogo = FileCopyUtils.copyToString(reader);

            } catch (final Exception e) {
                log.warn("Failed to load default logo image from filesystem: {}", defaultLogoFileName);
                _defaultLogo = null;
            }

            this.defaultLogo = _defaultLogo;
        } else {
            this.defaultLogo = null;
        }
    }

    @Override
    public void commence(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException authException) throws IOException, ServletException {

        final String institutionalEndpoint = extractInstitutionalEndpoint(request);

        if (StringUtils.isNotBlank(institutionalEndpoint)) {
            if (log.isDebugEnabled()) {
                log.debug("No default gui entrypoint requested: {}", institutionalEndpoint);
            }

            try {
                final Map<String, Object> uriVars = new HashMap<>();
                uriVars.put(API.INFO_PARAM_INST_SUFFIX, institutionalEndpoint);
                final String uriString = this.webserviceURIService.getURIBuilder()
                        .path(API.INFO_ENDPOINT + API.INFO_INST_ENDPOINT)
                        .uriVariables(uriVars)
                        .toUriString();
                final RestTemplate restTemplate = new RestTemplate();
                final List<EntityName> institutions = restTemplate
                        .exchange(
                                uriString,
                                HttpMethod.GET,
                                HttpEntity.EMPTY,
                                new ParameterizedTypeReference<List<EntityName>>() {
                                })
                        .getBody();

                if (institutions != null && !institutions.isEmpty()) {
                    request.getSession().setAttribute(
                            INST_SUFFIX_ATTRIBUTE,
                            StringUtils.isNotBlank(institutionalEndpoint)
                                    ? institutionalEndpoint
                                    : null);

                    if (log.isDebugEnabled()) {
                        log.debug("Known and active gui entrypoint requested: {}", institutions);
                    }

                    final String logoImageBase64 = requestLogoImage(institutionalEndpoint);
                    if (StringUtils.isNotBlank(logoImageBase64)) {
                        request.getSession().setAttribute(API.PARAM_LOGO_IMAGE, logoImageBase64);

                    }
                    forwardToEntryPoint(request, response, this.guiEntryPoint, false);
                    return;
                }
            } catch (final Exception e) {
                log.error("Failed to extract and set institutional endpoint request: ", e);
            }
        }

        request.getSession().setAttribute(INST_SUFFIX_ATTRIBUTE, null);
        request.getSession().removeAttribute(API.PARAM_LOGO_IMAGE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        forwardToEntryPoint(request, response, this.guiEntryPoint, institutionalEndpoint == null);

    }

    private void forwardToEntryPoint(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String entryPoint,
            final boolean redirect) throws ServletException, IOException {

        final String requestURI = request.getRequestURI();
        if (requestURI.startsWith(this.remoteProctoringEndpoint)) {

            final RequestDispatcher dispatcher = request
                    .getServletContext()
                    .getRequestDispatcher(this.remoteProctoringEndpoint);

            dispatcher.forward(request, response);
            return;
        }

        if (redirect) {
            response.sendRedirect(entryPoint);
        } else {
            final RequestDispatcher dispatcher = request
                    .getServletContext()
                    .getRequestDispatcher(entryPoint);

            dispatcher.forward(request, response);
        }
    }

    public static String extractInstitutionalEndpoint(final HttpServletRequest request) {
        final String requestURI = request.getRequestURI();
        if (StringUtils.isBlank(requestURI)) {
            return null;
        }

        if (requestURI.equals(Constants.SLASH.toString())) {
            return StringUtils.EMPTY;
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug("Trying to verify institution from requested entrypoint url: {}", requestURI);
            }

            final String[] split = StringUtils.split(requestURI, Constants.SLASH);
            if (split.length > 1) {
                return null;
            }

            return split[0];
        } catch (final Exception e) {
            log.error("Failed to extract institutional URL suffix: {}", e.getMessage());
            return null;
        }
    }

    public static String extractInstitutionalEndpoint() {
        try {
            final Object attribute = RWT.getUISession().getHttpSession().getAttribute(INST_SUFFIX_ATTRIBUTE);
            return (attribute != null) ? String.valueOf(attribute) : null;
        } catch (final Exception e) {
            log.warn("Failed to extract institutional endpoint form user session: {}", e.getMessage());
            return null;
        }
    }

    private String requestLogoImage(final String institutionalEndpoint) {
        if (StringUtils.isBlank(institutionalEndpoint)) {
            return this.defaultLogo;
        }

        try {

            final RestTemplate restTemplate = new RestTemplate();

            final ClientHttpRequestFactory clientHttpRequestFactory = this.clientHttpRequestFactoryService
                    .getClientHttpRequestFactory()
                    .getOrThrow();

            restTemplate.setRequestFactory(clientHttpRequestFactory);
            final Map<String, Object> uriVars = new HashMap<>();
            uriVars.put(API.INFO_PARAM_INST_SUFFIX, institutionalEndpoint);
            final String uriString = this.webserviceURIService.getURIBuilder()
                    .path(API.INFO_ENDPOINT + API.INSTITUTIONAL_LOGO_PATH)
                    .uriVariables(uriVars)
                    .toUriString();
            final ResponseEntity<String> exchange = restTemplate
                    .exchange(
                            uriString,
                            HttpMethod.GET,
                            HttpEntity.EMPTY,
                            String.class,
                            institutionalEndpoint);

            if (exchange.getStatusCodeValue() == HttpStatus.OK.value()) {
                return exchange.getBody();
            } else {
                log.warn("Failed to verify institution from requested entrypoint url: {}, response: {}",
                        institutionalEndpoint,
                        exchange);
            }
        } catch (final Exception e) {
            log.warn("Failed to verify institution from requested entrypoint url: {}",
                    institutionalEndpoint,
                    e);
        }

        return null;
    }

}