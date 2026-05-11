/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.api.seb.examconfig;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;

import ch.ethz.seb.sebserver.gui.api.RestCall;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Lazy
@Component

public class SaveExamConfigTableValues extends RestCall<ConfigurationTableValues> {

    public SaveExamConfigTableValues() {
        super(new TypeKey<>(
                CallType.SAVE,
                EntityType.CONFIGURATION_VALUE,
                new TypeReference<ConfigurationTableValues>() {
                }),
                HttpMethod.PUT,
                MediaType.APPLICATION_JSON,
                API.CONFIGURATION_VALUE_ENDPOINT + API.CONFIGURATION_TABLE_VALUE_PATH_SEGMENT);
    }

}
