/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api;

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

import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.model.user.UserFilter;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.webservice.weblayer.api.RestAPI;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class UserAPITest extends AdministrationAPIIntegrationTest {

    @Test
    public void getMyUserInfo() throws Exception {
        String sebAdminAccessToken = getSebAdminAccess();
        String contentAsString = this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/me")
                .header("Authorization", "Bearer " + sebAdminAccessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(
                "{\"uuid\":\"user1\","
                        + "\"institutionId\":1,"
                        + "\"name\":\"SEBAdmin\","
                        + "\"userName\":\"admin\","
                        + "\"email\":\"admin@nomail.nomail\","
                        + "\"active\":true,"
                        + "\"locale\":\"en\","
                        + "\"timezone\":\"UTC\","
                        + "\"userRoles\":[\"SEB_SERVER_ADMIN\"]}",
                contentAsString);

        sebAdminAccessToken = getAdminInstitution1Access();
        contentAsString = this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/me")
                .header("Authorization", "Bearer " + sebAdminAccessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(
                "{\"uuid\":\"user2\","
                        + "\"institutionId\":1,"
                        + "\"name\":\"Institutional1 Admin\","
                        + "\"userName\":\"inst1Admin\","
                        + "\"email\":\"admin@nomail.nomail\","
                        + "\"active\":true,"
                        + "\"locale\":\"en\","
                        + "\"timezone\":\"UTC\","
                        + "\"userRoles\":[\"INSTITUTIONAL_ADMIN\"]}",
                contentAsString);
    }

    @Test
    public void getUserInfoWithUUID() throws Exception {
        final String sebAdminAccessToken = getSebAdminAccess();
        String contentAsString = this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/user2")
                .header("Authorization", "Bearer " + sebAdminAccessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(
                "{\"uuid\":\"user2\","
                        + "\"institutionId\":1,"
                        + "\"name\":\"Institutional1 Admin\","
                        + "\"userName\":\"inst1Admin\","
                        + "\"email\":\"admin@nomail.nomail\","
                        + "\"active\":true,"
                        + "\"locale\":\"en\","
                        + "\"timezone\":\"UTC\","
                        + "\"userRoles\":[\"INSTITUTIONAL_ADMIN\"]}",
                contentAsString);

        final String adminInstitution2AccessToken = getAdminInstitution2Access();
        contentAsString = this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/user1")
                .header("Authorization", "Bearer " + adminInstitution2AccessToken))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertEquals(
                "{\"messageCode\":\"1001\","
                        + "\"systemMessage\":\"FORBIDDEN\","
                        + "\"details\":\"No grant: READ_ONLY on type: USER entity institution: 1 entity owner: user1 for user: user3\","
                        + "\"attributes\":[]}",
                contentAsString);
    }

    @Test
    public void getAllUserInfoNoFilter() throws Exception {
        String token = getSebAdminAccess();
        List<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserInfo>>() {
                });

        // expecting all users for a SEBAdmin except inactive.
        assertNotNull(userInfos);
        assertTrue(userInfos.size() == 6);
        assertNotNull(getUserInfo("admin", userInfos));
        assertNotNull(getUserInfo("inst1Admin", userInfos));
        assertNotNull(getUserInfo("examSupporter", userInfos));
        assertNotNull(getUserInfo("inst2Admin", userInfos));
        assertNotNull(getUserInfo("examAdmin1", userInfos));
        assertNotNull(getUserInfo("user1", userInfos));

        token = getAdminInstitution1Access();
        userInfos = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserInfo>>() {
                });

        // expecting all users of institution 1 for Institutional Admin of institution 1
        assertNotNull(userInfos);
        assertTrue(userInfos.size() == 3);
        assertNotNull(getUserInfo("admin", userInfos));
        assertNotNull(getUserInfo("inst1Admin", userInfos));
        assertNotNull(getUserInfo("examSupporter", userInfos));
    }

    @Test
    public void getAllUserInfoWithSearchInactive() throws Exception {
        final UserFilter filter = UserFilter.ofInactive();
        final String filterJson = this.jsonMapper.writeValueAsString(filter);

        final String token = getSebAdminAccess();
        final List<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(filterJson))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.size() == 1);
        assertNotNull(getUserInfo("deactivatedUser", userInfos));
    }

    @Test
    public void getAllUserInfoWithSearchUsernameLike() throws Exception {
        final UserFilter filter = new UserFilter(null, null, "exam", null, null, null);
        final String filterJson = this.jsonMapper.writeValueAsString(filter);

        final String token = getSebAdminAccess();
        final List<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(filterJson))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserInfo>>() {
                });

        assertNotNull(userInfos);
        assertTrue(userInfos.size() == 2);
        assertNotNull(getUserInfo("examAdmin1", userInfos));
        assertNotNull(getUserInfo("examSupporter", userInfos));
    }

    @Test
    public void testOwnerGet() throws Exception {
        final String examAdminToken1 = getExamAdmin1();
        this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/me")
                .header("Authorization", "Bearer " + examAdminToken1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void createUserTest() throws Exception {
        final UserInfo userInfo = new UserInfo(
                null, 1L, "NewTestUser", "NewTestUser",
                "", true, Locale.CANADA, DateTimeZone.UTC,
                new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name())));
        final UserMod newUser = new UserMod(userInfo, "123", "123");
        final String newUserJson = this.jsonMapper.writeValueAsString(newUser);

        final String token = getSebAdminAccess();
        final UserInfo createdUser = this.jsonMapper.readValue(
                this.mockMvc.perform(put(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(newUserJson))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(createdUser);
        assertEquals("NewTestUser", createdUser.name);

        // get newly created user and check equality
        final UserInfo createdUserGet = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/" + createdUser.uuid)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(createdUserGet);
        assertEquals(createdUser, createdUserGet);

        // check user activity log for newly created user
        final List<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG + "/user1?activityTypes=CREATE")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(1 == logs.size());
        final UserActivityLog userActivityLog = logs.get(0);
        assertEquals("user1", userActivityLog.userUUID);
        assertEquals("USER", userActivityLog.entityType.name());
        assertEquals("CREATE", userActivityLog.activityType.name());
        assertEquals(createdUserGet.uuid, userActivityLog.entityId);
    }

    @Test
    public void modifyUserTest() throws Exception {
        final String token = getSebAdminAccess();
        final UserInfo user = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/user7")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(user);
        assertEquals("User", user.name);
        assertEquals("user1", user.userName);
        assertEquals("user@nomail.nomail", user.email);
        assertEquals("[EXAM_SUPPORTER]", String.valueOf(user.roles));

        // change userName, email and roles
        final UserMod modifiedUser = new UserMod(new UserInfo(
                user.getUuid(),
                user.getInstitutionId(),
                user.getName(),
                "newUser1",
                "newUser@nomail.nomail",
                user.getActive(),
                user.getLocale(),
                user.getTimeZone(),
                Stream.of(UserRole.EXAM_ADMIN.name(), UserRole.EXAM_SUPPORTER.name()).collect(Collectors.toSet())),
                null, null);
        final String modifiedUserJson = this.jsonMapper.writeValueAsString(modifiedUser);

        UserInfo modifiedUserResult = this.jsonMapper.readValue(
                this.mockMvc.perform(post(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/save")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(modifiedUserJson))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(modifiedUserResult);
        assertEquals("User", modifiedUserResult.name);
        assertEquals("newUser1", modifiedUserResult.userName);
        assertEquals("newUser@nomail.nomail", modifiedUserResult.email);
        assertEquals("[EXAM_ADMIN, EXAM_SUPPORTER]", String.valueOf(modifiedUserResult.roles));

        // double check by getting the user by uuis
        modifiedUserResult = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/" + modifiedUserResult.uuid)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        assertNotNull(modifiedUserResult);
        assertEquals("User", modifiedUserResult.name);
        assertEquals("newUser1", modifiedUserResult.userName);
        assertEquals("newUser@nomail.nomail", modifiedUserResult.email);
        assertEquals("[EXAM_ADMIN, EXAM_SUPPORTER]", String.valueOf(modifiedUserResult.roles));
    }

    @Test
    public void testOwnerModifyPossible() throws Exception {
        final String examAdminToken1 = getExamAdmin1();
        final UserInfo examAdmin = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/me")
                        .header("Authorization", "Bearer " + examAdminToken1))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        final UserMod modifiedUser = new UserMod(examAdmin, null, null);
        final String modifiedUserJson = this.jsonMapper.writeValueAsString(modifiedUser);

        this.mockMvc.perform(post(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/save")
                .header("Authorization", "Bearer " + examAdminToken1)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(modifiedUserJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

    }

    @Test
    public void institutionalAdminTryToCreateOrModifyUserForOtherInstituionNotPossible() throws Exception {
        final UserInfo userInfo = new UserInfo(
                null, 2L, "NewTestUser", "NewTestUser",
                "", true, Locale.CANADA, DateTimeZone.UTC,
                new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name())));
        final UserMod newUser = new UserMod(userInfo, "123", "123");
        final String newUserJson = this.jsonMapper.writeValueAsString(newUser);

        final String token = getAdminInstitution1Access();
        this.mockMvc.perform(put(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/create")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(newUserJson))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        this.mockMvc.perform(post(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/save")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(newUserJson))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void unauthorizedAdminTryToCreateUserNotPossible() throws Exception {
        final UserInfo userInfo = new UserInfo(
                null, 2L, "NewTestUser", "NewTestUser",
                "", true, Locale.CANADA, DateTimeZone.UTC,
                new HashSet<>(Arrays.asList(UserRole.EXAM_ADMIN.name())));
        final UserMod newUser = new UserMod(userInfo, "123", "123");
        final String newUserJson = this.jsonMapper.writeValueAsString(newUser);

        final String token = getExamAdmin1();
        this.mockMvc.perform(put(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/create")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(newUserJson))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        this.mockMvc.perform(post(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/save")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
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
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/user4")
                        .header("Authorization", "Bearer " + sebAdminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<UserInfo>() {
                });

        final UserMod modifiedUser = new UserMod(
                UserInfo.of(examAdmin1),
                "newPassword",
                "newPassword");
        final String modifiedUserJson = this.jsonMapper.writeValueAsString(modifiedUser);

        this.mockMvc.perform(post(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/save")
                .header("Authorization", "Bearer " + sebAdminToken)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
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
        this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/me")
                .header("Authorization", "Bearer " + examAdminToken1))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // but it should be possible to get a new access token and request again
        final String examAdminToken2 = obtainAccessToken("examAdmin1", "newPassword");
        this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACCOUNT + "/me")
                .header("Authorization", "Bearer " + examAdminToken2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private UserInfo getUserInfo(final String name, final Collection<UserInfo> infos) {
        return infos
                .stream()
                .filter(ui -> ui.userName.equals(name))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }

}
