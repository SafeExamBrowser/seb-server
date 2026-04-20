/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AdHocAccountData;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.NoResourceFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "LMS Integration", description = "LMS integration API for full LMS course-exam lifecycle management")
@RestController
@RequestMapping("${sebserver.webservice.lms.api.endpoint}")
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_LMS_API)
public class LmsIntegrationController {

    private static final Logger log = LoggerFactory.getLogger(LmsIntegrationController.class);

    private final FullLmsIntegrationService fullLmsIntegrationService;
    private final WebserviceInfo webserviceInfo;

    public LmsIntegrationController(
            final FullLmsIntegrationService fullLmsIntegrationService,
            final WebserviceInfo webserviceInfo) {

        this.fullLmsIntegrationService = fullLmsIntegrationService;
        this.webserviceInfo = webserviceInfo;
    }

    @Operation(operationId = "createExamFromLms", summary = "Create and import an exam from an LMS course/quiz")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "400", description = "Bad request / validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path = API.LMS_FULL_INTEGRATION_EXAM_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void createExam(
            @Parameter(description = "The LMS setup UUID") @RequestParam(name = API.LMS_FULL_INTEGRATION_LMS_UUID) final String lmsUUId,
            @Parameter(description = "The LMS course identifier") @RequestParam(name = API.LMS_FULL_INTEGRATION_COURSE_ID) final String courseId,
            @Parameter(description = "The LMS quiz identifier") @RequestParam(name = API.LMS_FULL_INTEGRATION_QUIZ_ID) final String quizId,
            @Parameter(description = "The exam template identifier") @RequestParam(name = API.LMS_FULL_INTEGRATION_EXAM_TEMPLATE_ID) final String templateId,
            @Parameter(description = "Additional exam data") @RequestParam(name = API.LMS_FULL_INTEGRATION_EXAM_DATA, required = false) final String examData,
            @Parameter(description = "The SEB quit password") @RequestParam(name = API.LMS_FULL_INTEGRATION_QUIT_PASSWORD, required = false) final String quitPassword,
            @Parameter(description = "Whether to include a quit link") @RequestParam(name = API.LMS_FULL_INTEGRATION_QUIT_LINK, required = false) final Integer quitLink,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        log.info("Importing exam from LMS call. lmsUUId: {} courseId: {} quizId: {} templateId: {} quitPassword: {} quitLink: {}",
                lmsUUId,
                courseId,
                quizId,
                templateId,
                StringUtils.isNotBlank(quitPassword) ? "yes" : "no",
                quitLink);

        if (log.isDebugEnabled()) {
            log.debug("Importing exam from LMS call. All param: {}", request.getParameterNames());
        }

