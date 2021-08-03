/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.client.RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class OlatLmsRestTemplate extends RestTemplate {

    private static final Logger log = LoggerFactory.getLogger(OlatLmsRestTemplate.class);

    private String token;
    private ClientCredentialsResourceDetails details;

    public OlatLmsRestTemplate(ClientCredentialsResourceDetails details) {
        super();
        this.details = details;

        // Add X-OLAT-TOKEN request header to every request done using this RestTemplate
        this.getInterceptors().add(new ClientHttpRequestInterceptor(){
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                // if there's no token, authenticate first
                if (token == null) { authenticate(); }
                // when authenticating, just do a normal call
                else if (token.equals("authenticating")) { return execution.execute(request, body); }
                // otherwise, add the X-OLAT-TOKEN
                request.getHeaders().set("accept", "application/json");
                request.getHeaders().set("X-OLAT-TOKEN", token);
                ClientHttpResponse response = execution.execute(request, body);
                log.debug("OLAT [regular API call] {} Headers: {}", response.getStatusCode(), response.getHeaders());
                // If we get a 401, re-authenticate and try once more
                if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    authenticate();
                    request.getHeaders().set("X-OLAT-TOKEN", token);
                    response = execution.execute(request, body);
                    log.debug("OLAT [retry API call] {} Headers: {}", response.getStatusCode(), response.getHeaders());
                }
                return response;
            }
        });
    }

    private void authenticate() {
        // Authenticate with OLAT and store the received X-OLAT-TOKEN
        token = "authenticating";
        final String authUrl = String.format("%s%s?password=%s",
                details.getAccessTokenUri(),
                details.getClientId(),
                details.getClientSecret());
        try {
            final ResponseEntity<String> response = this.getForEntity(authUrl, String.class);
            final HttpHeaders responseHeaders = response.getHeaders();
            log.debug("OLAT [authenticate] {} Headers: {}", response.getStatusCode(), responseHeaders);
            token = responseHeaders.getFirst("X-OLAT-TOKEN");
        }
        catch (Exception e) {
            token = null;
            throw e;
        }
    }


}

