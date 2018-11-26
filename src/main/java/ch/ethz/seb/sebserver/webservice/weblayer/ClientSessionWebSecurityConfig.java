/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.WebClientDetailsService;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.WebResourceServerConfiguration;

/** This is the main web-security Spring configuration for SEB-Server webservice API
 *
 * Currently two separated Rest API's are implemented, one for administration and maintenance
 * of the SEB-Server (AdminAPI) and one for SEB-Client connection on running exams and eventually
 * also for LMS communication), if needed (ExamAPI). The AdministrationAPI uses OAuth 2 password
 * grant with refresh-token, same as in the prototype and the ExamAPI uses the client_credential grant.
 *
 * There is a Spring Authorization-Server defining this two clients (AdminAPIClient and ExamAPIClient) as well as
 * two Spring Resource-Server for the separation of the different API's
 *
 * The endpoint of the AdministrationAPI can be configured within the key; sebserver.webservice.api.admin.endpoint
 * and is by default set to "/admin-api/**"
 *
 * The endpoint of the ExamAPI can be configured within the key; sebserver.webservice.api.exam.endpoint
 * and is by default set to "/exam-api/**" */
@WebServiceProfile
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@Order(4)
public class ClientSessionWebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ClientSessionWebSecurityConfig.class);

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

    @Override
    public void configure(final HttpSecurity http) throws Exception {
        http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .antMatcher("/**")
                .authorizeRequests()
                .anyRequest()
                .authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(new LoginRedirectOnUnauthorized())
                .and()
                .formLogin().disable()
                .httpBasic().disable()
                .logout().disable()
                .headers().frameOptions().disable()
                .and()
                .csrf().disable();
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
                    new LoginRedirectOnUnauthorized(),
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
                    (request, response, exception) -> {
                        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        log.warn("Unauthorized Request: {}", request, exception);
                        log.info("Redirect to login after unauthorized request");
                        response.getOutputStream().println("{ \"error\": \"" + exception.getMessage() + "\" }");
                    },
                    EXAM_API_RESOURCE_ID,
                    apiEndpoint,
                    true,
                    2);
        }
    }

    private static class LoginRedirectOnUnauthorized implements AuthenticationEntryPoint {

        @Override
        public void commence(
                final HttpServletRequest request,
                final HttpServletResponse response,
                final AuthenticationException authenticationException) throws IOException, ServletException {

            log.warn("Unauthorized Request: {} : Redirect to login after unauthorized request",
                    request.getRequestURI());
            // TODO define login redirect
            response.sendRedirect("/gui/");
        }
    }

}
