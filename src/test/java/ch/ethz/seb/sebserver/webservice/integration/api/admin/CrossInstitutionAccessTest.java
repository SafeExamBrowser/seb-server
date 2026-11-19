/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCreationInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;

/** Entities of one institution must not be readable or modifiable by users of another institution.
 * <p>
 * Test data: configuration node 1 and SEB client configuration 1 belong to institution 1, the users inst2Admin
 * (Institutional Administrator) and examAdmin1 (Exam Administrator) belong to institution 2. */
@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class CrossInstitutionAccessTest extends AdministrationAPIIntegrationTester {

    private static final byte[] SEB_SETTINGS =
            "plnd<?xml version=\"1.0\" encoding=\"UTF-8\"?><plist version=\"1.0\"><dict/></plist>"
                    .getBytes(StandardCharsets.UTF_8);

    @Test
    public void dependenciesOfForeignInstitutionAreNotReadable() throws Exception {
        new RestAPITestHelper()
                .withAccessToken(getAdminInstitution2Access())
                .withPath(API.INSTITUTION_ENDPOINT)
                .withPath("1")
                .withPath(API.DEPENDENCY_PATH_SEGMENT)
                .withAttribute(API.PARAM_BULK_ACTION_TYPE, BulkActionType.DEACTIVATE.name())
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .checkStatus();
    }

    @Test
    public void foreignExamConfigCannotBeOverwrittenByImport() throws Exception {
        this.mockMvc.perform(post(this.endpoint + API.CONFIGURATION_NODE_ENDPOINT + "/1"
                + API.CONFIGURATION_IMPORT_PATH_SEGMENT)
                        .header("Authorization", "Bearer " + getExamAdmin1())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(SEB_SETTINGS))
                .andExpect(status().isForbidden());
    }

    @Test
    public void foreignExamConfigCannotBeDownloaded() throws Exception {
        new RestAPITestHelper()
                .withAccessToken(getExamAdmin1())
                .withPath(API.CONFIGURATION_NODE_ENDPOINT)
                .withPath("1")
                .withPath(API.CONFIGURATION_SEB_SETTINGS_DOWNLOAD_PATH_SEGMENT)
                .withAttribute(API.PARAM_INSTITUTION_ID, "1")
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .checkStatus();
    }

    @Test
    public void configKeyOfForeignExamConfigIsNotReadable() throws Exception {
        new RestAPITestHelper()
                .withAccessToken(getExamAdmin1())
                .withPath(API.CONFIGURATION_NODE_ENDPOINT)
                .withPath("1")
                .withPath(API.CONFIGURATION_CONFIG_KEY_PATH_SEGMENT)
                .withAttribute(API.PARAM_INSTITUTION_ID, "1")
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .checkStatus();
    }

    @Test
    public void foreignExamConfigCannotBeCopied() throws Exception {
        new RestAPITestHelper()
                .withAccessToken(getExamAdmin1())
                .withMethod(HttpMethod.PUT)
                .withPath(API.CONFIGURATION_NODE_ENDPOINT)
                .withPath(API.CONFIGURATION_COPY_PATH_SEGMENT)
                .withBodyJson(new ConfigCreationInfo(
                        1L, "copy of a foreign configuration", null, false, ConfigurationType.EXAM_CONFIG))
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .checkStatus();
    }

    @Test
    public void examConfigCannotBeCopiedIntoForeignInstitution() throws Exception {
        // admin may write configuration node 1 of institution 1 but must not create a copy in institution 2
        new RestAPITestHelper()
                .withAccessToken(getSebAdminAccess())
                .withMethod(HttpMethod.PUT)
                .withPath(API.CONFIGURATION_NODE_ENDPOINT)
                .withPath(API.CONFIGURATION_COPY_PATH_SEGMENT)
                .withAttribute(API.PARAM_INSTITUTION_ID, "2")
                .withBodyJson(new ConfigCreationInfo(
                        1L, "copy into a foreign institution", null, false, ConfigurationType.EXAM_CONFIG))
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .checkStatus();
    }

    @Test
    public void foreignSebClientConfigCannotBeDownloaded() throws Exception {
        new RestAPITestHelper()
                .withAccessToken(getAdminInstitution2Access())
                .withPath(API.SEB_CLIENT_CONFIG_ENDPOINT)
                .withPath(API.SEB_CLIENT_CONFIG_DOWNLOAD_PATH_SEGMENT)
                .withPath("1")
                .withExpectedStatus(HttpStatus.FORBIDDEN)
                .checkStatus();
    }
}