        fullLmsIntegrationService.importExam(
                        lmsUUId,
                        courseId,
                        quizId,
                        templateId,
                        quitPassword,
                        quitLink != null && BooleanUtils.toBoolean(quitLink),
                        examData)
                .onErrorHandle(e -> {
                    log.error(
                            "Failed to create/import exam: lmsId:{}, courseId: {}, quizId: {}, templateId: {} error: {}",
                            lmsUUId, courseId, quizId, templateId, e.getMessage());

                    log.info("Rollback Exam creation...");

                    fullLmsIntegrationService.deleteExam(lmsUUId, courseId, quizId)
                            .onError(error -> {
                                if (error instanceof NoResourceFoundException) {
                                    log.info("No Exam found for rollback.");
                                } else {
                                    log.error("Failed to rollback auto Exam import: ", error);
                                }
                            });
                    
                    return new LMSAutoImportException(
                            "Failed to import Exam due to error: " + e.getMessage() + ". " +
                            "All partial imported Exam components has been deleted on SEB Server (Rollback)",
                            e);
                })
                .onSuccess(exam -> log.info("Auto import of exam successful: {}", exam))
                .getOrThrow();
    }

    @Operation(operationId = "deleteExam", summary = "Delete an exam that was previously imported from an LMS")
    @ApiResponse(responseCode = "200", description = "Deleted", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not found")
    @RequestMapping(
            path = API.LMS_FULL_INTEGRATION_EXAM_ENDPOINT,
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void deleteExam(
            @Parameter(description = "The LMS setup UUID") @RequestParam(name = API.LMS_FULL_INTEGRATION_LMS_UUID) final String lmsUUId,
            @Parameter(description = "The LMS course identifier") @RequestParam(name = API.LMS_FULL_INTEGRATION_COURSE_ID) final String courseId,
            @Parameter(description = "The LMS quiz identifier") @RequestParam(name = API.LMS_FULL_INTEGRATION_QUIZ_ID) final String quizId,
            final HttpServletResponse response) {

        fullLmsIntegrationService.deleteExam(lmsUUId, courseId, quizId)
                .onError(e -> log.error(
                        "Failed to delete exam: lmsId:{}, courseId: {}, quizId: {}, error: {}",
                        lmsUUId, courseId, quizId, e.getMessage()))
                .onSuccess(examID -> log.info("Auto delete of exam successful: {}", examID));

    }

    @Operation(operationId = "getConnectionConfiguration", summary = "Get the SEB connection configuration for an LMS exam as a binary stream")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
    @ApiResponse(responseCode = "400", description = "Bad request / validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path = API.LMS_FULL_INTEGRATION_CONNECTION_CONFIG_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getConnectionConfiguration(
            @Parameter(description = "The LMS setup UUID") @RequestParam(name = API.LMS_FULL_INTEGRATION_LMS_UUID) final String lmsUUId,
            @Parameter(description = "The LMS course identifier") @RequestParam(name = API.LMS_FULL_INTEGRATION_COURSE_ID) final String courseId,
            @Parameter(description = "The LMS quiz identifier") @RequestParam(name = API.LMS_FULL_INTEGRATION_QUIZ_ID) final String quizId,
            final HttpServletResponse response) throws IOException {

        log.info(
                "LMS called get SEB Connection Configuration for: lmsUUId: {} courseId: {} quizId: {}",
                lmsUUId,
                courseId,
                quizId);

        final ServletOutputStream outputStream = response.getOutputStream();
        final PipedOutputStream pout;
        final PipedInputStream pin;
        try {
            pout = new PipedOutputStream();
            pin = new PipedInputStream(pout);

            fullLmsIntegrationService
                    .streamConnectionConfiguration(lmsUUId, courseId, quizId, pout)
                    .getOrThrow();

            IOUtils.copyLarge(pin, outputStream);

            response.setStatus(HttpStatus.OK.value());
            outputStream.flush();

        } catch (final APIMessage.APIMessageException me) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED.value());
            throw me;
        } catch (final Exception e) {
            log.error(
                    "Failed to stream connection configuration for exam: lmsId:{}, courseId: {}, quizId: {}",
                    lmsUUId, courseId, quizId, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        } finally {
            outputStream.flush();
            outputStream.close();
        }
    }

    @Operation(operationId = "getOneTimeLoginToken", summary = "Get a one-time login token for a specific LMS user and exam")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "400", description = "Bad request / validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path = API.LMS_FULL_INTEGRATION_LOGIN_TOKEN_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public FullLmsIntegrationService.TokenLoginResponse getOneTimeLoginToken(
            @Parameter(description = "The LMS setup UUID") @RequestParam(name = API.LMS_FULL_INTEGRATION_LMS_UUID) final String lmsUUId,
            @Parameter(description = "The LMS course identifier") @RequestParam(name = API.LMS_FULL_INTEGRATION_COURSE_ID) final String courseId,
            @Parameter(description = "The LMS quiz identifier") @RequestParam(name = API.LMS_FULL_INTEGRATION_QUIZ_ID) final String quizId,
            @Parameter(description = "The LMS user identifier") @RequestParam(name = API.LMS_FULL_INTEGRATION_USER_ID) final String userId,
            @Parameter(description = "The LMS user name") @RequestParam(name = API.LMS_FULL_INTEGRATION_USER_NAME, required = false) final String username,
            @Parameter(description = "The LMS user email address") @RequestParam(name = API.LMS_FULL_INTEGRATION_USER_EMAIL, required = false) final String userMail,
            @Parameter(description = "The LMS user first name") @RequestParam(name = API.LMS_FULL_INTEGRATION_USER_FIRST_NAME, required = false) final String firstName,
            @Parameter(description = "The LMS user last name") @RequestParam(name = API.LMS_FULL_INTEGRATION_USER_LAST_NAME, required = false) final String lastName,
            @Parameter(description = "The LMS user time zone") @RequestParam(name = API.LMS_FULL_INTEGRATION_TIME_ZONE, required = false) final String timezone,
            final HttpServletResponse response) {

        final AdHocAccountData adHocAccountData = new AdHocAccountData(
                userId,
                username,
                userMail,
                firstName,
                lastName,
                timezone
        );

        final String token = this.fullLmsIntegrationService
                .getOneTimeLoginToken(lmsUUId, courseId, quizId, adHocAccountData)
                .onError(error -> log.warn("Failed to create ad-hoc account with one time login token, error: {}", error.getMessage()))
                .getOr(null);
        
        if (token == null) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return null;
        }

        return new FullLmsIntegrationService.TokenLoginResponse(
                lmsUUId,
                webserviceInfo.getGUIAutologinURL() + API.LMS_FULL_INTEGRATION_LOGIN_TOKEN_ENDPOINT + "?jwt=" + token);
    }
}
