/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;

public class DefaultTokenServicesFallback extends DefaultTokenServices {

    private static final Logger log = LoggerFactory.getLogger(DefaultTokenServicesFallback.class);

    @Override
    public OAuth2AccessToken createAccessToken(final OAuth2Authentication authentication)
            throws AuthenticationException {

        try {
            return super.createAccessToken(authentication);
        } catch (final DuplicateKeyException e) {

            log.warn(
                    "Caught DuplicateKeyException, try to handle it by trying to get the already stored access token after waited some time");

            final String clientName = authentication.getName();
            if (StringUtils.isNotBlank(clientName)) {

                // wait some time...
                try {
                    Thread.sleep(500);
                } catch (final InterruptedException e1) {
                    log.warn("Failed to sleep: {}", e1.getMessage());
                }

                final OAuth2AccessToken accessToken = this.getAccessToken(authentication);
                if (accessToken != null) {
                    log.debug("Found original access token for client: {} ", clientName);
                    return accessToken;
                }
            }

            // If no access token is available, propagate the original exception
            log.error("Unable the handle DuplicateKeyException properly", e);
            throw e;
        }
    }

}
