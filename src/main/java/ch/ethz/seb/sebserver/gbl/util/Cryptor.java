/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.springframework.security.crypto.util.EncodingUtils.subArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi;
import org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi.BCPKCS12KeyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
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

    public CharSequence getInternalPWD() {
        return this.internalPWD;
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

    public static Result<CharSequence> encrypt(final CharSequence text, final CharSequence secret) {
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

    public Result<CharSequence> encryptCheckAlreadyEncrypted(final CharSequence text) {
        return Result.tryCatch(() -> {

            // try to decrypt to check if it is already encrypted
            final Result<CharSequence> decryption = this.decrypt(text);
            if (decryption.hasError()) {
                return encrypt(text).getOrThrow();
            } else {
                return text;
            }
        });
    }

    public static Result<CharSequence> decrypt(final CharSequence cipher, final CharSequence secret) {
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

    public static Result<CharSequence> decryptASK(
            final CharSequence cipherText,
            final CharSequence secret,
            final CharSequence salt) {

        return Result.tryCatch(() -> {

            final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
            final Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            final BytesKeyGenerator ivGen = KeyGenerators.secureRandom(16);
            final PBEKeySpec keySpec = new PBEKeySpec(Utils.toCharArray(secret), Hex.decode(salt), 1024, 256);
            final SecretKey secretKey = new SecretKeySpec(
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(keySpec).getEncoded(),
                    "AES");

            final byte[] plainCipherText = Hex.decode(cipherText);

            // decrypt
            final byte[] iv = subArray(plainCipherText, 0, ivGen.getKeyLength());
            final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            final byte[] decrypted = cipher.doFinal(subArray(plainCipherText, iv.length, plainCipherText.length));

            return new String(decrypted, "UTF-8");

        });
    }


}
