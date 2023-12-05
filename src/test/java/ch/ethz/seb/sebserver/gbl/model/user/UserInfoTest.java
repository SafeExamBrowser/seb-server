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
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Page;

public class UserInfoTest {

    @Test
    public void pageOfUserAccount() throws Exception {
        final Page<UserInfo> page = new Page<>(2, 1, 3, "name", Arrays.asList(
                new UserInfo("id1", 1L, new DateTime(0, DateTimeZone.UTC), "user1", "", "user1", "user1@inst2.none",
                        true, Locale.ENGLISH,
                        DateTimeZone.UTC,
                        new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name())),
                        Collections.emptyList(),
                        Collections.emptyList()),
                new UserInfo("id2", 3L, new DateTime(0, DateTimeZone.UTC), "user2", "", "user2", "user2@inst2.none",
                        true, Locale.ENGLISH,
                        DateTimeZone.UTC,
                        new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name())),
                        Collections.emptyList(),
                        Collections.emptyList()),
                new UserInfo("id3", 4L, new DateTime(0, DateTimeZone.UTC), "user3", "", "user3", "user3@inst2.none",
                        false, Locale.GERMAN,
                        DateTimeZone.UTC,
                        new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name())),
                        Collections.emptyList(),
                        Collections.emptyList())));

        final JSONMapper jsonMapper = new JSONMapper();
        //final ObjectWriter writerWithDefaultPrettyPrinter = jsonMapper.writerWithDefaultPrettyPrinter();
        final String json = jsonMapper.writeValueAsString(page);
        assertEquals(
                "{\"number_of_pages\":2,\"page_number\":1,\"page_size\":3,\"sort\":\"name\",\"content\":[{\"uuid\":\"id1\",\"institutionId\":1,\"creationDate\":\"1970-01-01T00:00:00.000Z\",\"name\":\"user1\",\"surname\":\"\",\"username\":\"user1\",\"email\":\"user1@inst2.none\",\"active\":true,\"language\":\"en\",\"timezone\":\"UTC\",\"userRoles\":[\"EXAM_ADMIN\"]},{\"uuid\":\"id2\",\"institutionId\":3,\"creationDate\":\"1970-01-01T00:00:00.000Z\",\"name\":\"user2\",\"surname\":\"\",\"username\":\"user2\",\"email\":\"user2@inst2.none\",\"active\":true,\"language\":\"en\",\"timezone\":\"UTC\",\"userRoles\":[\"EXAM_ADMIN\"]},{\"uuid\":\"id3\",\"institutionId\":4,\"creationDate\":\"1970-01-01T00:00:00.000Z\",\"name\":\"user3\",\"surname\":\"\",\"username\":\"user3\",\"email\":\"user3@inst2.none\",\"active\":false,\"language\":\"de\",\"timezone\":\"UTC\",\"userRoles\":[\"EXAM_ADMIN\"]}],\"complete\":true}",
                json);

    }

}
