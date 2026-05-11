/*
 * Copyright (c) 2024 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConnectionConfigurationService;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver.pwdgrant.OAuth2PasswordGrantAuthenticationConverter;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver.pwdgrant.OAuth2PasswordGrantAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

@Configuration
@EnableWebSecurity
public class AuthServerConfig {

    @Autowired
    private PasswordEncoder userPasswordEncoder;
    @Autowired
    private ConnectionConfigurationService sebConnectionConfigurationService;
    @Autowired
    private RegisteredGuiClient registeredGuiClient;
    @Autowired
    private RegisteredLMSClient registeredSEBServerClient;
    @Autowired
    private WebServiceUserDetails webServiceUserDetails;
    @Autowired
    private UserDAO userDAO;
    
    
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            AuthenticationManager authenticationManager,
            OAuth2AuthorizationService authorizationService) throws Exception {
        
       OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        OAuth2PasswordGrantAuthenticationProvider oAuth2PasswordGrantAuthenticationProvider =
                new OAuth2PasswordGrantAuthenticationProvider(authenticationManager, authorizationService);
        OAuth2ClientCredentialsGrantProvider oAuth2ClientCredentialsGrantProvider =
                new OAuth2ClientCredentialsGrantProvider(authorizationService);
        
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .authorizationEndpoint(c -> c.authenticationProviders( providers -> {
                    providers.clear();
                    providers.addFirst(oAuth2ClientCredentialsGrantProvider);
                    providers.addFirst(oAuth2PasswordGrantAuthenticationProvider);
                }));

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .tokenEndpoint( e -> e.accessTokenRequestConverters( converters -> {
                    converters.clear();
                    converters.add(new OAuth2PasswordGrantAuthenticationConverter());
                    converters.add(new OAuth2RefreshTokenAuthenticationConverter());
                    converters.add(new OAuth2ClientCredentialsAuthenticationConverter());
                }));
        
        DefaultSecurityFilterChain result = http.build();
        
        OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator = http.getSharedObject(OAuth2TokenGenerator.class);
        http.authenticationProvider(new OAuth2RefreshTokenAuthenticationProvider(authorizationService, tokenGenerator));
        // we have to initialize the custom providers after the chain has been built
        oAuth2PasswordGrantAuthenticationProvider.init(http);
        oAuth2ClientCredentialsGrantProvider.init(http);
        
        return result;
    }
    
    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider = 
                new PreAuthenticatedAuthenticationProvider();
        preAuthenticatedAuthenticationProvider.setPreAuthenticatedUserDetailsService(webServiceUserDetails);
        return preAuthenticatedAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(this.webServiceUserDetails)
                .passwordEncoder(this.userPasswordEncoder);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public UserDetailsManager userDetailsManager() {
        return new UserDetailsManagerImpl(userDAO);
    }

    @Bean
    public OAuth2AuthorizationService oAuth2AuthorizationService() {
        //return new DummyTokenStore();
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        return new RegisteredClientRepositoryImpl(
                sebConnectionConfigurationService,
                registeredGuiClient, 
                registeredSEBServerClient);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings
                .builder()
                .tokenEndpoint(API.OAUTH_TOKEN_ENDPOINT)
                .tokenRevocationEndpoint(API.OAUTH_REVOKE_TOKEN_ENDPOINT)
                .build();
    }

}
