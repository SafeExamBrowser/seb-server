/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.util.Result;

/** Adds some static logging support for DAO's */
public final class DAOLoggingSupport {

    public static final Logger log = LoggerFactory.getLogger(DAOLoggingSupport.class);

    public static <T> Stream<T> logUnexpectedErrorAndSkip(final Result<T> result) {
        return Result.skipOnError(
                result.onError(error -> log.error("Unexpected error. Object processing is skipped: ", error)));
    }

}
