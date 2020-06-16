/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.RunningExamInfo;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientConnectionService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.exam.endpoint.v1}")
public class ExamAPI_V1_Controller {

    private static final Logger log = LoggerFactory.getLogger(ExamAPI_V1_Controller.class);

    private final LmsSetupDAO lmsSetupDAO;
    private final ExamSessionService examSessionService;
    private final SEBClientConnectionService sebClientConnectionService;
    private final SEBClientConfigDAO sebClientConfigDAO;
    private final JSONMapper jsonMapper;
    private final Executor executor;

    protected ExamAPI_V1_Controller(
            final LmsSetupDAO lmsSetupDAO,
            final ExamSessionService examSessionService,
            final SEBClientConnectionService sebClientConnectionService,
            final SEBClientConfigDAO sebClientConfigDAO,
            final JSONMapper jsonMapper,
            @Qualifier(AsyncServiceSpringConfig.EXAM_API_EXECUTOR_BEAN_NAME) final Executor executor) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.examSessionService = examSessionService;
        this.sebClientConnectionService = sebClientConnectionService;
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.jsonMapper = jsonMapper;
        this.executor = executor;
    }

    @RequestMapping(
            path = API.EXAM_API_HANDSHAKE_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public CompletableFuture<Collection<RunningExamInfo>> handshakeCreate(
            @RequestParam(name = API.PARAM_INSTITUTION_ID, required = false) final Long instIdRequestParam,
            @RequestParam(name = API.EXAM_API_PARAM_EXAM_ID, required = false) final Long examIdRequestParam,
            @RequestBody(required = false) final MultiValueMap<String, String> formParams,
            final Principal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        return CompletableFuture.supplyAsync(
                () -> {

                    final POSTMapper mapper = new POSTMapper(formParams, request.getQueryString());

                    final String remoteAddr = request.getRemoteAddr();
                    final Long institutionId = (instIdRequestParam != null)
                            ? instIdRequestParam
                            : mapper.getLong(API.PARAM_INSTITUTION_ID);
                    final Long examId = (examIdRequestParam != null)
                            ? examIdRequestParam
                            : mapper.getLong(API.EXAM_API_PARAM_EXAM_ID);

                    // Create and get new ClientConnection if all integrity checks passes
                    final ClientConnection clientConnection = this.sebClientConnectionService
                            .createClientConnection(principal, institutionId, remoteAddr, examId)
                            .getOrThrow();

                    response.setHeader(
                            API.EXAM_API_SEB_CONNECTION_TOKEN,
                            clientConnection.connectionToken);

                    // Crate list of running exams
                    List<RunningExamInfo> result;
                    if (examId == null) {
                        result = this.examSessionService.getRunningExamsForInstitution(institutionId)
                                .getOrThrow()
                                .stream()
                                .map(this::createRunningExamInfo)
                                .collect(Collectors.toList());
                    } else {
                        final Exam exam = this.examSessionService.getExamDAO().byPK(examId)
                                .getOrThrow();

                        result = Arrays.asList(createRunningExamInfo(exam));
                    }

                    if (result.isEmpty()) {
                        log.warn(
                                "There are no currently running exams for institution: {}. SEB connection creation denied",
                                institutionId);
                        throw new IllegalStateException("There are no currently running exams");
                    }

                    return result;
                },
                this.executor);
    }

    @RequestMapping(
            path = API.EXAM_API_HANDSHAKE_ENDPOINT,
            method = RequestMethod.PATCH,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public CompletableFuture<Void> handshakeUpdate(
            @RequestHeader(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken,
            @RequestParam(name = API.EXAM_API_PARAM_EXAM_ID, required = false) final Long examId,
            @RequestParam(name = API.EXAM_API_USER_SESSION_ID, required = false) final String userSessionId,
            final Principal principal,
            final HttpServletRequest request) {

        return CompletableFuture.runAsync(
                () -> {

                    final String remoteAddr = request.getRemoteAddr();
                    final Long institutionId = getInstitutionId(principal);

                    if (log.isDebugEnabled()) {
                        log.debug("Request received on SEB Client Connection update endpoint: "
                                + "institution: {} "
                                + "exam: {} "
                                + "userSessionId: {} "
                                + "client-address: {}",
                                institutionId,
                                examId,
                                userSessionId,
                                remoteAddr);
                    }

                    this.sebClientConnectionService.updateClientConnection(
                            connectionToken,
                            institutionId,
                            examId,
                            remoteAddr,
                            userSessionId)
                            .getOrThrow();
                },
                this.executor);
    }

    @RequestMapping(
            path = API.EXAM_API_HANDSHAKE_ENDPOINT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public CompletableFuture<Void> handshakeEstablish(
            @RequestHeader(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken,
            @RequestParam(name = API.EXAM_API_PARAM_EXAM_ID, required = false) final Long examId,
            @RequestParam(name = API.EXAM_API_USER_SESSION_ID, required = false) final String userSessionId,
            final Principal principal,
            final HttpServletRequest request) {

        return CompletableFuture.runAsync(
                () -> {

                    final String remoteAddr = request.getRemoteAddr();
                    final Long institutionId = getInstitutionId(principal);

                    if (log.isDebugEnabled()) {
                        log.debug("Request received on SEB Client Connection establish endpoint: "
                                + "institution: {} "
                                + "exam: {} "
                                + "client-address: {}",
                                institutionId,
                                examId,
                                remoteAddr);
                    }

                    this.sebClientConnectionService.establishClientConnection(
                            connectionToken,
                            institutionId,
                            examId,
                            remoteAddr,
                            userSessionId)
                            .getOrThrow();
                },
                this.executor);
    }

    @RequestMapping(
            path = API.EXAM_API_HANDSHAKE_ENDPOINT,
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public CompletableFuture<Void> handshakeDelete(
            @RequestHeader(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken,
            final Principal principal,
            final HttpServletRequest request) {

        return CompletableFuture.runAsync(
                () -> {

                    final String remoteAddr = request.getRemoteAddr();
                    final Long institutionId = getInstitutionId(principal);

                    if (log.isDebugEnabled()) {
                        log.debug("Request received on SEB Client Connection close endpoint: "
                                + "institution: {} "
                                + "client-address: {}",
                                institutionId,
                                remoteAddr);
                    }

                    this.sebClientConnectionService.closeConnection(
                            connectionToken,
                            institutionId,
                            remoteAddr)
                            .getOrThrow();
                },
                this.executor);
    }

    @RequestMapping(
            path = API.EXAM_API_CONFIGURATION_REQUEST_ENDPOINT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public CompletableFuture<Void> getConfig(
            @RequestHeader(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken,
            @RequestParam(required = false) final MultiValueMap<String, String> formParams,
            final Principal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        return CompletableFuture.runAsync(
                () -> streamExamConfig(connectionToken, formParams, principal, response),
                this.executor);
    }

    @RequestMapping(
            path = API.EXAM_API_PING_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void ping(final HttpServletRequest request, final HttpServletResponse response) {

        final String connectionToken = request.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        final String timeStampString = request.getParameter(API.EXAM_API_PING_TIMESTAMP);
        final String pingNumString = request.getParameter(API.EXAM_API_PING_NUMBER);

        final String instruction = this.sebClientConnectionService
                .notifyPing(
                        connectionToken,
                        Long.parseLong(timeStampString),
                        pingNumString != null ? Integer.parseInt(pingNumString) : -1);

        if (instruction == null) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }

        try {
            response.setStatus(HttpStatus.OK.value());
            response.getOutputStream().write(instruction.getBytes());
        } catch (final IOException e) {
            log.error("Failed to send instruction as response: {}", connectionToken, e);
        }
    }

    @RequestMapping(
            path = API.EXAM_API_EVENT_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void event(
            @RequestHeader(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken,
            @RequestBody(required = true) final ClientEvent event) {

        this.sebClientConnectionService
                .notifyClientEvent(connectionToken, event);
    }

    private Long getInstitutionId(final Principal principal) {
        final String clientId = principal.getName();
        return this.sebClientConfigDAO.byClientName(clientId)
                .getOrThrow().institutionId;
    }

    private RunningExamInfo createRunningExamInfo(final Exam exam) {
        return new RunningExamInfo(
                exam,
                this.lmsSetupDAO.byPK(exam.lmsSetupId)
                        .map(lms -> lms.lmsType)
                        .getOr(null));
    }

    private void streamExamConfig(
            final String connectionToken,
            final MultiValueMap<String, String> formParams,
            final Principal principal,
            final HttpServletResponse response) {

        try {

            // if an examId is provided with the request, update the connection first
            if (formParams != null && formParams.containsKey(API.EXAM_API_PARAM_EXAM_ID)) {
                final String examId = formParams.getFirst(API.EXAM_API_PARAM_EXAM_ID);
                final Long institutionId = getInstitutionId(principal);
                final ClientConnection connection = this.sebClientConnectionService.updateClientConnection(
                        connectionToken,
                        institutionId,
                        Long.valueOf(examId),
                        null,
                        null)
                        .getOrThrow();

                if (log.isDebugEnabled()) {
                    log.debug("Updated connection: {}", connection);
                }
            }

            final ServletOutputStream outputStream = response.getOutputStream();

            try {

                final ClientConnectionData connection = this.examSessionService
                        .getConnectionData(connectionToken)
                        .getOrThrow();

                // exam integrity check
                if (connection.clientConnection.examId == null ||
                        !this.examSessionService.isExamRunning(connection.clientConnection.examId)) {

                    log.error("Missing exam identifier or requested exam is not running for connection: {}",
                            connection);
                    throw new IllegalStateException("Missing exam identifier or requested exam is not running");
                }
            } catch (final Exception e) {

                log.error("Unexpected error: ", e);

                final APIMessage errorMessage = APIMessage.ErrorMessage.GENERIC.of(e.getMessage());
                outputStream.write(Utils.toByteArray(this.jsonMapper.writeValueAsString(errorMessage)));
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                outputStream.flush();
                outputStream.close();
                return;
            }

            try {

                this.examSessionService
                        .streamDefaultExamConfig(
                                connectionToken,
                                outputStream);

                response.setStatus(HttpStatus.OK.value());

            } catch (final Exception e) {
                final APIMessage errorMessage = APIMessage.ErrorMessage.GENERIC.of(e.getMessage());
                outputStream.write(Utils.toByteArray(this.jsonMapper.writeValueAsString(errorMessage)));
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            } finally {
                outputStream.flush();
                outputStream.close();
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to stream SEB Exam Configuration to client with connection: {}",
                    connectionToken,
                    e);

            final APIMessage errorMessage = APIMessage.ErrorMessage.GENERIC.of(e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getOutputStream().write(Utils.toByteArray(this.jsonMapper.writeValueAsString(errorMessage)));
            } catch (final Exception e1) {
                log.error("Failed to write error to response: ", e1);
            }
        }
    }

}
