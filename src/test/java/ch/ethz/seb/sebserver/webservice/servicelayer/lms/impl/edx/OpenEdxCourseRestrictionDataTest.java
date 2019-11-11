/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.edx;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SebRestrictionData;

public class OpenEdxCourseRestrictionDataTest {

    @Test
    public void testEmpty1() throws JsonProcessingException {
        final JSONMapper mapper = new JSONMapper();

        final OpenEdxCourseRestrictionData data = new OpenEdxCourseRestrictionData(null, null, null, null, null, false);
        final String json = mapper.writeValueAsString(data);
        assertEquals(
                "{\"CONFIG_KEYS\":[],\"BROWSER_KEYS\":[],\"WHITELIST_PATHS\":[],\"BLACKLIST_CHAPTERS\":[],\"PERMISSION_COMPONENTS\":[\"AlwaysAllowStaff\"],\"USER_BANNING_ENABLED\":false}",
                json);
    }

    @Test
    public void testEmpty2() throws JsonProcessingException {
        final JSONMapper mapper = new JSONMapper();

        final OpenEdxCourseRestrictionData data =
                new OpenEdxCourseRestrictionData(new SebRestrictionData(null, null, null, null));
        final String json = mapper.writeValueAsString(data);
        assertEquals(
                "{\"CONFIG_KEYS\":[],\"BROWSER_KEYS\":[],\"WHITELIST_PATHS\":[],\"BLACKLIST_CHAPTERS\":[],\"PERMISSION_COMPONENTS\":[\"AlwaysAllowStaff\"],\"USER_BANNING_ENABLED\":false}",
                json);
    }

}
