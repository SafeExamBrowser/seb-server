/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@Lazy
@Service
@WebServiceProfile
public class InternalEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(InternalEncryptionService.class);

    static final String SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY = "sebserver.webservice.internalSecret";
    private static final String DEFAULT_SALT = "b7dbe99bbfa3e21e";

    private final Environment environment;

    protected InternalEncryptionService(final Environment environment) {
        this.environment = environment;
    }

    public String encrypt(final String text) {
        try {
            return Encryptors.text(
                    this.environment.getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY),
                    DEFAULT_SALT).encrypt(text);
        } catch (final Exception e) {
            log.error("Failed to encrypt text: ", e);
            return text;
        }
    }

    public String decrypt(final String text) {
        try {
            return Encryptors.text(
                    this.environment.getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY),
                    DEFAULT_SALT).decrypt(text);
        } catch (final Exception e) {
            log.error("Failed to decrypt text: ", e);
            return text;
        }
    }

    public String encrypt(final String text, final CharSequence salt) {
        try {
            return Encryptors.text(
                    this.environment.getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY),
                    salt).encrypt(text);
        } catch (final Exception e) {
            log.error("Failed to encrypt text: ", e);
            return text;
        }
    }

    public String decrypt(final String text, final CharSequence salt) {
        try {
            return Encryptors.text(
                    this.environment.getRequiredProperty(SEBSERVER_WEBSERVICE_INTERNAL_SECRET_KEY),
                    salt).decrypt(text);
        } catch (final Exception e) {
            log.error("Failed to decrypt text: ", e);
            return text;
        }
    }

    public String encrypt(final String text, final CharSequence secret, final CharSequence salt) {
        try {
            return Encryptors.text(secret, salt).encrypt(text);
        } catch (final Exception e) {
            log.error("Failed to encrypt text: ", e);
            return text;
        }
    }

    public String decrypt(final String text, final CharSequence secret, final CharSequence salt) {
        try {
            return Encryptors.text(secret, salt).decrypt(text);
        } catch (final Exception e) {
            log.error("Failed to decrypt text: ", e);
            return text;
        }
    }

}
