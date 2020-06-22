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

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult.ErrorType;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;

public class ModelObjectJSONGenerator {

    @Test
    public void generateJSON() throws Exception {
        final JSONMapper mapper = new JSONMapper();
        final ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();

        Object domainObject = new UserInfo("uuid", 1L, DateTime.now(), "name", "surname", "username", "email",
                true, Locale.ENGLISH, DateTimeZone.UTC,
                new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name(), UserRole.EXAM_SUPPORTER.name())));

        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new UserMod(
                "UUID", 1L, "NAME", "SURNAME", "USERNAME", "newPassword", "confirmNewPassword", "EMAIL",
                Locale.ENGLISH, DateTimeZone.UTC,
                new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name(), UserRole.EXAM_SUPPORTER.name())));
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new PasswordChange("userUUID", "password", "newPassword", "confirmNewPassword");
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new UserActivityLog(
                3L, "userUUID", "username", 123L, UserLogActivityType.EXPORT,
                EntityType.USER, "5", "message");
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new LmsSetup(
                1L, 1L, "name", LmsType.OPEN_EDX, "lmsApiAccountName", "lmsApiAccountPassword",
                "lmsApiUrl", "lmsApiToken", "proxyHost", 8085, "proxyAuthUsername", "proxyAuthSecret", true);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new LmsSetupTestResult(
                Arrays.asList(new LmsSetupTestResult.Error(ErrorType.QUIZ_ACCESS_API_REQUEST, "No Access")),
                Arrays.asList(APIMessage.ErrorMessage.UNEXPECTED.of()));
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

    }

}
