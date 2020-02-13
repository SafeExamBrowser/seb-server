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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.tomcat.util.buf.StringUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.StreamUtils;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.SEB_CLIENT_CONFIGURATION;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues.TableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.AttributeMapping;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ExamConfigurationServiceImpl;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestServiceImpl;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.ExportExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicator;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicatorPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.NewIndicator;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveIndicator;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.GetQuizData;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.GetQuizPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.ImportAsExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.ActivateClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.DeactivateClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.ExportClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.GetClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.GetClientConfigPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.NewClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.SaveClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ExportPlainXML;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigAttributes;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationTableValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationValuePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurations;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNode;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNodePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetFollowupConfiguration;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetOrientationPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetOrientations;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetViewList;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetViewPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetViews;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ImportExamConfigOnExistingConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ImportNewExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.NewExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfigHistory;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfigTableValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfigValue;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SebExamConfigUndo;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.ActivateUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.ChangePassword;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccountNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.NewUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.SaveUserAccount;

public class UseCasesIntegrationTest extends GuiIntegrationTest {

    @Before
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
    public void init() {

    }

    @After
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
    public void cleanup() {

    }

    @Test
    @Order(1)
    // *************************************
    // Use Case 1: SEB Administrator creates a new institution and activate this new institution

    public void testUsecase1() {
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
    public void testUsecase2() {
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
    public void testUsecase3() {
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
        assertEquals("TestInstAdmin", userNames.get(0).name);

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
                String.valueOf(error.getErrorMessages()));

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
                String.valueOf(error.getErrorMessages().get(0).getSystemMessage()));

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
    public void testUsecase4() {
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
                .withFormParam(Domain.USER.ATTR_EMAIL, "test@test.ch")
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
    public void testUsecase5() {
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

        // check still no quizzes available form the LMS (not active now)
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
                        lmsSetup.active))
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
    }

    @Test
    @Order(6)
    // *************************************
    // Use Case 6: Login as examAdmin2
    // - Check if there are some quizzes form previous LMS Setup
    // - Import a quiz as Exam
    // - get exam page and check the exam is there
    // - edit exam property and save again
    public void testUsecase6() {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetUserAccountNames(),
                new NewLmsSetup(),
                new GetQuizPage(),
                new GetQuizData(),
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
                .filter(userName -> "examSupport2".equals(userName.name))
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
                null, null,
                Utils.immutableCollectionOf(userId),
                ExamStatus.RUNNING,
                true,
                null,
                true,
                null);

        final Result<Exam> savedExamResult = restService
                .getBuilder(SaveExam.class)
                .withBody(examForSave)
                .call();

        assertNotNull(savedExamResult);
        assertFalse(savedExamResult.hasError());
        final Exam savedExam = savedExamResult.get();

