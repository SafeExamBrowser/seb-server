/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.integration;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.SEB_CLIENT_CONFIGURATION;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport.ErrorEntry;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.model.exam.IndicatorTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringFeature;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo.CertificateType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCreationInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues.TableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.gbl.model.session.MonitoringFullPageData;
import ch.ethz.seb.sebserver.gbl.model.session.MonitoringSEBConnectionData;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.AttributeMapping;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ExamConfigurationServiceImpl;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestServiceImpl;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.ActivateSEBRestriction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.CheckExamConsistency;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.CheckExamImported;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.CheckSEBRestriction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeactivateSEBRestriction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeleteExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeleteExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeleteExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeleteIndicatorTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.ExportSEBSettingsConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetCourseChapters;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetDefaultExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamConfigMappingNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamConfigMappingsPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamDependencies;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamTemplatePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamTemplates;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicator;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicatorPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicatorTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicatorTemplatePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicators;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetSEBRestrictionSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.NewExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.NewExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.NewIndicator;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.NewIndicatorTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveIndicator;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveIndicatorTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveSEBRestriction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.ActivateInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutionNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.NewInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.ActivateLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.DeactivateLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.GetLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.GetLmsSetupNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.NewLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.SaveLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.TestLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.DeleteAllUserLogs;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.GetExtendedClientEventPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.GetUserLogNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.GetUserLogPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.GetQuizData;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.GetQuizPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.ImportAsExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.cert.AddCertificate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.cert.GetCertificate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.cert.GetCertificateNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.cert.GetCertificatePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.cert.RemoveCertificate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.ActivateClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.DeactivateClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.ExportClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.GetClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.GetClientConfigPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.NewClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.SaveClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.AttachDefaultOrientation;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.CopyConfiguration;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ExportConfigKey;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigAttributes;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationTableValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationValuePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurations;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNode;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNodeNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNodePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetFollowupConfiguration;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetOrientationPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetOrientations;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetTemplateAttribute;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetTemplateAttributePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetViewList;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetViewPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetViews;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ImportExamConfigOnExistingConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ImportNewExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.NewExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.RemoveOrientation;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ResetTemplateValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SEBExamConfigUndo;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfigHistory;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfigTableValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfigValue;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.ConfirmPendingClientNotification;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.DisableClientConnection;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetClientConnection;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetClientConnectionDataList;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetClientConnectionPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetMonitoringFullPageData;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetPendingClientNotifications;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetRunningExamPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.PropagateInstruction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.ActivateUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.ChangePassword;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.DeleteUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccountNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserDependencies;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.NewUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.RegisterNewUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.SaveUserAccount;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UseCasesIntegrationTest extends GuiIntegrationTest {

    @Autowired
    private Cryptor cryptor;

    @Before
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
    public void init() {
        // Nothing
    }

    @After
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
    public void cleanup() {
        // Nothing
    }

    @Test
    @Order(1)
    // *************************************
    // Use Case 1: SEB Administrator creates a new institution and activate this new institution

    public void testUsecase01() {
        final RestServiceImpl restService = createRestServiceForUser(
                "admin",
                "admin",
                new NewInstitution(),
                new ActivateInstitution(),
                new GetInstitution());

        final Result<Institution> result = restService.getBuilder(NewInstitution.class)
                .withFormParam(Domain.INSTITUTION.ATTR_NAME, "Test Institution")
                .call();

        assertNotNull(result);
        assertFalse(result.hasError());
        Institution institution = result.get();
        assertEquals("Test Institution", institution.name);
        assertFalse(institution.active);

        final Result<EntityProcessingReport> resultActivation = restService.getBuilder(ActivateInstitution.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(institution.id))
                .call();

        assertNotNull(resultActivation);
        assertFalse(resultActivation.hasError());

        final Result<Institution> resultGet = restService.getBuilder(GetInstitution.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(institution.id))
                .call();

        assertNotNull(resultGet);
        assertFalse(resultGet.hasError());
        institution = resultGet.get();
        assertEquals("Test Institution", institution.name);
        assertTrue(institution.active);

    }

    @Test
    @Order(2)
    // *************************************
    // Use Case 2: SEB Administrator creates a new Institutional Administrator user for the
    // newly created institution and activate this user
    public void testUsecase02_CreateInstitutionalAdminUser() {
        final RestServiceImpl restService = createRestServiceForUser(
                "admin",
                "admin",
                new GetInstitution(),
                new GetInstitutionNames(),
                new NewUserAccount(),
                new ActivateUserAccount(),
                new GetUserAccount());

        final String instId = restService.getBuilder(GetInstitutionNames.class)
                .call()
                .getOrThrow()
                .stream()
                .filter(inst -> "Test Institution".equals(inst.name))
                .findFirst()
                .get().modelId;

        assertNotNull(instId);

        Result<UserInfo> result = restService.getBuilder(NewUserAccount.class)
                .withFormParam(Domain.USER.ATTR_INSTITUTION_ID, instId)
                .withFormParam(Domain.USER.ATTR_NAME, "TestInstAdmin")
                .withFormParam(Domain.USER.ATTR_USERNAME, "TestInstAdmin")
                .withFormParam(Domain.USER.ATTR_SURNAME, "TestInstAdmin")
                .withFormParam(Domain.USER.ATTR_EMAIL, "test@test.ch")
                .withFormParam(Domain.USER_ROLE.REFERENCE_NAME, UserRole.INSTITUTIONAL_ADMIN.name())
                .withFormParam(PasswordChange.ATTR_NAME_NEW_PASSWORD, "12345678")
                .withFormParam(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, "12345678")
                .withFormParam(Domain.USER.ATTR_LANGUAGE, Locale.ENGLISH.getLanguage())
                .withFormParam(Domain.USER.ATTR_TIMEZONE, DateTimeZone.UTC.getID())
                .call();

        assertFalse(result.hasError());
        UserInfo userInfo = result.get();
        assertNotNull(userInfo);
        assertEquals(instId, String.valueOf(userInfo.institutionId));
        assertEquals("TestInstAdmin", userInfo.name);
        assertEquals("TestInstAdmin", userInfo.username);
        assertEquals("test@test.ch", userInfo.email);
        assertEquals("[INSTITUTIONAL_ADMIN]", String.valueOf(userInfo.getRoles()));
        assertEquals(Locale.ENGLISH, userInfo.language);
        assertEquals(DateTimeZone.UTC, userInfo.timeZone);
        assertFalse(userInfo.isActive());

        final Result<EntityProcessingReport> activation = restService.getBuilder(ActivateUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(userInfo.uuid))
                .call();

        assertFalse(activation.hasError());
        final EntityProcessingReport entityProcessingReport = activation.get();
        assertTrue(entityProcessingReport.getErrors().isEmpty());

        result = restService.getBuilder(GetUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(userInfo.uuid))
                .call();

        assertFalse(result.hasError());
        userInfo = result.get();
        assertTrue(userInfo.isActive());
    }

    @Test
    @Order(3)
    // *************************************
    // Use Case 3: Login with the new TestInstAdmin and check that only its institution is available
    // check also that it is not possible to change to SEB Administrator role
    // check also this it is possible to change the password and after that a new login is needed
    // check also that property changes are possible. E.g: email
    public void testUsecase03_TestInstitutionalView() {
        RestServiceImpl restService = createRestServiceForUser(
                "TestInstAdmin",
                "12345678",
                new GetInstitutionNames(),
                new SaveUserAccount(),
                new ChangePassword(),
                new GetUserAccount(),
                new GetUserAccountNames());

        final List<EntityName> institutions = restService
                .getBuilder(GetInstitutionNames.class)
                .call()
                .getOrThrow();

        assertTrue(institutions.size() == 1);
        assertEquals("Test Institution", institutions.get(0).name);

        final List<EntityName> userNames = restService
                .getBuilder(GetUserAccountNames.class)
                .call()
                .getOrThrow();

        assertTrue(userNames.size() == 1);
        assertEquals("TestInstAdmin (TestInstAdmin TestInstAdmin)", userNames.get(0).name);

        final String userId = userNames.get(0).modelId;

        UserInfo userInfo = restService.getBuilder(GetUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, userId)
                .call()
                .getOrThrow();

        // change email (should work properly)
        assertEquals("test@test.ch", userInfo.email);
        userInfo = UserInfo.withEMail(userInfo, "newMail@test.ch");
        userInfo = restService.getBuilder(SaveUserAccount.class)
                .withBody(userInfo)
                .call()
                .getOrThrow();

        assertEquals("newMail@test.ch", userInfo.email);

        // adding new role that is lower should work (example Exam Admin)
        userInfo = UserInfo.withRoles(userInfo, UserRole.INSTITUTIONAL_ADMIN.name(), UserRole.EXAM_ADMIN.name());
        userInfo = restService.getBuilder(SaveUserAccount.class)
                .withBody(userInfo)
                .call()
                .getOrThrow();

        assertEquals(
                "[EXAM_ADMIN, INSTITUTIONAL_ADMIN]",
                String.valueOf(new LinkedHashSet<>(userInfo.getRoles())));

        // adding new role that is higher shouldn't work
        userInfo = UserInfo.withRoles(userInfo, UserRole.INSTITUTIONAL_ADMIN.name(), UserRole.SEB_SERVER_ADMIN.name());
        final Result<UserInfo> call = restService.getBuilder(SaveUserAccount.class)
                .withBody(userInfo)
                .call();

        assertTrue(call.hasError());
        //assertEquals("Unexpected error while rest call", call.getError().getMessage());
        RestCallError error = (RestCallError) call.getError();
        assertEquals(
                "[APIMessage [messageCode=1001, systemMessage=FORBIDDEN, details=No edit right grant for user: TestInstAdmin, attributes=[]]]",
                String.valueOf(error.getAPIMessages()));

        // change password
        final Result<UserInfo> passwordChange = restService
                .getBuilder(ChangePassword.class)
                .withBody(new PasswordChange(userId, "12345678", "987654321", "987654321"))
                .call();

        assertFalse(passwordChange.hasError());
        userInfo = passwordChange.get();

        // is the login still valid (should not)
        final Result<List<EntityName>> instNames = restService
                .getBuilder(GetInstitutionNames.class)
                .call();
        assertTrue(instNames.hasError());
        error = (RestCallError) instNames.getError();
        assertEquals(
                "UNAUTHORIZED",
                String.valueOf(error.getAPIMessages().get(0).getSystemMessage()));

        // login again with the new password and check roles
        restService = createRestServiceForUser(
                "TestInstAdmin",
                "987654321",
                new GetInstitutionNames(),
                new SaveUserAccount(),
                new ChangePassword(),
                new GetUserAccount(),
                new GetUserAccountNames());

        userInfo = restService.getBuilder(GetUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, userId)
                .call()
                .getOrThrow();

        assertNotNull(userInfo);
        assertEquals("[EXAM_ADMIN, INSTITUTIONAL_ADMIN]", String.valueOf(userInfo.getRoles()));
    }

    @Test
    @Order(4)
    // *************************************
    // Use Case 4:
    // - login as TestInstAdmin
    // - create a new user-account (examAdmin2) with Exam Administrator role
    // - create a new user-account (examSupport1) with Exam Supporter role
    // - create a new user-account (examSupport2) with Exam Administrator and Exam Supporter role
    public void testUsecase04_CreateUserAccount() {
        final RestServiceImpl restService = createRestServiceForUser(
                "TestInstAdmin",
                "987654321",
                new GetInstitutionNames(),
                new NewUserAccount(),
                new ActivateUserAccount());

        final String instId = restService.getBuilder(GetInstitutionNames.class)
                .call()
                .getOrThrow()
                .stream()
                .filter(inst -> "Test Institution".equals(inst.name))
                .findFirst()
                .get().modelId;

        assertNotNull(instId);

        Result<UserInfo> result = restService.getBuilder(NewUserAccount.class)
                .withFormParam(Domain.USER.ATTR_INSTITUTION_ID, instId)
                .withFormParam(Domain.USER.ATTR_NAME, "examAdmin2")
                .withFormParam(Domain.USER.ATTR_USERNAME, "examAdmin2")
                .withFormParam(Domain.USER.ATTR_SURNAME, "examAdmin2")
                .withFormParam(Domain.USER.ATTR_EMAIL, "test@test.ch")
                .withFormParam(Domain.USER_ROLE.REFERENCE_NAME, UserRole.EXAM_ADMIN.name())
                .withFormParam(PasswordChange.ATTR_NAME_NEW_PASSWORD, "examAdmin2")
                .withFormParam(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, "examAdmin2")
                .withFormParam(Domain.USER.ATTR_LANGUAGE, Locale.ENGLISH.getLanguage())
                .withFormParam(Domain.USER.ATTR_TIMEZONE, DateTimeZone.UTC.getID())
                .call();

        assertNotNull(result);
        assertFalse(result.hasError());

        Result<EntityProcessingReport> activation = restService.getBuilder(ActivateUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, result.get().uuid)
                .call();
        assertNotNull(activation);
        assertFalse(activation.hasError());

        // no unique email
        result = restService.getBuilder(NewUserAccount.class)
                .withFormParam(Domain.USER.ATTR_INSTITUTION_ID, instId)
                .withFormParam(Domain.USER.ATTR_NAME, "examSupport2")
                .withFormParam(Domain.USER.ATTR_USERNAME, "examSupport2")
                .withFormParam(Domain.USER.ATTR_SURNAME, "examSupport2")
                .withFormParam(Domain.USER.ATTR_EMAIL, "test@test.ch")
                .withFormParam(Domain.USER_ROLE.REFERENCE_NAME, UserRole.EXAM_SUPPORTER.name())
                .withFormParam(PasswordChange.ATTR_NAME_NEW_PASSWORD, "examSupport2")
                .withFormParam(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, "examSupport2")
                .withFormParam(Domain.USER.ATTR_LANGUAGE, Locale.ENGLISH.getLanguage())
                .withFormParam(Domain.USER.ATTR_TIMEZONE, DateTimeZone.UTC.getID())
                .call();

        assertNotNull(result);
        assertTrue(result.hasError());

        // no unique email
        result = restService.getBuilder(NewUserAccount.class)
                .withFormParam(Domain.USER.ATTR_INSTITUTION_ID, instId)
                .withFormParam(Domain.USER.ATTR_NAME, "examSupport2")
                .withFormParam(Domain.USER.ATTR_USERNAME, "examSupport2")
                .withFormParam(Domain.USER.ATTR_SURNAME, "examSupport2")
                .withFormParam(Domain.USER.ATTR_EMAIL, "test@test1.ch")
                .withFormParam(Domain.USER_ROLE.REFERENCE_NAME, UserRole.EXAM_SUPPORTER.name())
                .withFormParam(PasswordChange.ATTR_NAME_NEW_PASSWORD, "examSupport2")
                .withFormParam(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, "examSupport2")
                .withFormParam(Domain.USER.ATTR_LANGUAGE, Locale.ENGLISH.getLanguage())
                .withFormParam(Domain.USER.ATTR_TIMEZONE, DateTimeZone.UTC.getID())
                .call();

        assertNotNull(result);
        assertFalse(result.hasError());

        activation = restService.getBuilder(ActivateUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, result.get().uuid)
                .call();
        assertNotNull(activation);
        assertFalse(activation.hasError());

        result = restService.getBuilder(NewUserAccount.class)
                .withFormParam(Domain.USER.ATTR_INSTITUTION_ID, instId)
                .withFormParam(Domain.USER.ATTR_NAME, "examSupport1")
                .withFormParam(Domain.USER.ATTR_USERNAME, "examSupport1")
                .withFormParam(Domain.USER.ATTR_SURNAME, "examSupport1")
                .withFormParam(Domain.USER.ATTR_EMAIL, "test@test6.ch")
                .withFormParam(Domain.USER_ROLE.REFERENCE_NAME, UserRole.EXAM_SUPPORTER.name())
                .withFormParam(PasswordChange.ATTR_NAME_NEW_PASSWORD, "examSupport1")
                .withFormParam(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, "examSupport1")
                .withFormParam(Domain.USER.ATTR_LANGUAGE, Locale.ENGLISH.getLanguage())
                .withFormParam(Domain.USER.ATTR_TIMEZONE, DateTimeZone.UTC.getID())
                .call();

        assertNotNull(result);
        assertFalse(result.hasError());

        activation = restService.getBuilder(ActivateUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, result.get().uuid)
                .call();
        assertNotNull(activation);
        assertFalse(activation.hasError());

    }

    @Test
    @Order(5)
    // *************************************
    // Use Case 5: Login as TestInstAdmin and create new LMS Mockup and activate
    //  - login as TestInstAdmin : 987654321
    //  - check there are no LMS Setup and Quizzes currently available for the user
    //  - create new LMS Setup Mockup (no activation)
    //  - check the LMS Setup was created but there are still no quizzes available
    //  - activate LMS Setup
    //  - check now active and quizzes are available
    //  - change name of active LMS and check modification update
    //  - deactivate LMS Setup and check no quizzes are available
    //  - activate again for following tests
    public void testUsecase05_CreateLMSSetupMockup() {
        final RestServiceImpl restService = createRestServiceForUser(
                "TestInstAdmin",
                "987654321",
                new NewLmsSetup(),
                new GetLmsSetupNames(),
                new GetLmsSetup(),
                new SaveLmsSetup(),
                new ActivateLmsSetup(),
                new DeactivateLmsSetup(),
                new GetQuizPage());

        // check there are currently no LMS Setup defined for this user
        Result<List<EntityName>> lmsNames = restService
                .getBuilder(GetLmsSetupNames.class)
                .call();
        assertNotNull(lmsNames);
        assertFalse(lmsNames.hasError());
        List<EntityName> list = lmsNames.get();
        assertTrue(list.isEmpty());

        // check also there are currently no quizzes available for this user
        Result<Page<QuizData>> quizPageCall = restService
                .getBuilder(GetQuizPage.class)
                .call();
        assertNotNull(quizPageCall);
        assertFalse(quizPageCall.hasError());
        Page<QuizData> quizPage = quizPageCall.get();
        assertTrue(quizPage.isEmpty());

        // create new LMS Setup Mockup
        Result<LmsSetup> newLMSCall = restService
                .getBuilder(NewLmsSetup.class)
                .withFormParam(Domain.LMS_SETUP.ATTR_NAME, "Test LMS Mockup")
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_URL, "http://")
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_CLIENTNAME, "test")
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_CLIENTSECRET, "test")
                .call();

        assertNotNull(newLMSCall);
        assertFalse(newLMSCall.hasError());
        LmsSetup lmsSetup = newLMSCall.get();
        assertEquals("Test LMS Mockup", lmsSetup.name);
        assertFalse(lmsSetup.isActive());

        // check is available now
        lmsNames = restService
                .getBuilder(GetLmsSetupNames.class)
                .call();

        assertNotNull(lmsNames);
        assertFalse(lmsNames.hasError());
        list = lmsNames.get();
        assertFalse(list.isEmpty());

        // check still no quizzes available from the LMS (not active now)
        quizPageCall = restService
                .getBuilder(GetQuizPage.class)
                .call();
        assertNotNull(quizPageCall);
        assertFalse(quizPageCall.hasError());
        quizPage = quizPageCall.get();
        assertTrue(quizPage.isEmpty());

        // activate lms setup
        Result<EntityProcessingReport> activation = restService
                .getBuilder(ActivateLmsSetup.class)
                .withURIVariable(API.PARAM_MODEL_ID, lmsSetup.getModelId())
                .call();

        assertNotNull(activation);
        assertFalse(activation.hasError());

        // check lms setup is now active
        newLMSCall = restService
                .getBuilder(GetLmsSetup.class)
                .withURIVariable(API.PARAM_MODEL_ID, lmsSetup.getModelId())
                .call();

        assertNotNull(newLMSCall);
        assertFalse(newLMSCall.hasError());
        lmsSetup = newLMSCall.get();
        assertEquals("Test LMS Mockup", lmsSetup.name);
        assertTrue(lmsSetup.isActive());

        // check quizzes are available now
        quizPageCall = restService
                .getBuilder(GetQuizPage.class)
                .call();
        assertNotNull(quizPageCall);
        assertFalse(quizPageCall.hasError());
        quizPage = quizPageCall.get();
        assertFalse(quizPage.isEmpty());

        // change the name of LMS Setup and check modification update
        newLMSCall = restService
                .getBuilder(SaveLmsSetup.class)
                .withBody(new LmsSetup(
                        lmsSetup.id,
                        lmsSetup.institutionId,
                        "Test LMS Name Changed",
                        lmsSetup.lmsType,
                        lmsSetup.lmsAuthName,
                        lmsSetup.lmsAuthSecret,
                        lmsSetup.lmsApiUrl,
                        lmsSetup.lmsRestApiToken,
                        lmsSetup.proxyHost,
                        lmsSetup.proxyPort,
                        lmsSetup.proxyAuthUsername,
                        lmsSetup.proxyAuthSecret,
                        lmsSetup.active,
                        null))
                .call();

        assertNotNull(newLMSCall);
        assertFalse(newLMSCall.hasError());
        lmsSetup = newLMSCall.get();
        assertEquals("Test LMS Name Changed", lmsSetup.name);
        assertTrue(lmsSetup.isActive());

        // check quizzes are still available
        quizPageCall = restService
                .getBuilder(GetQuizPage.class)
                .call();
        assertNotNull(quizPageCall);
        assertFalse(quizPageCall.hasError());
        quizPage = quizPageCall.get();
        assertFalse(quizPage.isEmpty());

        // deactivate
        final Result<EntityProcessingReport> deactivation = restService
                .getBuilder(DeactivateLmsSetup.class)
                .withURIVariable(API.PARAM_MODEL_ID, lmsSetup.getModelId())
                .call();

        assertNotNull(deactivation);
        assertFalse(deactivation.hasError());

        // check lms setup is now active
        newLMSCall = restService
                .getBuilder(GetLmsSetup.class)
                .withURIVariable(API.PARAM_MODEL_ID, lmsSetup.getModelId())
                .call();

        assertNotNull(newLMSCall);
        assertFalse(newLMSCall.hasError());
        lmsSetup = newLMSCall.get();
        assertEquals("Test LMS Name Changed", lmsSetup.name);
        assertFalse(lmsSetup.isActive());

        // check quizzes are not available anymore
        quizPageCall = restService
                .getBuilder(GetQuizPage.class)
                .call();
        assertNotNull(quizPageCall);
        assertFalse(quizPageCall.hasError());
        quizPage = quizPageCall.get();
        assertTrue(quizPage.isEmpty());

        // activate LMS Setup again for following tests
        activation = restService
                .getBuilder(ActivateLmsSetup.class)
                .withURIVariable(API.PARAM_MODEL_ID, lmsSetup.getModelId())
                .call();

        assertNotNull(activation);
        assertFalse(activation.hasError());

        // check lms setup is now active
        newLMSCall = restService
                .getBuilder(GetLmsSetup.class)
                .withURIVariable(API.PARAM_MODEL_ID, lmsSetup.getModelId())
                .call();
        assertFalse(newLMSCall.hasError());
        final LmsSetup lmsSetup2 = newLMSCall.get();
        assertTrue(lmsSetup2.active);
        assertEquals("Test LMS Name Changed", lmsSetup2.name);
    }

    @Test
    @Order(7)
    // *************************************
    // Use Case 6: Login as examAdmin2
    // - Check if there are some quizzes from previous LMS Setup
    // - Import a quiz as Exam
    // - get exam page and check the exam is there
    // - edit exam property and save again
    public void testUsecase07_ImportExam() {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetUserAccountNames(),
                new NewLmsSetup(),
                new GetQuizPage(),
                new GetQuizData(),
                new CheckExamImported(),
                new ImportAsExam(),
                new SaveExam(),
                new GetExam(),
                new GetExamPage());

        final Result<List<EntityName>> userNamesResult = restService
                .getBuilder(GetUserAccountNames.class)
                .call();

        assertNotNull(userNamesResult);
        assertFalse(userNamesResult.hasError());

        final String userId = userNamesResult.get()
                .stream()
                .filter(userName -> userName.name != null && userName.name.startsWith("examSupport2"))
                .findFirst()
                .map(EntityName::getModelId)
                .orElse(null);

        // check quizzes are defines
        final Result<Page<QuizData>> quizPageCall = restService
                .getBuilder(GetQuizPage.class)
                .call();

        assertNotNull(quizPageCall);
        assertFalse(quizPageCall.hasError());
        final Page<QuizData> quizzes = quizPageCall.get();
        assertFalse(quizzes.isEmpty());
        final QuizData quizData = quizzes.content.get(0);
        assertNotNull(quizData);
        assertEquals("Demo Quiz 1 (MOCKUP)", quizData.name);
        // TODO: Java 8 and Java 11 seems to have different lmsSetupIds here
        //       Find out why!!
        //assertEquals(Long.valueOf(1), quizData.lmsSetupId);
        assertEquals(Long.valueOf(4), quizData.institutionId);

        // check imported
        final Result<Collection<EntityKey>> checkCall = restService.getBuilder(CheckExamImported.class)
                .withURIVariable(API.PARAM_MODEL_ID, quizData.getModelId())
                .call();
        assertFalse(checkCall.hasError());
        final Collection<EntityKey> importCheck = checkCall.getOrThrow();
        assertTrue(importCheck.isEmpty()); // not imported at all

        // import quiz as exam
        final Result<Exam> newExamResult = restService
                .getBuilder(ImportAsExam.class)
                .withFormParam(QuizData.QUIZ_ATTR_LMS_SETUP_ID, String.valueOf(quizData.lmsSetupId))
                .withFormParam(QuizData.QUIZ_ATTR_ID, quizData.id)
                .withFormParam(Domain.EXAM.ATTR_SUPPORTER, userId)
                .call();

        assertNotNull(newExamResult);
        assertFalse(newExamResult.hasError());
        final Exam newExam = newExamResult.get();

        assertEquals("Demo Quiz 1 (MOCKUP)", newExam.name);
        assertEquals(ExamType.UNDEFINED, newExam.type);
        assertFalse(newExam.supporter.isEmpty());

        // create Exam with type and supporter examSupport2
        final Exam examForSave = new Exam(
                newExam.id,
                newExam.institutionId,
                newExam.lmsSetupId,
                newExam.externalId,
                newExam.name,
                newExam.description,
                newExam.startTime,
                newExam.endTime,
                newExam.startURL,
                ExamType.MANAGED,
                null,
                Utils.immutableCollectionOf(userId),
                ExamStatus.RUNNING,
                false,
                null,
                true,
                null, null, null, null);

        final Result<Exam> savedExamResult = restService
                .getBuilder(SaveExam.class)
                .withBody(examForSave)
                .call();

        assertNotNull(savedExamResult);
        assertFalse(savedExamResult.hasError());
        final Exam savedExam = savedExamResult.get();

        assertEquals(ExamType.MANAGED, savedExam.type);
        assertFalse(savedExam.supporter.isEmpty());

        // get exam list
        final Result<Page<Exam>> exams = restService
                .getBuilder(GetExamPage.class)
                .call();

        assertNotNull(exams);
        assertFalse(exams.hasError());
        final Page<Exam> examPage = exams.get();
        assertFalse(examPage.isEmpty());
        assertTrue(examPage.content
                .stream()
                .filter(exam -> exam.name.equals(newExam.name))
                .findFirst().isPresent());

    }

    @Test
    @Order(8)
    // *************************************
    // Use Case 7: Login as examAdmin2
    // - Get imported exam
    // - add new indicator for exam
    // - save exam with new indicator and test
    // - create some thresholds for the new indicator
    public void testUsecase08_CreateExamIndicator() {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetExam(),
                new GetExamNames(),
                new NewIndicator(),
                new SaveIndicator(),
                new GetIndicator(),
                new GetIndicatorPage());

        final Result<List<EntityName>> examNamesResult = restService
                .getBuilder(GetExamNames.class)
                .call();

        assertNotNull(examNamesResult);
        assertFalse(examNamesResult.hasError());
        final List<EntityName> exams = examNamesResult.get();
        assertFalse(exams.isEmpty());
        final EntityName examName = exams.get(0);
        assertEquals("Demo Quiz 1 (MOCKUP)", examName.name);

        final Result<Exam> examResult = restService
                .getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, examName.modelId)
                .call();

        assertNotNull(examResult);
        assertFalse(examResult.hasError());
        final Exam exam = examResult.get();

        final Result<Indicator> newIndicatorResult = restService
                .getBuilder(NewIndicator.class)
                .withFormParam(Domain.INDICATOR.ATTR_EXAM_ID, exam.getModelId())
                .withFormParam(Domain.INDICATOR.ATTR_NAME, "Errors")
                .withFormParam(Domain.INDICATOR.ATTR_TYPE, IndicatorType.ERROR_COUNT.name)
                .withFormParam(Domain.INDICATOR.ATTR_COLOR, "000001")
                .call();

        assertNotNull(newIndicatorResult);
        assertFalse(newIndicatorResult.hasError());
        final Indicator newIndicator = newIndicatorResult.get();

        assertEquals("Errors", newIndicator.name);
        assertEquals("000001", newIndicator.defaultColor);

        final Indicator indicatorToSave = new Indicator(
                newIndicator.id,
                newIndicator.examId,
                newIndicator.name,
                newIndicator.type,
                newIndicator.defaultColor,
                newIndicator.defaultIcon,
                newIndicator.tags,
                Utils.immutableCollectionOf(
                        new Indicator.Threshold(2000d, "000011", null),
                        new Indicator.Threshold(5000d, "001111", null)));

        final Result<Indicator> savedIndicatorResult = restService
                .getBuilder(SaveIndicator.class)
                .withBody(indicatorToSave)
                .call();

        assertNotNull(savedIndicatorResult);
        assertFalse(savedIndicatorResult.hasError());
        final Indicator savedIndicator = savedIndicatorResult.get();

        assertEquals("Errors", savedIndicator.name);
        assertEquals("000001", savedIndicator.defaultColor);
        final Collection<Threshold> thresholds = savedIndicator.getThresholds();
        assertFalse(thresholds.isEmpty());
        assertTrue(thresholds.size() == 2);
        final Iterator<Threshold> iterator = thresholds.iterator();
        final Threshold t1 = iterator.next();
        final Threshold t2 = iterator.next();

        assertTrue(2000d - t1.value < .0001);
        assertEquals("000011", t1.color);
        assertTrue(5000d - t2.value < .0001);
        assertEquals("001111", t2.color);
    }

    @Test
    @Order(9)
    // *************************************
    // Use Case 9: Login as TestInstAdmin and create a SEB Client Configuration
    // - create one with and one without password
    // - activate one config
    // - export both configurations
    public void testUsecase09_CreateClientConfig() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "TestInstAdmin",
                "987654321",
                new GetClientConfig(),
                new GetClientConfigPage(),
                new NewClientConfig(),
                new SaveClientConfig(),
                new ActivateClientConfig(),
                new DeactivateClientConfig(),
                new ExportClientConfig());

        // create SEB Client Config without password protection
        final Result<SEBClientConfig> newConfigResponse = restService
                .getBuilder(NewClientConfig.class)
                .withFormParam(Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME, "No Password Protection")
                .withFormParam(SEBClientConfig.ATTR_FALLBACK, Constants.TRUE_STRING)
                .withFormParam(SEBClientConfig.ATTR_FALLBACK_START_URL, "http://fallback.com/fallback")
                .withFormParam(SEBClientConfig.ATTR_FALLBACK_TIMEOUT, "100")
                .withFormParam(SEBClientConfig.ATTR_FALLBACK_ATTEMPTS, "5")
                .withFormParam(SEBClientConfig.ATTR_FALLBACK_ATTEMPT_INTERVAL, "5")
                .withFormParam(SEBClientConfig.ATTR_CONFIG_PURPOSE, SEBClientConfig.ConfigPurpose.START_EXAM.name())
                .call();

        assertNotNull(newConfigResponse);
        assertFalse(newConfigResponse.hasError());
        final SEBClientConfig sebClientConfig = newConfigResponse.get();
        assertEquals("No Password Protection", sebClientConfig.name);
        assertFalse(sebClientConfig.isActive());
        assertEquals("http://fallback.com/fallback", sebClientConfig.fallbackStartURL);

        // activate the new Client Configuration
        final Result<EntityProcessingReport> activationResponse = restService
                .getBuilder(ActivateClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, sebClientConfig.getModelId())
                .call();

        assertNotNull(activationResponse);
        assertFalse(activationResponse.hasError());

        final Result<SEBClientConfig> getConfigResponse = restService
                .getBuilder(GetClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, sebClientConfig.getModelId())
                .call();

        assertNotNull(getConfigResponse);
        assertFalse(getConfigResponse.hasError());
        final SEBClientConfig activeConfig = getConfigResponse.get();
        assertTrue(activeConfig.isActive());

        // create a config with password protection
        final Result<SEBClientConfig> configWithPasswordResponse = restService
                .getBuilder(NewClientConfig.class)
                .withFormParam(Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME, "With Password Protection")
                .withFormParam(SEBClientConfig.ATTR_CONFIG_PURPOSE, SEBClientConfig.ConfigPurpose.START_EXAM.name())
                .withFormParam(SEBClientConfig.ATTR_FALLBACK, Constants.TRUE_STRING)
                .withFormParam(SEBClientConfig.ATTR_FALLBACK_START_URL, "http://fallback.com/fallback")
                .withFormParam(SEBClientConfig.ATTR_FALLBACK_TIMEOUT, "100")
                .withFormParam(SEBClientConfig.ATTR_FALLBACK_ATTEMPTS, "5")
                .withFormParam(SEBClientConfig.ATTR_FALLBACK_ATTEMPT_INTERVAL, "5")
                .withFormParam(SEB_CLIENT_CONFIGURATION.ATTR_ENCRYPT_SECRET, "123")
                .withFormParam(SEBClientConfig.ATTR_ENCRYPT_SECRET_CONFIRM, "123")
                .call();

        assertNotNull(configWithPasswordResponse);
        assertFalse(configWithPasswordResponse.hasError());
        final SEBClientConfig configWithPassword = configWithPasswordResponse.get();
        assertEquals("With Password Protection", configWithPassword.name);
        assertFalse(configWithPassword.isActive());
        assertEquals("http://fallback.com/fallback", configWithPassword.fallbackStartURL);

        // export client config No Password Protection
        Result<Boolean> exportResponse = restService
                .getBuilder(ExportClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, sebClientConfig.getModelId())
                .withResponseExtractor(response -> {
                    final InputStream input = response.getBody();
                    final List<String> readLines = IOUtils.readLines(input, "UTF-8");
                    assertNotNull(readLines);
                    assertFalse(readLines.isEmpty());
                    return true;
                })
                .call();
        assertNotNull(exportResponse);
        assertFalse(exportResponse.hasError());

        // export client config With Password Protection
        exportResponse = restService
                .getBuilder(ExportClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, configWithPassword.getModelId())
                .withResponseExtractor(response -> {
                    final InputStream input = response.getBody();
                    final List<String> readLines = IOUtils.readLines(input, "UTF-8");
                    assertNotNull(readLines);
                    assertFalse(readLines.isEmpty());
                    return true;
                })
                .call();

        assertNotNull(exportResponse);
        assertFalse(exportResponse.hasError());

        // get page
        final Result<Page<SEBClientConfig>> pageResponse = restService
                .getBuilder(GetClientConfigPage.class)
                .call();

        assertNotNull(pageResponse);
        assertFalse(pageResponse.hasError());
        final Page<SEBClientConfig> page = pageResponse.get();
        assertFalse(page.content.isEmpty());
        assertTrue(page.content.size() == 2);
    }

    @Test
    @Order(10)
    // *************************************
    // Use Case 10: Login as examAdmin2 and test Exam Configuration data basis
    // - get all Views for the default template
    // - get all Attributes and and Orientations for the default view
    @Sql(scripts = { "classpath:data-test-additional.sql" })
    public void testUsecase10_TestExamConfigBaseData() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetViewList(),
                new GetConfigAttributes(),
                new GetOrientations(),
                new GetOrientationPage());

        final ExamConfigurationServiceImpl examConfigurationService = new ExamConfigurationServiceImpl(
                restService,
                new JSONMapper(),
                null, null,
                Collections.emptyList());

        final Result<AttributeMapping> attributes = examConfigurationService.getAttributes(0l);
        assertNotNull(attributes);
        assertFalse(attributes.hasError());
        final AttributeMapping attributeMapping = attributes.get();
        assertEquals(194, attributeMapping.attributeIdMapping.size());
        assertEquals(
                "[active, audio, backToStart, browserSecurity, browserViewMode, exitSequence, functionKeys, kioskMode, logging, "
                        + "macSettings, newBrowserWindow, newwinsize, proxies, quitLink, registry, servicePolicy, sessionHandling, "
                        + "specialKeys, spellcheck, taskbar, urlFilter, userAgentDesktop, userAgentMac, userAgentTouch, winsize, wintoolbar, "
                        + "zoom, zoomMode]",
                attributeMapping.attributeGroupMapping.keySet()
                        .stream()
                        .sorted()
                        .collect(Collectors.toList())
                        .toString());

        final String viewIds = StringUtils.join(attributeMapping.getViewIds().stream().map(String::valueOf)
                .collect(Collectors.toList()),
                Constants.LIST_SEPARATOR_CHAR);

        assertEquals("1,2,3,4,5,6,8,9,10,11", viewIds);
        final Result<List<View>> viewsResponse = restService
                .getBuilder(GetViewList.class)
                .withQueryParam(API.PARAM_MODEL_ID_LIST, viewIds)
                .call();

        assertNotNull(viewsResponse);
        assertFalse(viewsResponse.hasError());
    }

    @Test
    @Order(11)
    // *************************************
    // Use Case 11: Login as examAdmin2 and create a new SEB Exam Configuration
    // - test creation
    // - save configuration in history
    // - change some attribute
    // - process an undo
    public void testUsecase11_CreateExamConfig() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new NewExamConfig(),
                new GetExamConfigNode(),
                new GetExamConfigNodePage(),
                new GetConfigurationPage(),
                new GetConfigurations(),
                new SaveExamConfigHistory(),
                new GetConfigurationTableValues(),
                new SEBExamConfigUndo(),
                new SaveExamConfigValue(),
                new SaveExamConfigTableValues(),
                new GetConfigurationValuePage(),
                new GetConfigurationValues(),
                new GetConfigAttributes(),
                new GetUserAccountNames());

        // get user id
        final String userId = restService
                .getBuilder(GetUserAccountNames.class)
                .call()
                .getOrThrow()
                .stream()
                .filter(userName -> userName.name != null && userName.name.startsWith("examAdmin2"))
                .map(EntityName::getModelId)
                .findFirst()
                .orElse(null);

        assertNotNull(userId);

        // get configuration page
        final Result<Page<ConfigurationNode>> pageResponse = restService
                .getBuilder(GetExamConfigNodePage.class)
                .call();

        // there should be not configuration (for this institution of examAdmin2) now
        assertNotNull(pageResponse);
        assertFalse(pageResponse.hasError());
        final Page<ConfigurationNode> page = pageResponse.get();
        assertTrue(page.content.isEmpty());

        final Result<ConfigurationNode> newConfigResponse = restService
                .getBuilder(NewExamConfig.class)
                .withFormParam(Domain.CONFIGURATION_NODE.ATTR_NAME, "New Exam Config")
                .withFormParam(Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION, "This is a New Exam Config")
                .call();

        assertNotNull(newConfigResponse);
        assertFalse(newConfigResponse.hasError());
        final ConfigurationNode newConfig = newConfigResponse.get();
        assertEquals("New Exam Config", newConfig.name);
        assertEquals(Long.valueOf(0), newConfig.templateId);
        assertEquals(userId, newConfig.owner);

        // get follow-up configuration
        Result<List<Configuration>> configHistoryResponse = restService
                .getBuilder(GetConfigurations.class)
                .withQueryParam(Configuration.FILTER_ATTR_CONFIGURATION_NODE_ID, newConfig.getModelId())
                .call();

        assertNotNull(configHistoryResponse);
        assertFalse(configHistoryResponse.hasError());
        List<Configuration> configHistory = configHistoryResponse.get();
        assertFalse(configHistory.isEmpty());
        assertTrue(2 == configHistory.size());
        final Configuration initConfig = configHistory.get(0);
        Configuration followup = configHistory.get(1);
        assertEquals("v0", initConfig.version);
        assertFalse(initConfig.followup);
        assertNull(followup.version);
        assertTrue(followup.followup);

        // get all configuration values
        Result<List<ConfigurationValue>> valuesResponse = restService
                .getBuilder(GetConfigurationValues.class)
                .withQueryParam(ConfigurationValue.FILTER_ATTR_CONFIGURATION_ID, followup.getModelId())
                .call();

        assertNotNull(valuesResponse);
        assertFalse(valuesResponse.hasError());
        List<ConfigurationValue> values = valuesResponse.get();
        assertFalse(values.isEmpty());

        UsecaseTestUtils.testProhibitedProcessesInit(
                followup.getModelId(),
                restService);
        UsecaseTestUtils.testPermittedProcessesInit(
                followup.getModelId(),
                restService);

        // update a value -- grab first
        ConfigurationValue value = values.get(0);
        if (value.attributeId == 1) {
            value = values.get(1);
        }
        ConfigurationValue newValue = new ConfigurationValue(
                null, value.institutionId, value.configurationId,
                value.attributeId, value.listIndex, "2");
        Result<ConfigurationValue> newValueResponse = restService
                .getBuilder(SaveExamConfigValue.class)
                .withBody(newValue)
                .call();

        assertNotNull(newValueResponse);
        assertFalse(newValueResponse.hasError());
        ConfigurationValue savedValue = newValueResponse.get();
        assertEquals("2", savedValue.value);

        // save to history
        final Result<Configuration> saveHistoryResponse = restService
                .getBuilder(SaveExamConfigHistory.class)
                .withURIVariable(API.PARAM_MODEL_ID, followup.getModelId())
                .call();

        assertNotNull(saveHistoryResponse);
        assertFalse(saveHistoryResponse.hasError());
        Configuration configuration = saveHistoryResponse.get();
        assertTrue(configuration.followup);

        configHistoryResponse = restService
                .getBuilder(GetConfigurations.class)
                .withQueryParam(Configuration.FILTER_ATTR_CONFIGURATION_NODE_ID, newConfig.getModelId())
                .call();

        assertNotNull(configHistoryResponse);
        assertFalse(configHistoryResponse.hasError());
        configHistory = configHistoryResponse.get();
        assertFalse(configHistory.isEmpty());
        assertTrue(3 == configHistory.size());

        configHistoryResponse = restService
                .getBuilder(GetConfigurations.class)
                .withQueryParam(Configuration.FILTER_ATTR_CONFIGURATION_NODE_ID, newConfig.getModelId())
                .withQueryParam(Configuration.FILTER_ATTR_FOLLOWUP, "true")
                .call();

        assertNotNull(configHistoryResponse);
        assertFalse(configHistoryResponse.hasError());
        followup = configHistoryResponse.get().get(0);
        assertNotNull(followup);
        assertTrue(followup.followup);

        // change value again
        newValue = new ConfigurationValue(
                null, value.institutionId, followup.id,
                value.attributeId, value.listIndex, "3");
        newValueResponse = restService
                .getBuilder(SaveExamConfigValue.class)
                .withBody(newValue)
                .call();

        assertNotNull(newValueResponse);
        assertFalse(newValueResponse.hasError());
        savedValue = newValueResponse.get();
        assertEquals("3", savedValue.value);

        // get current value
        valuesResponse = restService
                .getBuilder(GetConfigurationValues.class)
                .withQueryParam(ConfigurationValue.FILTER_ATTR_CONFIGURATION_ID, followup.getModelId())
                .call();

        assertNotNull(valuesResponse);
        assertFalse(valuesResponse.hasError());
        values = valuesResponse.get();
        assertFalse(values.isEmpty());
        assertNotNull(newValueResponse);
        assertFalse(newValueResponse.hasError());
        savedValue = newValueResponse.get();
        assertEquals("3", savedValue.value);

        // undo
        final Result<Configuration> undoResponse = restService
                .getBuilder(SEBExamConfigUndo.class)
                .withURIVariable(API.PARAM_MODEL_ID, followup.getModelId())
                .call();

        assertNotNull(undoResponse);
        assertFalse(undoResponse.hasError());
        configuration = undoResponse.get();
        assertTrue(configuration.followup);

        // check value has been reset
        valuesResponse = restService
                .getBuilder(GetConfigurationValues.class)
                .withQueryParam(ConfigurationValue.FILTER_ATTR_CONFIGURATION_ID, configuration.getModelId())
                .call();

        assertNotNull(valuesResponse);
        assertFalse(valuesResponse.hasError());
        values = valuesResponse.get();
        final ConfigurationValue _value = value;
        final ConfigurationValue currentValue =
                values.stream().filter(v -> v.attributeId == _value.attributeId).findFirst().orElse(null);
        assertNotNull(currentValue);
        assertEquals("2", currentValue.value);
    }

    @Test
    @Order(12)
    // *************************************
    // Use Case 12: Login as examAdmin2 and get newly created exam configuration
    // - get permitted processes table values
    // - modify permitted processes table values
    // - save permitted processes table values
    // - check save OK
    public void testUsecase12_TestInitDataOfNewExamConfig() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetConfigAttributes(),
                new GetExamConfigNodePage(),
                new GetConfigurations(),
                new GetConfigurationPage(),
                new SaveExamConfigHistory(),
                new GetConfigurationTableValues(),
                new SaveExamConfigTableValues());

        // get configuration page
        final Result<Page<ConfigurationNode>> pageResponse = restService
                .getBuilder(GetExamConfigNodePage.class)
                .call();

        assertNotNull(pageResponse);
        assertFalse(pageResponse.hasError());
        final Page<ConfigurationNode> page = pageResponse.get();
        assertFalse(page.content.isEmpty());

        final ConfigurationNode configurationNode = page.content.get(0);
        assertEquals("New Exam Config", configurationNode.name);

        // get follow-up configuration
        final Result<List<Configuration>> configHistoryResponse = restService
                .getBuilder(GetConfigurations.class)
                .withQueryParam(Configuration.FILTER_ATTR_CONFIGURATION_NODE_ID, configurationNode.getModelId())
                .call();

        final List<Configuration> configHistory = configHistoryResponse.get();
        final Configuration followup = configHistory
                .stream()
                .filter(config -> BooleanUtils.isTrue(config.followup))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Followup Node not found"));

        final ConfigurationTableValues permittedProcessValues = UsecaseTestUtils.getTableValues(
                "73",
                followup.getModelId(),
                restService);

        assertNotNull(permittedProcessValues);
        assertFalse(permittedProcessValues.values.isEmpty());

        // get all configuration attributes
        final Map<Long, ConfigurationAttribute> attributes = restService
                .getBuilder(GetConfigAttributes.class)
                .call()
                .getOrThrow()
                .stream()
                .collect(Collectors.toMap(attr -> attr.id, Function.identity()));

        // create new row by copy the values
        final List<TableValue> newTableValues = permittedProcessValues.values
                .stream()
                .map(attr -> new TableValue(attr.attributeId, 1, attributes.get(attr.attributeId).defaultValue))
                .collect(Collectors.toList());
        newTableValues.addAll(permittedProcessValues.values);

        // test institutional integrity violation
        try {
            final ConfigurationTableValues newTableValue = new ConfigurationTableValues(
                    1000L,
                    followup.id,
                    73L,
                    newTableValues);

            restService.getBuilder(SaveExamConfigTableValues.class)
                    .withBody(newTableValue)
                    .call()
                    .getOrThrow();

            fail("Exception expected here");
        } catch (final Exception e) {
            assertEquals("Unexpected error while rest call", e.getMessage());
        }

        // test follow-up integrity violation
        try {
            final ConfigurationTableValues newTableValue = new ConfigurationTableValues(
                    configHistory.get(0).id,
                    followup.id,
                    73L,
                    newTableValues);

            restService.getBuilder(SaveExamConfigTableValues.class)
                    .withBody(newTableValue)
                    .call()
                    .getOrThrow();

            fail("Exception expected here");
        } catch (final Exception e) {
            assertEquals("Unexpected error while rest call", e.getMessage());
        }

        final ConfigurationTableValues newTableValue = new ConfigurationTableValues(
                followup.institutionId,
                followup.id,
                73L,
                newTableValues);

        final ConfigurationTableValues savedValues = restService.getBuilder(SaveExamConfigTableValues.class)
                .withBody(newTableValue)
                .call()
                .getOrThrow();

        assertNotNull(savedValues);
        assertFalse(savedValues.values.isEmpty());
        assertTrue(savedValues.values.size() == newTableValues.size());
    }

    @Test
    @Order(13)
    // *************************************
    // Use Case 13: Login as examAdmin2 and use newly created configuration
    // - get follow-up configuration by API
    // - import
    // - export
    public void testUsecase13_ExamConfigImportExport() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetConfigAttributes(),
                new GetConfigurationValues(),
                new GetConfigurationValuePage(),
                new GetConfigurationTableValues(),
                new GetExamConfigNodePage(),
                new SaveExamConfigHistory(),
                new ExportSEBSettingsConfig(),
                new ImportNewExamConfig(),
                new ImportExamConfigOnExistingConfig(),
                new GetFollowupConfiguration());

        // get all configuration attributes
        final Collection<ConfigurationAttribute> attributes = new ArrayList<>(restService
                .getBuilder(GetConfigAttributes.class)
                .call()
                .getOrThrow());

        // get configuration page
        final Result<Page<ConfigurationNode>> pageResponse = restService
                .getBuilder(GetExamConfigNodePage.class)
                .call();

        assertNotNull(pageResponse);
        assertFalse(pageResponse.hasError());
        final Page<ConfigurationNode> page = pageResponse.get();
        assertFalse(page.content.isEmpty());

        final ConfigurationNode configurationNode = page.content.get(0);
        assertEquals("New Exam Config", configurationNode.name);

        final Configuration followup = restService
                .getBuilder(GetFollowupConfiguration.class)
                .withURIVariable(API.PARAM_MODEL_ID, configurationNode.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(followup);
        assertTrue(followup.followup);

        // export1
        restService
                .getBuilder(ExportSEBSettingsConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, configurationNode.getModelId())
                .withResponseExtractor(response -> {
                    final InputStream input = response.getBody();
                    final String xmlString = StreamUtils.copyToString(input, java.nio.charset.StandardCharsets.UTF_8);
                    assertNotNull(xmlString);
                    for (final ConfigurationAttribute attribute : attributes) {
                        if (attribute.name.contains(".") || attribute.name.equals("kioskMode")) {
                            continue;
                        }
                        if (!xmlString.contains(attribute.name)) {
                            fail("missing attribute: " + attribute.name);
                        }
                    }
                    return true;
                })
                .call()
                .getOrThrow();

        // import plain config
        InputStream inputStream = new ClassPathResource("importTest.seb").getInputStream();
        Configuration importedConfig = restService
                .getBuilder(ImportNewExamConfig.class)
                .withBody(inputStream)
                .withHeader(Domain.CONFIGURATION_NODE.ATTR_NAME, "Imported Test Configuration")
                .call()
                .getOrThrow();

        assertNotNull(importedConfig);

        // Check imported value
        final Configuration fallow_up = restService.getBuilder(GetFollowupConfiguration.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(importedConfig.configurationNodeId))
                .call()
                .getOrThrow();
        assertNotNull(fallow_up);

        final List<ConfigurationValue> values = restService.getBuilder(GetConfigurationValues.class)
                .withQueryParam(
                        ConfigurationValue.FILTER_ATTR_CONFIGURATION_ID,
                        String.valueOf(fallow_up.id))
                .call()
                .getOrThrow();

        assertNotNull(values);
        attributes
                .stream()
                .filter(attr -> "URLFilterEnable".equals(attr.name))
                .findFirst()
                .ifPresent(
                        attr -> {
                            values.stream()
                                    .filter(cv -> cv.attributeId.equals(attr.id))
                                    .findFirst()
                                    .ifPresent(
                                            val -> assertEquals(Constants.TRUE_STRING, val.value));
                        });

        attributes
                .stream()
                .filter(attr -> "URLFilterRules".equals(attr.name))
                .findFirst()
                .ifPresent(
                        parent -> {
                            attributes.stream()
                                    .filter(attr -> parent.id.equals(attr.parentId)
                                            && "URLFilterRules.expression".equals(attr.name))
                                    .findFirst()
                                    .ifPresent(
                                            tAttr -> {
                                                values.stream()
                                                        .filter(tVal -> tVal.attributeId.equals(tAttr.id)
                                                                && tVal.listIndex == 0)
                                                        .findFirst()
                                                        .ifPresent(
                                                                firstTVal -> assertEquals("jrtjrtzj", firstTVal.value));
                                            });

                        });

        // import with the same name should cause an exception
        try {
            restService
                    .getBuilder(ImportNewExamConfig.class)
                    .withBody(inputStream)
                    .withHeader(Domain.CONFIGURATION_NODE.ATTR_NAME, "Imported Test Configuration")
                    .withHeader(API.IMPORT_PASSWORD_ATTR_NAME, "123")
                    .call()
                    .getOrThrow();
            fail("Expecting an exception here");
        } catch (final Exception e) {

        }

        // import encrypted config with password encryption
        inputStream = new ClassPathResource("importTest_123.seb").getInputStream();
        importedConfig = restService
                .getBuilder(ImportNewExamConfig.class)
                .withBody(inputStream)
                .withHeader(Domain.CONFIGURATION_NODE.ATTR_NAME, "Imported Encrypted Test Configuration")
                .withHeader(API.IMPORT_PASSWORD_ATTR_NAME, "123")
                .call()
                .getOrThrow();

        assertNotNull(importedConfig);

        // import config within existing configuration
        inputStream = new ClassPathResource("importTest.seb").getInputStream();
        importedConfig = restService
                .getBuilder(ImportExamConfigOnExistingConfig.class)
                .withBody(inputStream)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(importedConfig.getConfigurationNodeId()))
                .call()
                .getOrThrow();
    }

    @Test
    @Order(14)
    // *************************************
    // Use Case 14: Login as examAdmin2 and use newly created configuration
    // - change configuration status to "Ready to Use"
    public void testUsecase14_EditExamConfig() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetConfigAttributes(),
                new GetExamConfigNodePage(),
                new SaveExamConfig());

        // get configuration from page
        final ConfigurationNode config = restService
                .getBuilder(GetExamConfigNodePage.class)
                .call()
                .getOrThrow().content
                        .get(0);
        assertEquals("New Exam Config", config.name);

        final ConfigurationNode newConfig = new ConfigurationNode(
                config.id,
                config.institutionId,
                config.templateId,
                config.name,
                config.description,
                ConfigurationType.EXAM_CONFIG,
                config.owner,
                ConfigurationStatus.READY_TO_USE);

        final ConfigurationNode savedConfig = restService
                .getBuilder(SaveExamConfig.class)
                .withBody(newConfig)
                .call()
                .getOrThrow();

        assertTrue(savedConfig.status == ConfigurationStatus.READY_TO_USE);
    }

    @Test
    @Order(15)
    // *************************************
    // Use Case 15: Login as examAdmin2 and get views and orientations
    // - Test Views API
    // - Create configuration template form existing configuration
    // - Check views and orientation created for template
    // - Remove one template attribute from orientation
    // - Change one template attribute value
    // - Reset template values
    public void testUsecase15_CreateConfigurationTemplate() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetViews(),
                new GetViewPage(),
                new GetOrientationPage(),
                new GetOrientations(),
                new CopyConfiguration(),
                new GetTemplateAttributePage(),
                new GetExamConfigNodePage(),
                new GetTemplateAttribute(),
                new GetConfigurationValues(),
                new GetConfigurationValuePage(),
                new SaveExamConfigValue(),
                new GetFollowupConfiguration(),
                new RemoveOrientation(),
                new AttachDefaultOrientation(),
                new ResetTemplateValues());

        final List<View> views = restService
                .getBuilder(GetViews.class)
                .call()
                .getOrThrow();

        assertNotNull(views);
        assertEquals(11, views.size());
        views.forEach(v -> assertEquals(v.templateId, ConfigurationNode.DEFAULT_TEMPLATE_ID));

        final List<Orientation> orientations = restService
                .getBuilder(GetOrientations.class)
                .call()
                .getOrThrow();
        assertNotNull(orientations);
        orientations.forEach(o -> assertEquals(o.templateId, ConfigurationNode.DEFAULT_TEMPLATE_ID));

        // get configuration page and first config from the page to copy as template
        final Result<Page<ConfigurationNode>> pageResponse = restService
                .getBuilder(GetExamConfigNodePage.class)
                .call();

        assertNotNull(pageResponse);
        assertFalse(pageResponse.hasError());
        final Page<ConfigurationNode> page = pageResponse.get();
        assertFalse(page.content.isEmpty());

        final ConfigurationNode configurationNode = page.content.get(0);
        assertEquals("New Exam Config", configurationNode.name);

        final ConfigCreationInfo copyInfo = new ConfigCreationInfo(
                configurationNode.id,
                "Config Template",
                "Test Config Template creation",
                false,
                ConfigurationType.TEMPLATE);

        final ConfigurationNode template = restService
                .getBuilder(CopyConfiguration.class)
                .withBody(copyInfo)
                .call()
                .getOrThrow();
        assertNotNull(template);
        // get template page and check new template is available
        final Page<ConfigurationNode> templates = restService
                .getBuilder(GetExamConfigNodePage.class)
                .withQueryParam(ConfigurationNode.FILTER_ATTR_TYPE, ConfigurationType.TEMPLATE.name())
                .call()
                .getOrThrow();
        assertNotNull(templates);
        assertFalse(templates.isEmpty());
        final ConfigurationNode newTemplate = templates.content.get(0);
        assertNotNull(newTemplate);
        assertEquals("Config Template", newTemplate.name);

        // check views for template where created
        final List<View> templateViews = restService
                .getBuilder(GetViews.class)
                .withQueryParam(View.FILTER_ATTR_TEMPLATE, String.valueOf(template.id))
                .call()
                .getOrThrow();

        assertNotNull(templateViews);
        assertFalse(templateViews.isEmpty());
        assertEquals(11, templateViews.size());

        // check orientations for template where created
        final List<Orientation> templateTrientations = restService
                .getBuilder(GetOrientations.class)
                .withQueryParam(Orientation.FILTER_ATTR_TEMPLATE_ID, String.valueOf(template.id))
                .call()
                .getOrThrow();
        assertNotNull(templateTrientations);
        assertFalse(templateTrientations.isEmpty());
        assertEquals(194, templateTrientations.size());

        // get template attributes page
        final Page<TemplateAttribute> templateAttributes = restService
                .getBuilder(GetTemplateAttributePage.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, String.valueOf(template.id))
                .call()
                .getOrThrow();

        assertNotNull(templateAttributes);
        assertFalse(templateAttributes.isEmpty());
        final TemplateAttribute templateAttr = templateAttributes.content.get(0);
        assertEquals(template.id, templateAttr.templateId);
        final Orientation orientation = templateAttr.getOrientation();
        assertNotNull(orientation);

        TemplateAttribute savedTAttribute = restService
                .getBuilder(RemoveOrientation.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, String.valueOf(template.id))
                .withURIVariable(API.PARAM_MODEL_ID, templateAttr.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(savedTAttribute);
        assertNull(savedTAttribute.getOrientation());

        // Re-attach default orientation
        savedTAttribute = restService
                .getBuilder(AttachDefaultOrientation.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, String.valueOf(template.id))
                .withURIVariable(API.PARAM_MODEL_ID, templateAttr.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(savedTAttribute);
        assertNotNull(savedTAttribute.getOrientation());
        assertEquals(orientation.viewId, savedTAttribute.getOrientation().viewId);
        assertEquals(orientation.templateId, savedTAttribute.getOrientation().templateId);
        assertEquals(orientation.attributeId, savedTAttribute.getOrientation().attributeId);

        // get first value and change it
        final Configuration fallow_up = restService.getBuilder(GetFollowupConfiguration.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(template.id))
                .call()
                .getOrThrow();
        assertNotNull(fallow_up);

        final List<ConfigurationValue> values = restService
                .getBuilder(GetConfigurationValues.class)
                .withQueryParam(ConfigurationValue.FILTER_ATTR_CONFIGURATION_ID, String.valueOf(fallow_up.id))
                .withQueryParam(ConfigurationValue.FILTER_ATTR_CONFIGURATION_ATTRIBUTE_ID, savedTAttribute.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(values);
        assertTrue(values.size() == 1);
        final ConfigurationValue templateValue = values.get(0);
        assertNull(templateValue.value);

        final ConfigurationValue newValue = new ConfigurationValue(
                templateValue.id, templateValue.institutionId, savedTAttribute.getTemplateId(),
                templateValue.attributeId, 0, "123");
        final ConfigurationValue newTemplValue = restService
                .getBuilder(SaveExamConfigValue.class)
                .withBody(newValue)
                .call()
                .getOrThrow();
        assertNotNull(newTemplValue);
        assertEquals("123", newTemplValue.value);

        // reset template values
        final TemplateAttribute attribute = restService
                .getBuilder(ResetTemplateValues.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, String.valueOf(template.id))
                .withURIVariable(API.PARAM_MODEL_ID, templateAttr.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(attribute);
        assertEquals("hashedAdminPassword", attribute.getConfigAttribute().name);

        restService
                .getBuilder(GetConfigurationValues.class)
                .withQueryParam(ConfigurationValue.FILTER_ATTR_CONFIGURATION_ID, String.valueOf(fallow_up.id))
                .withQueryParam(ConfigurationValue.FILTER_ATTR_CONFIGURATION_ATTRIBUTE_ID, savedTAttribute.getModelId())
                .call()
                .getOrThrow()
                .stream()
                .filter(cValue -> cValue.attributeId.equals(attribute.getConfigAttribute().id))
                .findFirst()
                .ifPresent(cValue -> assertNull(cValue.value));
    }

    @Test
    @Order(16)
    // *************************************
    // Use Case 16: Login as examAdmin2 and map a Exam Config to an Exam
    // - Get Exam
    // - Get List of available Exam Config for mapping
    // - Map a Exam Config to the Exam
    // - Remove Exam Config
    // - Add config again
    // - Export Config Key
    // - Export Config as XML
    public void testUsecase16_MapExamConfigToExam() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetExamPage(),
                new GetExamConfigNode(),
                new GetExamConfigNodeNames(),
                new GetExamConfigMappingNames(),
                new GetExamConfigMappingsPage(),
                new SaveExamConfigMapping(),
                new NewExamConfigMapping(),
                new CheckExamConsistency(),
                new DeleteExamConfigMapping(),
                new ExportConfigKey(),
                new ExportSEBSettingsConfig());

        // get exam
        final Result<Page<Exam>> exams = restService
                .getBuilder(GetExamPage.class)
                .call();

        assertNotNull(exams);
        assertFalse(exams.hasError());
        final Page<Exam> examPage = exams.get();
        assertFalse(examPage.isEmpty());
        final Exam exam = examPage.content.get(0);
        assertEquals("Demo Quiz 1 (MOCKUP)", exam.name);
        // check that the exam is running
        assertNull(exam.endTime);
        // check that the exam is marked with missing configuration alert
        final Collection<APIMessage> alerts = restService.getBuilder(CheckExamConsistency.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call()
                .getOr(Collections.emptyList());
        assertNotNull(alerts);
        assertFalse(alerts.isEmpty());
        final APIMessage message = alerts.iterator().next();
        assertNotNull(message);
        assertEquals("No SEB Exam Configuration defined for the Exam", message.systemMessage);

        // get available exam configs for mapping
        final Result<List<EntityName>> configs = restService.getBuilder(GetExamConfigNodeNames.class)
                .withQueryParam(
                        ConfigurationNode.FILTER_ATTR_TYPE,
                        ConfigurationType.EXAM_CONFIG.name())
                .withQueryParam(
                        ConfigurationNode.FILTER_ATTR_STATUS,
                        ConfigurationStatus.READY_TO_USE.name())
                .call();

        assertNotNull(configs);
        assertFalse(configs.hasError());
        final List<EntityName> list = configs.get();
        assertFalse(list.isEmpty());
        final EntityName configName = list.get(0);
        assertEquals("New Exam Config", configName.name);

        // get config mapping page and check there is no mapping yet
        final Result<Page<ExamConfigurationMap>> mappings = restService
                .getBuilder(GetExamConfigMappingsPage.class)
                .call();

        assertNotNull(mappings);
        assertFalse(mappings.hasError());
        final Page<ExamConfigurationMap> mappingsPage = mappings.get();
        assertTrue(mappingsPage.isEmpty());

        // create new config node mapping
        Result<ExamConfigurationMap> newExamConfigMap = restService.getBuilder(NewExamConfigMapping.class)
                .withFormParam(Domain.EXAM_CONFIGURATION_MAP.ATTR_INSTITUTION_ID, String.valueOf(exam.institutionId))
                .withFormParam(Domain.EXAM_CONFIGURATION_MAP.ATTR_EXAM_ID, String.valueOf(exam.id))
                .withFormParam(Domain.EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID, configName.modelId)
                .call();

        assertNotNull(newExamConfigMap);
        assertFalse(newExamConfigMap.hasError());
        ExamConfigurationMap examConfigurationMap = newExamConfigMap.get();
        assertNotNull(examConfigurationMap);
        assertEquals("New Exam Config", examConfigurationMap.configName);
        assertEquals(exam.name, examConfigurationMap.examName);

        final Result<Page<ExamConfigurationMap>> newMappings = restService
                .getBuilder(GetExamConfigMappingsPage.class)
                .call();

        assertNotNull(newMappings);
        assertFalse(newMappings.hasError());
        final Page<ExamConfigurationMap> newMappingsPage = newMappings.get();
        assertFalse(newMappingsPage.isEmpty());
        final ExamConfigurationMap newMapping = newMappingsPage.content.get(0);
        assertNotNull(newMapping);
        assertEquals("New Exam Config", newMapping.configName);
        assertEquals(exam.name, newMapping.examName);

        // check that the exam is not marked with missing configuration alert anymore
        final Collection<APIMessage> newAlerts = restService.getBuilder(CheckExamConsistency.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call()
                .getOr(Collections.emptyList());
        assertNotNull(newAlerts);
        assertTrue(newAlerts.isEmpty());

        // check the state of exam config is now in "In Use"
        Result<ConfigurationNode> examConfigCall = restService.getBuilder(GetExamConfigNode.class)
                .withURIVariable(API.PARAM_MODEL_ID, configName.modelId)
                .call();

        assertNotNull(examConfigCall);
        assertFalse(examConfigCall.hasError());
        ConfigurationNode configurationNode = examConfigCall.get();
        assertEquals(ConfigurationStatus.IN_USE, configurationNode.status);

        // delete the configuration mapping
        restService.getBuilder(DeleteExamConfigMapping.class)
                .withURIVariable(API.PARAM_MODEL_ID, examConfigurationMap.getModelId())
                .call();

        // check the state of exam config is now in "Ready To Use"
        final Result<ConfigurationNode> examConfigCall2 = restService.getBuilder(GetExamConfigNode.class)
                .withURIVariable(API.PARAM_MODEL_ID, configName.modelId)
                .call();

        assertNotNull(examConfigCall2);
        assertFalse(examConfigCall2.hasError());
        final ConfigurationNode configurationNode2 = examConfigCall2.get();
        assertEquals(ConfigurationStatus.READY_TO_USE, configurationNode2.status);

        // Re-Map the configuration to the exam and check again the state.
        newExamConfigMap = restService.getBuilder(NewExamConfigMapping.class)
                .withFormParam(Domain.EXAM_CONFIGURATION_MAP.ATTR_INSTITUTION_ID, String.valueOf(exam.institutionId))
                .withFormParam(Domain.EXAM_CONFIGURATION_MAP.ATTR_EXAM_ID, String.valueOf(exam.id))
                .withFormParam(Domain.EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID, configName.modelId)
                .call();

        assertNotNull(newExamConfigMap);
        assertFalse(newExamConfigMap.hasError());
        examConfigurationMap = newExamConfigMap.get();
        assertNotNull(examConfigurationMap);
        assertEquals("New Exam Config", examConfigurationMap.configName);
        assertEquals(exam.name, examConfigurationMap.examName);

        examConfigCall = restService.getBuilder(GetExamConfigNode.class)
                .withURIVariable(API.PARAM_MODEL_ID, configName.modelId)
                .call();

        assertNotNull(examConfigCall);
        assertFalse(examConfigCall.hasError());
        configurationNode = examConfigCall.get();
        assertEquals(ConfigurationStatus.IN_USE, configurationNode.status);

        // export Config Key
        final ConfigKey configKey = restService.getBuilder(ExportConfigKey.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(examConfigurationMap.configurationNodeId))
                .call()
                .getOrThrow();
        assertNotNull(configKey);
        //assertEquals("e4af6cf8deb9434e69e8dc6c373418712546de35807d8bfbd6bb98790f8d0774", configKey.key);

        // export config to XML
        restService.getBuilder(ExportSEBSettingsConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(examConfigurationMap.configurationNodeId))
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, String.valueOf(examConfigurationMap.examId))
                .withResponseExtractor(response -> {
                    final InputStream input = response.getBody();
                    final String xmlString = StreamUtils.copyToString(input, java.nio.charset.StandardCharsets.UTF_8);
                    assertNotNull(xmlString);
//                  assertEquals(
//                  "<?xml version=\"1.0\" encoding=\"utf-8\"?><!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\"><plist version=\"1.0\"><dict><key>allowAudioCapture</key><false /><key>allowBrowsingBackForward</key><false /><key>allowDictation</key><false /><key>allowDictionaryLookup</key><false /><key>allowDisplayMirroring</key><false /><key>allowDownUploads</key><true /><key>allowedDisplayBuiltin</key><true /><key>allowedDisplaysMaxNumber</key><integer>1</integer><key>allowFlashFullscreen</key><false /><key>allowiOSBetaVersionNumber</key><integer>0</integer><key>allowiOSVersionNumberMajor</key><integer>9</integer><key>allowiOSVersionNumberMinor</key><integer>3</integer><key>allowiOSVersionNumberPatch</key><integer>5</integer><key>allowPDFPlugIn</key><true /><key>allowPreferencesWindow</key><true /><key>allowQuit</key><true /><key>allowScreenSharing</key><false /><key>allowSiri</key><false /><key>allowSpellCheck</key><false /><key>allowSpellCheckDictionary</key><array><string>da-DK</string><string>en-AU</string><string>en-GB</string><string>en-US</string><string>es-ES</string><string>fr-FR</string><string>pt-PT</string><string>sv-SE</string><string>sv-FI</string></array><key>allowSwitchToApplications</key><false /><key>allowUserAppFolderInstall</key><false /><key>allowUserSwitching</key><false /><key>allowVideoCapture</key><false /><key>allowVirtualMachine</key><false /><key>allowWlan</key><false /><key>audioControlEnabled</key><false /><key>audioMute</key><false /><key>audioSetVolumeLevel</key><false /><key>audioVolumeLevel</key><integer>25</integer><key>blacklistURLFilter</key><string /><key>blockPopUpWindows</key><false /><key>browserMessagingPingTime</key><integer>120000</integer><key>browserMessagingSocket</key><string>ws://localhost:8706</string><key>browserScreenKeyboard</key><false /><key>browserURLSalt</key><true /><key>browserUserAgent</key><string /><key>browserUserAgentiOS</key><integer>0</integer><key>browserUserAgentiOSCustom</key><string /><key>browserUserAgentMac</key><integer>0</integer><key>browserUserAgentMacCustom</key><string /><key>browserUserAgentWinDesktopMode</key><integer>0</integer><key>browserUserAgentWinDesktopModeCustom</key><string /><key>browserUserAgentWinTouchMode</key><integer>0</integer><key>browserUserAgentWinTouchModeCustom</key><string /><key>browserUserAgentWinTouchModeIPad</key><string>Mozilla/5.0 (iPad; CPU OS 12_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1.2 Mobile/15E148 Safari/604.1</string><key>browserViewMode</key><integer>0</integer><key>browserWindowAllowReload</key><true /><key>browserWindowShowURL</key><integer>0</integer><key>browserWindowTitleSuffix</key><string /><key>chooseFileToUploadPolicy</key><integer>0</integer><key>createNewDesktop</key><true /><key>detectStoppedProcess</key><true /><key>downloadAndOpenSebConfig</key><true /><key>downloadDirectoryOSX</key><string /><key>downloadDirectoryWin</key><string /><key>downloadPDFFiles</key><true /><key>enableAltEsc</key><false /><key>enableAltF4</key><false /><key>enableAltMouseWheel</key><false /><key>enableAltTab</key><true /><key>enableAppSwitcherCheck</key><true /><key>enableBrowserWindowToolbar</key><false /><key>enableCtrlEsc</key><false /><key>enableDrawingEditor</key><false /><key>enableEsc</key><false /><key>enableF1</key><false /><key>enableF10</key><false /><key>enableF11</key><false /><key>enableF12</key><false /><key>enableF2</key><false /><key>enableF3</key><false /><key>enableF4</key><false /><key>enableF5</key><false /><key>enableF6</key><false /><key>enableF7</key><false /><key>enableF8</key><false /><key>enableF9</key><false /><key>enableJava</key><false /><key>enableJavaScript</key><true /><key>enableLogging</key><false /><key>enablePlugIns</key><true /><key>enablePrintScreen</key><false /><key>enablePrivateClipboard</key><true /><key>enableRightMouse</key><false /><key>enableSebBrowser</key><true /><key>enableStartMenu</key><false /><key>enableTouchExit</key><false /><key>enableZoomPage</key><true /><key>enableZoomText</key><true /><key>examSessionClearCookiesOnEnd</key><true /><key>examSessionClearCookiesOnStart</key><true /><key>exitKey1</key><integer>2</integer><key>exitKey2</key><integer>10</integer><key>exitKey3</key><integer>5</integer><key>forceAppFolderInstall</key><true /><key>hashedAdminPassword</key><string /><key>hashedQuitPassword</key><string /><key>hideBrowserWindowToolbar</key><false /><key>hookKeys</key><true /><key>ignoreExitKeys</key><false /><key>insideSebEnableChangeAPassword</key><false /><key>insideSebEnableEaseOfAccess</key><false /><key>insideSebEnableLockThisComputer</key><false /><key>insideSebEnableLogOff</key><false /><key>insideSebEnableNetworkConnectionSelector</key><false /><key>insideSebEnableShutDown</key><false /><key>insideSebEnableStartTaskManager</key><false /><key>insideSebEnableSwitchUser</key><false /><key>insideSebEnableVmWareClientShade</key><false /><key>killExplorerShell</key><false /><key>lockOnMessageSocketClose</key><false /><key>logDirectoryOSX</key><string /><key>logDirectoryWin</key><string /><key>logLevel</key><integer>1</integer><key>mainBrowserWindowHeight</key><string>100%</string><key>mainBrowserWindowPositioning</key><integer>1</integer><key>mainBrowserWindowWidth</key><string>100%</string><key>minMacOSVersion</key><integer>0</integer><key>mobileAllowPictureInPictureMediaPlayback</key><false /><key>mobileAllowQRCodeConfig</key><false /><key>mobileAllowSingleAppMode</key><false /><key>mobileEnableASAM</key><true /><key>mobileEnableGuidedAccessLinkTransform</key><false /><key>mobilePreventAutoLock</key><true /><key>mobileShowSettings</key><false /><key>mobileStatusBarAppearance</key><integer>1</integer><key>mobileStatusBarAppearanceExtended</key><integer>1</integer><key>monitorProcesses</key><false /><key>newBrowserWindowAllowReload</key><true /><key>newBrowserWindowByLinkBlockForeign</key><false /><key>newBrowserWindowByLinkHeight</key><string>100%</string><key>newBrowserWindowByLinkPolicy</key><integer>2</integer><key>newBrowserWindowByLinkPositioning</key><integer>2</integer><key>newBrowserWindowByLinkWidth</key><string>100%</string><key>newBrowserWindowByScriptBlockForeign</key><false /><key>newBrowserWindowByScriptPolicy</key><integer>2</integer><key>newBrowserWindowNavigation</key><true /><key>newBrowserWindowShowReloadWarning</key><false /><key>newBrowserWindowShowURL</key><integer>1</integer><key>openDownloads</key><false /><key>originatorVersion</key><string>SEB_Server_0.3.0</string><key>permittedProcesses</key><array><dict><key>active</key><true /><key>allowUserToChooseApp</key><false /><key>arguments</key><array /><key>autostart</key><true /><key>description</key><string /><key>executable</key><string>firefox.exe</string><key>iconInTaskbar</key><true /><key>identifier</key><string>Firefox</string><key>originalName</key><string>firefox.exe</string><key>os</key><integer>1</integer><key>path</key><string>../xulrunner/</string><key>runInBackground</key><false /><key>strongKill</key><true /><key>title</key><string>SEB</string></dict></array><key>pinEmbeddedCertificates</key><false /><key>prohibitedProcesses</key><array><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>Riot</string><key>identifier</key><string /><key>originalName</key><string>Riot</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>seamonkey</string><key>identifier</key><string /><key>originalName</key><string>seamonkey</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>Discord</string><key>identifier</key><string /><key>originalName</key><string>Discord</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>Slack</string><key>identifier</key><string /><key>originalName</key><string>Slack</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>Teams</string><key>identifier</key><string /><key>originalName</key><string>Teams</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>CamRecorder</string><key>identifier</key><string /><key>originalName</key><string>CamRecorder</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>join.me</string><key>identifier</key><string /><key>originalName</key><string>join.me</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>RPCSuite</string><key>identifier</key><string /><key>originalName</key><string>RPCSuite</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>RPCService</string><key>identifier</key><string /><key>originalName</key><string>RPCService</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>RemotePCDesktop</string><key>identifier</key><string /><key>originalName</key><string>RemotePCDesktop</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>beamyourscreen-host</string><key>identifier</key><string /><key>originalName</key><string>beamyourscreen-host</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>AeroAdmin</string><key>identifier</key><string /><key>originalName</key><string>AeroAdmin</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>Mikogo-host</string><key>identifier</key><string /><key>originalName</key><string>Mikogo-host</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>chromoting</string><key>identifier</key><string /><key>originalName</key><string>chromoting</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>vncserverui</string><key>identifier</key><string /><key>originalName</key><string>vncserverui</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>vncviewer</string><key>identifier</key><string /><key>originalName</key><string>vncviewer</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>vncserver</string><key>identifier</key><string /><key>originalName</key><string>vncserver</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>TeamViewer</string><key>identifier</key><string /><key>originalName</key><string>TeamViewer</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>GotoMeetingWinStore</string><key>identifier</key><string /><key>originalName</key><string>GotoMeetingWinStore</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>g2mcomm.exe</string><key>identifier</key><string /><key>originalName</key><string>g2mcomm.exe</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>SkypeHost</string><key>identifier</key><string /><key>originalName</key><string>SkypeHost</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict><dict><key>active</key><true /><key>currentUser</key><true /><key>description</key><string /><key>executable</key><string>Skype</string><key>identifier</key><string /><key>originalName</key><string>Skype</string><key>os</key><integer>1</integer><key>strongKill</key><false /><key>user</key><string /></dict></array><key>proxies</key><dict><key>AutoConfigurationEnabled</key><false /><key>AutoConfigurationJavaScript</key><string /><key>AutoConfigurationURL</key><string /><key>AutoDiscoveryEnabled</key><false /><key>ExceptionsList</key><array></array><key>ExcludeSimpleHostnames</key><false /><key>FTPEnable</key><false /><key>FTPPassive</key><true /><key>FTPPassword</key><string /><key>FTPPort</key><integer>21</integer><key>FTPProxy</key><string /><key>FTPRequiresPassword</key><false /><key>FTPUsername</key><string /><key>HTTPEnable</key><false /><key>HTTPPassword</key><string /><key>HTTPPort</key><integer>80</integer><key>HTTPProxy</key><string /><key>HTTPRequiresPassword</key><false /><key>HTTPSEnable</key><false /><key>HTTPSPassword</key><string /><key>HTTPSPort</key><integer>443</integer><key>HTTPSProxy</key><string /><key>HTTPSRequiresPassword</key><false /><key>HTTPSUsername</key><string /><key>HTTPUsername</key><string /><key>RTSPEnable</key><false /><key>RTSPPassword</key><string /><key>RTSPPort</key><integer>554</integer><key>RTSPProxy</key><string /><key>RTSPRequiresPassword</key><false /><key>RTSPUsername</key><string /><key>SOCKSEnable</key><false /><key>SOCKSPassword</key><string /><key>SOCKSPort</key><integer>1080</integer><key>SOCKSProxy</key><string /><key>SOCKSRequiresPassword</key><false /><key>SOCKSUsername</key><string /></dict><key>proxySettingsPolicy</key><integer>0</integer><key>quitURL</key><string /><key>quitURLConfirm</key><true /><key>removeBrowserProfile</key><false /><key>removeLocalStorage</key><false /><key>restartExamPasswordProtected</key><true /><key>restartExamText</key><string /><key>restartExamURL</key><string /><key>restartExamUseStartURL</key><false /><key>sebConfigPurpose</key><integer>0</integer><key>sebServicePolicy</key><integer>2</integer><key>sendBrowserExamKey</key><true /><key>showBackToStartButton</key><true /><key>showInputLanguage</key><false /><key>showMenuBar</key><false /><key>showNavigationButtons</key><false /><key>showReloadButton</key><true /><key>showReloadWarning</key><true /><key>showScanQRCodeButton</key><false /><key>showSettingsInApp</key><false /><key>showTaskBar</key><true /><key>showTime</key><true /><key>startResource</key><string /><key>taskBarHeight</key><integer>40</integer><key>touchOptimized</key><false /><key>URLFilterEnable</key><false /><key>URLFilterEnableContentFilter</key><false /><key>URLFilterMessage</key><integer>0</integer><key>URLFilterRules</key><array /><key>useAsymmetricOnlyEncryption</key><false /><key>whitelistURLFilter</key><string /><key>zoomMode</key><integer>0</integer></dict></plist>",
//                  xmlString);
                    return true;
                })
                .call()
                .getOrThrow();
    }

    @Autowired
    private SEBClientConfigDAO sebClientConfigDAO;

    @Test
    @Order(17)
    // *************************************
    // Use Case 17: Login as examSupport2 and get running exam with data
    // - Get list of running exams
    // - Simulate a SEB connection
    // - Join running exam by get the data for all SEB connections and for a single SEB connection.
    public void testUsecase17_RunningExam() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examSupport2",
                "examSupport2",
                new GetRunningExamPage(),
                new GetClientConnectionDataList(),
                new GetClientConnection(),
                new GetMonitoringFullPageData(),
                new GetExtendedClientEventPage(),
                new DisableClientConnection(),
                new PropagateInstruction(),
                new GetClientConnectionPage(),
                new GetPendingClientNotifications(),
                new ConfirmPendingClientNotification());

        final RestServiceImpl adminRestService = createRestServiceForUser(
                "TestInstAdmin",
                "987654321",
                new NewClientConfig(),
                new ActivateClientConfig(),
                new GetClientConfigPage(),
                new GetIndicatorPage(),
                new GetIndicators());

        // get running exams
        final Result<Page<Exam>> runningExamsCall = restService.getBuilder(GetRunningExamPage.class)
                .call();

        assertNotNull(runningExamsCall);
        assertFalse(runningExamsCall.hasError());
        final Page<Exam> page = runningExamsCall.get();
        assertFalse(page.content.isEmpty());
        final Exam exam = page.content.get(0);
        assertEquals("Demo Quiz 1 (MOCKUP)", exam.name);

        // get SEB connections
        Result<Collection<ClientConnectionData>> connectionsCall =
                restService.getBuilder(GetClientConnectionDataList.class)
                        .withURIVariable(API.PARAM_PARENT_MODEL_ID, exam.getModelId())
                        .call();

        assertNotNull(connectionsCall);
        assertFalse(connectionsCall.hasError());
        Collection<ClientConnectionData> connections = connectionsCall.get();
        // no SEB connections available yet
        assertTrue(connections.isEmpty());

        // get MonitoringFullPageData
        final Result<MonitoringFullPageData> fullPageData = restService.getBuilder(GetMonitoringFullPageData.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, exam.getModelId())
                .call();
        assertNotNull(fullPageData);
        assertFalse(fullPageData.hasError());
        final MonitoringSEBConnectionData monitoringConnectionData = fullPageData.get().monitoringConnectionData;
        assertTrue(monitoringConnectionData.connections.isEmpty());
        assertEquals(
                "[0, 0, 0, 0, 0, 0]",
                Arrays.stream(monitoringConnectionData.connectionsPerStatus).boxed().map(String::valueOf)
                        .collect(Collectors.toList()).toString());

        // get active client config's credentials
        final Result<Page<SEBClientConfig>> cconfigs = adminRestService.getBuilder(GetClientConfigPage.class)
                .call();
        assertNotNull(cconfigs);
        assertFalse(cconfigs.hasError());
        final Page<SEBClientConfig> ccPage = cconfigs.get();
        assertFalse(ccPage.content.isEmpty());

        final SEBClientConfig clientConfig = ccPage.content.get(0);
        assertTrue(clientConfig.isActive());
        final ClientCredentials credentials = this.sebClientConfigDAO.getSEBClientCredentials(clientConfig.getModelId())
                .getOrThrow();

        adminRestService.getBuilder(ActivateClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, clientConfig.getModelId())
                .call();

        // simulate a SEB connection
        try {
            new SEBClientBot(
                    credentials.clientIdAsString(),
                    this.cryptor.decrypt(credentials.secret).getOrThrow().toString(),
                    exam.getModelId(),
                    String.valueOf(exam.institutionId));
            Thread.sleep(1000);

            // send get connections
            connectionsCall =
                    restService.getBuilder(GetClientConnectionDataList.class)
                            .withURIVariable(API.PARAM_PARENT_MODEL_ID, exam.getModelId())
                            .call();

            assertNotNull(connectionsCall);
            assertFalse(connectionsCall.hasError());
            connections = connectionsCall.get();
            assertFalse(connections.isEmpty());
            final Iterator<ClientConnectionData> iterator = connections.iterator();
            iterator.next();
            final ClientConnectionData con = iterator.next();

            // get single client connection
            final Result<ClientConnection> ccCall = restService.getBuilder(GetClientConnection.class)
                    .withURIVariable(API.PARAM_MODEL_ID, con.clientConnection.getModelId())
                    .call();

            assertFalse(ccCall.hasError());
            final ClientConnection clientConnection = ccCall.get();
            assertEquals("1", clientConnection.examId.toString());
            //assertEquals("", clientConnection.status.name());

            // get notification
            final Result<Collection<ClientNotification>> notificationsCall =
                    restService.getBuilder(GetPendingClientNotifications.class)
                            .withURIVariable(API.PARAM_PARENT_MODEL_ID, exam.getModelId())
                            .withURIVariable(API.EXAM_API_SEB_CONNECTION_TOKEN, con.clientConnection.connectionToken)
                            .call();
            assertNotNull(notificationsCall);
            assertFalse(notificationsCall.hasError());
            final Collection<ClientNotification> collection = notificationsCall.get();
            assertFalse(collection.isEmpty());
            final ClientNotification notification = collection.iterator().next();
            assertEquals("NOTIFICATION", notification.eventType.name());

            // confirm notification
            final Result<Void> confirm = restService.getBuilder(ConfirmPendingClientNotification.class)
                    .withURIVariable(API.PARAM_PARENT_MODEL_ID, exam.getModelId())
                    .withURIVariable(API.PARAM_MODEL_ID, notification.getModelId())
                    .withURIVariable(API.EXAM_API_SEB_CONNECTION_TOKEN, con.clientConnection.connectionToken)
                    .call();
            assertFalse(confirm.hasError());

            // send quit instruction
            final ClientInstruction clientInstruction = new ClientInstruction(
                    null,
                    exam.id,
                    InstructionType.SEB_QUIT,
                    con.clientConnection.connectionToken,
                    null);

            final Result<String> instructionCall = restService.getBuilder(PropagateInstruction.class)
                    .withURIVariable(API.PARAM_PARENT_MODEL_ID, String.valueOf(exam.id))
                    .withBody(clientInstruction)
                    .call();

            assertNotNull(instructionCall);
            assertFalse(instructionCall.hasError());

            Thread.sleep(1000);
        } catch (final Exception e) {
            fail(e.getMessage());
        }

        final Result<List<Indicator>> call = adminRestService.getBuilder(GetIndicators.class)
                .withQueryParam(Indicator.FILTER_ATTR_EXAM_ID, exam.getModelId())
                .call()
                .onError(error -> error.printStackTrace());
        final List<Indicator> indicatorDefs = call.get();
        final Indicator indicator = indicatorDefs.get(0);
        assertEquals("Ping", indicator.getName());

        connectionsCall =
                restService.getBuilder(GetClientConnectionDataList.class)
                        .withURIVariable(API.PARAM_PARENT_MODEL_ID, exam.getModelId())
                        .call();

        assertNotNull(connectionsCall);
        assertFalse(connectionsCall.hasError());
        connections = connectionsCall.get();
        assertFalse(connections.isEmpty());
        ClientConnectionData conData = connections.iterator().next();
        assertNotNull(conData);
        assertEquals(exam.id, conData.clientConnection.examId);
        assertFalse(conData.indicatorValues.isEmpty());
        assertTrue(conData.indicatorValues.size() == 2);
        final IndicatorValue indicatorValue = conData.indicatorValues.get(0);
        assertEquals(indicator.id, indicatorValue.getIndicatorId()); // LAST_PING indicator

        // disable connection
        final Result<String> disableCall = restService.getBuilder(DisableClientConnection.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, exam.getModelId())
                .withFormParam(
                        Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN,
                        conData.clientConnection.connectionToken)
                .call();
        assertNotNull(disableCall);
        assertFalse(disableCall.hasError());
        connectionsCall =
                restService.getBuilder(GetClientConnectionDataList.class)
                        .withURIVariable(API.PARAM_PARENT_MODEL_ID, exam.getModelId())
                        .call();

        assertNotNull(connectionsCall);
        assertFalse(connectionsCall.hasError());
        connections = connectionsCall.get();
        assertFalse(connections.isEmpty());
        conData = connections.iterator().next();
        assertEquals("DISABLED", conData.clientConnection.status.name());

        // get client logs
        final Result<Page<ExtendedClientEvent>> clientLogPage = restService.getBuilder(GetExtendedClientEventPage.class)
                .call();

        assertNotNull(clientLogPage);
        assertFalse(clientLogPage.hasError());
        final Page<ExtendedClientEvent> clientLogs = clientLogPage.get();
        assertFalse(clientLogs.isEmpty());
        final ExtendedClientEvent extendedClientEvent = clientLogs.content.get(0);
        assertNotNull(extendedClientEvent);

        // get client connection page
        Result<Page<ClientConnection>> connectionPageRes = restService
                .getBuilder(GetClientConnectionPage.class)
                .call();

        assertNotNull(connectionPageRes);
        Page<ClientConnection> connectionPage = connectionPageRes.get();
        assertNotNull(connectionPage);
        assertFalse(connectionPage.isEmpty());

        connectionPageRes = restService
                .getBuilder(GetClientConnectionPage.class)
                .withQueryParam(ClientConnection.FILTER_ATTR_INFO, "ghfhrthjrt")
                .call();

        assertNotNull(connectionPageRes);
        connectionPage = connectionPageRes.get();
        assertNotNull(connectionPage);
        assertTrue(connectionPage.isEmpty());
    }

    @Test
    @Order(18)
    // *************************************
    // Use Case 18: Login as examAdmin2 and get dependencies of examAdmin2
    // - Get all dependencies and check correctnes.
    // - Get all dependencies including only Exam Configuration and check correctnes.
    // - Get all dependencies including only ClientConnection and check correctnes.
    public void testUsecase18_UserDependencies() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetUserAccountNames(),
                new GetUserDependencies(),
                new GetExam(),
                new GetExamConfigNode());

        final EntityName user = restService.getBuilder(GetUserAccountNames.class)
                .call()
                .getOrThrow()
                .stream()
                .filter(name -> name.name.startsWith("examAdmin2"))
                .findFirst()
                .get();

        List<EntityDependency> dependencies = restService.getBuilder(GetUserDependencies.class)
                .withURIVariable(API.PARAM_MODEL_ID, user.getModelId())
                .withQueryParam(API.PARAM_BULK_ACTION_TYPE, BulkActionType.HARD_DELETE.name())
                .call()
                .getOrThrow()
                .stream()
                .sorted()
                .collect(Collectors.toList());

        assertEquals(
                "[CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "EXAM, "
                        + "EXAM_CONFIGURATION_MAP, "
                        + "INDICATOR, "
                        + "INDICATOR]",
                dependencies.stream().map(dep -> dep.self.entityType).collect(Collectors.toList()).toString());

        // check that the user is owner of all depending exams and configurations
        dependencies.stream()
                .filter(key -> key.self.entityType == EntityType.EXAM)
                .forEach(key -> {
                    assertEquals(
                            user.modelId,
                            restService.getBuilder(GetExam.class)
                                    .withURIVariable(API.PARAM_MODEL_ID, key.self.getModelId())
                                    .call()
                                    .getOrThrow().owner);
                });

        dependencies.stream()
                .filter(key -> key.self.entityType == EntityType.CONFIGURATION_NODE)
                .forEach(key -> {
                    assertEquals(
                            user.modelId,
                            restService.getBuilder(GetExamConfigNode.class)
                                    .withURIVariable(API.PARAM_MODEL_ID, key.self.getModelId())
                                    .call()
                                    .getOrThrow().owner);
                });

        // only with exam dependencies
        dependencies = restService.getBuilder(GetUserDependencies.class)
                .withURIVariable(API.PARAM_MODEL_ID, user.getModelId())
                .withQueryParam(API.PARAM_BULK_ACTION_TYPE, BulkActionType.HARD_DELETE.name())
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.EXAM.name())
                .call()
                .getOrThrow()
                .stream()
                .sorted()
                .collect(Collectors.toList());

        assertEquals(
                "[CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "EXAM, "
                        + "EXAM_CONFIGURATION_MAP, "
                        + "INDICATOR, "
                        + "INDICATOR]",
                dependencies.stream().map(dep -> dep.self.entityType).collect(Collectors.toList()).toString());

        // only with configuration dependencies
        dependencies = restService.getBuilder(GetUserDependencies.class)
                .withURIVariable(API.PARAM_MODEL_ID, user.getModelId())
                .withQueryParam(API.PARAM_BULK_ACTION_TYPE, BulkActionType.HARD_DELETE.name())
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.CONFIGURATION_NODE.name())
                .call()
                .getOrThrow()
                .stream()
                .sorted()
                .collect(Collectors.toList());

        assertEquals(
                "[CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "EXAM_CONFIGURATION_MAP]",
                dependencies.stream().map(dep -> dep.self.entityType).collect(Collectors.toList()).toString());

        // only with exam and configuration dependencies
        dependencies = restService.getBuilder(GetUserDependencies.class)
                .withURIVariable(API.PARAM_MODEL_ID, user.getModelId())
                .withQueryParam(API.PARAM_BULK_ACTION_TYPE, BulkActionType.HARD_DELETE.name())
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.CONFIGURATION_NODE.name())
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.EXAM.name())
                .call()
                .getOrThrow()
                .stream()
                .sorted()
                .collect(Collectors.toList());

        assertEquals(
                "[CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "EXAM, "
                        + "EXAM_CONFIGURATION_MAP, "
                        + "INDICATOR, "
                        + "INDICATOR]",
                dependencies.stream().map(dep -> dep.self.entityType).collect(Collectors.toList()).toString());
    }

    @Test
    @Order(19)
    // *************************************
    // Use Case 19: Login as examAdmin2 and delete the user examAdmin2 and all its relational data
    // - First login as examAdmin2 and check that it is not possible to delete the own account
    public void testUsecase19_DeleteUserDependencies() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetUserAccountNames(),
                new DeleteUserAccount(),
                new GetUserAccount(),
                new GetExam(),
                new GetExamConfigMapping(),
                new GetExamConfigNode());

        final EntityName user = restService.getBuilder(GetUserAccountNames.class)
                .call()
                .getOrThrow()
                .stream()
                .filter(name -> name.name.startsWith("examAdmin2"))
                .findFirst()
                .get();

        // try to delete the own user account should result in error message
        try {
            restService.getBuilder(DeleteUserAccount.class)
                    .withURIVariable(API.PARAM_MODEL_ID, user.getModelId())
                    .call()
                    .getOrThrow();
            fail("Forbidden error message expected here");
        } catch (final RestCallError e) {
            final APIMessage apiMessage = e.getAPIMessages().get(0);
            assertEquals("1001", apiMessage.messageCode);
            assertEquals("FORBIDDEN", apiMessage.systemMessage);
        }

        // Now login as institutional admin and delete the user account with all dependencies (exam configs and exams)
        final RestServiceImpl restServiceAdmin = createRestServiceForUser(
                "TestInstAdmin",
                "987654321",
                new GetUserAccountNames(),
                new DeleteUserAccount(),
                new GetUserAccount(),
                new GetExam(),
                new GetExamConfigMapping(),
                new GetExamConfigNode());

        final EntityProcessingReport report = restServiceAdmin.getBuilder(DeleteUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, user.getModelId())
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.EXAM.name())
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.CONFIGURATION_NODE.name())
                .call()
                .getOrThrow();

        assertNotNull(report);
        final List<EntityKey> dependencies = report.getResults()
                .stream()
                .sorted()
                .collect(Collectors.toList());
        assertEquals(
                "[CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "CLIENT_CONNECTION, "
                        + "CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "CONFIGURATION_NODE, "
                        + "EXAM, "
                        + "EXAM_CONFIGURATION_MAP, "
                        + "INDICATOR, "
                        + "INDICATOR, "
                        + "USER]",
                dependencies.stream().map(EntityKey::getEntityType).collect(Collectors.toList()).toString());

        final Set<ErrorEntry> errors = report.getErrors();
        assertNotNull(errors);
        assertTrue(errors.isEmpty());

        // test deletion
        try {
            restServiceAdmin.getBuilder(GetUserAccount.class)
                    .withURIVariable(API.PARAM_MODEL_ID, user.getModelId())
                    .call()
                    .getOrThrow();
            fail("no resource found exception expected here");
        } catch (final RestCallError e) {
            final APIMessage apiMessage = e.getAPIMessages().get(0);
            assertEquals("1002", apiMessage.getMessageCode());
            assertEquals("resource not found", apiMessage.getSystemMessage());
        }
        try {
            restServiceAdmin.getBuilder(GetExam.class)
                    .withURIVariable(API.PARAM_MODEL_ID,
                            dependencies.stream().filter(key -> key.entityType == EntityType.EXAM).findFirst()
                                    .get().modelId)
                    .call()
                    .getOrThrow();
            fail("no resource found exception expected here");
        } catch (final RestCallError e) {
            final APIMessage apiMessage = e.getAPIMessages().get(0);
            assertEquals("1002", apiMessage.getMessageCode());
            assertEquals("resource not found", apiMessage.getSystemMessage());
        }
        try {
            restServiceAdmin.getBuilder(GetExamConfigMapping.class)
                    .withURIVariable(API.PARAM_MODEL_ID, "3")
                    .call()
                    .getOrThrow();
            fail("no resource found exception expected here");
        } catch (final RestCallError e) {
            final APIMessage apiMessage = e.getAPIMessages().get(0);
            assertEquals("1002", apiMessage.getMessageCode());
            assertEquals("resource not found", apiMessage.getSystemMessage());
        }
        try {
            restServiceAdmin.getBuilder(GetExamConfigNode.class)
                    .withURIVariable(API.PARAM_MODEL_ID, "2")
                    .call()
                    .getOrThrow();
            fail("no resource found exception expected here");
        } catch (final RestCallError e) {
            final APIMessage apiMessage = e.getAPIMessages().get(0);
            assertEquals("1002", apiMessage.getMessageCode());
            assertEquals("resource not found", apiMessage.getSystemMessage());
        }
        try {
            restServiceAdmin.getBuilder(GetExamConfigNode.class)
                    .withURIVariable(API.PARAM_MODEL_ID, "3")
                    .call()
                    .getOrThrow();
            fail("no resource found exception expected here");
        } catch (final RestCallError e) {
            final APIMessage apiMessage = e.getAPIMessages().get(0);
            assertEquals("1002", apiMessage.getMessageCode());
            assertEquals("resource not found", apiMessage.getSystemMessage());
        }
        try {
            restServiceAdmin.getBuilder(GetExamConfigNode.class)
                    .withURIVariable(API.PARAM_MODEL_ID, "4")
                    .call()
                    .getOrThrow();
            fail("no resource found exception expected here");
        } catch (final RestCallError e) {
            final APIMessage apiMessage = e.getAPIMessages().get(0);
            assertEquals("1002", apiMessage.getMessageCode());
            assertEquals("resource not found", apiMessage.getSystemMessage());
        }
        try {
            restServiceAdmin.getBuilder(GetExamConfigNode.class)
                    .withURIVariable(API.PARAM_MODEL_ID, "5")
                    .call()
                    .getOrThrow();
            fail("no resource found exception expected here");
        } catch (final RestCallError e) {
            final APIMessage apiMessage = e.getAPIMessages().get(0);
            assertEquals("1002", apiMessage.getMessageCode());
            assertEquals("resource not found", apiMessage.getSystemMessage());
        }
    }

    @Test
    @Order(20)
    // *************************************
    // Use Case 20: Login as admin and get and delete user activity logs
    public void testUsecase20_DeleteUserActivityLogs() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "admin",
                "admin",
                new GetUserLogPage(),
                new GetUserLogNames(),
                new DeleteAllUserLogs());

        // get all user logs
        final List<EntityName> allUserLogs = restService.getBuilder(GetUserLogNames.class)
                .call()
                .getOrThrow();

        assertNotNull(allUserLogs);
        assertFalse(allUserLogs.isEmpty());

        // delete unknown user logs
        final String toDelete = StringUtils.join(allUserLogs.stream()
                .filter(log -> log.name.equals("--"))
                .map(name -> name.getModelId())
                .collect(Collectors.toList()), ",");

        final EntityProcessingReport report = restService.getBuilder(DeleteAllUserLogs.class)
                .withFormParam(API.PARAM_MODEL_ID_LIST, toDelete)
                .call()
                .getOrThrow();

        assertNotNull(report);
        assertTrue(report.errors.isEmpty());
        assertFalse(report.results.isEmpty());

        final List<EntityName> allUserLogsAfterDel = restService.getBuilder(GetUserLogNames.class)
                .call()
                .getOrThrow();

        assertNotNull(allUserLogsAfterDel);
        assertFalse(allUserLogsAfterDel.isEmpty());
        assertTrue(allUserLogsAfterDel.stream()
                .filter(log -> log.name.equals("--"))
                .map(name -> name.getModelId())
                .collect(Collectors.toList()).isEmpty());

    }

    @Test
    @Order(21)
    // *************************************
    // Use Case 5.5: Login as TestInstAdmin and create new Open edX LMS setup and activate
    //  - login as TestInstAdmin : 987654321
    public void testUsecase21_CreateOpenEdxLMSSetup() {
        final RestServiceImpl restService = createRestServiceForUser(
                "TestInstAdmin",
                "987654321",
                new NewLmsSetup(),
                new TestLmsSetup(),
                new GetLmsSetupNames(),
                new GetLmsSetup(),
                new SaveLmsSetup(),
                new ActivateLmsSetup(),
                new DeactivateLmsSetup(),
                new GetQuizPage());

        // create new LMS Setup Mockup
        final Result<LmsSetup> newLMSCall = restService
                .getBuilder(NewLmsSetup.class)
                .withFormParam(Domain.LMS_SETUP.ATTR_NAME, "Test Open edx")
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.OPEN_EDX.name())
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_URL, "http://localhost:8080/openedxtest")
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_CLIENTNAME, "test")
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_CLIENTSECRET, "test")
                .call();

        assertNotNull(newLMSCall);
        assertFalse(newLMSCall.hasError());
        final LmsSetup lmsSetup = newLMSCall.get();
        assertEquals("Test Open edx", lmsSetup.name);
        assertFalse(lmsSetup.isActive());

        // activate lms setup
        final LmsSetupTestResult testResult = restService
                .getBuilder(TestLmsSetup.class)
                .withURIVariable(API.PARAM_MODEL_ID, lmsSetup.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(testResult);
//        System.out.print("********************** testResult: " + testResult);
//        assertFalse(testResult.isOk());
//        assertEquals("[Error [errorType=TOKEN_REQUEST, message=Failed to gain access token from OpenEdX Rest API:\n" +
//                " tried token endpoints: [/oauth2/access_token]]]", String.valueOf(testResult.errors));

        // TODO how to mockup an Open edX response
    }

    @Test
    @Order(22)
    // *************************************
    // Use Case 22: Login as TestInstAdmin and create new Exam Template
    //  - login as TestInstAdmin : 987654321
    //  - check exam template list is empty
    //  - create new exam template with existing configuration template
    //  - check exam template list contains created exam template
    //  - add indicator templates to the exam template
    //  - add configuration template to exam templare
    //  - create exam from template
    //  - delete exam template
    //  - delete exam
    public void testUsecase22_ExamTemplate() {
        final RestServiceImpl restService = createRestServiceForUser(
                "TestInstAdmin",
                "987654321",
                new GetExamTemplatePage(),
                new GetExamTemplate(),
                new GetExamTemplates(),
                new GetDefaultExamTemplate(),
                new NewExamTemplate(),
                new NewExamConfig(),
                new SaveExamTemplate(),
                new GetIndicatorTemplatePage(),
                new NewIndicatorTemplate(),
                new SaveIndicatorTemplate(),
                new DeleteIndicatorTemplate(),
                new DeleteExamTemplate(),
                new GetIndicatorTemplate(),
                new GetExamConfigNodeNames(),
                new GetUserAccountNames(),
                new GetQuizPage(),
                new ImportAsExam(),
                new GetExamDependencies(),
                new GetExam(),
                new DeleteExam());

        Page<ExamTemplate> examTemplatePage = restService
                .getBuilder(GetExamTemplatePage.class)
                .call()
                .getOrThrow();

        assertTrue(examTemplatePage.isEmpty());

        // create new exam config template
        final ConfigurationNode configTemplate = restService
                .getBuilder(NewExamConfig.class)
                .withFormParam(Domain.CONFIGURATION_NODE.ATTR_NAME, "templateTest")
                .withFormParam(Domain.CONFIGURATION_NODE.ATTR_TYPE, ConfigurationType.TEMPLATE.name())
                .call()
                .getOrThrow();

        assertNotNull(configTemplate);
        assertEquals("templateTest", configTemplate.name);

        // create exam template with config template reference
        final ExamTemplate examTemplate = restService
                .getBuilder(NewExamTemplate.class)
                .withFormParam(Domain.EXAM_TEMPLATE.ATTR_NAME, "examTemplate")
                .withFormParam(Domain.EXAM_TEMPLATE.ATTR_CONFIGURATION_TEMPLATE_ID, configTemplate.getModelId())
                .withFormParam(Domain.EXAM_TEMPLATE.ATTR_INSTITUTIONAL_DEFAULT, "true")
                .withFormParam(Domain.EXAM_TEMPLATE.ATTR_EXAM_TYPE, ExamType.MANAGED.name())
                .call()
                .getOrThrow();

        assertNotNull(examTemplate);
        assertEquals("examTemplate", examTemplate.name);
        assertTrue(examTemplate.institutionalDefault);
        assertEquals(configTemplate.institutionId, examTemplate.institutionId);
        assertEquals(configTemplate.id, examTemplate.configTemplateId);

        // get default exam template
        final ExamTemplate defaultExamTemplate = restService
                .getBuilder(GetDefaultExamTemplate.class)
                .call()
                .getOrThrow();

        assertNotNull(defaultExamTemplate);

        // get list again and check entry
        examTemplatePage = restService
                .getBuilder(GetExamTemplatePage.class)
                .call()
                .getOrThrow();

        assertFalse(examTemplatePage.isEmpty());
        final ExamTemplate templateFromList = examTemplatePage.getContent().iterator().next();
        assertNotNull(templateFromList);
        assertEquals("examTemplate", templateFromList.name);
        assertTrue(templateFromList.institutionalDefault);
        assertEquals(configTemplate.institutionId, templateFromList.institutionId);
        assertEquals(configTemplate.id, templateFromList.configTemplateId);

        // create new indicator template
        MultiValueMap<String, String> thresholds = new LinkedMultiValueMap<>();
        thresholds.add(Domain.THRESHOLD.REFERENCE_NAME, "1|000001");
        thresholds.add(Domain.THRESHOLD.REFERENCE_NAME, "2|000002");
        thresholds.add(Domain.THRESHOLD.REFERENCE_NAME, "3|000003");
        IndicatorTemplate indicatorTemplate = restService
                .getBuilder(NewIndicatorTemplate.class)
                .withFormParam(IndicatorTemplate.ATTR_EXAM_TEMPLATE_ID, examTemplate.getModelId())
                .withFormParam(Domain.INDICATOR.ATTR_NAME, "Errors")
                .withFormParam(Domain.INDICATOR.ATTR_TYPE, IndicatorType.ERROR_COUNT.name)
                .withFormParam(Domain.INDICATOR.ATTR_COLOR, "000001")
                .withFormParams(thresholds)
                .call()
                .getOrThrow();

        assertNotNull(indicatorTemplate);
        assertEquals(examTemplate.id, indicatorTemplate.examTemplateId);
        assertEquals("Errors", indicatorTemplate.name);
        assertTrue(indicatorTemplate.thresholds.size() == 3);

        // get indicator list for template
        final Page<IndicatorTemplate> indicatorList = restService
                .getBuilder(GetIndicatorTemplatePage.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, examTemplate.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(indicatorList);
        assertFalse(indicatorList.isEmpty());
        assertTrue(indicatorList.content.size() == 1);
        final IndicatorTemplate indicator = indicatorList.content.get(0);

        final IndicatorTemplate ind = restService
                .getBuilder(GetIndicatorTemplate.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, examTemplate.getModelId())
                .withURIVariable(API.PARAM_MODEL_ID, indicator.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(ind);
        assertEquals(ind, indicator);

        // get exam config template for use
        final List<EntityName> configTemplateNames = restService.getBuilder(GetExamConfigNodeNames.class)
                .withQueryParam(ConfigurationNode.FILTER_ATTR_TYPE, ConfigurationType.TEMPLATE.name())
                .call()
                .getOrThrow();

        assertNotNull(configTemplateNames);
        assertFalse(configTemplateNames.isEmpty());
        final EntityName configTemplateName = configTemplateNames.get(0);

        // edit/save exam template
        ExamTemplate savedTemplate = restService
                .getBuilder(SaveExamTemplate.class)
                .withBody(new ExamTemplate(
                        examTemplate.id,
                        examTemplate.institutionId,
                        examTemplate.name,
                        "New Description",
                        null,
                        null,
                        Long.parseLong(configTemplateName.modelId), // assosiate with given config template
                        true,
                        null,
                        null))
                .call()
                .getOrThrow();

        assertNotNull(savedTemplate);
        assertEquals("New Description", savedTemplate.description);
        assertNotNull(savedTemplate.configTemplateId);
        assertTrue(savedTemplate.institutionalDefault);

        // edit/save indicator template
        IndicatorTemplate savedIndicatorTemplate = restService
                .getBuilder(SaveIndicatorTemplate.class)
                .withBody(new IndicatorTemplate(
                        indicatorTemplate.id,
                        indicatorTemplate.examTemplateId,
                        "New Errors",
                        indicatorTemplate.type,
                        null, null, null, null))
                .call()
                .getOrThrow();

        assertNotNull(savedIndicatorTemplate);
        assertEquals("New Errors", savedIndicatorTemplate.name);

        savedTemplate = restService
                .getBuilder(GetExamTemplate.class)
                .withURIVariable(API.PARAM_MODEL_ID, savedTemplate.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(savedTemplate);
        assertNotNull(savedTemplate.indicatorTemplates);
        assertFalse(savedTemplate.indicatorTemplates.isEmpty());

        savedIndicatorTemplate = savedTemplate.indicatorTemplates.iterator().next();
        assertNotNull(savedIndicatorTemplate);
        assertEquals("New Errors", savedIndicatorTemplate.name);

        // create/remove indicator template
        thresholds = new LinkedMultiValueMap<>();
        thresholds.add(Domain.THRESHOLD.REFERENCE_NAME, "1|000001");
        thresholds.add(Domain.THRESHOLD.REFERENCE_NAME, "2|000002");
        thresholds.add(Domain.THRESHOLD.REFERENCE_NAME, "3|000003");
        indicatorTemplate = restService
                .getBuilder(NewIndicatorTemplate.class)
                .withFormParam(IndicatorTemplate.ATTR_EXAM_TEMPLATE_ID, examTemplate.getModelId())
                .withFormParam(Domain.INDICATOR.ATTR_NAME, "Errors")
                .withFormParam(Domain.INDICATOR.ATTR_TYPE, IndicatorType.ERROR_COUNT.name)
                .withFormParam(Domain.INDICATOR.ATTR_COLOR, "000001")
                .withFormParams(thresholds)
                .call()
                .getOrThrow();

        savedTemplate = restService
                .getBuilder(GetExamTemplate.class)
                .withURIVariable(API.PARAM_MODEL_ID, savedTemplate.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(savedTemplate);
        assertNotNull(savedTemplate.indicatorTemplates);
        assertFalse(savedTemplate.indicatorTemplates.isEmpty());
        assertTrue(savedTemplate.indicatorTemplates.size() == 2);
        final Iterator<IndicatorTemplate> iterator = savedTemplate.indicatorTemplates.iterator();
        final IndicatorTemplate next1 = iterator.next();
        final IndicatorTemplate next2 = iterator.next();
        assertEquals("New Errors", next1.name);
        assertEquals("Errors", next2.name);

        final EntityKey entityKey = restService
                .getBuilder(DeleteIndicatorTemplate.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, savedTemplate.getModelId())
                .withURIVariable(API.PARAM_MODEL_ID, next2.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(entityKey);
        savedTemplate = restService
                .getBuilder(GetExamTemplate.class)
                .withURIVariable(API.PARAM_MODEL_ID, savedTemplate.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(savedTemplate);
        assertNotNull(savedTemplate.indicatorTemplates);
        assertFalse(savedTemplate.indicatorTemplates.isEmpty());
        assertTrue(savedTemplate.indicatorTemplates.size() == 1);
        assertEquals("New Errors", savedTemplate.indicatorTemplates.iterator().next().name);

        // create exam from template
        // check quizzes are defined
        final String userId = restService
                .getBuilder(GetUserAccountNames.class)
                .call()
                .get()
                .stream()
                .filter(userName -> userName.name != null && userName.name.startsWith("examSupport2"))
                .findFirst()
                .map(EntityName::getModelId)
                .orElse(null);
        final QuizData quizData = restService
                .getBuilder(GetQuizPage.class)
                .call()
                .get().content
                        .stream()
                        .filter(q -> q.name.contains("Demo Quiz 6"))
                        .findFirst()
                        .get();

        // import quiz as exam
        final Result<Exam> newExamResult = restService
                .getBuilder(ImportAsExam.class)
                .withFormParam(QuizData.QUIZ_ATTR_LMS_SETUP_ID, String.valueOf(quizData.lmsSetupId))
                .withFormParam(QuizData.QUIZ_ATTR_ID, quizData.id)
                .withFormParam(Domain.EXAM.ATTR_SUPPORTER, userId)
                .withFormParam(Domain.EXAM.ATTR_EXAM_TEMPLATE_ID, savedTemplate.getModelId())
                .call();

        assertFalse(newExamResult.hasError());
        final Exam exam = newExamResult.get();
        assertEquals("Demo Quiz 6 (MOCKUP)", exam.name);
        // get dependencies report for exam
        final Result<Set<EntityDependency>> dependenciesCall = restService
                .getBuilder(GetExamDependencies.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .withQueryParam(API.PARAM_BULK_ACTION_TYPE, BulkActionType.HARD_DELETE.name())
                .withQueryParam(API.PARAM_BULK_ACTION_ADD_INCLUDES, Constants.TRUE_STRING)
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.INDICATOR.name())
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.EXAM_CONFIGURATION_MAP.name())
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.CONFIGURATION_NODE.name())
                .call();

        assertFalse(dependenciesCall.hasError());
        assertTrue(dependenciesCall.get().size() == 2);

        // delete exam template
        final Result<EntityProcessingReport> deleteTemplateCall = restService
                .getBuilder(DeleteExamTemplate.class)
                .withURIVariable(API.PARAM_MODEL_ID, savedTemplate.getModelId())
                .call();

        assertFalse(deleteTemplateCall.hasError());
        final Result<Exam> examCall = restService
                .getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call();
        assertFalse(examCall.hasError());
        final Exam exam2 = examCall.get();
        assertNull(exam2.examTemplateId);

        // delete exam
        final Result<EntityProcessingReport> deleteExamCall = restService
                .getBuilder(DeleteExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam2.getModelId())
                .withQueryParam(API.PARAM_BULK_ACTION_ADD_INCLUDES, Constants.TRUE_STRING)
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.INDICATOR.name())
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.EXAM_CONFIGURATION_MAP.name())
                .withQueryParam(API.PARAM_BULK_ACTION_INCLUDES, EntityType.CONFIGURATION_NODE.name())
                .call();

        assertFalse(deleteExamCall.hasError());
        final EntityProcessingReport entityProcessingReport = deleteExamCall.get();
        assertTrue(entityProcessingReport.getErrors().isEmpty());
        assertTrue(entityProcessingReport.results.size() == 3);
    }

    @Test
    @Order(23)
    // *************************************
    // Use Case 23: Certificates
    // - check certificates (list) is empty
    // - upload test certificate
    // - check certificates
    // - upload identity certificate
    // - check certificates
    // - create new connection config with identity certigficate encryption
    // - donwload connection config with identity certigficate encryption
    // - remove certificate
    public void testUsecase23_Certificates() throws Exception {
        final RestServiceImpl restService = createRestServiceForUser(
                "TestInstAdmin",
                "987654321",
                new GetCertificate(),
                new GetCertificateNames(),
                new GetCertificatePage(),
                new RemoveCertificate(),
                new AddCertificate(),
                new NewClientConfig(),
                new SaveClientConfig(),
                new ActivateClientConfig(),
                new DeactivateClientConfig(),
                new ExportClientConfig());

        // - check certificates (list) is empty
        Page<CertificateInfo> certificates = restService
                .getBuilder(GetCertificatePage.class)
                .call()
                .getOrThrow();

        assertNotNull(certificates);
        assertTrue(certificates.content.isEmpty());

        // - upload test certificate
        InputStream inputStream = new ClassPathResource("sebserver-test.cer").getInputStream();
        final CertificateInfo newCert = restService
                .getBuilder(AddCertificate.class)
                .withBody(inputStream)
                .withHeader(API.IMPORT_FILE_ATTR_NAME, "sebserver-test.cer")
                .call()
                .getOrThrow();

        assertNotNull(newCert);
        assertEquals("test.anhefti.sebserver", newCert.alias);
        assertTrue(newCert.types.contains(CertificateType.DIGITAL_SIGNATURE));
        assertTrue(newCert.types.contains(CertificateType.DATA_ENCIPHERMENT));

        // - check certificates
        certificates = restService
                .getBuilder(GetCertificatePage.class)
                .call()
                .getOrThrow();

        assertNotNull(certificates);
        assertFalse(certificates.content.isEmpty());

        final CertificateInfo certificateInfo = certificates.content.get(0);
        assertEquals("test.anhefti.sebserver", certificateInfo.alias);

        // - upload identity certificate
        inputStream = new ClassPathResource("testIdentity123.pfx").getInputStream();
        final CertificateInfo newCert1 = restService
                .getBuilder(AddCertificate.class)
                .withBody(inputStream)
                .withHeader(API.IMPORT_FILE_ATTR_NAME, "testIdentity123.pfx")
                .withHeader(API.IMPORT_PASSWORD_ATTR_NAME, "123")
                .call()
                .getOrThrow();

        assertNotNull(newCert1);
        assertEquals("*.2mdn.net", newCert1.alias);
        assertTrue(newCert1.types.contains(CertificateType.DATA_ENCIPHERMENT_PRIVATE_KEY));

        // - check certificates
        certificates = restService
                .getBuilder(GetCertificatePage.class)
                .call()
                .getOrThrow();

        assertNotNull(certificates);
        assertFalse(certificates.content.isEmpty());
        assertTrue(certificates.content.size() == 2);

        // - create new connection config with identity certigficate encryption
        final SEBClientConfig newConfig = restService
                .getBuilder(NewClientConfig.class)
                .withFormParam(Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME, "Connection Config with Cert")
                .withFormParam(SEBClientConfig.ATTR_ENCRYPT_CERTIFICATE_ALIAS, "*.2mdn.net")
                .withFormParam(SEBClientConfig.ATTR_CONFIG_PURPOSE, SEBClientConfig.ConfigPurpose.START_EXAM.name())
                .call()
                .getOrThrow();

        assertNotNull(newConfig);
        assertEquals("*.2mdn.net", newConfig.encryptCertificateAlias);

        // - donwload connection config with identity certigficate encryption
        final Result<Boolean> exportResponse = restService
                .getBuilder(ExportClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, newConfig.getModelId())
                .withResponseExtractor(response -> {
                    final InputStream input = response.getBody();
                    final List<String> readLines = IOUtils.readLines(input, "UTF-8");
                    assertNotNull(readLines);
                    assertFalse(readLines.isEmpty());
                    return true;
                })
                .call();

        assertNotNull(exportResponse);
        assertTrue(exportResponse.get());

        // - remmove certificate
        final CertificateInfo cert = restService
                .getBuilder(GetCertificate.class)
                .withURIVariable(API.CERTIFICATE_ALIAS, "test.anhefti.sebserver")
                //.withURIVariable(API.PARAM_MODEL_ID, newCert.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(cert);
        assertEquals("test.anhefti.sebserver", cert.alias);

        final Result<Collection<EntityKey>> removeCert = restService
                .getBuilder(RemoveCertificate.class)
                .withFormParam(API.CERTIFICATE_ALIAS, "test.anhefti.sebserver")
                .call();

        assertNotNull(removeCert);
        assertFalse(removeCert.hasError());
        final Collection<EntityKey> collection = removeCert.get();
        assertFalse(collection.isEmpty());
        final EntityKey next = collection.iterator().next();
        assertEquals(
                "EntityKey [modelId=test.anhefti.sebserver, entityType=CERTIFICATE]",
                next.toString());

    }

    @Test
    @Order(24)
    // *************************************
    // Use Case 24: Register a new User
    public void testUsecase24_RegisterUser() throws Exception {
        final RestServiceImpl restService = createRestServiceForUser(
                "admin",
                "admin",
                new RegisterNewUser());

        final UserInfo newUser = restService.getBuilder(RegisterNewUser.class)
                .withFormParam(Domain.USER.ATTR_INSTITUTION_ID, "1")
                .withFormParam(Domain.USER.ATTR_NAME, "testSupport1")
                .withFormParam(Domain.USER.ATTR_USERNAME, "testSupport1")
                .withFormParam(Domain.USER.ATTR_SURNAME, "testSupport1")
                .withFormParam(Domain.USER.ATTR_EMAIL, "test@test16.ch")
                .withFormParam(PasswordChange.ATTR_NAME_NEW_PASSWORD, "testSupport1")
                .withFormParam(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, "testSupport1")
                .withFormParam(Domain.USER.ATTR_LANGUAGE, Locale.ENGLISH.getLanguage())
                .withFormParam(Domain.USER.ATTR_TIMEZONE, DateTimeZone.UTC.getID())
                .call()
                .getOrThrow();

        assertNotNull(newUser);
        assertEquals("testSupport1", newUser.name);
        assertEquals("[EXAM_SUPPORTER]", newUser.getRoles().toString());

        // password mismatch
        final Result<UserInfo> call = restService.getBuilder(RegisterNewUser.class)
                .withFormParam(Domain.USER.ATTR_INSTITUTION_ID, "1")
                .withFormParam(Domain.USER.ATTR_NAME, "testSupport1")
                .withFormParam(Domain.USER.ATTR_USERNAME, "testSupport1")
                .withFormParam(Domain.USER.ATTR_SURNAME, "testSupport1")
                .withFormParam(Domain.USER.ATTR_EMAIL, "test@test16.ch")
                .withFormParam(PasswordChange.ATTR_NAME_NEW_PASSWORD, "testSupport123")
                .withFormParam(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, "testSupport1")
                .withFormParam(Domain.USER.ATTR_LANGUAGE, Locale.ENGLISH.getLanguage())
                .withFormParam(Domain.USER.ATTR_TIMEZONE, DateTimeZone.UTC.getID())
                .call();
        assertNotNull(call);
        assertTrue(call.hasError());
        final Exception error = call.getError();
        assertTrue(error.getMessage().contains("confirmNewPassword:password.mismatch"));
    }

    @Test
    @Order(25)
    // *************************************
    // Use Case 25: Exam administration SEB restrcition
    public void testUsecase25_ExamAdminSEBRestriction() {
        final RestServiceImpl restService = createRestServiceForUser(
                "admin",
                "admin",
                new CheckSEBRestriction(),
                new GetSEBRestrictionSettings(),
                new ActivateSEBRestriction(),
                new SaveSEBRestriction(),
                new GetExamPage(),
                new DeactivateSEBRestriction(),
                new GetCourseChapters());

        Exam exam = createTestExam("admin", "admin");
        assertNotNull(exam);
        assertEquals("Demo Quiz 6 (MOCKUP)", exam.name);

        // check SEB restriction
        final Boolean check = restService
                .getBuilder(CheckSEBRestriction.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call()
                .getOrThrow();

        assertFalse(check);

        // get restriction settings
        SEBRestriction restriction = restService
                .getBuilder(GetSEBRestrictionSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(restriction);
        assertEquals(exam.id.toString(), restriction.examId.toString());
        assertEquals(
                "[]",
                restriction.configKeys.toString());
        assertEquals(
                "[]",
                restriction.browserExamKeys.toString());

        final List<String> examKeys = Arrays.asList("exam-key");

        exam = restService
                .getBuilder(SaveSEBRestriction.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .withBody(new SEBRestriction(
                        restriction.examId,
                        restriction.configKeys,
                        examKeys,
                        Collections.emptyMap()))
                .call()
                .getOrThrow();
        assertNotNull(exam);
        assertEquals("Demo Quiz 6 (MOCKUP)", exam.name);
        restriction = restService
                .getBuilder(GetSEBRestrictionSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(restriction);
        assertEquals(exam.id.toString(), restriction.examId.toString());
        assertEquals(
                "[]",
                restriction.configKeys.toString());
        assertEquals(
                "[exam-key]",
                restriction.browserExamKeys.toString());

        final Result<Exam> applyCall = restService
                .getBuilder(ActivateSEBRestriction.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call();

        assertTrue(applyCall.hasError());
        assertTrue(applyCall.getError() instanceof RestCallError);
        assertTrue(applyCall.getError().getCause().getMessage().contains("SEB Restriction feature not available"));

        final Result<Exam> deleteCall = restService
                .getBuilder(DeactivateSEBRestriction.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call();

        assertTrue(deleteCall.hasError());
        assertTrue(deleteCall.getError() instanceof RestCallError);
        assertTrue(deleteCall.getError().getCause().getMessage().contains("SEB Restriction feature not available"));

        final Result<Chapters> chaptersCall = restService
                .getBuilder(GetCourseChapters.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call();

        assertTrue(chaptersCall.hasError());
        assertTrue(chaptersCall.getError() instanceof RestCallError);
        assertTrue(chaptersCall.getError().getCause().getMessage().contains("Course Chapter feature not supported"));

    }

    @Test
    @Order(26)
    // *************************************
    // Use Case 26: Exam administration SEB restrcition
    public void testUsecase26_ExamAdminProctoring() {
        final RestServiceImpl restService = createRestServiceForUser(
                "admin",
                "admin",
                new GetProctoringSettings(),
                new SaveProctoringSettings());

        final Exam exam = createTestExam("admin", "admin");
        assertNotNull(exam);
        assertEquals("Demo Quiz 6 (MOCKUP)", exam.name);

        final ProctoringServiceSettings settings = restService
                .getBuilder(GetProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call()
                .getOrThrow();

        assertNotNull(settings);
        assertFalse(settings.enableProctoring);

        final ProctoringServiceSettings newSettings = new ProctoringServiceSettings(
                settings.examId,
                false,
                ProctoringServerType.JITSI_MEET,
                "https://seb-jitsi.ethz.ch/",
                20,
                EnumSet.of(ProctoringFeature.TOWN_HALL, ProctoringFeature.ONE_TO_ONE),
                false,
                "apiKey",
                "apiSecret",
                "sdkKey",
                "sdkSecret",
                false);

        final Result<Exam> saveCall = restService
                .getBuilder(SaveProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .withBody(newSettings)
                .call();

        if (!saveCall.hasError()) {
            assertFalse(saveCall.hasError());
            final Exam exam2 = saveCall.get();
            assertEquals(exam2.id, exam.id);
        }
    }

    private Exam createTestExam(final String userName, final String secret) {
        final RestServiceImpl restService = createRestServiceForUser(
                userName,
                secret,
                new NewLmsSetup(),
                new ActivateLmsSetup(),
                new GetQuizPage(),
                new ImportAsExam());

        // create LMS Setup (MOCKUP)
        final String uuid = UUID.randomUUID().toString();
        final LmsSetup lmsSetup = restService
                .getBuilder(NewLmsSetup.class)
                .withFormParam(Domain.LMS_SETUP.ATTR_NAME, "Test LMS Mockup " + uuid)
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsType.MOCKUP.name())
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_URL, "http://")
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_CLIENTNAME, "test")
                .withFormParam(Domain.LMS_SETUP.ATTR_LMS_CLIENTSECRET, "test")
                .call()
                .getOrThrow();
        restService
                .getBuilder(ActivateLmsSetup.class)
                .withURIVariable(API.PARAM_MODEL_ID, lmsSetup.getModelId())
                .call()
                .getOrThrow();

        // import quiz6 as exam return id
        final QuizData quizData = restService
                .getBuilder(GetQuizPage.class)
                .withQueryParam(LmsSetup.FILTER_ATTR_LMS_SETUP, lmsSetup.id.toString())
                .call()
                .map(q -> {
                    System.out.println("************************** q: " + q);
                    return q;
                })
                .get().content
                        .stream()
                        .filter(q -> q.lmsSetupId.longValue() == lmsSetup.id.longValue()
                                && q.name.contains("Demo Quiz 6"))
                        .findFirst()
                        .get();

        return restService
                .getBuilder(ImportAsExam.class)
                .withFormParam(QuizData.QUIZ_ATTR_LMS_SETUP_ID, String.valueOf(quizData.lmsSetupId))
                .withFormParam(QuizData.QUIZ_ATTR_ID, quizData.id)
                .call()
                .getOrThrow();
    }

}
