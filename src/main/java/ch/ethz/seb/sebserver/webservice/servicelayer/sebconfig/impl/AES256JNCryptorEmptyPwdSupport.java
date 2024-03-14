/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.CryptorException;

import ch.ethz.seb.sebserver.gbl.Constants;

class AES256JNCryptorEmptyPwdSupport extends AES256JNCryptor {

    static final int AES_256_KEY_SIZE = 256 / 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    protected AES256JNCryptorEmptyPwdSupport() {
        super();
    }

    protected AES256JNCryptorEmptyPwdSupport(final int iterations) {
        super(iterations);
    }

    static byte[] getSecureRandomData(final int length) {
        final byte[] result = new byte[length];
        SECURE_RANDOM.nextBytes(result);
        return result;
    }

    @Override
    public SecretKey keyForPassword(final char[] password, final byte[] salt) throws CryptorException {
        try {
            final SecretKeyFactory factory = SecretKeyFactory
                    .getInstance(Constants.KEY_DERIVATION_ALGORITHM);
            final SecretKey tmp = factory.generateSecret(new PBEKeySpec(password, salt,
                    getPBKDFIterations(), AES_256_KEY_SIZE * 8));
            return new SecretKeySpec(tmp.getEncoded(), Constants.AES);
        } catch (final GeneralSecurityException e) {
            throw new CryptorException(String.format(
                    "Failed to generate key from password using %s.",
                    Constants.KEY_DERIVATION_ALGORITHM), e);
        }
    }

}
