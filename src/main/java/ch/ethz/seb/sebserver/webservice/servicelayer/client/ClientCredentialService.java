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

    Result<ClientCredentials> createGeneratedClientCredentials();

    ClientCredentials encryptClientCredentials(
            CharSequence clientIdPlaintext,
            CharSequence secretPlaintext,
            CharSequence accessTokenPlaintext);

    default ClientCredentials encryptClientCredentials(
            final CharSequence clientIdPlaintext,
            final CharSequence secretPlaintext) {

        return encryptClientCredentials(clientIdPlaintext, secretPlaintext, null);
    }

    CharSequence getPlainClientId(ClientCredentials credentials);

    CharSequence getPlainClientSecret(ClientCredentials credentials);

    CharSequence getPlainAccessToken(ClientCredentials credentials);

    CharSequence encrypt(final CharSequence text);

    CharSequence decrypt(final CharSequence cipher);

}