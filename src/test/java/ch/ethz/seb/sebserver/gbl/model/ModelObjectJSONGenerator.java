/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectWriter;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;

public class ModelObjectJSONGenerator {

    @Test
    public void generateJSON() throws Exception {
        final Object domainObject = new UserInfo("uuid", 1L, DateTime.now(), "name", "surname", "username", "email",
                true, Locale.ENGLISH, DateTimeZone.UTC,
                new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name(), UserRole.EXAM_SUPPORTER.name())));

        final JSONMapper mapper = new JSONMapper();
        final ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();
        System.out.print(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

    }

}
