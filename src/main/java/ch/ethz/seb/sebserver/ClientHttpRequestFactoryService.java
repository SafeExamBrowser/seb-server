/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
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

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.ssl.SSLContextBuilder;
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

import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@Lazy
@Service
@WebServiceProfile
@GuiProfile
public class ClientHttpRequestFactoryService {

    private static final Logger log = LoggerFactory.getLogger(ClientHttpRequestFactoryService.class);

    private static final Collection<String> DEV_PROFILES = Arrays.asList("dev-gui", "test", "demo", "dev-ws");
    private static final Collection<String> PROD_PROFILES = Arrays.asList("prod-gui", "prod-ws");

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
            @Value("${sebserver.http.client.read-timeout:10000}") final int readTimeout) {

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
     * @return ClientHttpRequestFactory bean for development profiles */
    private ClientHttpRequestFactory clientHttpRequestFactory(final ProxyData proxy) {

        if (log.isDebugEnabled()) {
            log.debug("Initialize ClientHttpRequestFactory with insecure ClientHttpRequestFactory for development");
        }

        if (proxy != null) {

            if (log.isDebugEnabled()) {
                log.debug("Initialize ClientHttpRequestFactory with proxy: {}", proxy);
            }

            final HttpComponentsClientHttpRequestFactory factory =
                    new HttpComponentsClientHttpRequestFactory();
            factory.setHttpClient(this.createProxiedClient(proxy, null));
            factory.setBufferRequestBody(false);
            factory.setConnectionRequestTimeout(this.connectionRequestTimeout);
            factory.setConnectTimeout(this.connectTimeout);
            factory.setReadTimeout(this.readTimeout);
            return factory;

        } else {

            final HttpComponentsClientHttpRequestFactory devClientHttpRequestFactory =
                    new HttpComponentsClientHttpRequestFactory();

            devClientHttpRequestFactory.setBufferRequestBody(false);
            devClientHttpRequestFactory.setConnectionRequestTimeout(this.connectionRequestTimeout);
            devClientHttpRequestFactory.setConnectTimeout(this.connectTimeout);
            devClientHttpRequestFactory.setReadTimeout(this.readTimeout);
            return devClientHttpRequestFactory;
        }
    }

    /** A ClientHttpRequestFactory used in production with TSL SSL configuration.
     *
     * @return ClientHttpRequestFactory with TLS / SSL configuration
     * @throws IOException
     * @throws FileNotFoundException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException */
    private ClientHttpRequestFactory clientHttpRequestFactoryTLS(final ProxyData proxy) throws KeyManagementException,
            NoSuchAlgorithmException, KeyStoreException, CertificateException, FileNotFoundException, IOException {

        if (log.isDebugEnabled()) {
            log.debug("Initialize with secure ClientHttpRequestFactory for production");
        }

        final String truststoreFilePath = this.environment
                .getProperty("server.ssl.trust-store", "");

        SSLContext sslContext;
        if (StringUtils.isBlank(truststoreFilePath)) {

            if (log.isDebugEnabled()) {
                log.debug("Securing outgoing calls without trust-store by trusting all certificates");
            }

            sslContext = org.apache.http.ssl.SSLContexts
                    .custom()
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
                            "pkcs12"))
                    .build();
        }

        if (proxy != null) {

            if (log.isDebugEnabled()) {
                log.debug("Initialize ClientHttpRequestFactory with proxy: {}", proxy);
            }

            final HttpClient client = createProxiedClient(proxy, sslContext);
            return new HttpComponentsClientHttpRequestFactory(client);
        } else {

            final HttpClient client = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .build();
            final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);
            factory.setConnectionRequestTimeout(this.connectionRequestTimeout);
            factory.setConnectTimeout(this.connectTimeout);
            factory.setReadTimeout(this.readTimeout);
            return factory;
        }
    }

    private HttpClient createProxiedClient(final ProxyData proxy, final SSLContext sslContext) {

        final HttpHost httpHost = new HttpHost(
                proxy.proxyName,
                proxy.proxyPort);

        final HttpClientBuilder clientBuilder = HttpClients
                .custom()
                .useSystemProperties()
                .setProxy(httpHost)

                .setDefaultRequestConfig(RequestConfig
                        .custom()
                        .setRedirectsEnabled(true)
                        .setCircularRedirectsAllowed(true)
                        .build())
                .setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());

        if (proxy.clientCredentials != null && StringUtils.isNotBlank(proxy.clientCredentials.clientId)) {
            final CredentialsProvider credsProvider = new BasicCredentialsProvider();
            final String plainClientId = proxy.clientCredentials.clientIdAsString();
            final String plainClientSecret = Utils.toString(this.clientCredentialService
                    .getPlainClientSecret(proxy.clientCredentials));

            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(plainClientId, plainClientSecret));

            clientBuilder.setDefaultCredentialsProvider(credsProvider);
        }

        if (sslContext != null) {
            clientBuilder.setSSLContext(sslContext);
        }

        return clientBuilder.build();
    }

}
