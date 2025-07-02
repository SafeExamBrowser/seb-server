/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest.api.exam.template;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
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
public class SaveExamTemplateScreenProctoringSettings extends RestCall<ScreenProctoringSettings> {

    public SaveExamTemplateScreenProctoringSettings() {
        super(new TypeKey<>(
                CallType.SAVE,
                EntityType.EXAM_PROCTOR_DATA,
                new TypeReference<ScreenProctoringSettings>() {
                }),
                HttpMethod.POST,
                MediaType.APPLICATION_JSON,
                API.EXAM_TEMPLATE_ENDPOINT
                        + API.MODEL_ID_VAR_PATH_SEGMENT
                        + API.EXAM_ADMINISTRATION_SCREEN_PROCTORING_PATH_SEGMENT);
    }

}
