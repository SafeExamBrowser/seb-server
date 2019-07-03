/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.client;

import ch.ethz.seb.sebserver.gbl.util.Result;

/** Service interface used for internal encryption of client credentials or
 * other credentials to save within the persistent store.
 * <p>
 * There may be credentials needed for various purposes like connecting
 * to other REST API's or passwords for encrypted configurations text files.
 * <p>
 * This service offers functionality to encrypt this credentials with a
 * SEB Server internal password (from SEB Server configuration given on startup)
 * for storing this credentials securely within a persistence storage.
 * And also offers functionality for decryption. */
public interface ClientCredentialService {

    /** Use this to create generated and encrypted ClientCredentials (clientId and secret)
     * The generator creates a random clientId of 16 characters and a random
     * secret of 64 character. This are encrypted within this service encryption function
     * and returned warped within a ClientCredentials instance.
     *
     * @return generated and encrypted ClientCredentials (clientId and secret) */
    Result<ClientCredentials> generatedClientCredentials();

    /** Encrypts the given credentials (if not null) and returns the ciphers of them
     * warped within a ClientCredentials instance
     *
     * @param clientIdPlaintext the plain clientId text
     * @param secretPlaintext the plain secret text
     * @param accessTokenPlaintext the plain accessToken text
     * @return encrypted client credentials */
    ClientCredentials encryptClientCredentials(
            CharSequence clientIdPlaintext,
            CharSequence secretPlaintext,
            CharSequence accessTokenPlaintext);

    /** Encrypts the given credentials (if not null) and returns the ciphers of them
     * warped within a ClientCredentials instance
     *
     * @param clientIdPlaintext the plain clientId text
     * @param secretPlaintext the plain secret text
     * @return encrypted client credentials */
    default ClientCredentials encryptClientCredentials(
            final CharSequence clientIdPlaintext,
            final CharSequence secretPlaintext) {

        return encryptClientCredentials(clientIdPlaintext, secretPlaintext, null);
    }

    /** Use this to get a decrypted plain text secret form given ClientCredentials
     *
     * @param credentials ClientCredentials containing the secret to decrypt
     * @return decrypted plain text secret */
    CharSequence getPlainClientSecret(ClientCredentials credentials);

    /** Use this to get a decrypted plain text accessToken form given ClientCredentials
     *
     * @param credentials ClientCredentials containing the accessToken to decrypt
     * @return decrypted plain text accessToken */
    CharSequence getPlainAccessToken(ClientCredentials credentials);

    /** Encrypts a given plain text that uses {@link org.springframework.security.crypto.encrypt.Encryptors#stronger}
     * and a randomly generated salt that is added to the cipher text.
     * The salt is generated by using {@link org.springframework.security.crypto.keygen.KeyGenerators#string}
     *
     * @param text the plain text to encrypt
     * @return encrypted cipher text with additional salt */
    CharSequence encrypt(final CharSequence text);

    /** Decrypt a given cipher that was encrypted with the method used by this services encrypt method.
     *
     * @param cipher the cipher text with additional salt
     * @return plain text decrypt from the given cipher */
    CharSequence decrypt(final CharSequence cipher);

}