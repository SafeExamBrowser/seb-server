/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;

@Lazy
@Component
@GuiProfile
public class ExportSEBConfig extends RestCall<byte[]> {

    protected ExportSEBConfig() {
        super(new TypeKey<>(
                CallType.UNDEFINED,
                EntityType.INSTITUTION,
                new TypeReference<byte[]>() {
                }),
                HttpMethod.GET,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.SEB_CONFIG_EXPORT_ENDPOINT);
    }

    @Override
    protected Result<byte[]> exchange(final RestCallBuilder builder) {
        try {
            final ResponseEntity<byte[]> responseEntity = this.restService
                    .getWebserviceAPIRestTemplate()
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
                    "Error while trying to export SEB Config from webservice. Response: " + responseEntity);
        } catch (final Throwable t) {
            return Result.ofError(t);
        }
    }

}
