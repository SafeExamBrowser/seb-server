/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.lms.api.endpoint}")
public class LmsIntegrationController {

    private static final Logger log = LoggerFactory.getLogger(LmsIntegrationController.class);

    private final FullLmsIntegrationService fullLmsIntegrationService;

    public LmsIntegrationController(final FullLmsIntegrationService fullLmsIntegrationService) {
        this.fullLmsIntegrationService = fullLmsIntegrationService;
    }

    @RequestMapping(
            path = API.LMS_FULL_INTEGRATION_REFRESH_TOKEN_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void refreshAccessToken(
            @RequestParam(name = API.LMS_FULL_INTEGRATION_LMS_UUID, required = true) final String lmsUUID,
            final HttpServletResponse response) {

        final Result<Void> result = fullLmsIntegrationService.refreshAccessToken(lmsUUID)
                .onError(e -> log.error("Failed to refresh access token for LMS Setup: {}", lmsUUID, e));

        if (result.hasError()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
        } else {
            response.setStatus(HttpStatus.OK.value());
        }
    }
}
