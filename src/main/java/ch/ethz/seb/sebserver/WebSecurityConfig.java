/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CharacterEncodingFilter;

/** This is the overall seb-server Spring web-configuration that is loaded for all profiles.
 * Defines some overall web-security beans needed on both -- web-service and web-gui -- profiles */
@Configuration
@RestController
public class WebSecurityConfig {

    @Value("${sebserver.webservice.http.redirect.gui}")
    private String guiRedirect;
    @Value("${sebserver.webservice.api.exam.endpoint.discovery}")
    private String examAPIDiscoveryEndpoint;
    @Value("${sebserver.webservice.api.admin.endpoint}")
    private String adminAPIEndpoint;

    @Bean
    @Primary
    public JSONMapper jsonMapper() {
        return new JSONMapper();
    }

    /** Password encoder used for user passwords (stronger protection) */
    @Bean
    public PasswordEncoder userPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> filterRegistrationBean() {
        final FilterRegistrationBean<CharacterEncodingFilter> registrationBean = new FilterRegistrationBean<>();
        final CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setForceEncoding(true);
        characterEncodingFilter.setEncoding("UTF-8");
        registrationBean.setFilter(characterEncodingFilter);
        return registrationBean;
    }
}
