/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam;

import java.util.List;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

public class GetFollowupSelection extends RestCall<List<EntityName>>  {

    public GetFollowupSelection() {
        super(new TypeKey<>(
                        CallType.GET_NAMES,
                        EntityType.EXAM,
                        new TypeReference<List<EntityName>>() {
                        }),
                HttpMethod.GET,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.EXAM_ADMINISTRATION_ENDPOINT + 
                        API.NAMES_PATH_SEGMENT + 
                        API.EXAM_ADMINISTRATION_FOLLOWUP_PATH_SEGMENT);
    }
    
}
