/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LmsSetupTest {

    @Test
    public void testDeserialization() throws JsonParseException, JsonMappingException, IOException {
        final String jsonString =
                "{\"id\":1,\"institutionId\":1,\"name\":\"new LmsSetup 1\",\"lmsType\":\"MOCKUP\",\"lmsClientname\":\"lms1Name\",\"lmsClientsecret\":\"lms1Secret\",\"lmsUrl\":\"https://www.lms1.com\",\"lmsRestApiToken\":null,\"sebClientname\":\"seb1Name\",\"sebClientsecret\":\"seb1Secret\",\"active\":false}";

        final LmsSetup lmsSetup = new ObjectMapper().readValue(jsonString, LmsSetup.class);
        assertNotNull(lmsSetup);
    }

}
