/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.client;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;

@Lazy
@Service
@WebServiceProfile
public class ClientCredentialService {

    private static final Logger log = LoggerFactory.getLogger(ClientCredentialService.class);

    static final String SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY = "sebserver.webservice.internalSecret";
    static final CharSequence DEFAULT_SALT = "b7dbe99bbfa3e21e";

    private final Environment environment;

    protected ClientCredentialService(final Environment environment) {
        this.environment = environment;
    }

    public Result<ClientCredentials> createGeneratedClientCredentials() {
        return createGeneratedClientCredentials(null);
    }

    public Result<ClientCredentials> createGeneratedClientCredentials(final CharSequence salt) {
        return Result.tryCatch(() -> {

            try {
                return encryptedClientCredentials(
                        new ClientCredentials(
                                generateClientId().toString(),
                                generateClientSecret().toString(),
                                null),
                        salt);
            } catch (final UnsupportedEncodingException e) {
                log.error("Error while trying to generate client credentials: ", e);
                throw new RuntimeException("cause: ", e);
            }
        });
    }

    public ClientCredentials encryptedClientCredentials(final ClientCredentials clientCredentials) {
        return encryptedClientCredentials(clientCredentials, null);
    }

    public ClientCredentials encryptedClientCredentials(
            final ClientCredentials clientCredentials,
            final CharSequence salt) {

        final CharSequence secret = this.environment
                .getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return new ClientCredentials(
                (clientCredentials.clientId != null)
                        ? encrypt(clientCredentials.clientId, secret, salt).toString()
                        : null,
                (clientCredentials.secret != null)
                        ? encrypt(clientCredentials.secret, secret, salt).toString()
                        : null,
                (clientCredentials.accessToken != null)
                        ? encrypt(clientCredentials.accessToken, secret, salt).toString()
                        : null);
    }

    public CharSequence getPlainClientId(final ClientCredentials credentials) {
        return getPlainClientId(credentials, null);
    }

    public CharSequence getPlainClientId(final ClientCredentials credentials, final CharSequence salt) {
        if (credentials == null || credentials.clientId == null) {
            return null;
        }

        final CharSequence secret = this.environment
                .getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return this.decrypt(credentials.clientId, secret, salt);
    }

    public CharSequence getPlainClientSecret(final ClientCredentials credentials) {
        return getPlainClientSecret(credentials, null);
    }

    public CharSequence getPlainClientSecret(final ClientCredentials credentials, final CharSequence salt) {
        if (credentials == null || credentials.secret == null) {
            return null;
        }

        final CharSequence secret = this.environment
                .getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);
        return this.decrypt(credentials.secret, secret, salt);
    }

    public CharSequence getPlainAccessToken(final ClientCredentials credentials) {
        return getPlainAccessToken(credentials, null);
    }

    public CharSequence getPlainAccessToken(final ClientCredentials credentials, final CharSequence salt) {
        if (credentials == null || credentials.accessToken == null) {
            return null;
        }

        final CharSequence secret = this.environment
                .getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);
        return this.decrypt(credentials.accessToken, secret, salt);
    }

    CharSequence encrypt(final CharSequence text, final CharSequence secret, final CharSequence salt) {
        if (text == null) {
            throw new IllegalArgumentException("Text has null reference");
        }

        try {
            return Encryptors
                    .delux(secret, getSalt(salt))
                    .encrypt(text.toString());

        } catch (final Exception e) {
            log.error("Failed to encrypt text: ", e);
            return text;
        }
    }

    CharSequence decrypt(final CharSequence text, final CharSequence secret, final CharSequence salt) {
        if (text == null) {
            throw new IllegalArgumentException("Text has null reference");
        }

        try {

            return Encryptors
                    .delux(secret, getSalt(salt))
                    .decrypt(text.toString());

        } catch (final Exception e) {
            log.error("Failed to decrypt text: ", e);
            return text;
        }
    }

    private final static char[] possibleCharacters = (new String(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^&*()-_=+[{]}?"))
                    .toCharArray();

    private CharSequence getSalt(final CharSequence saltPlain) throws UnsupportedEncodingException {
        final CharSequence _salt = (saltPlain == null || saltPlain.length() <= 0)
                ? this.environment.getProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY, DEFAULT_SALT.toString())
                : saltPlain;
        return new String(Hex.encode(_salt.toString().getBytes("UTF-8")));
    }

    private CharSequence generateClientId() {
        return RandomStringUtils.random(
                16, 0, possibleCharacters.length - 1, false, false,
                possibleCharacters, new SecureRandom());
    }

    private CharSequence generateClientSecret() throws UnsupportedEncodingException {
        return RandomStringUtils.random(
                64, 0, possibleCharacters.length - 1, false, false,
                possibleCharacters, new SecureRandom());
    }

}
