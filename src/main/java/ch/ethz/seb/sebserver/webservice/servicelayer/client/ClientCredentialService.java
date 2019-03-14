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

    Result<ClientCredentials> createGeneratedClientCredentials(CharSequence salt);

    ClientCredentials encryptedClientCredentials(ClientCredentials clientCredentials);

    ClientCredentials encryptedClientCredentials(
            ClientCredentials clientCredentials,
            CharSequence salt);

    CharSequence getPlainClientId(ClientCredentials credentials);

    CharSequence getPlainClientId(ClientCredentials credentials, CharSequence salt);

    CharSequence getPlainClientSecret(ClientCredentials credentials);

    CharSequence getPlainClientSecret(ClientCredentials credentials, CharSequence salt);

    CharSequence getPlainAccessToken(ClientCredentials credentials);

    CharSequence getPlainAccessToken(ClientCredentials credentials, CharSequence salt);

}