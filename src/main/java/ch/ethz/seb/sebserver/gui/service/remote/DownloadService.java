/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.service.ServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Lazy
@Service
@GuiProfile
public class DownloadService implements ServiceHandler {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);

    public static final String DOWNLOAD_SERVICE_NAME = "DOWNLOAD_SERVICE";
    public static final String HANDLER_NAME_PARAMETER = "download-handler-name";

    private final Map<String, DownloadServiceHandler> handler;

    protected DownloadService(final Collection<DownloadServiceHandler> handler) {
        this.handler = handler
                .stream()
                .collect(Collectors.toMap(h -> h.getName(), Function.identity()));
    }

    @Override
    public void service(
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException, ServletException {

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

}
