/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectWriter;

import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
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
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig.ConfigPurpose;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig.VDIType;
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
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.monitoring.SimpleIndicatorValue;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.PendingNotificationIndication;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ClientConnectionDataInternal;

public class ModelObjectJSONGenerator {

    @Test
    public void generateJSON() throws Exception {
        final JSONMapper mapper = new JSONMapper();
        final ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();

        Object domainObject = new UserInfo("uuid", 1L, DateTime.now(), "name", "surname", "username", "email",
                true, true, true,
                Locale.ENGLISH, DateTimeZone.UTC,
                new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name(), UserRole.EXAM_SUPPORTER.name())),
                Collections.emptyList(),
                Collections.emptyList());

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
                "lmsApiUrl", "lmsApiToken", "proxyHost", 8085, "proxyAuthUsername", "proxyAuthSecret", true,
                System.currentTimeMillis(), null);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new LmsSetupTestResult(
                LmsType.MOCKUP,
                Arrays.asList(new LmsSetupTestResult.Error(ErrorType.QUIZ_ACCESS_API_REQUEST, "No Access")),
                Arrays.asList(APIMessage.ErrorMessage.UNEXPECTED.of()));
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new SEBClientConfig(
                1L, 1L, "name", ConfigPurpose.CONFIGURE_CLIENT,
                1000L,
                VDIType.NO, null, null, null,
                true, "fallbackStartURL", 20000L, (short) 3, (short) 1000, "fallbackPassword",
                "fallbackPasswordConfirm",
                "quitPassword",
                "quitPasswordConfirm",
                DateTime.now(),
                "encryptSecret",
                "encryptSecretConfirm",
                "certAlias",
                false,
                true,
                DateTime.now(),
                "user123",
                null);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ConfigurationNode(
                1L, 1L, 1L, "name", "description", ConfigurationType.EXAM_CONFIG, "ownerUUID",
                ConfigurationStatus.CONSTRUCTION, DateTime.now(), "user123");
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new Configuration(1L, 1L, 1L, "v1", DateTime.now(), false);
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
                1L, 1L, 1L, "externalId", true, "name", DateTime.now(), DateTime.now(),
                ExamType.BYOD, "owner",
                Arrays.asList("user1", "user2"),
                ExamStatus.RUNNING, null, false, "browserExamKeys", true, null, null, null, null);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new Indicator(
                1L, 1L, "name", IndicatorType.LAST_PING, "#111111", "icon1", "tag1", Arrays.asList(
                        new Indicator.Threshold(100.0, "#111111", "icon1"),
                        new Indicator.Threshold(200.0, "#222222", "icon2"),
                        new Indicator.Threshold(300.0, "#333333", "icon3")));
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ExamConfigurationMap(
                1L, 1L, 1L, "examName", "examDescription", DateTime.now(), ExamType.BYOD, ExamStatus.RUNNING,
                1L, 1L, "encryptSecret", "confirmEncryptSecret", "configName", "configDescription",
                ConfigurationStatus.IN_USE);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new SEBRestriction(
                1L, Arrays.asList("key1", "key2"), Arrays.asList("key1", "key2"), attrs, null);
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

        domainObject =
                new Page<>(3, 1, 5, "columnName", Arrays.asList("Entry1", "Entry1", "Entry1", "Entry1", "Entry1"));
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject =
                new RunningExamInfo("exam123", "An Exam", "https.//some.example.com/exam1", LmsType.OPEN_EDX.name());
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject =
                new SimpleIndicatorValue(1L, 1.0);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));
        final long currentTimeMillis = System.currentTimeMillis();
        domainObject =
                new ClientConnection(
                        1L, 1L, 1L, ConnectionStatus.ACTIVE, UUID.randomUUID().toString(),
                        "user-account-1", "86.119.30.213",
                        "seb_os_name", "seb_machine_name", "seb_version",
                        "vdiID", true, "", currentTimeMillis, currentTimeMillis,
                        123L, false, 123L,
                        true,
                        false, null, null);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new ClientConnectionData(
                false,
                false,
                new ClientConnection(
                        1L, 1L, 1L, ConnectionStatus.ACTIVE, UUID.randomUUID().toString(),
                        "user-account-1", "86.119.30.213",
                        "seb_os_name", "seb_machine_name", "seb_version",
                        "vdiID", true, "", currentTimeMillis, currentTimeMillis,
                        123L, false, 123L,
                        true,
                        false, null, null),
                Arrays.asList(
                        new SimpleIndicatorValue(1L, 1.0),
                        new SimpleIndicatorValue(2L, 2.0),
                        new SimpleIndicatorValue(3L, 3.0)),
                new HashSet<>(Arrays.asList(1L, 2L)));
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        final ClientConnectionDataInternal clientConnectionDataInternal = new ClientConnectionDataInternal(
                new ClientConnection(
                        1L, 1L, 1L, ConnectionStatus.ACTIVE, UUID.randomUUID().toString(),
                        "user-account-1", "86.119.30.213",
                        "seb_os_name", "seb_machine_name", "seb_version",
                        "vdiID", true, "", currentTimeMillis, currentTimeMillis,
                        123L, false, 123L,
                        true,
                        false, null, null),
                new PendingNotificationIndication() {
                    @Override
                    public boolean notifictionPending() {
                        return false;
                    }
                },
                Arrays.asList(
                        new ClientIndicatorTestImpl(1L, 1.0),
                        new ClientIndicatorTestImpl(2L, 2.0),
                        new ClientIndicatorTestImpl(3L, 3.0)),
                new HashSet<>(Arrays.asList(1L, 2L)));

        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(
                writerWithDefaultPrettyPrinter.writeValueAsString(clientConnectionDataInternal.monitoringDataView));

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

        domainObject = new EntityDependency(
                new EntityKey(1L, EntityType.EXAM),
                new EntityKey(1L, EntityType.INDICATOR),
                "IndicatorName", "some description");
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = new EntityProcessingReport(
                Arrays.asList(new EntityKey(1L, EntityType.EXAM)),
                Arrays.asList(new EntityKey(1L, EntityType.INDICATOR), new EntityKey(2L, EntityType.INDICATOR)),
                Arrays.asList(new EntityProcessingReport.ErrorEntry(new EntityKey(2L, EntityType.INDICATOR),
                        APIMessage.ErrorMessage.UNEXPECTED.of())),
                BulkActionType.HARD_DELETE);
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

        domainObject = APIMessage.ErrorMessage.UNEXPECTED.of(
                new RuntimeException("some unexpected exception"),
                "attribute1",
                "attribute2",
                "attribute3");
        System.out.println(domainObject.getClass().getSimpleName() + ":");
        System.out.println(writerWithDefaultPrettyPrinter.writeValueAsString(domainObject));

    }

    private static class ClientIndicatorTestImpl implements ClientIndicator {

        public final Long indicatorId;
        public final double value;

        public ClientIndicatorTestImpl(final Long indicatorId, final double value) {
            this.value = value;
            this.indicatorId = indicatorId;
        }

        @Override
        public Long getIndicatorId() {
            // TODO Auto-generated method stub
            return this.indicatorId;
        }

        @Override
        public double getValue() {
            return this.value;
        }

        @Override
        public void init(final Indicator indicatorDefinition, final Long connectionId, final boolean active,
                final boolean cachingEnabled) {
        }

        @Override
        public IndicatorType getType() {
            return IndicatorType.ERROR_COUNT;
        }

        @Override
        public Long examId() {
            return 1L;
        }

        @Override
        public Long connectionId() {
            return 1L;
        }

        @Override
        public double computeValueAt(final long timestamp) {
            return 0;
        }

        @Override
        public Set<EventType> observedEvents() {
            return Collections.emptySet();
        }

        @Override
        public void notifyValueChange(final String textValue, final double numValue) {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean hasIncident() {
            // TODO Auto-generated method stub
            return false;
        }

    }

}
