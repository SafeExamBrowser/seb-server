/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;

public class ClientInstructionTest {

    @Test
    public void testSerialization() throws JsonProcessingException {
        final JSONMapper mapper = new JSONMapper();

        final ClientInstruction instruction =
                new ClientInstruction(2L, 45L, InstructionType.SEB_QUIT, "3L", null);
        final String stringValue = mapper.writeValueAsString(instruction);
        assertEquals(
                "{\"id\":2,\"examId\":45,\"type\":\"SEB_QUIT\",\"connectionToken\":\"3L\",\"attributes\":{}}",
                stringValue);

    }

    @Test
    public void testSerializationWithAttrs() throws JsonProcessingException {
        final JSONMapper mapper = new JSONMapper();
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("attr1", "value1");
        attributes.put("attr2", "value2");
        attributes.put("attr3", "value3");

        final ClientInstruction instruction =
                new ClientInstruction(2L, 45L, InstructionType.SEB_QUIT, "3L", attributes);
        final String stringValue = mapper.writeValueAsString(instruction);
        assertEquals(
                "{\"id\":2,\"examId\":45,\"type\":\"SEB_QUIT\",\"connectionToken\":\"3L\",\"attributes\":{\"attr2\":\"value2\",\"attr1\":\"value1\",\"attr3\":\"value3\"}}",
                stringValue);

    }

}
