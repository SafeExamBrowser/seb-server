/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.api.TooManyRequests;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;

public abstract class RestCall<T> {

    private static final Logger log = LoggerFactory.getLogger(RestCall.class);

    public static final LocTextKey REQUEST_TIMEOUT_MESSAGE = new LocTextKey("sebserver.overall.message.requesttimeout");

    public enum CallType {
        UNDEFINED,
        GET_SINGLE,
        GET_PAGE,
        GET_NAMES,
        GET_DEPENDENCIES,
        GET_LIST,
        NEW,
        REGISTER,
        SAVE,
        DELETE,
        ACTIVATION_ACTIVATE,
        ACTIVATION_DEACTIVATE
    }

    protected RestService restService;
    protected JSONMapper jsonMapper;
    protected TypeKey<T> typeKey;
    protected final HttpMethod httpMethod;
    protected final MediaType contentType;
    protected final String path;

    protected RestCall(
            final TypeKey<T> typeKey,
            final HttpMethod httpMethod,
            final MediaType contentType,
            final String path) {

        this.typeKey = typeKey;
        this.httpMethod = httpMethod;
        this.contentType = contentType;
        this.path = path;

    }

    protected RestCall<T> init(
            final RestService restService,
            final JSONMapper jsonMapper) {

        this.restService = restService;
        this.jsonMapper = jsonMapper;
        return this;
    }

    public EntityType getEntityType() {
        if (this.typeKey != null) {
            return this.typeKey.entityType;
        }

        return null;
    }

    protected Result<T> exchange(final RestCallBuilder builder) {

        log.debug("Call webservice API on {} for {}", this.path, builder);

        try {
            final ResponseEntity<String> responseEntity = builder.restTemplate
                    .exchange(
                            builder.buildURI(),
                            this.httpMethod,
                            builder.buildRequestEntity(),
                            String.class,
                            builder.uriVariables);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {

                if (log.isTraceEnabled()) {
                    log.trace("response body --> {}" + responseEntity.getBody());
                }

                if (!responseEntity.hasBody()) {
                    return Result.ofEmpty();
                }

                return Result.of(RestCall.this.jsonMapper.readValue(
                        responseEntity.getBody(),
                        RestCall.this.typeKey.typeRef));

            } else if (responseEntity.getStatusCode() == HttpStatus.PARTIAL_CONTENT) {
                return handleRestCallPartialResponse(responseEntity);
            } else {
                return handleRestCallError(responseEntity);
            }
        } catch (final RestClientResponseException responseError) {

            if (responseError.getRawStatusCode() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                final String code = responseError.getResponseBodyAsString();
                if (StringUtils.isNotBlank(code)) {
                    return Result.ofError(new TooManyRequests(TooManyRequests.Code.valueOf(code)));
                } else {
                    return Result.ofError(new TooManyRequests());
                }
            }

            final RestCallError restCallError = new RestCallError("Unexpected error while rest call", responseError);
            try {

                final String responseBody = responseError.getResponseBodyAsString();
                restCallError.errors.addAll(RestCall.this.jsonMapper.readValue(
                        responseBody,
                        new TypeReference<List<APIMessage>>() {
                        }));

            } catch (final IOException e) {
                restCallError.errors.add(APIMessage.ErrorMessage.UNEXPECTED.of(
                        responseError,
                        "NO RESPONSE AVAILABLE" + " cause: " + e.getMessage(),
                        String.valueOf(builder)));
            }

            return Result.ofError(restCallError);
        } catch (final ResourceAccessException rae) {
            if (rae.getMessage().contains("Read timed out")) {
                return Result.ofError(new PageMessageException(REQUEST_TIMEOUT_MESSAGE));
            }
            return Result.ofError(rae);
        } catch (final Exception e) {
            final RestCallError restCallError = new RestCallError("Unexpected error while rest call", e);
            restCallError.errors.add(APIMessage.ErrorMessage.UNEXPECTED.of(
                    e,
                    "NO RESPONSE AVAILABLE",
                    String.valueOf(builder)));
            return Result.ofError(e);
        }
    }

