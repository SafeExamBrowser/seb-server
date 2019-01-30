/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.ResourceUtils;

import ch.ethz.seb.sebserver.gbl.profile.DevGuiProfile;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.profile.ProdGuiProfile;

@Configuration
@GuiProfile
public class WebserviceConnectionConfig {

    /** A ClientHttpRequestFactory for development profile with no TSL SSL protocol and
     * not following redirects on redirect responses.
     *
     * @return ClientHttpRequestFactory bean for development profiles */
    @Bean
    @DevGuiProfile
    public ClientHttpRequestFactory clientHttpRequestFactory() {
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

        return new HttpComponentsClientHttpRequestFactory(client);
    }

}
