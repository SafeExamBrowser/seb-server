/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.ethz.seb.sebserver.gbl.util.Result;

public abstract class AbstractExportCall extends RestCall<byte[]> {

    protected AbstractExportCall(
            final TypeKey<byte[]> typeKey,
            final HttpMethod httpMethod,
            final MediaType contentType,
            final String path) {

        super(typeKey, httpMethod, contentType, path);
    }

    @Override
    protected Result<byte[]> exchange(final RestCallBuilder builder) {
        try {
            final ResponseEntity<byte[]> responseEntity = builder
                    .getRestTemplate()
                    .exchange(
                            builder.buildURI(),
                            this.httpMethod,
                            builder.buildRequestEntity(),
                            byte[].class,
                            builder.getURIVariables());

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                return Result.of(responseEntity.getBody());
            }

            return Result.ofRuntimeError(
                    "Error while trying to export from webservice. Response: " + responseEntity);
        } catch (final Throwable t) {
            return Result.ofError(t);
        }
    }

}
