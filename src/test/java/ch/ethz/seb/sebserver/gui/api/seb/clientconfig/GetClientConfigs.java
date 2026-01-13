/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.api.seb.clientconfig;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;

import ch.ethz.seb.sebserver.gui.api.PageToListCallAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Lazy
@Component

public class GetClientConfigs extends PageToListCallAdapter<SEBClientConfig> {

    public GetClientConfigs() {
        super(
                GetClientConfigPage.class,
                EntityType.SEB_CLIENT_CONFIGURATION,
                new TypeReference<List<SEBClientConfig>>() {
                },
                API.SEB_CLIENT_CONFIG_ENDPOINT);
    }

}
