/*
 * Copyright (c) 2021 ETH Zürich, IT Services
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.OAuth2AccessTokenSupport;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.RemoteProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.AdditionalZoomRoomData;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.ApplyUserSettingsRequest;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.CreateMeetingRequest;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.CreateUserRequest;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.MeetingResponse;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.SDKJWTPayload;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ZoomRoomRequestResponse.UserResponse;

@Lazy
@Service
@WebServiceProfile
public class ZoomProctoringService implements RemoteProctoringService {

    private static final Logger log = LoggerFactory.getLogger(ZoomProctoringService.class);

    private static final String TOKEN_ENCODE_ALG = "HmacSHA256";

    private static final String ZOOM_ACCESS_TOKEN_HEADER =
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

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
    private final Cryptor cryptor;
    private final AsyncService asyncService;
    private final JSONMapper jsonMapper;
    private final RemoteProctoringRoomDAO remoteProctoringRoomDAO;
    private final AuthorizationService authorizationService;
    private final SEBClientInstructionService sebInstructionService;
    private final boolean enableWaitingRoom;
    private final boolean sendRejoinForCollectingRoom;
    private final int tokenExpirySeconds;

    public ZoomProctoringService(
            final ExamSessionService examSessionService,
            final Cryptor cryptor,
            final AsyncService asyncService,
            final JSONMapper jsonMapper,
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final AuthorizationService authorizationService,
            final SEBClientInstructionService sebInstructionService,
            @Value("${sebserver.webservice.proctoring.enableWaitingRoom:false}") final boolean enableWaitingRoom,
            @Value("${sebserver.webservice.proctoring.sendRejoinForCollectingRoom:false}") final boolean sendRejoinForCollectingRoom,
            @Value("${sebserver.webservice.proctoring.zoom.tokenexpiry.seconds:86400}") final int tokenExpirySeconds) {

        this.examSessionService = examSessionService;
        this.cryptor = cryptor;
        this.asyncService = asyncService;
        this.jsonMapper = jsonMapper;
        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.authorizationService = authorizationService;
        this.sebInstructionService = sebInstructionService;
        this.enableWaitingRoom = enableWaitingRoom;
        this.sendRejoinForCollectingRoom = sendRejoinForCollectingRoom;
        this.tokenExpirySeconds = tokenExpirySeconds;
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

                final ProctoringServiceSettings proctoringServiceSettings = new ProctoringServiceSettings(
                        proctoringSettings.examId,
                        proctoringSettings.enableProctoring,
                        proctoringSettings.serverType,
                        proctoringSettings.serverURL,
                        proctoringSettings.collectingRoomSize,
                        proctoringSettings.enabledFeatures,
                        proctoringSettings.serviceInUse,
                        null,
                        null,
                        proctoringSettings.accountId,
                        proctoringSettings.clientId,
                        this.cryptor.encrypt(proctoringSettings.clientSecret).getOrThrow(),
                        proctoringSettings.sdkKey,
                        this.cryptor.encrypt(proctoringSettings.sdkSecret).getOrThrow(),
                        proctoringSettings.useZoomAppClientForCollectingRoom);

                final ZoomRestTemplate newRestTemplate = createNewRestTemplate(proctoringServiceSettings);

                final ResponseEntity<String> result = newRestTemplate.testServiceConnection();

                if (result.getStatusCode() != HttpStatus.OK) {
                    throw new RuntimeException("Invalid Zoom Service response: " + result);
                }
            } catch (final Exception e) {
                log.error("Failed to access Zoom service at: {}", proctoringSettings.serverURL, e);
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

            // Note: since SEB Server version 1.5 we work only with SDKKey instead of AppKey which is deprecated
            final ClientCredentials sdkCredentials = new ClientCredentials(
                    proctoringSettings.sdkKey,
                    proctoringSettings.sdkSecret,
                    remoteProctoringRoom.joinKey);

            final String sdkJWT = this.createSDKJWT(
                    sdkCredentials,
                    expiryTimeforExam(proctoringSettings),
                    additionalZoomRoomData.meeting_id,
                    true);

            return new ProctoringRoomConnection(
                    ProctoringServerType.ZOOM,
                    null,
                    proctoringSettings.serverURL,
                    additionalZoomRoomData.join_url,
                    roomName,
                    subject,
                    sdkJWT,
                    sdkJWT,
                    sdkCredentials.accessToken,
                    sdkCredentials.clientId,
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

            final ClientConnectionData clientConnection = this.examSessionService
                    .getConnectionData(connectionToken)
                    .getOrThrow();

            // Note: since SEB Server version 1.5 we work only with SDKKey instead of AppKey which is deprecated
            final ClientCredentials sdkCredentials = new ClientCredentials(
                    proctoringSettings.sdkKey,
                    proctoringSettings.sdkSecret,
                    remoteProctoringRoom.joinKey);

            final String sdkJWT = this.createSDKJWT(
                    sdkCredentials,
                    expiryTimeforExam(proctoringSettings),
                    additionalZoomRoomData.meeting_id,
                    false);

            return new ProctoringRoomConnection(
                    ProctoringServerType.ZOOM,
                    connectionToken,
                    proctoringSettings.serverURL,
                    additionalZoomRoomData.join_url,
                    roomName,
                    subject,
                    sdkJWT,
                    sdkJWT,
                    sdkCredentials.accessToken,
                    sdkCredentials.clientId,
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

                this.deleteAdHocMeeting(
                        proctoringSettings,
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
                        true,
                        true)
                .onError(error -> log.error("Failed to send join instruction: {}", connectionToken, error));
    }

    private Result<NewRoom> createAdHocMeeting(
            final String roomName,
            final String subject,
            final int duration,
            final ProctoringServiceSettings proctoringSettings) {

        return Result.tryCatch(() -> {

            final ZoomRestTemplate zoomRestTemplate = getZoomRestTemplate(proctoringSettings);
            // First create a new user/host for the new room
            final ResponseEntity<String> createUser = zoomRestTemplate.createUser(roomName);

            final int statusCodeValue = createUser.getStatusCodeValue();
            if (statusCodeValue >= 400) {
                throw new RuntimeException("Failed to create new Zoom user for room: " + createUser.getBody());
            }

            final UserResponse userResponse = this.jsonMapper.readValue(
                    createUser.getBody(),
                    UserResponse.class);

            zoomRestTemplate.applyUserSettings(userResponse.id);

            // Then create new meeting with the ad-hoc user/host
            final CharSequence meetingPwd = UUID.randomUUID().toString().subSequence(0, 9);
            final ResponseEntity<String> createMeeting = zoomRestTemplate.createMeeting(
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
            final Long meetingId,
            final String userId) {

        return Result.tryCatch(() -> {

            final ZoomRestTemplate zoomRestTemplate = getZoomRestTemplate(proctoringSettings);
            zoomRestTemplate.deleteMeeting(meetingId);
            zoomRestTemplate.deleteUser(userId);

        });
    }

    private String createSDKJWT(
            final ClientCredentials sdkCredentials,
            final Long expTime,
            final long meetingNumber,
            final boolean host) {

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
            final String sdkKey = sdkCredentials.clientIdAsString();
            final SDKJWTPayload sdkjwtPayload = new SDKJWTPayload(
                    sdkKey,
                    sdkKey,
                    meetingNumber,
                    host ? 1 : 0,
                    secondsNow,
                    expTime,
                    expTime);

            final String jwtPayload = this.jsonMapper
                    .writeValueAsString(sdkjwtPayload)
                    .replaceAll(" ", "")
                    .replaceAll("\n", "");

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

    private long expiryTimeforExam(final ProctoringServiceSettings examProctoring) {

        final long nowInSeconds = Utils.getSecondsNow();
        final long nowPlusOneDayInSeconds = nowInSeconds + this.tokenExpirySeconds;

        // NOTE: It seems that since the update of web sdk to SDKToken to 1.7.0, the max is new + one day
        return nowPlusOneDayInSeconds - 10;
    }

    @Override
    public synchronized void clearRestTemplateCache(final Long examId) {
        this.restTemplatesCache.remove(examId);
    }

    private final LinkedHashMap<Long, ZoomRestTemplate> restTemplatesCache = new LinkedHashMap<>();

    private synchronized ZoomRestTemplate getZoomRestTemplate(final ProctoringServiceSettings proctoringSettings) {
        if (!this.restTemplatesCache.containsKey(proctoringSettings.examId)) {
            this.restTemplatesCache.put(
                    proctoringSettings.examId,
                    createNewRestTemplate(proctoringSettings));
        } else {
            final ZoomRestTemplate zoomRestTemplate = this.restTemplatesCache.get(proctoringSettings.examId);
            if (!zoomRestTemplate.isValid(proctoringSettings)) {
                this.restTemplatesCache.remove(proctoringSettings.examId);
                this.restTemplatesCache.put(
                        proctoringSettings.examId,
                        createNewRestTemplate(proctoringSettings));
            }
        }

        if (this.restTemplatesCache.size() > 5) {
            final Long toRemove = this.restTemplatesCache.keySet().iterator().next();
            if (!Objects.equals(proctoringSettings.examId, toRemove)) {
                this.restTemplatesCache.remove(toRemove);
            }
        }

        return this.restTemplatesCache.get(proctoringSettings.examId);
    }

    private ZoomRestTemplate createNewRestTemplate(final ProctoringServiceSettings proctoringSettings) {
        return new OAuthZoomRestTemplate(this, proctoringSettings);
    }

    private static abstract class ZoomRestTemplate {

        protected static final int LIZENSED_USER = 2;
        protected static final String API_TEST_ENDPOINT = "v2/users";
        protected static final String API_CREATE_USER_ENDPOINT = "v2/users";
        protected static final String API_APPLY_USER_SETTINGS_ENDPOINT = "v2/users/{userId}/settings";
        protected static final String API_DELETE_USER_ENDPOINT = "v2/users/{userid}?action=delete";
        protected static final String API_USER_CUST_CREATE = "custCreate";
        protected static final String API_ZOOM_ROOM_USER = "SEBProctoringRoomUser";
        protected static final String API_CREATE_MEETING_ENDPOINT = "v2/users/{userid}/meetings";
        protected static final String API_DELETE_MEETING_ENDPOINT = "v2/meetings/{meetingid}";
        protected static final String API_END_MEETING_ENDPOINT = "v2/meetings/{meetingid}/status";

        protected final ZoomProctoringService zoomProctoringService;
        protected final CircuitBreaker<ResponseEntity<String>> circuitBreaker;
        protected final ProctoringServiceSettings proctoringSettings;

        protected ClientCredentials credentials;
        protected RestTemplate restTemplate;

        public ZoomRestTemplate(
                final ZoomProctoringService zoomProctoringService,
                final ProctoringServiceSettings proctoringSettings) {

            this.zoomProctoringService = zoomProctoringService;
            this.circuitBreaker = zoomProctoringService.asyncService.createCircuitBreaker(
                    2,
                    10 * Constants.SECOND_IN_MILLIS,
                    10 * Constants.SECOND_IN_MILLIS);
            this.proctoringSettings = proctoringSettings;
            initConnection();
        }

        protected abstract void initConnection();

        protected abstract HttpHeaders getHeaders();

        boolean isValid(final ProctoringServiceSettings proctoringSettings) {
            return Objects.equals(proctoringSettings.serverURL, this.proctoringSettings.serverURL) &&
                    Objects.equals(proctoringSettings.appKey, this.proctoringSettings.appKey) &&
                    Objects.equals(proctoringSettings.accountId, this.proctoringSettings.accountId) &&
                    Objects.equals(proctoringSettings.clientId, this.proctoringSettings.clientId);
        }

        public ResponseEntity<String> testServiceConnection() {
            try {

                final String url = UriComponentsBuilder
                        .fromUriString(this.proctoringSettings.serverURL)
                        .path(API_TEST_ENDPOINT)
                        .queryParam("status", "active")
                        .queryParam("page_size", "10")
                        .queryParam("page_number", "1")
                        .queryParam("data_type", "Json")
                        .build()
                        .toUriString();
                return exchange(url, HttpMethod.GET);

            } catch (final Exception e) {
                log.error("Failed to test zoom service connection: ", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        public ResponseEntity<String> createUser(final String roomName) {

            try {

                final String url = UriComponentsBuilder
                        .fromUriString(this.proctoringSettings.serverURL)
                        .path(API_CREATE_USER_ENDPOINT)
                        .toUriString();
                final String host = new URL(this.proctoringSettings.serverURL).getHost();
                final CreateUserRequest createUserRequest = new CreateUserRequest(
                        API_USER_CUST_CREATE,
                        new CreateUserRequest.UserInfo(
                                roomName + "@" + host,
                                LIZENSED_USER,
                                roomName,
                                API_ZOOM_ROOM_USER));

                final String body = this.zoomProctoringService.jsonMapper.writeValueAsString(createUserRequest);
                final HttpHeaders headers = getHeaders();
                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                return exchange(url, HttpMethod.POST, body, headers);

            } catch (final Exception e) {
                log.error("Failed to create Zoom ad-hoc user for room: {}", roomName, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        public ResponseEntity<String> applyUserSettings(final String userId) {
            try {
                final String url = UriComponentsBuilder
                        .fromUriString(this.proctoringSettings.serverURL)
                        .path(API_APPLY_USER_SETTINGS_ENDPOINT)
                        .buildAndExpand(userId)
                        .normalize()
                        .toUriString();

                final ApplyUserSettingsRequest applySettingsRequest = new ApplyUserSettingsRequest();
                final String body = this.zoomProctoringService.jsonMapper.writeValueAsString(applySettingsRequest);
                final HttpHeaders headers = getHeaders();

                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                final ResponseEntity<String> exchange = exchange(url, HttpMethod.PATCH, body, headers);
                final int statusCodeValue = exchange.getStatusCodeValue();
                if (statusCodeValue >= 400) {
                    log.warn("Failed to apply user settings for Zoom user: {} response: {}", userId, exchange);
                }

                return exchange;
            } catch (final Exception e) {
                log.error("Failed to apply user settings for Zoom user: {}", userId, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        public ResponseEntity<String> createMeeting(
                final String userId,
                final String topic,
                final int duration,
                final CharSequence password,
                final boolean waitingRoom) {

            try {

                final String url = UriComponentsBuilder
                        .fromUriString(this.proctoringSettings.serverURL)
                        .path(API_CREATE_MEETING_ENDPOINT)
                        .buildAndExpand(userId)
                        .toUriString();

                final CreateMeetingRequest createRoomRequest = new CreateMeetingRequest(
                        topic,
                        duration,
                        password,
                        waitingRoom);

                final String body = this.zoomProctoringService.jsonMapper.writeValueAsString(createRoomRequest);
                final HttpHeaders headers = getHeaders();
                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                return exchange(url, HttpMethod.POST, body, headers);

            } catch (final Exception e) {
                log.error("Failed to create Zoom ad-hoc meeting: {}", topic, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        public ResponseEntity<String> deleteMeeting(final Long meetingId) {

            // try to set meeting status to ended first
            try {

                final String url = UriComponentsBuilder
                        .fromUriString(this.proctoringSettings.serverURL)
                        .path(API_END_MEETING_ENDPOINT)
                        .buildAndExpand(meetingId)
                        .toUriString();

                final HttpHeaders headers = getHeaders();
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
                        .fromUriString(this.proctoringSettings.serverURL)
                        .path(API_DELETE_MEETING_ENDPOINT)
                        .buildAndExpand(meetingId)
                        .toUriString();

                return exchange(url, HttpMethod.DELETE);

            } catch (final Exception e) {
                log.warn("Failed to delete Zoom ad-hoc meeting: {} cause: {} / {}",
                        meetingId,
                        e.getMessage(),
                        (e.getCause() != null) ? e.getCause().getMessage() : Constants.EMPTY_NOTE);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        public ResponseEntity<String> deleteUser(final String userId) {

            try {
                final String url = UriComponentsBuilder
                        .fromUriString(this.proctoringSettings.serverURL)
                        .path(API_DELETE_USER_ENDPOINT)
                        .buildAndExpand(userId)
                        .normalize()
                        .toUriString();

                return exchange(url, HttpMethod.DELETE);

            } catch (final Exception e) {
                log.error("Failed to delete Zoom ad-hoc user with id: {} cause: {} / {}",
                        userId,
                        e.getMessage(),
                        (e.getCause() != null) ? e.getCause().getMessage() : Constants.EMPTY_NOTE);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        private ResponseEntity<String> exchange(
                final String url,
                final HttpMethod method) {

            return exchange(url, method, null, getHeaders());
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

    private final static class OAuthZoomRestTemplate extends ZoomRestTemplate {

        private BaseOAuth2ProtectedResourceDetails resource;

        public OAuthZoomRestTemplate(
                final ZoomProctoringService zoomProctoringService,
                final ProctoringServiceSettings proctoringSettings) {

            super(zoomProctoringService, proctoringSettings);
        }

        @Override
        protected void initConnection() {

            if (this.resource == null) {

                this.credentials = new ClientCredentials(
                        this.proctoringSettings.clientId,
                        this.proctoringSettings.clientSecret);

                final CharSequence decryptedSecret = this.zoomProctoringService.cryptor
                        .decrypt(this.credentials.secret)
                        .getOrThrow();

                this.resource = new ClientCredentialsResourceDetails();
                this.resource.setAccessTokenUri(this.proctoringSettings.serverURL + "/oauth/token");
                this.resource.setClientId(this.credentials.clientIdAsString());
                this.resource.setClientSecret(decryptedSecret.toString());
                this.resource.setGrantType("account_credentials");
                this.resource.setId(this.proctoringSettings.accountId);

                final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
                final HttpClient httpClient = HttpClientBuilder.create()
                        .disableCookieManagement()
                        .useSystemProperties()
                        .build();
                requestFactory.setHttpClient(httpClient);
                final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(this.resource);
                oAuth2RestTemplate.setRequestFactory(requestFactory);
                oAuth2RestTemplate.setAccessTokenProvider(new ZoomCredentialsAccessTokenProvider());
                this.restTemplate = oAuth2RestTemplate;
            }
        }

        @Override
        boolean isValid(final ProctoringServiceSettings proctoringSettings) {
            final boolean valid = super.isValid(proctoringSettings);
            if (!valid) {
                return false;
            }

            try {
                final OAuth2RestTemplate oAuth2RestTemplate = (OAuth2RestTemplate) super.restTemplate;
                final OAuth2AccessToken accessToken = oAuth2RestTemplate.getAccessToken();
                if (accessToken == null) {
                    return false;
                }

                final boolean expired = accessToken.isExpired();
                if (expired) {
                    return false;
                }

                final int expiresIn = accessToken.getExpiresIn();
                if (expiresIn < 60) {
                    return false;
                }

                return true;
            } catch (final Exception e) {
                log.error("Failed to verify Zoom OAuth2RestTemplate status", e);
                return false;
            }
        }

        @Override
        public HttpHeaders getHeaders() {
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            return httpHeaders;
        }
    }

    private static final class ZoomCredentialsAccessTokenProvider extends OAuth2AccessTokenSupport
            implements AccessTokenProvider {

        @Override
        public boolean supportsResource(final OAuth2ProtectedResourceDetails resource) {
            return resource instanceof ClientCredentialsResourceDetails
                    && "account_credentials".equals(resource.getGrantType());
        }

        @Override
        public boolean supportsRefresh(final OAuth2ProtectedResourceDetails resource) {
            return true;
        }

        @Override
        public OAuth2AccessToken refreshAccessToken(final OAuth2ProtectedResourceDetails resource,
                final OAuth2RefreshToken refreshToken, final AccessTokenRequest request)
                throws UserRedirectRequiredException {

            return this.obtainAccessToken(resource, request);
        }

        @Override
        public OAuth2AccessToken obtainAccessToken(final OAuth2ProtectedResourceDetails details,
                final AccessTokenRequest request)
                throws UserRedirectRequiredException, AccessDeniedException, OAuth2AccessDeniedException {

            final ClientCredentialsResourceDetails resource = (ClientCredentialsResourceDetails) details;
            return retrieveToken(request, resource, getParametersForTokenRequest(resource), new HttpHeaders());
        }

        private MultiValueMap<String, String> getParametersForTokenRequest(
                final ClientCredentialsResourceDetails resource) {

            final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.set("grant_type", "account_credentials");
            form.set("account_id", resource.getId());

            if (resource.isScoped()) {

                final StringBuilder builder = new StringBuilder();
                final List<String> scope = resource.getScope();

                if (scope != null) {
                    final Iterator<String> scopeIt = scope.iterator();
                    while (scopeIt.hasNext()) {
                        builder.append(scopeIt.next());
                        if (scopeIt.hasNext()) {
                            builder.append(' ');
                        }
                    }
                }

                form.set("scope", builder.toString());
            }
            return form;
        }
    }

}
