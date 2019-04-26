/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.nio.ByteBuffer;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService.Strategy;

/** Interface for a SEB Configuration encryption and decryption strategy.
 *
 * To support a new SEB Configuration encryption and decryption strategy use this interface
 * to implement a concrete strategy for encryption and decryption of SEB configurations */
public interface SebConfigCryptor {

    /** The type of strategies a concrete implementation is supporting
     *
     * @return Set of strategies a concrete implementation is supporting */
    Set<Strategy> strategies();

    /** Encrypt a given SEB configuration plain text representation within the given SebConfigEncryptionContext
     *
     * @param plainTextConfig SEB configuration plain text representation
     * @param context SebConfigEncryptionContext containing additional data if needed
     * @return Result of encrypted data within a ByteBuffer or reference to an Exception on error case */
    Result<ByteBuffer> encrypt(
            final CharSequence plainTextConfig,
            final SebConfigEncryptionContext context);

    /** Decrypt a given encrypted SEB configuration that has been encrypted by one of the supported strategies.
     *
     * @param cipher the encrypted SEB configuration cipher(text) within a ByteBuffer
     * @param context SebConfigEncryptionContext containing additional data if needed
     * @return Result of decrypted SEB configuration within a ByteBuffer or reference to an Exception on error case. */
    Result<ByteBuffer> decrypt(
            final ByteBuffer cipher,
            final SebConfigEncryptionContext context);

}
