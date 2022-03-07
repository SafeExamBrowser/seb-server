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

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class UserActivityLogAPITest extends AdministrationAPIIntegrationTester {

    @Test
    public void getAllAsSEBAdmin() throws Exception {
        final String token = getSebAdminAccess();
        final Page<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(5 == logs.content.size());
    }

    @Test
    public void getAllAsInstAdmin2ForUser() throws Exception {
        final String token = getAdminInstitution2Access();
        // for a user in another institution, the institution has to be defined
        Page<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(
                                this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT + "?username=examAdmin1&institutionId=2")
                                        .header("Authorization", "Bearer " + token)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(2 == logs.content.size());

        // for a user in the same institution no institution is needed
        logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT + "?username=inst2Admin")
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(1 == logs.content.size());
    }

    @Test
    public void getAllAsInst2AdminInTimeRange() throws Exception {
        final DateTime zeroDate = DateTime.parse("1970-01-01T00:00:00Z", Constants.STANDARD_DATE_TIME_FORMATTER);
        assertEquals("0", String.valueOf(zeroDate.getMillis()));
        final String sec2 = zeroDate.plus(1000).toString(Constants.STANDARD_DATE_TIME_FORMATTER);
        final String sec4 = zeroDate.plus(4000).toString(Constants.STANDARD_DATE_TIME_FORMATTER);
        final String sec5 = zeroDate.plus(5000).toString(Constants.STANDARD_DATE_TIME_FORMATTER);
        final String sec6 = zeroDate.plus(6000).toString(Constants.STANDARD_DATE_TIME_FORMATTER);

        final String token = getAdminInstitution2Access();
        Page<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc.perform(
                        get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT + "?institutionId=2&from=" + sec2)
                                .header("Authorization", "Bearer " + token)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(3 == logs.content.size());

        logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT + "?institutionId=2&from="
                                + sec2 + "&to=" + sec4)
                                        .header("Authorization", "Bearer " + token)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(1 == logs.content.size());

        logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT + "?institutionId=2&from=" + sec2
                                        + "&to=" + sec5)
                                                .header("Authorization", "Bearer " + token)
                                                .header(HttpHeaders.CONTENT_TYPE,
                                                        MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(2 == logs.content.size());

        logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT + "?institutionId=2&from=" + sec2
                                        + "&to=" + sec6)
                                                .header("Authorization", "Bearer " + token)
                                                .header(HttpHeaders.CONTENT_TYPE,
                                                        MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(3 == logs.content.size());
    }

    @Test
    public void getAllAsSEBAdminForActivityType() throws Exception {
        final String token = getSebAdminAccess();
        Page<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc.perform(
                        get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT + "?activity_types=CREATE")
                                .header("Authorization", "Bearer " + token)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(3 == logs.content.size());

        logs = this.jsonMapper.readValue(
                this.mockMvc.perform(
                        get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT + "?institutionId=1&activity_types=CREATE")
                                .header("Authorization", "Bearer " + token)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(1 == logs.content.size());

        logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT
                                        + "?institutionId=1&activity_types=CREATE,MODIFY")
                                                .header("Authorization", "Bearer " + token)
                                                .header(HttpHeaders.CONTENT_TYPE,
                                                        MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(2 == logs.content.size());

        // for other institution (2)
        final String adminInstitution2Access = getAdminInstitution2Access();
        logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT
                                        + "?institutionId=2&activity_types=CREATE,MODIFY")
                                                .header("Authorization", "Bearer " + adminInstitution2Access)
                                                .header(HttpHeaders.CONTENT_TYPE,
                                                        MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(3 == logs.content.size());
    }

    @Test
    public void getAllAsSEBAdminForEntityType() throws Exception {
        final String token = getSebAdminAccess();
        Page<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT
                                + "?institutionId=1&entity_types=INSTITUTION")
                                        .header("Authorization", "Bearer " + token)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(1 == logs.content.size());

        logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT
                                        + "?institutionId=1&entity_types=INSTITUTION,EXAM")
                                                .header("Authorization", "Bearer " + token)
                                                .header(HttpHeaders.CONTENT_TYPE,
                                                        MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(2 == logs.content.size());

        final String adminInstitution2Access = getAdminInstitution2Access();
        logs = this.jsonMapper.readValue(
                this.mockMvc
                        .perform(
                                get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT
                                        + "?entity_types=INSTITUTION,EXAM&institutionId=2")
                                                .header("Authorization", "Bearer " + adminInstitution2Access)
                                                .header(HttpHeaders.CONTENT_TYPE,
                                                        MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(3 == logs.content.size());
    }

    @Test
    public void getAllAsInstitutionalAdmin() throws Exception {
        final String token = getAdminInstitution1Access();
        final Page<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(2 == logs.content.size());
    }

    @Test
    public void getNoPermission() throws Exception {
        String token = getExamAdmin1();

        // no privilege at all
        this.mockMvc.perform(get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(status().isForbidden());
        // no privilege at all
        this.mockMvc.perform(get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT + "?user=user4")
                .header("Authorization", "Bearer " + token)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(status().isForbidden());

        // no privilege to query logs of users of other institution for institutional admin
        token = getAdminInstitution1Access();
        final Page<UserActivityLog> logs = this.jsonMapper.readValue(
                this.mockMvc.perform(get(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT + "?username=examAdmin1")
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<Page<UserActivityLog>>() {
                });

        assertNotNull(logs);
        assertTrue(logs.content.isEmpty());
    }

    @Test
    public void testReadonly() throws Exception {
        final String token = getSebAdminAccess();
        this.mockMvc
                .perform(put(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .content("{"
                                + "  \"id\" : 3,"
                                + "  \"userUuid\" : \"userUUID\","
                                + "  \"username\" : \"username\","
                                + "  \"timestamp\" : 123,"
                                + "  \"activityType\" : \"EXPORT\","
                                + "  \"entityType\" : \"USER\","
                                + "  \"entityId\" : \"5\","
                                + "  \"message\" : \"message\""
                                + "}"))
                .andExpect(status().isForbidden());

        final MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        multiValueMap.add("institutionId", "1");
        this.mockMvc
                .perform(post(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(multiValueMap))
                .andExpect(status().isForbidden());

        this.mockMvc
                .perform(delete(this.endpoint + API.USER_ACTIVITY_LOG_ENDPOINT + "/12")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

    }

}
