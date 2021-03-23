/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class Cryptor {

    private static final Logger log = LoggerFactory.getLogger(Cryptor.class);

    public static final String SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY = "sebserver.webservice.internalSecret";

    private final Environment environment;

    public Cryptor(final Environment environment) {
        this.environment = environment;
    }

    public CharSequence encrypt(final CharSequence text) {

        final CharSequence secret = this.environment
                .getProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return encrypt(text, secret);
    }

    public CharSequence decrypt(final CharSequence text) {

        final CharSequence secret = this.environment
                .getProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY);

        return decrypt(text, secret);
    }

    public static CharSequence encrypt(final CharSequence text, final CharSequence secret) {
        if (text == null) {
            throw new IllegalArgumentException("Text has null reference");
        }

        if (secret == null) {
            log.warn("No internal secret supplied: skip encryption");
            return text;
        }

        try {

            final CharSequence salt = KeyGenerators.string().generateKey();
            final CharSequence cipher = Encryptors
                    .delux(secret, salt)
                    .encrypt(text.toString());

            return new StringBuilder(cipher)
                    .append(salt);

        } catch (final Exception e) {
            log.error("Failed to encrypt text: {}", e.getMessage());
            throw e;
        }
    }

    public static CharSequence decrypt(final CharSequence cipher, final CharSequence secret) {
        if (cipher == null) {
            throw new IllegalArgumentException("Cipher has null reference");
        }

        if (secret == null) {
            log.warn("No internal secret supplied: skip decryption");
            return cipher;
        }

        try {

            final int length = cipher.length();
            final int cipherTextLength = length - 16;
            final CharSequence salt = cipher.subSequence(cipherTextLength, length);
            final CharSequence cipherText = cipher.subSequence(0, cipherTextLength);

            return Encryptors
                    .delux(secret, salt)
                    .decrypt(cipherText.toString());

        } catch (final Exception e) {
            log.error("Failed to decrypt text: {}", e.getMessage());
            throw e;
        }
    }
}
