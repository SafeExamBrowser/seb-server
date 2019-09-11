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

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.jdbc.Sql;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.SEB_CLIENT_CONFIGURATION;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SebClientConfig;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ActivateExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.DeactivateExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigAttributes;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNode;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNodePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetFollowupConfiguration;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetOrientationPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetOrientations;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetViewList;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.NewExamConfig;
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

    @BeforeAll
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
    public void init() {

    }

    @AfterAll
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
        assertEquals("Demo Quiz 1", quizData.name);

        // import quiz as exam
        final Result<Exam> newExamResult = restService
                .getBuilder(ImportAsExam.class)
                .withFormParam(QuizData.QUIZ_ATTR_LMS_SETUP_ID, String.valueOf(quizData.lmsSetupId))
                .withFormParam(QuizData.QUIZ_ATTR_ID, quizData.id)
                .call();

        assertNotNull(newExamResult);
        assertFalse(newExamResult.hasError());
        final Exam newExam = newExamResult.get();

        assertEquals("Demo Quiz 1", newExam.name);
        assertEquals(ExamType.UNDEFINED, newExam.type);
        assertTrue(newExam.supporter.isEmpty());

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
                null, null, null,
                Utils.immutableCollectionOf(userId),
                true);

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
        assertEquals("Demo Quiz 1", examName.name);

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
//        assertEquals(
//                "AttributeMapping [templateId=0, attributeIdMapping={256=ConfigurationAttribute [id=256, parentId=220, name=SOCKSPort, type=INTEGER, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=1080], 512=ConfigurationAttribute [id=512, parentId=null, name=enableF4, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 1=ConfigurationAttribute [id=1, parentId=null, name=hashedAdminPassword, type=PASSWORD_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 257=ConfigurationAttribute [id=257, parentId=220, name=SOCKSRequiresPassword, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=false], 513=ConfigurationAttribute [id=513, parentId=null, name=enableF5, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 2=ConfigurationAttribute [id=2, parentId=null, name=allowQuit, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 258=ConfigurationAttribute [id=258, parentId=220, name=SOCKSUsername, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=null], 514=ConfigurationAttribute [id=514, parentId=null, name=enableF6, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 3=ConfigurationAttribute [id=3, parentId=null, name=ignoreExitKeys, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 259=ConfigurationAttribute [id=259, parentId=220, name=SOCKSPassword, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=null], 515=ConfigurationAttribute [id=515, parentId=null, name=enableF7, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 4=ConfigurationAttribute [id=4, parentId=null, name=hashedQuitPassword, type=PASSWORD_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 260=ConfigurationAttribute [id=260, parentId=220, name=RTSPEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=false], 516=ConfigurationAttribute [id=516, parentId=null, name=enableF8, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 5=ConfigurationAttribute [id=5, parentId=null, name=exitKey1, type=SINGLE_SELECTION, resources=0,1,2,3,4,5,6,7,8,9,10,11, validator=ExitKeySequenceValidator, dependencies=resourceLocTextKey=sebserver.examconfig.props.label.exitKey, defaultValue=2], 261=ConfigurationAttribute [id=261, parentId=220, name=RTSPProxy, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=null], 517=ConfigurationAttribute [id=517, parentId=null, name=enableF9, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 6=ConfigurationAttribute [id=6, parentId=null, name=exitKey2, type=SINGLE_SELECTION, resources=0,1,2,3,4,5,6,7,8,9,10,11, validator=ExitKeySequenceValidator, dependencies=resourceLocTextKey=sebserver.examconfig.props.label.exitKey, defaultValue=10], 262=ConfigurationAttribute [id=262, parentId=220, name=RTSPPort, type=INTEGER, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=554], 518=ConfigurationAttribute [id=518, parentId=null, name=enableF10, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 7=ConfigurationAttribute [id=7, parentId=null, name=exitKey3, type=SINGLE_SELECTION, resources=0,1,2,3,4,5,6,7,8,9,10,11, validator=ExitKeySequenceValidator, dependencies=resourceLocTextKey=sebserver.examconfig.props.label.exitKey, defaultValue=5], 263=ConfigurationAttribute [id=263, parentId=220, name=RTSPRequiresPassword, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=false], 519=ConfigurationAttribute [id=519, parentId=null, name=enableF11, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 8=ConfigurationAttribute [id=8, parentId=null, name=browserViewMode, type=RADIO_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=0], 264=ConfigurationAttribute [id=264, parentId=220, name=RTSPUsername, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=null], 520=ConfigurationAttribute [id=520, parentId=null, name=enableF12, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 9=ConfigurationAttribute [id=9, parentId=null, name=enableTouchExit, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 265=ConfigurationAttribute [id=265, parentId=220, name=RTSPPassword, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=null], 10=ConfigurationAttribute [id=10, parentId=null, name=mainBrowserWindowWidth, type=COMBO_SELECTION, resources=50%,100%,800,1000, validator=WindowsSizeValidator, dependencies=null, defaultValue=100%], 11=ConfigurationAttribute [id=11, parentId=null, name=mainBrowserWindowHeight, type=COMBO_SELECTION, resources=80%,100%,600,800, validator=WindowsSizeValidator, dependencies=null, defaultValue=100%], 12=ConfigurationAttribute [id=12, parentId=null, name=mainBrowserWindowPositioning, type=SINGLE_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=1], 13=ConfigurationAttribute [id=13, parentId=null, name=enableBrowserWindowToolbar, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 14=ConfigurationAttribute [id=14, parentId=null, name=hideBrowserWindowToolbar, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 15=ConfigurationAttribute [id=15, parentId=null, name=showMenuBar, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 16=ConfigurationAttribute [id=16, parentId=null, name=showTaskBar, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 17=ConfigurationAttribute [id=17, parentId=null, name=taskBarHeight, type=COMBO_SELECTION, resources=40,60,80, validator=IntegerTypeValidator, dependencies=null, defaultValue=40], 18=ConfigurationAttribute [id=18, parentId=null, name=showReloadButton, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 19=ConfigurationAttribute [id=19, parentId=null, name=showTime, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 20=ConfigurationAttribute [id=20, parentId=null, name=showInputLanguage, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 21=ConfigurationAttribute [id=21, parentId=null, name=enableZoomPage, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 22=ConfigurationAttribute [id=22, parentId=null, name=enableZoomText, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 23=ConfigurationAttribute [id=23, parentId=null, name=zoomMode, type=RADIO_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0], 24=ConfigurationAttribute [id=24, parentId=null, name=audioControlEnabled, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 25=ConfigurationAttribute [id=25, parentId=null, name=audioMute, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 26=ConfigurationAttribute [id=26, parentId=null, name=audioSetVolumeLevel, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 27=ConfigurationAttribute [id=27, parentId=null, name=audioVolumeLevel, type=SLIDER, resources=0,100, validator=null, dependencies=null, defaultValue=25], 28=ConfigurationAttribute [id=28, parentId=null, name=allowSpellCheck, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 29=ConfigurationAttribute [id=29, parentId=null, name=allowDictionaryLookup, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 30=ConfigurationAttribute [id=30, parentId=null, name=allowSpellCheckDictionary, type=MULTI_CHECKBOX_SELECTION, resources=da-DK,en-AU,en-GB,en-US,es-ES,fr-FR,pt-PT,sv-SE,sv-FI, validator=null, dependencies=null, defaultValue=da-DK,en-AU,en-GB,en-US,es-ES,fr-FR,pt-PT,sv-SE,sv-FI], 31=ConfigurationAttribute [id=31, parentId=null, name=newBrowserWindowByLinkPolicy, type=RADIO_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=2], 32=ConfigurationAttribute [id=32, parentId=null, name=newBrowserWindowByLinkBlockForeign, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 33=ConfigurationAttribute [id=33, parentId=null, name=newBrowserWindowByLinkWidth, type=COMBO_SELECTION, resources=50%,100%,800,1000, validator=WindowsSizeValidator, dependencies=null, defaultValue=100%], 34=ConfigurationAttribute [id=34, parentId=null, name=newBrowserWindowByLinkHeight, type=COMBO_SELECTION, resources=80%,100%,600,800, validator=WindowsSizeValidator, dependencies=null, defaultValue=100%], 35=ConfigurationAttribute [id=35, parentId=null, name=newBrowserWindowByLinkPositioning, type=SINGLE_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=2], 36=ConfigurationAttribute [id=36, parentId=null, name=enablePlugIns, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 37=ConfigurationAttribute [id=37, parentId=null, name=enableJavaScript, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 38=ConfigurationAttribute [id=38, parentId=null, name=enableJava, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 39=ConfigurationAttribute [id=39, parentId=null, name=blockPopUpWindows, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 40=ConfigurationAttribute [id=40, parentId=null, name=allowVideoCapture, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 41=ConfigurationAttribute [id=41, parentId=null, name=allowAudioCapture, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 42=ConfigurationAttribute [id=42, parentId=null, name=allowBrowsingBackForward, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 43=ConfigurationAttribute [id=43, parentId=null, name=newBrowserWindowNavigation, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 44=ConfigurationAttribute [id=44, parentId=null, name=browserWindowAllowReload, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 300=ConfigurationAttribute [id=300, parentId=null, name=sebServicePolicy, type=RADIO_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=2], 45=ConfigurationAttribute [id=45, parentId=null, name=newBrowserWindowAllowReload, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 301=ConfigurationAttribute [id=301, parentId=null, name=kioskMode, type=RADIO_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=0], 46=ConfigurationAttribute [id=46, parentId=null, name=showReloadWarning, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 302=ConfigurationAttribute [id=302, parentId=null, name=allowVirtualMachine, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 47=ConfigurationAttribute [id=47, parentId=null, name=newBrowserWindowShowReloadWarning, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 303=ConfigurationAttribute [id=303, parentId=null, name=allowScreenSharing, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 48=ConfigurationAttribute [id=48, parentId=null, name=removeBrowserProfile, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 304=ConfigurationAttribute [id=304, parentId=null, name=enablePrivateClipboard, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 49=ConfigurationAttribute [id=49, parentId=null, name=removeLocalStorage, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 305=ConfigurationAttribute [id=305, parentId=null, name=enableLogging, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 50=ConfigurationAttribute [id=50, parentId=null, name=browserUserAgent, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 306=ConfigurationAttribute [id=306, parentId=null, name=logDirectoryWin, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 51=ConfigurationAttribute [id=51, parentId=null, name=browserUserAgentWinDesktopMode, type=RADIO_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0], 307=ConfigurationAttribute [id=307, parentId=null, name=logDirectoryOSX, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=NSTemporaryDirectory], 52=ConfigurationAttribute [id=52, parentId=null, name=browserUserAgentWinDesktopModeCustom, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 308=ConfigurationAttribute [id=308, parentId=null, name=minMacOSVersion, type=SINGLE_SELECTION, resources=0,1,2,3,4,5,6,7, validator=null, dependencies=null, defaultValue=0], 53=ConfigurationAttribute [id=53, parentId=null, name=browserUserAgentWinTouchMode, type=RADIO_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=0], 309=ConfigurationAttribute [id=309, parentId=null, name=enableAppSwitcherCheck, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 54=ConfigurationAttribute [id=54, parentId=null, name=browserUserAgentWinTouchModeCustom, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 310=ConfigurationAttribute [id=310, parentId=null, name=forceAppFolderInstall, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 55=ConfigurationAttribute [id=55, parentId=null, name=browserUserAgentMac, type=RADIO_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0], 311=ConfigurationAttribute [id=311, parentId=null, name=allowUserAppFolderInstall, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 56=ConfigurationAttribute [id=56, parentId=null, name=browserUserAgentMacCustom, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 312=ConfigurationAttribute [id=312, parentId=null, name=allowSiri, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 57=ConfigurationAttribute [id=57, parentId=null, name=enableSebBrowser, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 313=ConfigurationAttribute [id=313, parentId=null, name=detectStoppedProcess, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 58=ConfigurationAttribute [id=58, parentId=null, name=browserWindowTitleSuffix, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 314=ConfigurationAttribute [id=314, parentId=null, name=allowDisplayMirroring, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 59=ConfigurationAttribute [id=59, parentId=null, name=allowDownUploads, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 315=ConfigurationAttribute [id=315, parentId=null, name=allowedDisplaysMaxNumber, type=COMBO_SELECTION, resources=1,2,3, validator=null, dependencies=null, defaultValue=1], 60=ConfigurationAttribute [id=60, parentId=null, name=downloadDirectoryWin, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 316=ConfigurationAttribute [id=316, parentId=null, name=allowedDisplayBuiltin, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 61=ConfigurationAttribute [id=61, parentId=null, name=downloadDirectoryOSX, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 317=ConfigurationAttribute [id=317, parentId=null, name=logLevel, type=SINGLE_SELECTION, resources=0,1,2,3,4, validator=null, dependencies=null, defaultValue=1], 62=ConfigurationAttribute [id=62, parentId=null, name=openDownloads, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 63=ConfigurationAttribute [id=63, parentId=null, name=chooseFileToUploadPolicy, type=RADIO_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=0], 64=ConfigurationAttribute [id=64, parentId=null, name=downloadPDFFiles, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 65=ConfigurationAttribute [id=65, parentId=null, name=allowPDFPlugIn, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 66=ConfigurationAttribute [id=66, parentId=null, name=downloadAndOpenSebConfig, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 67=ConfigurationAttribute [id=67, parentId=null, name=quitURL, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 68=ConfigurationAttribute [id=68, parentId=null, name=quitURLConfirm, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 69=ConfigurationAttribute [id=69, parentId=null, name=restartExamUseStartURL, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 70=ConfigurationAttribute [id=70, parentId=null, name=restartExamURL, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 71=ConfigurationAttribute [id=71, parentId=null, name=restartExamText, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], 72=ConfigurationAttribute [id=72, parentId=null, name=restartExamPasswordProtected, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 73=ConfigurationAttribute [id=73, parentId=null, name=permittedProcesses, type=TABLE, resources=null, validator=null, dependencies=null, defaultValue=null], 74=ConfigurationAttribute [id=74, parentId=73, name=permittedProcesses.active, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 75=ConfigurationAttribute [id=75, parentId=73, name=permittedProcesses.os, type=SINGLE_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0], 76=ConfigurationAttribute [id=76, parentId=73, name=permittedProcesses.title, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 77=ConfigurationAttribute [id=77, parentId=73, name=permittedProcesses.description, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 78=ConfigurationAttribute [id=78, parentId=73, name=permittedProcesses.executable, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 79=ConfigurationAttribute [id=79, parentId=73, name=permittedProcesses.originalName, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 80=ConfigurationAttribute [id=80, parentId=73, name=permittedProcesses.allowedExecutables, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 81=ConfigurationAttribute [id=81, parentId=73, name=permittedProcesses.path, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 82=ConfigurationAttribute [id=82, parentId=73, name=permittedProcesses.arguments, type=INLINE_TABLE, resources=1:active:CHECKBOX|4:argument:TEXT_FIELD, validator=null, dependencies=null, defaultValue=null], 85=ConfigurationAttribute [id=85, parentId=73, name=permittedProcesses.identifier, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 86=ConfigurationAttribute [id=86, parentId=73, name=permittedProcesses.iconInTaskbar, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 87=ConfigurationAttribute [id=87, parentId=73, name=permittedProcesses.autostart, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 88=ConfigurationAttribute [id=88, parentId=73, name=permittedProcesses.runInBackground, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 89=ConfigurationAttribute [id=89, parentId=73, name=permittedProcesses.allowUserToChooseApp, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 90=ConfigurationAttribute [id=90, parentId=73, name=permittedProcesses.strongKill, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 91=ConfigurationAttribute [id=91, parentId=null, name=allowSwitchToApplications, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 92=ConfigurationAttribute [id=92, parentId=null, name=allowFlashFullscreen, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 93=ConfigurationAttribute [id=93, parentId=null, name=prohibitedProcesses, type=TABLE, resources=null, validator=null, dependencies=null, defaultValue=null], 94=ConfigurationAttribute [id=94, parentId=93, name=prohibitedProcesses.active, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 95=ConfigurationAttribute [id=95, parentId=93, name=prohibitedProcesses.os, type=SINGLE_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0], 96=ConfigurationAttribute [id=96, parentId=93, name=prohibitedProcesses.executable, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 97=ConfigurationAttribute [id=97, parentId=93, name=prohibitedProcesses.description, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 98=ConfigurationAttribute [id=98, parentId=93, name=prohibitedProcesses.originalName, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 99=ConfigurationAttribute [id=99, parentId=93, name=prohibitedProcesses.identifier, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 100=ConfigurationAttribute [id=100, parentId=93, name=prohibitedProcesses.strongKill, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 400=ConfigurationAttribute [id=400, parentId=null, name=insideSebEnableSwitchUser, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 401=ConfigurationAttribute [id=401, parentId=null, name=insideSebEnableLockThisComputer, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 402=ConfigurationAttribute [id=402, parentId=null, name=insideSebEnableChangeAPassword, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 403=ConfigurationAttribute [id=403, parentId=null, name=insideSebEnableStartTaskManager, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 404=ConfigurationAttribute [id=404, parentId=null, name=insideSebEnableLogOff, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 405=ConfigurationAttribute [id=405, parentId=null, name=insideSebEnableShutDown, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 406=ConfigurationAttribute [id=406, parentId=null, name=insideSebEnableEaseOfAccess, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 407=ConfigurationAttribute [id=407, parentId=null, name=insideSebEnableVmWareClientShade, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 408=ConfigurationAttribute [id=408, parentId=null, name=insideSebEnableNetworkConnectionSelector, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 200=ConfigurationAttribute [id=200, parentId=null, name=URLFilterEnable, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 201=ConfigurationAttribute [id=201, parentId=null, name=URLFilterEnableContentFilter, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 202=ConfigurationAttribute [id=202, parentId=null, name=URLFilterRules, type=TABLE, resources=null, validator=null, dependencies=null, defaultValue=null], 203=ConfigurationAttribute [id=203, parentId=202, name=URLFilterRules.active, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 204=ConfigurationAttribute [id=204, parentId=202, name=URLFilterRules.regex, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 205=ConfigurationAttribute [id=205, parentId=202, name=URLFilterRules.expression, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], 206=ConfigurationAttribute [id=206, parentId=202, name=URLFilterRules.action, type=SINGLE_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=], 210=ConfigurationAttribute [id=210, parentId=null, name=proxySettingsPolicy, type=RADIO_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0], 220=ConfigurationAttribute [id=220, parentId=null, name=proxies, type=COMPOSITE_TABLE, resources=active,TABLE_ENTRY|autoDiscovery,autoConfiguration,http,https,ftp,socks,rtsp, validator=null, dependencies=null, defaultValue=null], 221=ConfigurationAttribute [id=221, parentId=220, name=ExcludeSimpleHostnames, type=CHECKBOX, resources=null, validator=null, dependencies=showInView=true,createDefaultValue=true, defaultValue=false], 222=ConfigurationAttribute [id=222, parentId=220, name=ExceptionsList, type=TEXT_AREA, resources=null, validator=null, dependencies=showInView=true,createDefaultValue=true, defaultValue=null], 223=ConfigurationAttribute [id=223, parentId=220, name=FTPPassive, type=CHECKBOX, resources=null, validator=null, dependencies=showInView=true,createDefaultValue=true, defaultValue=true], 231=ConfigurationAttribute [id=231, parentId=220, name=AutoDiscoveryEnabled, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=autoDiscovery,createDefaultValue=true, defaultValue=false], 233=ConfigurationAttribute [id=233, parentId=220, name=AutoConfigurationEnabled, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=autoConfiguration,createDefaultValue=true, defaultValue=false], 234=ConfigurationAttribute [id=234, parentId=220, name=AutoConfigurationURL, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=autoConfiguration,createDefaultValue=true, defaultValue=null], 235=ConfigurationAttribute [id=235, parentId=220, name=AutoConfigurationJavaScript, type=TEXT_AREA, resources=null, validator=null, dependencies=groupId=autoConfiguration,createDefaultValue=true, defaultValue=null], 236=ConfigurationAttribute [id=236, parentId=220, name=HTTPEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=false], 237=ConfigurationAttribute [id=237, parentId=220, name=HTTPProxy, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=null], 238=ConfigurationAttribute [id=238, parentId=220, name=HTTPPort, type=INTEGER, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=80], 239=ConfigurationAttribute [id=239, parentId=220, name=HTTPRequiresPassword, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=false], 240=ConfigurationAttribute [id=240, parentId=220, name=HTTPUsername, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=null], 241=ConfigurationAttribute [id=241, parentId=220, name=HTTPPassword, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=null], 242=ConfigurationAttribute [id=242, parentId=220, name=HTTPSEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=false], 243=ConfigurationAttribute [id=243, parentId=220, name=HTTPSProxy, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=null], 244=ConfigurationAttribute [id=244, parentId=220, name=HTTPSPort, type=INTEGER, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=443], 500=ConfigurationAttribute [id=500, parentId=null, name=enableEsc, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 245=ConfigurationAttribute [id=245, parentId=220, name=HTTPSRequiresPassword, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=false], 501=ConfigurationAttribute [id=501, parentId=null, name=enablePrintScreen, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 246=ConfigurationAttribute [id=246, parentId=220, name=HTTPSUsername, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=null], 502=ConfigurationAttribute [id=502, parentId=null, name=enableCtrlEsc, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 247=ConfigurationAttribute [id=247, parentId=220, name=HTTPSPassword, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=null], 503=ConfigurationAttribute [id=503, parentId=null, name=enableAltEsc, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 248=ConfigurationAttribute [id=248, parentId=220, name=FTPEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=false], 504=ConfigurationAttribute [id=504, parentId=null, name=enableAltTab, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], 249=ConfigurationAttribute [id=249, parentId=220, name=FTPProxy, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=null], 505=ConfigurationAttribute [id=505, parentId=null, name=enableAltF4, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 250=ConfigurationAttribute [id=250, parentId=220, name=FTPPort, type=INTEGER, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=21], 506=ConfigurationAttribute [id=506, parentId=null, name=enableStartMenu, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 251=ConfigurationAttribute [id=251, parentId=220, name=FTPRequiresPassword, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=false], 507=ConfigurationAttribute [id=507, parentId=null, name=enableRightMouse, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 252=ConfigurationAttribute [id=252, parentId=220, name=FTPUsername, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=null], 508=ConfigurationAttribute [id=508, parentId=null, name=enableAltMouseWheel, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 253=ConfigurationAttribute [id=253, parentId=220, name=FTPPassword, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=null], 509=ConfigurationAttribute [id=509, parentId=null, name=enableF1, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 254=ConfigurationAttribute [id=254, parentId=220, name=SOCKSEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=false], 510=ConfigurationAttribute [id=510, parentId=null, name=enableF2, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], 255=ConfigurationAttribute [id=255, parentId=220, name=SOCKSProxy, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=null], 511=ConfigurationAttribute [id=511, parentId=null, name=enableF3, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false]}, attributeNameIdMapping={HTTPSPort=244, browserUserAgentWinTouchModeCustom=54, newBrowserWindowShowReloadWarning=47, HTTPProxy=237, permittedProcesses.autostart=87, permittedProcesses.runInBackground=88, enableAltMouseWheel=508, permittedProcesses=73, showTaskBar=16, SOCKSProxy=255, allowQuit=2, proxySettingsPolicy=210, ignoreExitKeys=3, permittedProcesses.strongKill=90, allowScreenSharing=303, forceAppFolderInstall=310, proxies=220, RTSPProxy=261, downloadDirectoryWin=60, AutoConfigurationURL=234, prohibitedProcesses.description=97, insideSebEnableLockThisComputer=401, permittedProcesses.allowedExecutables=80, taskBarHeight=17, enablePlugIns=36, restartExamText=71, SOCKSEnable=254, kioskMode=301, enableAltEsc=503, enableCtrlEsc=502, minMacOSVersion=308, browserWindowAllowReload=44, allowDisplayMirroring=314, allowDownUploads=59, prohibitedProcesses.originalName=98, enableBrowserWindowToolbar=13, HTTPSPassword=247, FTPUsername=252, RTSPUsername=264, allowUserAppFolderInstall=311, newBrowserWindowByLinkHeight=34, newBrowserWindowAllowReload=45, insideSebEnableStartTaskManager=403, audioMute=25, FTPPort=250, AutoDiscoveryEnabled=231, newBrowserWindowByLinkPolicy=31, ExceptionsList=222, browserViewMode=8, enablePrintScreen=501, permittedProcesses.description=77, allowSwitchToApplications=91, SOCKSRequiresPassword=257, allowVideoCapture=40, prohibitedProcesses.os=95, HTTPRequiresPassword=239, RTSPPassword=265, enableAppSwitcherCheck=309, HTTPSProxy=243, logLevel=317, quitURLConfirm=68, restartExamURL=70, prohibitedProcesses.active=94, newBrowserWindowByLinkBlockForeign=32, RTSPEnable=260, allowedDisplaysMaxNumber=315, FTPPassive=223, FTPProxy=249, permittedProcesses.active=74, enableZoomText=22, mainBrowserWindowWidth=10, enableLogging=305, removeLocalStorage=49, newBrowserWindowByLinkPositioning=35, permittedProcesses.iconInTaskbar=86, downloadPDFFiles=64, enableAltF4=505, allowVirtualMachine=302, enableRightMouse=507, exitKey2=6, exitKey1=5, exitKey3=7, showInputLanguage=20, prohibitedProcesses.identifier=99, URLFilterEnable=200, SOCKSPassword=259, newBrowserWindowByLinkWidth=33, permittedProcesses.originalName=79, URLFilterEnableContentFilter=201, enableF2=510, permittedProcesses.title=76, enableF1=509, URLFilterRules=202, enableF4=512, SOCKSPort=256, enableF3=511, enableF6=514, enableF5=513, enableF8=516, URLFilterRules.active=203, FTPPassword=253, insideSebEnableLogOff=404, enableF7=515, mainBrowserWindowHeight=11, enableJava=38, showReloadWarning=46, prohibitedProcesses=93, enableZoomPage=21, prohibitedProcesses.executable=96, HTTPSUsername=246, enableF9=517, browserUserAgentMac=55, RTSPPort=262, audioControlEnabled=24, browserUserAgentWinDesktopModeCustom=52, AutoConfigurationJavaScript=235, HTTPPassword=241, ExcludeSimpleHostnames=221, allowedDisplayBuiltin=316, showTime=19, zoomMode=23, FTPRequiresPassword=251, newBrowserWindowNavigation=43, enableTouchExit=9, RTSPRequiresPassword=263, blockPopUpWindows=39, enableEsc=500, showMenuBar=15, hideBrowserWindowToolbar=14, browserWindowTitleSuffix=58, mainBrowserWindowPositioning=12, insideSebEnableVmWareClientShade=407, logDirectoryOSX=307, openDownloads=62, HTTPEnable=236, chooseFileToUploadPolicy=63, enablePrivateClipboard=304, permittedProcesses.identifier=85, URLFilterRules.regex=204, allowFlashFullscreen=92, downloadDirectoryOSX=61, showReloadButton=18, removeBrowserProfile=48, insideSebEnableEaseOfAccess=406, HTTPSRequiresPassword=245, enableAltTab=504, insideSebEnableSwitchUser=400, insideSebEnableNetworkConnectionSelector=408, allowDictionaryLookup=29, browserUserAgentWinDesktopMode=51, allowAudioCapture=41, permittedProcesses.path=81, allowBrowsingBackForward=42, insideSebEnableShutDown=405, URLFilterRules.expression=205, permittedProcesses.allowUserToChooseApp=89, sebServicePolicy=300, SOCKSUsername=258, allowSiri=312, enableF11=519, enableF10=518, allowSpellCheck=28, enableJavaScript=37, permittedProcesses.arguments=82, insideSebEnableChangeAPassword=402, permittedProcesses.os=75, enableF12=520, restartExamUseStartURL=69, permittedProcesses.executable=78, FTPEnable=248, downloadAndOpenSebConfig=66, enableStartMenu=506, quitURL=67, URLFilterRules.action=206, audioSetVolumeLevel=26, logDirectoryWin=306, allowSpellCheckDictionary=30, restartExamPasswordProtected=72, hashedAdminPassword=1, browserUserAgentWinTouchMode=53, prohibitedProcesses.strongKill=100, hashedQuitPassword=4, HTTPPort=238, browserUserAgentMacCustom=56, browserUserAgent=50, AutoConfigurationEnabled=233, audioVolumeLevel=27, HTTPSEnable=242, enableSebBrowser=57, detectStoppedProcess=313, allowPDFPlugIn=65, HTTPUsername=240}, orientationAttributeMapping={256=Orientation [id=256, attributeId=256, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], 512=Orientation [id=512, attributeId=512, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=4, width=3, height=1, title=NONE], 1=Orientation [id=1, attributeId=1, templateId=0, viewId=1, groupId=null, xPosition=1, yPosition=1, width=1, height=2, title=LEFT], 257=Orientation [id=257, attributeId=257, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], 513=Orientation [id=513, attributeId=513, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=5, width=3, height=1, title=NONE], 2=Orientation [id=2, attributeId=2, templateId=0, viewId=1, groupId=null, xPosition=1, yPosition=3, width=1, height=1, title=LEFT], 258=Orientation [id=258, attributeId=258, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT], 514=Orientation [id=514, attributeId=514, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=6, width=3, height=1, title=NONE], 3=Orientation [id=3, attributeId=3, templateId=0, viewId=1, groupId=null, xPosition=1, yPosition=4, width=1, height=1, title=LEFT], 259=Orientation [id=259, attributeId=259, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], 515=Orientation [id=515, attributeId=515, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=7, width=3, height=1, title=NONE], 4=Orientation [id=4, attributeId=4, templateId=0, viewId=1, groupId=null, xPosition=1, yPosition=5, width=1, height=2, title=LEFT], 260=Orientation [id=260, attributeId=260, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], 516=Orientation [id=516, attributeId=516, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=8, width=3, height=1, title=NONE], 5=Orientation [id=5, attributeId=5, templateId=0, viewId=1, groupId=exitSequence, xPosition=2, yPosition=1, width=1, height=1, title=NONE], 261=Orientation [id=261, attributeId=261, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], 517=Orientation [id=517, attributeId=517, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=9, width=3, height=1, title=NONE], 6=Orientation [id=6, attributeId=6, templateId=0, viewId=1, groupId=exitSequence, xPosition=2, yPosition=2, width=1, height=1, title=NONE], 262=Orientation [id=262, attributeId=262, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], 518=Orientation [id=518, attributeId=518, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=10, width=3, height=1, title=NONE], 7=Orientation [id=7, attributeId=7, templateId=0, viewId=1, groupId=exitSequence, xPosition=2, yPosition=3, width=1, height=1, title=NONE], 263=Orientation [id=263, attributeId=263, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], 519=Orientation [id=519, attributeId=519, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=11, width=3, height=1, title=NONE], 8=Orientation [id=8, attributeId=8, templateId=0, viewId=2, groupId=browserViewMode, xPosition=0, yPosition=0, width=3, height=3, title=NONE], 264=Orientation [id=264, attributeId=264, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT], 520=Orientation [id=520, attributeId=520, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=12, width=3, height=1, title=NONE], 9=Orientation [id=9, attributeId=9, templateId=0, viewId=2, groupId=browserViewMode, xPosition=3, yPosition=2, width=4, height=1, title=NONE], 265=Orientation [id=265, attributeId=265, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], 10=Orientation [id=10, attributeId=10, templateId=0, viewId=2, groupId=winsize, xPosition=1, yPosition=4, width=2, height=1, title=LEFT], 11=Orientation [id=11, attributeId=11, templateId=0, viewId=2, groupId=winsize, xPosition=1, yPosition=5, width=2, height=1, title=LEFT], 12=Orientation [id=12, attributeId=12, templateId=0, viewId=2, groupId=winsize, xPosition=5, yPosition=4, width=2, height=1, title=LEFT_SPAN], 13=Orientation [id=13, attributeId=13, templateId=0, viewId=2, groupId=wintoolbar, xPosition=0, yPosition=6, width=3, height=1, title=NONE], 14=Orientation [id=14, attributeId=14, templateId=0, viewId=2, groupId=wintoolbar, xPosition=3, yPosition=6, width=4, height=1, title=NONE], 15=Orientation [id=15, attributeId=15, templateId=0, viewId=2, groupId=wintoolbar, xPosition=0, yPosition=7, width=3, height=1, title=NONE], 16=Orientation [id=16, attributeId=16, templateId=0, viewId=2, groupId=taskbar, xPosition=0, yPosition=9, width=3, height=1, title=NONE], 17=Orientation [id=17, attributeId=17, templateId=0, viewId=2, groupId=taskbar, xPosition=5, yPosition=9, width=2, height=1, title=LEFT_SPAN], 18=Orientation [id=18, attributeId=18, templateId=0, viewId=2, groupId=taskbar, xPosition=0, yPosition=10, width=3, height=1, title=NONE], 19=Orientation [id=19, attributeId=19, templateId=0, viewId=2, groupId=taskbar, xPosition=0, yPosition=11, width=3, height=1, title=NONE], 20=Orientation [id=20, attributeId=20, templateId=0, viewId=2, groupId=taskbar, xPosition=0, yPosition=12, width=3, height=1, title=NONE], 21=Orientation [id=21, attributeId=21, templateId=0, viewId=2, groupId=zoom, xPosition=0, yPosition=14, width=3, height=1, title=NONE], 22=Orientation [id=22, attributeId=22, templateId=0, viewId=2, groupId=zoom, xPosition=0, yPosition=15, width=3, height=1, title=NONE], 23=Orientation [id=23, attributeId=23, templateId=0, viewId=2, groupId=zoomMode, xPosition=3, yPosition=14, width=4, height=1, title=NONE], 24=Orientation [id=24, attributeId=24, templateId=0, viewId=2, groupId=audio, xPosition=7, yPosition=0, width=5, height=1, title=NONE], 25=Orientation [id=25, attributeId=25, templateId=0, viewId=2, groupId=audio, xPosition=7, yPosition=1, width=5, height=1, title=NONE], 26=Orientation [id=26, attributeId=26, templateId=0, viewId=2, groupId=audio, xPosition=7, yPosition=2, width=5, height=1, title=NONE], 27=Orientation [id=27, attributeId=27, templateId=0, viewId=2, groupId=audio, xPosition=7, yPosition=3, width=5, height=1, title=NONE], 28=Orientation [id=28, attributeId=28, templateId=0, viewId=2, groupId=spellcheck, xPosition=7, yPosition=4, width=5, height=1, title=NONE], 29=Orientation [id=29, attributeId=29, templateId=0, viewId=2, groupId=spellcheck, xPosition=7, yPosition=5, width=5, height=1, title=NONE], 30=Orientation [id=30, attributeId=30, templateId=0, viewId=2, groupId=spellcheck, xPosition=7, yPosition=7, width=5, height=9, title=TOP], 31=Orientation [id=31, attributeId=31, templateId=0, viewId=3, groupId=newBrowserWindow, xPosition=0, yPosition=0, width=3, height=3, title=NONE], 32=Orientation [id=32, attributeId=32, templateId=0, viewId=3, groupId=newBrowserWindow, xPosition=4, yPosition=0, width=3, height=1, title=NONE], 33=Orientation [id=33, attributeId=33, templateId=0, viewId=3, groupId=newwinsize, xPosition=1, yPosition=4, width=2, height=1, title=LEFT], 34=Orientation [id=34, attributeId=34, templateId=0, viewId=3, groupId=newwinsize, xPosition=1, yPosition=5, width=2, height=1, title=LEFT], 35=Orientation [id=35, attributeId=35, templateId=0, viewId=3, groupId=newwinsize, xPosition=5, yPosition=4, width=2, height=1, title=LEFT_SPAN], 36=Orientation [id=36, attributeId=36, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=5, width=4, height=1, title=NONE], 37=Orientation [id=37, attributeId=37, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=5, width=3, height=1, title=NONE], 38=Orientation [id=38, attributeId=38, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=6, width=4, height=1, title=NONE], 39=Orientation [id=39, attributeId=39, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=6, width=3, height=1, title=NONE], 40=Orientation [id=40, attributeId=40, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=7, width=4, height=1, title=NONE], 41=Orientation [id=41, attributeId=41, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=7, width=3, height=1, title=NONE], 42=Orientation [id=42, attributeId=42, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=8, width=4, height=1, title=NONE], 43=Orientation [id=43, attributeId=43, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=8, width=3, height=1, title=NONE], 44=Orientation [id=44, attributeId=44, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=9, width=4, height=1, title=NONE], 300=Orientation [id=300, attributeId=300, templateId=0, viewId=9, groupId=servicePolicy, xPosition=0, yPosition=0, width=4, height=3, title=NONE], 45=Orientation [id=45, attributeId=45, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=9, width=3, height=1, title=NONE], 301=Orientation [id=301, attributeId=301, templateId=0, viewId=9, groupId=kioskMode, xPosition=4, yPosition=0, width=3, height=3, title=NONE], 46=Orientation [id=46, attributeId=46, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=10, width=4, height=1, title=NONE], 302=Orientation [id=302, attributeId=302, templateId=0, viewId=9, groupId=null, xPosition=0, yPosition=5, width=4, height=1, title=NONE], 47=Orientation [id=47, attributeId=47, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=10, width=3, height=1, title=NONE], 303=Orientation [id=303, attributeId=303, templateId=0, viewId=9, groupId=null, xPosition=0, yPosition=6, width=4, height=1, title=NONE], 48=Orientation [id=48, attributeId=48, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=11, width=4, height=1, title=NONE], 304=Orientation [id=304, attributeId=304, templateId=0, viewId=9, groupId=null, xPosition=4, yPosition=5, width=3, height=1, title=NONE], 49=Orientation [id=49, attributeId=49, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=11, width=3, height=1, title=NONE], 305=Orientation [id=305, attributeId=305, templateId=0, viewId=9, groupId=logging, xPosition=0, yPosition=8, width=6, height=1, title=NONE], 50=Orientation [id=50, attributeId=50, templateId=0, viewId=3, groupId=null, xPosition=7, yPosition=1, width=5, height=1, title=TOP], 306=Orientation [id=306, attributeId=306, templateId=0, viewId=9, groupId=logging, xPosition=3, yPosition=9, width=4, height=1, title=LEFT_SPAN], 51=Orientation [id=51, attributeId=51, templateId=0, viewId=3, groupId=userAgentDesktop, xPosition=7, yPosition=2, width=5, height=2, title=NONE], 307=Orientation [id=307, attributeId=307, templateId=0, viewId=9, groupId=logging, xPosition=3, yPosition=10, width=4, height=1, title=LEFT_SPAN], 52=Orientation [id=52, attributeId=52, templateId=0, viewId=3, groupId=userAgentDesktop, xPosition=7, yPosition=3, width=5, height=1, title=NONE], 308=Orientation [id=308, attributeId=308, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=1, width=5, height=1, title=TOP], 53=Orientation [id=53, attributeId=53, templateId=0, viewId=3, groupId=userAgentTouch, xPosition=7, yPosition=4, width=5, height=3, title=NONE], 309=Orientation [id=309, attributeId=309, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=2, width=5, height=1, title=NONE], 54=Orientation [id=54, attributeId=54, templateId=0, viewId=3, groupId=userAgentTouch, xPosition=7, yPosition=8, width=5, height=1, title=NONE], 310=Orientation [id=310, attributeId=310, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=3, width=5, height=1, title=NONE], 55=Orientation [id=55, attributeId=55, templateId=0, viewId=3, groupId=userAgentMac, xPosition=7, yPosition=9, width=5, height=2, title=NONE], 311=Orientation [id=311, attributeId=311, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=4, width=5, height=1, title=NONE], 56=Orientation [id=56, attributeId=56, templateId=0, viewId=3, groupId=userAgentMac, xPosition=7, yPosition=11, width=5, height=1, title=NONE], 312=Orientation [id=312, attributeId=312, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=5, width=5, height=1, title=NONE], 57=Orientation [id=57, attributeId=57, templateId=0, viewId=3, groupId=null, xPosition=0, yPosition=14, width=6, height=1, title=NONE], 313=Orientation [id=313, attributeId=313, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=6, width=5, height=1, title=NONE], 58=Orientation [id=58, attributeId=58, templateId=0, viewId=3, groupId=null, xPosition=7, yPosition=14, width=5, height=1, title=TOP], 314=Orientation [id=314, attributeId=314, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=7, width=5, height=1, title=NONE], 59=Orientation [id=59, attributeId=59, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=0, width=8, height=1, title=NONE], 315=Orientation [id=315, attributeId=315, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=9, width=5, height=1, title=TOP], 60=Orientation [id=60, attributeId=60, templateId=0, viewId=4, groupId=null, xPosition=3, yPosition=1, width=5, height=1, title=LEFT_SPAN], 316=Orientation [id=316, attributeId=316, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=10, width=5, height=1, title=NONE], 61=Orientation [id=61, attributeId=61, templateId=0, viewId=4, groupId=null, xPosition=3, yPosition=2, width=5, height=1, title=LEFT_SPAN], 317=Orientation [id=317, attributeId=317, templateId=0, viewId=9, groupId=logging, xPosition=3, yPosition=11, width=4, height=1, title=LEFT_SPAN], 62=Orientation [id=62, attributeId=62, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=3, width=8, height=1, title=NONE], 63=Orientation [id=63, attributeId=63, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=5, width=8, height=2, title=TOP], 64=Orientation [id=64, attributeId=64, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=8, width=8, height=1, title=NONE], 65=Orientation [id=65, attributeId=65, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=9, width=8, height=1, title=NONE], 66=Orientation [id=66, attributeId=66, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=10, width=8, height=1, title=NONE], 67=Orientation [id=67, attributeId=67, templateId=0, viewId=5, groupId=quitLink, xPosition=0, yPosition=1, width=8, height=1, title=TOP], 68=Orientation [id=68, attributeId=68, templateId=0, viewId=5, groupId=quitLink, xPosition=0, yPosition=2, width=8, height=1, title=NONE], 69=Orientation [id=69, attributeId=69, templateId=0, viewId=5, groupId=backToStart, xPosition=0, yPosition=4, width=8, height=1, title=NONE], 70=Orientation [id=70, attributeId=70, templateId=0, viewId=5, groupId=backToStart, xPosition=0, yPosition=6, width=8, height=2, title=TOP], 71=Orientation [id=71, attributeId=71, templateId=0, viewId=5, groupId=backToStart, xPosition=0, yPosition=8, width=8, height=2, title=TOP], 72=Orientation [id=72, attributeId=72, templateId=0, viewId=5, groupId=backToStart, xPosition=0, yPosition=10, width=8, height=1, title=NONE], 73=Orientation [id=73, attributeId=73, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=2, width=10, height=6, title=TOP], 74=Orientation [id=74, attributeId=74, templateId=0, viewId=6, groupId=null, xPosition=1, yPosition=1, width=1, height=1, title=LEFT], 75=Orientation [id=75, attributeId=75, templateId=0, viewId=6, groupId=null, xPosition=2, yPosition=2, width=1, height=1, title=LEFT], 76=Orientation [id=76, attributeId=76, templateId=0, viewId=6, groupId=null, xPosition=4, yPosition=4, width=2, height=1, title=LEFT], 77=Orientation [id=77, attributeId=77, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], 78=Orientation [id=78, attributeId=78, templateId=0, viewId=6, groupId=null, xPosition=3, yPosition=4, width=4, height=1, title=LEFT], 79=Orientation [id=79, attributeId=79, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], 80=Orientation [id=80, attributeId=80, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=6, width=1, height=1, title=LEFT], 81=Orientation [id=81, attributeId=81, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=7, width=1, height=1, title=LEFT], 82=Orientation [id=82, attributeId=82, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=8, width=1, height=3, title=LEFT], 85=Orientation [id=85, attributeId=85, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=8, width=1, height=1, title=LEFT], 86=Orientation [id=86, attributeId=86, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=7, width=1, height=1, title=LEFT], 87=Orientation [id=87, attributeId=87, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=9, width=1, height=1, title=LEFT], 88=Orientation [id=88, attributeId=88, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=10, width=1, height=1, title=LEFT], 89=Orientation [id=89, attributeId=89, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=11, width=1, height=1, title=LEFT], 90=Orientation [id=90, attributeId=90, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=12, width=1, height=1, title=LEFT], 91=Orientation [id=91, attributeId=91, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=0, width=5, height=1, title=NONE], 92=Orientation [id=92, attributeId=92, templateId=0, viewId=6, groupId=null, xPosition=5, yPosition=0, width=5, height=1, title=NONE], 93=Orientation [id=93, attributeId=93, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=10, width=10, height=6, title=TOP], 94=Orientation [id=94, attributeId=94, templateId=0, viewId=6, groupId=null, xPosition=1, yPosition=1, width=1, height=1, title=LEFT], 95=Orientation [id=95, attributeId=95, templateId=0, viewId=6, groupId=null, xPosition=2, yPosition=2, width=1, height=1, title=LEFT], 96=Orientation [id=96, attributeId=96, templateId=0, viewId=6, groupId=null, xPosition=3, yPosition=3, width=4, height=1, title=LEFT], 97=Orientation [id=97, attributeId=97, templateId=0, viewId=6, groupId=null, xPosition=4, yPosition=5, width=2, height=1, title=LEFT], 98=Orientation [id=98, attributeId=98, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT], 99=Orientation [id=99, attributeId=99, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=6, width=1, height=1, title=LEFT], 100=Orientation [id=100, attributeId=100, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=7, width=1, height=1, title=LEFT], 400=Orientation [id=400, attributeId=400, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=1, width=4, height=1, title=NONE], 401=Orientation [id=401, attributeId=401, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=2, width=4, height=1, title=NONE], 402=Orientation [id=402, attributeId=402, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=3, width=4, height=1, title=NONE], 403=Orientation [id=403, attributeId=403, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=4, width=4, height=1, title=NONE], 404=Orientation [id=404, attributeId=404, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=5, width=4, height=1, title=NONE], 405=Orientation [id=405, attributeId=405, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=6, width=4, height=1, title=NONE], 406=Orientation [id=406, attributeId=406, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=7, width=4, height=1, title=NONE], 407=Orientation [id=407, attributeId=407, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=8, width=4, height=1, title=NONE], 408=Orientation [id=408, attributeId=408, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=9, width=4, height=1, title=NONE], 200=Orientation [id=200, attributeId=200, templateId=0, viewId=8, groupId=urlFilter, xPosition=0, yPosition=0, width=3, height=1, title=NONE], 201=Orientation [id=201, attributeId=201, templateId=0, viewId=8, groupId=urlFilter, xPosition=3, yPosition=0, width=4, height=1, title=NONE], 202=Orientation [id=202, attributeId=202, templateId=0, viewId=8, groupId=urlFilter, xPosition=0, yPosition=1, width=12, height=6, title=NONE], 203=Orientation [id=203, attributeId=203, templateId=0, viewId=8, groupId=urlFilter, xPosition=1, yPosition=1, width=1, height=1, title=LEFT], 204=Orientation [id=204, attributeId=204, templateId=0, viewId=8, groupId=urlFilter, xPosition=2, yPosition=2, width=1, height=1, title=LEFT], 205=Orientation [id=205, attributeId=205, templateId=0, viewId=8, groupId=urlFilter, xPosition=3, yPosition=3, width=4, height=1, title=LEFT], 206=Orientation [id=206, attributeId=206, templateId=0, viewId=8, groupId=urlFilter, xPosition=4, yPosition=4, width=2, height=1, title=LEFT], 210=Orientation [id=210, attributeId=210, templateId=0, viewId=8, groupId=proxies, xPosition=0, yPosition=6, width=5, height=2, title=NONE], 220=Orientation [id=220, attributeId=220, templateId=0, viewId=8, groupId=proxies, xPosition=7, yPosition=7, width=5, height=7, title=TOP], 221=Orientation [id=221, attributeId=221, templateId=0, viewId=8, groupId=proxies, xPosition=0, yPosition=8, width=6, height=1, title=NONE], 222=Orientation [id=222, attributeId=222, templateId=0, viewId=8, groupId=proxies, xPosition=0, yPosition=10, width=6, height=2, title=TOP], 223=Orientation [id=223, attributeId=223, templateId=0, viewId=8, groupId=proxies, xPosition=0, yPosition=11, width=6, height=1, title=NONE], 231=Orientation [id=231, attributeId=231, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], 233=Orientation [id=233, attributeId=233, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], 234=Orientation [id=234, attributeId=234, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], 235=Orientation [id=235, attributeId=235, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], 236=Orientation [id=236, attributeId=236, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], 237=Orientation [id=237, attributeId=237, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], 238=Orientation [id=238, attributeId=238, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], 239=Orientation [id=239, attributeId=239, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], 240=Orientation [id=240, attributeId=240, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT], 241=Orientation [id=241, attributeId=241, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], 242=Orientation [id=242, attributeId=242, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], 243=Orientation [id=243, attributeId=243, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], 244=Orientation [id=244, attributeId=244, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], 500=Orientation [id=500, attributeId=500, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=1, width=3, height=1, title=NONE], 245=Orientation [id=245, attributeId=245, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], 501=Orientation [id=501, attributeId=501, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=2, width=3, height=1, title=NONE], 246=Orientation [id=246, attributeId=246, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT], 502=Orientation [id=502, attributeId=502, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=3, width=3, height=1, title=NONE], 247=Orientation [id=247, attributeId=247, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], 503=Orientation [id=503, attributeId=503, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=4, width=3, height=1, title=NONE], 248=Orientation [id=248, attributeId=248, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], 504=Orientation [id=504, attributeId=504, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=5, width=3, height=1, title=NONE], 249=Orientation [id=249, attributeId=249, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], 505=Orientation [id=505, attributeId=505, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=6, width=3, height=1, title=NONE], 250=Orientation [id=250, attributeId=250, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], 506=Orientation [id=506, attributeId=506, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=7, width=3, height=1, title=NONE], 251=Orientation [id=251, attributeId=251, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], 507=Orientation [id=507, attributeId=507, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=8, width=3, height=1, title=NONE], 252=Orientation [id=252, attributeId=252, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT], 508=Orientation [id=508, attributeId=508, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=9, width=3, height=1, title=NONE], 253=Orientation [id=253, attributeId=253, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], 509=Orientation [id=509, attributeId=509, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=1, width=3, height=1, title=NONE], 254=Orientation [id=254, attributeId=254, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], 510=Orientation [id=510, attributeId=510, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=2, width=3, height=1, title=NONE], 255=Orientation [id=255, attributeId=255, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], 511=Orientation [id=511, attributeId=511, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=3, width=3, height=1, title=NONE]}, orientationAttributeNameMapping={HTTPSPort=Orientation [id=244, attributeId=244, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], browserUserAgentWinTouchModeCustom=Orientation [id=54, attributeId=54, templateId=0, viewId=3, groupId=userAgentTouch, xPosition=7, yPosition=8, width=5, height=1, title=NONE], newBrowserWindowShowReloadWarning=Orientation [id=47, attributeId=47, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=10, width=3, height=1, title=NONE], HTTPProxy=Orientation [id=237, attributeId=237, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], permittedProcesses.autostart=Orientation [id=87, attributeId=87, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=9, width=1, height=1, title=LEFT], permittedProcesses.runInBackground=Orientation [id=88, attributeId=88, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=10, width=1, height=1, title=LEFT], enableAltMouseWheel=Orientation [id=508, attributeId=508, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=9, width=3, height=1, title=NONE], permittedProcesses=Orientation [id=73, attributeId=73, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=2, width=10, height=6, title=TOP], showTaskBar=Orientation [id=16, attributeId=16, templateId=0, viewId=2, groupId=taskbar, xPosition=0, yPosition=9, width=3, height=1, title=NONE], SOCKSProxy=Orientation [id=255, attributeId=255, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], allowQuit=Orientation [id=2, attributeId=2, templateId=0, viewId=1, groupId=null, xPosition=1, yPosition=3, width=1, height=1, title=LEFT], proxySettingsPolicy=Orientation [id=210, attributeId=210, templateId=0, viewId=8, groupId=proxies, xPosition=0, yPosition=6, width=5, height=2, title=NONE], ignoreExitKeys=Orientation [id=3, attributeId=3, templateId=0, viewId=1, groupId=null, xPosition=1, yPosition=4, width=1, height=1, title=LEFT], permittedProcesses.strongKill=Orientation [id=90, attributeId=90, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=12, width=1, height=1, title=LEFT], allowScreenSharing=Orientation [id=303, attributeId=303, templateId=0, viewId=9, groupId=null, xPosition=0, yPosition=6, width=4, height=1, title=NONE], forceAppFolderInstall=Orientation [id=310, attributeId=310, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=3, width=5, height=1, title=NONE], proxies=Orientation [id=220, attributeId=220, templateId=0, viewId=8, groupId=proxies, xPosition=7, yPosition=7, width=5, height=7, title=TOP], RTSPProxy=Orientation [id=261, attributeId=261, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], downloadDirectoryWin=Orientation [id=60, attributeId=60, templateId=0, viewId=4, groupId=null, xPosition=3, yPosition=1, width=5, height=1, title=LEFT_SPAN], AutoConfigurationURL=Orientation [id=234, attributeId=234, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], prohibitedProcesses.description=Orientation [id=97, attributeId=97, templateId=0, viewId=6, groupId=null, xPosition=4, yPosition=5, width=2, height=1, title=LEFT], insideSebEnableLockThisComputer=Orientation [id=401, attributeId=401, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=2, width=4, height=1, title=NONE], permittedProcesses.allowedExecutables=Orientation [id=80, attributeId=80, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=6, width=1, height=1, title=LEFT], taskBarHeight=Orientation [id=17, attributeId=17, templateId=0, viewId=2, groupId=taskbar, xPosition=5, yPosition=9, width=2, height=1, title=LEFT_SPAN], enablePlugIns=Orientation [id=36, attributeId=36, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=5, width=4, height=1, title=NONE], restartExamText=Orientation [id=71, attributeId=71, templateId=0, viewId=5, groupId=backToStart, xPosition=0, yPosition=8, width=8, height=2, title=TOP], SOCKSEnable=Orientation [id=254, attributeId=254, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], kioskMode=Orientation [id=301, attributeId=301, templateId=0, viewId=9, groupId=kioskMode, xPosition=4, yPosition=0, width=3, height=3, title=NONE], enableAltEsc=Orientation [id=503, attributeId=503, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=4, width=3, height=1, title=NONE], enableCtrlEsc=Orientation [id=502, attributeId=502, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=3, width=3, height=1, title=NONE], minMacOSVersion=Orientation [id=308, attributeId=308, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=1, width=5, height=1, title=TOP], browserWindowAllowReload=Orientation [id=44, attributeId=44, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=9, width=4, height=1, title=NONE], allowDisplayMirroring=Orientation [id=314, attributeId=314, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=7, width=5, height=1, title=NONE], allowDownUploads=Orientation [id=59, attributeId=59, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=0, width=8, height=1, title=NONE], prohibitedProcesses.originalName=Orientation [id=98, attributeId=98, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT], enableBrowserWindowToolbar=Orientation [id=13, attributeId=13, templateId=0, viewId=2, groupId=wintoolbar, xPosition=0, yPosition=6, width=3, height=1, title=NONE], HTTPSPassword=Orientation [id=247, attributeId=247, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], FTPUsername=Orientation [id=252, attributeId=252, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT], RTSPUsername=Orientation [id=264, attributeId=264, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT], allowUserAppFolderInstall=Orientation [id=311, attributeId=311, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=4, width=5, height=1, title=NONE], newBrowserWindowByLinkHeight=Orientation [id=34, attributeId=34, templateId=0, viewId=3, groupId=newwinsize, xPosition=1, yPosition=5, width=2, height=1, title=LEFT], newBrowserWindowAllowReload=Orientation [id=45, attributeId=45, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=9, width=3, height=1, title=NONE], insideSebEnableStartTaskManager=Orientation [id=403, attributeId=403, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=4, width=4, height=1, title=NONE], audioMute=Orientation [id=25, attributeId=25, templateId=0, viewId=2, groupId=audio, xPosition=7, yPosition=1, width=5, height=1, title=NONE], FTPPort=Orientation [id=250, attributeId=250, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], AutoDiscoveryEnabled=Orientation [id=231, attributeId=231, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], newBrowserWindowByLinkPolicy=Orientation [id=31, attributeId=31, templateId=0, viewId=3, groupId=newBrowserWindow, xPosition=0, yPosition=0, width=3, height=3, title=NONE], ExceptionsList=Orientation [id=222, attributeId=222, templateId=0, viewId=8, groupId=proxies, xPosition=0, yPosition=10, width=6, height=2, title=TOP], browserViewMode=Orientation [id=8, attributeId=8, templateId=0, viewId=2, groupId=browserViewMode, xPosition=0, yPosition=0, width=3, height=3, title=NONE], enablePrintScreen=Orientation [id=501, attributeId=501, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=2, width=3, height=1, title=NONE], permittedProcesses.description=Orientation [id=77, attributeId=77, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], allowSwitchToApplications=Orientation [id=91, attributeId=91, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=0, width=5, height=1, title=NONE], SOCKSRequiresPassword=Orientation [id=257, attributeId=257, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], allowVideoCapture=Orientation [id=40, attributeId=40, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=7, width=4, height=1, title=NONE], prohibitedProcesses.os=Orientation [id=95, attributeId=95, templateId=0, viewId=6, groupId=null, xPosition=2, yPosition=2, width=1, height=1, title=LEFT], HTTPRequiresPassword=Orientation [id=239, attributeId=239, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], RTSPPassword=Orientation [id=265, attributeId=265, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], enableAppSwitcherCheck=Orientation [id=309, attributeId=309, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=2, width=5, height=1, title=NONE], HTTPSProxy=Orientation [id=243, attributeId=243, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], logLevel=Orientation [id=317, attributeId=317, templateId=0, viewId=9, groupId=logging, xPosition=3, yPosition=11, width=4, height=1, title=LEFT_SPAN], quitURLConfirm=Orientation [id=68, attributeId=68, templateId=0, viewId=5, groupId=quitLink, xPosition=0, yPosition=2, width=8, height=1, title=NONE], restartExamURL=Orientation [id=70, attributeId=70, templateId=0, viewId=5, groupId=backToStart, xPosition=0, yPosition=6, width=8, height=2, title=TOP], prohibitedProcesses.active=Orientation [id=94, attributeId=94, templateId=0, viewId=6, groupId=null, xPosition=1, yPosition=1, width=1, height=1, title=LEFT], newBrowserWindowByLinkBlockForeign=Orientation [id=32, attributeId=32, templateId=0, viewId=3, groupId=newBrowserWindow, xPosition=4, yPosition=0, width=3, height=1, title=NONE], RTSPEnable=Orientation [id=260, attributeId=260, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], allowedDisplaysMaxNumber=Orientation [id=315, attributeId=315, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=9, width=5, height=1, title=TOP], FTPPassive=Orientation [id=223, attributeId=223, templateId=0, viewId=8, groupId=proxies, xPosition=0, yPosition=11, width=6, height=1, title=NONE], FTPProxy=Orientation [id=249, attributeId=249, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=1, width=1, height=1, title=LEFT], permittedProcesses.active=Orientation [id=74, attributeId=74, templateId=0, viewId=6, groupId=null, xPosition=1, yPosition=1, width=1, height=1, title=LEFT], enableZoomText=Orientation [id=22, attributeId=22, templateId=0, viewId=2, groupId=zoom, xPosition=0, yPosition=15, width=3, height=1, title=NONE], mainBrowserWindowWidth=Orientation [id=10, attributeId=10, templateId=0, viewId=2, groupId=winsize, xPosition=1, yPosition=4, width=2, height=1, title=LEFT], enableLogging=Orientation [id=305, attributeId=305, templateId=0, viewId=9, groupId=logging, xPosition=0, yPosition=8, width=6, height=1, title=NONE], removeLocalStorage=Orientation [id=49, attributeId=49, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=11, width=3, height=1, title=NONE], newBrowserWindowByLinkPositioning=Orientation [id=35, attributeId=35, templateId=0, viewId=3, groupId=newwinsize, xPosition=5, yPosition=4, width=2, height=1, title=LEFT_SPAN], permittedProcesses.iconInTaskbar=Orientation [id=86, attributeId=86, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=7, width=1, height=1, title=LEFT], downloadPDFFiles=Orientation [id=64, attributeId=64, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=8, width=8, height=1, title=NONE], enableAltF4=Orientation [id=505, attributeId=505, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=6, width=3, height=1, title=NONE], allowVirtualMachine=Orientation [id=302, attributeId=302, templateId=0, viewId=9, groupId=null, xPosition=0, yPosition=5, width=4, height=1, title=NONE], enableRightMouse=Orientation [id=507, attributeId=507, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=8, width=3, height=1, title=NONE], exitKey2=Orientation [id=6, attributeId=6, templateId=0, viewId=1, groupId=exitSequence, xPosition=2, yPosition=2, width=1, height=1, title=NONE], exitKey1=Orientation [id=5, attributeId=5, templateId=0, viewId=1, groupId=exitSequence, xPosition=2, yPosition=1, width=1, height=1, title=NONE], exitKey3=Orientation [id=7, attributeId=7, templateId=0, viewId=1, groupId=exitSequence, xPosition=2, yPosition=3, width=1, height=1, title=NONE], showInputLanguage=Orientation [id=20, attributeId=20, templateId=0, viewId=2, groupId=taskbar, xPosition=0, yPosition=12, width=3, height=1, title=NONE], prohibitedProcesses.identifier=Orientation [id=99, attributeId=99, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=6, width=1, height=1, title=LEFT], URLFilterEnable=Orientation [id=200, attributeId=200, templateId=0, viewId=8, groupId=urlFilter, xPosition=0, yPosition=0, width=3, height=1, title=NONE], SOCKSPassword=Orientation [id=259, attributeId=259, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], newBrowserWindowByLinkWidth=Orientation [id=33, attributeId=33, templateId=0, viewId=3, groupId=newwinsize, xPosition=1, yPosition=4, width=2, height=1, title=LEFT], permittedProcesses.originalName=Orientation [id=79, attributeId=79, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], URLFilterEnableContentFilter=Orientation [id=201, attributeId=201, templateId=0, viewId=8, groupId=urlFilter, xPosition=3, yPosition=0, width=4, height=1, title=NONE], enableF2=Orientation [id=510, attributeId=510, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=2, width=3, height=1, title=NONE], permittedProcesses.title=Orientation [id=76, attributeId=76, templateId=0, viewId=6, groupId=null, xPosition=4, yPosition=4, width=2, height=1, title=LEFT], enableF1=Orientation [id=509, attributeId=509, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=1, width=3, height=1, title=NONE], URLFilterRules=Orientation [id=202, attributeId=202, templateId=0, viewId=8, groupId=urlFilter, xPosition=0, yPosition=1, width=12, height=6, title=NONE], enableF4=Orientation [id=512, attributeId=512, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=4, width=3, height=1, title=NONE], SOCKSPort=Orientation [id=256, attributeId=256, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], enableF3=Orientation [id=511, attributeId=511, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=3, width=3, height=1, title=NONE], enableF6=Orientation [id=514, attributeId=514, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=6, width=3, height=1, title=NONE], enableF5=Orientation [id=513, attributeId=513, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=5, width=3, height=1, title=NONE], enableF8=Orientation [id=516, attributeId=516, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=8, width=3, height=1, title=NONE], URLFilterRules.active=Orientation [id=203, attributeId=203, templateId=0, viewId=8, groupId=urlFilter, xPosition=1, yPosition=1, width=1, height=1, title=LEFT], FTPPassword=Orientation [id=253, attributeId=253, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], insideSebEnableLogOff=Orientation [id=404, attributeId=404, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=5, width=4, height=1, title=NONE], enableF7=Orientation [id=515, attributeId=515, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=7, width=3, height=1, title=NONE], mainBrowserWindowHeight=Orientation [id=11, attributeId=11, templateId=0, viewId=2, groupId=winsize, xPosition=1, yPosition=5, width=2, height=1, title=LEFT], enableJava=Orientation [id=38, attributeId=38, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=6, width=4, height=1, title=NONE], showReloadWarning=Orientation [id=46, attributeId=46, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=10, width=4, height=1, title=NONE], prohibitedProcesses=Orientation [id=93, attributeId=93, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=10, width=10, height=6, title=TOP], enableZoomPage=Orientation [id=21, attributeId=21, templateId=0, viewId=2, groupId=zoom, xPosition=0, yPosition=14, width=3, height=1, title=NONE], prohibitedProcesses.executable=Orientation [id=96, attributeId=96, templateId=0, viewId=6, groupId=null, xPosition=3, yPosition=3, width=4, height=1, title=LEFT], HTTPSUsername=Orientation [id=246, attributeId=246, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT], enableF9=Orientation [id=517, attributeId=517, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=9, width=3, height=1, title=NONE], browserUserAgentMac=Orientation [id=55, attributeId=55, templateId=0, viewId=3, groupId=userAgentMac, xPosition=7, yPosition=9, width=5, height=2, title=NONE], RTSPPort=Orientation [id=262, attributeId=262, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], audioControlEnabled=Orientation [id=24, attributeId=24, templateId=0, viewId=2, groupId=audio, xPosition=7, yPosition=0, width=5, height=1, title=NONE], browserUserAgentWinDesktopModeCustom=Orientation [id=52, attributeId=52, templateId=0, viewId=3, groupId=userAgentDesktop, xPosition=7, yPosition=3, width=5, height=1, title=NONE], AutoConfigurationJavaScript=Orientation [id=235, attributeId=235, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], HTTPPassword=Orientation [id=241, attributeId=241, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=5, width=1, height=1, title=LEFT], ExcludeSimpleHostnames=Orientation [id=221, attributeId=221, templateId=0, viewId=8, groupId=proxies, xPosition=0, yPosition=8, width=6, height=1, title=NONE], allowedDisplayBuiltin=Orientation [id=316, attributeId=316, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=10, width=5, height=1, title=NONE], showTime=Orientation [id=19, attributeId=19, templateId=0, viewId=2, groupId=taskbar, xPosition=0, yPosition=11, width=3, height=1, title=NONE], zoomMode=Orientation [id=23, attributeId=23, templateId=0, viewId=2, groupId=zoomMode, xPosition=3, yPosition=14, width=4, height=1, title=NONE], FTPRequiresPassword=Orientation [id=251, attributeId=251, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], newBrowserWindowNavigation=Orientation [id=43, attributeId=43, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=8, width=3, height=1, title=NONE], enableTouchExit=Orientation [id=9, attributeId=9, templateId=0, viewId=2, groupId=browserViewMode, xPosition=3, yPosition=2, width=4, height=1, title=NONE], RTSPRequiresPassword=Orientation [id=263, attributeId=263, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], blockPopUpWindows=Orientation [id=39, attributeId=39, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=6, width=3, height=1, title=NONE], enableEsc=Orientation [id=500, attributeId=500, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=1, width=3, height=1, title=NONE], showMenuBar=Orientation [id=15, attributeId=15, templateId=0, viewId=2, groupId=wintoolbar, xPosition=0, yPosition=7, width=3, height=1, title=NONE], hideBrowserWindowToolbar=Orientation [id=14, attributeId=14, templateId=0, viewId=2, groupId=wintoolbar, xPosition=3, yPosition=6, width=4, height=1, title=NONE], browserWindowTitleSuffix=Orientation [id=58, attributeId=58, templateId=0, viewId=3, groupId=null, xPosition=7, yPosition=14, width=5, height=1, title=TOP], mainBrowserWindowPositioning=Orientation [id=12, attributeId=12, templateId=0, viewId=2, groupId=winsize, xPosition=5, yPosition=4, width=2, height=1, title=LEFT_SPAN], insideSebEnableVmWareClientShade=Orientation [id=407, attributeId=407, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=8, width=4, height=1, title=NONE], logDirectoryOSX=Orientation [id=307, attributeId=307, templateId=0, viewId=9, groupId=logging, xPosition=3, yPosition=10, width=4, height=1, title=LEFT_SPAN], openDownloads=Orientation [id=62, attributeId=62, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=3, width=8, height=1, title=NONE], HTTPEnable=Orientation [id=236, attributeId=236, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], chooseFileToUploadPolicy=Orientation [id=63, attributeId=63, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=5, width=8, height=2, title=TOP], enablePrivateClipboard=Orientation [id=304, attributeId=304, templateId=0, viewId=9, groupId=null, xPosition=4, yPosition=5, width=3, height=1, title=NONE], permittedProcesses.identifier=Orientation [id=85, attributeId=85, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=8, width=1, height=1, title=LEFT], URLFilterRules.regex=Orientation [id=204, attributeId=204, templateId=0, viewId=8, groupId=urlFilter, xPosition=2, yPosition=2, width=1, height=1, title=LEFT], allowFlashFullscreen=Orientation [id=92, attributeId=92, templateId=0, viewId=6, groupId=null, xPosition=5, yPosition=0, width=5, height=1, title=NONE], downloadDirectoryOSX=Orientation [id=61, attributeId=61, templateId=0, viewId=4, groupId=null, xPosition=3, yPosition=2, width=5, height=1, title=LEFT_SPAN], showReloadButton=Orientation [id=18, attributeId=18, templateId=0, viewId=2, groupId=taskbar, xPosition=0, yPosition=10, width=3, height=1, title=NONE], removeBrowserProfile=Orientation [id=48, attributeId=48, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=11, width=4, height=1, title=NONE], insideSebEnableEaseOfAccess=Orientation [id=406, attributeId=406, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=7, width=4, height=1, title=NONE], HTTPSRequiresPassword=Orientation [id=245, attributeId=245, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=3, width=1, height=1, title=LEFT], enableAltTab=Orientation [id=504, attributeId=504, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=5, width=3, height=1, title=NONE], insideSebEnableSwitchUser=Orientation [id=400, attributeId=400, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=1, width=4, height=1, title=NONE], insideSebEnableNetworkConnectionSelector=Orientation [id=408, attributeId=408, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=9, width=4, height=1, title=NONE], allowDictionaryLookup=Orientation [id=29, attributeId=29, templateId=0, viewId=2, groupId=spellcheck, xPosition=7, yPosition=5, width=5, height=1, title=NONE], browserUserAgentWinDesktopMode=Orientation [id=51, attributeId=51, templateId=0, viewId=3, groupId=userAgentDesktop, xPosition=7, yPosition=2, width=5, height=2, title=NONE], allowAudioCapture=Orientation [id=41, attributeId=41, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=7, width=3, height=1, title=NONE], permittedProcesses.path=Orientation [id=81, attributeId=81, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=7, width=1, height=1, title=LEFT], allowBrowsingBackForward=Orientation [id=42, attributeId=42, templateId=0, viewId=3, groupId=browserSecurity, xPosition=0, yPosition=8, width=4, height=1, title=NONE], insideSebEnableShutDown=Orientation [id=405, attributeId=405, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=6, width=4, height=1, title=NONE], URLFilterRules.expression=Orientation [id=205, attributeId=205, templateId=0, viewId=8, groupId=urlFilter, xPosition=3, yPosition=3, width=4, height=1, title=LEFT], permittedProcesses.allowUserToChooseApp=Orientation [id=89, attributeId=89, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=11, width=1, height=1, title=LEFT], sebServicePolicy=Orientation [id=300, attributeId=300, templateId=0, viewId=9, groupId=servicePolicy, xPosition=0, yPosition=0, width=4, height=3, title=NONE], SOCKSUsername=Orientation [id=258, attributeId=258, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT], allowSiri=Orientation [id=312, attributeId=312, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=5, width=5, height=1, title=NONE], enableF11=Orientation [id=519, attributeId=519, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=11, width=3, height=1, title=NONE], enableF10=Orientation [id=518, attributeId=518, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=10, width=3, height=1, title=NONE], allowSpellCheck=Orientation [id=28, attributeId=28, templateId=0, viewId=2, groupId=spellcheck, xPosition=7, yPosition=4, width=5, height=1, title=NONE], enableJavaScript=Orientation [id=37, attributeId=37, templateId=0, viewId=3, groupId=browserSecurity, xPosition=4, yPosition=5, width=3, height=1, title=NONE], permittedProcesses.arguments=Orientation [id=82, attributeId=82, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=8, width=1, height=3, title=LEFT], insideSebEnableChangeAPassword=Orientation [id=402, attributeId=402, templateId=0, viewId=10, groupId=registry, xPosition=0, yPosition=3, width=4, height=1, title=NONE], permittedProcesses.os=Orientation [id=75, attributeId=75, templateId=0, viewId=6, groupId=null, xPosition=2, yPosition=2, width=1, height=1, title=LEFT], enableF12=Orientation [id=520, attributeId=520, templateId=0, viewId=11, groupId=functionKeys, xPosition=3, yPosition=12, width=3, height=1, title=NONE], restartExamUseStartURL=Orientation [id=69, attributeId=69, templateId=0, viewId=5, groupId=backToStart, xPosition=0, yPosition=4, width=8, height=1, title=NONE], permittedProcesses.executable=Orientation [id=78, attributeId=78, templateId=0, viewId=6, groupId=null, xPosition=3, yPosition=4, width=4, height=1, title=LEFT], FTPEnable=Orientation [id=248, attributeId=248, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], downloadAndOpenSebConfig=Orientation [id=66, attributeId=66, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=10, width=8, height=1, title=NONE], enableStartMenu=Orientation [id=506, attributeId=506, templateId=0, viewId=11, groupId=specialKeys, xPosition=0, yPosition=7, width=3, height=1, title=NONE], quitURL=Orientation [id=67, attributeId=67, templateId=0, viewId=5, groupId=quitLink, xPosition=0, yPosition=1, width=8, height=1, title=TOP], URLFilterRules.action=Orientation [id=206, attributeId=206, templateId=0, viewId=8, groupId=urlFilter, xPosition=4, yPosition=4, width=2, height=1, title=LEFT], audioSetVolumeLevel=Orientation [id=26, attributeId=26, templateId=0, viewId=2, groupId=audio, xPosition=7, yPosition=2, width=5, height=1, title=NONE], logDirectoryWin=Orientation [id=306, attributeId=306, templateId=0, viewId=9, groupId=logging, xPosition=3, yPosition=9, width=4, height=1, title=LEFT_SPAN], allowSpellCheckDictionary=Orientation [id=30, attributeId=30, templateId=0, viewId=2, groupId=spellcheck, xPosition=7, yPosition=7, width=5, height=9, title=TOP], restartExamPasswordProtected=Orientation [id=72, attributeId=72, templateId=0, viewId=5, groupId=backToStart, xPosition=0, yPosition=10, width=8, height=1, title=NONE], hashedAdminPassword=Orientation [id=1, attributeId=1, templateId=0, viewId=1, groupId=null, xPosition=1, yPosition=1, width=1, height=2, title=LEFT], browserUserAgentWinTouchMode=Orientation [id=53, attributeId=53, templateId=0, viewId=3, groupId=userAgentTouch, xPosition=7, yPosition=4, width=5, height=3, title=NONE], prohibitedProcesses.strongKill=Orientation [id=100, attributeId=100, templateId=0, viewId=6, groupId=null, xPosition=0, yPosition=7, width=1, height=1, title=LEFT], hashedQuitPassword=Orientation [id=4, attributeId=4, templateId=0, viewId=1, groupId=null, xPosition=1, yPosition=5, width=1, height=2, title=LEFT], HTTPPort=Orientation [id=238, attributeId=238, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=2, width=1, height=1, title=LEFT], browserUserAgentMacCustom=Orientation [id=56, attributeId=56, templateId=0, viewId=3, groupId=userAgentMac, xPosition=7, yPosition=11, width=5, height=1, title=NONE], browserUserAgent=Orientation [id=50, attributeId=50, templateId=0, viewId=3, groupId=null, xPosition=7, yPosition=1, width=5, height=1, title=TOP], AutoConfigurationEnabled=Orientation [id=233, attributeId=233, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], audioVolumeLevel=Orientation [id=27, attributeId=27, templateId=0, viewId=2, groupId=audio, xPosition=7, yPosition=3, width=5, height=1, title=NONE], HTTPSEnable=Orientation [id=242, attributeId=242, templateId=0, viewId=8, groupId=active, xPosition=0, yPosition=0, width=1, height=1, title=LEFT], enableSebBrowser=Orientation [id=57, attributeId=57, templateId=0, viewId=3, groupId=null, xPosition=0, yPosition=14, width=6, height=1, title=NONE], detectStoppedProcess=Orientation [id=313, attributeId=313, templateId=0, viewId=9, groupId=macSettings, xPosition=7, yPosition=6, width=5, height=1, title=NONE], allowPDFPlugIn=Orientation [id=65, attributeId=65, templateId=0, viewId=4, groupId=null, xPosition=0, yPosition=9, width=8, height=1, title=NONE], HTTPUsername=Orientation [id=240, attributeId=240, templateId=0, viewId=8, groupId=null, xPosition=0, yPosition=4, width=1, height=1, title=LEFT]}, childAttributeMapping={256=[], 512=[], 1=[], 257=[], 513=[], 2=[], 258=[], 514=[], 3=[], 259=[], 515=[], 4=[], 260=[], 516=[], 5=[], 261=[], 517=[], 6=[], 262=[], 518=[], 7=[], 263=[], 519=[], 8=[], 264=[], 520=[], 9=[], 265=[], 10=[], 11=[], 12=[], 13=[], 14=[], 15=[], 16=[], 17=[], 18=[], 19=[], 20=[], 21=[], 22=[], 23=[], 24=[], 25=[], 26=[], 27=[], 28=[], 29=[], 30=[], 31=[], 32=[], 33=[], 34=[], 35=[], 36=[], 37=[], 38=[], 39=[], 40=[], 41=[], 42=[], 43=[], 44=[], 300=[], 45=[], 301=[], 46=[], 302=[], 47=[], 303=[], 48=[], 304=[], 49=[], 305=[], 50=[], 306=[], 51=[], 307=[], 52=[], 308=[], 53=[], 309=[], 54=[], 310=[], 55=[], 311=[], 56=[], 312=[], 57=[], 313=[], 58=[], 314=[], 59=[], 315=[], 60=[], 316=[], 61=[], 317=[], 62=[], 63=[], 64=[], 65=[], 66=[], 67=[], 68=[], 69=[], 70=[], 71=[], 72=[], 73=[ConfigurationAttribute [id=77, parentId=73, name=permittedProcesses.description, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=79, parentId=73, name=permittedProcesses.originalName, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=80, parentId=73, name=permittedProcesses.allowedExecutables, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=81, parentId=73, name=permittedProcesses.path, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=82, parentId=73, name=permittedProcesses.arguments, type=INLINE_TABLE, resources=1:active:CHECKBOX|4:argument:TEXT_FIELD, validator=null, dependencies=null, defaultValue=null], ConfigurationAttribute [id=85, parentId=73, name=permittedProcesses.identifier, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=86, parentId=73, name=permittedProcesses.iconInTaskbar, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=87, parentId=73, name=permittedProcesses.autostart, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=88, parentId=73, name=permittedProcesses.runInBackground, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=89, parentId=73, name=permittedProcesses.allowUserToChooseApp, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=90, parentId=73, name=permittedProcesses.strongKill, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=74, parentId=73, name=permittedProcesses.active, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=75, parentId=73, name=permittedProcesses.os, type=SINGLE_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0], ConfigurationAttribute [id=78, parentId=73, name=permittedProcesses.executable, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=76, parentId=73, name=permittedProcesses.title, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=]], 74=[], 75=[], 76=[], 77=[], 78=[], 79=[], 80=[], 81=[], 82=[], 85=[], 86=[], 87=[], 88=[], 89=[], 90=[], 91=[], 92=[], 93=[ConfigurationAttribute [id=98, parentId=93, name=prohibitedProcesses.originalName, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=99, parentId=93, name=prohibitedProcesses.identifier, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=100, parentId=93, name=prohibitedProcesses.strongKill, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=94, parentId=93, name=prohibitedProcesses.active, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=95, parentId=93, name=prohibitedProcesses.os, type=SINGLE_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0], ConfigurationAttribute [id=96, parentId=93, name=prohibitedProcesses.executable, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=97, parentId=93, name=prohibitedProcesses.description, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=]], 94=[], 95=[], 96=[], 97=[], 98=[], 99=[], 100=[], 400=[], 401=[], 402=[], 403=[], 404=[], 405=[], 406=[], 407=[], 408=[], 200=[], 201=[], 202=[ConfigurationAttribute [id=203, parentId=202, name=URLFilterRules.active, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=204, parentId=202, name=URLFilterRules.regex, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=205, parentId=202, name=URLFilterRules.expression, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=206, parentId=202, name=URLFilterRules.action, type=SINGLE_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=]], 203=[], 204=[], 205=[], 206=[], 210=[], 220=[ConfigurationAttribute [id=256, parentId=220, name=SOCKSPort, type=INTEGER, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=1080], ConfigurationAttribute [id=257, parentId=220, name=SOCKSRequiresPassword, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=258, parentId=220, name=SOCKSUsername, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=259, parentId=220, name=SOCKSPassword, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=260, parentId=220, name=RTSPEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=261, parentId=220, name=RTSPProxy, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=262, parentId=220, name=RTSPPort, type=INTEGER, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=554], ConfigurationAttribute [id=263, parentId=220, name=RTSPRequiresPassword, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=264, parentId=220, name=RTSPUsername, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=265, parentId=220, name=RTSPPassword, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=221, parentId=220, name=ExcludeSimpleHostnames, type=CHECKBOX, resources=null, validator=null, dependencies=showInView=true,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=222, parentId=220, name=ExceptionsList, type=TEXT_AREA, resources=null, validator=null, dependencies=showInView=true,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=223, parentId=220, name=FTPPassive, type=CHECKBOX, resources=null, validator=null, dependencies=showInView=true,createDefaultValue=true, defaultValue=true], ConfigurationAttribute [id=231, parentId=220, name=AutoDiscoveryEnabled, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=autoDiscovery,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=233, parentId=220, name=AutoConfigurationEnabled, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=autoConfiguration,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=234, parentId=220, name=AutoConfigurationURL, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=autoConfiguration,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=235, parentId=220, name=AutoConfigurationJavaScript, type=TEXT_AREA, resources=null, validator=null, dependencies=groupId=autoConfiguration,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=236, parentId=220, name=HTTPEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=237, parentId=220, name=HTTPProxy, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=238, parentId=220, name=HTTPPort, type=INTEGER, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=80], ConfigurationAttribute [id=239, parentId=220, name=HTTPRequiresPassword, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=240, parentId=220, name=HTTPUsername, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=241, parentId=220, name=HTTPPassword, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=242, parentId=220, name=HTTPSEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=243, parentId=220, name=HTTPSProxy, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=244, parentId=220, name=HTTPSPort, type=INTEGER, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=443], ConfigurationAttribute [id=245, parentId=220, name=HTTPSRequiresPassword, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=246, parentId=220, name=HTTPSUsername, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=247, parentId=220, name=HTTPSPassword, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=248, parentId=220, name=FTPEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=249, parentId=220, name=FTPProxy, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=250, parentId=220, name=FTPPort, type=INTEGER, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=21], ConfigurationAttribute [id=251, parentId=220, name=FTPRequiresPassword, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=252, parentId=220, name=FTPUsername, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=253, parentId=220, name=FTPPassword, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=254, parentId=220, name=SOCKSEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=255, parentId=220, name=SOCKSProxy, type=TEXT_FIELD, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=null]], 221=[], 222=[], 223=[], 231=[], 233=[], 234=[], 235=[], 236=[], 237=[], 238=[], 239=[], 240=[], 241=[], 242=[], 243=[], 244=[], 500=[], 245=[], 501=[], 246=[], 502=[], 247=[], 503=[], 248=[], 504=[], 249=[], 505=[], 250=[], 506=[], 251=[], 507=[], 252=[], 508=[], 253=[], 509=[], 254=[], 510=[], 255=[], 511=[]}, attributeGroupMapping={browserViewMode=[ConfigurationAttribute [id=8, parentId=null, name=browserViewMode, type=RADIO_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=0], ConfigurationAttribute [id=9, parentId=null, name=enableTouchExit, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false]], exitSequence=[ConfigurationAttribute [id=5, parentId=null, name=exitKey1, type=SINGLE_SELECTION, resources=0,1,2,3,4,5,6,7,8,9,10,11, validator=ExitKeySequenceValidator, dependencies=resourceLocTextKey=sebserver.examconfig.props.label.exitKey, defaultValue=2], ConfigurationAttribute [id=6, parentId=null, name=exitKey2, type=SINGLE_SELECTION, resources=0,1,2,3,4,5,6,7,8,9,10,11, validator=ExitKeySequenceValidator, dependencies=resourceLocTextKey=sebserver.examconfig.props.label.exitKey, defaultValue=10], ConfigurationAttribute [id=7, parentId=null, name=exitKey3, type=SINGLE_SELECTION, resources=0,1,2,3,4,5,6,7,8,9,10,11, validator=ExitKeySequenceValidator, dependencies=resourceLocTextKey=sebserver.examconfig.props.label.exitKey, defaultValue=5]], functionKeys=[ConfigurationAttribute [id=509, parentId=null, name=enableF1, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=510, parentId=null, name=enableF2, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=511, parentId=null, name=enableF3, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=512, parentId=null, name=enableF4, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=513, parentId=null, name=enableF5, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=514, parentId=null, name=enableF6, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=515, parentId=null, name=enableF7, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=516, parentId=null, name=enableF8, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=517, parentId=null, name=enableF9, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=518, parentId=null, name=enableF10, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=519, parentId=null, name=enableF11, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=520, parentId=null, name=enableF12, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false]], browserSecurity=[ConfigurationAttribute [id=36, parentId=null, name=enablePlugIns, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=37, parentId=null, name=enableJavaScript, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=38, parentId=null, name=enableJava, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=39, parentId=null, name=blockPopUpWindows, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=40, parentId=null, name=allowVideoCapture, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=41, parentId=null, name=allowAudioCapture, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=42, parentId=null, name=allowBrowsingBackForward, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=43, parentId=null, name=newBrowserWindowNavigation, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=44, parentId=null, name=browserWindowAllowReload, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=45, parentId=null, name=newBrowserWindowAllowReload, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=46, parentId=null, name=showReloadWarning, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=47, parentId=null, name=newBrowserWindowShowReloadWarning, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=48, parentId=null, name=removeBrowserProfile, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=49, parentId=null, name=removeLocalStorage, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false]], newwinsize=[ConfigurationAttribute [id=33, parentId=null, name=newBrowserWindowByLinkWidth, type=COMBO_SELECTION, resources=50%,100%,800,1000, validator=WindowsSizeValidator, dependencies=null, defaultValue=100%], ConfigurationAttribute [id=35, parentId=null, name=newBrowserWindowByLinkPositioning, type=SINGLE_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=2], ConfigurationAttribute [id=34, parentId=null, name=newBrowserWindowByLinkHeight, type=COMBO_SELECTION, resources=80%,100%,600,800, validator=WindowsSizeValidator, dependencies=null, defaultValue=100%]], proxies=[ConfigurationAttribute [id=210, parentId=null, name=proxySettingsPolicy, type=RADIO_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0], ConfigurationAttribute [id=220, parentId=null, name=proxies, type=COMPOSITE_TABLE, resources=active,TABLE_ENTRY|autoDiscovery,autoConfiguration,http,https,ftp,socks,rtsp, validator=null, dependencies=null, defaultValue=null], ConfigurationAttribute [id=221, parentId=220, name=ExcludeSimpleHostnames, type=CHECKBOX, resources=null, validator=null, dependencies=showInView=true,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=222, parentId=220, name=ExceptionsList, type=TEXT_AREA, resources=null, validator=null, dependencies=showInView=true,createDefaultValue=true, defaultValue=null], ConfigurationAttribute [id=223, parentId=220, name=FTPPassive, type=CHECKBOX, resources=null, validator=null, dependencies=showInView=true,createDefaultValue=true, defaultValue=true]], macSettings=[ConfigurationAttribute [id=308, parentId=null, name=minMacOSVersion, type=SINGLE_SELECTION, resources=0,1,2,3,4,5,6,7, validator=null, dependencies=null, defaultValue=0], ConfigurationAttribute [id=309, parentId=null, name=enableAppSwitcherCheck, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=310, parentId=null, name=forceAppFolderInstall, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=311, parentId=null, name=allowUserAppFolderInstall, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=312, parentId=null, name=allowSiri, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=313, parentId=null, name=detectStoppedProcess, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=314, parentId=null, name=allowDisplayMirroring, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=315, parentId=null, name=allowedDisplaysMaxNumber, type=COMBO_SELECTION, resources=1,2,3, validator=null, dependencies=null, defaultValue=1], ConfigurationAttribute [id=316, parentId=null, name=allowedDisplayBuiltin, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true]], audio=[ConfigurationAttribute [id=24, parentId=null, name=audioControlEnabled, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=25, parentId=null, name=audioMute, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=26, parentId=null, name=audioSetVolumeLevel, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=27, parentId=null, name=audioVolumeLevel, type=SLIDER, resources=0,100, validator=null, dependencies=null, defaultValue=25]], urlFilter=[ConfigurationAttribute [id=200, parentId=null, name=URLFilterEnable, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=201, parentId=null, name=URLFilterEnableContentFilter, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=202, parentId=null, name=URLFilterRules, type=TABLE, resources=null, validator=null, dependencies=null, defaultValue=null], ConfigurationAttribute [id=203, parentId=202, name=URLFilterRules.active, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=204, parentId=202, name=URLFilterRules.regex, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=205, parentId=202, name=URLFilterRules.expression, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=206, parentId=202, name=URLFilterRules.action, type=SINGLE_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=]], wintoolbar=[ConfigurationAttribute [id=13, parentId=null, name=enableBrowserWindowToolbar, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=14, parentId=null, name=hideBrowserWindowToolbar, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=15, parentId=null, name=showMenuBar, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false]], registry=[ConfigurationAttribute [id=400, parentId=null, name=insideSebEnableSwitchUser, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=401, parentId=null, name=insideSebEnableLockThisComputer, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=402, parentId=null, name=insideSebEnableChangeAPassword, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=403, parentId=null, name=insideSebEnableStartTaskManager, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=404, parentId=null, name=insideSebEnableLogOff, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=405, parentId=null, name=insideSebEnableShutDown, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=406, parentId=null, name=insideSebEnableEaseOfAccess, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=407, parentId=null, name=insideSebEnableVmWareClientShade, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=408, parentId=null, name=insideSebEnableNetworkConnectionSelector, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false]], kioskMode=[ConfigurationAttribute [id=301, parentId=null, name=kioskMode, type=RADIO_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=0]], zoomMode=[ConfigurationAttribute [id=23, parentId=null, name=zoomMode, type=RADIO_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0]], userAgentDesktop=[ConfigurationAttribute [id=51, parentId=null, name=browserUserAgentWinDesktopMode, type=RADIO_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0], ConfigurationAttribute [id=52, parentId=null, name=browserUserAgentWinDesktopModeCustom, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null]], active=[ConfigurationAttribute [id=260, parentId=220, name=RTSPEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=rtsp,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=231, parentId=220, name=AutoDiscoveryEnabled, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=autoDiscovery,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=233, parentId=220, name=AutoConfigurationEnabled, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=autoConfiguration,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=236, parentId=220, name=HTTPEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=http,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=242, parentId=220, name=HTTPSEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=https,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=248, parentId=220, name=FTPEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=ftp,createDefaultValue=true, defaultValue=false], ConfigurationAttribute [id=254, parentId=220, name=SOCKSEnable, type=CHECKBOX, resources=null, validator=null, dependencies=groupId=socks,createDefaultValue=true, defaultValue=false]], zoom=[ConfigurationAttribute [id=21, parentId=null, name=enableZoomPage, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=22, parentId=null, name=enableZoomText, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true]], winsize=[ConfigurationAttribute [id=10, parentId=null, name=mainBrowserWindowWidth, type=COMBO_SELECTION, resources=50%,100%,800,1000, validator=WindowsSizeValidator, dependencies=null, defaultValue=100%], ConfigurationAttribute [id=12, parentId=null, name=mainBrowserWindowPositioning, type=SINGLE_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=1], ConfigurationAttribute [id=11, parentId=null, name=mainBrowserWindowHeight, type=COMBO_SELECTION, resources=80%,100%,600,800, validator=WindowsSizeValidator, dependencies=null, defaultValue=100%]], newBrowserWindow=[ConfigurationAttribute [id=31, parentId=null, name=newBrowserWindowByLinkPolicy, type=RADIO_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=2], ConfigurationAttribute [id=32, parentId=null, name=newBrowserWindowByLinkBlockForeign, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false]], taskbar=[ConfigurationAttribute [id=16, parentId=null, name=showTaskBar, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=17, parentId=null, name=taskBarHeight, type=COMBO_SELECTION, resources=40,60,80, validator=IntegerTypeValidator, dependencies=null, defaultValue=40], ConfigurationAttribute [id=18, parentId=null, name=showReloadButton, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=19, parentId=null, name=showTime, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=20, parentId=null, name=showInputLanguage, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false]], backToStart=[ConfigurationAttribute [id=69, parentId=null, name=restartExamUseStartURL, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=70, parentId=null, name=restartExamURL, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], ConfigurationAttribute [id=71, parentId=null, name=restartExamText, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], ConfigurationAttribute [id=72, parentId=null, name=restartExamPasswordProtected, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true]], spellcheck=[ConfigurationAttribute [id=28, parentId=null, name=allowSpellCheck, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=29, parentId=null, name=allowDictionaryLookup, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=30, parentId=null, name=allowSpellCheckDictionary, type=MULTI_CHECKBOX_SELECTION, resources=da-DK,en-AU,en-GB,en-US,es-ES,fr-FR,pt-PT,sv-SE,sv-FI, validator=null, dependencies=null, defaultValue=da-DK,en-AU,en-GB,en-US,es-ES,fr-FR,pt-PT,sv-SE,sv-FI]], servicePolicy=[ConfigurationAttribute [id=300, parentId=null, name=sebServicePolicy, type=RADIO_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=2]], specialKeys=[ConfigurationAttribute [id=500, parentId=null, name=enableEsc, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=501, parentId=null, name=enablePrintScreen, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=502, parentId=null, name=enableCtrlEsc, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=503, parentId=null, name=enableAltEsc, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=504, parentId=null, name=enableAltTab, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true], ConfigurationAttribute [id=505, parentId=null, name=enableAltF4, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=506, parentId=null, name=enableStartMenu, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=507, parentId=null, name=enableRightMouse, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=508, parentId=null, name=enableAltMouseWheel, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false]], quitLink=[ConfigurationAttribute [id=67, parentId=null, name=quitURL, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null], ConfigurationAttribute [id=68, parentId=null, name=quitURLConfirm, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=true]], logging=[ConfigurationAttribute [id=305, parentId=null, name=enableLogging, type=CHECKBOX, resources=null, validator=null, dependencies=null, defaultValue=false], ConfigurationAttribute [id=306, parentId=null, name=logDirectoryWin, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=], ConfigurationAttribute [id=307, parentId=null, name=logDirectoryOSX, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=NSTemporaryDirectory], ConfigurationAttribute [id=317, parentId=null, name=logLevel, type=SINGLE_SELECTION, resources=0,1,2,3,4, validator=null, dependencies=null, defaultValue=1]], userAgentTouch=[ConfigurationAttribute [id=53, parentId=null, name=browserUserAgentWinTouchMode, type=RADIO_SELECTION, resources=0,1,2, validator=null, dependencies=null, defaultValue=0], ConfigurationAttribute [id=54, parentId=null, name=browserUserAgentWinTouchModeCustom, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null]], userAgentMac=[ConfigurationAttribute [id=55, parentId=null, name=browserUserAgentMac, type=RADIO_SELECTION, resources=0,1, validator=null, dependencies=null, defaultValue=0], ConfigurationAttribute [id=56, parentId=null, name=browserUserAgentMacCustom, type=TEXT_FIELD, resources=null, validator=null, dependencies=null, defaultValue=null]]}]",
//                attributeMapping.toString());

