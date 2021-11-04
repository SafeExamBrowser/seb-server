/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam;

import java.io.InputStream;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.AbstractExportCall;

@Lazy
@Component
@GuiProfile
public class ExportSEBClientLogs extends AbstractExportCall {

    public ExportSEBClientLogs() {
        super(new TypeKey<>(
                CallType.UNDEFINED,
                EntityType.CLIENT_EVENT,
                new TypeReference<InputStream>() {
                }),
                HttpMethod.GET,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.SEB_CLIENT_EVENT_ENDPOINT
                        + API.SEB_CLIENT_EVENT_EXPORT_PATH_SEGMENT);
    }

}
