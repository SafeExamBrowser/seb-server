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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.web.util.UriComponentsBuilder;

public class AdminResourceServerConfig extends ResourceServerConfiguration {

    @Value("${server.address}")
    private String webServerAdress;
    @Value("${server.port}")
    private String webServerPort;
    @Value("${sebserver.webservice.protocol}")
    private String webProtocol;
    @Value("${sebserver.oauth.clients.guiClient.id}")
    private String guiClientId;
    // TODO secret should not be referenced here (should go to stack and disappear after use)
    @Value("${sebserver.oauth.clients.guiClient.secret}")
    private String guiClientSecret;

    public AdminResourceServerConfig(final AccessTokenConverter accessTokenConverter) {
        setConfigurers(Arrays.<ResourceServerConfigurer> asList(new ResourceServerConfigurerAdapter() {

            @Override
            public void configure(final ResourceServerSecurityConfigurer resources) throws Exception {
                resources.resourceId(GuiClientDetails.RESOURCE_ID);
                // TODO try to use DefualtTokenServices like in SebClientResourceServerConfig
                final RemoteTokenServices tokenService = new RemoteTokenServices();
                tokenService.setCheckTokenEndpointUrl(
                        UriComponentsBuilder
                                .fromHttpUrl(AdminResourceServerConfig.this.webProtocol + "://"
                                        + AdminResourceServerConfig.this.webServerAdress)
                                .port(AdminResourceServerConfig.this.webServerPort)
                                .path("oauth/check_token")
                                .toUriString());
                tokenService.setClientId(AdminResourceServerConfig.this.guiClientId);
                tokenService.setClientSecret(AdminResourceServerConfig.this.guiClientSecret);
                tokenService.setAccessTokenConverter(accessTokenConverter);
                resources.tokenServices(tokenService);
            }

            @Override
            public void configure(final HttpSecurity http) throws Exception {
                http
                        .sessionManagement()
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .and()
                        .antMatcher("/admin/**")
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
        setOrder(1);
    }

    // Switch off the Spring Boot auto configuration
    @Override
    public void setConfigurers(final List<ResourceServerConfigurer> configurers) {
        super.setConfigurers(configurers);
    }

}
