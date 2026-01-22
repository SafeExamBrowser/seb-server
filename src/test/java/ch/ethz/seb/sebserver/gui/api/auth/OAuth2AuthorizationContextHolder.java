/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.api.auth;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.user.LoginForward;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.OAuthRestTemplate;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Lazy
@Component
public class OAuth2AuthorizationContextHolder implements AuthorizationContextHolder {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthorizationContextHolder.class);

    private static final String CONTEXT_HOLDER_ATTRIBUTE = "CONTEXT_HOLDER_ATTRIBUTE";

    private final String guiClientId;
    private final String guiClientSecret;
    private final WebserviceURIService webserviceURIService;
    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;

    private final OAuth2AuthorizationContext context;

    @Autowired
    public OAuth2AuthorizationContextHolder(
            @Value("${sebserver.webservice.api.admin.clientId}") final String guiClientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String guiClientSecret,
            final WebserviceURIService webserviceURIService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService) {

        this.guiClientId = guiClientId;
        this.guiClientSecret = guiClientSecret;
        this.webserviceURIService = webserviceURIService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        context = new OAuth2AuthorizationContext(
                guiClientId,
                guiClientSecret,
                webserviceURIService,
                clientHttpRequestFactoryService.getClientHttpRequestFactory().getOrThrow());
    }

    @Override
    public WebserviceURIService getWebserviceURIService() {
        return this.webserviceURIService;
    }

    @Override
    public SEBServerAuthorizationContext getAuthorizationContext() {
        return context;
    }

//    @Override
//    public SEBServerAuthorizationContext getAuthorizationContext(final HttpSession session) {
//        if (log.isTraceEnabled()) {
//            log.trace("Trying to get OAuth2AuthorizationContext from HttpSession: {}", session.getId());
//        }
//
//        OAuth2AuthorizationContext context =
//                (OAuth2AuthorizationContext) session.getAttribute(CONTEXT_HOLDER_ATTRIBUTE);
//
//        if (context == null || !context.valid) {
//            log.debug(
//                    "OAuth2AuthorizationContext for HttpSession: {} is not present or is invalid. "
//                            + "Create new OAuth2AuthorizationContext for this session",
//                    session.getId());
//
//            final ClientHttpRequestFactory clientHttpRequestFactory = this.clientHttpRequestFactoryService
//                    .getClientHttpRequestFactory()
//                    .getOrThrow();
//
//            context = new OAuth2AuthorizationContext(
//                    this.guiClientId,
//                    this.guiClientSecret,
//                    this.webserviceURIService,
//                    clientHttpRequestFactory);
//
//            session.setAttribute(CONTEXT_HOLDER_ATTRIBUTE, context);
//        }
//
//        return context;
//    }

