/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.crypto.util.EncodingUtils.subArray;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;

public class CryptorTest {

    @Test
    public void testEncryptDecrypt() {
        final String clientName = "simpleClientName";
        String encrypted =
                Cryptor.encrypt(clientName, "secret1").getOrThrow().toString();
        String decrypted = Cryptor.decrypt(encrypted, "secret1").getOrThrow().toString();

        assertEquals(clientName, decrypted);

        final String clientSecret = "fbjreij39ru29305ruà££àèLöäöäü65%(/%(ç87";

        encrypted =
                Cryptor.encrypt(clientSecret, "secret1").getOrThrow().toString();
        decrypted = Cryptor.decrypt(encrypted, "secret1").getOrThrow().toString();

        assertEquals(clientSecret, decrypted);
    }

    @Test
    public void testEncryptDecryptService() {
        final Environment envMock = mock(Environment.class);
        when(envMock.getRequiredProperty("sebserver.webservice.internalSecret"))
                .thenReturn("secret1");

        final Cryptor cryptor = new Cryptor(envMock);
        final String clientName = "simpleClientName";

        String encrypted =
                cryptor.encrypt(clientName).getOrThrow().toString();
        String decrypted = cryptor.decrypt(encrypted).getOrThrow().toString();

        assertEquals(clientName, decrypted);

        final String clientSecret = "fbjreij39ru29305ruà££àèLöäöäü65%(/%(ç87";

        encrypted =
                cryptor.encrypt(clientSecret).getOrThrow().toString();
        decrypted = cryptor.decrypt(encrypted).getOrThrow().toString();

        assertEquals(clientSecret, decrypted);
    }

    @Test
    public void askEncryptDecryptStd() throws Exception {

        final String ask = "AppSignatureKey_+23";
        final String askHex = new String(Hex.encode(Utf8.encode(ask)));

        final String password = "somePassword";
        final String salt = "someSalt";
        final String saltHex = new String(Hex.encode(Utf8.encode(salt)));

        final String encrypted = Encryptors.delux(password, saltHex).encrypt(ask);

        final String decrypt = Encryptors.delux(password, saltHex).decrypt(encrypted);

        assertEquals("AppSignatureKey_+23", decrypt);
    }

