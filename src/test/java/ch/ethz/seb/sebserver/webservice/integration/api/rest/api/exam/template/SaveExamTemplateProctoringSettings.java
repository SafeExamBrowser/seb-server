/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest.api.exam.template;

import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.RestCall;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Lazy
@Component
@GuiProfile
public class SaveExamTemplateProctoringSettings extends RestCall<ProctoringServiceSettings> {

    public SaveExamTemplateProctoringSettings() {
        super(new TypeKey<>(
                CallType.SAVE,
                EntityType.EXAM_PROCTOR_DATA,
                new TypeReference<ProctoringServiceSettings>() {
                }),
                HttpMethod.POST,
                MediaType.APPLICATION_JSON,
                API.EXAM_TEMPLATE_ENDPOINT
                        + API.MODEL_ID_VAR_PATH_SEGMENT
                        + API.EXAM_ADMINISTRATION_PROCTORING_PATH_SEGMENT);
    }

}
