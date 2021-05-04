/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.lang3.Validate;
import org.cryptonode.jncryptor.CryptorException;

import ch.ethz.seb.sebserver.gbl.Constants;

class AES256JNCryptorOutputStreamEmptyPwdSupport extends OutputStream {

    static final int SALT_LENGTH = 8;
    static final int AES_BLOCK_SIZE = 16;
    static final int VERSION = 3;
    static final int FLAG_PASSWORD = 0x01;

    private CipherOutputStream cipherStream;
    private MacOutputStream macOutputStream;
    private boolean writtenHeader;
    private final boolean passwordBased;
    private final byte[] encryptionSalt;
    private byte[] iv;
    private final byte[] hmacSalt;

    /** Creates an output stream for password-encrypted data, using a specific
     * number of PBKDF iterations.
     *
     * @param out
     *            the {@code OutputStream} to write the JNCryptor data to
     * @param password
     *            the password
     * @param iterations
     *            the number of PBKDF iterations to perform */
    public AES256JNCryptorOutputStreamEmptyPwdSupport(final OutputStream out, final char[] password,
            final int iterations) throws CryptorException {

        Validate.notNull(out, "Output stream cannot be null.");
        Validate.notNull(password, "Password cannot be null.");
        Validate.isTrue(iterations > 0, "Iterations must be greater than zero.");

        final AES256JNCryptorEmptyPwdSupport cryptor = new AES256JNCryptorEmptyPwdSupport(iterations);

        this.encryptionSalt = AES256JNCryptorEmptyPwdSupport.getSecureRandomData(SALT_LENGTH);
        final SecretKey encryptionKey = cryptor.keyForPassword(password, this.encryptionSalt);

        this.hmacSalt = AES256JNCryptorEmptyPwdSupport.getSecureRandomData(SALT_LENGTH);
        final SecretKey hmacKey = cryptor.keyForPassword(password, this.hmacSalt);

        this.iv = AES256JNCryptorEmptyPwdSupport.getSecureRandomData(AES_BLOCK_SIZE);

        this.passwordBased = true;
        createStreams(encryptionKey, hmacKey, this.iv, out);
    }

    /** Creates the cipher and MAC streams required,
     *
     * @param encryptionKey
     *            the encryption key
     * @param hmacKey
     *            the HMAC key
     * @param iv
     *            the IV
     * @param out
     *            the output stream we are wrapping
     * @throws CryptorException */
    private void createStreams(final SecretKey encryptionKey, final SecretKey hmacKey,
            final byte[] iv, final OutputStream out) throws CryptorException {

        this.iv = iv;

        try {
            final Cipher cipher = Cipher.getInstance(Constants.AES_CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new IvParameterSpec(iv));

            try {
                final Mac mac = Mac.getInstance(Constants.HMAC_ALGORITHM);
                mac.init(hmacKey);

                this.macOutputStream = new MacOutputStream(out, mac);
                this.cipherStream = new CipherOutputStream(this.macOutputStream, cipher);

            } catch (final GeneralSecurityException e) {
                throw new CryptorException("Failed to initialize HMac", e);
            }

        } catch (final GeneralSecurityException e) {
            throw new CryptorException("Failed to initialize AES cipher", e);
        }
    }

    /** Writes the header data to the output stream.
     *
     * @throws IOException */
    private void writeHeader() throws IOException {
        /* Write out the header */
        if (this.passwordBased) {
            this.macOutputStream.write(VERSION);
            this.macOutputStream.write(FLAG_PASSWORD);
            this.macOutputStream.write(this.encryptionSalt);
            this.macOutputStream.write(this.hmacSalt);
            this.macOutputStream.write(this.iv);
        } else {
            this.macOutputStream.write(VERSION);
            this.macOutputStream.write(0);
            this.macOutputStream.write(this.iv);
        }
    }

    /** Writes one byte to the encrypted output stream.
     *
     * @param b
     *            the byte to write
     * @throws IOException
     *             if an I/O error occurs */
    @Override
    public void write(final int b) throws IOException {
        if (!this.writtenHeader) {
            writeHeader();
            this.writtenHeader = true;
        }
        this.cipherStream.write(b);
    }

    /** Writes bytes to the encrypted output stream.
     *
     * @param b
     *            a buffer of bytes to write
     * @param off
     *            the offset into the buffer
     * @param len
     *            the number of bytes to write (starting from the offset)
     * @throws IOException
     *             if an I/O error occurs */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (!this.writtenHeader) {
            writeHeader();
            this.writtenHeader = true;
        }
        this.cipherStream.write(b, off, len);
    }

    /** Closes the stream. This causes the HMAC calculation to be concluded and
     * written to the output.
     *
     * @throws IOException
     *             if an I/O error occurs */
    @Override
    public void close() throws IOException {
        this.cipherStream.close();
    }

    /** An output stream to update a Mac object with all bytes passed through, then
     * write the Mac data to the stream upon close to complete the RNCryptor file
     * format. */
    private static class MacOutputStream extends FilterOutputStream {
        private final Mac mac;

        MacOutputStream(final OutputStream out, final Mac mac) {
            super(out);
            this.mac = mac;
        }

        @Override
        public void write(final int b) throws IOException {
            this.mac.update((byte) b);
            this.out.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            this.mac.update(b, off, len);
            this.out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            final byte[] macData = this.mac.doFinal();
            this.out.write(macData);
            this.out.flush();
            this.out.close();
        }
    }

}
