/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.client;

import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.security.SecureRandom;

import org.apache.commons.lang3.RandomStringUtils;
import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.JNCryptor;
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
public class ClientCredentialServiceImpl implements ClientCredentialService {

    private static final Logger log = LoggerFactory.getLogger(ClientCredentialServiceImpl.class);

    static final String SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY = "sebserver.webservice.internalSecret";
    static final CharSequence DEFAULT_SALT =
            CharBuffer.wrap(
                    new char[] { 'b', '7', 'd', 'b', 'e', '9', '9', 'b', 'b', 'f', 'a', '3', 'e', '2', '1', 'e' });

    private final Environment environment;

    // TODO try to integrate with JNCryptor since this is also used by SEB
    private final JNCryptor cryptor = new AES256JNCryptor();

    protected ClientCredentialServiceImpl(final Environment environment) {
        this.environment = environment;
    }

    @Override
    public Result<ClientCredentials> createGeneratedClientCredentials(final CharSequence salt) {
        return Result.tryCatch(() -> {
            try {

                return encryptClientCredentials(
                        generateClientId(),
                        generateClientSecret(),
                        salt);

            } catch (final UnsupportedEncodingException e) {
                log.error("Error while trying to generate client credentials: ", e);
                throw new RuntimeException("cause: ", e);
            }
        });
    }

    @Override
    public ClientCredentials encryptClientCredentials(
            final CharSequence clientIdPlaintext,
            final CharSequence secretPlaintext,
            final CharSequence accessTokenPlaintext,
            final CharSequence salt) {

        final CharSequence secret = this.environment
                .getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return new ClientCredentials(
                (clientIdPlaintext != null)
                        ? encrypt(clientIdPlaintext, secret, salt).toString()
                        : null,
                (secretPlaintext != null)
                        ? encrypt(secretPlaintext, secret, salt).toString()
                        : null,
                (accessTokenPlaintext != null)
                        ? encrypt(accessTokenPlaintext, secret, salt).toString()
                        : null);
    }

    @Override
    public CharSequence getPlainClientId(final ClientCredentials credentials, final CharSequence salt) {
        if (credentials == null || !credentials.hasClientId()) {
            return null;
        }

        final CharSequence secret = this.environment
                .getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return this.decrypt(credentials.clientId, secret, salt);
    }

    @Override
    public CharSequence getPlainClientSecret(final ClientCredentials credentials, final CharSequence salt) {
        if (credentials == null || !credentials.hasSecret()) {
            return null;
        }

        final CharSequence secret = this.environment
                .getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);
        return this.decrypt(credentials.secret, secret, salt);
    }

    @Override
    public CharSequence getPlainAccessToken(final ClientCredentials credentials, final CharSequence salt) {
        if (credentials == null || !credentials.hasAccessToken()) {
            return null;
        }

        final CharSequence secret = this.environment
                .getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);
        return this.decrypt(credentials.accessToken, secret, salt);
    }

    @Override
    public CharSequence encrypt(final CharSequence text) {
        final CharSequence secret = this.environment
                .getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);
        return encrypt(text, secret, null);
    }

    @Override
    public CharSequence decrypt(final CharSequence text) {
        final CharSequence secret = this.environment
                .getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);
        return decrypt(text, secret, null);
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
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^*()-_=+[{]}?"))
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
