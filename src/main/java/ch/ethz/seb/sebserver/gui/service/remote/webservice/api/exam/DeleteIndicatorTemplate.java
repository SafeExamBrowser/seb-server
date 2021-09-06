/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;

@Lazy
@Component
@GuiProfile
public class DeleteIndicatorTemplate extends RestCall<EntityKey> {

    public DeleteIndicatorTemplate() {
        super(new TypeKey<>(
                CallType.DELETE,
                EntityType.INDICATOR,
                new TypeReference<EntityKey>() {
                }),
                HttpMethod.DELETE,
                MediaType.APPLICATION_JSON,
                API.EXAM_TEMPLATE_ENDPOINT
                        + API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                        + API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT
                        + API.MODEL_ID_VAR_PATH_SEGMENT);
    }

}
