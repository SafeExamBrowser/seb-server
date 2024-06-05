/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.web.client.RestTemplate;

public class OlatLmsRestTemplate extends RestTemplate {

    private static final Logger log = LoggerFactory.getLogger(OlatLmsRestTemplate.class);

    private String token;
    private ClientCredentialsResourceDetails details;

    public void testAuthentication() {
        if (this.token == null) {
            authenticate();
        }
    }

    public OlatLmsRestTemplate(final ClientCredentialsResourceDetails details) {
        super();
        this.details = details;

        // Add X-OLAT-TOKEN request header to every request done using this RestTemplate
        this.getInterceptors().add(new ClientHttpRequestInterceptor() {
            @Override
            public synchronized ClientHttpResponse intercept(
                    final HttpRequest request,
                    final byte[] body,
                    final ClientHttpRequestExecution execution) throws IOException {

                try {

                    // if there's no token, authenticate first
                    if (OlatLmsRestTemplate.this.token == null) {
                        authenticate();
                    }
                    // when authenticating, just do a normal call
                    else if (OlatLmsRestTemplate.this.token.equals("authenticating")) {

                        if (log.isDebugEnabled()) {
                            log.debug("OLAT [authentication call]: URL {}", request.getURI());
                        }

                        return execution.execute(request, body);
                    }

                    // otherwise, add the X-OLAT-TOKEN
                    request.getHeaders().set("accept", "application/json");
                    request.getHeaders().set("X-OLAT-TOKEN", OlatLmsRestTemplate.this.token);

                    if (log.isDebugEnabled()) {
                        log.debug("OLAT [regular API call]: URL {}", request.getURI());
                    }

                    ClientHttpResponse response = execution.execute(request, body);

                    if (log.isDebugEnabled()) {
                        log.debug("OLAT [regular API call response] {} Headers: {}",
                                response.getStatusCode(),
                                response.getHeaders());
                    }

                    // If we get a 401, re-authenticate and try once more
                    if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {

                        authenticate();
                        request.getHeaders().set("X-OLAT-TOKEN", OlatLmsRestTemplate.this.token);

                        if (log.isDebugEnabled()) {
                            log.debug("OLAT [retry API call]: URL {}", request.getURI());
                        }

                        response.close();
                        response = execution.execute(request, body);

                        if (log.isDebugEnabled()) {
                            log.debug("OLAT [retry API call response] {} Headers: {}",
                                    response.getStatusCode(),
                                    response.getHeaders());
                        }
                    }
                    return response;

                } catch (final Exception e) {
                    // TODO find a way to better deal with Olat temporary unavailability
                    log.error("Unexpected error: {}", e.getMessage());
                    throw e;
                }
            }
        });
    }

    private void authenticate() {
        // Authenticate with OLAT and store the received X-OLAT-TOKEN
        this.token = "authenticating";
        final String authUrl = this.details.getAccessTokenUri();
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("username", this.details.getClientId());
        parameters.put("password", this.details.getClientSecret());
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("content-type", "application/json");
        final HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(httpHeaders);
        try {
            final ResponseEntity<String> response = this.exchange(authUrl, HttpMethod.GET, requestEntity, String.class, parameters);
            final HttpHeaders responseHeaders = response.getHeaders();

            if (log.isDebugEnabled()) {
                log.debug("OLAT [authenticated] {} Headers: {}", response.getStatusCode(), responseHeaders);
            }

            this.token = responseHeaders.getFirst("X-OLAT-TOKEN");
        } catch (final Exception e) {
            this.token = null;
            throw e;
        }
    }

}
