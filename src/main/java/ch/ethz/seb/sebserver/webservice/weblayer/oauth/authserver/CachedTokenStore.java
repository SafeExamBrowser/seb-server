/*
 * Copyright (c) 2024 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver;

import ch.ethz.seb.sebserver.gbl.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedTokenStore implements OAuth2AuthorizationService  {

    private static final Logger log = LoggerFactory.getLogger(CachedTokenStore.class);

    private final JdbcTokenStore jdbcTokenStore;

    private final Object REFRESH_TOKEN_LOCK = new Object();
    private final Map<String, OAuth2Authorization> authorizationsByAccessToken = new ConcurrentHashMap<>();

    public CachedTokenStore(DataSource dataSource) {
        this.jdbcTokenStore = new JdbcTokenStore(dataSource);
    }

    @Override
    public void save(final OAuth2Authorization authorization) {
        if (authorization == null) {
            return;
        }

        String aToken = null;
        String rToken = null;
        final OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        final OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();

        // Note: we only store OAuth2Authorization with refresh tokens since the others come from SEB or LMS and will
        //       just get invalid when they expire and they are not exposed within a browser.
        if (refreshToken != null) {
            rToken = refreshToken.getToken().getTokenValue();
            if (rToken != null) {
                if (accessToken != null) {
                    aToken = accessToken.getToken().getTokenValue();
                    if (aToken != null) {
                        authorizationsByAccessToken.put(aToken, authorization);
                        storeAuthorization(authorization, accessToken, refreshToken);
                    }
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.info("New token for: {} token: {} refreshToken: {}", authorization.getPrincipalName(), aToken, rToken);
        }
    }

    @Override
    public void remove(final OAuth2Authorization authorization) {
        if (authorization != null) {
            deleteAuthorization(
                    authorization,
                    authorization.getAccessToken(),
                    authorization.getRefreshToken());
        }
    }

    @Override
    public OAuth2Authorization findById(String id) {
        throw new UnsupportedOperationException("findById is not supported");
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
            if (authorizationsByAccessToken.containsKey(token)) {
                return authorizationsByAccessToken.get(token);
            } else {
                return loadAuthorizationByAccessToken(token);
            }

        }
        if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            synchronized (REFRESH_TOKEN_LOCK) {
                return loadAuthorizationByRefreshToken(token);
            }
        }
        return null;
    }

    private synchronized void storeAuthorization(
            final OAuth2Authorization authorization,
            final OAuth2Authorization.Token<OAuth2AccessToken> accessToken,
            final OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken) {

        try {

            // if we have old entries, update the access token with new refresh token and delete old refresh token reference
            final OAuth2Authorization existing = jdbcTokenStore.tryReadAuthorizationById(authorization.getId());
            if (existing != null) {
                jdbcTokenStore.removeAccessToken(existing.getAccessToken().getToken());
            }

            // store the new access token and refresh token in to DB
            jdbcTokenStore.storeAccessToken(accessToken.getToken(), authorization);

            if (refreshToken != null) {
                jdbcTokenStore.storeRefreshToken(refreshToken.getToken(), authorization);
            }

        } catch (Exception e) {
             log.error("Failed to store authorization: {} cause: {}", authorization.getId(), e.getMessage());
        }
    }

    private OAuth2Authorization loadAuthorizationByAccessToken(final String accessToken) {
        OAuth2Authorization oAuth2Authorization = jdbcTokenStore.readAuthorization(accessToken);

        // cache by access token
        if (oAuth2Authorization != null) {
            authorizationsByAccessToken.put(accessToken, oAuth2Authorization);
        }
        return oAuth2Authorization;
    }

    private OAuth2Authorization loadAuthorizationByRefreshToken(final String refreshToken) {
        OAuth2Authorization authorization = jdbcTokenStore.readAuthorizationForRefreshToken(refreshToken);

        // delete refresh token since it was reused
        if (authorization!= null) {
            jdbcTokenStore.removeRefreshToken(refreshToken);
        }

        return authorization;
    }

    private void deleteAuthorization(
            final OAuth2Authorization authorization,
            final OAuth2Authorization.Token<OAuth2AccessToken> accessToken,
            final OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken) {

        if (authorization == null) {
            return;
        }

        try {

            jdbcTokenStore.removeAccessToken(accessToken.getToken());

            if (refreshToken != null) {
                jdbcTokenStore.removeRefreshToken(refreshToken.getToken());
            }

        } catch (Exception e) {
            log.error("Failed to delete authorization: {} cause: {}", authorization.getId(), e.getMessage());
        }

        OAuth2Authorization remove = authorizationsByAccessToken.remove(accessToken.getToken().getTokenValue());
        if (remove == null) {
            log.debug("Access Token was not in the cache: {}", accessToken.getToken().getTokenValue());
        }
    }

    @Scheduled(
            fixedDelay = Constants.HOUR_IN_MILLIS,
            initialDelay = 10000)
    private void cleanup() {

        log.debug("Cleanup out dated OAuth Tokens...");

        try {
            final Collection<OAuth2AccessToken> allAccessTokens = jdbcTokenStore.getAllAccessTokens();
            final Collection<OAuth2RefreshToken> allRefreshTokens = jdbcTokenStore.getAllRefreshTokens();

            Instant now = Instant.now();
            allAccessTokens.forEach(aToken -> {
                Instant expiresAt = aToken.getExpiresAt();
                if (expiresAt != null && expiresAt.isBefore(now)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Remove out dated access token: {} expired at: {}", aToken.getTokenValue(), expiresAt);
                    }
                    jdbcTokenStore.removeAccessToken(aToken);
                }
            });
            allRefreshTokens.forEach(rToken -> {
                Instant expiresAt = rToken.getExpiresAt();
                if (expiresAt != null && expiresAt.isBefore(now)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Remove out dated refresh token: {} expired at: {}", rToken.getTokenValue(), expiresAt);
                    }
                    jdbcTokenStore.removeRefreshToken(rToken);
                }
            });
        } catch (Exception e) {
            log.error("Failed to cleanup token store: {}", e.getMessage());
        }

        authorizationsByAccessToken.clear();
    }
}
