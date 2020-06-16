/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@Lazy
@Service
@WebServiceProfile
public class WebserviceInfo {

    private static final Logger log = LoggerFactory.getLogger(WebserviceInfo.class);

    private static final String VERSION_KEY = "sebserver.version";
    private static final String WEB_SERVICE_TEST_PROPERTY = "sebserver.test.property";
    private static final String WEB_SERVICE_SERVER_NAME_KEY = "sebserver.webservice.http.external.servername";
    private static final String WEB_SERVICE_HTTP_SCHEME_KEY = "sebserver.webservice.http.external.scheme";
    private static final String WEB_SERVICE_HTTP_PORT = "sebserver.webservice.http.external.port";
    private static final String WEB_SERVICE_HOST_ADDRESS_KEY = "server.address";
    private static final String WEB_SERVICE_SERVER_PORT_KEY = "server.port";
    private static final String WEB_SERVICE_EXAM_API_DISCOVERY_ENDPOINT_KEY =
            "sebserver.webservice.api.exam.endpoint.discovery";
    private static final String WEB_SERVICE_EXTERNAL_ADDRESS_ALIAS = "sebserver.webservice.lms.address.alias";

    private final String sebServerVersion;
    private final String testProperty;
    private final String httpScheme;
    private final String hostAddress; // internal
    private final String webserverName; // external
    private final String serverPort; // internal
    private final String webserverPort; // external
    private final String discoveryEndpoint;

    private final String serverURLPrefix;
    private final boolean isDistributed;

    private Map<String, String> lmsExternalAddressAlias;

    public WebserviceInfo(final Environment environment) {
        this.sebServerVersion = environment.getRequiredProperty(VERSION_KEY);
        this.testProperty = environment.getProperty(WEB_SERVICE_TEST_PROPERTY, "NOT_AVAILABLE");
        this.httpScheme = environment.getRequiredProperty(WEB_SERVICE_HTTP_SCHEME_KEY);
        this.hostAddress = environment.getRequiredProperty(WEB_SERVICE_HOST_ADDRESS_KEY);
        this.webserverName = environment.getProperty(WEB_SERVICE_SERVER_NAME_KEY, "");
        this.serverPort = environment.getRequiredProperty(WEB_SERVICE_SERVER_PORT_KEY);
        this.webserverPort = environment.getProperty(WEB_SERVICE_HTTP_PORT);
        this.discoveryEndpoint = environment.getRequiredProperty(WEB_SERVICE_EXAM_API_DISCOVERY_ENDPOINT_KEY);

        if (StringUtils.isEmpty(this.webserverName)) {
            log.warn("NOTE: External server name, property : 'sebserver.webservice.http.external.servername' "
                    + "is not set from configuration. The external server name is set to the server address!");
        }

        final UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme(this.httpScheme)
                .host((StringUtils.isNotBlank(this.webserverName))
                        ? this.webserverName
                        : this.hostAddress);
        if (StringUtils.isNotBlank(this.webserverPort)) {
            builder.port(this.webserverPort);
        }

        this.serverURLPrefix = builder.toUriString();

        this.isDistributed = BooleanUtils.toBoolean(environment.getProperty(
                "sebserver.webservice.distributed",
                Constants.FALSE_STRING));

        final String addressAlias = environment.getProperty(WEB_SERVICE_EXTERNAL_ADDRESS_ALIAS, "");
        if (StringUtils.isNotBlank(addressAlias)) {
            try {
                final String[] aliass = StringUtils.split(addressAlias, Constants.LIST_SEPARATOR);
                final Map<String, String> mapping = new LinkedHashMap<>();
                for (final String alias : aliass) {
                    final String[] nameValue =
                            StringUtils.split(alias, Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR);
                    mapping.put(nameValue[0], nameValue[1]);
                }
                this.lmsExternalAddressAlias = Collections.unmodifiableMap(mapping);
            } catch (final Exception e) {
                log.error("Failed to parse sebserver.webservice.lms.address.alias: ", e);
                this.lmsExternalAddressAlias = Collections.emptyMap();
            }
        } else {
            this.lmsExternalAddressAlias = Collections.emptyMap();
        }
    }

    public String getSEBServerVersion() {
        return this.sebServerVersion;
    }

    public String getTestProperty() {
        return this.testProperty;
    }

    public String getHttpScheme() {
        return this.httpScheme;
    }

    public String getHostAddress() {
        return this.hostAddress;
    }

    public String getWebserviceDomainName() {
        return this.webserverName;
    }

    public String getServerPort() {
        return this.serverPort;
    }

    public String getServerExternalPort() {
        return this.webserverPort;
    }

    public String getDiscoveryEndpoint() {
        return this.discoveryEndpoint;
    }

    public String getDiscoveryEndpointAddress() {
        return this.serverURLPrefix + this.discoveryEndpoint;
    }

    public String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            return null;
        }
    }

    public String getLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (final UnknownHostException e) {
            return null;
        }
    }

    public String getLoopbackHostName() {
        return InetAddress.getLoopbackAddress().getHostName();
    }

    public String getLoopbackHostAddress() {
        return InetAddress.getLoopbackAddress().getHostAddress();
    }

    /** Get the server URL prefix in form of;
     * [scheme{http|https}]://[server-address{DNS-name|IP}]:[port]
     *
     * E.g.: https://seb.server.ch:8080
     *
     * @return the server URL prefix */
    public String getExternalServerURL() {
        return this.serverURLPrefix;
    }

    public boolean isDistributed() {
        return this.isDistributed;
    }

    public Map<String, String> getLmsExternalAddressAlias() {
        return this.lmsExternalAddressAlias;
    }

    public String getLmsExternalAddressAlias(final String internalAddress) {
        return this.lmsExternalAddressAlias
                .entrySet()
                .stream()
                .filter(entry -> internalAddress.contains(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("WebserviceInfo [testProperty=");
        builder.append(this.testProperty);
        builder.append(", httpScheme=");
        builder.append(this.httpScheme);
        builder.append(", hostAddress=");
        builder.append(this.hostAddress);
        builder.append(", webserverName=");
        builder.append(this.webserverName);
        builder.append(", serverPort=");
        builder.append(this.serverPort);
        builder.append(", webserverPort=");
        builder.append(this.webserverPort);
        builder.append(", discoveryEndpoint=");
        builder.append(this.discoveryEndpoint);
        builder.append(", serverURLPrefix=");
        builder.append(this.serverURLPrefix);
        builder.append(", isDistributed=");
        builder.append(this.isDistributed);
        builder.append(", lmsExternalAddressAlias=");
        builder.append(this.lmsExternalAddressAlias);
        builder.append("]");
        return builder.toString();
    }

}
