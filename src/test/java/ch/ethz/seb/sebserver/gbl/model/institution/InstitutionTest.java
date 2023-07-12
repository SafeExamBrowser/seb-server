/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.Page;

public class InstitutionTest {

    @Test
    public void test() throws JsonParseException, JsonMappingException, IOException {
        final String testJson = "{\"id\":\"1\",\"name\":\"ETH Zürich\",\"urlSuffix\":\"\"}";

        final JSONMapper mapper = new JSONMapper();
        final Institution inst = mapper.readValue(testJson, Institution.class);
        assertTrue(inst.id != null);
        assertTrue(inst.id.longValue() == 1);
        assertEquals("ETH Zürich", inst.name);
    }

    @Test
    public void pageOfInstituions() throws Exception {
        final Page<Institution> page = new Page<>(2, 1, 3, "name", Arrays.asList(
                new Institution(1L, "InstOne", "one", "", "", true),
                new Institution(2L, "InstTwo", "two", "", "", true),
                new Institution(3L, "InstThree", "three", "", "", true)));

        final JSONMapper jsonMapper = new JSONMapper();
        //final ObjectWriter writerWithDefaultPrettyPrinter = jsonMapper.writerWithDefaultPrettyPrinter();
        String json = jsonMapper.writeValueAsString(page);
        assertEquals(
                "{\"number_of_pages\":2,\"page_number\":1,\"page_size\":3,\"sort\":\"name\",\"content\":[{\"id\":1,\"name\":\"InstOne\",\"urlSuffix\":\"one\",\"logoImage\":\"\",\"themeName\":\"\",\"active\":true},{\"id\":2,\"name\":\"InstTwo\",\"urlSuffix\":\"two\",\"logoImage\":\"\",\"themeName\":\"\",\"active\":true},{\"id\":3,\"name\":\"InstThree\",\"urlSuffix\":\"three\",\"logoImage\":\"\",\"themeName\":\"\",\"active\":true}],\"complete\":true}",
                json);

        final List<EntityName> namesList = page.content.stream()
                .map(inst -> new EntityName(inst.getEntityKey(), inst.name))
                .collect(Collectors.toList());

        json = jsonMapper.writeValueAsString(namesList);
        assertEquals(
                "[{\"modelId\":\"1\",\"entityType\":\"INSTITUTION\",\"name\":\"InstOne\"},{\"modelId\":\"2\",\"entityType\":\"INSTITUTION\",\"name\":\"InstTwo\"},{\"modelId\":\"3\",\"entityType\":\"INSTITUTION\",\"name\":\"InstThree\"}]",
                json);
    }

    @Test
    public void testNullValues() throws Exception {
        final JSONMapper jsonMapper = new JSONMapper();

        final Institution inst = new Institution(1L, null, "suffix", "logo", "theme", null);
        final String jsonString = jsonMapper.writeValueAsString(inst);
        assertEquals("{\"id\":1,\"urlSuffix\":\"suffix\",\"logoImage\":\"logo\",\"themeName\":\"theme\"}", jsonString);
    }

}
