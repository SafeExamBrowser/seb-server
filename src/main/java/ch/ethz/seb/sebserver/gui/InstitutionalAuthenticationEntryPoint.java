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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.WebserviceURIService;
import ch.ethz.seb.sebserver.gui.widget.ImageUploadSelection;

@Lazy
@Component
final class InstitutionalAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(InstitutionalAuthenticationEntryPoint.class);

    private final String guiEntryPoint;
    private final String defaultLogo;
    private final WebserviceURIService webserviceURIService;
    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;

    protected InstitutionalAuthenticationEntryPoint(
            @Value("${sebserver.gui.entrypoint}") final String guiEntryPoint,
            @Value("${sebserver.gui.defaultLogo:" + Constants.NO_NAME + "}") final String defaultLogoFileName,
            final WebserviceURIService webserviceURIService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final ResourceLoader resourceLoader) {

        this.guiEntryPoint = guiEntryPoint;
        this.webserviceURIService = webserviceURIService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;

        String _defaultLogo = null;
        if (!Constants.NO_NAME.equals(defaultLogoFileName)) {
            try {

                final String extension = ImageUploadSelection.SUPPORTED_IMAGE_FILES.stream()
                        .filter(ext -> defaultLogoFileName.endsWith(ext))
                        .findFirst()
                        .orElse(null);

                if (extension == null) {
                    throw new IllegalArgumentException("Image of type: " + defaultLogoFileName + " not supported");
                }

                final Resource resource = resourceLoader.getResource("file:" + defaultLogoFileName);
                final Reader reader = new InputStreamReader(
                        new Base64InputStream(resource.getInputStream(), true),
                        Charsets.UTF_8);

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

        log.info("No default gui entrypoint requested: {}", institutionalEndpoint);

        final String logoImageBase64 = requestLogoImage(institutionalEndpoint);
        if (StringUtils.isNotBlank(logoImageBase64)) {
            request.getSession().setAttribute(API.PARAM_LOGO_IMAGE, logoImageBase64);
            request.getSession().setAttribute("themeId", "sms");
            forwardToEntryPoint(request, response, this.guiEntryPoint);
        } else {
            request.getSession().removeAttribute(API.PARAM_LOGO_IMAGE);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            forwardToEntryPoint(request, response, this.guiEntryPoint);
        }
    }

    private void forwardToEntryPoint(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String entryPoint) throws ServletException, IOException {

        final RequestDispatcher dispatcher = request
                .getServletContext()
                .getRequestDispatcher(entryPoint);

        dispatcher.forward(request, response);
    }

    private String extractInstitutionalEndpoint(final HttpServletRequest request) {
        final String requestURI = request.getRequestURI();

        log.debug("Trying to verify insitution from requested entrypoint url: {}", requestURI);

        final String instPrefix = requestURI.replaceAll("/", "");
        if (StringUtils.isBlank(instPrefix)) {
            return null;
        }

        return instPrefix;
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

            final ResponseEntity<String> exchange = restTemplate
                    .exchange(
                            this.webserviceURIService.getURIBuilder()
                                    .path(API.INFO_ENDPOINT + API.INSTITUTIONAL_LOGO_PATH)
                                    .toUriString(),
                            HttpMethod.GET,
                            HttpEntity.EMPTY,
                            String.class,
                            institutionalEndpoint);

            if (exchange.getStatusCodeValue() == HttpStatus.OK.value()) {
                return exchange.getBody();
            } else {
                log.error("Failed to verify insitution from requested entrypoint url: {}, response: {}",
                        institutionalEndpoint,
                        exchange);
            }
        } catch (final Exception e) {
            log.warn("Failed to verify insitution from requested entrypoint url: {}",
                    institutionalEndpoint,
                    e);
        }

        return null;
    }

    /** TODO this seems not to work as expected. Different Theme is only possible in RAP on different
     * entry-points and since entry-points are statically defined within the RAPConficuration
     * there is no possibility to apply them dynamically within an institution so far.
     *
     * @param institutionalEndpoint
     * @return */
//    private boolean initInstitutionalBasedThemeEntryPoint(final String institutionalEndpoint) {
//        try {
//            final ApplicationContextImpl appContext = (ApplicationContextImpl) RWT.getApplicationContext();
//            final Map<String, String> properties = new HashMap<>();
//            properties.put(WebClient.THEME_ID, "sms");
//            appContext.getEntryPointManager().register(
//                    institutionalEndpoint,
//                    new RAPSpringEntryPointFactory(),
//                    properties);
//
//            return true;
//        } catch (final Exception e) {
//            log.warn("Failed to dynamically set entry point for institution: {}", institutionalEndpoint, e);
//            return false;
//        }
//    }

}