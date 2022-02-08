/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.weblayer.WebServiceSecurityConfig;
import ch.ethz.seb.sebserver.webservice.weblayer.WebServiceUserDetails;

/** This is the main Spring configuration of OAuth2 Authorization Server.
 *
 * Currently supporting two client types for the two different API's on
 * SEB Server webservice;
 * - Administration API for administrative purpose using password grant type with refresh token
 * - Exam API for SEB-Client connections on running exams using client_credential grant type */
@WebServiceProfile
@Configuration
@EnableAuthorizationServer
@Order(100)
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private AccessTokenConverter accessTokenConverter;
    @Autowired(required = true)
    private TokenStore tokenStore;
    @Autowired
    private WebServiceUserDetails webServiceUserDetails;
    @Autowired
    private WebClientDetailsService webServiceClientDetails;
    @Autowired
    @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME)
    private PasswordEncoder clientPasswordEncoder;
    @Autowired
    @Qualifier(WebServiceSecurityConfig.AUTHENTICATION_MANAGER)
    private AuthenticationManager authenticationManager;
    @Value("${sebserver.webservice.api.admin.accessTokenValiditySeconds:3600}")
    private Integer adminAccessTokenValSec;
    @Value("${sebserver.webservice.api.admin.refreshTokenValiditySeconds:-1}")
    private Integer adminRefreshTokenValSec;

    @Override
    public void configure(final AuthorizationServerSecurityConfigurer oauthServer) {
        oauthServer
                .tokenKeyAccess("permitAll()")
                .checkTokenAccess("isAuthenticated()")
                .passwordEncoder(this.clientPasswordEncoder);
    }

    @Override
    public void configure(final ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(this.webServiceClientDetails);
    }

    @Override
    public void configure(final AuthorizationServerEndpointsConfigurer endpoints) {
        final JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        jwtAccessTokenConverter.setAccessTokenConverter(this.accessTokenConverter);

        final DefaultTokenServices defaultTokenServices = new DefaultTokenServicesFallback();
        defaultTokenServices.setTokenStore(this.tokenStore);
        defaultTokenServices.setAuthenticationManager(this.authenticationManager);
        defaultTokenServices.setSupportRefreshToken(true);
        defaultTokenServices.setReuseRefreshToken(true);
        defaultTokenServices.setTokenEnhancer(jwtAccessTokenConverter);
        defaultTokenServices.setAccessTokenValiditySeconds(this.adminAccessTokenValSec);
        defaultTokenServices.setRefreshTokenValiditySeconds(this.adminRefreshTokenValSec);

        endpoints
                .tokenStore(this.tokenStore)
                .authenticationManager(this.authenticationManager)
                .userDetailsService(this.webServiceUserDetails)
                .accessTokenConverter(jwtAccessTokenConverter)
                .tokenServices(defaultTokenServices);
    }

}
