/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

/** Spring web security configuration for all endpoints needed for SEB-Client session management.
 * This are:
 *
 * <pre>
 *      - /sebauth/sebhandshake/ the SEB-Client handshake and authentication endpoint
 *      - /sebauth/lmshandshake/ the LMS-Client handshake and authentication endpoint
 *      - /ws/ the root of all web-socket endpoints on HTTP level
 * </pre>
 *
 * This configuration secures the above endpoints by using custom client authentication filter */
@Configuration
@WebServiceProfile
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(2)
public class ClientSessionWebSecurityConfig extends WebSecurityConfigurerAdapter {

    public static final AntPathRequestMatcher SEB_HANDSHAKE_ENDPOINT =
            new AntPathRequestMatcher("/sebauth/sebhandshake/**");
    public static final AntPathRequestMatcher SEB_WEB_SOCKET_ENDPOINT =
            new AntPathRequestMatcher("/ws/**");
    public static final AntPathRequestMatcher LMS_HANDSHAKE_ENDPOINT =
            new AntPathRequestMatcher("/sebauth/lmshandshake/**");

    public static final RequestMatcher SEB_CLIENT_ENDPOINTS = new OrRequestMatcher(
            SEB_HANDSHAKE_ENDPOINT,
            SEB_WEB_SOCKET_ENDPOINT);

    public static final RequestMatcher SEB_CONNECTION_PROTECTED_URLS = new OrRequestMatcher(
            SEB_CLIENT_ENDPOINTS,
            LMS_HANDSHAKE_ENDPOINT);

    @Autowired
    private CustomAuthenticationError customAuthenticationError;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        System.out.println("**************** WebServiceWebConfig: ");
        //@formatter:off
        http
            // The Web-Service is designed as a stateless Rest API
            // for SEB session management only endpoints for handshake and web-socket is used what is stateless on HTTP
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

// TODO
//          .and()
//            .requestMatcher(SEB_CONNECTION_PROTECTED_URLS)
//            .addFilterBefore(
//                    this.sebClientAuthenticationFilter,
//                    BasicAuthenticationFilter.class)
//            .addFilterBefore(
//                    this.lmsClientAuthenticationFilter,
//                    SEBClientAuthenticationFilter.class)
//            .authorizeRequests()
//            .requestMatchers(SEB_CONNECTION_PROTECTED_URLS)
//            .authenticated()
// instead of:

        .and()
            .antMatcher("/webservice/**")
            .authorizeRequests()
            .anyRequest()
            .fullyAuthenticated()
// end TODO

        .and()
            .exceptionHandling()
            .defaultAuthenticationEntryPointFor(
                    this.customAuthenticationError,
                    SEB_CONNECTION_PROTECTED_URLS)

        .and()
            // disable session based security and functionality
            .formLogin().disable()
            .httpBasic().disable()
            .logout().disable()
            .headers().frameOptions().disable()
         .and()
            .csrf().disable();
      //@formatter:on
    }
}
