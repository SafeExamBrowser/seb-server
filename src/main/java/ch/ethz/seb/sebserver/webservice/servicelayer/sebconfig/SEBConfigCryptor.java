/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.springframework.scheduling.annotation.Async;

import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionService.Strategy;

/** Interface for a SEB Configuration encryption and decryption strategy.
 *
 * To support a new SEB Configuration encryption and decryption strategy use this interface
 * to implement a concrete strategy for encryption and decryption of SEB configurations */
public interface SEBConfigCryptor {

    /** The type of strategies a concrete implementation is supporting
     *
     * @return Set of strategies a concrete implementation is supporting */
    Set<Strategy> strategies();

    /** Encrypt an incoming plain data stream to an outgoing cipher data stream
     * This uses Springs @Async annotation to run in a separated thread
     *
     * @param output the output stream to write encrypted data to
     * @param input the input stream to read plain data from
     * @param context the SEBConfigEncryptionContext to access strategy specific data needed for encryption */
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void encrypt(
            final OutputStream output,
            final InputStream input,
            final SEBConfigEncryptionContext context);

    /** Decrypt an incoming cipher data stream to an outgoing plain text data stream
     * This uses Springs @Async annotation to run in a separated thread
     *
     * @param output the output stream to write the plain text data to
     * @param input the input stream to read the cipher text from
     * @param context the SEBConfigEncryptionContext to access strategy specific data needed for encryption */
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void decrypt(
            final OutputStream output,
            final InputStream input,
            final SEBConfigEncryptionContext context);

}