    @Test
    public void askEncrypt() throws Exception {

        final String ask = "AppSignatureKey_+23";
        final String password = "somePassword";
        final String salt = "someSalt";

        final byte[] ASKBin = Utf8.encode(ask);
        final String passwordHex = toHex(password);
        final String saltHex = toHex(salt);

        assertEquals(password, fromHex(passwordHex));
        assertEquals(salt, fromHex(saltHex));

        final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
        final Cipher cipher = newCipher(AES_GCM_ALGORITHM);
        final BytesKeyGenerator ivGen = KeyGenerators.secureRandom(16);
        final PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), Hex.decode(saltHex), 1024, 256);
        final SecretKey secretKey = new SecretKeySpec(newSecretKey("PBKDF2WithHmacSHA1", keySpec).getEncoded(), "AES");

        // encrypt
        final byte[] iv = ivGen.generateKey();
        final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        final byte[] encrypted = doFinal(cipher, ASKBin);
        final byte[] encryptedFinal = concatenate(iv, encrypted);
        final String encryptedHex = new String(Hex.encode(encryptedFinal));

        //assertEquals("", encryptedHex);

        final byte[] encryptedBin = Hex.decode(encryptedHex);

        // decrypt
        final byte[] ivFromCipher = subArray(encryptedBin, 0, ivGen.getKeyLength());
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        final byte[] decrypted = doFinal(cipher, encrypted(encryptedBin, ivFromCipher.length));

        final String decryptedString = new String(decrypted, "UTF-8");
        assertEquals("AppSignatureKey_+23", decryptedString);

    }

    @Test
    public void askDecrypt() throws Exception {

        final String ASKHex =
                "ad36faae29807600a242466999f76f53fa62e5c285d109f1be049b71f92807343dfdb27bf56cb2ad974bad95cf50a9b56bc920";
        final String password = "somePassword";
        final String salt = "someSalt";

        final byte[] ASKBin = Hex.decode(ASKHex);
        final String passwordHex = toHex(password);
        final String saltHex = toHex(salt);

        final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
        final Cipher cipher = newCipher(AES_GCM_ALGORITHM);
        final BytesKeyGenerator ivGen = KeyGenerators.secureRandom(16);
        final PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), Hex.decode(saltHex), 1024, 256);
        final SecretKey secretKey = new SecretKeySpec(newSecretKey("PBKDF2WithHmacSHA1", keySpec).getEncoded(), "AES");

        // decrypt
        final byte[] iv = subArray(ASKBin, 0, ivGen.getKeyLength());
        final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        final byte[] decrypted = doFinal(cipher, encrypted(ASKBin, iv.length));

        final String decryptedString = new String(decrypted, "UTF-8");

        assertEquals("AppSignatureKey_+23", decryptedString);
    }

    @Test
    public void testCryptor_decryptASK() throws Exception {
        final String ASKHex =
                "ad36faae29807600a242466999f76f53fa62e5c285d109f1be049b71f92807343dfdb27bf56cb2ad974bad95cf50a9b56bc920";
        final String password = "somePassword";
        final String salt = "someSalt";
        final String saltHex = toHex(salt);

        final Result<CharSequence> decryptASK = Cryptor.decryptASK(ASKHex, password, saltHex);
        assertEquals("AppSignatureKey_+23", decryptASK.get());
    }

    @Test
    public void testCryptor_decryptASK_Dany() throws Exception {
        final String ASKHex =
                "011101ADFE1263B4768397D209159023b25286d33f423ecb8be0964dad8986ebf81a93bd0dcb45236d320363";
        final String password = "password";
        final String saltHex = "abcdef1234";

        final byte[] saltByteArray = Hex.decode(saltHex);
        final String saltUTF8 = new String(saltByteArray, "UTF-8");

        final Result<CharSequence> decryptASK = Cryptor.decryptASK(ASKHex, password, saltHex);
        if (decryptASK.hasError()) {
            decryptASK.getError().printStackTrace();
        }
        assertEquals("Hello World!", decryptASK.get());
    }

    @Test
    public void testCryptor_decryptASK_Origin() throws Exception {
        final String ASKHex =
                "011101ADFE1263B4768397D209159023b25286d33f423ecb8be0964dad8986ebf81a93bd0dcb45236d320363";
        final String password = "password";
        final String saltHex = "abcdef1234";

        final String decryptASK = Encryptors.delux(password, saltHex)
                .decrypt(ASKHex);
        //final Result<CharSequence> decryptASK = Cryptor.decrypt(ASKHex + saltHex, password);

        final byte[] saltByteArray = Hex.decode(saltHex);
        final String saltUTF8 = new String(saltByteArray, "UTF-8");

        assertEquals("Hello World!", decryptASK);
    }

    private byte[] encrypted(final byte[] encryptedBytes, final int ivLength) {
        return subArray(encryptedBytes, ivLength, encryptedBytes.length);
    }

    private String toHex(final String in) {
        return String.valueOf(Hex.encode(in.getBytes()));
    }

    private String fromHex(final String in) throws Exception {
        return new String(Hex.decode(in), "UTF-8");
    }

    public static SecretKey newSecretKey(final String algorithm, final PBEKeySpec keySpec) {
        try {
            final SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            return factory.generateSecret(keySpec);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Not a valid encryption algorithm", e);
        } catch (final InvalidKeySpecException e) {
            throw new IllegalArgumentException("Not a valid secret key", e);
        }
    }

    public static Cipher newCipher(final String algorithm) {
        try {
            return Cipher.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Not a valid encryption algorithm", e);
        } catch (final NoSuchPaddingException e) {
            throw new IllegalStateException("Should not happen", e);
        }
    }

    public static byte[] doFinal(final Cipher cipher, final byte[] input) {
        try {
            return cipher.doFinal(input);
        } catch (final IllegalBlockSizeException e) {
            throw new IllegalStateException(
                    "Unable to invoke Cipher due to illegal block size", e);
        } catch (final BadPaddingException e) {
            throw new IllegalStateException("Unable to invoke Cipher due to bad padding",
                    e);
        }
    }

    public static byte[] concatenate(final byte[]... arrays) {
        int length = 0;
        for (final byte[] array : arrays) {
            length += array.length;
        }
        final byte[] newArray = new byte[length];
        int destPos = 0;
        for (final byte[] array : arrays) {
            System.arraycopy(array, 0, newArray, destPos, array.length);
            destPos += array.length;
        }
        return newArray;
    }

}
