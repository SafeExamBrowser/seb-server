/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;

public class InstitutionTest {

    @Test
    public void test() throws JsonParseException, JsonMappingException, IOException {
        final String testJson = "{\"id\":\"1\",\"name\":\"ETH Zürichrgerg\",\"urlSuffix\":\"\"}";

        final JSONMapper mapper = new JSONMapper();
        final Institution inst = mapper.readValue(testJson, Institution.class);
    }

}
