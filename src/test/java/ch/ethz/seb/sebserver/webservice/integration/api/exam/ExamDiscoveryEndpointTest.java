/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.exam;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;

public class ExamDiscoveryEndpointTest extends ExamAPIIntegrationTester {

    @Value("${sebserver.webservice.api.exam.endpoint.discovery}")
    private String discoveryEndpoint;
    @Autowired
    private JSONMapper jsonMapper;

    @Test
    public void testExamDiscoveryEndpoint() throws Exception {
        // no authorization needed here

        final String contentAsString = this.mockMvc.perform(get(this.discoveryEndpoint))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        final Object json = this.jsonMapper.readValue(contentAsString, Object.class);
        assertEquals(
                "{\r\n" +
                        "  \"title\" : \"Safe Exam Browser Server / Exam API Description\",\r\n" +
                        "  \"description\" : \"This is a description of Safe Exam Browser Server's Exam API\",\r\n" +
                        "  \"server-location\" : \"http://localhost:0\",\r\n" +
                        "  \"api-versions\" : [ {\r\n" +
                        "    \"name\" : \"v1\",\r\n" +
                        "    \"endpoints\" : [ {\r\n" +
                        "      \"name\" : \"access-token-endpoint\",\r\n" +
                        "      \"descripiton\" : \"request OAuth2 access token with client credentials grant\",\r\n" +
                        "      \"location\" : \"/oauth/token\",\r\n" +
                        "      \"authorization\" : \"Basic\"\r\n" +
                        "    }, {\r\n" +
                        "      \"name\" : \"seb-handshake-endpoint\",\r\n" +
                        "      \"descripiton\" : \"endpoint to establish SEB - SEB Server connection\",\r\n" +
                        "      \"location\" : \"/exam-api/v1/handshake\",\r\n" +
                        "      \"authorization\" : \"Bearer\"\r\n" +
                        "    }, {\r\n" +
                        "      \"name\" : \"seb-configuration-endpoint\",\r\n" +
                        "      \"descripiton\" : \"endpoint to get SEB exam configuration in exchange of connection-token and exam identifier\",\r\n"
                        +
                        "      \"location\" : \"/exam-api/v1/examconfig\",\r\n" +
                        "      \"authorization\" : \"Bearer\"\r\n" +
                        "    }, {\r\n" +
                        "      \"name\" : \"seb-ping-endpoint\",\r\n" +
                        "      \"descripiton\" : \"endpoint to send pings to while running exam\",\r\n" +
                        "      \"location\" : \"/exam-api/v1/sebping\",\r\n" +
                        "      \"authorization\" : \"Bearer\"\r\n" +
                        "    }, {\r\n" +
                        "      \"name\" : \"seb-ping-endpoint\",\r\n" +
                        "      \"descripiton\" : \"endpoint to send log events to while running exam\",\r\n" +
                        "      \"location\" : \"/exam-api/v1/seblog\",\r\n" +
                        "      \"authorization\" : \"Bearer\"\r\n" +
                        "    } ]\r\n" +
                        "  } ]\r\n" +
                        "}",
                this.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
    }

}
