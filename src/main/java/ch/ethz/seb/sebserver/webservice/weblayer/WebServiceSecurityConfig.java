/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.filters.RemoteIpFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
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
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.PreAuthProvider;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.WebClientDetailsService;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.WebserviceResourceConfiguration;

/** This is the main web-security Spring configuration for SEB-Server webservice API
 * <p>
 * Currently two separated Rest API's are implemented, one for administration and maintenance
 * of the SEB-Server (AdminAPI) and one for SEB-Client connection on running exams and eventually
 * also for LMS communication), if needed (ExamAPI). The AdministrationAPI uses OAuth 2 password
 * grant with refresh-token, same as in the prototype and the ExamAPI uses the client_credential grant.
 * <p>
 * There is a Spring Authorization-Server defining this two clients (AdminAPIClient and ExamAPIClient) as well as
 * two Spring Resource-Server for the separation of the different API's
 * <p>
 * The endpoint of the AdministrationAPI can be configured within the key; sebserver.webservice.api.admin.endpoint
 * and is by default set to "/admin-api/**"
 * <p>
 * The endpoint of the ExamAPI can be configured within the key; sebserver.webservice.api.exam.endpoint
 * and is by default set to "/exam-api/**" */
@WebServiceProfile
@Configuration
@EnableWebSecurity
@Order(6)
@Import(DataSourceAutoConfiguration.class)
public class WebServiceSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebServiceSecurityConfig.class);

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
    private WebClientDetailsService webClientDetailsService;
    @Autowired
    private PreAuthProvider preAuthProvider;

    @Value("${sebserver.webservice.api.admin.endpoint}")
    private String adminAPIEndpoint;
    @Value("${sebserver.webservice.api.exam.endpoint}")
    private String examAPIEndpoint;
    @Value("${sebserver.webservice.lms.api.endpoint}")
    private String lmsAPIEndpoint;
    @Value("${management.endpoints.web.base-path:NONE}")
    private String actuatorEndpoint;
    @Value("${sebserver.webservice.http.redirect.gui}")
    private String unauthorizedRedirect;

    @Value("${sebserver.webservice.api.admin.accessTokenValiditySeconds:3600}")
    private Integer adminAccessTokenValSec;
    @Value("${sebserver.webservice.api.admin.refreshTokenValiditySeconds:-1}")
    private Integer adminRefreshTokenValSec;
    @Value("${sebserver.webservice.api.exam.accessTokenValiditySeconds:43200}")
    private Integer examAccessTokenValSec;
    @Value("${sebserver.webservice.lms.api.accessTokenValiditySeconds:-1}")
    private Integer lmsAccessTokenValSec;


    /** Used to get real remote IP address by using "X-Forwarded-For" and "X-Forwarded-Proto" header.
     * <a href="https://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/catalina/filters/RemoteIpFilter.html">see</a>
     *
     * @return RemoteIpFilter instance */
    @Bean
    public RemoteIpFilter remoteIpFilter() {
        return new RemoteIpFilter();
    }

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
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(this.webServiceUserDetails)
                .passwordEncoder(this.userPasswordEncoder);
        auth.authenticationProvider(this.preAuthProvider);
    }

    @Override
    public void configure(final HttpSecurity http) throws Exception {
        http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .formLogin().disable()
                .httpBasic().disable()
                .logout().disable()
                .headers().frameOptions().disable()
                .and()
                .csrf().disable();
    }

    @Bean
    protected ResourceServerConfiguration sebServerAdminAPIResources() throws Exception {
        return new AdminAPIResourceServerConfiguration(
                this.tokenStore,
                this.webClientDetailsService,
                authenticationManagerBean(),
                this.adminAPIEndpoint,
                this.unauthorizedRedirect,
                this.adminAccessTokenValSec,
                this.adminRefreshTokenValSec);
    }

    @Bean
    protected ResourceServerConfiguration sebServerExamAPIResources() throws Exception {
        return new ExamAPIClientResourceServerConfiguration(
                this.tokenStore,
                this.webClientDetailsService,
                authenticationManagerBean(),
                this.examAPIEndpoint,
                this.examAccessTokenValSec);
    }

    @Bean
    protected ResourceServerConfiguration sebServerLMSAPIResources() throws Exception {
        return new LMSAPIClientResourceServerConfiguration(
                this.tokenStore,
                this.webClientDetailsService,
                authenticationManagerBean(),
                this.lmsAPIEndpoint,
                this.lmsAccessTokenValSec);
    }


    // NOTE: We need different class types here to support Spring configuration for different
    private static final class AdminAPIResourceServerConfiguration extends WebserviceResourceConfiguration {

        public AdminAPIResourceServerConfiguration(
                final TokenStore tokenStore,
                final WebClientDetailsService webServiceClientDetails,
                final AuthenticationManager authenticationManager,
                final String apiEndpoint,
                final String redirect,
                final int adminAccessTokenValSec,
                final int adminRefreshTokenValSec) {

            super(
                    tokenStore,
                    webServiceClientDetails,
                    authenticationManager,
                    new LoginRedirectOnUnauthorized(redirect),
                    ADMIN_API_RESOURCE_ID,
                    apiEndpoint,
                    true,
                    2,
                    adminAccessTokenValSec,
                    adminRefreshTokenValSec);
        }
    }

    // NOTE: We need different class types here to support Spring configuration for different
    private static final class ExamAPIClientResourceServerConfiguration extends WebserviceResourceConfiguration {

        public ExamAPIClientResourceServerConfiguration(
                final TokenStore tokenStore,
                final WebClientDetailsService webServiceClientDetails,
                final AuthenticationManager authenticationManager,
                final String apiEndpoint,
                final int adminAccessTokenValSec) {

            super(
                    tokenStore,
                    webServiceClientDetails,
                    authenticationManager,
                    (request, response, exception) -> {
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        log.warn("Unauthorized Request: {}", request, exception);
                        log.info("Redirect to login after unauthorized request");
                        response.getOutputStream().println("{ \"error\": \"" + exception.getMessage() + "\" }");
                    },
                    EXAM_API_RESOURCE_ID,
                    apiEndpoint,
                    true,
                    3,
                    adminAccessTokenValSec,
                    1);
        }
    }

    // NOTE: We need different class types here to support Spring configuration for different
    private static final class LMSAPIClientResourceServerConfiguration extends WebserviceResourceConfiguration {

        public LMSAPIClientResourceServerConfiguration(
                final TokenStore tokenStore,
                final WebClientDetailsService webServiceClientDetails,
                final AuthenticationManager authenticationManager,
                final String apiEndpoint,
                final int accessTokenValSec) {

            super(
                    tokenStore,
                    webServiceClientDetails,
                    authenticationManager,
                    (request, response, exception) -> {
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        log.warn("Unauthorized Request: {}", request, exception);
                        log.info("Redirect to login after unauthorized request");
                        response.getOutputStream().println("{ \"error\": \"" + exception.getMessage() + "\" }");
                    },
                    EXAM_API_RESOURCE_ID,
                    apiEndpoint,
                    true,
                    4,
                    accessTokenValSec,
                    1);
        }
    }

    private static class LoginRedirectOnUnauthorized implements AuthenticationEntryPoint {

        private final String redirect;

        protected LoginRedirectOnUnauthorized(final String redirect) {
            this.redirect = redirect;
        }

        @Override
        public void commence(
                final HttpServletRequest request,
                final HttpServletResponse response,
                final AuthenticationException authenticationException) throws IOException {

            log.warn("Unauthorized Request: {} : Redirect to login after unauthorized request",
                    request.getRequestURI());

            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader(HttpHeaders.LOCATION, this.redirect);
            response.flushBuffer();
        }
    }

}
