/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.api.seb.cert;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;

import ch.ethz.seb.sebserver.gui.api.RestCall;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Lazy
@Component

public class RemoveCertificate extends RestCall<Collection<EntityKey>> {

    public RemoveCertificate() {
        super(new TypeKey<>(
                CallType.DELETE,
                EntityType.CERTIFICATE,
                new TypeReference<Collection<EntityKey>>() {
                }),
                HttpMethod.DELETE,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.CERTIFICATE_ENDPOINT);
    }

}
