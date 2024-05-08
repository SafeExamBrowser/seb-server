/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.lms.api.endpoint}")
public class LmsIntegrationController {

    private static final Logger log = LoggerFactory.getLogger(LmsIntegrationController.class);

    private final FullLmsIntegrationService fullLmsIntegrationService;

    public LmsIntegrationController(final FullLmsIntegrationService fullLmsIntegrationService) {
        this.fullLmsIntegrationService = fullLmsIntegrationService;
    }

    @RequestMapping(
            path = API.LMS_FULL_INTEGRATION_EXAM_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void createExam(
            @RequestParam(name = API.LMS_FULL_INTEGRATION_LMS_UUID, required = true) final String lmsUUId,
            @RequestParam(name = API.LMS_FULL_INTEGRATION_COURSE_ID, required = true) final String courseId,
            @RequestParam(name = API.LMS_FULL_INTEGRATION_QUIZ_ID, required = true) final String quizId,
            @RequestParam(name = API.LMS_FULL_INTEGRATION_EXAM_TEMPLATE_ID, required = true) final String templateId,
            @RequestParam(name = API.LMS_FULL_INTEGRATION_QUIT_PASSWORD, required = false) final String quitPassword,
            @RequestParam(name = API.LMS_FULL_INTEGRATION_QUIT_LINK, required = false) final String quitLink,
            @RequestParam(name = API.LMS_FULL_INTEGRATION_TIME_ZONE, required = false) final String timezone,
            final HttpServletResponse response) {

        final Exam exam = fullLmsIntegrationService.importExam(
                        lmsUUId,
                        courseId,
                        quizId,
                        templateId,
                        quitPassword,
                        quitLink,
                        timezone)
                .onError(e -> log.error(
                        "Failed to create/import exam: lmsId:{}, courseId: {}, quizId: {}, templateId: {}",
                        lmsUUId, courseId, quizId, templateId, e))
                .getOrThrow();

        log.info("Auto import of exam successful: {}", exam);
    }

    @RequestMapping(
            path = API.LMS_FULL_INTEGRATION_EXAM_ENDPOINT,
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void deleteExam(
            @RequestParam(name = API.LMS_FULL_INTEGRATION_LMS_UUID, required = true) final String lmsUUId,
            @RequestParam(name = API.LMS_FULL_INTEGRATION_COURSE_ID, required = true) final String courseId,
            @RequestParam(name = API.LMS_FULL_INTEGRATION_QUIZ_ID, required = true) final String quizId,
            final HttpServletResponse response) {

        final EntityKey examID = fullLmsIntegrationService.deleteExam(lmsUUId, courseId, quizId)
                .onError(e -> log.error(
                        "Failed to delete exam: lmsId:{}, courseId: {}, quizId: {}",
                        lmsUUId, courseId, quizId, e))
                .getOrThrow();

        log.info("Auto delete of exam successful: {}", examID);
    }

    @RequestMapping(
            path = API.LMS_FULL_INTEGRATION_CONNECTION_CONFIG_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getConnectionConfiguration(
            @RequestParam(name = API.LMS_FULL_INTEGRATION_LMS_UUID, required = true) final String lmsUUId,
            @RequestParam(name = API.LMS_FULL_INTEGRATION_COURSE_ID, required = true) final String courseId,
            @RequestParam(name = API.LMS_FULL_INTEGRATION_QUIZ_ID, required = true) final String quizId,
            final HttpServletResponse response) throws IOException {

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
                response.setStatus(HttpStatus.BAD_REQUEST.value());
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
}
