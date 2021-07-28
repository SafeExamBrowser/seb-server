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
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class OlatLmsRestTemplate extends RestTemplate {

  private static final Logger log = LoggerFactory.getLogger(OlatLmsRestTemplate.class);

  public String token;

  public OlatLmsRestTemplate(ClientCredentialsResourceDetails details) {
        super();

        // Authenticate with OLAT and store the received X-OLAT-TOKEN
        final String authUrl = String.format("%s%s?password=%s",
                details.getAccessTokenUri(),
                details.getClientId(),
                details.getClientSecret());
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("accept", "application/json");
        ResponseEntity<String> response = this.getForEntity(authUrl, String.class);
        HttpHeaders responseHeaders = response.getHeaders();
        log.debug("OLAT Auth Response Headers: {}", responseHeaders);
        token = responseHeaders.getFirst("X-OLAT-TOKEN");

        // Add X-OLAT-TOKEN request header to every request done using this RestTemplate
        this.getInterceptors().add(new ClientHttpRequestInterceptor(){
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
              request.getHeaders().set("X-OLAT-TOKEN", token);
              request.getHeaders().set("accept", "application/json");
              HttpHeaders responseHeaders = response.getHeaders();
              return execution.execute(request, body);
            }
        });
  }
}

