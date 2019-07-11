/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.TokenRequest;

/** Just another level of indirection and work-around to solve the problem described here:
 *
 * https://github.com/spring-projects/spring-security-oauth/issues/276
 *
 * The work-around is to add some retry logic within grant if there happens a DuplicateKeyException
 * from underling token store.
 * This assumes that there is a JDBC Token Store in place and the authentication_id of the oauth_access_token
 * table has a unique identifier constraint. */
public class SynchronizedTokenGranter implements TokenGranter {

    private static final Logger log = LoggerFactory.getLogger(SynchronizedTokenGranter.class);

    private static final int retrymax = 3;
    private static final long wait = 200;

    private final TokenGranter delegate;

    public SynchronizedTokenGranter(final TokenGranter delegate) {
        this.delegate = delegate;
    }

    @Override
    public OAuth2AccessToken grant(final String grantType, final TokenRequest tokenRequest) {
        try {

            if (log.isDebugEnabled()) {
                log.debug("First try, delegate to original TokenGranter");
            }

            return this.delegate.grant(grantType, tokenRequest);

        } catch (final DuplicateKeyException e) {

            log.error("Failed to grant access token on DuplicateKeyException. Start retry...");

            final int retry = 1;
            OAuth2AccessToken grant = null;
            while (grant == null && retry <= retrymax) {
                try {
                    Thread.sleep(wait);
                } catch (final InterruptedException e1) {
                    e1.printStackTrace();
                }

                if (log.isDebugEnabled()) {
                    log.debug("Retry: {}, delegate to original TokenGranter", retry);
                }

                try {
                    grant = this.delegate.grant(grantType, tokenRequest);
                } catch (final DuplicateKeyException ee) {
                    log.error("Retry: {} failed: ", ee);
                }
            }

            return grant;

        } finally {

            if (log.isDebugEnabled()) {
                log.debug("Finised token grant");
            }
        }
    }

}
