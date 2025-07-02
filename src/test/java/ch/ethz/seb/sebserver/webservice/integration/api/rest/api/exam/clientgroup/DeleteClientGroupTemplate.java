/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest.api.exam.clientgroup;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.RestCall;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Lazy
@Component
@GuiProfile
public class DeleteClientGroupTemplate extends RestCall<EntityKey> {

    public DeleteClientGroupTemplate() {
        super(new TypeKey<>(
                CallType.DELETE,
                EntityType.CLIENT_GROUP,
                new TypeReference<EntityKey>() {
                }),
                HttpMethod.DELETE,
                MediaType.APPLICATION_JSON,
                API.EXAM_TEMPLATE_ENDPOINT
                        + API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                        + API.EXAM_TEMPLATE_CLIENT_GROUP_PATH_SEGMENT
                        + API.MODEL_ID_VAR_PATH_SEGMENT);
    }

}
