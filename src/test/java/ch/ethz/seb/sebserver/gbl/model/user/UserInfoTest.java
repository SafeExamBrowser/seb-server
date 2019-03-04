/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectWriter;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Page;

public class UserInfoTest {

    @Test
    public void pageOfUserAccount() throws Exception {
        final Page<UserInfo> page = new Page<>(2, 1, "name", Arrays.asList(
                new UserInfo("id1", 1L, "user1", "user1", "user1@inst2.none", true, Locale.ENGLISH, DateTimeZone.UTC,
                        new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name()))),
                new UserInfo("id2", 3L, "user2", "user2", "user2@inst2.none", true, Locale.ENGLISH, DateTimeZone.UTC,
                        new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name()))),
                new UserInfo("id3", 4L, "user3", "user3", "user3@inst2.none", false, Locale.GERMAN, DateTimeZone.UTC,
                        new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name())))));

        final JSONMapper jsonMapper = new JSONMapper();
        final ObjectWriter writerWithDefaultPrettyPrinter = jsonMapper.writerWithDefaultPrettyPrinter();
        final String json = writerWithDefaultPrettyPrinter.writeValueAsString(page);
        assertEquals("{\r\n" +
                "  \"number_of_pages\" : 2,\r\n" +
                "  \"page_number\" : 1,\r\n" +
                "  \"sort\" : \"name\",\r\n" +
                "  \"content\" : [ {\r\n" +
                "    \"uuid\" : \"id1\",\r\n" +
                "    \"institutionId\" : 1,\r\n" +
                "    \"name\" : \"user1\",\r\n" +
                "    \"username\" : \"user1\",\r\n" +
                "    \"email\" : \"user1@inst2.none\",\r\n" +
                "    \"active\" : true,\r\n" +
                "    \"language\" : \"en\",\r\n" +
                "    \"timezone\" : \"UTC\",\r\n" +
                "    \"userRoles\" : [ \"EXAM_ADMIN\" ]\r\n" +
                "  }, {\r\n" +
                "    \"uuid\" : \"id2\",\r\n" +
                "    \"institutionId\" : 3,\r\n" +
                "    \"name\" : \"user2\",\r\n" +
                "    \"username\" : \"user2\",\r\n" +
                "    \"email\" : \"user2@inst2.none\",\r\n" +
                "    \"active\" : true,\r\n" +
                "    \"language\" : \"en\",\r\n" +
                "    \"timezone\" : \"UTC\",\r\n" +
                "    \"userRoles\" : [ \"EXAM_ADMIN\" ]\r\n" +
                "  }, {\r\n" +
                "    \"uuid\" : \"id3\",\r\n" +
                "    \"institutionId\" : 4,\r\n" +
                "    \"name\" : \"user3\",\r\n" +
                "    \"username\" : \"user3\",\r\n" +
                "    \"email\" : \"user3@inst2.none\",\r\n" +
                "    \"active\" : false,\r\n" +
                "    \"language\" : \"de\",\r\n" +
                "    \"timezone\" : \"UTC\",\r\n" +
                "    \"userRoles\" : [ \"EXAM_ADMIN\" ]\r\n" +
                "  } ],\r\n" +
                "  \"page_size\" : 3\r\n" +
                "}", json);

    }

}
