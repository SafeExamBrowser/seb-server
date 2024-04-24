/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService.IntegrationData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleResponseException;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class MoodlePluginFullIntegration implements FullLmsIntegrationAPI {

    private static final Logger log = LoggerFactory.getLogger(MoodlePluginFullIntegration.class);

    private static final String FUNCTION_NAME_SEBSERVER_CONNECTION = "sebserver_connection";
    private static final String FUNCTION_NAME_SEBSERVER_CONNECTION_DELETE = "sebserver_connection_delete";
    private final JSONMapper jsonMapper;
    private final MoodleRestTemplateFactory restTemplateFactory;

    public MoodlePluginFullIntegration(
            final JSONMapper jsonMapper,
            final MoodleRestTemplateFactory restTemplateFactory) {

        this.jsonMapper = jsonMapper;
        this.restTemplateFactory = restTemplateFactory;
    }

    @Override
    public LmsSetupTestResult testFullIntegrationAPI() {
        final LmsSetupTestResult attributesCheck = this.restTemplateFactory.test();
        if (!attributesCheck.isOk()) {
            return attributesCheck;
        }

        final Result<MoodleAPIRestTemplate> restTemplateRequest = getRestTemplate();
        if (restTemplateRequest.hasError()) {
            final String message = "Failed to gain access token from Moodle Rest API:\n tried token endpoints: " +
                    this.restTemplateFactory.getKnownTokenAccessPaths();
            log.error(message + " cause: {}", restTemplateRequest.getError().getMessage());
            return LmsSetupTestResult.ofTokenRequestError(LmsSetup.LmsType.MOODLE_PLUGIN, message);
        }

        try {
            final MoodleAPIRestTemplate restTemplate = restTemplateRequest.get();
            restTemplate.testAPIConnection(
                    FUNCTION_NAME_SEBSERVER_CONNECTION,
                    FUNCTION_NAME_SEBSERVER_CONNECTION_DELETE);

        } catch (final RuntimeException e) {
            return LmsSetupTestResult.ofQuizAccessAPIError(LmsSetup.LmsType.MOODLE_PLUGIN, e.getMessage());
        }

        return LmsSetupTestResult.ofOkay(LmsSetup.LmsType.MOODLE_PLUGIN);
    }

    @Override
    public Result<IntegrationData> applyConnectionDetails(final IntegrationData data) {
        return Result.tryCatch(() -> {
            // validation
            if (StringUtils.isBlank( data.id)) {
                throw new APIMessage.FieldValidationException("lmsFullIntegration:id", "id is mandatory");
            }
            if (StringUtils.isBlank( data.url)) {
                throw new APIMessage.FieldValidationException("lmsFullIntegration:url", "url is mandatory");
            }
            if (StringUtils.isBlank( data.access_token)) {
                throw new APIMessage.FieldValidationException("lmsFullIntegration:access_token", "access_token is mandatory");
            }
//            if (data.exam_templates.isEmpty()) {
//                throw new APIMessage.FieldValidationException("lmsFullIntegration:exam_templates", "exam_templates is mandatory");
//            }

            // apply
            final LmsSetup lmsSetup = this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup();
            final String jsonPayload = jsonMapper.writeValueAsString(data);
            final MoodleAPIRestTemplate rest = getRestTemplate().getOrThrow();
            final String response = rest.postToMoodleAPIFunction(FUNCTION_NAME_SEBSERVER_CONNECTION, jsonPayload);

            if (response != null && response.startsWith("{\"exception\":")) {
                // Seems there was an error response from Moodle side.
                // How do we know now what is active on Moodle side?
                // Shall we mark the connection as invalid here?

                log.warn(
                        "Failed to apply SEB Server connection details to Moodle for full integration. Moodle error {}, lmsSetup: {} data: {}",
                        response,
                        lmsSetup,
                        data
                );

                throw new MoodleResponseException("Failed to apply SEB Server connection: " + lmsSetup, response);
            }

            if (log.isDebugEnabled()) {
                log.debug("Successfully applied SEB Server connection for Moodle. Connection data: {} LMS Setup: {}", data, lmsSetup);
            }

            return data;
        });
    }

    @Override
    public Result<String> deleteConnectionDetails() {
        return Result.tryCatch(() -> {
            // get connection identifier
            final LmsSetup lmsSetup = this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup();
            final String connectionId = lmsSetup.getConnectionId();
            if (StringUtils.isBlank(connectionId)) {
                throw new RuntimeException("LMS Setup still has no SEB Server connection identifier: " + lmsSetup);
            }

            final MoodleAPIRestTemplate rest = getRestTemplate().getOrThrow();
            final MultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();
            queryAttributes.set("id", connectionId);
            final String response = rest.callMoodleAPIFunction(
                    FUNCTION_NAME_SEBSERVER_CONNECTION_DELETE,
                    null,
                    queryAttributes);

            if (response.startsWith("{\"exception\":")) {
                throw new MoodleResponseException("Failed to delete SEB Server connection: " + lmsSetup, response);
            }

            log.info("Successfully deleted SEB Server connection for Moodle. LMS Setup: {}", lmsSetup);
            return response;
        });
    }

    private Result<MoodleAPIRestTemplate> getRestTemplate() {

        final Result<MoodleAPIRestTemplate> result = this.restTemplateFactory.getRestTemplate();
        if (!result.hasError()) {
            return result;
        }

        return this.restTemplateFactory.createRestTemplate(MooldePluginLmsAPITemplateFactory.SEB_SERVER_SERVICE_NAME);
    }
}
