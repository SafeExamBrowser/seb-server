/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.CreateUserRequest;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.UserPageResponse;

@Lazy
@Service
@WebServiceProfile
public class ZoomProctoringService implements ExamProctoringService {

    private static final Logger log = LoggerFactory.getLogger(ZoomProctoringService.class);

    private static final String TOKEN_ENCODE_ALG = "HmacSHA256";

    private static final String ZOOM_ACCESS_TOKEN_HEADER =
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String ZOOM_ACCESS_TOKEN_PAYLOAD =
            "{\"iss\":\"%s\",\"exp\":%s}";

    private static final Map<String, String> SEB_API_NAME_INSTRUCTION_NAME_MAPPING = Utils.immutableMapOf(Arrays.asList(
            new Tuple<>(
                    API.EXAM_PROCTORING_ATTR_RECEIVE_AUDIO,
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_RECEIVE_AUDIO),
            new Tuple<>(
                    API.EXAM_PROCTORING_ATTR_RECEIVE_VIDEO,
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_RECEIVE_VIDEO),
            new Tuple<>(
                    API.EXAM_PROCTORING_ATTR_ALLOW_CHAT,
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_ALLOW_CHAT))
            .stream().collect(Collectors.toMap(Tuple::get_1, Tuple::get_2)));

    private static final Map<String, String> SEB_INSTRUCTION_DEFAULTS = Utils.immutableMapOf(Arrays.asList(
            new Tuple<>(
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_RECEIVE_AUDIO,
                    Constants.FALSE_STRING),
            new Tuple<>(
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_RECEIVE_VIDEO,
                    Constants.FALSE_STRING),
            new Tuple<>(
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_ALLOW_CHAT,
                    Constants.FALSE_STRING))
            .stream().collect(Collectors.toMap(Tuple::get_1, Tuple::get_2)));

    private final ExamSessionService examSessionService;
    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    private final Cryptor cryptor;
    private final AsyncService asyncService;
    private final JSONMapper jsonMapper;
    private final ZoomRestTemplate zoomRestTemplate;

    public ZoomProctoringService(
            final ExamSessionService examSessionService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final Cryptor cryptor,
            final AsyncService asyncService,
            final JSONMapper jsonMapper) {

        this.examSessionService = examSessionService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        this.cryptor = cryptor;
        this.asyncService = asyncService;
        this.jsonMapper = jsonMapper;
        this.zoomRestTemplate = new ZoomRestTemplate();
    }

    @Override
    public ProctoringServerType getType() {
        return ProctoringServerType.ZOOM;
    }

    @Override
    public Result<Boolean> testExamProctoring(final ProctoringServiceSettings proctoringSettings) {
        return Result.tryCatch(() -> {
            if (proctoringSettings.serverURL != null && proctoringSettings.serverURL.contains("?")) {
                throw new FieldValidationException(
                        "serverURL",
                        "proctoringSettings:serverURL:invalidURL");
            }

            try {

                final ResponseEntity<String> result = this.zoomRestTemplate
                        .testServiceConnection(proctoringSettings);

                if (result.getStatusCode() != HttpStatus.OK) {
                    throw new APIMessageException(
                            APIMessage.ErrorMessage.BINDING_ERROR,
                            String.valueOf(result.getStatusCode()));
                } else {
                    final UserPageResponse response = this.jsonMapper.readValue(
                            result.getBody(),
                            UserPageResponse.class);

                    System.out.println(response);

                    final ResponseEntity<String> createUser = this.zoomRestTemplate
                            .createUser(proctoringSettings);

                    System.out.println(response);
                }
            } catch (final Exception e) {
                log.error("Failed to access Zoom service at: {}", proctoringSettings.serverURL, e);
                throw new APIMessageException(APIMessage.ErrorMessage.BINDING_ERROR, e.getMessage());
            }

            return true;
        });
    }

    @Override
    public Result<ProctoringRoomConnection> getClientBreakOutRoomConnection(
            final ProctoringServiceSettings proctoringSettings,
            final String connectionToken,
            final String roomName,
            final String subject) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<ProctoringRoomConnection> getClientCollectingRoomConnection(
            final ProctoringServiceSettings proctoringSettings,
            final String connectionToken,
            final String roomName,
            final String subject) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> createJoinInstructionAttributes(final ProctoringRoomConnection proctoringConnection) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<ProctoringRoomConnection> getProctorRoomConnection(
            final ProctoringServiceSettings proctoringSettings,
            final String roomName,
            final String subject) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Void> disposeServiceRoomsForExam(final Exam exam) {
        // TODO get all rooms of the exam
        // close the rooms on Zoom service
        return null;
    }

    @Override
    public Result<NewRoom> newCollectingRoom(final Long roomNumber) {
        // TODO create new room on zoom side and use the id as room name
        return null;
    }

    @Override
    public Result<NewRoom> newBreakOutRoom(final String subject) {
        // TODO create new room on zoom side and use the id as room name
        return null;
    }

    @Override
    public Result<Void> disposeBreakOutRoom(final String roomName) {
        // TODO close the room with specified roomName on zoom side
        return null;
    }

    @Override
    public Map<String, String> getDefaultInstructionAttributes() {
        return SEB_INSTRUCTION_DEFAULTS;
    }

    @Override
    public Map<String, String> getInstructionAttributes(final Map<String, String> attributes) {
        return attributes.entrySet().stream()
                .map(entry -> new Tuple<>(
                        SEB_API_NAME_INSTRUCTION_NAME_MAPPING.getOrDefault(entry.getKey(), entry.getKey()),
                        entry.getValue()))
                .collect(Collectors.toMap(Tuple::get_1, Tuple::get_2));
    }

