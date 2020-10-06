/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Configuration
@GuiProfile
@Order(5)
public class GuiWebsecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private InstitutionalAuthenticationEntryPoint institutionalAuthenticationEntryPoint;

    @Value("${sebserver.gui.entrypoint:/gui}")
    private String guiEntryPoint;
    @Value("${sebserver.gui.remote.proctoring.entrypoint:/remote-proctoring}")
    private String remoteProctoringEndpoint;

    /** Gui-service related public URLS from spring web security perspective */
    public static final RequestMatcher PUBLIC_URLS = new OrRequestMatcher(
            // OAuth entry-points
            new AntPathRequestMatcher(API.OAUTH_REVOKE_TOKEN_ENDPOINT),
            // RAP/RWT resources has to be accessible
            new AntPathRequestMatcher("/rwt-resources/**"),
            // project specific static resources
            new AntPathRequestMatcher("/images/**"),

            new AntPathRequestMatcher("/favicon.ico"));

    @Override
    public void configure(final WebSecurity web) {
        web
                .ignoring()
                .requestMatchers(PUBLIC_URLS)
                .antMatchers(this.guiEntryPoint)
                .antMatchers(this.remoteProctoringEndpoint + "/*");
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
                .authenticationEntryPoint(this.institutionalAuthenticationEntryPoint)
                .and()
                .formLogin().disable()
                .httpBasic().disable()
                .logout().disable()
                .headers().frameOptions().disable()
                .and()
                .csrf().disable();
    }

}
