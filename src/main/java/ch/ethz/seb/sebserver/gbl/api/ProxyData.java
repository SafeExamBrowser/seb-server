/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

import ch.ethz.seb.sebserver.gbl.util.Utils;

public class ProxyData {

    public enum ProxyAuthType {
        NONE,
        BASIC_AUTH
    }

    public final ProxyAuthType proxyAuthType;
    public final String proxyName;
    public final int proxyPort;
    public final CharSequence proxyAuthUsername;
    public final CharSequence proxyAuthSecret;

    public ProxyData(
            final ProxyAuthType proxyAuthType,
            final String proxyName,
            final int proxyPort,
            final CharSequence proxyAuthUsername,
            final CharSequence proxyAuthSecret) {

        this.proxyAuthType = proxyAuthType;
        this.proxyName = proxyName;
        this.proxyPort = proxyPort;
        this.proxyAuthUsername = proxyAuthUsername;
        this.proxyAuthSecret = proxyAuthSecret;
    }

    public String getProxyName() {
        return this.proxyName;
    }

    public int getProxyPort() {
        return this.proxyPort;
    }

    public ProxyAuthType getProxyAuthType() {
        return this.proxyAuthType;
    }

    public CharSequence getProxyAuthUsername() {
        return this.proxyAuthUsername;
    }

    public CharSequence getProxyAuthSecret() {
        return this.proxyAuthSecret;
    }

    public String getProxyAuthUsernameAsString() {
        return Utils.toString(this.proxyAuthUsername);
    }

    public String getProxyAuthSecretAsString() {
        return Utils.toString(this.proxyAuthSecret);
    }

}
