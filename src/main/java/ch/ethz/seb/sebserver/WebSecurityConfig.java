/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CharacterEncodingFilter;

import ch.ethz.seb.sebserver.gbl.profile.DevGuiProfile;
import ch.ethz.seb.sebserver.gbl.profile.DevWebServiceProfile;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.profile.ProdGuiProfile;
import ch.ethz.seb.sebserver.gbl.profile.ProdWebServiceProfile;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

/** This is the overall seb-server Spring web-configuration that is loaded for all profiles.
 * Defines some overall web-security beans needed on both -- web-service and web-gui -- profiles */
@Configuration
@WebServiceProfile
@GuiProfile
@RestController
@Order(7)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);

//    @Value("${sebserver.webservice.api.admin.endpoint}")
//    private String adminEndpoint;
    @Value("${sebserver.webservice.api.redirect.unauthorized}")
    private String unauthorizedRedirect;
    @Value("${sebserver.webservice.api.exam.endpoint.discovery}")
    private String examAPIDiscoveryEndpoint;

    /** Spring bean name of user password encoder */
    public static final String USER_PASSWORD_ENCODER_BEAN_NAME = "userPasswordEncoder";
    /** Spring bean name of client (application) password encoder */
    public static final String CLIENT_PASSWORD_ENCODER_BEAN_NAME = "clientPasswordEncoder";

    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> filterRegistrationBean() {
        final FilterRegistrationBean<CharacterEncodingFilter> registrationBean = new FilterRegistrationBean<>();
        final CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setForceEncoding(true);
        characterEncodingFilter.setEncoding("UTF-8");
        registrationBean.setFilter(characterEncodingFilter);
        return registrationBean;
    }

    /** Password encoder used for user passwords (stronger protection) */
    @Bean(USER_PASSWORD_ENCODER_BEAN_NAME)
    public PasswordEncoder userPasswordEncoder() {
        return new BCryptPasswordEncoder(8);
    }

    /** Password encode used for client (application) passwords */
    @Bean(CLIENT_PASSWORD_ENCODER_BEAN_NAME)
    public PasswordEncoder clientPasswordEncoder() {
        return new BCryptPasswordEncoder(4);
    }

    @Override
    public void configure(final WebSecurity web) {
        web
                .ignoring()
                .antMatchers("/error")
                .antMatchers(this.examAPIDiscoveryEndpoint)
                .and();
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
                .permitAll()
                .and()
                .exceptionHandling()
                .accessDeniedHandler(new OAuth2AccessDeniedHandler());
    }

    @RequestMapping("/error")
    public void handleError(final HttpServletResponse response) throws IOException {
        //response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader(HttpHeaders.LOCATION, this.unauthorizedRedirect);
        response.flushBuffer();
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }

    /** A ClientHttpRequestFactory for development profile with no TSL SSL protocol and
     * not following redirects on redirect responses.
     *
     * @return ClientHttpRequestFactory bean for development profiles */
    @Bean
    @DevGuiProfile
    @DevWebServiceProfile
    public ClientHttpRequestFactory clientHttpRequestFactory() {

        log.info("Initialize with insecure ClientHttpRequestFactory for development");

        return new DevClientHttpRequestFactory();
    }

    /** A ClientHttpRequestFactory used in production with TSL SSL configuration.
     *
     * NOTE:
     * environment property: sebserver.gui.truststore.pwd is expected to have the correct truststore password set
     * environment property: sebserver.gui.truststore.type is expected to set to the correct type of truststore
     * truststore.jks is expected to be on the classpath containing all trusted certificates for request
     * to SSL secured SEB Server webservice
     *
     * @return ClientHttpRequestFactory with TLS / SSL configuration
     * @throws IOException
     * @throws FileNotFoundException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException */
    @Bean
    @ProdGuiProfile
    @ProdWebServiceProfile
    public ClientHttpRequestFactory clientHttpRequestFactoryTLS(final Environment env) throws KeyManagementException,
            NoSuchAlgorithmException, KeyStoreException, CertificateException, FileNotFoundException, IOException {

        log.info("Initialize with secure ClientHttpRequestFactory for production");

        final char[] password = env
                .getProperty("sebserver.gui.truststore.pwd", "")
                .toCharArray();

        if (password.length < 3) {
            log.error("Missing or incorrect trust-store password: " + String.valueOf(password));
            throw new IllegalArgumentException("Missing or incorrect trust-store password");
        }

        final File trustStoreFile = ResourceUtils.getFile("classpath:truststore.jks");

        final SSLContext sslContext = SSLContextBuilder
                .create()
                .loadTrustMaterial(trustStoreFile, password)
                .build();

        final HttpClient client = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        // TODO set connection and read timeout!? configurable!?
        return new HttpComponentsClientHttpRequestFactory(client);
    }

    // TODO set connection and read timeout!? configurable!?
    private static class DevClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

        @Override
        protected void prepareConnection(
                final HttpURLConnection connection,
                final String httpMethod) throws IOException {

            super.prepareConnection(connection, httpMethod);
            connection.setInstanceFollowRedirects(false);
        }
    }

}
