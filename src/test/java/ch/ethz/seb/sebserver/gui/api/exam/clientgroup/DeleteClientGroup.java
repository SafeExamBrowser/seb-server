/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.api.exam.clientgroup;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;

import ch.ethz.seb.sebserver.gui.api.RestCall;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Lazy
@Component

public class DeleteClientGroup extends RestCall<EntityProcessingReport> {

    public DeleteClientGroup() {
        super(new TypeKey<>(
                CallType.DELETE,
                EntityType.CLIENT_GROUP,
                new TypeReference<EntityProcessingReport>() {
                }),
                HttpMethod.DELETE,
                MediaType.APPLICATION_JSON,
                API.EXAM_CLIENT_GROUP_ENDPOINT + API.MODEL_ID_VAR_PATH_SEGMENT);
    }

}
