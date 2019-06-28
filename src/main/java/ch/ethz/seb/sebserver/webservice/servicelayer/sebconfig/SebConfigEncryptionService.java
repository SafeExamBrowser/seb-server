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
import java.security.cert.Certificate;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.scheduling.annotation.Async;

import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.util.Utils;

/** Used for SEB Configuration encryption and decryption */
public interface SebConfigEncryptionService {

    /** Types of encryption strategies */
    enum Type {
        /** Plain text, no encryption at all */
        PLAIN,
        /** Password based encryption */
        PASSWORD,
        /** Certificate based encryption */
        CERTIFICATE
    }

    enum Strategy {
        /** Plain text, no encryption at all */
        PLAIN_TEXT(Type.PLAIN, "plnd"),
        /** Password encryption with 'pswd' header and */
        PASSWORD_PSWD(Type.PASSWORD, "pswd"),
        /** Password encryption with 'pwcc' header */
        PASSWORD_PWCC(Type.PASSWORD, "pwcc"),

// NOTE not supported yet but eventually needed for SEB config import.
//        PUBLIC_KEY_HASH(Type.CERTIFICATE, "pkhs"),
//        PUBLIC_KEY_HASH_SYMMETRIC_KEY(Type.CERTIFICATE, "phsk")

        ;

        public final Type type;
        public final byte[] header;

        private Strategy(final Type type, final String headerKey) {
            this.type = type;
            this.header = Utils.toByteArray(headerKey);
        }

    }

    /** This can be used to stream incoming plain text data to encrypted cipher data output stream.
     *
     * @param output the output data stream to write the cipher text to
     * @param input the input stream to read the plain text from
     * @param context the SebConfigEncryptionContext to access strategy specific data needed for encryption */
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void streamEncrypted(
            final OutputStream output,
            final InputStream input,
            SebConfigEncryptionContext context);

    /** This can be used to stream incoming cipher data to decrypted plain text data output stream.
     *
     * @param output the output data stream to write encrypted plain text to
     * @param input the input stream to read the cipher text from
     * @param context the SebConfigEncryptionContext to access strategy specific data needed for encryption */
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void streamDecrypted(
            final OutputStream output,
            final InputStream input,
            Supplier<CharSequence> passwordSupplier,
            Function<CharSequence, Certificate> certificateStore);

}
