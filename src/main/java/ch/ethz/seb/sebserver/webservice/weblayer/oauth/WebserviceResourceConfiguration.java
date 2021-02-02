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
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Abstract Spring ResourceServerConfiguration to configure different resource services
 * for different API's. */
public abstract class WebserviceResourceConfiguration extends ResourceServerConfiguration {

    /** The resource identifier of Administration API resources */
    public static final String ADMIN_API_RESOURCE_ID = "seb-server-administration-api";
    /** The resource identifier of the Exam API resources */
    public static final String EXAM_API_RESOURCE_ID = "seb-server-exam-api";

    public WebserviceResourceConfiguration(
            final TokenStore tokenStore,
            final WebClientDetailsService webServiceClientDetails,
            final AuthenticationManager authenticationManager,
            final AuthenticationEntryPoint authenticationEntryPoint,
            final String resourceId,
            final String apiEndpoint,
            final boolean supportRefreshToken,
            final int order,
            final int accessTokenValiditySeconds,
            final int refreshTokenValiditySeconds) {

        super();
        final ConfigurerAdapter configurerAdapter = new ConfigurerAdapter(
                this,
                tokenStore,
                webServiceClientDetails,
                authenticationManager,
                authenticationEntryPoint,
                resourceId,
                apiEndpoint,
                supportRefreshToken,
                accessTokenValiditySeconds,
                refreshTokenValiditySeconds);

        setConfigurers(Arrays.asList(configurerAdapter));
        super.setOrder(order);
    }

    // Switches off the Spring Boot auto configuration
    @Override
    @SuppressWarnings("PMD")
    public final void setConfigurers(final List<ResourceServerConfigurer> configurers) {
        super.setConfigurers(configurers);
    }

    protected void addConfiguration(final ConfigurerAdapter configurerAdapter, final HttpSecurity http)
            throws Exception {
        // To override of additional configuration is needed
        http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .antMatcher(configurerAdapter.apiEndpoint + "/**")
                .authorizeRequests()
                .anyRequest()
                .authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(configurerAdapter.authenticationEntryPoint)
                .and()
                .formLogin().disable()
                .httpBasic().disable()
                .logout().disable()
                .headers().frameOptions().disable()
                .and()
                .csrf().disable();
    }

    protected static final class ConfigurerAdapter extends ResourceServerConfigurerAdapter {

        public final WebserviceResourceConfiguration webserviceResourceConfiguration;
        public final TokenStore tokenStore;
        public final WebClientDetailsService webServiceClientDetails;
        public final AuthenticationManager authenticationManager;
        public final AuthenticationEntryPoint authenticationEntryPoint;
        public final String resourceId;
        public final String apiEndpoint;
        public final boolean supportRefreshToken;
        public final int accessTokenValiditySeconds;
        public final int refreshTokenValiditySeconds;

        public ConfigurerAdapter(
                final WebserviceResourceConfiguration webserviceResourceConfiguration,
                final TokenStore tokenStore,
                final WebClientDetailsService webServiceClientDetails,
                final AuthenticationManager authenticationManager,
                final AuthenticationEntryPoint authenticationEntryPoint,
                final String resourceId,
                final String apiEndpoint,
                final boolean supportRefreshToken,
                final int accessTokenValiditySeconds,
                final int refreshTokenValiditySeconds) {

            super();
            this.webserviceResourceConfiguration = webserviceResourceConfiguration;
            this.tokenStore = tokenStore;
            this.webServiceClientDetails = webServiceClientDetails;
            this.authenticationManager = authenticationManager;
            this.authenticationEntryPoint = authenticationEntryPoint;
            this.resourceId = resourceId;
            this.apiEndpoint = apiEndpoint;
            this.supportRefreshToken = supportRefreshToken;
            this.accessTokenValiditySeconds = accessTokenValiditySeconds;
            this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
        }

        @Override
        public void configure(final ResourceServerSecurityConfigurer resources) {
            resources.resourceId(this.resourceId);
            final DefaultTokenServices tokenService = new DefaultTokenServices();
            tokenService.setTokenStore(this.tokenStore);
            tokenService.setClientDetailsService(this.webServiceClientDetails);
            tokenService.setSupportRefreshToken(this.supportRefreshToken);
            tokenService.setAuthenticationManager(this.authenticationManager);
            tokenService.setAccessTokenValiditySeconds(this.accessTokenValiditySeconds);
            tokenService.setRefreshTokenValiditySeconds(this.refreshTokenValiditySeconds);
            resources.tokenServices(tokenService);
        }

        @Override
        public void configure(final HttpSecurity http) throws Exception {
            this.webserviceResourceConfiguration.addConfiguration(this, http);
        }
    }

}
