/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import java.util.Arrays;
import java.util.Collection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.transaction.annotation.Transactional;

public class CachableJdbcTokenStore implements TokenStore {

    public static final String CACHE_NAME = "ACCESS_TOKEN_STORE_CACHE";

    private static final Logger log = LoggerFactory.getLogger(CachableJdbcTokenStore.class);

    private final JdbcTokenStore jdbcTokenStore;

    public CachableJdbcTokenStore(final DataSource dataSource) {
        this.jdbcTokenStore = new JdbcTokenStore(dataSource);
        this.jdbcTokenStore.setAuthenticationKeyGenerator(new KeyGenerator());
    }

    @Override
    public OAuth2AccessToken getAccessToken(final OAuth2Authentication authentication) {
        return this.jdbcTokenStore.getAccessToken(authentication);
    }

    @Override
    @Transactional
    public void storeAccessToken(final OAuth2AccessToken token, final OAuth2Authentication authentication) {
        this.jdbcTokenStore.storeAccessToken(token, authentication);
    }

    @Override
    public OAuth2Authentication readAuthentication(final OAuth2AccessToken token) {
        if (log.isDebugEnabled()) {
            log.debug("Read authentication from persistent and cache if available");
        }

        return this.jdbcTokenStore.readAuthentication(token);
    }

    @Override
    public OAuth2Authentication readAuthentication(final String token) {
        return this.jdbcTokenStore.readAuthentication(token);
    }

    @Override
    @Cacheable(
            cacheNames = CACHE_NAME,
            key = "#tokenValue",
            unless = "#result == null")
    public OAuth2AccessToken readAccessToken(final String tokenValue) {
        return this.jdbcTokenStore.readAccessToken(tokenValue);
    }

    @Override
    @CacheEvict(
            cacheNames = CACHE_NAME,
            key = "#token.getValue()")
    public void removeAccessToken(final OAuth2AccessToken token) {
        if (log.isDebugEnabled()) {
            log.debug("Evict token from cache and remove it also from persistent store");
        }

        this.jdbcTokenStore.removeAccessToken(token);
    }

    @Override
    public void storeRefreshToken(final OAuth2RefreshToken refreshToken, final OAuth2Authentication authentication) {
        this.jdbcTokenStore.storeRefreshToken(refreshToken, authentication);
    }

    @Override
    public OAuth2RefreshToken readRefreshToken(final String tokenValue) {
        return this.jdbcTokenStore.readRefreshToken(tokenValue);
    }

    @Override
    public OAuth2Authentication readAuthenticationForRefreshToken(final OAuth2RefreshToken token) {
        return this.jdbcTokenStore.readAuthenticationForRefreshToken(token);
    }

    @Override
    public void removeRefreshToken(final OAuth2RefreshToken token) {
        this.jdbcTokenStore.removeRefreshToken(token);
    }

    @Override
    public void removeAccessTokenUsingRefreshToken(final OAuth2RefreshToken refreshToken) {
        this.jdbcTokenStore.removeAccessTokenUsingRefreshToken(refreshToken);
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(final String clientId, final String userName) {
        return this.jdbcTokenStore.findTokensByClientIdAndUserName(clientId, userName);
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientId(final String clientId) {
        return this.jdbcTokenStore.findTokensByClientId(clientId);
    }

    /** Used do proper handle key generation on null-able authentication.
     * If given OAuth2Authentication this returns null instead of throwing a
     * NullPointerException. */
    private static final class KeyGenerator extends DefaultAuthenticationKeyGenerator {

        @Override
        public String extractKey(final OAuth2Authentication authentication) {
            if (authentication == null) {
                log.warn("Given OAuth2Authentication is null. Should not happen. Stack is: {}",
                        Arrays.toString(Thread.currentThread().getStackTrace()));

                return null;
            }
            return super.extractKey(authentication);
        }

    }

}
