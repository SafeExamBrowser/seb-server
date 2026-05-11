/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.ans;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

public class AnsPersonalRestTemplate extends RestTemplate {
    private static final Logger log = LoggerFactory.getLogger(AnsPersonalRestTemplate.class);
    public String token;

    public AnsPersonalRestTemplate(final String token) {
        super();
        this.token = token;
        this.getInterceptors().add(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
                    final ClientHttpRequestExecution execution) throws IOException {

                request.getHeaders().set("Authorization", "Bearer " + AnsPersonalRestTemplate.this.token);
                //log.debug("Matching curl: curl -X GET {} -H  'accept: application/json' -H  'Authorization: Bearer {}'", request.getURI(), token);
                final ClientHttpResponse response = execution.execute(request, body);
                log.debug("Response Headers      : {}", response.getHeaders());
                return response;
            }
        });
    }
}
