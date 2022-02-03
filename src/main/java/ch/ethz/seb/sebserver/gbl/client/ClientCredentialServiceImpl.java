/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.client;

import java.nio.CharBuffer;
import java.security.SecureRandom;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;

@Lazy
@Service
public class ClientCredentialServiceImpl implements ClientCredentialService {

    private final Cryptor cryptor;

    protected ClientCredentialServiceImpl(final Cryptor cryptor) {
        this.cryptor = cryptor;
    }

    @Override
    public Result<ClientCredentials> generatedClientCredentials() {
        return encryptClientCredentials(
                generateClientId(),
                generateClientSecret());
    }

    @Override
    public Result<ClientCredentials> encryptClientCredentials(
            final CharSequence clientIdPlaintext,
            final CharSequence secretPlaintext,
            final CharSequence accessTokenPlaintext) {

        return Result.tryCatch(() -> {
            return new ClientCredentials(
                    clientIdPlaintext,
                    (StringUtils.isNoneBlank(secretPlaintext))
                            ? this.cryptor.encrypt(secretPlaintext)
                                    .getOrThrow()
                                    .toString()
                            : null,
                    (StringUtils.isNoneBlank(accessTokenPlaintext))
                            ? this.cryptor.encrypt(accessTokenPlaintext)
                                    .getOrThrow()
                                    .toString()
                            : null);
        });
    }

    @Override
    public Result<CharSequence> getPlainClientSecret(final ClientCredentials credentials) {
        if (credentials == null || !credentials.hasSecret()) {
            return null;
        }

        return this.cryptor.decrypt(credentials.secret);
    }

    @Override
    public Result<CharSequence> getPlainAccessToken(final ClientCredentials credentials) {
        if (credentials == null || !credentials.hasAccessToken()) {
            return Result.ofRuntimeError("No token available");
        }

        return this.cryptor.decrypt(credentials.accessToken);
    }

    @Override
    public Result<CharSequence> encrypt(final CharSequence text) {
        return this.cryptor.encrypt(text);
    }

    @Override
    public Result<CharSequence> decrypt(final CharSequence text) {
        return this.cryptor.decrypt(text);
    }

    private final static char[] possibleCharacters =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^*()-_=+[{]}?"
                    .toCharArray();

    public static CharSequence generateClientId() {
        return RandomStringUtils.random(
                16, 0, possibleCharacters.length - 1, false, false,
                possibleCharacters, new SecureRandom());
    }

    public static CharSequence generateClientSecret() {
        return RandomStringUtils.random(
                64, 0, possibleCharacters.length - 1, false, false,
                possibleCharacters, new SecureRandom());
    }

    public static void clearChars(final CharSequence sequence) {
        if (sequence == null) {
            return;
        }

        if (sequence instanceof CharBuffer) {
            ((CharBuffer) sequence).clear();
            ((CharBuffer) sequence).put(new char[((CharBuffer) sequence).capacity()]);
            return;
        }

        throw new IllegalArgumentException(
                "Cannot clear chars on CharSequence of type: " + sequence.getClass().getName());
    }

}
