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

import org.springframework.scheduling.annotation.Async;

import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;

public interface ZipService {

    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void write(OutputStream out, InputStream in);

    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void read(OutputStream out, InputStream in);

}
