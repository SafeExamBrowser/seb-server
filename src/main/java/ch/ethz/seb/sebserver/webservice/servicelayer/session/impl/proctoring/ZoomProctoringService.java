/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.RemoteProctoringRoomDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.CreateMeetingRequest;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.CreateUserRequest;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.MeetingResponse;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.UserResponse;

@Lazy
@Service
@WebServiceProfile
public class ZoomProctoringService implements ExamProctoringService {

    private static final Logger log = LoggerFactory.getLogger(ZoomProctoringService.class);

    private static final String TOKEN_ENCODE_ALG = "HmacSHA256";

    private static final String ZOOM_ACCESS_TOKEN_HEADER =
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String ZOOM_API_ACCESS_TOKEN_PAYLOAD =
            "{\"iss\":\"%s\",\"exp\":%s}";
    private static final String ZOOM_MEETING_ACCESS_TOKEN_PAYLOAD =
            "{\"app_key\":\"%s\",\"iat\":%s,\"exp\":%s,\"tpc\":\"%s\",\"pwd\":\"%s\"}";

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
    private final RemoteProctoringRoomDAO remoteProctoringRoomDAO;
    private final AuthorizationService authorizationService;

    public ZoomProctoringService(
            final ExamSessionService examSessionService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final Cryptor cryptor,
            final AsyncService asyncService,
            final JSONMapper jsonMapper,
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final AuthorizationService authorizationService) {

        this.examSessionService = examSessionService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        this.cryptor = cryptor;
        this.asyncService = asyncService;
        this.jsonMapper = jsonMapper;
        this.zoomRestTemplate = new ZoomRestTemplate(this);
        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.authorizationService = authorizationService;
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

                final ClientCredentials credentials = new ClientCredentials(
                        proctoringSettings.appKey,
                        proctoringSettings.appSecret);

                final ResponseEntity<String> result = this.zoomRestTemplate
                        .testServiceConnection(
                                proctoringSettings.serverURL,
                                credentials);

                if (result.getStatusCode() != HttpStatus.OK) {
                    throw new APIMessageException(
                            APIMessage.ErrorMessage.BINDING_ERROR,
                            String.valueOf(result.getStatusCode()));
                } else {
                    // TODO this is just for cleaning up along development process.
                    // Remove this before finish up the Zoom integration

                    try {

                    } catch (final Exception e) {
                        log.error("Failed to dev-cleanup rooms: ", e);
                    }
                }

            } catch (final Exception e) {
                log.error("Failed to access Zoom service at: {}", proctoringSettings.serverURL, e);
                throw new APIMessageException(APIMessage.ErrorMessage.BINDING_ERROR, e.getMessage());
            }

