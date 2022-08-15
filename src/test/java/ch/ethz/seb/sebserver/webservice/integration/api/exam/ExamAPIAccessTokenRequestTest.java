/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.exam;

import static org.junit.Assert.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class ExamAPIAccessTokenRequestTest extends ExamAPIIntegrationTester {

    @Test
    public void testRequestAccessToken() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);
    }

    @Test
    public void testAccessTokenResponse() throws Exception {
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("scope", "read write");

        final ResultActions result = this.mockMvc.perform(post("/oauth/token")
                .params(params)
                .with(httpBasic("test", "test"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));

        final String resultString = result.andReturn().getResponse().getContentAsString();
        final JacksonJsonParser jsonParser = new JacksonJsonParser();
        final Object expiry = jsonParser.parseMap(resultString).get("expires_in");
        assertNotNull(expiry);
    }

}
