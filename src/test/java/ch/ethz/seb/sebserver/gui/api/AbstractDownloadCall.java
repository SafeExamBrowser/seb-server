/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.api;

import ch.ethz.seb.sebserver.gbl.util.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.util.LinkedMultiValueMap;

public class AbstractDownloadCall extends RestCall<Boolean> {

    protected AbstractDownloadCall(
            final MediaType contentType,
            final String path) {

        super(new TypeKey<>(CallType.UNDEFINED, null, new TypeReference<Boolean>() {
        }), HttpMethod.GET, contentType, path);
    }

    @Override
    protected Result<Boolean> exchange(final RestCallBuilder builder) {
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return Result.tryCatch(() -> builder
                .getRestTemplate()
                .execute(
                        builder.buildURI(),
                        this.httpMethod,
                        builder.getResponseExtractor(),
                        headers,
                        builder.getURIVariables(),
                        Boolean.class));
    }

}
