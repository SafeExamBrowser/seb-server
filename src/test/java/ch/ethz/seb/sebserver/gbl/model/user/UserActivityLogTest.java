/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;

public class UserActivityLogTest {

    private final JSONMapper jsonMapper = new JSONMapper();

    @Test
    public void testFromToJson() throws IOException {
        final UserActivityLog testModel = new UserActivityLog(
                1L,
                "testUser",
                "testUser",
                123l,
                UserLogActivityType.CREATE,
                EntityType.EXAM,
                "321",
                "noComment");

        final String jsonValue = this.jsonMapper.writeValueAsString(testModel);

        assertEquals(
                "{\"id\":1,\"userUuid\":\"testUser\",\"username\":\"testUser\",\"timestamp\":123,\"activityType\":\"CREATE\",\"entityType\":\"EXAM\",\"entityId\":\"321\",\"message\":\"noComment\"}",
                jsonValue);
    }

}
