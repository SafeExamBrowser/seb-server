/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.client;

import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ClientCredentialService {

    Result<ClientCredentials> createGeneratedClientCredentials(CharSequence salt);

    default Result<ClientCredentials> createGeneratedClientCredentials() {
        return createGeneratedClientCredentials(null);
    }

    ClientCredentials encryptClientCredentials(
            CharSequence clientIdPlaintext,
            CharSequence secretPlaintext,
            CharSequence accessTokenPlaintext,
            CharSequence salt);

    default ClientCredentials encryptClientCredentials(
            final CharSequence clientIdPlaintext,
            final CharSequence secretPlaintext) {

        return encryptClientCredentials(clientIdPlaintext, secretPlaintext, null, null);
    }

    default ClientCredentials encryptClientCredentials(
            final CharSequence clientIdPlaintext,
            final CharSequence secretPlaintext,
            final CharSequence accessTokenPlaintext) {

        return encryptClientCredentials(clientIdPlaintext, secretPlaintext, accessTokenPlaintext, null);
    }

    CharSequence getPlainClientId(ClientCredentials credentials, CharSequence salt);

    default CharSequence getPlainClientId(final ClientCredentials credentials) {
        return getPlainClientId(credentials, null);
    }

    CharSequence getPlainClientSecret(ClientCredentials credentials, CharSequence salt);

    default CharSequence getPlainClientSecret(final ClientCredentials credentials) {
        return getPlainClientSecret(credentials, null);
    }

    CharSequence getPlainAccessToken(ClientCredentials credentials, CharSequence salt);

    default CharSequence getPlainAccessToken(final ClientCredentials credentials) {
        return getPlainAccessToken(credentials, null);
    }

    CharSequence encrypt(final CharSequence text);

    CharSequence decrypt(final CharSequence text);

}