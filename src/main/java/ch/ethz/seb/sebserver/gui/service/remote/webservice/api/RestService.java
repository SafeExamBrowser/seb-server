/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.WebserviceURIService;

@Lazy
@Service
@GuiProfile
public class RestService {

    private final AuthorizationContextHolder authorizationContextHolder;
    private final WebserviceURIService webserviceURIBuilderSupplier;
    private final Map<String, RestCall<?>> calls;

    public RestService(
            final AuthorizationContextHolder authorizationContextHolder,
            final JSONMapper jsonMapper,
            final Collection<RestCall<?>> calls) {

        this.authorizationContextHolder = authorizationContextHolder;
        this.webserviceURIBuilderSupplier = authorizationContextHolder
                .getWebserviceURIService();

        this.calls = calls
                .stream()
                .collect(Collectors.toMap(
                        call -> call.getClass().getName(),
                        call -> call.init(this, jsonMapper)));
    }

    public final RestTemplate getWebserviceAPIRestTemplate() {
        return this.authorizationContextHolder
                .getAuthorizationContext()
                .getRestTemplate();
    }

    public final UriComponentsBuilder getWebserviceURIBuilder() {
        return this.webserviceURIBuilderSupplier.getURIBuilder();
    }

    @SuppressWarnings("unchecked")
    public final <T> RestCall<T> getRestCall(final Class<? extends RestCall<T>> type) {
        return (RestCall<T>) this.calls.get(type.getName());
    }

    public final <T> RestCall<T>.RestCallBuilder getBuilder(final Class<? extends RestCall<T>> type) {
        @SuppressWarnings("unchecked")
        final RestCall<T> restCall = (RestCall<T>) this.calls.get(type.getName());
        if (restCall == null) {
            return null;
        }

        return restCall.newBuilder();
    }

}
