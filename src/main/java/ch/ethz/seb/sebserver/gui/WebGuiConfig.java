/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Configuration
@EnableWebSecurity
@GuiProfile
@Order(2)
public class WebGuiConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        System.out.println("**************** GuiWebConfig: ");
        //@formatter:off
        http
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
            .antMatcher("/gui/**")
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
      //@formatter:on
    }
}
