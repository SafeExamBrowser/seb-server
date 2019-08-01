/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import java.io.IOException;

import org.apache.http.HttpHeaders;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import ch.ethz.seb.sebserver.gbl.util.Result;

public abstract class AbstractExportCall extends RestCall<byte[]> {

    protected AbstractExportCall(
            final TypeKey<byte[]> typeKey,
            final HttpMethod httpMethod,
            final MediaType contentType,
            final String path) {

        super(typeKey, httpMethod, contentType, path);
    }

    // We need a WebClient here to separate the request from the usual RestTemplate
    // and allow also to get async responses
    // The OAut2 bearer is get from the current OAuth2RestTemplate
    // TODO create better API for this on RestCallBuilder site
    @Override
    protected Result<byte[]> exchange(final RestCallBuilder builder) {
        try {

            final OAuth2RestTemplate restTemplate = (OAuth2RestTemplate) builder.getRestTemplate();
            final OAuth2AccessToken accessToken = restTemplate.getAccessToken();
            final String value = accessToken.getValue();

            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            WebClient.create()
                    .method(this.httpMethod)
                    .uri(
                            builder.buildURI(),
                            builder.getURIVariables())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + value)
                    .headers(h -> h.addAll(builder.buildRequestEntity().getHeaders()))
                    .body(BodyInserters.fromObject("grant_type=client_credentials&scope=read,write"))
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .map(source -> {

                        try {
                            IOUtils.copyLarge(source.asInputStream(), byteArrayOutputStream);
                        } catch (final IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        return source;
                    })
                    .blockLast();

            final byte[] byteArray = byteArrayOutputStream.toByteArray();

            return Result.of(byteArray);

//            final byte[] value = builder
//                    .getRestTemplate()
//                    .execute(
//                            builder.buildURI(),
//                            this.httpMethod,
//                            request -> {
//                            },
//                            response -> {
//                                final InputStream input = IOUtils.toBufferedInputStream(response.getBody());
//                                final ByteArrayOutputStream output = new ByteArrayOutputStream();
//                                IOUtils.copyLarge(input, output);
//                                return output.toByteArray();
//                            },
//                            builder.getURIVariables());
//
//            System.out.println("************************ getResponse " + Utils.toString(value));
//
//            return Result.of(value);
//
//            final ResponseEntity<byte[]> responseEntity = builder
//                    .getRestTemplate()
//                    .exchange(
//                            builder.buildURI(),
//                            this.httpMethod,
//                            builder.buildRequestEntity(),
//                            byte[].class,
//                            builder.getURIVariables());

//            if (responseEntity.getStatusCode() == HttpStatus.OK) {
//                final byte[] body = responseEntity.getBody();
//                System.out.println("************************ getResponse " + Utils.toString(body));
//                return Result.of(body);
//            }
//
//            return Result.ofRuntimeError(
//                    "Error while trying to export from webservice. Response: " + responseEntity);
        } catch (final Throwable t) {
            return Result.ofError(t);
        }
    }

}
