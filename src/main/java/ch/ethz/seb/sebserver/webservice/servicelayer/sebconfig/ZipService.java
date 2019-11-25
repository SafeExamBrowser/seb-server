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

/** A Zip service that can be used to compress or uncompress a given data stream. */
public interface ZipService {

    /** Use this to read uncompressed data from a given input-stream,
     * compress this data with gzip and write the compressed data to
     * a given output stream.
     *
     * @param out the OutputStream to write the compressed data to
     * @param in the InputStream to read the uncompressed data from */
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void write(OutputStream out, InputStream in);

    /** Use this to read gzip-compressed data from a given input-stream,
     * uncompress this data and write the uncompressed data to
     * a given output stream.
     *
     * @param out the OutputStream to write the uncompressed data to
     * @param in the InputStream to read the compressed data from */
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void read(OutputStream out, InputStream in);

}
