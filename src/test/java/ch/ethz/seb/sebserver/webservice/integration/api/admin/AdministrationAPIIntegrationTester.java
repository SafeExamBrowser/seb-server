/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.SEBServer;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Entity;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = "file.encoding=UTF-8",
        classes = SEBServer.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class AdministrationAPIIntegrationTester {

    @Value("${sebserver.webservice.api.admin.clientId}")
    protected String clientId;
    @Value("${sebserver.webservice.api.admin.clientSecret}")
    protected String clientSecret;
    @Value("${sebserver.webservice.api.admin.endpoint}")
    protected String endpoint;

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

    protected String obtainAccessToken(final String username, final String password) throws Exception {
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "password");
        params.add("client_id", this.clientId);
        params.add("username", username);
        params.add("password", password);

        final ResultActions result = this.mockMvc.perform(post("/oauth/token")
                .params(params)
                .with(httpBasic(this.clientId, this.clientSecret))
                .accept("application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"));

        final String resultString = result.andReturn().getResponse().getContentAsString();

        final JacksonJsonParser jsonParser = new JacksonJsonParser();
        return jsonParser.parseMap(resultString).get("access_token").toString();
    }

    protected String getSebAdminAccess() throws Exception {
        return obtainAccessToken("admin", "admin");
    }

    protected String getAdminInstitution1Access() throws Exception {
        return obtainAccessToken("inst1Admin", "admin");
    }

    protected String getAdminInstitution2Access() throws Exception {
        return obtainAccessToken("inst2Admin", "admin");
    }

    protected String getExamAdmin1() throws Exception {
        return obtainAccessToken("examAdmin1", "admin");
    }

    protected RestAPITestHelper restAPITestHelper() {
        return new RestAPITestHelper();
    }

    protected class RestAPITestHelper {

        private String path = "";
        private final Map<String, String> queryAttrs = new HashMap<>();
        private String accessToken;
        private HttpStatus expectedStatus;
        private HttpMethod httpMethod = HttpMethod.GET;
        private MediaType contentType = MediaType.APPLICATION_FORM_URLENCODED;
        private String body = null;

        public RestAPITestHelper withAccessToken(final String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public RestAPITestHelper withPath(final String path) {
            if (path == null) {
                return this;
            }
            this.path += (path.startsWith("/")) ? path : "/" + path;
            return this;
        }

        public RestAPITestHelper withAttribute(final String name, final String value) {
            this.queryAttrs.put(name, value);
            return this;
        }

        public RestAPITestHelper withExpectedStatus(final HttpStatus expectedStatus) {
            this.expectedStatus = expectedStatus;
            return this;
        }

        public RestAPITestHelper withMethod(final HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public RestAPITestHelper withBodyJson(final Object object) throws Exception {
            this.contentType = MediaType.APPLICATION_JSON;
            this.body = AdministrationAPIIntegrationTester.this.jsonMapper.writeValueAsString(object);
            return this;
        }

        public void checkStatus() throws Exception {
            this.getAsString();
        }

        public String getAsString() throws Exception {
            final ResultActions action = AdministrationAPIIntegrationTester.this.mockMvc
                    .perform(requestBuilder());

            if (this.expectedStatus != null) {
                action.andExpect(status().is(this.expectedStatus.value()));
            }

            return action
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
        }

        public <T> T getAsObject(final TypeReference<T> ref) throws Exception {
            final ResultActions action = AdministrationAPIIntegrationTester.this.mockMvc
                    .perform(requestBuilder());
            if (this.expectedStatus != null) {
                action.andExpect(status().is(this.expectedStatus.value()));
            }

            return AdministrationAPIIntegrationTester.this.jsonMapper.readValue(
                    action
                            .andReturn()
                            .getResponse()
                            .getContentAsString(),
                    ref);
        }

        private RequestBuilder requestBuilder() {
            MockHttpServletRequestBuilder builder = get(getFullPath());
            switch (this.httpMethod) {
                case GET:
                    builder = get(getFullPath());
                    break;
                case POST:
                    builder = post(getFullPath());
                    break;
                case PUT:
                    builder = put(getFullPath());
                    break;
                case DELETE:
                    builder = delete(getFullPath());
                    break;
                case PATCH:
                    builder = patch(getFullPath());
                    break;
                default:
                    get(getFullPath());
                    break;
            }
            builder.header("Authorization", "Bearer " + this.accessToken);

            if (this.contentType != null) {
                builder.contentType(this.contentType);
            }
            if (this.body != null) {
                builder.content(this.body);
            }

            return builder;
        }

        private String getFullPath() {
            final StringBuilder sb = new StringBuilder();
            sb.append(AdministrationAPIIntegrationTester.this.endpoint);
            sb.append(this.path);
            if (!this.queryAttrs.isEmpty()) {
                sb.append("?");
                this.queryAttrs.entrySet()
                        .stream()
                        .reduce(
                                sb,
                                (buffer, entry) -> buffer.append(entry.getKey()).append("=").append(entry.getValue())
                                        .append("&"),
                                (sb1, sb2) -> sb1.append(sb2));
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        }
    }

    protected String getOrderedUUIDs(final Collection<? extends Entity> list) {
        final List<String> l = list
                .stream()
                .map(userInfo -> userInfo.getModelId())
                .collect(Collectors.toList());
        l.sort((s1, s2) -> s1.compareTo(s2));
        return l.toString();
    }

}
