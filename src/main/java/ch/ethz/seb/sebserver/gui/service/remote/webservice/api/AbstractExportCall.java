/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;

import ch.ethz.seb.sebserver.gbl.util.Result;

@Deprecated // This is not streaming correctly. Use AbstractDownloadCall instead
public abstract class AbstractExportCall extends RestCall<InputStream> {

    protected AbstractExportCall(
            final TypeKey<InputStream> typeKey,
            final HttpMethod httpMethod,
            final MediaType contentType,
            final String path) {

        super(typeKey, httpMethod, contentType, path);
    }

    @Override
    protected Result<InputStream> exchange(final RestCallBuilder builder) {

        return Result.tryCatch(() -> builder
                .getRestTemplate()
                .execute(
                        builder.buildURI(),
                        this.httpMethod,
                        (final ClientHttpRequest requestCallback) -> {
                        },
                        response -> IOUtils.toBufferedInputStream(response.getBody()),
                        builder.getURIVariables()));
    }

}