            return true;
        });
    }

    @Override
    public Map<String, String> createJoinInstructionAttributes(final ProctoringRoomConnection proctoringConnection) {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.SERVICE_TYPE,
                ProctoringServiceSettings.ProctoringServerType.ZOOM.name());
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.METHOD,
                ClientInstruction.ProctoringInstructionMethod.JOIN.name());
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_URL,
                proctoringConnection.serverURL);
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_ROOM,
                proctoringConnection.roomName);
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_TOKEN,
                String.valueOf(proctoringConnection.accessToken));
        if (StringUtils.isNotBlank(proctoringConnection.apiKey)) {
            attributes.put(
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_API_KEY,
                    String.valueOf(proctoringConnection.apiKey));
        }
        if (StringUtils.isNotBlank(proctoringConnection.roomKey)) {
            attributes.put(
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_MEETING_KEY,
                    String.valueOf(proctoringConnection.roomKey));
        }
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_USER_NAME,
                proctoringConnection.userName);
        if (StringUtils.isNotBlank(proctoringConnection.subject)) {
            attributes.put(
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_ROOM_SUBJECT,
                    proctoringConnection.subject);
        }

        return attributes;
    }

    @Override
    public Result<ProctoringRoomConnection> getProctorRoomConnection(
            final ProctoringServiceSettings proctoringSettings,
            final String roomName,
            final String subject) {

        return Result.tryCatch(() -> {

            final RemoteProctoringRoom remoteProctoringRoom = this.remoteProctoringRoomDAO
                    .getRoom(proctoringSettings.examId, roomName)
                    .getOrThrow();

            final AdditionalZoomRoomData additionalZoomRoomData = this.jsonMapper.readValue(
                    remoteProctoringRoom.additionalRoomData,
                    AdditionalZoomRoomData.class);

            final ClientCredentials credentials = new ClientCredentials(
                    proctoringSettings.appKey,
                    proctoringSettings.appSecret,
                    remoteProctoringRoom.joinKey);

            final String jwt = this.createJWTForMeetingAccess(
                    credentials,
                    String.valueOf(additionalZoomRoomData.meeting_id),
                    true);

            return new ProctoringRoomConnection(
                    ProctoringServerType.ZOOM,
                    null,
                    proctoringSettings.serverURL,
                    additionalZoomRoomData.join_url,
                    roomName,
                    subject,
                    jwt,
                    this.cryptor.decrypt(credentials.accessToken),
                    credentials.clientId,
                    String.valueOf(additionalZoomRoomData.meeting_id),
                    this.authorizationService.getUserService().getCurrentUser().getUsername());
        });
    }

    @Override
    public Result<ProctoringRoomConnection> getClientRoomConnection(
            final ProctoringServiceSettings proctoringSettings,
            final String connectionToken,
            final String roomName,
            final String subject) {

        return Result.tryCatch(() -> {

            final RemoteProctoringRoom remoteProctoringRoom = this.remoteProctoringRoomDAO
                    .getRoom(proctoringSettings.examId, roomName)
                    .getOrThrow();

            final AdditionalZoomRoomData additionalZoomRoomData = this.jsonMapper.readValue(
                    remoteProctoringRoom.additionalRoomData,
                    AdditionalZoomRoomData.class);

            final ClientCredentials credentials = new ClientCredentials(
                    proctoringSettings.appKey,
                    proctoringSettings.appSecret,
                    remoteProctoringRoom.joinKey);

            final String jwt = this.createJWTForMeetingAccess(
                    credentials,
                    String.valueOf(additionalZoomRoomData.meeting_id),
                    false);

            final ClientConnectionData clientConnection = this.examSessionService
                    .getConnectionData(connectionToken)
                    .getOrThrow();

            return new ProctoringRoomConnection(
                    ProctoringServerType.ZOOM,
                    connectionToken,
                    proctoringSettings.serverURL,
                    additionalZoomRoomData.join_url,
                    roomName,
                    subject,
                    jwt,
                    this.cryptor.decrypt(credentials.accessToken),
                    credentials.clientId,
                    String.valueOf(additionalZoomRoomData.meeting_id),
                    clientConnection.clientConnection.userSessionId);
        });
    }

    @Override
    public Result<Void> disposeServiceRoomsForExam(
            final Long examId,
            final ProctoringServiceSettings proctoringSettings) {

        return Result.tryCatch(() -> {

            this.remoteProctoringRoomDAO
                    .getRooms(examId)
                    .getOrThrow()
                    .stream()
                    .forEach(room -> {
                        disposeBreakOutRoom(proctoringSettings, room.name)
                                .onError(error -> log.warn("Failed to dispose proctoring room record for: {} cause: {}",
                                        room,
                                        error.getMessage()));
                    });
        });
    }

    @Override
    public Result<NewRoom> newCollectingRoom(
            final ProctoringServiceSettings proctoringSettings,
            final Long roomNumber) {

        return createAdHocMeeting(
                UUID.randomUUID().toString(),
                "Proctoring Room " + (roomNumber + 1),
                proctoringSettings);
    }

    @Override
    public Result<NewRoom> newBreakOutRoom(
            final ProctoringServiceSettings proctoringSettings,
            final String subject) {

        return createAdHocMeeting(
                UUID.randomUUID().toString(),
                subject,
                proctoringSettings);
    }

    @Override
    public Result<Void> disposeBreakOutRoom(
            final ProctoringServiceSettings proctoringSettings,
            final String roomName) {

        return Result.tryCatch(() -> {
            try {

                final RemoteProctoringRoom roomData = this.remoteProctoringRoomDAO
                        .getRoom(proctoringSettings.examId, roomName)
                        .getOrThrow();

                final AdditionalZoomRoomData additionalZoomRoomData = this.jsonMapper.readValue(
                        roomData.getAdditionalRoomData(),
                        AdditionalZoomRoomData.class);

                this.deleteAdHocMeeting(
                        proctoringSettings,
                        roomName,
                        additionalZoomRoomData.user_id)
                        .getOrThrow();

            } catch (final Exception e) {
                throw new RuntimeException("Unexpected error while trying to dispose ad-hoc room for zoom proctoring");
            }
        });

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

    private Result<NewRoom> createAdHocMeeting(
            final String roomName,
            final String subject,
            final ProctoringServiceSettings proctoringSettings) {

        return Result.tryCatch(() -> {
            final ClientCredentials credentials = new ClientCredentials(
                    proctoringSettings.appKey,
                    this.cryptor.decrypt(proctoringSettings.appSecret));

            // First create a new user/host for the new room
            final ResponseEntity<String> createUser = this.zoomRestTemplate.createUser(
                    proctoringSettings.serverURL,
                    credentials,
                    roomName);
            final UserResponse userResponse = this.jsonMapper.readValue(
                    createUser.getBody(),
                    UserResponse.class);

            // Then create new meeting with the ad-hoc user/host
            final CharSequence meetingPwd = UUID.randomUUID().toString().subSequence(0, 9);
            final ResponseEntity<String> createMeeting = this.zoomRestTemplate.createMeeting(
                    proctoringSettings.serverURL,
                    credentials,
                    userResponse.id,
                    subject,
                    meetingPwd);
            final MeetingResponse meetingResponse = this.jsonMapper.readValue(
                    createMeeting.getBody(),
                    MeetingResponse.class);

            // TODO start the meeting automatically ???

            // Create NewRoom data with all needed information to store persistent
            final AdditionalZoomRoomData additionalZoomRoomData = new AdditionalZoomRoomData(
                    meetingResponse.id,
                    userResponse.id,
                    meetingResponse.start_url,
                    meetingResponse.join_url);
            final String additionalZoomRoomDataString = this.jsonMapper
                    .writeValueAsString(additionalZoomRoomData);

            return new NewRoom(
                    roomName,
                    subject,
                    this.cryptor.encrypt(meetingResponse.meetingPwd),
                    additionalZoomRoomDataString);
        });
    }

    private Result<Void> deleteAdHocMeeting(
            final ProctoringServiceSettings proctoringSettings,
            final String meetingId,
            final String userId) {

        return Result.tryCatch(() -> {

            final ClientCredentials credentials = new ClientCredentials(
                    proctoringSettings.appKey,
                    this.cryptor.decrypt(proctoringSettings.appSecret));

            this.zoomRestTemplate.deleteMeeting(proctoringSettings.serverURL, credentials, meetingId);
            this.zoomRestTemplate.deleteUser(proctoringSettings.serverURL, credentials, userId);

        });
    }

    private String createJWTForAPIAccess(
            final ClientCredentials credentials,
            final Long expTime) {

        try {

            CharSequence decryptedSecret = credentials.secret;
            try {
                decryptedSecret = this.cryptor.decrypt(credentials.secret);
            } catch (final Exception e) {
                log.debug("Testing zoom account connection");
            }

            final StringBuilder builder = new StringBuilder();
            final Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

            final String jwtHeaderPart = urlEncoder.encodeToString(
                    ZOOM_ACCESS_TOKEN_HEADER.getBytes(StandardCharsets.UTF_8));
            final String jwtPayload = String.format(
                    ZOOM_API_ACCESS_TOKEN_PAYLOAD.replaceAll(" ", "").replaceAll("\n", ""),
                    credentials.clientIdAsString(),
                    expTime);
            final String jwtPayloadPart = urlEncoder.encodeToString(
                    jwtPayload.getBytes(StandardCharsets.UTF_8));
            final String message = jwtHeaderPart + "." + jwtPayloadPart;

            final Mac sha256_HMAC = Mac.getInstance(TOKEN_ENCODE_ALG);
            final SecretKeySpec secret_key = new SecretKeySpec(
                    Utils.toByteArray(decryptedSecret),
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

    private String createJWTForMeetingAccess(
            final ClientCredentials credentials,
            final String meetingId,
            final boolean host) {

        try {
            final String apiKey = credentials.clientIdAsString();
            final int status = host ? 1 : 0;
            final CharSequence decryptedSecret = this.cryptor.decrypt(credentials.secret);

            final Mac hasher = Mac.getInstance("HmacSHA256");
            final String ts = Long.toString(System.currentTimeMillis() - 30000);
            final String msg = String.format("%s%s%s%d", apiKey, meetingId, ts, status);

            hasher.init(new SecretKeySpec(decryptedSecret.toString().getBytes(), "HmacSHA256"));

            final String message = Base64.getEncoder().encodeToString(msg.getBytes());
            final byte[] hash = hasher.doFinal(message.getBytes());

            final String hashBase64Str = DatatypeConverter.printBase64Binary(hash);
            final String tmpString = String.format("%s.%s.%s.%d.%s", apiKey, meetingId, ts, status, hashBase64Str);
            final String encodedString = Base64.getEncoder().encodeToString(tmpString.getBytes());

            return encodedString.replaceAll("\\=+$", "");

//            final long iat = Utils.getMillisecondsNow() / 1000;
//            final long exp = iat + 7200;
//
//            final CharSequence decryptedSecret = this.cryptor.decrypt(credentials.secret);
//            final CharSequence decryptedMeetingPWD = this.cryptor.decrypt(credentials.secret);
//            final StringBuilder builder = new StringBuilder();
//            final Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
//
//            final String jwtHeaderPart = urlEncoder.encodeToString(
//                    ZOOM_ACCESS_TOKEN_HEADER.getBytes(StandardCharsets.UTF_8));
//            final String jwtPayload = String.format(
//                    ZOOM_MEETING_ACCESS_TOKEN_PAYLOAD.replaceAll(" ", "").replaceAll("\n", ""),
//                    credentials.clientIdAsString(),
//                    iat,
//                    exp,
//                    subject,
//                    decryptedMeetingPWD);
//            final String jwtPayloadPart = urlEncoder.encodeToString(
//                    jwtPayload.getBytes(StandardCharsets.UTF_8));
//            final String message = jwtHeaderPart + "." + jwtPayloadPart;
//
//            final Mac sha256_HMAC = Mac.getInstance(TOKEN_ENCODE_ALG);
//            final SecretKeySpec secret_key = new SecretKeySpec(
//                    Utils.toByteArray(decryptedSecret),
//                    TOKEN_ENCODE_ALG);
//            sha256_HMAC.init(secret_key);
//            final String hash = urlEncoder.encodeToString(
//                    sha256_HMAC.doFinal(Utils.toByteArray(message)));
//
//            builder.append(message)
//                    .append(".")
//                    .append(hash);
//
//            return builder.toString();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create JWT for Zoom meeting access: ", e);
        }
    }

    private final static class ZoomRestTemplate {

        private static final String API_TEST_ENDPOINT = "v2/users";
        private static final String API_CREATE_USER_ENDPOINT = "v2/users";
        private static final String API_DELETE_USER_ENDPOINT = "v2/users/{userid}?action=delete";
        private static final String API_USER_CUST_CREATE = "custCreate";
        private static final String API_ZOOM_ROOM_USER = "SEBProctoringRoomUser";
        private static final String API_CREATE_MEETING_ENDPOINT = "v2/users/{userid}/meetings";
        private static final String API_DELETE_MEETING_ENDPOINT = "v2/meetings/{meetingid}";

        private final ZoomProctoringService zoomProctoringService;
        private final RestTemplate restTemplate;
        private final CircuitBreaker<ResponseEntity<String>> circuitBreaker;

        public ZoomRestTemplate(final ZoomProctoringService zoomProctoringService) {

            this.zoomProctoringService = zoomProctoringService;
            this.restTemplate = new RestTemplate(zoomProctoringService.clientHttpRequestFactoryService
                    .getClientHttpRequestFactory()
                    .getOrThrow());

            this.circuitBreaker = zoomProctoringService.asyncService.createCircuitBreaker(
                    2,
                    10 * Constants.SECOND_IN_MILLIS,
                    10 * Constants.SECOND_IN_MILLIS);
        }

        public ResponseEntity<String> testServiceConnection(
                final String zoomServerUrl,
                final ClientCredentials credentials) {

            try {

                final String url = UriComponentsBuilder
                        .fromUriString(zoomServerUrl)
                        .path(API_TEST_ENDPOINT)
                        .queryParam("status", "active")
                        .queryParam("page_size", "10")
                        .queryParam("page_number", "1")
                        .queryParam("data_type", "Json")
                        .build()
                        .toUriString();
                return exchange(url, HttpMethod.GET, credentials);

            } catch (final Exception e) {
                log.error("Failed to test zoom service connection: ", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        public ResponseEntity<String> createUser(
                final String zoomServerUrl,
                final ClientCredentials credentials,
                final String roomName) {

            try {
                final String url = UriComponentsBuilder
                        .fromUriString(zoomServerUrl)
                        .path(API_CREATE_USER_ENDPOINT)
                        .toUriString();
                final String host = new URL(zoomServerUrl).getHost();
                final CreateUserRequest createUserRequest = new CreateUserRequest(
                        API_USER_CUST_CREATE,
                        new CreateUserRequest.UserInfo(
                                roomName + "@" + host,
                                1,
                                roomName,
                                API_ZOOM_ROOM_USER));

                final String body = this.zoomProctoringService.jsonMapper.writeValueAsString(createUserRequest);
                final HttpHeaders headers = getHeaders(credentials);
                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                return exchange(url, HttpMethod.POST, body, headers);

            } catch (final Exception e) {
                log.error("Failed to create Zoom ad-hoc user for room: {}", roomName, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        public ResponseEntity<String> createMeeting(
                final String zoomServerUrl,
                final ClientCredentials credentials,
                final String userId,
                final String topic,
                final CharSequence password) {

            try {

                final String url = UriComponentsBuilder
                        .fromUriString(zoomServerUrl)
                        .path(API_CREATE_MEETING_ENDPOINT)
                        .buildAndExpand(userId)
                        .toUriString();

                final CreateMeetingRequest createRoomRequest = new CreateMeetingRequest(topic, password);

                final String body = this.zoomProctoringService.jsonMapper.writeValueAsString(createRoomRequest);
                final HttpHeaders headers = getHeaders(credentials);
                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                return exchange(url, HttpMethod.POST, body, headers);

            } catch (final Exception e) {
                log.error("Failed to create Zoom ad-hoc meeting: {}", topic, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        public ResponseEntity<String> deleteMeeting(
                final String zoomServerUrl,
                final ClientCredentials credentials,
                final String meetingId) {

            try {

                final String url = UriComponentsBuilder
                        .fromUriString(zoomServerUrl)
                        .path(API_DELETE_MEETING_ENDPOINT)
                        .buildAndExpand(meetingId)
                        .toUriString();

                return exchange(url, HttpMethod.DELETE, credentials);

            } catch (final Exception e) {
                log.error("Failed to delete Zoom ad-hoc meeting: {}", meetingId, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        public ResponseEntity<String> deleteUser(
                final String zoomServerUrl,
                final ClientCredentials credentials,
                final String userId) {

            try {
                final String url = UriComponentsBuilder
                        .fromUriString(zoomServerUrl)
                        .path(API_DELETE_USER_ENDPOINT)
                        .buildAndExpand(userId)
                        .normalize()
                        .toUriString();

                return exchange(url, HttpMethod.DELETE, credentials);

            } catch (final Exception e) {
                log.error("Failed to delete Zoom ad-hoc user with id: {}", userId, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        private HttpHeaders getHeaders(final ClientCredentials credentials) {
            final String jwt = this.zoomProctoringService
                    .createJWTForAPIAccess(
                            credentials,
                            System.currentTimeMillis() + Constants.MINUTE_IN_MILLIS);

            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
            httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            return httpHeaders;
        }

        private ResponseEntity<String> exchange(
                final String url,
                final HttpMethod method,
                final ClientCredentials credentials) {

            return exchange(url, HttpMethod.GET, null, getHeaders(credentials));
        }

        private ResponseEntity<String> exchange(
                final String url,
                final HttpMethod method,
                final Object body,
                final HttpHeaders httpHeaders) {

            final Result<ResponseEntity<String>> protectedRunResult = this.circuitBreaker.protectedRun(() -> {
                final HttpEntity<Object> httpEntity = (body != null)
                        ? new HttpEntity<>(body, httpHeaders)
                        : new HttpEntity<>(httpHeaders);

                final ResponseEntity<String> result = this.restTemplate.exchange(
                        url, //"https://ethz.zoom.us/v2/users?Fstatus=active&page_size=30&page_number=1&data_type=Json",
                        method,
                        httpEntity,
                        String.class);

                if (result.getStatusCode() != HttpStatus.OK && result.getStatusCode() != HttpStatus.CREATED) {
                    log.warn("Error response on Zoom API call to {} response status: {}", url, result.getStatusCode());
                }

                return result;
            });
            return protectedRunResult.getOrThrow();
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class AdditionalZoomRoomData {

        @JsonProperty("meeting_id")
        private final Long meeting_id;
        @JsonProperty("user_id")
        public final String user_id;
        @JsonProperty("start_url")
        public final String start_url;
        @JsonProperty("join_url")
        public final String join_url;

        @JsonCreator
        public AdditionalZoomRoomData(
                @JsonProperty("meeting_id") final Long meeting_id,
                @JsonProperty("user_id") final String user_id,
                @JsonProperty("start_url") final String start_url,
                @JsonProperty("join_url") final String join_url) {

            this.meeting_id = meeting_id;
            this.user_id = user_id;
            this.start_url = start_url;
            this.join_url = join_url;
        }
    }

}
