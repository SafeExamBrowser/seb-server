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
import java.net.HttpURLConnection;
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
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

import ch.ethz.seb.sebserver.gbl.api.ProxyData;
import ch.ethz.seb.sebserver.gbl.api.ProxyData.ProxyAuthType;
import ch.ethz.seb.sebserver.gbl.util.Result;

@Service
public class ClientHttpRequestFactoryService {

    private static final Logger log = LoggerFactory.getLogger(ClientHttpRequestFactoryService.class);

    private static final Collection<String> DEV_PROFILES = Arrays.asList("dev-gui", "test", "demo", "dev-ws");
    private static final Collection<String> PROD_PROFILES = Arrays.asList("prod-gui", "prod-ws");

    private final Environment environment;

    public ClientHttpRequestFactoryService(final Environment environment) {
        this.environment = environment;
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

        log.info("Initialize ClientHttpRequestFactory with insecure ClientHttpRequestFactory for development");

        if (proxy != null && proxy.proxyAuthType != null && proxy.proxyAuthType != ProxyAuthType.NONE) {

            log.info("Initialize ClientHttpRequestFactory with proxy auth: {} : {}",
                    proxy.proxyAuthType,
                    proxy.proxyName);

            final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setHttpClient(this.createProxiedClient(proxy, null));
            return factory;

        } else {
            final DevClientHttpRequestFactory devClientHttpRequestFactory = new DevClientHttpRequestFactory();
            devClientHttpRequestFactory.setOutputStreaming(false);
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

        log.info("Initialize with secure ClientHttpRequestFactory for production");

        final String truststoreFilePath = this.environment
                .getProperty("server.ssl.trust-store", "");

        SSLContext sslContext = null;
        if (StringUtils.isBlank(truststoreFilePath)) {

            log.info("Securing outgoing calls without trust-store by trusting all certificates");

            sslContext = org.apache.http.ssl.SSLContexts
                    .custom()
                    .loadTrustMaterial(null, new TrustAllStrategy())
                    .build();

        } else {

            log.info("Securing with defined trust-store");

            final File trustStoreFile = ResourceUtils.getFile("file:" + truststoreFilePath);

            final char[] password = this.environment
                    .getProperty("server.ssl.trust-store-password", "")
                    .toCharArray();

            if (password.length < 3) {
                log.error("Missing or incorrect trust-store password: " + String.valueOf(password));
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

        if (proxy != null &&
                proxy.proxyAuthType != null &&
                proxy.proxyAuthType != ProxyAuthType.NONE) {

            log.info("Initialize ClientHttpRequestFactory with proxy auth: {} : {}",
                    proxy.proxyAuthType,
                    proxy.proxyName);

            final HttpClient client = createProxiedClient(proxy, sslContext);
            return new HttpComponentsClientHttpRequestFactory(client);
        } else {

            final HttpClient client = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .build();
            return new HttpComponentsClientHttpRequestFactory(client);
        }
    }

    // TODO set connection and read timeout!? configurable!?
    private HttpClient createProxiedClient(final ProxyData proxy, final SSLContext sslContext) {

        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(
                        proxy.getProxyAuthUsernameAsString(),
                        proxy.getProxyAuthSecretAsString()));

        final HttpClientBuilder clientBuilder = HttpClients
                .custom()
                .useSystemProperties()
                .setProxy(new HttpHost(proxy.proxyName,
                        proxy.proxyPort))
                .setDefaultCredentialsProvider(credsProvider)
                .setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());

        if (sslContext != null) {
            clientBuilder.setSSLContext(sslContext);
        }

        return clientBuilder.build();
    }

    // TODO set connection and read timeout!? configurable!?
    private static class DevClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

        @Override
        protected void prepareConnection(
                final HttpURLConnection connection,
                final String httpMethod) throws IOException {

            super.prepareConnection(connection, httpMethod);
            super.setBufferRequestBody(false);
            connection.setInstanceFollowRedirects(false);
        }
    }

}
