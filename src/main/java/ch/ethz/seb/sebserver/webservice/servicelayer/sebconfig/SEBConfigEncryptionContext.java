/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.security.cert.Certificate;

import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionService.Strategy;

/** Encryption context used to supply additional data for encryption or decryption
 * within a concrete strategy. */
public interface SEBConfigEncryptionContext {

    /** Get the current encryption/decryption strategy
     *
     * @return the current encryption/decryption strategy */
    Strategy getStrategy();

    /** Get a password as CharSequence if supported.
     *
     * @return a password as CharSequence
     * @throws UnsupportedOperationException if not supported */
    CharSequence getPassword();

    /** Get a defined Certificate if supported.
     *
     * @param key The key of the Certificate to get
     * @return a defined Certificate
     * @throws UnsupportedOperationException if not supported */
    Certificate getCertificate(CharSequence key);

}
