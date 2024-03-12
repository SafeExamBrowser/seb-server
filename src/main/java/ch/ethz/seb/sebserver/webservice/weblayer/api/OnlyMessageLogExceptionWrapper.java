/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.slf4j.Logger;
import org.springframework.boot.logging.LogLevel;

public class OnlyMessageLogExceptionWrapper extends RuntimeException {

    private static final long serialVersionUID = 4177915563660228494L;
    public final LogLevel logLevel;

    public OnlyMessageLogExceptionWrapper(final Exception cause) {
        super(cause);
        this.logLevel = LogLevel.WARN;
    }

    public OnlyMessageLogExceptionWrapper(final Exception cause, final LogLevel logLevel) {
        super(cause);
        this.logLevel = logLevel;
    }

    public final void log(final Logger logger) {
        final String message = this.getCause().getMessage();
        switch (this.logLevel) {
            case ERROR:
            case FATAL:
                logger.error(message);
                break;
            case INFO:
                logger.info(message);
                break;
            case TRACE:
                logger.trace(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            default:
                logger.debug(message);
                break;
        }
    }

}
