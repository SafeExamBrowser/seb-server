/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ExportPlainXML;

@Lazy
@Component
@GuiProfile
public class SebExamConfigDownload extends AbstractDownloadServiceHandler {

    private final RestService restService;

    protected SebExamConfigDownload(final RestService restService) {
        this.restService = restService;
    }

    @Override
    protected byte[] webserviceCall(final String modelId) {
        return this.restService.getBuilder(ExportPlainXML.class)
                .withURIVariable(API.PARAM_MODEL_ID, modelId)
                .call()
                .getOrThrow();
    }

}
