/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.exam;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ch.ethz.seb.sebserver.gbl.api.ExamAPIDiscovery;
import ch.ethz.seb.sebserver.gbl.api.ExamAPIDiscovery.ExamAPIVersion;
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

//        final ObjectWriter writer = this.jsonMapper.writerWithDefaultPrettyPrinter();

        final ExamAPIDiscovery examAPIDiscovery = this.jsonMapper.readValue(contentAsString, ExamAPIDiscovery.class);
        assertNotNull(examAPIDiscovery);
        assertEquals("Safe Exam Browser Server / Exam API Description", examAPIDiscovery.title());
        assertEquals("This is a description of Safe Exam Browser Server's Exam API", examAPIDiscovery.description());
        assertTrue(!examAPIDiscovery.versions().isEmpty());
        final ExamAPIVersion version1 = examAPIDiscovery.versions().iterator().next();
        assertEquals("v1", version1.name());
        assertTrue(!version1.endpoints().isEmpty());
    }

}
