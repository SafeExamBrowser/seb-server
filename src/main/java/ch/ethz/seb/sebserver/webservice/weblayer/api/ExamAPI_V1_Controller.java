/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.seb.PingResponse;
import ch.ethz.seb.sebserver.gbl.model.seb.RunningExam;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.exam.endpoint.v1}")
public class ExamAPI_V1_Controller {

    @RequestMapping(
            path = API.EXAM_API_HANDSHAKE_ENDPOINT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Collection<RunningExam> handshake(
            @RequestParam(name = API.PARAM_INSTITUTION_ID, required = true) final Long institutionId,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        // TODO
        return Arrays.asList(new RunningExam("1", "testExam", "TODO"));
    }

    @RequestMapping(
            path = API.EXAM_API_CONFIGURATION_REQUEST_ENDPOINT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<StreamingResponseBody> getConfig(
            @RequestParam(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken,
            @RequestParam(name = API.EXAM_API_PARAM_EXAM_ID, required = true) final String examId) {

        // TODO
        // 1. check connection validity (connection token)
        // 2. get and stream SEB Exam configuration for specified exam (Id)
        final StreamingResponseBody stream = out -> out.write(Utils.toByteArray("TODO SEB Config"));
        return new ResponseEntity<>(stream, HttpStatus.OK);
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
