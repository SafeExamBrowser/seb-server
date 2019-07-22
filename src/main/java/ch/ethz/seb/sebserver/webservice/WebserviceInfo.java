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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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

    private static final String WEB_SERVICE_SERVER_NAME_KEY = "sebserver.webservice.http.server.name";
    private static final String WEB_SERVICE_HTTP_SCHEME_KEY = "sebserver.webservice.http.scheme";
    private static final String WEB_SERVICE_HOST_ADDRESS_KEY = "server.address";
    private static final String WEB_SERVICE_SERVER_PORT_KEY = "server.port";
    private static final String WEB_SERVICE_EXAM_API_DISCOVERY_ENDPOINT_KEY =
            "sebserver.webservice.api.exam.endpoint.discovery";

    private final String httpScheme;
    private final String hostAddress; // internal
    private final String serverName; // external
    private final String serverPort;
    private final String discoveryEndpoint;

    private final String serverURLPrefix;

    private final boolean isDistributed;

    public WebserviceInfo(final Environment environment) {
        this.httpScheme = environment.getRequiredProperty(WEB_SERVICE_HTTP_SCHEME_KEY);
        this.hostAddress = environment.getRequiredProperty(WEB_SERVICE_HOST_ADDRESS_KEY);
        this.serverName = environment.getProperty(WEB_SERVICE_SERVER_NAME_KEY, "");
        this.serverPort = environment.getRequiredProperty(WEB_SERVICE_SERVER_PORT_KEY);
        this.discoveryEndpoint = environment.getRequiredProperty(WEB_SERVICE_EXAM_API_DISCOVERY_ENDPOINT_KEY);

        this.serverURLPrefix = UriComponentsBuilder.newInstance()
                .scheme(this.httpScheme)
                .host((StringUtils.isNotBlank(this.serverName))
                        ? this.serverName
                        : this.hostAddress)
                .port(this.serverPort)
                .toUriString();

        this.isDistributed = BooleanUtils.toBoolean(environment.getProperty(
                "sebserver.webservice.distributed",
                Constants.FALSE_STRING));
    }

    public String getHttpScheme() {
        return this.httpScheme;
    }

    public String getHostAddress() {
        return this.hostAddress;
    }

    public String getServerName() {
        return this.serverName;
    }

    public String getServerPort() {
        return this.serverPort;
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
    public String getServerURL() {
        return this.serverURLPrefix;
    }

    public boolean isDistributed() {
        return this.isDistributed;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("WebserviceInfo [httpScheme=");
        builder.append(this.httpScheme);
        builder.append(", hostAddress=");
        builder.append(this.hostAddress);
        builder.append(", serverName=");
        builder.append(this.serverName);
        builder.append(", serverPort=");
        builder.append(this.serverPort);
        builder.append(", discoveryEndpoint=");
        builder.append(this.discoveryEndpoint);
        builder.append(", serverURLPrefix=");
        builder.append(this.serverURLPrefix);
        builder.append(", getLocalHostName()=");
        builder.append(getLocalHostName());
        builder.append(", getLocalHostAddress()=");
        builder.append(getLocalHostAddress());
        builder.append(", getLoopbackHostName()=");
        builder.append(getLoopbackHostName());
        builder.append(", getLoopbackHostAddress()=");
        builder.append(getLoopbackHostAddress());
        builder.append("]");
        return builder.toString();
    }

}
