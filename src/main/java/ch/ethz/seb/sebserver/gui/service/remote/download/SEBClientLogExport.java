/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.ExportSEBClientLogs;

@Lazy
@Component
@GuiProfile
public class SEBClientLogExport extends AbstractDownloadServiceHandler {

    private static final Logger log = LoggerFactory.getLogger(SEBClientLogExport.class);

    private final RestService restService;

    protected SEBClientLogExport(final RestService restService) {
        this.restService = restService;
    }

    @Override
    protected void webserviceCall(
            final String modelId,
            final String parentModelId,
            final OutputStream downloadOut,
            final HttpServletRequest request) {

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

        final Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            final String param = paramNames.nextElement();
            queryParams.add(param, String.valueOf(request.getParameter(param)));
        }

        this.restService
                .getBuilder(ExportSEBClientLogs.class)
                .withResponseExtractor(response -> {

                    try {
                        final InputStream input = response.getBody();
                        IOUtils.copyLarge(input, downloadOut);
                    } catch (final IOException e) {
                        log.error("Unexpected error while streaming to output-stream of download response: ", e);
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            downloadOut.flush();
                            downloadOut.close();
                        } catch (final IOException e) {
                            log.error("Unexpected error while trying to close download output-stream");
                        }
                    }

                    return true;
                })
                .withQueryParams(queryParams)
                .call()
                .onError(error -> log.error("SEB Client logs download failed: ", error));
    }

}
