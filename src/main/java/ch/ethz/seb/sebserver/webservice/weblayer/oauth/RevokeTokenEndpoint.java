/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

/** Spring MVC controller that defines a revoke token endpoint */
@Controller
@WebServiceProfile
public class RevokeTokenEndpoint {

    private final ConsumerTokenServices tokenServices;
    private final AdminAPIClientDetails adminAPIClientDetails;
    private final TokenStore tokenStore;

    public RevokeTokenEndpoint(
            final ConsumerTokenServices tokenServices,
            final AdminAPIClientDetails adminAPIClientDetails,
            final TokenStore tokenStore) {

        this.tokenServices = tokenServices;
        this.adminAPIClientDetails = adminAPIClientDetails;
        this.tokenStore = tokenStore;
    }

    @RequestMapping(value = "/oauth/revoke-token", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void logout(final HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            final String tokenId = authHeader.substring("Bearer".length() + 1);
            this.tokenServices.revokeToken(tokenId);
        }
    }

    @EventListener(RevokeTokenEvent.class)
    void revokeAccessToken(final RevokeTokenEvent event) {
        final String clientId = this.adminAPIClientDetails.getClientId();
        final Collection<OAuth2AccessToken> tokens = this.tokenStore
                .findTokensByClientIdAndUserName(clientId, event.userName);

        if (tokens != null) {
            for (final OAuth2AccessToken token : tokens) {
                this.tokenStore.removeAccessToken(token);
            }
        }
    }

    public static final class RevokeTokenEvent extends ApplicationEvent {

        private static final long serialVersionUID = 5776699085388043743L;

        public final String userName;

        public RevokeTokenEvent(final Object source, final String userName) {
            super(source);
            this.userName = userName;
        }

    }

}
