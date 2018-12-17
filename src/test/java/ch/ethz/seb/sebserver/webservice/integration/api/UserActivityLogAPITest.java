/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.Test;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.webservice.weblayer.api.RestAPI;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class UserActivityLogAPITest extends AdministrationAPIIntegrationTest {

    @Test
    public void getAllAsSEBAdmin() throws Exception {
        final String token = getSebAdminAccess();
        final List<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(5 == logs.size());
    }

    @Test
    public void getAllAsSEBAdminForUser() throws Exception {
        final String token = getSebAdminAccess();
        List<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG + "/user4")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(2 == logs.size());

        logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG + "/user2")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(1 == logs.size());
    }

    @Test
    public void getAllAsSEBAdminInTimeRange() throws Exception {
        final String token = getSebAdminAccess();
        List<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG + "?from=2")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(4 == logs.size());

        logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG + "?from=2&to=3")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(1 == logs.size());

        logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG + "?from=2&to=4")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(2 == logs.size());
    }

    @Test
    public void getAllAsSEBAdminForActivityType() throws Exception {
        final String token = getSebAdminAccess();
        List<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG + "?activityTypes=CREATE")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(3 == logs.size());

        logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG + "?activityTypes=CREATE,MODIFY")
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(5 == logs.size());
    }

    @Test
    public void getAllAsSEBAdminForEntityType() throws Exception {
        final String token = getSebAdminAccess();
        List<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG + "?entityTypes=INSTITUTION")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(1 == logs.size());

        logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG
                                        + "?entityTypes=INSTITUTION,EXAM")
                                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(5 == logs.size());
    }

    @Test
    public void getAllAsInstitutionalAdmin() throws Exception {
        final String token = getAdminInstitution1Access();
        final List<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG)
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(2 == logs.size());
    }

    @Test
    public void getNoPermission() throws Exception {
        String token = getExamAdmin1();

        // no privilege at all
        this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        // no privilege at all
        this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG + "/user4")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // no privilege to query logs of users of other institution
        token = getAdminInstitution1Access();
        final List<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + RestAPI.ENDPOINT_USER_ACTIVITY_LOG + "/user4")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<List<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(logs.isEmpty());
    }

}
