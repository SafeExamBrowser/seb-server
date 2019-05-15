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

import ch.ethz.seb.sebserver.gbl.util.Utils;

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

    void streamEncrypted(
            final OutputStream output,
            final InputStream input,
            SebConfigEncryptionContext context);

    void streamDecrypted(
            final OutputStream output,
            final InputStream input,
            Supplier<CharSequence> passwordSupplier,
            Function<CharSequence, Certificate> certificateStore);

}
