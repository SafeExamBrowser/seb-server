/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectWriter;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult.ErrorType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCreationInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig.ConfigPurpose;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TitleOrientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.RunningExamInfo;
import ch.ethz.seb.sebserver.gbl.model.session.SimpleIndicatorValue;
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

        domainObject = new SEBClientConfig(
                1L, 1L, "name", ConfigPurpose.CONFIGURE_CLIENT,
                true, "fallbackStartURL", 20000L, (short) 3, (short) 1000, "fallbackPassword",
                "fallbackPasswordConfirm",
                "quitPassword", "quitPasswordConfirm", DateTime.now(), "encryptSecret", "encryptSecretConfirm", true);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ConfigurationNode(
                1L, 1L, 1L, "name", "description", ConfigurationType.EXAM_CONFIG, "ownerUUID",
                ConfigurationStatus.CONSTRUCTION);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new View(1L, "name", 20, 1, 1L);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ConfigurationAttribute(
                1L, 1L, "name", AttributeType.CHECKBOX, "resources", "validator", "dependencies", "false");
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ConfigurationValue(1L, 1L, 1L, 1L, 0, "");
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ConfigurationTableValues(1L, 1L, 1L, Arrays.asList(
                new ConfigurationTableValues.TableValue(1L, 0, "value"),
                new ConfigurationTableValues.TableValue(1L, 1, "value"),
                new ConfigurationTableValues.TableValue(1L, 2, "value")));
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new Orientation(1L, 1L, 1L, 1L, "groupId", 2, 3, 2, 2, TitleOrientation.LEFT);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ConfigCreationInfo(1L, "name", "description", true, ConfigurationType.EXAM_CONFIG);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ConfigKey("key");
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        final HashMap<String, String> attrs = new HashMap<>();
        attrs.put("attr1", "value1");
        attrs.put("attr2", "value2");
        attrs.put("attr3", "value3");
        domainObject = new QuizData(
                "courseId", 1L, 1L, LmsType.OPEN_EDX, "name", "description", DateTime.now(), DateTime.now(), "startURL",
                attrs);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new Chapters(Arrays.asList(
                new Chapters.Chapter("name", "id"),
                new Chapters.Chapter("name", "id"),
                new Chapters.Chapter("name", "id")));
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new Exam(
                1L, 1L, 1L, "externalId", "name", "description", DateTime.now(), DateTime.now(),
                "startURL", ExamType.BYOD, "owner",
                Arrays.asList("user1", "user2"),
                ExamStatus.RUNNING, "browserExamKeys", true, null);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new Indicator(
                1L, 1L, "name", IndicatorType.LAST_PING, "#111111", Arrays.asList(
                        new Indicator.Threshold(100.0, "#111111"),
                        new Indicator.Threshold(200.0, "#222222"),
                        new Indicator.Threshold(300.0, "#333333")));
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ExamConfigurationMap(
                1L, 1L, 1L, "examName", "examDescription", DateTime.now(), ExamType.BYOD,
                1L, "userNames", "encryptSecret", "confirmEncryptSecret", "configName", "configDescription",
                ConfigurationStatus.IN_USE);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new SEBRestriction(
                1L, Arrays.asList("key1", "key2"), Arrays.asList("key1", "key2"), attrs);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new OpenEdxSEBRestriction(
                Arrays.asList("key1", "key2"), Arrays.asList("key1", "key2"),
                Arrays.asList("path1", "path2"), Arrays.asList("chapterId1", "chapterId2"),
                Arrays.asList("key1", "key2"), false);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new EntityKey(1L, EntityType.EXAM);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new EntityName("1", EntityType.EXAM, "name");
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new Page<>(3, 1, "columnName", Arrays.asList("Entry1", "Entry1", "Entry1", "Entry1", "Entry1"));
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject =
                new RunningExamInfo("exam123", "An Exam", "https.//some.example.com/exam1", LmsType.OPEN_EDX.name());
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject =
                new SimpleIndicatorValue(IndicatorType.LAST_PING, 1.0);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject =
                new ClientConnection(
                        1L, 1L, 1L, ConnectionStatus.ACTIVE, UUID.randomUUID().toString(),
                        "user-account-1", "86.119.30.213", "0.0.0.0", System.currentTimeMillis());
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ClientConnectionData(
                false,
                new ClientConnection(
                        1L, 1L, 1L, ConnectionStatus.ACTIVE, UUID.randomUUID().toString(),
                        "user-account-1", "86.119.30.213", "0.0.0.0", System.currentTimeMillis()),
                Arrays.asList(
                        new SimpleIndicatorValue(IndicatorType.LAST_PING, 1.0),
                        new SimpleIndicatorValue(IndicatorType.ERROR_COUNT, 2.0),
                        new SimpleIndicatorValue(IndicatorType.WARN_COUNT, 3.0)));
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ClientEvent(1L, 1L, EventType.WARN_LOG,
                System.currentTimeMillis(), System.currentTimeMillis(), 123.0, "text");
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ExtendedClientEvent(
                1L, 1L, "user-account-1", 1L, 1L, EventType.WARN_LOG,
                System.currentTimeMillis(), System.currentTimeMillis(), 123.0, "text");
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ClientInstruction(1L, 1L, InstructionType.SEB_QUIT, UUID.randomUUID().toString(), attrs);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

    }

}
