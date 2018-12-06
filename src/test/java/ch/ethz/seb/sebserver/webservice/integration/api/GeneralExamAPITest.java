/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

public class GeneralExamAPITest extends ExamAPIIntegrationTest {

    @Test
    public void getHello_givenNoToken_thenUnauthorized() throws Exception {
        this.mockMvc.perform(get(this.endpoint + "/hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void get_same_token_for_same_scope() throws Exception {
        final String accessToken1 = obtainAccessToken("test", "test", "testScope");
        final String accessToken2 = obtainAccessToken("test", "test", "testScope");

        assertEquals(accessToken1, accessToken2);
    }

    @Test
    public void get_different_tokens_for_different_scopes() throws Exception {
        final String accessToken1 = obtainAccessToken("test", "test", "testScope1");
        final String accessToken2 = obtainAccessToken("test", "test", "testScope2");

        assertNotEquals(accessToken1, accessToken2);
    }

    @Test
    public void getHello_givenToken_thenOK() throws UnsupportedEncodingException, Exception {
        final String accessToken = obtainAccessToken("test", "test", "testScope");
        final String contentAsString = this.mockMvc.perform(get(this.endpoint + "/hello")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals("Hello From Exam-Web-Service", contentAsString);
    }

}
