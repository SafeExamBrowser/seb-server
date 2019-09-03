/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.integration;

import static org.junit.Assert.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.jdbc.Sql;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestServiceImpl;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.GetQuizPage;
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
        assertEquals("Unexpected error while rest call", call.getError().getMessage());
        RestCallError error = (RestCallError) call.getError();
        assertEquals(
                "[APIMessage [messageCode=1100, systemMessage=Unexpected intenral server-side error, details=No edit right grant for user: TestInstAdmin, attributes=[]]]",
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
    // - create a new user-account (examAdmin1) with Exam Administrator role
    // - create a new user-account (examSupport1) with Exam Supporter role
    // - create a new user-account (examSupport2) with Exam Administrator and Exam Supporter role
    public void testUsecase4() {
        final RestServiceImpl restService = createRestServiceForUser(
                "TestInstAdmin",
                "12345678",
                new GetInstitutionNames(),
                new SaveUserAccount(),
                new ChangePassword(),
                new GetUserAccount(),
                new GetUserAccountNames());

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

}
