/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import java.security.Principal;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

/** A service to get the authenticated user from current request */
@Lazy
@Service
@WebServiceProfile
public class CurrentUserService {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserService.class);

    public interface ExtractUserFromAuthenticationStrategy {
        SEBServerUser extract(Principal principal);
    }

    private final Collection<ExtractUserFromAuthenticationStrategy> extractStrategies;

    public CurrentUserService(final Collection<ExtractUserFromAuthenticationStrategy> extractStrategies) {
        this.extractStrategies = extractStrategies;
    }

    /** Use this to get the current User within a request-response thread cycle.
     *
     * @return the SEBServerUser instance of the current request
     * @throws IllegalStateException if no Authentication was found
     * @throws IllegalArgumentException if fromPrincipal is not able to extract the User of the Authentication
     *             instance */
    public SEBServerUser getCurrentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No Authentication found within Springs SecurityContextHolder");
        }

        return extractFromPrincipal(authentication);
    }

    /** Extracts the internal SEBServerUser from a given Principal.
     *
     * This is attended to apply some known strategies to extract the internal user from Principal. If there is no
     * internal user found on the given Principal, a IllegalArgumentException is thrown.
     *
     * If there is certainly a internal user within the given Principal but no strategy that finds it, this method can
     * be extended with the needed strategy.
     *
     * @param principal
     * @return internal User instance if it was found within the Principal and the existing strategies
     * @throws IllegalArgumentException if no internal User can be found */
    public SEBServerUser extractFromPrincipal(final Principal principal) {
        for (final ExtractUserFromAuthenticationStrategy extractStrategie : this.extractStrategies) {
            try {
                final SEBServerUser user = extractStrategie.extract(principal);
                if (user != null) {
                    return user;
                }
            } catch (final Exception e) {
                log.error("Unexpected error while trying to extract user form principal: ", e);
            }
        }

        throw new IllegalArgumentException("Unable to extract internal user from Principal: " + principal);
    }

    // 1. OAuth2Authentication strategy
    @Lazy
    @Component
    public static class DefaultUserExtractStrategy implements ExtractUserFromAuthenticationStrategy {

        @Override
        public SEBServerUser extract(final Principal principal) {
            if (principal instanceof OAuth2Authentication) {
                final Authentication userAuthentication = ((OAuth2Authentication) principal).getUserAuthentication();
                if (userAuthentication instanceof UsernamePasswordAuthenticationToken) {
                    final Object userPrincipal =
                            ((UsernamePasswordAuthenticationToken) userAuthentication).getPrincipal();
                    if (userPrincipal instanceof SEBServerUser) {
                        return (SEBServerUser) userPrincipal;
                    }
                }
            }

            return null;
        }
    }

}
