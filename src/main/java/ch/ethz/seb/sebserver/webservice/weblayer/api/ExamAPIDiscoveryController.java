/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.ExamAPIDiscovery;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebClientConfigService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.exam.endpoint.discovery}")
public class ExamAPIDiscoveryController {

    private final SebClientConfigService sebClientConfigService;
    private final String examAPI_V1_Endpoint;

    protected ExamAPIDiscoveryController(
            final SebClientConfigService sebClientConfigService,
            @Value("${sebserver.webservice.api.exam.endpoint.v1}") final String examAPI_V1_Endpoint) {

        this.sebClientConfigService = sebClientConfigService;
        this.examAPI_V1_Endpoint = examAPI_V1_Endpoint;
    }

    private ExamAPIDiscovery DISCOVERY_INFO;

    @PostConstruct
    void init() {
        this.DISCOVERY_INFO = new ExamAPIDiscovery(
                "Safe Exam Browser Server / Exam API Description",
                "This is a description of Safe Exam Browser Server's Exam API",
                this.sebClientConfigService.getServerURL(),
                Arrays.asList(new ExamAPIDiscovery.ExamAPIVersion(
                        "v1",
                        Arrays.asList(
                                new ExamAPIDiscovery.Endpoint(
                                        "access-token-endpoint",
                                        "request OAuth2 access token with client credentials grant",
                                        API.OAUTH_TOKEN_ENDPOINT,
                                        "Basic"),
                                new ExamAPIDiscovery.Endpoint(
                                        "seb-handshake-endpoint",
                                        "endpoint to establish SEB - SEB Server connection",
                                        this.examAPI_V1_Endpoint + API.EXAM_API_HANDSHAKE_ENDPOINT,
                                        "Bearer"),
                                new ExamAPIDiscovery.Endpoint(
                                        "seb-configuration-endpoint",
                                        "endpoint to get SEB exam configuration in exchange of connection-token and exam identifier",
                                        this.examAPI_V1_Endpoint + API.EXAM_API_CONFIGURATION_REQUEST_ENDPOINT,
                                        "Bearer"),
                                new ExamAPIDiscovery.Endpoint(
                                        "seb-ping-endpoint",
                                        "endpoint to send pings to while running exam",
                                        this.examAPI_V1_Endpoint + API.EXAM_API_PING_ENDPOINT,
                                        "Bearer"),
                                new ExamAPIDiscovery.Endpoint(
                                        "seb-ping-endpoint",
                                        "endpoint to send log events to while running exam",
                                        this.examAPI_V1_Endpoint + API.EXAM_API_EVENT_ENDPOINT,
                                        "Bearer")))));
    }

    @RequestMapping(
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ExamAPIDiscovery getDiscovery() {
        return this.DISCOVERY_INFO;
    }

}
