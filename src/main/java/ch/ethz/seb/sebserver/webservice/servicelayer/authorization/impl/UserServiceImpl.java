/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl;

import java.beans.PropertyEditorSupport;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.WebDataBinder;

import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;

@Lazy
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserDAO userDAO;

    public interface ExtractUserFromAuthenticationStrategy {
        SEBServerUser extract(Principal principal);
    }

    private final Collection<ExtractUserFromAuthenticationStrategy> extractStrategies;

    public UserServiceImpl(
            final UserDAO userDAO,
            final Collection<ExtractUserFromAuthenticationStrategy> extractStrategies) {

        this.userDAO = userDAO;
        this.extractStrategies = extractStrategies;
    }

    @Override
    public SEBServerUser getCurrentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No Authentication found within Springs SecurityContextHolder");
        }

        return extractFromPrincipal(authentication);
    }

    @Override
    public SEBServerUser extractFromPrincipal(final Principal principal) {
        for (final ExtractUserFromAuthenticationStrategy extractStrategy : this.extractStrategies) {
            try {
                final SEBServerUser user = extractStrategy.extract(principal);
                if (user != null) {
                    return user;
                }
            } catch (final Exception e) {
                log.error("Unexpected error while trying to extract user form principal: ", e);
            }
        }

        throw new IllegalArgumentException("Unable to extract internal user from Principal: " + principal);
    }

    @Override
    public SEBServerUser getAnonymousUser() {
        return ANONYMOUS_USER;
    }

    @Override
    public void addUsersInstitutionDefaultPropertySupport(final WebDataBinder binder) {
        final PropertyEditorSupport usersInstitutionDefaultEditor = new PropertyEditorSupport() {
            @Override
            public void setAsText(final String text) throws IllegalArgumentException {
                if (UserService.USERS_INSTITUTION_AS_DEFAULT.equals(text)) {
                    setValue(getCurrentUser().institutionId());
                } else {
                    try {
                        setValue((text == null) ? null : Long.decode(text));
                    } catch (final Exception e) {
                        log.error("Failed to set institution from user: ", e);
                        setValue(-1);
                    }
                }
            }
        };
        binder.registerCustomEditor(Long.class, usersInstitutionDefaultEditor);
    }

    @Override
    public void setAuthenticationIfAbsent(final Authentication authentication) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    @Override
    public UserInfo getUser(final String userId) {
        if (Objects.equals(userId, LMS_INTEGRATION_CLIENT_UUID)) {
            return createLMSIntegrationClientUser().getUserInfo();
        }

        final UserInfo user = this.userDAO
                .byModelId(userId)
                .onError(error -> log.error("Failed to find user for id: {} error: {}", userId, error.getMessage()))
                .getOr(null);

        if (user != null && !user.isActive()) {
            log.warn("Try to get inactive user for processing: {}", userId);
            return null;
        }

        return user;
    }

    // 1. OAuth2Authentication strategy
    @Lazy
    @Component
    public static class DefaultUserExtractStrategy implements ExtractUserFromAuthenticationStrategy {

        final UserCacheService userCacheService;
        final String lmsClientId;

        DefaultUserExtractStrategy(
                UserCacheService userCacheService,
                final Environment env,
                @Value("${sebserver.webservice.lms.api.clientId}") final String lmsClientId) {

            this.userCacheService = userCacheService;
            this.lmsClientId = lmsClientId;

            HashSet<String> profiles = new HashSet<>(Arrays.asList(env.getActiveProfiles()));
        }

        @Override
        public SEBServerUser extract(final Principal principal) {
            String name = principal.getName();
            SEBServerUser lmsIntegrationClient = isLMSIntegrationClient(name);
            if (lmsIntegrationClient != null) {
                return lmsIntegrationClient;
            }

            SEBServerUser sebServerUser = userCacheService.serverUserByName(name);
            if (sebServerUser == null) {
                throw new UsernameNotFoundException("User for name: " + name + " not found");
            }

            return sebServerUser;
        }

        private SEBServerUser isLMSIntegrationClient(final String name) {
            if (lmsClientId.equals(name)) {
                return createLMSIntegrationClientUser();
            }
            return null;
        }

    }

    private static SEBServerUser createLMSIntegrationClientUser() {
        return new SEBServerUser(
                -1L,
                new UserInfo(
                        LMS_INTEGRATION_CLIENT_UUID,
                        -1L,
                        null,
                        LMS_INTEGRATION_CLIENT_NAME,
                        LMS_INTEGRATION_CLIENT_NAME,
                        LMS_INTEGRATION_CLIENT_NAME, null,
                        false,
                        false,
                        true,
                        null, null,
                        Arrays.stream(UserRole.values())
                                .map(Enum::name)
                                .collect(Collectors.toSet()),
                        Collections.emptyList(),
                        Collections.emptyList()),
                null);
    }

    // 2. Separated thread strategy
    @Lazy
    @Component
    public static class OtherThreadUserExtractStrategy implements ExtractUserFromAuthenticationStrategy {

        @Override
        public SEBServerUser extract(final Principal principal) {
            if (principal instanceof SEBServerUser) {
                return (SEBServerUser) principal;
            }

            return null;
        }
    }

    private static final SEBServerUser ANONYMOUS_USER = new SEBServerUser(
            -1L,
            new UserInfo("SEB_SERVER_ANONYMOUS_USER", -2L, null, "anonymous", "anonymous", "anonymous", null,
                    false,
                    false,
                    true,
                    null, null,
                    Arrays.stream(UserRole.values())
                            .map(Enum::name)
                            .collect(Collectors.toSet()),
                    Collections.emptyList(),
                    Collections.emptyList()),
            null);

}
