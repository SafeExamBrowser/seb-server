/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import ch.ethz.seb.sebserver.SEBServer;
import ch.ethz.seb.sebserver.gbl.JSONMapper;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SEBServer.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ExamAPIIntegrationTest {

    @Value("${sebserver.webservice.api.exam.endpoint}")
    private String endpoint;

    @Autowired
    protected WebApplicationContext wac;
    @Autowired
    protected JSONMapper jsonMapper;
    @Autowired
    protected FilterChainProxy springSecurityFilterChain;

    protected MockMvc mockMvc;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
                .addFilter(this.springSecurityFilterChain).build();
    }

    protected String obtainAccessToken(
            final String clientId,
            final String clientSecret,
            final String scope) throws Exception {

        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", clientId);
        params.add("scope", scope);

        final ResultActions result = this.mockMvc.perform(post("/oauth/token")
                .params(params)
                .with(httpBasic(clientId, clientSecret))
                .accept("application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"));

        final String resultString = result.andReturn().getResponse().getContentAsString();

        final JacksonJsonParser jsonParser = new JacksonJsonParser();
        return jsonParser.parseMap(resultString).get("access_token").toString();
    }

    @Test
    public void getHello_givenNoToken_thenUnauthorized() throws Exception {
        this.mockMvc.perform(get(this.endpoint + "/hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void get_same_token_for_same_scope() throws Exception {
        final String accessToken1 = obtainAccessToken("test", "test", "testScope");
        final String accessToken2 = obtainAccessToken("test", "test", "testScope");

        assertEquals(accessToken1, accessToken2);
    }

    @Test
    public void get_different_tokens_for_different_scopes() throws Exception {
        final String accessToken1 = obtainAccessToken("test", "test", "testScope1");
        final String accessToken2 = obtainAccessToken("test", "test", "testScope2");

        assertNotEquals(accessToken1, accessToken2);
    }

    @Test
    public void getHello_givenToken_thenOK() throws UnsupportedEncodingException, Exception {
        final String accessToken = obtainAccessToken("test", "test", "testScope");
        final String contentAsString = this.mockMvc.perform(get(this.endpoint + "/hello")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals("Hello From Exam-Web-Service", contentAsString);
    }

}
