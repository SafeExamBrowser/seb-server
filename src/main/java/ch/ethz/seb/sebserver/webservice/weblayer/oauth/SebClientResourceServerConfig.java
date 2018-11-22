/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;

public class SebClientResourceServerConfig extends ResourceServerConfiguration {

    public SebClientResourceServerConfig(
            final AccessTokenConverter accessTokenConverter,
            final TokenStore tokenStore,
            final WebServiceClientDetails webServiceClientDetails,
            final AuthenticationManager authenticationManager) {

        setConfigurers(Arrays.<ResourceServerConfigurer> asList(new ResourceServerConfigurerAdapter() {

            @Override
            public void configure(final ResourceServerSecurityConfigurer resources) throws Exception {
                resources.resourceId(WebServiceClientDetails.RESOURCE_ID);
                final DefaultTokenServices tokenService = new DefaultTokenServices();
                tokenService.setTokenStore(tokenStore);
                tokenService.setClientDetailsService(webServiceClientDetails);
                tokenService.setSupportRefreshToken(false);
                tokenService.setAuthenticationManager(authenticationManager);
                resources.tokenServices(tokenService);
            }

            @Override
            public void configure(final HttpSecurity http) throws Exception {
                http
                        .sessionManagement()
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .and()
                        .antMatcher("/sebclient/**")
                        .authorizeRequests()
                        .anyRequest()
                        .authenticated()
                        .and()
                        .formLogin().disable()
                        .httpBasic().disable()
                        .logout().disable()
                        .headers().frameOptions().disable()
                        .and()
                        .csrf().disable();
            }

        }));
        setOrder(2);
    }

    // Switch off the Spring Boot auto configuration
    @Override
    public void setConfigurers(final List<ResourceServerConfigurer> configurers) {
        super.setConfigurers(configurers);
    }

}
