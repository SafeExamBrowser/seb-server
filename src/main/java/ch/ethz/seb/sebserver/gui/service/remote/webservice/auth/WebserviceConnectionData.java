/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.auth;

import org.springframework.web.util.UriComponentsBuilder;

public class WebserviceConnectionData {

    final String id;
    final String webserviceProtocol;
    final String webserviceServerAddress;
    final String webserviceServerPort;
    final String webserviceAPIPath;
    final String webserviceServerURL;

    private final UriComponentsBuilder webserviceURIBuilder;

    protected WebserviceConnectionData(
            final String id,
            final String webserviceProtocol,
            final String webserviceServerAddress,
            final String webserviceServerPort,
            final String webserviceAPIPath) {

        this.id = id;
        this.webserviceProtocol = webserviceProtocol;
        this.webserviceServerAddress = webserviceServerAddress;
        this.webserviceServerPort = webserviceServerPort;
        this.webserviceAPIPath = webserviceAPIPath;

        this.webserviceServerURL = webserviceProtocol + "://" + webserviceServerAddress + ":" + webserviceServerPort;
        this.webserviceURIBuilder = UriComponentsBuilder
                .fromHttpUrl(webserviceProtocol + "://" + webserviceServerAddress)
                .port(webserviceServerPort)
                .path(webserviceAPIPath);
    }

    public String getId() {
        return this.id;
    }

    public String getWebserviceProtocol() {
        return this.webserviceProtocol;
    }

    public String getWebserviceServerAddress() {
        return this.webserviceServerAddress;
    }

    public String getWebserviceServerPort() {
        return this.webserviceServerPort;
    }

    public String getWebserviceAPIPath() {
        return this.webserviceAPIPath;
    }

    public String getWebserviceServerURL() {
        return this.webserviceServerURL;
    }

    public UriComponentsBuilder getWebserviceURIBuilder() {
        return this.webserviceURIBuilder.cloneBuilder();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final WebserviceConnectionData other = (WebserviceConnectionData) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        } else if (!this.id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("WebserviceConnectionData [id=");
        builder.append(this.id);
        builder.append(", webserviceProtocol=");
        builder.append(this.webserviceProtocol);
        builder.append(", webserviceServerAddress=");
        builder.append(this.webserviceServerAddress);
        builder.append(", webserviceServerPort=");
        builder.append(this.webserviceServerPort);
        builder.append(", webserviceAPIPath=");
        builder.append(this.webserviceAPIPath);
        builder.append("]");
        return builder.toString();
    }

}
