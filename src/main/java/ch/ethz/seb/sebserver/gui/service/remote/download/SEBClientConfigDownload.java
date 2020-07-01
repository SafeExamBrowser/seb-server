/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.ExportClientConfig;

@Lazy
@Component
@GuiProfile
public class SEBClientConfigDownload extends AbstractDownloadServiceHandler {

    private static final Logger log = LoggerFactory.getLogger(SEBClientConfigDownload.class);

    private final RestService restService;
    public final String downloadFileName;

    protected SEBClientConfigDownload(
            final RestService restService,
            @Value("${sebserver.gui.seb.client.config.download.filename}") final String downloadFileName) {

        this.restService = restService;
        this.downloadFileName = downloadFileName;
    }

    @Override
    protected void webserviceCall(final String modelId, final String parentModelId, final OutputStream downloadOut) {

        final InputStream input = this.restService.getBuilder(ExportClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, modelId)
                .call()
                .getOrThrow();

        try {
            IOUtils.copyLarge(input, downloadOut);
        } catch (final IOException e) {
            log.error(
                    "Unexpected error while streaming incoming config data from web-service to output-stream of download response: ",
                    e);
        } finally {
            try {
                downloadOut.flush();
                downloadOut.close();
            } catch (final IOException e) {
                log.error("Unexpected error while trying to close download output-stream");
            }
        }
    }

}
