/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.exam;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import ch.ethz.seb.sebserver.SEBServer;
import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebClientConfigService;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.AdminAPIClientDetails;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.WebClientDetailsService;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.WebserviceResourceConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = { SEBServer.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class ExamAPIIntegrationTester {

    @Value("${sebserver.webservice.api.exam.endpoint.v1}")
    protected String endpoint;

    @Autowired
    protected WebApplicationContext wac;
    @Autowired
    protected JSONMapper jsonMapper;
    @Autowired
    protected FilterChainProxy springSecurityFilterChain;

    protected MockMvc mockMvc;

    @Autowired
    protected CacheManager cacheManager;

    @MockBean
    public WebClientDetailsService webClientDetailsService;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
                .addFilter(this.springSecurityFilterChain).build();
        Mockito.when(this.webClientDetailsService.loadClientByClientId(Mockito.anyString())).thenReturn(
                getForExamClientAPI());

        // clear all caches before a test
        this.cacheManager.getCacheNames()
                .stream()
                .map(name -> this.cacheManager.getCache(name))
                .forEach(cache -> cache.clear());
    }

    protected ClientDetails getForExamClientAPI() {
        final BaseClientDetails baseClientDetails = new BaseClientDetails(
                "test",
                WebserviceResourceConfiguration.EXAM_API_RESOURCE_ID,
                null,
                "client_credentials",
                "");
        baseClientDetails.setScope(Collections.emptySet());
        baseClientDetails
                .setClientSecret(ExamAPIIntegrationTester.this.clientPasswordEncoder.encode("test"));
        return baseClientDetails;
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
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));

        final String resultString = result.andReturn().getResponse().getContentAsString();

        final JacksonJsonParser jsonParser = new JacksonJsonParser();
        return jsonParser.parseMap(resultString).get("access_token").toString();
    }

    protected MockHttpServletResponse createConnection(
            final String accessToken,
            final Long institutionId,
            final Long examId) throws Exception {

        final MockHttpServletRequestBuilder builder = get(this.endpoint + "/handshake")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE);

        String body = "";

        if (institutionId != null) {
            body += "institutionId=" + institutionId;
        }
        if (examId != null) {
            body += "&examId=" + examId;
        }

        builder.content(body);

        final ResultActions result = this.mockMvc.perform(builder)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));

        return result.andReturn().getResponse();
    }

    protected MockHttpServletResponse updateConnection(
            final String accessToken,
            final String connectionToken,
            final Long examId,
            final String userSessionId) throws Exception {

        return updateConnection(accessToken, connectionToken, examId, userSessionId, false);
    }

    protected MockHttpServletResponse establishConnection(
            final String accessToken,
            final String connectionToken,
            final Long examId,
            final String userSessionId) throws Exception {

        return updateConnection(accessToken, connectionToken, examId, userSessionId, true);
    }

    protected MockHttpServletResponse updateConnection(
            final String accessToken,
            final String connectionToken,
            final Long examId,
            final String userSessionId,
            final boolean establish) throws Exception {

        final MockHttpServletRequestBuilder builder = (establish)
                ? put(this.endpoint + "/handshake")
                        .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .header("Authorization", "Bearer " + accessToken)
                        .header(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken)
                        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                : patch(this.endpoint + "/handshake")
                        .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .header("Authorization", "Bearer " + accessToken)
                        .header(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken)
                        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE);

        String body = "";
        if (examId != null) {
            body += "examId=" + examId;
        }
        if (userSessionId != null) {
            if (!StringUtils.isBlank(body)) {
                body += "&";
            }

            body += API.EXAM_API_USER_SESSION_ID + "=" + userSessionId;
        }
        builder.content(body);

        final ResultActions result = this.mockMvc.perform(builder);

        return result.andReturn().getResponse();
    }

    protected MockHttpServletResponse closeConnection(final String accessToken, final String connectionToken)
            throws Exception {
        final MockHttpServletRequestBuilder builder = delete(this.endpoint + "/handshake")
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header("Authorization", "Bearer " + accessToken)
                .header(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE);
        final ResultActions result = this.mockMvc.perform(builder);
        return result.andReturn().getResponse();
    }

    protected MockHttpServletResponse sendPing(
            final String accessToken,
            final String connectionToken,
            final int num) throws Exception {
        final MockHttpServletRequestBuilder builder = post(this.endpoint + API.EXAM_API_PING_ENDPOINT)
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header("Authorization", "Bearer " + accessToken)
                .header(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE);

        final String body = API.EXAM_API_PING_TIMESTAMP + "=" + DateTime.now(DateTimeZone.UTC).getMillis()
                + "&" + API.EXAM_API_PING_NUMBER + "=" + num;
        builder.content(body);

        final ResultActions result = this.mockMvc.perform(builder);
        return result.andReturn().getResponse();
    }

    protected MockHttpServletResponse sendEvent(
            final String accessToken,
            final String connectionToken,
            final String type,
            final long timestamp,
            final double value,
            final String text) throws Exception {

        final MockHttpServletRequestBuilder builder = post(this.endpoint + API.EXAM_API_EVENT_ENDPOINT)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("Authorization", "Bearer " + accessToken)
                .header(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE);

        final String body = "{ \"type\": \"%s\", \"timestamp\": %s, \"numericValue\": %s, \"text\": \"%s\" }";
        builder.content(String.format(body, type, timestamp, value, text));
        final ResultActions result = this.mockMvc.perform(builder);
        return result.andReturn().getResponse();
    }

    protected MockHttpServletResponse getExamConfig(
            final String accessToken,
            final String connectionToken,
            final Long examId) throws Exception {

        final MockHttpServletRequestBuilder builder = get(this.endpoint + API.EXAM_API_CONFIGURATION_REQUEST_ENDPOINT)
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header("Authorization", "Bearer " + accessToken)
                .header(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken)
                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        if (examId != null) {
            builder.content("examId=" + examId);
        }

        final ResultActions result = this.mockMvc
                .perform(builder)
                .andDo(MvcResult::getAsyncResult);

        return result.andReturn().getResponse();
    }

    @Autowired
    AdminAPIClientDetails adminClientDetails;
    @Autowired
    SebClientConfigService sebClientConfigService;
    @Autowired
    @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME)
    private PasswordEncoder clientPasswordEncoder;

}