//    private static final class DisposableOAuth2RestTemplate extends OAuthRestTemplate {
//
//        private boolean enabled = true;
//
//        public DisposableOAuth2RestTemplate(final OAuth2ProtectedResourceDetails resource) {
//            super(
//                    resource,
//                    new DefaultOAuth2ClientContext(new DefaultAccessTokenRequest()));
//        }
//
//        @Override
//        protected <T> T doExecute(
//                final URI url,
//                final HttpMethod method,
//                final RequestCallback requestCallback,
//                final ResponseExtractor<T> responseExtractor) throws RestClientException {
//
//            if (this.enabled) {
//                return super.doExecute(url, method, requestCallback, responseExtractor);
//            } else {
//                throw new DisposedOAuth2RestTemplateException(
//                        "Error: Forbidden execution call on disabled DisposableOAuth2RestTemplate");
//            }
//        }
//    }

    public static final class ClientSettingsProvider implements OAuthRestTemplate.ClientSettingsProvider {
        private final String clientId;
        private final CharSequence clientSecret;
        private String username;
        private CharSequence password;
        private final String scopes;

        public ClientSettingsProvider(
                String clientId,
                CharSequence clientSecret,
                String scopes) {

            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.scopes = scopes;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setPassword(CharSequence password) {
            this.password = password;
        }

        public String getClientId() {
            return clientId;
        }

        public CharSequence getClientSecret() {
            return clientSecret;
        }

        public String getScopes() {
            return scopes;
        }

        public CharSequence getPassword() {
            return password;
        }

        public String getUsername() {
            return username;
        }

        @Override
        public String getBasicAuthHeader() {
            final String auth = clientId + Constants.COLON + clientSecret;
            final String authEncoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            return "Basic " + authEncoded;
        }

        @Override
        public String getOAuthBody() {
            String body = "grant_type=password&username=" + username + "&password=" + password;
            if (scopes != null) {
                body += "&scope=" + scopes;
            }

            return body;
        }
    }

    private static final class OAuth2AuthorizationContext implements SEBServerAuthorizationContext {

        private boolean valid = true;

        private final ClientHttpRequestFactory clientHttpRequestFactory;
        //private final ResourceOwnerPasswordResourceDetails resource;
        private final ClientSettingsProvider clientSettingsProvider;
        private final OAuthRestTemplate restTemplate;
        private final String revokeTokenURI;
        private final String currentUserURI;
        private final String loginLogURI;
        private final String logoutLogURI;
        private final String jwtTokenVerificationURI;

        private Result<UserInfo> loggedInUser = null;
        private LoginForward loginForward = null;

        OAuth2AuthorizationContext(
                final String guiClientId,
                final String guiClientSecret,
                final WebserviceURIService webserviceURIService,
                final ClientHttpRequestFactory clientHttpRequestFactory) {

            this.clientHttpRequestFactory = clientHttpRequestFactory;
            this.clientSettingsProvider = new ClientSettingsProvider(
                    guiClientId,
                    guiClientSecret,
                    "read write"
            );

//            this.resource = new ResourceOwnerPasswordResourceDetails();
//            this.resource.setAccessTokenUri(webserviceURIService.getOAuthTokenURI());
//            this.resource.setClientId(guiClientId);
//            this.resource.setClientSecret(guiClientSecret);
//            this.resource.setGrantType(API.GRANT_TYPE_PASSWORD);
//            this.resource.setScope(API.RW_SCOPES);

//            this.restTemplate = new DisposableOAuth2RestTemplate(this.resource);
//            this.restTemplate.setRequestFactory(clientHttpRequestFactory);
//            this.restTemplate.setErrorHandler(new ErrorHandler(this.resource));
//            this.restTemplate
//                    .getMessageConverters()
//                    .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

            RestTemplate rest = new RestTemplate();
            rest.setRequestFactory(clientHttpRequestFactory);
            this.restTemplate = new OAuthRestTemplate(
                    webserviceURIService.getWebserviceServerAddress(),
                    API.OAUTH_TOKEN_ENDPOINT,
                    this.clientSettingsProvider,
                    rest);

            this.revokeTokenURI = webserviceURIService.getOAuthRevokeTokenURI();
            this.currentUserURI = webserviceURIService.getCurrentUserRequestURI();
            this.loginLogURI = webserviceURIService.getLoginLogPostURI();
            this.logoutLogURI = webserviceURIService.getLogoutLogPostURI();
            this.jwtTokenVerificationURI = webserviceURIService.getJWTTokenVerificationURI();
        }

        @Override
        public boolean isValid() {
            return this.valid;
        }

        @Override
        public boolean isLoggedIn() {
            try {
                if (!valid) {
                    return false;
                }

                final ResponseEntity<String> forEntity =
                        this.restTemplate.getForEntity(this.currentUserURI, String.class);
                
                if (forEntity.getStatusCode() != HttpStatus.OK) {
                    log.warn("Failed to verify user login on webservice: {}", forEntity.getBody());
                    return false;
                }
            } catch (final Exception e) {
                log.error("Failed to verify logged in user: {}", e.getMessage());
                return false;
            }

            return true;
        }

        @Override
        public CharSequence getUserPassword() {
            if (isLoggedIn()) {
                return this.clientSettingsProvider.getPassword();
            }
            return null;
        }

        @Override
        public LoginForward getLoginForward() {
            return loginForward;
        }

        @Override
        public boolean login(final String username, final CharSequence password) {
            if (!this.valid) {
                return false;
            }

            this.clientSettingsProvider.setUsername(username);
            this.clientSettingsProvider.setPassword(Utils.toString(password));

            log.debug("Trying to login for user: {}", username);

            try {
                this.restTemplate.getAccessToken();
                log.debug("Got token for user: {}", username);
                this.loggedInUser = getLoggedInUser();
                // check valid login
                if (!this.loggedInUser.hasError()) {
                    final UserInfo userInfo = loggedInUser.get();
                    // TODO check directLogin here instead of role
                    if (!BooleanUtils.isTrue(userInfo.directLogin)) {
                        log.warn("No direct Login for this account available: {}", username);
                        throw new AccessDeniedException("No direct Login for this account available");
                    }
                }
                // call log login on webservice API
                try {
                    final ResponseEntity<Void> response = this.restTemplate.exchange(
                            this.loginLogURI,
                            HttpMethod.POST,
                            null,
                            new HttpHeaders(),
                            Void.class);
                    if (response.getStatusCode() != HttpStatus.OK) {
                        log.error("Failed to log login: {}", response.getStatusCode());
                    }
                } catch (final Exception e) {
                    log.error("Failed to log login: {}", e.getMessage());
                }

                return true;
            } catch (AccessDeniedException e) {
                log.info("Access Denied for user: {}", username);
                return false;
            }
        }

        @Override
        public boolean autoLogin(final String oneTimeToken) {
            return false;
        }

        @Override
        public boolean logout() {
            restTemplate.clearToken();
            valid = false;
            return true;
        }

        @Override
        public OAuthRestTemplate getRestTemplate() {
            return this.restTemplate;
        }

        @Override
        public void refreshUser(final UserInfo userInfo) {
            restTemplate.clearToken();
            if (!userInfo.username.equals(getLoggedInUser().get().username)) {
                // Set new username to be able to request new access token
                this.clientSettingsProvider.setUsername(userInfo.username);
            }
            restTemplate.getAccessToken();
        }

        @Override
        public Result<UserInfo> getLoggedInUser() {
            if (this.loggedInUser != null) {
                return this.loggedInUser;
            }

            log.debug("Request logged in User from SEBserver web-service API");

            try {
                if (isValid() && isLoggedIn()) {
                    final ResponseEntity<UserInfo> response =
                            this.restTemplate.getForEntity(this.currentUserURI, UserInfo.class);
                    if (response.getStatusCode() == HttpStatus.OK) {
                        this.loggedInUser = Result.of(response.getBody());
                        return this.loggedInUser;
                    } else {
                        log.error("Unexpected error response: {}", response);
                        return Result.ofError(new IllegalStateException(
                                "Http Request responded with status: " + response.getStatusCode()));
                    }
                } else {
                    return Result.ofError(
                            new IllegalStateException("Logged in User requested on invalid or not logged in "));
                }
            } catch (final AccessDeniedException ade) {
                log.error("Acccess denied while trying to request logged in User from API", ade);
                return Result.ofError(ade);
            } catch (final Exception e) {
                log.error("Unexpected error while trying to request logged in User from API", e);
                return Result.ofError(
                        new RuntimeException("Unexpected error while trying to request logged in User from API", e));
            }
        }

        @Override
        public boolean hasRole(final UserRole role) {
            if (!isValid() || !isLoggedIn()) {
                return false;
            }

            return getLoggedInUser()
                    .getOrThrow().roles
                            .contains(role.name());
        }

//        private static final class ErrorHandler extends OAuth2ErrorHandler {
//            private ErrorHandler(final OAuth2ProtectedResourceDetails resource) {
//                super(resource);
//            }
//
//            @Override
//            public boolean hasError(final ClientHttpResponse response) throws IOException {
//                try {
//                    final HttpStatus statusCode = HttpStatus.resolve(response.getRawStatusCode());
//                    return (statusCode != null && statusCode.series().equals(HttpStatus.Series.SERVER_ERROR));
//                } catch (final Exception e) {
//                    log.error("Unexpected: ", e);
//                    return super.hasError(response);
//                }
//            }
//        }
    }
}
