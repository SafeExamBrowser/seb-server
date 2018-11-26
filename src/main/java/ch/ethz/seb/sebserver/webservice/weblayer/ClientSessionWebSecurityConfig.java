/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.WebClientDetailsService;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.WebResourceServerConfiguration;

/** This is the main web-security Spring configuration for SEB-Server webservice API */
@WebServiceProfile
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
public class ClientSessionWebSecurityConfig extends WebSecurityConfigurerAdapter {

    /** Spring bean name of single AuthenticationManager bean */
    public static final String AUTHENTICATION_MANAGER = "AUTHENTICATION_MANAGER";

    @Autowired
    private WebServiceUserDetails webServiceUserDetails;
    @Autowired
    @Qualifier(WebSecurityConfig.USER_PASSWORD_ENCODER_BEAN_NAME)
    private PasswordEncoder userPasswordEncoder;
    @Autowired
    private TokenStore tokenStore;
    @Autowired
    private WebClientDetailsService webServiceClientDetails;

    @Value("${sebserver.webservice.api.admin.endpoint}")
    private String adminAPIEndpoint;
    @Value("${sebserver.webservice.api.exam.endpoint}")
    private String examAPIEndpoint;

    @Bean
    public AccessTokenConverter accessTokenConverter() {
        final DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
        accessTokenConverter.setUserTokenConverter(userAuthenticationConverter());
        return accessTokenConverter;
    }

    @Bean
    public UserAuthenticationConverter userAuthenticationConverter() {
        final DefaultUserAuthenticationConverter userAuthenticationConverter =
                new DefaultUserAuthenticationConverter();
        userAuthenticationConverter.setUserDetailsService(this.webServiceUserDetails);
        return userAuthenticationConverter;
    }

    @Override
    @Bean(AUTHENTICATION_MANAGER)
    public AuthenticationManager authenticationManagerBean() throws Exception {
        final AuthenticationManager authenticationManagerBean = super.authenticationManagerBean();
        return authenticationManagerBean;
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(this.webServiceUserDetails)
                .passwordEncoder(this.userPasswordEncoder);
    }

    @Bean
    protected ResourceServerConfiguration sebServerAdminAPIResources() throws Exception {
        return new AdminAPIResourceServerConfiguration(
                this.tokenStore,
                this.webServiceClientDetails,
                authenticationManagerBean(),
                this.adminAPIEndpoint);
    }

    @Bean
    protected ResourceServerConfiguration sebServerExamAPIResources() throws Exception {
        return new ExamAPIClientResourceServerConfiguration(
                this.tokenStore,
                this.webServiceClientDetails,
                authenticationManagerBean(),
                this.examAPIEndpoint);
    }

    // NOTE: We need two different class types here to support Spring configuration for different
    //       ResourceServerConfiguration. There is a class type now for the Admin API as well as for the Exam API
    private static final class AdminAPIResourceServerConfiguration extends WebResourceServerConfiguration {

        public AdminAPIResourceServerConfiguration(
                final TokenStore tokenStore,
                final WebClientDetailsService webServiceClientDetails,
                final AuthenticationManager authenticationManager,
                final String apiEndpoint) {

            super(
                    tokenStore,
                    webServiceClientDetails,
                    authenticationManager,
                    ADMIN_API_RESOURCE_ID,
                    apiEndpoint,
                    true,
                    1);
        }
    }

    // NOTE: We need two different class types here to support Spring configuration for different
    //       ResourceServerConfiguration. There is a class type now for the Admin API as well as for the Exam API
    private static final class ExamAPIClientResourceServerConfiguration extends WebResourceServerConfiguration {

        public ExamAPIClientResourceServerConfiguration(
                final TokenStore tokenStore,
                final WebClientDetailsService webServiceClientDetails,
                final AuthenticationManager authenticationManager,
                final String apiEndpoint) {

            super(
                    tokenStore,
                    webServiceClientDetails,
                    authenticationManager,
                    EXAM_API_RESOURCE_ID,
                    apiEndpoint,
                    true,
                    2);
        }
    }

}