    protected String createPayload(
            final String clientKey,
            final Long expTime) {

        return String.format(
                ZOOM_ACCESS_TOKEN_PAYLOAD.replaceAll(" ", "").replaceAll("\n", ""),
                clientKey,
                expTime);
    }

//    private long forExam(final ProctoringServiceSettings examProctoring) {
//        if (examProctoring.examId == null) {
//            throw new IllegalStateException("Missing exam identifier from ExamProctoring data");
//        }
//
//        long expTime = System.currentTimeMillis() + Constants.DAY_IN_MILLIS;
//        if (this.examSessionService.isExamRunning(examProctoring.examId)) {
//            final Exam exam = this.examSessionService.getRunningExam(examProctoring.examId)
//                    .getOrThrow();
//            if (exam.endTime != null) {
//                expTime = exam.endTime.getMillis();
//            }
//        }
//        return expTime;
//    }

    private class ZoomRestTemplate {

        private static final String API_TEST_ENDPOINT =
                "v2/users?status=active&page_size=30&page_number=1&data_type=Json";

        private final RestTemplate restTemplate;
        private final CircuitBreaker<ResponseEntity<String>> circuitBreaker;

        public ZoomRestTemplate() {

            this.restTemplate = new RestTemplate(ZoomProctoringService.this.clientHttpRequestFactoryService
                    .getClientHttpRequestFactory()
                    .getOrThrow());

            this.circuitBreaker = ZoomProctoringService.this.asyncService.createCircuitBreaker(
                    2,
                    10 * Constants.SECOND_IN_MILLIS,
                    10 * Constants.SECOND_IN_MILLIS);
        }

        public ResponseEntity<String> testServiceConnection(final ProctoringServiceSettings proctoringSettings) {
            final String url = proctoringSettings.serverURL.endsWith(Constants.SLASH.toString())
                    ? proctoringSettings.serverURL + API_TEST_ENDPOINT
                    : proctoringSettings.serverURL + "/" + API_TEST_ENDPOINT;
            return exchange(url, HttpMethod.GET, proctoringSettings);
        }

        public ResponseEntity<String> createUser(final ProctoringServiceSettings proctoringSettings)
                throws JsonProcessingException {
            final String url = proctoringSettings.serverURL.endsWith(Constants.SLASH.toString())
                    ? proctoringSettings.serverURL + "v2/users"
                    : proctoringSettings.serverURL + "/" + "v2/users";
            final CreateUserRequest createUserRequest = new CreateUserRequest(
                    "custCreate",
                    new CreateUserRequest.UserInfo(
                            "andreas.hefti@let.ethz.ch",
                            1,
                            "Andreas",
                            "Hefti"));
            final String body = ZoomProctoringService.this.jsonMapper.writeValueAsString(createUserRequest);
            final HttpHeaders headers = getHeaders(proctoringSettings);
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return exchange(url, HttpMethod.POST, body, headers, null);
        }

        private HttpHeaders getHeaders(final ProctoringServiceSettings proctoringSettings) {
            final String jwt = createJWT(
                    proctoringSettings.appKey,
                    proctoringSettings.appSecret,
                    System.currentTimeMillis() + Constants.MINUTE_IN_MILLIS);

            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
            httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            return httpHeaders;
        }

        private ResponseEntity<String> exchange(
                final String url,
                final HttpMethod method,
                final ProctoringServiceSettings proctoringSettings) {

            return exchange(url, HttpMethod.GET, null, getHeaders(proctoringSettings), null);
        }

        private ResponseEntity<String> exchange(
                final String url,
                final HttpMethod method,
                final Object body,
                final HttpHeaders httpHeaders,
                final Map<String, ?> uriVariables) {

            final Result<ResponseEntity<String>> protectedRunResult = this.circuitBreaker.protectedRun(() -> {
                final HttpEntity<Object> httpEntity = (body != null)
                        ? new HttpEntity<>(body, httpHeaders)
                        : new HttpEntity<>(httpHeaders);

                final ResponseEntity<String> result = (uriVariables != null)
                        ? this.restTemplate.exchange(
                                url,
                                method,
                                httpEntity,
                                String.class,
                                uriVariables)
                        : this.restTemplate.exchange(
                                url,
                                method,
                                httpEntity,
                                String.class);

                if (result.getStatusCode() != HttpStatus.OK) {
                    log.warn("Zoom API call to {} respond not 200 -> {}", url, result.getStatusCode());
                }

                return result;
            });
            return protectedRunResult.getOrThrow();
        }

        private String createJWT(
                final String appKey,
                final CharSequence appSecret,
                final Long expTime) {

            try {
                final StringBuilder builder = new StringBuilder();
                final Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

                final String jwtHeaderPart = urlEncoder.encodeToString(
                        ZOOM_ACCESS_TOKEN_HEADER.getBytes(StandardCharsets.UTF_8));
                final String jwtPayload = createPayload(appKey, expTime);
                final String jwtPayloadPart = urlEncoder.encodeToString(
                        jwtPayload.getBytes(StandardCharsets.UTF_8));
                final String message = jwtHeaderPart + "." + jwtPayloadPart;

                final Mac sha256_HMAC = Mac.getInstance(TOKEN_ENCODE_ALG);
                final SecretKeySpec secret_key = new SecretKeySpec(
                        Utils.toByteArray(appSecret),
                        TOKEN_ENCODE_ALG);
                sha256_HMAC.init(secret_key);
                final String hash = urlEncoder.encodeToString(
                        sha256_HMAC.doFinal(Utils.toByteArray(message)));

                builder.append(message)
                        .append(".")
                        .append(hash);

                return builder.toString();
            } catch (final Exception e) {
                throw new RuntimeException("Failed to create JWT for Zoom API access: ", e);
            }
        }
    }

}
