/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.nio.ByteBuffer;
import java.util.Set;

import org.cryptonode.jncryptor.JNCryptor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionContext;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService.Strategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigCryptor;

@Lazy
@Component
@WebServiceProfile
public class PasswordEncryptor implements SebConfigCryptor {

    private static final Set<Strategy> STRATEGIES = Utils.immutableSetOf(
            Strategy.PASSWORD_PSWD,
            Strategy.PASSWORD_PWCC);

    private final JNCryptor jnCryptor;

    protected PasswordEncryptor(final JNCryptor jnCryptor) {
        this.jnCryptor = jnCryptor;
    }

    @Override
    public Set<Strategy> strategies() {
        return STRATEGIES;
    }

    @Override
    public Result<ByteBuffer> encrypt(final CharSequence plainTextConfig, final SebConfigEncryptionContext context) {
        return Result.tryCatch(() -> {
            return ByteBuffer.wrap(this.jnCryptor.encryptData(
                    Utils.toByteArray(plainTextConfig),
                    Utils.toCharArray(context.getPassword())));
        });
    }

    @Override
    public Result<ByteBuffer> decrypt(final ByteBuffer cipher, final SebConfigEncryptionContext context) {
        return Result.tryCatch(() -> {
            return ByteBuffer.wrap(this.jnCryptor.decryptData(
                    Utils.toByteArray(cipher),
                    Utils.toCharArray(context.getPassword())));
        });
    }

}
