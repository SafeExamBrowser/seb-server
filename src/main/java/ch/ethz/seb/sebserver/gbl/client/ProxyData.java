/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.client;

public class ProxyData {

    public final String proxyName;
    public final int proxyPort;
    public final ClientCredentials clientCredentials;

    public ProxyData(
            final String proxyName,
            final Integer proxyPort,
            final ClientCredentials clientCredentials) {

        this.proxyName = proxyName;
        this.proxyPort = (proxyPort != null) ? proxyPort : -1;
        this.clientCredentials = clientCredentials;
    }

    public String getProxyName() {
        return this.proxyName;
    }

    public int getProxyPort() {
        return this.proxyPort;
    }

    public ClientCredentials getClientCredentials() {
        return this.clientCredentials;
    }

    @Override
    public String toString() {
        return "ProxyData [proxyName=" + this.proxyName +
                ", proxyPort=" + this.proxyPort +
                ", clientCredentials=" + this.clientCredentials +
                "]";
    }

}
