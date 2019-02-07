/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService.SortOrder;

public abstract class RestCall<T> {

    private static final Logger log = LoggerFactory.getLogger(RestCall.class);

    private RestService restService;
    private JSONMapper jsonMapper;
    protected final TypeReference<T> typeRef;
    protected final HttpMethod httpMethod;
    protected final MediaType contentType;
    protected final String path;

    protected RestCall(
            final TypeReference<T> typeRef,
            final HttpMethod httpMethod,
            final MediaType contentType,
            final String path) {

        this.typeRef = typeRef;
        this.httpMethod = httpMethod;
        this.contentType = contentType;
        this.path = path;

    }

    RestCall<T> init(final RestService restService, final JSONMapper jsonMapper) {
        this.restService = restService;
        this.jsonMapper = jsonMapper;
        return this;
    }

    protected Result<T> exchange(final RestCallBuilder builder) {

        log.debug("Call webservice API on {} for {}", this.path, builder);

        try {
            final ResponseEntity<String> responseEntity = RestCall.this.restService
                    .getWebserviceAPIRestTemplate()
                    .exchange(
                            builder.buildURI(),
                            this.httpMethod,
                            builder.buildRequestEntity(),
                            String.class,
                            builder.uriVariables);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {

                return Result.of(RestCall.this.jsonMapper.readValue(
                        responseEntity.getBody(),
                        RestCall.this.typeRef));

            } else {

                final RestCallError restCallError =
                        new RestCallError("Response Entity: " + responseEntity.toString());
                restCallError.errors.addAll(RestCall.this.jsonMapper.readValue(
                        responseEntity.getBody(),
                        new TypeReference<List<APIMessage>>() {
                        }));

                log.debug(
                        "Webservice answered with well defined error- or validation-failure-response: ",
                        restCallError);

                return Result.ofError(restCallError);
            }

        } catch (final Throwable t) {
            final RestCallError restCallError = new RestCallError("Unexpected error while rest call", t);
            try {
                final String responseBody = ((RestClientResponseException) t).getResponseBodyAsString();
                restCallError.errors.addAll(RestCall.this.jsonMapper.readValue(
                        responseBody,
                        new TypeReference<List<APIMessage>>() {
                        }));
            } catch (final Exception e) {
                log.error("Unexpected error-response while webservice API call for: {}", builder, e);
            }

            return Result.ofError(restCallError);
        }
    }

    public RestCallBuilder newBuilder() {
        return new RestCallBuilder();
    }

    public final class RestCallBuilder {

        private final HttpHeaders httpHeaders = new HttpHeaders();
        private String body = null;
        private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        private final Map<String, String> uriVariables = new HashMap<>();

        RestCallBuilder() {
            this.httpHeaders.set(
                    HttpHeaders.CONTENT_TYPE,
                    RestCall.this.contentType.toString());
        }

        public RestCallBuilder withHeaders(final HttpHeaders headers) {
            this.httpHeaders.addAll(headers);
            return this;
        }

        public RestCallBuilder withHeader(final String name, final String value) {
            this.httpHeaders.set(name, value);
            return this;
        }

        public RestCallBuilder withBody(final Object body) {
            if (body instanceof String) {
                this.body = String.valueOf(body);
            }

            try {
                this.body = RestCall.this.jsonMapper.writeValueAsString(body);
            } catch (final JsonProcessingException e) {
                log.error("Error while trying to parse body json object: " + body);
            }

            return this;
        }

        public RestCallBuilder withURIVariable(final String name, final String value) {
            this.uriVariables.put(name, value);
            return this;
        }

        public RestCallBuilder withQueryParam(final String name, final String value) {
            this.queryParams.put(name, Arrays.asList(value));
            return this;
        }

        public RestCallBuilder withPaging(final int pageNumber, final int pageSize) {
            this.queryParams.put(Page.ATTR_PAGE_NUMBER, Arrays.asList(String.valueOf(pageNumber)));
            this.queryParams.put(Page.ATTR_PAGE_SIZE, Arrays.asList(String.valueOf(pageSize)));
            return this;
        }

        public RestCallBuilder withSorting(final String column, final SortOrder order) {
            if (column != null) {
                this.queryParams.put(Page.ATTR_SORT, Arrays.asList(order.encode(column)));
            }
            return this;
        }

        public RestCallBuilder withFilterAttributes(final FilterAttributeSupplier filterAttributes) {
            if (filterAttributes != null) {
                this.queryParams.putAll(filterAttributes.getAttributes());
            }
            return this;
        }

        public RestCallBuilder withFormBinding(final FormBinding formBinding) {
            // TODO Auto-generated method stub
            return this;
        }

        public RestCallBuilder onlyActive(final boolean active) {
            this.queryParams.put(Entity.FILTER_ATTR_ACTIVE, Arrays.asList(String.valueOf(active)));
            return this;
        }

        public final Result<T> call() {
            return RestCall.this.exchange(this);
        }

        String buildURI() {
            return RestCall.this.restService.getWebserviceURIBuilder()
                    .path(RestCall.this.path)
                    .queryParams(this.queryParams)
                    .toUriString();
        }

        HttpEntity<?> buildRequestEntity() {
            if (this.body != null) {
                return new HttpEntity<>(this.body, this.httpHeaders);
            } else {
                return new HttpEntity<>(this.httpHeaders);
            }
        }

        @Override
        public String toString() {
            return "RestCallBuilder [httpHeaders=" + this.httpHeaders + ", body=" + this.body + ", queryParams="
                    + this.queryParams
                    + ", uriVariables=" + this.uriVariables + "]";
        }

    }

}
