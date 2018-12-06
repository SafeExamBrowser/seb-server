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

import java.io.UnsupportedEncodingException;

import org.junit.Test;

public class GeneralAdminAPITest extends AdministrationAPIIntegrationTest {

    @Test
    public void getHello_givenNoToken_thenRedirect() throws Exception {
        this.mockMvc.perform(get(this.endpoint + "/hello"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void getHello_givenToken_thenOK() throws UnsupportedEncodingException, Exception {
        final String accessToken = obtainAccessToken("admin", "admin");
        final String contentAsString = this.mockMvc.perform(get(this.endpoint + "/hello")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals("Hello From Admin-Web-Service", contentAsString);
    }

    @Test
    public void accessGrantedForActiveUser() throws Exception {
        final String obtainAccessToken = obtainAccessToken("user1", "test");
        assertNotNull(obtainAccessToken);
    }

    @Test
    public void accessDeniedForInactiveUser() throws Exception {
        try {
            obtainAccessToken("deactivatedUser", "test");
            fail("AssertionError expected here");
        } catch (final AssertionError e) {
            assertEquals("Status expected:<200> but was:<400>", e.getMessage());
        }
    }

}
