/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.model.user.UserFilter;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;

public class UserAPITest extends AdministrationAPIIntegrationTest {

    @Test
    public void getMyUserInfo() throws Exception {
        String sebAdminAccessToken = getSebAdminAccess();
        String contentAsString = this.mockMvc.perform(get(this.endpoint + "/useraccount/me")
                .header("Authorization", "Bearer " + sebAdminAccessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(
                "{\"uuid\":\"1\","
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
        contentAsString = this.mockMvc.perform(get(this.endpoint + "/useraccount/me")
                .header("Authorization", "Bearer " + sebAdminAccessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(
                "{\"uuid\":\"2\","
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
        String contentAsString = this.mockMvc.perform(get(this.endpoint + "/useraccount/2")
                .header("Authorization", "Bearer " + sebAdminAccessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(
                "{\"uuid\":\"2\","
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
        contentAsString = this.mockMvc.perform(get(this.endpoint + "/useraccount/1")
                .header("Authorization", "Bearer " + adminInstitution2AccessToken))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertEquals(
                "{\"messageCode\":\"1001\","
                        + "\"systemMessage\":\"FORBIDDEN\","
                        + "\"details\":\"No grant: READ_ONLY on type: USER entity institution: 1 entity owner: null for user: inst2Admin\","
                        + "\"attributes\":[]}",
                contentAsString);
    }

    @Test
    public void getAllUserInfoNoFilter() throws Exception {
        String token = getSebAdminAccess();
        List<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + "/useraccount")
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
                this.mockMvc.perform(get(this.endpoint + "/useraccount")
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

        // TODO more tests
    }

    @Test
    public void getAllUserInfoWithSearchInactive() throws Exception {
        final UserFilter filter = UserFilter.ofInactive();
        final String filterJson = this.jsonMapper.writeValueAsString(filter);

        final String token = getSebAdminAccess();
        final List<UserInfo> userInfos = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + "/useraccount")
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
                this.mockMvc.perform(get(this.endpoint + "/useraccount")
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

    private UserInfo getUserInfo(final String name, final Collection<UserInfo> infos) {
        return infos
                .stream()
                .filter(ui -> ui.userName.equals(name))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }

}
