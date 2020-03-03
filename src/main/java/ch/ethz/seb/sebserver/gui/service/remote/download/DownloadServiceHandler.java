/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.download;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Interface defining a service to handle downloads */
public interface DownloadServiceHandler {

    /** Process a requested download
     *
     * @param request The download HttpServletRequest
     * @param response the response to send the download to */
    void processDownload(final HttpServletRequest request, final HttpServletResponse response);

}