        assertEquals(ExamType.MANAGED, savedExam.type);
        assertFalse(savedExam.supporter.isEmpty());
    }

    @Test
    @Order(7)
    // *************************************
    // Use Case 7: Login as examAdmin2
    // - Get imported exam
    // - add new indicator for exam
    // - save exam with new indicator and test
    // - create some thresholds for the new indicator
    public void testUsecase7() {
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
                .withFormParam(Domain.INDICATOR.ATTR_NAME, "Ping")
                .withFormParam(Domain.INDICATOR.ATTR_TYPE, IndicatorType.LAST_PING.name)
                .withFormParam(Domain.INDICATOR.ATTR_COLOR, "000001")
                .call();

        assertNotNull(newIndicatorResult);
        assertFalse(newIndicatorResult.hasError());
        final Indicator newIndicator = newIndicatorResult.get();

        assertEquals("Ping", newIndicator.name);
        assertEquals("000001", newIndicator.defaultColor);

        final Indicator indicatorToSave = new Indicator(
                newIndicator.id, newIndicator.examId, newIndicator.name, newIndicator.type, newIndicator.defaultColor,
                Utils.immutableCollectionOf(
                        new Indicator.Threshold(2000d, "000011"),
                        new Indicator.Threshold(5000d, "001111")));

        final Result<Indicator> savedIndicatorResult = restService
                .getBuilder(SaveIndicator.class)
                .withBody(indicatorToSave)
                .call();

        assertNotNull(savedIndicatorResult);
        assertFalse(savedIndicatorResult.hasError());
        final Indicator savedIndicator = savedIndicatorResult.get();

        assertEquals("Ping", savedIndicator.name);
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
    @Order(8)
    // *************************************
    // Use Case 8: Login as TestInstAdmin and create a SEB Client Configuration
    // - create one with and one without password
    // - activate one config
    // - export both configurations
    public void testUsecase8() throws IOException {
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
        final Result<SebClientConfig> newConfigResponse = restService
                .getBuilder(NewClientConfig.class)
                .withFormParam(Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME, "No Password Protection")
                .withFormParam(SebClientConfig.ATTR_FALLBACK_START_URL, "http://fallback.com/fallback")
                .call();

        assertNotNull(newConfigResponse);
        assertFalse(newConfigResponse.hasError());
        final SebClientConfig sebClientConfig = newConfigResponse.get();
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

        final Result<SebClientConfig> getConfigResponse = restService
                .getBuilder(GetClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, sebClientConfig.getModelId())
                .call();

        assertNotNull(getConfigResponse);
        assertFalse(getConfigResponse.hasError());
        final SebClientConfig activeConfig = getConfigResponse.get();
        assertTrue(activeConfig.isActive());

        // create a config with password protection
        final Result<SebClientConfig> configWithPasswordResponse = restService
                .getBuilder(NewClientConfig.class)
                .withFormParam(Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME, "With Password Protection")
                .withFormParam(SebClientConfig.ATTR_FALLBACK_START_URL, "http://fallback.com/fallback")
                .withFormParam(SEB_CLIENT_CONFIGURATION.ATTR_ENCRYPT_SECRET, "123")
                .withFormParam(SebClientConfig.ATTR_CONFIRM_ENCRYPT_SECRET, "123")
                .call();

        assertNotNull(configWithPasswordResponse);
        assertFalse(configWithPasswordResponse.hasError());
        final SebClientConfig configWithPassword = configWithPasswordResponse.get();
        assertEquals("With Password Protection", configWithPassword.name);
        assertFalse(configWithPassword.isActive());
        assertEquals("http://fallback.com/fallback", configWithPassword.fallbackStartURL);

        // export client config No Password Protection
        Result<InputStream> exportResponse = restService
                .getBuilder(ExportClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, sebClientConfig.getModelId())
                .call();

        assertNotNull(exportResponse);
        assertFalse(exportResponse.hasError());

        List<String> readLines = IOUtils.readLines(exportResponse.get(), "UTF-8");
        assertNotNull(readLines);
        assertFalse(readLines.isEmpty());
        assertTrue(readLines.get(0).startsWith("plnd"));

        // export client config With Password Protection
        exportResponse = restService
                .getBuilder(ExportClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, configWithPassword.getModelId())
                .call();

        assertNotNull(exportResponse);
        assertFalse(exportResponse.hasError());

        readLines = IOUtils.readLines(exportResponse.get(), "UTF-8");
        assertNotNull(readLines);
        assertFalse(readLines.isEmpty());
        assertTrue(readLines.get(0).startsWith("pswd"));

        // get page
        final Result<Page<SebClientConfig>> pageResponse = restService
                .getBuilder(GetClientConfigPage.class)
                .call();

        assertNotNull(pageResponse);
        assertFalse(pageResponse.hasError());
        final Page<SebClientConfig> page = pageResponse.get();
        assertFalse(page.content.isEmpty());
        assertTrue(page.content.size() == 2);
    }

    @Test
    @Order(9)
    // *************************************
    // Use Case 9: Login as examAdmin2 and test Exam Configuration data basis
    // - get all Views for the default template
    // - get all Attributes and and Orientations for the default view
    @Sql(scripts = { "classpath:data-test-additional.sql" })
    public void testUsecase9() throws IOException {
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
        assertEquals(192, attributeMapping.attributeIdMapping.size());
        assertEquals("[active, audio, backToStart, browserSecurity, browserViewMode, "
                + "exitSequence, functionKeys, kioskMode, logging, macSettings, "
                + "newBrowserWindow, newwinsize, proxies, quitLink, registry, "
                + "servicePolicy, specialKeys, spellcheck, taskbar, urlFilter, "
                + "userAgentDesktop, userAgentMac, userAgentTouch, winsize, wintoolbar, zoom, zoomMode]",
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
    @Order(10)
    // *************************************
    // Use Case 10: Login as examAdmin2 and create a new SEB Exam Configuration
    // - test creation
    // - save configuration in history
    // - change some attribute
    // - process an undo
    public void testUsecase10() throws IOException {
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
                new SebExamConfigUndo(),
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
                .filter(userName -> "examAdmin2".equals(userName.name))
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
        final ConfigurationValue value = values.get(0);
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
                .getBuilder(SebExamConfigUndo.class)
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
        final ConfigurationValue currentValue =
                values.stream().filter(v -> v.attributeId == value.attributeId).findFirst().orElse(null);
        assertNotNull(currentValue);
        assertEquals("2", currentValue.value);
    }

    @Test
    @Order(11)
    // *************************************
    // Use Case 11: Login as examAdmin2 and get newly created exam configuration
    // - get permitted processes table values
    // - modify permitted processes table values
    // - save permitted processes table values
    // - check save OK
    public void testUsecase11() throws IOException {
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
    @Order(12)
    // *************************************
    // Use Case 12: Login as examAdmin2 and use newly created configuration
    // - get follow-up configuration by API
    // - import
    // - export
    public void testUsecase12() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetConfigAttributes(),
                new GetExamConfigNodePage(),
                new SaveExamConfigHistory(),
                new ExportExamConfig(),
                new ImportNewExamConfig(),
                new ImportExamConfigOnExistingConfig(),
                new ExportPlainXML(),
                new GetFollowupConfiguration());

        // get all configuration attributes
        final Collection<ConfigurationAttribute> attributes = restService
                .getBuilder(GetConfigAttributes.class)
                .call()
                .getOrThrow()
                .stream()
                .collect(Collectors.toList());

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
        final InputStream input = restService
                .getBuilder(ExportPlainXML.class)
                .withURIVariable(API.PARAM_MODEL_ID, configurationNode.getModelId())
                .call()
                .getOrThrow();

        final String xmlString = StreamUtils.copyToString(input, Charsets.UTF_8);
        assertNotNull(xmlString);
        for (final ConfigurationAttribute attribute : attributes) {
            if (attribute.name.contains(".") || attribute.name.equals("kioskMode")) {
                continue;
            }
            if (!xmlString.contains(attribute.name)) {
                fail("missing attribute: " + attribute.name);
            }
        }

        // import plain config
        InputStream inputStream = new ClassPathResource("importTest.seb").getInputStream();
        Configuration importedConfig = restService
                .getBuilder(ImportNewExamConfig.class)
                .withBody(inputStream)
                .withHeader(Domain.CONFIGURATION_NODE.ATTR_NAME, "Imported Test Configuration")
                .call()
                .getOrThrow();

        assertNotNull(importedConfig);

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
    @Order(13)
    // *************************************
    // Use Case 13: Login as examAdmin2 and use newly created configuration
    // - change configuration status to "Ready to Use"
    public void testUsecase13() throws IOException {
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
    // - test Views API
    public void testUsecase15() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new GetViews(),
                new GetViewPage(),
                new GetOrientationPage(),
                new GetOrientations());

        final List<View> views = restService
                .getBuilder(GetViews.class)
                .call()
                .getOrThrow();

        assertNotNull(views);
        assertTrue(views.size() == 11);

        final List<Orientation> orientations = restService
                .getBuilder(GetOrientations.class)
                .call()
                .getOrThrow();

        assertNotNull(orientations);
    }

}
