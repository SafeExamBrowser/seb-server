/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi;
import org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi.BCPKCS12KeyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

/** Cryptor dealing with internal encryption and decryption. */
@Lazy
@Service
public class Cryptor {

    private static final Logger log = LoggerFactory.getLogger(Cryptor.class);

    private final CharSequence internalPWD;

    public Cryptor(final Environment environment) {
        this.internalPWD = environment.getProperty("sebserver.webservice.internalSecret");
    }

    /** Use this to encrypt a text with the internal password
     *
     * @param text The text to encrypt with the internal password
     * @return the encrypted text cipher */
    public Result<CharSequence> encrypt(final CharSequence text) {
        return encrypt(text, this.internalPWD);
    }

    /** Use this to decrypt a cipher text with the internal password
     *
     * @param text The cipher text to decrypt with the internal password
     * @return the plain text */
    public Result<CharSequence> decrypt(final CharSequence text) {
        return decrypt(text, this.internalPWD);
    }

    public Result<PKCS12KeyStoreSpi> createNewEmptyKeyStore() {
        return loadKeyStore(null);
    }

    public Result<PKCS12KeyStoreSpi> loadKeyStore(final InputStream in) {
        return Result.tryCatch(() -> {
            final BCPKCS12KeyStore bcpkcs12KeyStore = new BCPKCS12KeyStore();
            bcpkcs12KeyStore.engineLoad(in, Utils.toCharArray(this.internalPWD));
            return bcpkcs12KeyStore;
        });
    }

    public Result<Void> storeKeyStore(final PKCS12KeyStoreSpi keyStore, final OutputStream out) {
        try {
            keyStore.engineStore(out, Utils.toCharArray(this.internalPWD));
            return Result.EMPTY;
        } catch (final IOException e) {
            log.error("Failed to store KeyStore: ", e);
            return Result.ofError(e);
        }
    }

    public Result<PKCS12KeyStoreSpi> addPrivateKey(
            final PKCS12KeyStoreSpi keyStore,
            final PrivateKey privateKey,
            final String alias,
            final Certificate certificate) {

        return Result.tryCatch(() -> {

            keyStore.engineSetKeyEntry(
                    alias,
                    privateKey,
                    Utils.toCharArray(this.internalPWD),
                    new Certificate[] { certificate });

            return keyStore;
        });
    }

    public Result<PrivateKey> getPrivateKey(final PKCS12KeyStoreSpi store, final String alias) {
        return Result.tryCatch(() -> {
            return (PrivateKey) store.engineGetKey(alias, Utils.toCharArray(this.internalPWD));
        });
    }

    static Result<CharSequence> encrypt(final CharSequence text, final CharSequence secret) {
        return Result.tryCatch(() -> {
            if (text == null) {
                throw new IllegalArgumentException("Text has null reference");
            }

            if (secret == null) {
                log.warn("No internal secret supplied: skip encryption");
                return text;
            }

            final CharSequence salt = KeyGenerators.string().generateKey();
            final CharSequence cipher = Encryptors
                    .delux(secret, salt)
                    .encrypt(text.toString());

            return new StringBuilder(cipher)
                    .append(salt);

        });
    }

    static Result<CharSequence> decrypt(final CharSequence cipher, final CharSequence secret) {
        return Result.tryCatch(() -> {
            if (cipher == null) {
                throw new IllegalArgumentException("Cipher has null reference");
            }

            if (secret == null) {
                log.warn("No internal secret supplied: skip decryption");
                return cipher;
            }

            final int length = cipher.length();
            final int cipherTextLength = length - 16;
            final CharSequence salt = cipher.subSequence(cipherTextLength, length);
            final CharSequence cipherText = cipher.subSequence(0, cipherTextLength);

            return Encryptors
                    .delux(secret, salt)
                    .decrypt(cipherText.toString());

        });
    }

}
