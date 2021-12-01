/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.util.Result;

public class AbstractDownloadCall extends RestCall<Boolean> {

    protected AbstractDownloadCall(
            final MediaType contentType,
            final String path) {

        super(new RestCall.TypeKey<>(CallType.UNDEFINED, null, new TypeReference<Boolean>() {
        }), HttpMethod.GET, contentType, path);
    }

    @Override
    protected Result<Boolean> exchange(final RestCallBuilder builder) {

        return Result.tryCatch(() -> builder
                .getRestTemplate()
                .execute(
                        builder.buildURI(),
                        this.httpMethod,
                        (final ClientHttpRequest requestCallback) -> {
                        },
                        builder.getResponseExtractor(),
                        builder.getURIVariables()));
    }

}
