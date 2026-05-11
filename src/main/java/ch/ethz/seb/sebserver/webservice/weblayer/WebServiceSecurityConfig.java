/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.webservice.WebserviceInit;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.resserver.UnauthorizedRequestHandler;

import org.apache.catalina.filters.RemoteIpFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
@Configuration
public class WebServiceSecurityConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebServiceSecurityConfig.class);

    private static String[] OPEN_ENDPOINTS = new String[] {
            API.CHECK_PATH,
            API.OAUTH_REVOKE_TOKEN_ENDPOINT,
            API.OAUTH_JWT_TOKEN_ENDPOINT + "/**"
    };
    public static final String V_3_API_DOCS_ENDPOINT = "/v3/api-docs/**";
    public static final String SWAGGER_UI_ENDPOINT = "/swagger-ui/**";

    @Value("${springdoc.api-docs.enabled:false}")
    private boolean apiDocEnabled;
    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerEnabled;

    @Bean
    @Order(5)
    public SecurityFilterChain baseFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/**")
                .authorizeHttpRequests((requests) -> {
                    requests.requestMatchers(OPEN_ENDPOINTS).permitAll();
                    if (apiDocEnabled) {
                        SEBServerInit.INIT_LOGGER.info("**** API DOCS V3 ENABLED!");
                        requests.requestMatchers(V_3_API_DOCS_ENDPOINT).permitAll();
                    } else {
                        SEBServerInit.INIT_LOGGER.info("xxxx API DOCS V3 NOT ENABLED!");
                    }
                    if (swaggerEnabled) {
                        SEBServerInit.INIT_LOGGER.info("**** SWAGGER UI ENABLED!");
                        requests.requestMatchers(SWAGGER_UI_ENDPOINT).permitAll();
                    } else {
                        SEBServerInit.INIT_LOGGER.info("xxxx SWAGGER UI NOT ENABLED!");
                    }
                    requests.anyRequest().denyAll();
                })
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .headers(c -> c.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .exceptionHandling( c -> c.authenticationEntryPoint(new UnauthorizedRequestHandler("root")));

        return http.build();
    }

}
