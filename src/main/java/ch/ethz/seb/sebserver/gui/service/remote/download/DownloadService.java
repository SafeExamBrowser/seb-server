/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.download;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.service.ServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;

/** Implements a eclipse RAP ServiceHandler to handle downloads */
@Lazy
@Service
@GuiProfile
public class DownloadService implements ServiceHandler {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);

    public static final String DOWNLOAD_SERVICE_NAME = "DOWNLOAD_SERVICE";
    public static final String HANDLER_NAME_PARAMETER = "download-handler-name";
    public static final String DOWNLOAD_FILE_NAME = "download-file-name";

    private final Map<String, DownloadServiceHandler> handler;

    protected DownloadService(final Collection<DownloadServiceHandler> handler) {
        this.handler = handler
                .stream()
                .collect(Collectors.toMap(
                        h -> h.getClass().getSimpleName(),
                        Function.identity()));
    }

    @Override
    public void service(
            final HttpServletRequest request,
            final HttpServletResponse response) {

        log.debug("Received download service request: {}", request.getRequestURI());

        final String handlerName = request.getParameter(HANDLER_NAME_PARAMETER);
        if (StringUtils.isBlank(handlerName)) {
            log.error("Missing request parameter {}. Ignoring download service request",
                    HANDLER_NAME_PARAMETER);
            return;
        }

        if (!this.handler.containsKey(handlerName)) {
            log.error("Missing DownloadServiceHandler with name {}. Ignoring download service request",
                    handlerName);
            return;
        }

        this.handler
                .get(handlerName)
                .processDownload(request, response);
    }

    public String createDownloadURL(
            final Class<? extends DownloadServiceHandler> handlerClass,
            final String downloadFileName,
            final Map<String, String> queryAttrs) {

        final StringBuilder url = new StringBuilder()
                .append(RWT.getServiceManager()
                        .getServiceHandlerUrl(DownloadService.DOWNLOAD_SERVICE_NAME))
                .append(Constants.FORM_URL_ENCODED_SEPARATOR)
                .append(DownloadService.HANDLER_NAME_PARAMETER)
                .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                .append(handlerClass.getSimpleName())
                .append(Constants.FORM_URL_ENCODED_SEPARATOR)
                .append(DownloadService.DOWNLOAD_FILE_NAME)
                .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                .append(downloadFileName);

        queryAttrs.forEach((name, value) -> {
            url.append(Constants.FORM_URL_ENCODED_SEPARATOR)
                    .append(name)
                    .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                    .append(Utils.encodeFormURL_UTF_8(value));
        });

        return url.toString();
    }

    public String createDownloadURL(
            final String modelId,
            final Class<? extends DownloadServiceHandler> handlerClass,
            final String downloadFileName) {

        return createDownloadURL(modelId, null, handlerClass, downloadFileName);
    }

    public String createDownloadURL(
            final String modelId,
            final String parentModelId,
            final Class<? extends DownloadServiceHandler> handlerClass,
            final String downloadFileName) {

        final StringBuilder url = new StringBuilder()
                .append(RWT.getServiceManager()
                        .getServiceHandlerUrl(DownloadService.DOWNLOAD_SERVICE_NAME))
                .append(Constants.FORM_URL_ENCODED_SEPARATOR)
                .append(API.PARAM_MODEL_ID)
                .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                .append(modelId)
                .append(Constants.FORM_URL_ENCODED_SEPARATOR)
                .append(DownloadService.HANDLER_NAME_PARAMETER)
                .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                .append(handlerClass.getSimpleName())
                .append(Constants.FORM_URL_ENCODED_SEPARATOR)
                .append(DownloadService.DOWNLOAD_FILE_NAME)
                .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                .append(downloadFileName);

        if (StringUtils.isNotBlank(parentModelId)) {
            url.append(Constants.FORM_URL_ENCODED_SEPARATOR)
                    .append(API.PARAM_PARENT_MODEL_ID)
                    .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                    .append(parentModelId);
        }

        return url.toString();
    }

}
