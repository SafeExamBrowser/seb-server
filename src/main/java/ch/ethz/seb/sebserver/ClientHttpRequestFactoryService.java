/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
//import org.apache.http.HttpHost;
//import org.apache.http.auth.AuthScope;
//import org.apache.http.auth.UsernamePasswordCredentials;
//import org.apache.http.client.CredentialsProvider;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.CredentialsProvider;
//import org.apache.http.client.config.RequestConfig;
//import org.apache.http.conn.ssl.TrustAllStrategy;
//import org.apache.http.impl.client.*;
//import org.apache.http.ssl.SSLContextBuilder;
//import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;

import javax.net.ssl.SSLContext;


@Lazy
@Service
public class ClientHttpRequestFactoryService {

    private static final Logger log = LoggerFactory.getLogger(ClientHttpRequestFactoryService.class);

    private static final Collection<String> DEV_PROFILES = Arrays.asList("dev", "test", "demo", "e2e");
    private static final Collection<String> PROD_PROFILES = Arrays.asList("prod");

    private final int connectTimeout;
    private final int connectionRequestTimeout;
    private final int readTimeout;

    private final Environment environment;
    private final ClientCredentialService clientCredentialService;

    public ClientHttpRequestFactoryService(
            final Environment environment,
            final ClientCredentialService clientCredentialService,
            @Value("${sebserver.http.client.connect-timeout:15000}") final int connectTimeout,
            @Value("${sebserver.http.client.connection-request-timeout:20000}") final int connectionRequestTimeout,
            @Value("${sebserver.http.client.read-timeout:30000}") final int readTimeout) {

        this.environment = environment;
        this.clientCredentialService = clientCredentialService;
        this.connectTimeout = connectTimeout;
        this.connectionRequestTimeout = connectionRequestTimeout;
        this.readTimeout = readTimeout;
    }

    public Result<ClientHttpRequestFactory> getClientHttpRequestFactory() {
        return getClientHttpRequestFactory(null);
    }

    public Result<ClientHttpRequestFactory> getClientHttpRequestFactory(final ProxyData proxy) {
        return Result.tryCatch(() -> {
            final List<String> activeProfiles = Arrays.asList(this.environment.getActiveProfiles());
            if (CollectionUtils.containsAny(activeProfiles, DEV_PROFILES)) {
                return clientHttpRequestFactory(proxy);
            } else if (CollectionUtils.containsAny(activeProfiles, PROD_PROFILES)) {
                return clientHttpRequestFactoryTLS(proxy);
            } else {
                throw new IllegalStateException("Unknown or invalid Spring profile setup: " + activeProfiles);
            }
        });
    }

    /** A ClientHttpRequestFactory for development profile with no TSL SSL protocol and
     * not following redirects on redirect responses.
     *
     * @return ClientHttpRequestFactory bean for development profiles*/
    private ClientHttpRequestFactory clientHttpRequestFactory(final ProxyData proxy)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

        if (log.isDebugEnabled()) {
            log.debug("Initialize ClientHttpRequestFactory with insecure ClientHttpRequestFactory for development");
        }

        SSLContext sslContext = SSLContextBuilder
                .create()
                .loadTrustMaterial(null, new TrustAllStrategy())
                .build();

        return getRequestFactory(proxy, sslContext);
    }

    /** A ClientHttpRequestFactory used in production with TSL SSL configuration.
     *
     * @return ClientHttpRequestFactory with TLS / SSL configuration*/
    private ClientHttpRequestFactory clientHttpRequestFactoryTLS(final ProxyData proxy) throws KeyManagementException,
            NoSuchAlgorithmException, KeyStoreException, CertificateException, FileNotFoundException, IOException {

        if (log.isDebugEnabled()) {
            log.debug("Initialize with secure ClientHttpRequestFactory for production");
        }

        final String truststoreFilePath = this.environment
                .getProperty("server.ssl.trust-store", "");

        final SSLContext sslContext;

        if (StringUtils.isBlank(truststoreFilePath)) {

            if (log.isDebugEnabled()) {
                log.debug("Securing outgoing calls without trust-store by trusting all certificates");
            }

            sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(null, new TrustAllStrategy())
                    .build();

        } else {

            if (log.isDebugEnabled()) {
                log.debug("Securing with defined trust-store");
            }

            final File trustStoreFile = ResourceUtils.getFile("file:" + truststoreFilePath);

            final char[] password = this.environment
                    .getProperty("server.ssl.trust-store-password", "")
                    .toCharArray();

            if (password.length < 3) {
                log.error("Missing or incorrect trust-store password");
                throw new IllegalArgumentException("Missing or incorrect trust-store password");
            }

            // Set the specified trust-store also on javax.net.ssl level
            System.setProperty("javax.net.ssl.trustStore", truststoreFilePath);
            System.setProperty("javax.net.ssl.trustStorePassword", String.valueOf(password));

            sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(trustStoreFile, password)
                    .setKeyStoreType(this.environment.getProperty(
                            "server.ssl.key-store-type",
                            Constants.PKCS_12))
                    .build();
        }

        return getRequestFactory(proxy, sslContext);
    }

    private HttpComponentsClientHttpRequestFactory getRequestFactory(ProxyData proxy, SSLContext sslContext) {
        if (proxy != null) {

            if (log.isDebugEnabled()) {
                log.debug("Initialize ClientHttpRequestFactory with proxy: {}", proxy);
            }

            CloseableHttpClient httpClient = createClient(proxy, sslContext);
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            return requestFactory;
        } else {
            CloseableHttpClient httpClient = createClient(null, sslContext);
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            return requestFactory;
        }
    }

    private CloseableHttpClient createClient(final ProxyData proxy, SSLContext sslcontext) {

        if (proxy != null) {
            final String plainClientId = proxy.clientCredentials.clientIdAsString();
            final CharSequence secret = this.clientCredentialService
                    .getPlainClientSecret(proxy.clientCredentials)
                    .getOrThrow();
            final String plainClientSecret = Utils.toString(secret);

            BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(proxy.proxyName, proxy.proxyPort),
                    new UsernamePasswordCredentials(plainClientId, plainClientSecret.toCharArray())
            );

            SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(sslcontext, new NoopHostnameVerifier());
            PoolingHttpClientConnectionManager connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslConSocFactory).build();

            HttpHost myProxy = new HttpHost(proxy.proxyName, proxy.proxyPort);
            HttpClientBuilder clientBuilder = HttpClientBuilder.create();
            clientBuilder.setProxy(myProxy).setDefaultCredentialsProvider(credsProvider).disableCookieManagement();
            clientBuilder.setConnectionManager(connectionManagerBuilder);

            return clientBuilder.build();
        } else {
            SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(sslcontext, new NoopHostnameVerifier());
            PoolingHttpClientConnectionManager connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslConSocFactory).build();
            return HttpClientBuilder.create().setConnectionManager(connectionManagerBuilder).build();
        }
    }

}
