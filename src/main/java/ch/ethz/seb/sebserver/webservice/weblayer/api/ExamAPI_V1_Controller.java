/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import org.apache.commons.lang3.StringUtils;
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

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.RunningExamInfo;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientConnectionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientSessionService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.exam.endpoint.v1}")
public class ExamAPI_V1_Controller {

    private static final Logger log = LoggerFactory.getLogger(ExamAPI_V1_Controller.class);

    private final LmsSetupDAO lmsSetupDAO;
    private final ExamSessionService examSessionService;
    private final SEBClientConnectionService sebClientConnectionService;
    private final SEBClientSessionService sebClientSessionService;
    private final SEBClientConfigDAO sebClientConfigDAO;
    private final Executor executor;

    protected ExamAPI_V1_Controller(
            final LmsSetupDAO lmsSetupDAO,
            final ExamSessionService examSessionService,
            final SEBClientConnectionService sebClientConnectionService,
            final SEBClientSessionService sebClientSessionService,
            final SEBClientConfigDAO sebClientConfigDAO,
            @Qualifier(AsyncServiceSpringConfig.EXAM_API_EXECUTOR_BEAN_NAME) final Executor executor) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.examSessionService = examSessionService;
        this.sebClientConnectionService = sebClientConnectionService;
        this.sebClientSessionService = sebClientSessionService;
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.executor = executor;
    }

    @RequestMapping(
            path = API.EXAM_API_HANDSHAKE_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<Collection<RunningExamInfo>> handshakeCreate(
            @RequestParam(name = API.PARAM_INSTITUTION_ID, required = false) final Long instIdRequestParam,
            @RequestParam(name = API.EXAM_API_PARAM_EXAM_ID, required = false) final Long examIdRequestParam,
            @RequestParam(name = API.EXAM_API_PARAM_CLIENT_ID, required = false) final String clientIdRequestParam,
            @RequestBody(required = false) final MultiValueMap<String, String> formParams,
            final Principal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        return CompletableFuture.supplyAsync(
                () -> {

                    final POSTMapper mapper = new POSTMapper(formParams, request.getQueryString());

                    final String remoteAddr = this.getClientAddress(request);
                    final Long institutionId = (instIdRequestParam != null)
                            ? instIdRequestParam
                            : mapper.getLong(API.PARAM_INSTITUTION_ID);
                    final Long examId = (examIdRequestParam != null)
                            ? examIdRequestParam
                            : mapper.getLong(API.EXAM_API_PARAM_EXAM_ID);
                    final String clientId = (clientIdRequestParam != null)
                            ? clientIdRequestParam
                            : mapper.getString(API.EXAM_API_PARAM_CLIENT_ID);

                    // Create and get new ClientConnection if all integrity checks passes
                    final ClientConnection clientConnection = this.sebClientConnectionService
                            .createClientConnection(
                                    principal,
                                    institutionId,
                                    remoteAddr,
                                    mapper.getString(API.EXAM_API_PARAM_SEB_VERSION),
                                    mapper.getString(API.EXAM_API_PARAM_SEB_OS_NAME),
                                    mapper.getString(API.EXAM_API_PARAM_SEB_MACHINE_NAME),
                                    examId,
                                    clientId)
                            .getOrThrow();

                    response.setHeader(
                            API.EXAM_API_SEB_CONNECTION_TOKEN,
                            clientConnection.connectionToken);

                    // Crate list of running exams
                    final List<RunningExamInfo> result;
                    if (examId == null) {

                        result = this.examSessionService.getRunningExams(
                                    institutionId,
                                    getExamSelectionPredicate(principal.getName()))
                                .getOrThrow()
                                .stream()
                                .map(this::createRunningExamInfo)
                                .filter(this::checkConsistency)
                                .collect(Collectors.toList());
                    } else {

                        final Exam exam = this.examSessionService
                                .getExamDAO()
                                .byPK(examId)
                                .getOrThrow();

                        result = Arrays.asList(createRunningExamInfo(exam));
                        processASKSalt(response, clientConnection);
                        processAlternativeBEK(response, clientConnection.examId);
                    }

                    if (result.isEmpty()) {
                        log.warn(
                                "There are no currently running exams for institution: {}. SEB connection creation denied",
                                institutionId);
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
            @RequestParam(name = API.EXAM_API_PARAM_SEB_VERSION, required = false) final String sebVersion,
            @RequestParam(name = API.EXAM_API_PARAM_SEB_OS_NAME, required = false) final String sebOSName,
            @RequestParam(name = API.EXAM_API_PARAM_SEB_MACHINE_NAME, required = false) final String sebMachineName,
            @RequestParam(
                    name = API.EXAM_API_PARAM_SIGNATURE_KEY,
                    required = false) final String browserSignatureKey,
            @RequestParam(name = API.EXAM_API_PARAM_CLIENT_ID, required = false) final String clientId,
            final Principal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        return CompletableFuture.runAsync(
                () -> {

                    final String remoteAddr = this.getClientAddress(request);
                    final Long institutionId = getInstitutionId(principal);

                    final ClientConnection clientConnection = this.sebClientConnectionService
                            .updateClientConnection(
                                    connectionToken,
                                    institutionId,
                                    examId,
                                    remoteAddr,
                                    sebVersion,
                                    sebOSName,
                                    sebMachineName,
                                    userSessionId,
                                    clientId,
                                    browserSignatureKey)
                            .getOrThrow();

                    if (clientConnection.examId != null) {
                        processASKSalt(response, clientConnection);
                        processAlternativeBEK(response, clientConnection.examId);
                    }
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
            @RequestParam(name = API.EXAM_API_PARAM_SEB_VERSION, required = false) final String sebVersion,
            @RequestParam(name = API.EXAM_API_PARAM_SEB_OS_NAME, required = false) final String sebOSName,
            @RequestParam(name = API.EXAM_API_PARAM_SEB_MACHINE_NAME, required = false) final String setMachineName,
            @RequestParam(
                    name = API.EXAM_API_PARAM_SIGNATURE_KEY,
                    required = false) final String browserSignatureKey,
            @RequestParam(name = API.EXAM_API_PARAM_CLIENT_ID, required = false) final String clientId,
            final Principal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        return CompletableFuture.runAsync(
                () -> {

                    final String remoteAddr = this.getClientAddress(request);
                    final Long institutionId = getInstitutionId(principal);

                    final ClientConnection clientConnection = this.sebClientConnectionService
                            .establishClientConnection(
                                    connectionToken,
                                    institutionId,
                                    examId,
                                    remoteAddr,
                                    sebVersion,
                                    sebOSName,
                                    setMachineName,
                                    userSessionId,
                                    clientId,
                                    browserSignatureKey)
                            .getOrThrow();

                    if (clientConnection.examId != null) {
                        processAlternativeBEK(response, clientConnection.examId);
                    }
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

                    final String remoteAddr = this.getClientAddress(request);
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

        Long examId;
        try {
            examId = Long.parseLong(Objects.requireNonNull(formParams.getFirst(API.EXAM_API_PARAM_EXAM_ID)));
        } catch (final Exception e) {
            examId = null;
        }
        final Long _examId = examId;
        final String remoteAddr = this.getClientAddress(request);

        return CompletableFuture.runAsync(
                () -> this.sebClientConnectionService.streamExamConfig(
                        getInstitutionId(principal),
                        _examId,
                        connectionToken,
                        remoteAddr,
                        response),
                this.executor);
    }

    @RequestMapping(
            path = API.EXAM_API_PING_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void ping(final HttpServletRequest request, final HttpServletResponse response) {

        final String connectionToken = request.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        final String instructionConfirm = request.getParameter(API.EXAM_API_PING_INSTRUCTION_CONFIRM);

        final String instruction = this.sebClientSessionService
                .notifyPing(connectionToken, 0, instructionConfirm);

        if (instruction == null) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
        } else {
            try {
                response.setStatus(HttpStatus.OK.value());
                response.getOutputStream().write(instruction.getBytes(StandardCharsets.UTF_8));
            } catch (final IOException e) {
                log.error("Failed to send instruction as response: {}", connectionToken, e);
            }
        }
    }

    @RequestMapping(
            path = API.EXAM_API_EVENT_ENDPOINT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void event(
            @RequestHeader(name = API.EXAM_API_SEB_CONNECTION_TOKEN, required = true) final String connectionToken,
            @RequestBody(required = true) final String jsonBody) {

        this.sebClientSessionService
                .notifyClientEvent(connectionToken, jsonBody);
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



    private String getClientAddress(final HttpServletRequest request) {
        try {
            final String ipAddress = request.getHeader("X-FORWARDED-FOR");

            if (ipAddress == null) {
                return request.getRemoteAddr();
            }

            if (ipAddress.contains(",")) {
                return StringUtils.split(ipAddress, Constants.COMMA)[0];
            }

            return ipAddress;
        } catch (final Exception e) {
            log.warn("Failed to verify client IP address: {}", e.getMessage());
            return request.getHeader("X-FORWARDED-FOR");
        }
    }

    private void processASKSalt(final HttpServletResponse response, final ClientConnection clientConnection) {
        this.examSessionService
                .getAppSignatureKeySalt(clientConnection.examId)
                .onSuccess(salt -> response.setHeader(API.EXAM_API_EXAM_SIGNATURE_SALT_HEADER, salt))
                .onError(error -> log.error(
                        "Failed to get security key salt for connection: {}",
                        clientConnection,
                        error));
    }

    private void processAlternativeBEK(final HttpServletResponse response, final Long examId) {
        if (examId == null) {
            return;
        }
        this.examSessionService.getRunningExam(examId)
                .map(exam -> exam.getAdditionalAttribute(SEBRestrictionService.ADDITIONAL_ATTR_ALTERNATIVE_SEB_BEK))
                .onSuccess(bek -> response.setHeader(API.EXAM_API_EXAM_ALT_BEK, bek));
    }

    private Predicate<Long> getExamSelectionPredicate(final String clientName) {
        return this.sebClientConfigDAO
                .byClientName(clientName)
                .map(this::getExamSelectionPredicate)
                .onError(error -> log.warn("Failed to get SEB connection configuration by name: {}", clientName))
                .getOr(Utils.truePredicate());
    }

    private Predicate<Long> getExamSelectionPredicate(final SEBClientConfig config) {
        if (config == null || config.selectedExams.isEmpty()) {
            return Utils.truePredicate();
        }
        return config.getSelectedExams()::contains;
    }

    private boolean checkConsistency(final RunningExamInfo info) {
        if (StringUtils.isNotBlank(info.name) &&
                StringUtils.isNotBlank(info.url) &&
                StringUtils.isNotBlank(info.examId)) {

            return true;
        }

        log.warn("Invalid running exam detected. Filter out exam : {}", info);
        return false;
    }

}
