/*
 * Copyright (c) 2024 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver;

import ch.ethz.seb.sebserver.gbl.api.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class OAuth2ClientCredentialsGrantProvider implements AuthenticationProvider  {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ClientCredentialsGrantProvider.class);
    
    private final OAuth2AuthorizationService authorizationService;
    private final String lmsClientName;
    private OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private Consumer<OAuth2ClientCredentialsAuthenticationContext> authenticationValidator = new OAuth2ClientCredentialsAuthenticationValidator();

    public OAuth2ClientCredentialsGrantProvider(
            final OAuth2AuthorizationService authorizationService,
            final String lmsClientName) {
        this.authorizationService = authorizationService;
        this.lmsClientName = lmsClientName;
    }

    public void init(HttpSecurity http) {
        tokenGenerator = (DelegatingOAuth2TokenGenerator) http.getSharedObject(OAuth2TokenGenerator.class);
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2ClientCredentialsAuthenticationToken clientCredentialsAuthentication = (OAuth2ClientCredentialsAuthenticationToken)authentication;
        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(clientCredentialsAuthentication);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
        if (logger.isTraceEnabled()) {
            logger.trace("Retrieved registered client");
        }

        if (!registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Invalid request: requested grant_type is not allowed for registered client {}", registeredClient.getId());
            }

            throw new OAuth2AuthenticationException("unauthorized_client");
        } else {
            OAuth2ClientCredentialsAuthenticationContext authenticationContext = OAuth2ClientCredentialsAuthenticationContext.with(clientCredentialsAuthentication).registeredClient(registeredClient).build();
            this.authenticationValidator.accept(authenticationContext);
            Set<String> authorizedScopes = new LinkedHashSet<>(clientCredentialsAuthentication.getScopes());
            final String clientName = authenticationContext.getRegisteredClient().getClientId();
            if (Objects.equals(clientName, this.lmsClientName)) {
                authorizedScopes.add(API.LMS_API_SCOPE_NAME);
            } else {
                authorizedScopes.add(API.SEB_API_SCOPE_NAME);
            }

            if (logger.isTraceEnabled()) {
                logger.trace("Validated token request parameters");
            }

            OAuth2TokenContext tokenContext = ((DefaultOAuth2TokenContext.Builder)((DefaultOAuth2TokenContext.Builder)((DefaultOAuth2TokenContext.Builder)((DefaultOAuth2TokenContext.Builder) DefaultOAuth2TokenContext
                    .builder()
                    .registeredClient(registeredClient))
                    .principal(clientPrincipal))
                    .authorizationServerContext(AuthorizationServerContextHolder.getContext()))
                    .authorizedScopes(authorizedScopes))
                    .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .authorizationGrant(clientCredentialsAuthentication)
                    .build();
            
            OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);
            if (generatedAccessToken == null) {
                OAuth2Error error = new OAuth2Error("server_error", "The token generator failed to generate the access token.", "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2");
                throw new OAuth2AuthenticationException(error);
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Generated access token");
                }

                OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                        .withRegisteredClient(registeredClient)
                        .principalName(clientPrincipal.getName())
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .authorizedScopes(authorizedScopes);
                
                OAuth2AccessToken accessToken = accessToken(authorizationBuilder, generatedAccessToken, tokenContext);
                OAuth2Authorization authorization = authorizationBuilder.build();
                this.authorizationService.save(authorization);
                if (logger.isTraceEnabled()) {
                    logger.trace("Saved authorization");
                    logger.trace("Authenticated token request");
                }

                return new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken);
            }
        }
    }

    public boolean supports(Class<?> authentication) {
        return OAuth2ClientCredentialsAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public void setAuthenticationValidator(Consumer<OAuth2ClientCredentialsAuthenticationContext> authenticationValidator) {
        Assert.notNull(authenticationValidator, "authenticationValidator cannot be null");
        this.authenticationValidator = authenticationValidator;
    }

    static OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(Authentication authentication) {
        OAuth2ClientAuthenticationToken clientPrincipal = null;
        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken)authentication.getPrincipal();
        }

        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        } else {
            throw new OAuth2AuthenticationException("invalid_client");
        }
    }

    static <T extends OAuth2Token> OAuth2AccessToken accessToken(OAuth2Authorization.Builder builder, T token, OAuth2TokenContext accessTokenContext) {
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token.getTokenValue(), token.getIssuedAt(), token.getExpiresAt(), accessTokenContext.getAuthorizedScopes());
        OAuth2TokenFormat accessTokenFormat = accessTokenContext.getRegisteredClient().getTokenSettings().getAccessTokenFormat();
        builder.token(accessToken, (metadata) -> {
            if (token instanceof ClaimAccessor claimAccessor) {
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claimAccessor.getClaims());
            }

            metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, false);
            metadata.put(OAuth2TokenFormat.class.getName(), accessTokenFormat.getValue());
        });
        return accessToken;
    }
}