    private Result<T> handleRestCallPartialResponse(final ResponseEntity<String> responseEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    public RestCallBuilder newBuilder() {
        return new RestCallBuilder(
                this.restService.getWebserviceAPIRestTemplate(),
                this.restService.getWebserviceURIBuilder());
    }

    public RestCall<T>.RestCallBuilder newBuilder(final RestCall<?>.RestCallBuilder builder) {
        return new RestCallBuilder(builder);
    }

    private Result<T> handleRestCallError(final ResponseEntity<String> responseEntity)
            throws IOException {

        final RestCallError restCallError =
                new RestCallError("Response Entity: " + responseEntity.toString());

        try {
            restCallError.errors.addAll(RestCall.this.jsonMapper.readValue(
                    responseEntity.getBody(),
                    new TypeReference<List<APIMessage>>() {
                    }));
        } catch (final JsonParseException jpe) {
            if (responseEntity.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                restCallError.errors.add(APIMessage.ErrorMessage.UNAUTHORIZED.of(responseEntity.getBody()));
            } else {
                restCallError.errors.add(APIMessage.ErrorMessage.GENERIC.of(responseEntity.getBody()));
            }
        }

        log.debug(
                "Webservice answered with well defined error- or validation-failure-response: {}",
                restCallError.toString());

        return Result.ofError(restCallError);
    }

    public class RestCallBuilder {

        private RestTemplate restTemplate;
        private UriComponentsBuilder uriComponentsBuilder;
        private final HttpHeaders httpHeaders;
        private String body = null;
        private InputStream streamingBody = null;
        private ResponseExtractor<Boolean> responseExtractor = null;

        private final MultiValueMap<String, String> queryParams;
        private final Map<String, String> uriVariables;

        protected RestCallBuilder(final RestTemplate restTemplate, final UriComponentsBuilder uriComponentsBuilder) {
            this.restTemplate = restTemplate;
            this.uriComponentsBuilder = uriComponentsBuilder;
            this.httpHeaders = new HttpHeaders();
            this.queryParams = new LinkedMultiValueMap<>();
            this.uriVariables = new HashMap<>();
            this.httpHeaders.set(
                    HttpHeaders.CONTENT_TYPE,
                    RestCall.this.contentType.toString());
        }

        public RestCallBuilder(final RestCall<?>.RestCallBuilder builder) {
            this.restTemplate = builder.restTemplate;
            this.uriComponentsBuilder = builder.uriComponentsBuilder;
            this.httpHeaders = builder.httpHeaders;
            this.body = builder.body;
            this.streamingBody = builder.streamingBody;
            this.queryParams = new LinkedMultiValueMap<>(builder.queryParams);
            this.uriVariables = new HashMap<>(builder.uriVariables);
        }

        public RestTemplate getRestTemplate() {
            return this.restTemplate;
        }

        public RestCallBuilder withResponseExtractor(final ResponseExtractor<Boolean> responseExtractor) {
            this.responseExtractor = responseExtractor;
            return this;
        }

        public ResponseExtractor<Boolean> getResponseExtractor() {
            return this.responseExtractor;
        }

        public RestCallBuilder withRestTemplate(final RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
            return this;
        }

        public RestCallBuilder withUriComponentsBuilder(final UriComponentsBuilder uriComponentsBuilder) {
            this.uriComponentsBuilder = uriComponentsBuilder;
            return this;
        }

        public RestCallBuilder withHeaders(final HttpHeaders headers) {
            this.httpHeaders.addAll(headers);
            return this;
        }

        public RestCallBuilder withHeader(final String name, final String value) {
            this.httpHeaders.set(name, value);
            return this;
        }

        public RestCallBuilder withHeaders(final MultiValueMap<String, String> params) {
            this.httpHeaders.addAll(params);
            return this;
        }

        public RestCallBuilder apply(final Function<RestCallBuilder, RestCallBuilder> f) {
            return f.apply(this);
        }

        public RestCallBuilder withBody(final Object body) {
            if (body == null) {
                this.body = null;
                return this;
            }

            if (body instanceof String) {
                this.body = String.valueOf(body);
                return this;
            }

            if (body instanceof InputStream) {
                this.streamingBody = (InputStream) body;
                return this;
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
            this.queryParams.add(name, value);
            return this;
        }

        public RestCallBuilder withQueryParams(final MultiValueMap<String, String> params) {
            if (params != null) {
                this.queryParams.putAll(params);
            }
            return this;
        }

        public RestCallBuilder withFormParam(final String name, final String value) {
            final String encodedVal = Utils.encodeFormURL_UTF_8(value);
            if (StringUtils.isBlank(this.body)) {
                this.body = name + Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR + encodedVal;
            } else {
                this.body = this.body + Constants.FORM_URL_ENCODED_SEPARATOR + name +
                        Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR + encodedVal;
            }

            return this;
        }

        public RestCallBuilder withFormParams(final MultiValueMap<String, String> params) {
            if (params != null) {
                params.entrySet()
                        .stream()
                        .forEach(param -> {
                            final String name = param.getKey();
                            param.getValue().stream().forEach(p -> withFormParam(name, p));
                        });
            }
            return this;
        }

        public RestCallBuilder withPaging(final int pageNumber, final int pageSize) {
            this.queryParams.put(Page.ATTR_PAGE_NUMBER, Arrays.asList(String.valueOf(pageNumber)));
            this.queryParams.put(Page.ATTR_PAGE_SIZE, Arrays.asList(String.valueOf(pageSize)));
            return this;
        }

        public RestCallBuilder withSorting(final String column, final PageSortOrder order) {
            if (column != null) {
                this.queryParams.put(Page.ATTR_SORT, Arrays.asList(order.encode(column)));
            }
            return this;
        }

        public RestCallBuilder withFormBinding(final FormBinding formBinding) {
            if (RestCall.this.httpMethod == HttpMethod.PUT) {
                return withBody(formBinding.getFormAsJson());
            } else {
                this.body = formBinding.getFormUrlEncoded();
                return this;
            }
        }

        public RestCallBuilder onlyActive(final boolean active) {
            this.queryParams.put(Entity.FILTER_ATTR_ACTIVE, Arrays.asList(String.valueOf(active)));
            return this;
        }

        public final Result<T> call() {
            return RestCall.this.exchange(this);
        }

        public String buildURI() {
            return this.uriComponentsBuilder
                    .cloneBuilder()
                    .path(RestCall.this.path)
                    .queryParams(this.queryParams)
                    .build(false)
                    .toString();
        }

        public HttpEntity<?> buildRequestEntity() {
            if (this.streamingBody != null) {
                return new HttpEntity<>(new InputStreamResource(this.streamingBody), this.httpHeaders);
            } else if (this.body != null) {
                return new HttpEntity<>(this.body, this.httpHeaders);
            } else {
                return new HttpEntity<>(this.httpHeaders);
            }
        }

        public Map<String, String> getURIVariables() {
            return Utils.immutableMapOf(this.uriVariables);
        }

        @Override
        public String toString() {
            return "RestCallBuilder [httpHeaders=" + this.httpHeaders + ", body=" + this.body + ", queryParams="
                    + this.queryParams
                    + ", uriVariables=" + this.uriVariables + "]";
        }
    }

    public static final class TypeKey<T> {
        final CallType callType;
        final EntityType entityType;
        private final TypeReference<T> typeRef;

        public TypeKey(
                final CallType callType,
                final EntityType entityType,
                final TypeReference<T> typeRef) {

            this.callType = callType;
            this.entityType = entityType;
            this.typeRef = typeRef;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.callType == null) ? 0 : this.callType.hashCode());
            result = prime * result + ((this.entityType == null) ? 0 : this.entityType.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final TypeKey<?> other = (TypeKey<?>) obj;
            if (this.callType != other.callType)
                return false;
            if (this.entityType != other.entityType)
                return false;
            return true;
        }
    }

}
