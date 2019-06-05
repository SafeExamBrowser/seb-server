/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote;

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

            final String configId = request.getParameter(API.PARAM_MODEL_ID);
            if (StringUtils.isBlank(configId)) {
                log.error(
                        "Mandatory modelId parameter not found within HttpServletRequest. Download request is ignored");
                return;
            }

            log.debug(
                    "Found modelId: {} for {} download. Trying to request webservice...",
                    configId,
                    downloadFileName);

            final byte[] configFile = webserviceCall(configId);

            if (configFile == null) {
                log.error("No or empty download received from webservice. Download request is ignored");
                return;
            }

            log.debug("Sucessfully downloaded from webservice. File size: {}", configFile.length);

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setContentLength(configFile.length);

            final String header =
                    "attachment; filename=\"" + Utils.preventResponseSplittingAttack(downloadFileName) + "\"";
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, header);

            log.debug("Write the download data to response output");

            response.getOutputStream().write(configFile);

        } catch (final Exception e) {
            log.error(
                    "Unexpected error while trying to start download. The download is ignored. Cause: ",
                    e);
        }
    }

    protected abstract byte[] webserviceCall(String configId);

}
