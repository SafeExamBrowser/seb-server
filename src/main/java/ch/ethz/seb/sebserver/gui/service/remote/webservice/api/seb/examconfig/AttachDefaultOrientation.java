/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;

@Lazy
@Component
@GuiProfile
public class AttachDefaultOrientation extends RestCall<TemplateAttribute> {

    public AttachDefaultOrientation() {
        super(new TypeKey<>(
                CallType.SAVE,
                EntityType.CONFIGURATION_NODE,
                new TypeReference<TemplateAttribute>() {
                }),
                HttpMethod.PATCH,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.CONFIGURATION_NODE_ENDPOINT
                        + API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                        + API.TEMPLATE_ATTRIBUTE_ENDPOINT
                        + API.MODEL_ID_VAR_PATH_SEGMENT
                        + API.TEMPLATE_ATTRIBUTE_ATTACH_DEFAULT_ORIENTATION);
    }

}
