/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest;

import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpSession;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.SEBServer;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.RestCall;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.RestServiceImpl;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.auth.OAuth2AuthorizationContextHolder;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.auth.SEBServerAuthorizationContext;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.auth.WebserviceURIService;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.WebserviceInfoDAO;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = { "file.encoding=UTF-8" },
        classes = SEBServer.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class GuiIntegrationTest {

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
    @Autowired
    protected WebserviceInfoDAO webserviceInfoDAO;
    @Autowired
    protected WebserviceInfo webserviceInfo;

    protected MockMvc mockMvc;

    @Before
    public void setup() {
        //this.webserviceInfoDAO.unregister(this.webserviceInfo.getWebserviceUUID());
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
                .addFilter(this.springSecurityFilterChain).build();

    }

    protected OAuth2AuthorizationContextHolder getAuthorizationContextHolder() {

        final HttpSession sessionMock = Mockito.mock(HttpSession.class);
        final WebserviceURIService webserviceURIService = new WebserviceURIService(
                "http", "localhost", "8888", "/", this.endpoint);

        final ClientHttpRequestFactoryService clientHttpRequestFactoryService = Mockito
                .mock(ClientHttpRequestFactoryService.class);
        Mockito.when(clientHttpRequestFactoryService.getClientHttpRequestFactory()).thenReturn(
                Result.of(new HttpComponentsClientHttpRequestFactory()));

        return new OAuth2AuthorizationContextHolder(
                this.clientId,
                this.clientSecret,
                webserviceURIService,
                clientHttpRequestFactoryService) {

            private SEBServerAuthorizationContext authContext = null;

            @Override
            public SEBServerAuthorizationContext getAuthorizationContext() {
                if (this.authContext == null || !this.authContext.isValid()) {
                    this.authContext = super.getAuthorizationContext(sessionMock);
                }
                return this.authContext;
            }
        };
    }

    protected SEBServerAuthorizationContext getAuthorizationContext() {
        final SEBServerAuthorizationContext authorizationContext =
                getAuthorizationContextHolder().getAuthorizationContext();
        assertNotNull(authorizationContext);
        return authorizationContext;

    }

    protected OAuth2AuthorizationContextHolder login(final String name, final String pwd) {
        final OAuth2AuthorizationContextHolder authorizationContextHolder = getAuthorizationContextHolder();
        final SEBServerAuthorizationContext authorizationContext = authorizationContextHolder.getAuthorizationContext();
        if (authorizationContext.isLoggedIn()) {
            throw new IllegalStateException("another user is already logged in");
        }
        authorizationContext.login(name, pwd);
        return authorizationContextHolder;
    }

    protected RestServiceImpl createRestServiceForUser(
            final String username,
            final String password,
            final RestCall<?>... calls) {

        final OAuth2AuthorizationContextHolder authorizationContextHolder = login(username, password);
        final RestServiceImpl restService = new RestServiceImpl(
                authorizationContextHolder,
                new JSONMapper(),
                java.util.Arrays.asList(calls));
        return restService;
    }

}
