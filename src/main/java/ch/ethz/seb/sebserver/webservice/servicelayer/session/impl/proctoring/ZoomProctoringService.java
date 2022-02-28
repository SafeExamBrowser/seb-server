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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
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
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.ApplyUserSettingsRequest;
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
    private static final String ZOOM_SDK_ACCESS_TOKEN_PAYLOAD =
            "{\"appKey\":\"%s\",\"iat\":%s,\"exp\":%s,\"tokenExp\":%s}";

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

    private static final Map<String, String> SEB_RECONFIG_INSTRUCTION_DEFAULTS = Utils.immutableMapOf(Arrays.asList(
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
    private final SEBClientInstructionService sebInstructionService;
    private final boolean enableWaitingRoom;
    private final boolean sendRejoinForCollectingRoom;

    public ZoomProctoringService(
            final ExamSessionService examSessionService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final Cryptor cryptor,
            final AsyncService asyncService,
            final JSONMapper jsonMapper,
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final AuthorizationService authorizationService,
            final SEBClientInstructionService sebInstructionService,
            @Value("${sebserver.webservice.proctoring.enableWaitingRoom:false}") final boolean enableWaitingRoom,
            @Value("${sebserver.webservice.proctoring.sendRejoinForCollectingRoom:true}") final boolean sendRejoinForCollectingRoom) {

        this.examSessionService = examSessionService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        this.cryptor = cryptor;
        this.asyncService = asyncService;
        this.jsonMapper = jsonMapper;
        this.zoomRestTemplate = new ZoomRestTemplate(this);
        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.authorizationService = authorizationService;
        this.sebInstructionService = sebInstructionService;
        this.enableWaitingRoom = enableWaitingRoom;
        this.sendRejoinForCollectingRoom = sendRejoinForCollectingRoom;
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
                        this.cryptor
                                .encrypt(proctoringSettings.appSecret)
                                .getOrThrow());

                final ResponseEntity<String> result = this.zoomRestTemplate
                        .testServiceConnection(
                                proctoringSettings.serverURL,
                                credentials);

                if (result.getStatusCode() != HttpStatus.OK) {
                    throw new APIMessageException(Arrays.asList(
                            APIMessage.fieldValidationError(ProctoringServiceSettings.ATTR_SERVER_URL,
                                    "proctoringSettings:serverURL:url.invalid"),
                            APIMessage.ErrorMessage.EXTERNAL_SERVICE_BINDING_ERROR.of()));
                }
            } catch (final Exception e) {
                log.error("Failed to access Zoom service at: {}", proctoringSettings.serverURL, e.getMessage());
                throw new APIMessageException(Arrays.asList(
                        APIMessage.fieldValidationError(ProctoringServiceSettings.ATTR_SERVER_URL,
                                "proctoringSettings:serverURL:url.noservice"),
                        APIMessage.ErrorMessage.EXTERNAL_SERVICE_BINDING_ERROR.of()));
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
                proctoringConnection.meetingId);
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_TOKEN,
                String.valueOf(proctoringConnection.accessToken));
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.ZOOM_SDK_TOKEN,
                String.valueOf(proctoringConnection.sdkToken));
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

            final String jwt = this.createSignatureForMeetingAccess(
                    credentials,
                    String.valueOf(additionalZoomRoomData.meeting_id),
                    true);

            String sdkJWT = null;
            if (StringUtils.isNotBlank(proctoringSettings.sdkKey)) {

                final ClientCredentials sdkCredentials = new ClientCredentials(
                        proctoringSettings.sdkKey,
                        proctoringSettings.sdkSecret,
                        remoteProctoringRoom.joinKey);

                sdkJWT = this.createJWTForSDKAccess(
                        sdkCredentials,
                        forExam(proctoringSettings));
            }

            return new ProctoringRoomConnection(
                    ProctoringServerType.ZOOM,
                    null,
                    proctoringSettings.serverURL,
                    additionalZoomRoomData.join_url,
                    roomName,
                    subject,
                    jwt,
                    sdkJWT,
                    credentials.accessToken,
                    credentials.clientId,
                    String.valueOf(additionalZoomRoomData.meeting_id),
                    this.authorizationService.getUserService().getCurrentUser().getUsername(),
                    remoteProctoringRoom.additionalRoomData);
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

            final String signature = this.createSignatureForMeetingAccess(
                    credentials,
                    String.valueOf(additionalZoomRoomData.meeting_id),
                    false);

            final ClientConnectionData clientConnection = this.examSessionService
                    .getConnectionData(connectionToken)
                    .getOrThrow();

            String sdkJWT = null;
            if (StringUtils.isNotBlank(proctoringSettings.sdkKey)) {

                final ClientCredentials sdkCredentials = new ClientCredentials(
                        proctoringSettings.sdkKey,
                        proctoringSettings.sdkSecret,
                        remoteProctoringRoom.joinKey);

                sdkJWT = this.createJWTForSDKAccess(
                        sdkCredentials,
                        forExam(proctoringSettings));
            }

            return new ProctoringRoomConnection(
                    ProctoringServerType.ZOOM,
                    connectionToken,
                    proctoringSettings.serverURL,
                    additionalZoomRoomData.join_url,
                    roomName,
                    subject,
                    signature,
                    sdkJWT,
                    credentials.accessToken,
                    credentials.clientId,
                    String.valueOf(additionalZoomRoomData.meeting_id),
                    clientConnection.clientConnection.userSessionId,
                    remoteProctoringRoom.additionalRoomData);
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
                getMeetingDuration(proctoringSettings.examId),
                proctoringSettings);
    }

    @Override
    public Result<NewRoom> newBreakOutRoom(
            final ProctoringServiceSettings proctoringSettings,
            final String subject) {

        return createAdHocMeeting(
                UUID.randomUUID().toString(),
                subject,
                getMeetingDuration(proctoringSettings.examId),
                proctoringSettings);
    }

    private int getMeetingDuration(final Long examId) {
        try {
            final DateTime endTime = this.examSessionService
                    .getRunningExam(examId)
                    .getOrThrow()
                    .getEndTime();
            final Long result = new Interval(DateTime.now(DateTimeZone.UTC), endTime)
                    .toDurationMillis() / Constants.MINUTE_IN_MILLIS;
            return result.intValue();
        } catch (final Exception e) {
            log.error("Failed to get duration for meeting from exam: {} cause: {}", examId, e.getMessage());
            return Constants.DAY_IN_MIN;
        }
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

                final ClientCredentials credentials = new ClientCredentials(
                        proctoringSettings.appKey,
                        proctoringSettings.appSecret);

                this.deleteAdHocMeeting(
                        proctoringSettings,
                        credentials,
                        additionalZoomRoomData.meeting_id,
                        additionalZoomRoomData.user_id)
                        .getOrThrow();

            } catch (final Exception e) {
                throw new RuntimeException(
                        "Unexpected error while trying to dispose ad-hoc room for zoom proctoring",
                        e);
            }
        });

    }

    @Override
    public Map<String, String> getDefaultReconfigInstructionAttributes() {
        return SEB_RECONFIG_INSTRUCTION_DEFAULTS;
    }

    @Override
    public Map<String, String> mapReconfigInstructionAttributes(final Map<String, String> attributes) {
        return attributes.entrySet().stream()
                .map(entry -> new Tuple<>(
                        SEB_API_NAME_INSTRUCTION_NAME_MAPPING.getOrDefault(entry.getKey(), entry.getKey()),
                        entry.getValue()))
                .collect(Collectors.toMap(Tuple::get_1, Tuple::get_2));
    }

    @Override
    public Result<Void> notifyBreakOutRoomOpened(
            final ProctoringServiceSettings proctoringSettings,
            final RemoteProctoringRoom room) {

        // Not needed for Zoom integration so far

        return Result.EMPTY;
    }

    @Override
    public Result<Void> notifyCollectingRoomOpened(
            final ProctoringServiceSettings proctoringSettings,
            final RemoteProctoringRoom room,
            final Collection<ClientConnection> clientConnections) {

        return Result.tryCatch(() -> {

            if (!this.sendRejoinForCollectingRoom) {
                // does nothing if the rejoin feature is not enabled
                return;
            }

            if (this.remoteProctoringRoomDAO.isTownhallRoomActive(proctoringSettings.examId)) {
                // does nothing if the town-hall of this exam is open. The clients will automatically join
                // the meeting once the town-hall has been closed
                return;
            }

            clientConnections.stream()
                    .forEach(cc -> {
                        try {
                            sendJoinInstruction(
                                    proctoringSettings.examId,
                                    cc.connectionToken,
                                    getClientRoomConnection(
                                            proctoringSettings,
                                            cc.connectionToken,
                                            room.name,
                                            room.subject)
                                                    .getOrThrow());
                        } catch (final Exception e) {
                            log.error("Failed to send rejoin instruction to SEB client: {}", cc.connectionToken, e);
                        }
                    });
        });
    }

    private void sendJoinInstruction(
            final Long examId,
            final String connectionToken,
            final ProctoringRoomConnection proctoringConnection) {

        final Map<String, String> attributes = this
                .createJoinInstructionAttributes(proctoringConnection);

        this.sebInstructionService
                .registerInstruction(
                        examId,
                        InstructionType.SEB_PROCTORING,
                        attributes,
                        connectionToken,
                        true)
                .onError(error -> log.error("Failed to send join instruction: {}", connectionToken, error));
    }

    private Result<NewRoom> createAdHocMeeting(
            final String roomName,
            final String subject,
            final int duration,
            final ProctoringServiceSettings proctoringSettings) {

        return Result.tryCatch(() -> {
            final ClientCredentials credentials = new ClientCredentials(
                    proctoringSettings.appKey,
                    proctoringSettings.appSecret);

            // First create a new user/host for the new room
            final ResponseEntity<String> createUser = this.zoomRestTemplate.createUser(
                    proctoringSettings.serverURL,
                    credentials,
                    roomName);

            final UserResponse userResponse = this.jsonMapper.readValue(
                    createUser.getBody(),
                    UserResponse.class);

            this.zoomRestTemplate.applyUserSettings(
                    proctoringSettings.serverURL,
                    credentials,
                    userResponse.id);

            // Then create new meeting with the ad-hoc user/host
            final CharSequence meetingPwd = UUID.randomUUID().toString().subSequence(0, 9);
            final ResponseEntity<String> createMeeting = this.zoomRestTemplate.createMeeting(
                    proctoringSettings.serverURL,
                    credentials,
                    userResponse.id,
                    subject,
                    duration,
                    meetingPwd,
                    this.enableWaitingRoom);

            final MeetingResponse meetingResponse = this.jsonMapper.readValue(
                    createMeeting.getBody(),
                    MeetingResponse.class);

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
                    meetingResponse.encryptedMeetingPwd,
                    additionalZoomRoomDataString);
        });
    }

    private Result<Void> deleteAdHocMeeting(
            final ProctoringServiceSettings proctoringSettings,
            final ClientCredentials credentials,
            final Long meetingId,
            final String userId) {

        return Result.tryCatch(() -> {

            this.zoomRestTemplate.deleteMeeting(proctoringSettings.serverURL, credentials, meetingId);
            this.zoomRestTemplate.deleteUser(proctoringSettings.serverURL, credentials, userId);

        });
    }

    private String createJWTForAPIAccess(
            final ClientCredentials credentials,
            final Long expTime) {

        try {

            final CharSequence decryptedSecret = this.cryptor
                    .decrypt(credentials.secret)
                    .getOrThrow();

            final StringBuilder builder = new StringBuilder();
            final Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

            final String jwtHeaderPart = urlEncoder
                    .encodeToString(ZOOM_ACCESS_TOKEN_HEADER.getBytes(StandardCharsets.UTF_8));

            final String jwtPayload = String.format(
                    ZOOM_API_ACCESS_TOKEN_PAYLOAD
                            .replaceAll(" ", "")
                            .replaceAll("\n", ""),
                    credentials.clientIdAsString(),
                    expTime);

            if (log.isTraceEnabled()) {
                log.trace("Zoom API Token payload: {}", jwtPayload);
            }

            final String jwtPayloadPart = urlEncoder
                    .encodeToString(jwtPayload.getBytes(StandardCharsets.UTF_8));

            final String message = jwtHeaderPart + "." + jwtPayloadPart;

            final Mac sha256_HMAC = Mac.getInstance(TOKEN_ENCODE_ALG);
            final SecretKeySpec secret_key = new SecretKeySpec(
                    Utils.toByteArray(decryptedSecret),
                    TOKEN_ENCODE_ALG);

            sha256_HMAC.init(secret_key);
            final String hash = urlEncoder
                    .encodeToString(sha256_HMAC.doFinal(Utils.toByteArray(message)));

            builder.append(message)
                    .append(".")
                    .append(hash);

            return builder.toString();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create JWT for Zoom API access: ", e);
        }
    }

    private String createJWTForSDKAccess(
            final ClientCredentials sdkCredentials,
            final Long expTime) {

        try {

            final CharSequence decryptedSecret = this.cryptor
                    .decrypt(sdkCredentials.secret)
                    .getOrThrow();

            final StringBuilder builder = new StringBuilder();
            final Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

            final String jwtHeaderPart = urlEncoder
                    .encodeToString(ZOOM_ACCESS_TOKEN_HEADER.getBytes(StandardCharsets.UTF_8));

            // epoch time in seconds
            final long secondsNow = Utils.getSecondsNow();

            final String jwtPayload = String.format(
                    ZOOM_SDK_ACCESS_TOKEN_PAYLOAD
                            .replaceAll(" ", "")
                            .replaceAll("\n", ""),
                    sdkCredentials.clientIdAsString(),
                    secondsNow,
                    expTime,
                    expTime);

            if (log.isTraceEnabled()) {
                log.trace("Zoom SDK Token payload: {}", jwtPayload);
            }

            final String jwtPayloadPart = urlEncoder
                    .encodeToString(jwtPayload.getBytes(StandardCharsets.UTF_8));

            final String message = jwtHeaderPart + "." + jwtPayloadPart;

            final Mac sha256_HMAC = Mac.getInstance(TOKEN_ENCODE_ALG);
            final SecretKeySpec secret_key = new SecretKeySpec(
                    Utils.toByteArray(decryptedSecret),
                    TOKEN_ENCODE_ALG);

            sha256_HMAC.init(secret_key);
            final String hash = urlEncoder
                    .encodeToString(sha256_HMAC.doFinal(Utils.toByteArray(message)));

            builder.append(message)
                    .append(".")
                    .append(hash);

            return builder.toString();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create JWT for Zoom API access: ", e);
        }
    }

    private String createSignatureForMeetingAccess(
            final ClientCredentials credentials,
            final String meetingId,
            final boolean host) {

        try {
            final String apiKey = credentials.clientIdAsString();
            final int status = host ? 1 : 0;
            final CharSequence decryptedSecret = this.cryptor
                    .decrypt(credentials.secret)
                    .getOrThrow();

            final Mac hasher = Mac.getInstance("HmacSHA256");
            final String ts = Long.toString(System.currentTimeMillis() - 30000);
            final String msg = String.format("%s%s%s%d", apiKey, meetingId, ts, status);

            hasher.init(new SecretKeySpec(decryptedSecret.toString().getBytes(), "HmacSHA256"));

            final String message = Base64.getEncoder().encodeToString(msg.getBytes());
            final byte[] hash = hasher.doFinal(message.getBytes());

            final String hashBase64Str = DatatypeConverter.printBase64Binary(hash);
            final String tmpString = String.format("%s.%s.%s.%d.%s", apiKey, meetingId, ts, status, hashBase64Str);
            final String encodedString = Base64.getEncoder().encodeToString(tmpString.getBytes());

            if (log.isTraceEnabled()) {
                log.trace("Zoom Meeting signature payload: {}", tmpString);
            }

            return encodedString.replaceAll("\\=+$", "");

        } catch (final Exception e) {
            throw new RuntimeException("Failed to create JWT for Zoom meeting access: ", e);
        }
    }

    private long forExam(final ProctoringServiceSettings examProctoring) {

        // NOTE: following is the original code that includes the exam end time but seems to make trouble for OLAT
        final long nowInSeconds = Utils.getSecondsNow();
        final long nowPlus30MinInSeconds = nowInSeconds + Utils.toSeconds(30 * Constants.MINUTE_IN_MILLIS);
        final long nowPlusOneDayInSeconds = nowInSeconds + Utils.toSeconds(Constants.DAY_IN_MILLIS);
        final long nowPlusTwoDayInSeconds = nowInSeconds + Utils.toSeconds(2 * Constants.DAY_IN_MILLIS);

        long expTime = nowPlusOneDayInSeconds;
        if (examProctoring.examId == null && this.examSessionService.isExamRunning(examProctoring.examId)) {
            final Exam exam = this.examSessionService.getRunningExam(examProctoring.examId)
                    .getOrThrow();
            if (exam.endTime != null) {
                expTime = Utils.toSeconds(exam.endTime.getMillis());
            }
        }
        // refer to https://marketplace.zoom.us/docs/sdk/native-sdks/auth
        // "exp": 0, //JWT expiration date (Min:1800 seconds greater than iat value, Max: 48 hours greater than iat value) in epoch format.
        if (expTime > nowPlusTwoDayInSeconds) {
            expTime = nowPlusTwoDayInSeconds - 10; // Do not set to max because it is not well defined if max is included or not
        } else if (expTime < nowPlus30MinInSeconds) {
            expTime = nowPlusOneDayInSeconds;
        }

        log.debug("**** SDK Token exp time with exam-end-time inclusion would be: {}", expTime);

        // NOTE: Set this to the maximum according to https://marketplace.zoom.us/docs/sdk/native-sdks/auth
        return nowPlusTwoDayInSeconds - 10; // Do not set to max because it is not well defined if max is included or not;
    }

    private final static class ZoomRestTemplate {

        private static final int LIZENSED_USER = 2;
        private static final String API_TEST_ENDPOINT = "v2/users";
        private static final String API_CREATE_USER_ENDPOINT = "v2/users";
        private static final String API_APPLY_USER_SETTINGS_ENDPOINT = "v2/users/{userId}/settings";
        private static final String API_DELETE_USER_ENDPOINT = "v2/users/{userid}?action=delete";
        private static final String API_USER_CUST_CREATE = "custCreate";
        private static final String API_ZOOM_ROOM_USER = "SEBProctoringRoomUser";
        private static final String API_CREATE_MEETING_ENDPOINT = "v2/users/{userid}/meetings";
        private static final String API_DELETE_MEETING_ENDPOINT = "v2/meetings/{meetingid}";
        private static final String API_END_MEETING_ENDPOINT = "v2/meetings/{meetingid}/status";

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
                                LIZENSED_USER,
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

        public ResponseEntity<String> applyUserSettings(
                final String zoomServerUrl,
                final ClientCredentials credentials,
                final String userId) {
            try {
                final String url = UriComponentsBuilder
                        .fromUriString(zoomServerUrl)
                        .path(API_APPLY_USER_SETTINGS_ENDPOINT)
                        .buildAndExpand(userId)
                        .normalize()
                        .toUriString();

                final ApplyUserSettingsRequest applySettingsRequest = new ApplyUserSettingsRequest();
                final String body = this.zoomProctoringService.jsonMapper.writeValueAsString(applySettingsRequest);
                final HttpHeaders headers = getHeaders(credentials);

                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                final ResponseEntity<String> exchange = exchange(url, HttpMethod.PATCH, body, headers);
                return exchange;
            } catch (final Exception e) {
                log.error("Failed to apply user settings for Zoom user: {}", userId, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        public ResponseEntity<String> createMeeting(
                final String zoomServerUrl,
                final ClientCredentials credentials,
                final String userId,
                final String topic,
                final int duration,
                final CharSequence password,
                final boolean waitingRoom) {

            try {

                final String url = UriComponentsBuilder
                        .fromUriString(zoomServerUrl)
                        .path(API_CREATE_MEETING_ENDPOINT)
                        .buildAndExpand(userId)
                        .toUriString();

                final CreateMeetingRequest createRoomRequest = new CreateMeetingRequest(
                        topic,
                        duration,
                        password,
                        waitingRoom);

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
                final Long meetingId) {

            // try to set set meeting status to ended first
            try {

                final String url = UriComponentsBuilder
                        .fromUriString(zoomServerUrl)
                        .path(API_END_MEETING_ENDPOINT)
                        .buildAndExpand(meetingId)
                        .toUriString();

                final HttpHeaders headers = getHeaders(credentials);
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

                final ResponseEntity<String> exchange = exchange(
                        url,
                        HttpMethod.PUT,
                        "{\"action\": \"end\"}",
                        headers);

                if (log.isDebugEnabled() && exchange.getStatusCodeValue() != 204) {
                    log.debug("Failed to set meeting to end state. Meeting: {}, http: {}",
                            meetingId,
                            exchange.getStatusCodeValue());
                }

            } catch (final Exception e) {
                log.warn("Failed to end Zoom ad-hoc meeting: {} cause: {} / {}",
                        meetingId,
                        e.getMessage(),
                        (e.getCause() != null) ? e.getCause().getMessage() : Constants.EMPTY_NOTE);
            }

            try {

                final String url = UriComponentsBuilder
                        .fromUriString(zoomServerUrl)
                        .path(API_DELETE_MEETING_ENDPOINT)
                        .buildAndExpand(meetingId)
                        .toUriString();

                return exchange(url, HttpMethod.DELETE, credentials);

            } catch (final Exception e) {
                log.warn("Failed to delete Zoom ad-hoc meeting: {} cause: {} / {}",
                        meetingId,
                        e.getMessage(),
                        (e.getCause() != null) ? e.getCause().getMessage() : Constants.EMPTY_NOTE);
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
                log.error("Failed to delete Zoom ad-hoc user with id: {} cause: {} / {}",
                        userId,
                        e.getMessage(),
                        (e.getCause() != null) ? e.getCause().getMessage() : Constants.EMPTY_NOTE);
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

            return exchange(url, method, null, getHeaders(credentials));
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

                try {
                    final ResponseEntity<String> result = this.restTemplate.exchange(
                            url,
                            method,
                            httpEntity,
                            String.class);

                    if (result.getStatusCode().value() >= 400) {
                        log.warn("Error response on Zoom API call to {} response status: {}", url,
                                result.getStatusCode());
                    }

                    return result;
                } catch (final RestClientResponseException rce) {
                    return ResponseEntity
                            .status(rce.getRawStatusCode())
                            .body(rce.getResponseBodyAsString());
                }
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
