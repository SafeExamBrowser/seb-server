/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

/** This is the main Spring configuration and also the main web-security-configuration for
 * the web-service-part of the SEBServer.
 * 
 * 
 * The web-service is designed as a stateless Rest based web-service defining various rest
 * endpoints secured with OAuth (2) and some (web-socket) endpoints using specialized
 * authentication filter. */
@Configuration
@EnableWebSecurity
@WebServiceProfile
@Order(1)
public class WebServiceConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        System.out.println("**************** WebServiceWebConfig: ");
        //@formatter:off
        http
            // The Web-Service is designed as a stateless Rest API
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
            .antMatcher("/webservice/**")
            .authorizeRequests()
            .anyRequest()
            .authenticated()
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
