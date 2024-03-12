/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

public class ExamAPIDiscoveryTest {

    @Test
    public void testCreation() throws IOException {
        final JSONMapper jsonMapper = new JSONMapper();
        final ExamAPIDiscovery examAPIDiscovery = new ExamAPIDiscovery(
                "title",
                "description",
                "serverURL",
                new ExamAPIDiscovery.ExamAPIVersion(
                        "v1",
                        Arrays.asList(new ExamAPIDiscovery.Endpoint("name", "desc", "loc", "auth"))),
                new ExamAPIDiscovery.ExamAPIVersion(
                        "v2",
                        Arrays.asList(new ExamAPIDiscovery.Endpoint("name", "desc", "loc", "auth"))));

        final String asString = jsonMapper.writeValueAsString(examAPIDiscovery);

        assertEquals(
                "{\"title\":\"title\",\"description\":\"description\",\"server-location\":\"serverURL\",\"api-versions\":[{\"name\":\"v1\",\"endpoints\":[{\"name\":\"name\",\"description\":\"desc\",\"location\":\"loc\",\"authorization\":\"auth\"}]},{\"name\":\"v2\",\"endpoints\":[{\"name\":\"name\",\"description\":\"desc\",\"location\":\"loc\",\"authorization\":\"auth\"}]}]}",
                asString);

        final ExamAPIDiscovery examAPIDiscovery2 = jsonMapper.readValue(asString, ExamAPIDiscovery.class);
        assertEquals(asString, jsonMapper.writeValueAsString(examAPIDiscovery2));
    }

}
