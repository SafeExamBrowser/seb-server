/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.seb.PingResponse;
import ch.ethz.seb.sebserver.gbl.model.seb.RunningExams;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.exam.endpoint}")
public class ExamAPIController {

    @RequestMapping(
            path = API.EXAM_API_HANDSHAKE_ENDPOINT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public RunningExams handshake(
            @RequestParam(name = API.PARAM_INSTITUTION_ID, required = true) final Long institutionId,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        // TODO
        return null;
    }

    @RequestMapping(
            path = API.EXAM_API_CONFIGURATION_REQUEST_ENDPOINT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_XML_VALUE)
    public RunningExams getConfig(
            @RequestParam(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken,
            @RequestParam(name = API.EXAM_API_PARAM_EXAM_ID, required = true) final String examId,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        // TODO
        return null;
    }

    @RequestMapping(
            path = API.EXAM_API_PING_ENDPOINT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_XML_VALUE)
    public PingResponse ping(
            @RequestParam(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        // TODO
        return null;
    }

    @RequestMapping(
            path = API.EXAM_API_EVENT_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void event(
            @RequestParam(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        // TODO

    }

}
