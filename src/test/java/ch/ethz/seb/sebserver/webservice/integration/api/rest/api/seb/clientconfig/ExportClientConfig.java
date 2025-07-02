/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest.api.seb.clientconfig;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.AbstractDownloadCall;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Lazy
@Component
@GuiProfile
public class ExportClientConfig extends AbstractDownloadCall {

    public ExportClientConfig() {
        super(MediaType.APPLICATION_FORM_URLENCODED,
                API.SEB_CLIENT_CONFIG_ENDPOINT
                        + API.SEB_CLIENT_CONFIG_DOWNLOAD_PATH_SEGMENT
                        + API.MODEL_ID_VAR_PATH_SEGMENT);
    }

}
