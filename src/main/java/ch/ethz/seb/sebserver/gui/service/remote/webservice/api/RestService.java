/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ch.ethz.seb.sebserver.gbl.JSONMapper;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.WebserviceURIBuilderSupplier;

@Lazy
@Service
@GuiProfile
public class RestService {

    private static final Logger log = LoggerFactory.getLogger(RestService.class);

    private final AuthorizationContextHolder authorizationContextHolder;
    private final WebserviceURIBuilderSupplier webserviceURIBuilderSupplier;
    private final JSONMapper jsonMapper;

    public RestService(
            final AuthorizationContextHolder authorizationContextHolder,
            final WebserviceURIBuilderSupplier webserviceURIBuilderSupplier,
            final JSONMapper jsonMapper) {

        this.authorizationContextHolder = authorizationContextHolder;
        this.webserviceURIBuilderSupplier = webserviceURIBuilderSupplier;
        this.jsonMapper = jsonMapper;
    }

    public RestTemplate getWebserviceAPIRestTemplate() {
        return this.authorizationContextHolder
                .getAuthorizationContext()
                .getRestTemplate();
    }

    public UriComponentsBuilder getWebserviceURIBuilder() {
        return this.webserviceURIBuilderSupplier.getBuilder();
    }

    public <T> RestCall<T> getRestCall(final Class<? extends RestCall<T>> type) {
        try {
            final RestCall<T> restCall = type.getDeclaredConstructor().newInstance();
            restCall.init(this, this.jsonMapper);
            return restCall;
        } catch (final Exception e) {
            log.error("Error while trying to create RestCall of type: {}", type, e);
            return new BuildErrorCall<>(e);
        }
    }

}
