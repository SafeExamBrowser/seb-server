/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@Order(6)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter implements ErrorController {

    @Value("${sebserver.webservice.api.redirect.unauthorized}")
    private String unauthorizedRedirect;

    /** Spring bean name of user password encoder */
    public static final String USER_PASSWORD_ENCODER_BEAN_NAME = "userPasswordEncoder";
    /** Spring bean name of client (application) password encoder */
    public static final String CLIENT_PASSWORD_ENCODER_BEAN_NAME = "clientPasswordEncoder";

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
                .antMatchers("/error");
    }

    @RequestMapping("/error")
    public void handleError(final HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.sendRedirect(this.unauthorizedRedirect);
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
        // TODO set connection and read timeout!? configurable!?
        return new SimpleClientHttpRequestFactory() {

            @Override
            protected void prepareConnection(final HttpURLConnection connection, final String httpMethod)
                    throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
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

        final char[] password = env
                .getProperty("sebserver.gui.truststore.pwd")
                .toCharArray();

        final SSLContext sslContext = SSLContextBuilder
                .create()
                .loadTrustMaterial(ResourceUtils.getFile(
                        "classpath:truststore.jks"),
                        password)
                .build();

        final HttpClient client = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        // TODO set connection and read timeout!? configurable!?
        return new HttpComponentsClientHttpRequestFactory(client);
    }

}
