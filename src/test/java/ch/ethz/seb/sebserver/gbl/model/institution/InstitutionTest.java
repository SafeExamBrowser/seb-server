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
import com.fasterxml.jackson.databind.ObjectWriter;

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
        final Page<Institution> page = new Page<>(2, 1, "name", Arrays.asList(
                new Institution(1L, "InstOne", "one", "", true),
                new Institution(2L, "InstTwo", "two", "", true),
                new Institution(3L, "InstThree", "three", "", true)));

        final JSONMapper jsonMapper = new JSONMapper();
        final ObjectWriter writerWithDefaultPrettyPrinter = jsonMapper.writerWithDefaultPrettyPrinter();
        String json = writerWithDefaultPrettyPrinter.writeValueAsString(page);
        assertEquals("{\r\n" +
                "  \"number_of_pages\" : 2,\r\n" +
                "  \"page_number\" : 1,\r\n" +
                "  \"sort\" : \"name\",\r\n" +
                "  \"content\" : [ {\r\n" +
                "    \"id\" : 1,\r\n" +
                "    \"name\" : \"InstOne\",\r\n" +
                "    \"urlSuffix\" : \"one\",\r\n" +
                "    \"logoImage\" : \"\",\r\n" +
                "    \"active\" : true\r\n" +
                "  }, {\r\n" +
                "    \"id\" : 2,\r\n" +
                "    \"name\" : \"InstTwo\",\r\n" +
                "    \"urlSuffix\" : \"two\",\r\n" +
                "    \"logoImage\" : \"\",\r\n" +
                "    \"active\" : true\r\n" +
                "  }, {\r\n" +
                "    \"id\" : 3,\r\n" +
                "    \"name\" : \"InstThree\",\r\n" +
                "    \"urlSuffix\" : \"three\",\r\n" +
                "    \"logoImage\" : \"\",\r\n" +
                "    \"active\" : true\r\n" +
                "  } ],\r\n" +
                "  \"page_size\" : 3\r\n" +
                "}", json);

        final List<EntityName> namesList = page.content.stream()
                .map(inst -> new EntityName(inst.getEntityKey(), inst.name))
                .collect(Collectors.toList());

        json = writerWithDefaultPrettyPrinter.writeValueAsString(namesList);
        assertEquals("[ {\r\n" +
                "  \"entityType\" : \"INSTITUTION\",\r\n" +
                "  \"modelId\" : \"1\",\r\n" +
                "  \"name\" : \"InstOne\"\r\n" +
                "}, {\r\n" +
                "  \"entityType\" : \"INSTITUTION\",\r\n" +
                "  \"modelId\" : \"2\",\r\n" +
                "  \"name\" : \"InstTwo\"\r\n" +
                "}, {\r\n" +
                "  \"entityType\" : \"INSTITUTION\",\r\n" +
                "  \"modelId\" : \"3\",\r\n" +
                "  \"name\" : \"InstThree\"\r\n" +
                "} ]", json);
    }

}
