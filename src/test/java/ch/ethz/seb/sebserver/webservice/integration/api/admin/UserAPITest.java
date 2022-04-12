/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class UserAPITest extends AdministrationAPIIntegrationTester {

    @Test
    public void getMyUserInfo() throws Exception {
        String contentAsString = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.USER_ACCOUNT_ENDPOINT + "/me")
                .withExpectedStatus(HttpStatus.OK)
                .getAsString();

        assertEquals(
                "{\"uuid\":\"user1\","
                        + "\"institutionId\":1,"
                        + "\"creationDate\":\"2019-01-01T00:00:00.000Z\","
                        + "\"name\":\"SEBAdmin\","
                        + "\"surname\":\"\","
                        + "\"username\":\"admin\","
                        + "\"email\":\"admin1@nomail.nomail\","
                        + "\"active\":true,"
                        + "\"language\":\"en\","
                        + "\"timezone\":\"UTC\","
                        + "\"userRoles\":[\"EXAM_ADMIN\",\"EXAM_SUPPORTER\",\"SEB_SERVER_ADMIN\",\"INSTITUTIONAL_ADMIN\"]}",
                contentAsString);

        contentAsString = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.USER_ACCOUNT_ENDPOINT + "/me")
                .withExpectedStatus(HttpStatus.OK)
                .getAsString();

        assertEquals(
                "{\"uuid\":\"user2\","
                        + "\"institutionId\":1,"
                        + "\"creationDate\":\"2019-01-01T00:00:00.000Z\","
                        + "\"name\":\"Institutional1 Admin\","
                        + "\"surname\":\"\","
                        + "\"username\":\"inst1Admin\","
                        + "\"email\":\"admin2@nomail.nomail\","
                        + "\"active\":true,"
                        + "\"language\":\"en\","
                        + "\"timezone\":\"UTC\","
                        + "\"userRoles\":[\"INSTITUTIONAL_ADMIN\"]}",
                contentAsString);
    }

    @Test
    public void getUserInfoWithUUID() throws Exception {
        final String sebAdminAccessToken = getSebAdminAccess();
        String contentAsString = this.mockMvc
                .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/user2")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Bearer " + sebAdminAccessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(
                "{\"uuid\":\"user2\","
                        + "\"institutionId\":1,"
                        + "\"creationDate\":\"2019-01-01T00:00:00.000Z\","
                        + "\"name\":\"Institutional1 Admin\","
                        + "\"surname\":\"\","
                        + "\"username\":\"inst1Admin\","
                        + "\"email\":\"admin2@nomail.nomail\","
                        + "\"active\":true,"
                        + "\"language\":\"en\","
                        + "\"timezone\":\"UTC\","
                        + "\"userRoles\":[\"INSTITUTIONAL_ADMIN\"]}",
                contentAsString);

        final String adminInstitution2AccessToken = getAdminInstitution2Access();
        contentAsString = this.mockMvc
                .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/user1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Bearer " + adminInstitution2AccessToken))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertEquals(
                "[{\"messageCode\":\"1001\","
                        + "\"systemMessage\":\"FORBIDDEN\","
                        + "\"details\":\"No grant: READ on type: USER entity institution: 1 entity owner: user1 for user: user3\","
                        + "\"attributes\":[]}]",
                contentAsString);
    }

    @Test
    public void institutionalAdminNotAllowedToSeeUsersOfOtherInstitution() throws Exception {
        new RestAPITestHelper()
                .withAccessToken(getAdminInstitution1Access())
                .withPath(API.USER_ACCOUNT_ENDPOINT + "?institutionId=2")
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .getAsString();
    }

    @Test
    public void getAllUserInfoNoFilter() throws Exception {
        Page<UserInfo> userInfos = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.USER_ACCOUNT_ENDPOINT + "?institutionId=1")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<UserInfo>>() {
                });

        // expecting all users for a SEBAdmin except inactive.
        assertNotNull(userInfos);
        assertTrue(userInfos.content.size() == 3);
        assertNotNull(getUserInfo("admin", userInfos.content));
        assertNotNull(getUserInfo("inst1Admin", userInfos.content));
        assertNotNull(getUserInfo("examSupporter", userInfos.content));

        userInfos = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution2Access())
                .withPath(API.USER_ACCOUNT_ENDPOINT)
                .withAttribute("institutionId", "2")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<UserInfo>>() {
                });

        // expecting all users of institution 2 also inactive when active flag is not set
        assertNotNull(userInfos);
        assertTrue(userInfos.content.size() == 4);
        assertNotNull(getUserInfo("inst2Admin", userInfos.content));
        assertNotNull(getUserInfo("examAdmin1", userInfos.content));
        assertNotNull(getUserInfo("deactivatedUser", userInfos.content));
        assertNotNull(getUserInfo("user1", userInfos.content));

        //.. and without inactive, if active flag is set to true
        userInfos = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution2Access())
                .withPath(API.USER_ACCOUNT_ENDPOINT)
                .withAttribute(Entity.FILTER_ATTR_INSTITUTION, "2")
                .withAttribute(Entity.FILTER_ATTR_ACTIVE, "true")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.content.size() == 3);
        assertNotNull(getUserInfo("inst2Admin", userInfos.content));
        assertNotNull(getUserInfo("examAdmin1", userInfos.content));
        assertNotNull(getUserInfo("user1", userInfos.content));

        //.. and only inactive, if active flag is set to false
        userInfos = new RestAPITestHelper()
                .withAccessToken(getAdminInstitution2Access())
                .withPath(API.USER_ACCOUNT_ENDPOINT)
                .withAttribute(Entity.FILTER_ATTR_INSTITUTION, "2")
                .withAttribute(Entity.FILTER_ATTR_ACTIVE, "false")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.content.size() == 1);
        assertNotNull(getUserInfo("deactivatedUser", userInfos.content));

    }

    @Test
    public void getPageNoFilterNoPageAttributes() throws Exception {

        // expecting all user accounts of the institution of SEBAdmin

        final String token = getSebAdminAccess();
        final Page<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "?institutionId=1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.numberOfPages == 1);
        assertNotNull(userInfos.content);
        assertTrue(userInfos.content.size() == 3);
        assertEquals("[user1, user2, user5]", getOrderedUUIDs(userInfos.content));
    }

    @Test
    public void getPageNoFilterNoPageAttributesFromOtherInstitution() throws Exception {

        // expecting all user accounts of institution 2

        final String token = getSebAdminAccess();
        final Page<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "?institutionId=2")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.numberOfPages == 1);
        assertNotNull(userInfos.content);
        assertTrue(userInfos.content.size() == 4);
        assertEquals("[user3, user4, user6, user7]", getOrderedUUIDs(userInfos.content));
    }

    @Test
    public void getPageNoFilterNoPageAttributesDescendingOrder() throws Exception {
        final String token = getSebAdminAccess();
        final Page<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "?sort=-&institutionId=1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.numberOfPages == 1);
        assertNotNull(userInfos.content);
        assertTrue(userInfos.content.size() == 3);
        assertEquals("[user1, user2, user5]", getOrderedUUIDs(userInfos.content));
    }

    @Test
    public void getPageOfSize3NoFilter() throws Exception {
        final String token = getSebAdminAccess();

        // first page default sort order
        Page<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT
                                + "?page_number=1&page_size=3&institutionId=2")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.numberOfPages == 2);
        assertNotNull(userInfos.content);
        assertTrue(userInfos.content.size() == 3);
        assertEquals("[user3, user4, user6]", getOrderedUUIDs(userInfos.content));

        // second page default sort order
        userInfos = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT
                                + "?page_number=2&page_size=3&institutionId=2")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.numberOfPages == 2);
        assertNotNull(userInfos.content);
        assertTrue(userInfos.pageSize == 1);
        assertTrue(userInfos.content.size() == 1);
        assertEquals("[user7]", getOrderedUUIDs(userInfos.content));

        // invalid page number should refer to last page
        userInfos = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT
                                + "?page_number=3&page_size=3&institutionId=2")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.numberOfPages == 2);
        assertTrue(userInfos.pageNumber == 2);
        assertNotNull(userInfos.content);
        assertTrue(userInfos.content.size() == 1);
        assertEquals("[user7]", getOrderedUUIDs(userInfos.content));

        // first page descending sort order
        userInfos = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT
                                + "?page_number=1&page_size=3&sort=-&institutionId=2")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.numberOfPages == 2);
        assertNotNull(userInfos.content);
        assertTrue(userInfos.content.size() == 3);
        assertEquals("[user4, user6, user7]", getOrderedUUIDs(userInfos.content));
    }

    @Test
    public void getAllUserInfo() throws Exception {
        final String token = getSebAdminAccess();
        final Page<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "?institutionId=1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.content.size() == 3);
    }

    @Test
    public void getAllUserInfoWithOnlyActive() throws Exception {
        final String token = getSebAdminAccess();
        final Page<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT
                                + "?active=true&institutionId=2")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.content.size() == 3);
        assertNull(getUserInfo("deactivatedUser", userInfos.content));
    }

    @Test
    public void getAllUserInfoOnlyInactive() throws Exception {

        // expecting none for SEBAdmins institution
        final String token = getSebAdminAccess();
        Page<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "?active=false&institutionId=1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.content.size() == 0);

        // expecting one for institution 2
        userInfos = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT
                                + "?active=false&institutionId=2")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.content.size() == 1);
        assertNotNull(getUserInfo("deactivatedUser", userInfos.content));
    }

    @Test
    public void getAllUserInfoWithSearchUsernameLike() throws Exception {
        final String token = getSebAdminAccess();
        final Page<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "?username=exam&institutionId=1")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.content.size() == 1);
        assertNotNull(getUserInfo("examSupporter", userInfos.content));
    }

    @Test
    public void testOwnerGet() throws Exception {
        final String examAdminToken1 = getExamAdmin1();
        this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/me")
                .header("Authorization", "Bearer " + examAdminToken1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void createUserTest() throws Exception {
        final String token = getSebAdminAccess();
        final UserInfo createdUser = this.jsonMapper.readValue(
                this.mockMvc.perform(post(this.endpoint + API.USER_ACCOUNT_ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(Domain.USER.ATTR_NAME, "NewTestUser")
                        .param(Domain.USER.ATTR_USERNAME, "NewTestUser")
                        .param(Domain.USER.ATTR_SURNAME, "NewTestUser")
                        .param(Domain.USER.ATTR_LANGUAGE, Locale.ENGLISH.toLanguageTag())
                        .param(Domain.USER.ATTR_TIMEZONE, DateTimeZone.UTC.getID())
                        .param(Domain.USER_ROLE.REFERENCE_NAME, UserRole.EXAM_ADMIN.name())
                        .param(PasswordChange.ATTR_NAME_NEW_PASSWORD, "12345678")
                        .param(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, "12345678"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(createdUser);
        assertEquals("NewTestUser", createdUser.name);

        // get newly created user and check equality
        final UserInfo createdUserGet = this.jsonMapper.readValue(
                this.mockMvc.perform(
                        get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/" + createdUser.uuid)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .header("Authorization", "Bearer " + token)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(createdUserGet);
        assertEquals(createdUser.getEntityKey(), createdUserGet.getEntityKey());
        assertFalse(createdUserGet.isActive());

        // check user activity log for newly created user
        final Page<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT
                                        + "?username=admin&activity_types=CREATE")
                                                .header("Authorization", "Bearer " + token)
                                                .header(HttpHeaders.CONTENT_TYPE,
                                                        MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(1 == logs.content.size());
        final UserActivityLog userActivityLog = logs.content.iterator().next();
        assertEquals("user1", userActivityLog.userUUID);
        assertEquals("USER", userActivityLog.entityType.name());
        assertEquals("CREATE", userActivityLog.activityType.name());
        assertEquals(createdUserGet.uuid, userActivityLog.entityId);
    }

// NOTE: this tests transaction rollback is working but for now only if a runtime exception is thrown on
//       UserDaoImpl.updateUser after the main record (UserRecord) is stored but the new roles are not
//       updated so far.
// TODO: make this test running separately in an test with UserDaoImpl mockup

//    @Test
//    public void modifyUserTestTransaction() throws Exception {
//        final String token = getSebAdminAccess();
//        final UserInfo user = this.jsonMapper.readValue(
//                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/user7")
//                        .header("Authorization", "Bearer " + token))
//                        .andExpect(status().isOk())
//                        .andReturn().getResponse().getContentAsString(),
//                new TypeReference<UserInfo>() {
//                });
//
//        assertNotNull(user);
//        assertEquals("User", user.name);
//        assertEquals("user1", user.userName);
//        assertEquals("user@nomail.nomail", user.email);
//        assertEquals("[EXAM_SUPPORTER]", String.valueOf(user.roles));
//
//        // change userName, email and roles
//        final UserMod modifyUser = new UserMod(new UserInfo(
//                user.getUuid(),
//                user.getInstitutionId(),
//                user.getName(),
//                "newUser1",
//                "newUser@nomail.nomail",
//                user.getActive(),
//                user.getLocale(),
//                user.getTimeZone(),
//                Stream.of(UserRole.EXAM_ADMIN.name(), UserRole.EXAM_SUPPORTER.name()).collect(Collectors.toSet())),
//                null, null);
//        final String modifyUserJson = this.jsonMapper.writeValueAsString(modifyUser);
//
//        final String contentAsString = this.mockMvc
//                .perform(post(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/save")
//                        .header("Authorization", "Bearer " + token)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(modifyUserJson))
//                .andReturn().getResponse().getContentAsString();
//
//        // double check by getting the user by UUID
//        final UserInfo unmodifiedUserResult = this.jsonMapper.readValue(
//                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/" + user.uuid)
//                        .header("Authorization", "Bearer " + token))
//                        .andExpect(status().isOk())
//                        .andReturn().getResponse().getContentAsString(),
//                new TypeReference<UserInfo>() {
//                });
//
//        assertNotNull(unmodifiedUserResult);
//        assertEquals("User", unmodifiedUserResult.name);
//        assertEquals("user1", unmodifiedUserResult.userName);
//        assertEquals("user@nomail.nomail", unmodifiedUserResult.email);
//        assertEquals("[EXAM_SUPPORTER]", String.valueOf(unmodifiedUserResult.roles));
//    }

    @Test
    public void modifyUserWithPUTMethod() throws Exception {
        final String token = getSebAdminAccess();
        final UserInfo user = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/user7")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(user);
        assertEquals("User", user.name);
        assertEquals("user1", user.username);
        assertEquals("user2@nomail.nomail", user.email);
        assertEquals("[EXAM_SUPPORTER]", String.valueOf(user.roles));

        // change userName, email and roles
        final UserInfo modifyUser = new UserInfo(
                user.uuid,
                user.getInstitutionId(),
                new DateTime(0, DateTimeZone.UTC),
                user.getName(),
                user.getSurname(),
                "newUser1",
                "newUser@nomail.nomail",
                user.getActive(),
                user.getLanguage(),
                user.getTimeZone(),
                Stream.of(UserRole.EXAM_ADMIN.name(), UserRole.EXAM_SUPPORTER.name()).collect(Collectors.toSet()));
        final String modifyUserJson = this.jsonMapper.writeValueAsString(modifyUser);

        UserInfo modifiedUserResult = this.jsonMapper.readValue(
                this.mockMvc.perform(put(this.endpoint + API.USER_ACCOUNT_ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modifyUserJson))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(modifiedUserResult);
        assertEquals(user.uuid, modifiedUserResult.uuid);
        assertEquals("User", modifiedUserResult.name);
        assertEquals("newUser1", modifiedUserResult.username);
        assertEquals("newUser@nomail.nomail", modifiedUserResult.email);
        assertEquals("[EXAM_ADMIN, EXAM_SUPPORTER]", String.valueOf(modifiedUserResult.roles));

        // double check by getting the user by UUID
        modifiedUserResult = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/"
                                + modifiedUserResult.uuid)
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(modifiedUserResult);
        assertEquals("User", modifiedUserResult.name);
        assertEquals("newUser1", modifiedUserResult.username);
        assertEquals("newUser@nomail.nomail", modifiedUserResult.email);
        assertEquals("[EXAM_ADMIN, EXAM_SUPPORTER]", String.valueOf(modifiedUserResult.roles));
    }

    @Test
    public void testModifyUserOnInactiveInstitutionNotAllowed() throws Exception {
        // create new institution with seb-admin that is not active
        final Institution institution = new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withPath(API.INSTITUTION_ENDPOINT)
                .withMethod(HttpMethod.POST)
                .withAttribute("name", "new institution")
                .withAttribute("urlSuffix", "new_inst")
                .withAttribute("active", "false")
                .withExpectedStatus(HttpStatus.OK)
                .getAsObject(new TypeReference<Institution>() {
                });

        assertNotNull(institution);
        assertNotNull(institution.id);
        assertEquals("new institution", institution.name);

        // try to create a user for this institution should not be possible
        final Collection<APIMessage> errors = this.jsonMapper.readValue(
                this.mockMvc.perform(post(this.endpoint + API.USER_ACCOUNT_ENDPOINT)
                        .header("Authorization", "Bearer " + getSebAdminAccess())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(Domain.USER.ATTR_INSTITUTION_ID, String.valueOf(institution.id))
                        .param(Domain.USER.ATTR_NAME, "NewTestUser")
                        .param(Domain.USER.ATTR_USERNAME, "NewTestUser")
                        .param(Domain.USER.ATTR_SURNAME, "NewTestUser")
                        .param(Domain.USER.ATTR_LANGUAGE, Locale.ENGLISH.toLanguageTag())
                        .param(Domain.USER.ATTR_TIMEZONE, DateTimeZone.UTC.getID())
                        .param(Domain.USER_ROLE.REFERENCE_NAME, UserRole.EXAM_ADMIN.name())
                        .param(PasswordChange.ATTR_NAME_NEW_PASSWORD, "12345678")
                        .param(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, "12345678"))
                        .andExpect(status().isBadRequest())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Collection<APIMessage>>() {
                });

        assertNotNull(errors);
        assertTrue(errors.size() == 1);
        assertEquals(
                "Illegal API request argument",
                errors.iterator().next().systemMessage);
        assertEquals(
                "User within an inactive institution cannot be created nor modified",
                errors.iterator().next().details);
    }

//    @Test
//    public void modifyUserWithPOSTMethod() throws Exception {
//        final String token = getSebAdminAccess();
//
//        final UserInfo modifiedUser = this.jsonMapper.readValue(
//                this.mockMvc.perform(patch(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/user4")
//                        .header("Authorization", "Bearer " + token)
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .param("name", "PostModifyTest"))
//                        .andExpect(status().isOk())
//                        .andReturn().getResponse().getContentAsString(),
//                new TypeReference<UserInfo>() {
//                });
//
//        assertNotNull(modifiedUser);
//        assertEquals("PostModifyTest", modifiedUser.name);
//
//        // check validation
//        final Collection<APIMessage> errors = this.jsonMapper.readValue(
//                this.mockMvc.perform(patch(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/user4")
//                        .header("Authorization", "Bearer " + token)
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .param("name", "P"))
//                        .andExpect(status().isBadRequest())
//                        .andReturn().getResponse().getContentAsString(),
//                new TypeReference<Collection<APIMessage>>() {
//                });
//
//        assertNotNull(errors);
//        assertFalse(errors.isEmpty());
//        final APIMessage error = errors.iterator().next();
//        assertEquals("1200", error.messageCode);
//    }

    @Test
    public void testOwnerModifyPossibleForExamAdmin() throws Exception {
        final String examAdminToken1 = getExamAdmin1();
        final UserInfo examAdmin = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/me")
                        .header("Authorization", "Bearer " + examAdminToken1))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        final String modifiedUserJson = this.jsonMapper.writeValueAsString(examAdmin);

        this.mockMvc.perform(put(this.endpoint + API.USER_ACCOUNT_ENDPOINT)
                .header("Authorization", "Bearer " + examAdminToken1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(modifiedUserJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void institutionalAdminTryToCreateOrModifyUserForOtherInstituionNotPossible() throws Exception {

        final String token = getAdminInstitution1Access();
        this.mockMvc.perform(post(this.endpoint + API.USER_ACCOUNT_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param(Domain.USER.ATTR_INSTITUTION_ID, "2")
                .param(Domain.USER.ATTR_NAME, "NewTestUser")
                .param(PasswordChange.ATTR_NAME_NEW_PASSWORD, "12345678")
                .param(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, "12345678"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        final UserInfo userInfo = new UserInfo(
                "NewTestUser", 2L, new DateTime(0, DateTimeZone.UTC), "NewTestUser", "", "NewTestUser",
                "", true, Locale.CANADA, DateTimeZone.UTC,
                new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name())));
        final String newUserJson = this.jsonMapper.writeValueAsString(userInfo);
        this.mockMvc.perform(put(this.endpoint + API.USER_ACCOUNT_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newUserJson))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void unauthorizedAdminTryToCreateUserNotPossible() throws Exception {

        final String token = getExamAdmin1();
        this.mockMvc.perform(post(this.endpoint + API.USER_ACCOUNT_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param(Domain.USER.ATTR_INSTITUTION_ID, "2")
                .param(Domain.USER.ATTR_NAME, "NewTestUser")
                .param(PasswordChange.ATTR_NAME_NEW_PASSWORD, "12345678")
                .param(PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD, "12345678"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        final UserInfo userInfo = new UserInfo(
                "NewTestUser", 2L, new DateTime(0, DateTimeZone.UTC), "NewTestUser", "", "NewTestUser",
                "", true, Locale.CANADA, DateTimeZone.UTC,
                new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name())));
        //final UserMod newUser = new UserMod(userInfo, "12345678", "12345678");
        final String newUserJson = this.jsonMapper.writeValueAsString(userInfo);
        this.mockMvc.perform(put(this.endpoint + API.USER_ACCOUNT_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newUserJson))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void modifyUserPassword() throws Exception {
        final String examAdminToken1 = getExamAdmin1();
        assertNotNull(examAdminToken1);

        // a SEB Server Admin now changes the password of ExamAdmin1
        final String sebAdminToken = getSebAdminAccess();
        final UserInfo examAdmin1 = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/user4")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        final PasswordChange passwordChange = new PasswordChange(
                examAdmin1.uuid,
                "admin",
                "newPassword",
                "newPassword");
        final String modifiedUserJson = this.jsonMapper.writeValueAsString(passwordChange);

        this.mockMvc.perform(
                put(this.endpoint + API.USER_ACCOUNT_ENDPOINT + API.PASSWORD_PATH_SEGMENT)
                        .header("Authorization", "Bearer " + sebAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modifiedUserJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // now it should not be possible to get a access token for ExamAdmin1 with the standard password
        try {
            getExamAdmin1();
            fail("AssertionError expected here");
        } catch (final AssertionError e) {
            assertEquals("Status expected:<200> but was:<400>", e.getMessage());
        }

        // it should also not be possible to use an old token again after password change
        this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/me")
                .header("Authorization", "Bearer " + examAdminToken1))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // but it should be possible to get a new access token and request again
        final String examAdminToken2 = obtainAccessToken("examAdmin1", "newPassword");
        this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/me")
                .header("Authorization", "Bearer " + examAdminToken2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void modifyUserPasswordInvalidPasswords() throws Exception {
        final String sebAdminToken = getSebAdminAccess();
        final UserInfo examAdmin1 = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/user4")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        // must be longer then 8 chars
        PasswordChange passwordChange = new PasswordChange(
                examAdmin1.uuid,
                "admin",
                "new",
                "new");
        String modifiedUserJson = this.jsonMapper.writeValueAsString(passwordChange);

        List<APIMessage> messages = this.jsonMapper.readValue(
                this.mockMvc.perform(
                        put(this.endpoint + API.USER_ACCOUNT_ENDPOINT + API.PASSWORD_PATH_SEGMENT)
                                .header("Authorization", "Bearer " + sebAdminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(modifiedUserJson))
                        .andExpect(status().isBadRequest())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<APIMessage>>() {
                });

        assertNotNull(messages);
        assertTrue(1 == messages.size());
        assertEquals("1200", messages.get(0).messageCode);
        assertEquals("[user, newPassword, size, 8, 255, new]", String.valueOf(messages.get(0).getAttributes()));

        // wrong password retype
        passwordChange = new PasswordChange(
                examAdmin1.uuid,
                "admin",
                "12345678",
                "87654321");
        modifiedUserJson = this.jsonMapper.writeValueAsString(passwordChange);

        messages = this.jsonMapper.readValue(
                this.mockMvc.perform(
                        put(this.endpoint + API.USER_ACCOUNT_ENDPOINT + API.PASSWORD_PATH_SEGMENT)
                                .header("Authorization", "Bearer " + sebAdminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(modifiedUserJson))
                        .andExpect(status().isBadRequest())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<APIMessage>>() {
                });

        assertNotNull(messages);
        assertTrue(1 == messages.size());
        assertEquals("1200", messages.get(0).messageCode);
    }

    @Test
    public void deactivateUserAccount() throws Exception {
        final String timeNow = DateTime.now(DateTimeZone.UTC).toString(Constants.STANDARD_DATE_TIME_FORMATTER);
        // only a SEB Administrator or an Institutional administrator should be able to deactivate a user-account
        final String examAdminToken = getExamAdmin1();
        this.mockMvc.perform(post(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/user4/inactive")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Bearer " + examAdminToken))
                .andExpect(status().isForbidden());

        if ("0".equals(timeNow)) {
            System.out.println("******");
        }

        // With SEB Administrator it should work
        final String sebAdminToken = getSebAdminAccess();
        final EntityProcessingReport report = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(post(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/user4/inactive")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<EntityProcessingReport>() {
                });

        assertNotNull(report);
        assertNotNull(report.source);
        assertFalse(report.results.isEmpty());
        assertTrue(report.errors.isEmpty());
        assertTrue(report.source.size() == 1);

        // get user and check activity
        final EntityKey key = report.source.iterator().next();
        final UserInfo user = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/" + key.modelId)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(user);
        assertFalse(user.isActive());

        // check also user activity log
        final Page<UserActivityLog> userLogs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT
                                        + "/?user=user1&from=" + timeNow)
                                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                                .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(userLogs);
        assertTrue(userLogs.content.size() == 1);
        final UserActivityLog userLog = userLogs.content.iterator().next();
        assertEquals(UserLogActivityType.DEACTIVATE, userLog.activityType);
        assertEquals("user4", userLog.entityId);
    }

    @Test
    public void activateUserAccount() throws Exception {
        final String timeNow = DateTime.now(DateTimeZone.UTC).toString(Constants.STANDARD_DATE_TIME_FORMATTER);
        // only a SEB Administrator or an Institutional administrator should be able to deactivate a user-account
        final String examAdminToken = getExamAdmin1();
        this.mockMvc.perform(post(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/user6/active")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Bearer " + examAdminToken))
                .andExpect(status().isForbidden());

        // With SEB Administrator it should work
        final String sebAdminToken = getSebAdminAccess();
        final EntityProcessingReport report = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(post(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/user6/active")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<EntityProcessingReport>() {
                });

        assertNotNull(report);
        assertNotNull(report.source);
        assertFalse(report.results.isEmpty());
        assertTrue(report.errors.isEmpty());
        assertTrue(report.source.size() == 1);

        // get user and check activity
        final EntityKey key = report.source.iterator().next();
        final UserInfo user = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/" + key.modelId)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(user);
        assertTrue(user.isActive());

        // check also user activity log
        final Page<UserActivityLog> userLogs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT
                                + "?user=user1&from=" + timeNow)
                                        .header("Authorization", "Bearer " + sebAdminToken)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(userLogs);
        assertTrue(userLogs.content.size() == 1);
        final UserActivityLog userLog = userLogs.content.iterator().next();
        assertEquals(UserLogActivityType.ACTIVATE, userLog.activityType);
        assertEquals("user6", userLog.entityId);
    }

    @Test
    public void testGeneralAllActiveInactiveEndpoint() throws Exception {
        final String sebAdminToken = getSebAdminAccess();

        // all active for the own institution
        Page<UserInfo> usersPage = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/active")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(usersPage);
        assertTrue(usersPage.pageSize == 3);
        assertEquals("[user1, user2, user5]", getOrderedUUIDs(usersPage.content));

        // all inactive of the own institution
        usersPage = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/inactive")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(usersPage);
        assertTrue(usersPage.pageSize == 0);
        assertEquals("[]", getOrderedUUIDs(usersPage.content));

        // all active of institution 2
        usersPage = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT
                                + "/active?institutionId=2")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(usersPage);
        assertTrue(usersPage.pageSize == 3);
        assertEquals("[user3, user4, user7]", getOrderedUUIDs(usersPage.content));

        // all inactive of institution 2
        usersPage = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT
                                + "/inactive?institutionId=2")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserInfo>>() {
                });

        assertNotNull(usersPage);
        assertTrue(usersPage.pageSize == 1);
        assertEquals("[user6]", getOrderedUUIDs(usersPage.content));
    }

    @Test
    public void testGeneralListEndpoint() throws Exception {
        final String sebAdminToken = getSebAdminAccess();

        // for SEB Admin it should be possible to get from different institutions
        Collection<UserInfo> users = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + API.USER_ACCOUNT_ENDPOINT
                                        + "/list?modelIds=user1,user2,user6,user7")
                                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                                .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Collection<UserInfo>>() {
                });

        assertNotNull(users);
        assertTrue(users.size() == 4);
        assertEquals("[user1, user2, user6, user7]", getOrderedUUIDs(users));

        // for an institutional admin it should only be possible to get from own institution
        final String instAdminToken = getAdminInstitution2Access();
        users = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + API.USER_ACCOUNT_ENDPOINT
                                        + "/list?modelIds=user1,user2,user6,user7")
                                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                                .header("Authorization", "Bearer " + instAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Collection<UserInfo>>() {
                });

        assertNotNull(users);
        assertTrue(users.size() == 2);
        assertEquals("[user6, user7]", getOrderedUUIDs(users));
    }

    @Test
    public void testGeneralNamesEndpoint() throws Exception {
        final String sebAdminToken = getSebAdminAccess();

        // for SEB Admin
        Collection<EntityName> names = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/names" + "?institutionId=1")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Collection<EntityName>>() {
                });

        assertNotNull(names);
        assertTrue(names.size() == 3);
        assertEquals("[EntityName [entityType=USER, modelId=user1, name=admin ( SEBAdmin)], " +
                "EntityName [entityType=USER, modelId=user2, name=inst1Admin ( Institutional1 Admin)], " +
                "EntityName [entityType=USER, modelId=user5, name=examSupporter ( Exam Supporter)]]", names.toString());

        // for an institutional admin 2
        final String instAdminToken = getAdminInstitution2Access();
        names = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/names" + "?institutionId=2")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .header("Authorization", "Bearer " + instAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Collection<EntityName>>() {
                });

        assertNotNull(names);
        assertTrue(names.size() == 4);
        assertEquals("[EntityName [entityType=USER, modelId=user3, name=inst2Admin ( Institutional2 Admin)], " +
                "EntityName [entityType=USER, modelId=user4, name=examAdmin1 ( ExamAdmin1)], " +
                "EntityName [entityType=USER, modelId=user6, name=deactivatedUser ( Deactivated)], " +
                "EntityName [entityType=USER, modelId=user7, name=user1 ( User)]]", names.toString());

        // for an institutional admin 2 only active
        names = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + API.USER_ACCOUNT_ENDPOINT + "/names?active=true&institutionId=2")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .header("Authorization", "Bearer " + instAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Collection<EntityName>>() {
                });

        assertNotNull(names);
        assertTrue(names.size() == 3);
        assertEquals("[EntityName [entityType=USER, modelId=user3, name=inst2Admin ( Institutional2 Admin)], " +
                "EntityName [entityType=USER, modelId=user4, name=examAdmin1 ( ExamAdmin1)], " +
                "EntityName [entityType=USER, modelId=user7, name=user1 ( User)]]", names.toString());
    }

//    @Test
//    public void createWithRoleAddRoleDeleteRole() throws Exception {
//        final String token = getSebAdminAccess();
//        UserInfo createdUser = this.jsonMapper.readValue(
//                this.mockMvc.perform(post(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT)
//                        .header("Authorization", "Bearer " + token)
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .param(Domain.USER.ATTR_NAME, "NewTestUser")
//                        .param(Domain.USER.ATTR_USERNAME, "NewTestUser")
//                        .param(Domain.USER.ATTR_LOCALE, Locale.ENGLISH.toLanguageTag())
//                        .param(Domain.USER.ATTR_TIMEZONE, DateTimeZone.UTC.getID())
//                        .param(UserMod.ATTR_NAME_NEW_PASSWORD, "12345678")
//                        .param(UserMod.ATTR_NAME_RETYPED_NEW_PASSWORD, "12345678"))
//                        .andExpect(status().isOk())
//                        .andReturn().getResponse().getContentAsString(),
//                new TypeReference<UserInfo>() {
//                });
//
//        assertNotNull(createdUser);
//        assertEquals("NewTestUser", createdUser.name);
//        assertEquals("[]", String.valueOf(createdUser.roles));
//
//        // add two roles
//        createdUser = this.jsonMapper.readValue(
//                this.mockMvc.perform(patch(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/" + createdUser.uuid)
//                        .header("Authorization", "Bearer " + token)
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .param(USER_ROLE.REFERENCE_NAME, "EXAM_SUPPORTER", "EXAM_ADMIN"))
//                        .andExpect(status().isOk())
//                        .andReturn().getResponse().getContentAsString(),
//                new TypeReference<UserInfo>() {
//                });
//
//        assertNotNull(createdUser);
//        assertEquals("NewTestUser", createdUser.name);
//        assertEquals("[]", String.valueOf(createdUser.roles));
//    }

    private UserInfo getUserInfo(final String name, final Collection<UserInfo> infos) {
        try {
            return infos
                    .stream()
                    .filter(ui -> ui.username.equals(name))
                    .findFirst()
                    .orElseThrow(NoSuchElementException::new);
        } catch (final Exception e) {
            return null;
        }
    }

}
