/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest.api.logs;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.AbstractDownloadCall;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Lazy
@Component
@GuiProfile
public class ExportSEBClientLogs extends AbstractDownloadCall {

    public ExportSEBClientLogs() {
        super(MediaType.APPLICATION_FORM_URLENCODED,
                API.SEB_CLIENT_EVENT_ENDPOINT
                        + API.SEB_CLIENT_EVENT_EXPORT_PATH_SEGMENT);
    }

}
