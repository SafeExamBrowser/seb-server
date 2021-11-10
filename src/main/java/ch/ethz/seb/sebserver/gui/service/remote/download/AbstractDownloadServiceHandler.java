/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.download;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public abstract class AbstractDownloadServiceHandler implements DownloadServiceHandler {

    private static final Logger log = LoggerFactory.getLogger(AbstractDownloadServiceHandler.class);

    @Override
    public void processDownload(final HttpServletRequest request, final HttpServletResponse response) {
        try {

            final String downloadFileName = request.getParameter(DownloadService.DOWNLOAD_FILE_NAME);
            if (StringUtils.isBlank(downloadFileName)) {
                log.error(
                        "Mandatory downloadFileName parameter not found within HttpServletRequest. Download request is ignored");
                return;
            }

            log.debug("download requested... trying to get needed parameter from request");

            final String modelId = request.getParameter(API.PARAM_MODEL_ID);
            if (log.isDebugEnabled()) {
                log.debug("Found modelId: {} for {} download.", modelId);
            }

            final String parentModelId = request.getParameter(API.PARAM_PARENT_MODEL_ID);
            if (log.isDebugEnabled()) {
                log.debug(
                        "Found parentModelId: {} for {} download. Trying to request webservice...",
                        modelId,
                        downloadFileName);
            }

            final String header = "attachment; filename=\"" +
                    Utils.escapeHTML_XML_EcmaScript(Utils.preventResponseSplittingAttack(downloadFileName)) +
                    "\"";
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, header);
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

            webserviceCall(modelId, parentModelId, response.getOutputStream(), request);

        } catch (final Exception e) {
            log.error(
                    "Unexpected error while trying to start download. The download is ignored. Cause: ",
                    e);
        }
    }

    protected abstract void webserviceCall(
            String modelId,
            String parentModelId,
            OutputStream downloadOut,
            HttpServletRequest request);

}