//        final Result<List<View>> viewsCall = restService
//                .getBuilder(GetViewList.class)
//                .call();
//
//        assertNotNull(viewsCall);
//        assertFalse(viewsCall.hasError());
//        final List<View> views = viewsCall.get();
//        assertFalse(views.isEmpty());
//        final List<String> viewNames = views
//                .stream()
//                .sorted((v1, v2) -> v1.position.compareTo(v2.position))
//                .map(v -> v.name)
//                .collect(Collectors.toList());
//        assertEquals("", viewNames.toString());

    }

    @Test
    @Order(10)
    // *************************************
    // Use Case 10: Login as examAdmin2 and create a new SEB Exam Configuration
    // - save configuration in history
    // - change some attribute
    // - process an undo
    // - table value add, delete, modify
    // - export
    public void testUsecase10() throws IOException {
        final RestServiceImpl restService = createRestServiceForUser(
                "examAdmin2",
                "examAdmin2",
                new NewExamConfig(),
                new GetExamConfigNode(),
                new GetExamConfigNodePage(),
                new SaveExamConfigHistory(),
                new ExportExamConfig(),
                new GetFollowupConfiguration(),
                new SebExamConfigUndo(),
                new SaveExamConfigValue(),
                new SaveExamConfigTableValues(),
                new GetConfigurationValues(),
                new ActivateExamConfig(),
                new DeactivateExamConfig());
    }

}
