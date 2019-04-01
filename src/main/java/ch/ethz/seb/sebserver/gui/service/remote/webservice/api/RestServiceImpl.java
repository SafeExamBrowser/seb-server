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

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall.CallType;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.WebserviceURIService;

@Lazy
@Service
@GuiProfile
public class RestServiceImpl implements RestService {

    private final AuthorizationContextHolder authorizationContextHolder;
    private final WebserviceURIService webserviceURIBuilderSupplier;
    private final Map<String, RestCall<?>> calls;

    public RestServiceImpl(
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

    @Override
    public final RestTemplate getWebserviceAPIRestTemplate() {
        return this.authorizationContextHolder
                .getAuthorizationContext()
                .getRestTemplate();
    }

    @Override
    public final UriComponentsBuilder getWebserviceURIBuilder() {
        return this.webserviceURIBuilderSupplier.getURIBuilder();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> RestCall<T> getRestCall(final Class<? extends RestCall<T>> type) {
        return (RestCall<T>) this.calls.get(type.getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> RestCall<T> getRestCall(final EntityType entityType, final CallType callType) {

        if (callType == CallType.UNDEFINED) {
            throw new IllegalArgumentException("Undefined CallType not supported");
        }

        return (RestCall<T>) this.calls.values()
                .stream()
                .filter(call -> call.typeKey.callType == callType && call.typeKey.entityType == entityType)
                .findFirst()
                .orElse(null);
    }

    @Override
    public final <T> RestCall<T>.RestCallBuilder getBuilder(final Class<? extends RestCall<T>> type) {
        @SuppressWarnings("unchecked")
        final RestCall<T> restCall = (RestCall<T>) this.calls.get(type.getName());
        if (restCall == null) {
            return null;
        }

        return restCall.newBuilder();
    }

    @Override
    public final <T> RestCall<T>.RestCallBuilder getBuilder(
            final EntityType entityType,
            final CallType callType) {

        if (callType == CallType.UNDEFINED) {
            throw new IllegalArgumentException("Undefined CallType not supported");
        }

        final RestCall<T> restCall = getRestCall(entityType, callType);
        if (restCall == null) {
            return null;
        }

        return restCall.newBuilder();
    }

//    @Override
//    public <T> PageAction activation(final PageAction action) {
//        if (action.restCallType() == null) {
//            throw new IllegalArgumentException("ActionDefinition needs to define a restCallType to use this action");
//        }
//
//        @SuppressWarnings("unchecked")
//        final Class<? extends RestCall<T>> restCallType =
//                (Class<? extends RestCall<T>>) action.restCallType();
//
//        this.getBuilder(restCallType)
//                .withURIVariable(
//                        API.PARAM_MODEL_ID,
//                        action.pageContext().getAttribute(AttributeKeys.ENTITY_ID))
//                .call()
//                .onErrorDo(t -> action.pageContext().notifyError(t));
//
//        return action;
//    }

}
