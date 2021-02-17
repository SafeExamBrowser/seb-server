/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.RemoteProctoringRoomDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;

@Lazy
@Service
@WebServiceProfile
public class ExamJITSIProctoringService implements ExamProctoringService {

    private static final Logger log = LoggerFactory.getLogger(ExamJITSIProctoringService.class);

    private static final String JITSI_ACCESS_TOKEN_HEADER =
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private static final String JITSI_ACCESS_TOKEN_PAYLOAD =
            "{\"context\":{\"user\":{\"name\":\"%s\"}},\"iss\":\"%s\",\"aud\":\"%s\",\"sub\":\"%s\",\"room\":\"%s\"%s%s}";

    private final RemoteProctoringRoomDAO remoteProctoringRoomDAO;
    private final AuthorizationService authorizationService;
    private final ExamSessionService examSessionService;
    private final SEBClientInstructionService sebClientInstructionService;
    private final Cryptor cryptor;

    protected ExamJITSIProctoringService(
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final AuthorizationService authorizationService,
            final ExamSessionService examSessionService,
            final SEBClientInstructionService sebClientInstructionService,
            final Cryptor cryptor) {

        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.authorizationService = authorizationService;
        this.examSessionService = examSessionService;
        this.sebClientInstructionService = sebClientInstructionService;
        this.cryptor = cryptor;
    }

    @Override
    public ProctoringServerType getType() {
        return ProctoringServerType.JITSI_MEET;
    }

    @Override
    public Result<Boolean> testExamProctoring(final ProctoringServiceSettings examProctoring) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<ProctoringRoomConnection> getProctorRoomConnection(
            final ProctoringServiceSettings proctoringSettings,
            final String roomName,
            final String subject) {

        return this.createProctorPublicRoomConnection(
                proctoringSettings,
                roomName,
                StringUtils.isNoneBlank(subject) ? subject : roomName);
    }

    @Override
    public Result<ProctoringRoomConnection> sendJoinRoomToClients(
            final ProctoringServiceSettings proctoringSettings,
            final Collection<String> clientConnectionTokens,
            final String roomName,
            final String subject) {

        return Result.tryCatch(() -> {
            clientConnectionTokens
                    .stream()
                    .forEach(connectionToken -> {
                        final ProctoringRoomConnection proctoringConnection =
                                getClientRoomConnection(
                                        proctoringSettings,
                                        connectionToken,
                                        verifyRoomName(roomName, connectionToken),
                                        (StringUtils.isNotBlank(subject)) ? subject : roomName)
                                                .onError(error -> log.error(
                                                        "Failed to get client room connection data for {} cause: {}",
                                                        connectionToken,
                                                        error.getMessage()))
                                                .get();
                        if (proctoringConnection != null) {
                            sendJoinInstruction(
                                    proctoringSettings.examId,
                                    connectionToken,
                                    proctoringConnection);
                        }
                    });

            return createProctorPublicRoomConnection(
                    proctoringSettings,
                    roomName,
                    (StringUtils.isNotBlank(subject)) ? subject : roomName)
                            .getOrThrow();
        });
    }

    @Override
    public Result<Void> sendJoinCollectingRoomToClients(
            final ProctoringServiceSettings proctoringSettings,
            final Collection<String> clientConnectionTokens) {

        return Result.tryCatch(() -> {
            clientConnectionTokens
                    .stream()
                    .forEach(connectionToken -> {
                        final ClientConnectionData clientConnection = this.examSessionService
                                .getConnectionData(connectionToken)
                                .getOrThrow();
                        final String roomName = this.remoteProctoringRoomDAO
                                .getRoomName(clientConnection.clientConnection.getRemoteProctoringRoomId())
                                .getOrThrow();

                        final ProctoringRoomConnection proctoringConnection = getClientExamCollectingRoomConnection(
                                proctoringSettings,
                                clientConnection.clientConnection.connectionToken,
                                roomName,
                                clientConnection.clientConnection.userSessionId)
                                        .getOrThrow();

                        sendJoinInstruction(
                                proctoringSettings.examId,
                                clientConnection.clientConnection.connectionToken,
                                proctoringConnection);
                    });
        });
    }

    private void sendJoinInstruction(
            final Long examId,
            final String connectionToken,
            final ProctoringRoomConnection proctoringConnection) {

        final Map<String, String> attributes = new HashMap<>();

        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.SERVICE_TYPE,
                ProctoringServiceSettings.ProctoringServerType.JITSI_MEET.name());
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.METHOD,
                ClientInstruction.ProctoringInstructionMethod.JOIN.name());
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.JITSI_URL,
                proctoringConnection.serverURL);
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.JITSI_ROOM,
                proctoringConnection.roomName);
        if (StringUtils.isNotBlank(proctoringConnection.subject)) {
            attributes.put(
                    ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.JITSI_ROOM_SUBJECT,
                    proctoringConnection.subject);
        }
        attributes.put(
                ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_PROCTORING.JITSI_TOKEN,
                proctoringConnection.accessToken);

        this.sebClientInstructionService.registerInstruction(
                examId,
                InstructionType.SEB_PROCTORING,
                attributes,
                connectionToken,
                true)
                .onError(error -> log.error("Failed to send join instruction: {}", connectionToken, error));
    }

    private Result<ProctoringRoomConnection> createProctorPublicRoomConnection(
            final ProctoringServiceSettings proctoringSettings,
            final String roomName,
            final String subject) {

        return Result.tryCatch(() -> {
            return createProctoringConnection(
                    proctoringSettings.serverType,
                    null,
                    proctoringSettings.serverURL,
                    proctoringSettings.appKey,
                    proctoringSettings.getAppSecret(),
                    this.authorizationService.getUserService().getCurrentUser().getUsername(),
                    "seb-server",
                    roomName,
                    subject,
                    forExam(proctoringSettings),
                    true)
                            .getOrThrow();
        });
    }

    private Result<ProctoringRoomConnection> getClientExamCollectingRoomConnection(
            final ProctoringServiceSettings proctoringSettings,
            final String connectionToken,
            final String roomName,
            final String subject) {

        return Result.tryCatch(() -> {
            final ClientConnectionData clientConnection = this.examSessionService
                    .getConnectionData(connectionToken)
                    .getOrThrow();

            return createProctoringConnection(
                    proctoringSettings.serverType,
                    null,
                    proctoringSettings.serverURL,
                    proctoringSettings.appKey,
                    proctoringSettings.getAppSecret(),
                    clientConnection.clientConnection.userSessionId,
                    "seb-client",
                    roomName,
                    subject,
                    forExam(proctoringSettings),
                    false)
                            .getOrThrow();
        });
    }

    private Result<ProctoringRoomConnection> getClientRoomConnection(
            final ProctoringServiceSettings proctoringSettings,
            final String connectionToken,
            final String roomName,
            final String subject) {

        return Result.tryCatch(() -> {
            final long expTime = forExam(proctoringSettings);

            final ClientConnectionData connectionData = this.examSessionService
                    .getConnectionData(connectionToken)
                    .getOrThrow();

            return createProctoringConnection(
                    proctoringSettings.serverType,
                    connectionToken,
                    proctoringSettings.serverURL,
                    proctoringSettings.appKey,
                    proctoringSettings.getAppSecret(),
                    connectionData.clientConnection.userSessionId,
                    "seb-client",
                    roomName,
                    subject,
                    expTime,
                    false)
                            .getOrThrow();
        });

    }

    protected Result<ProctoringRoomConnection> createProctoringConnection(
            final ProctoringServerType proctoringServerType,
            final String connectionToken,
            final String url,
            final String appKey,
            final CharSequence appSecret,
            final String clientName,
            final String clientKey,
            final String roomName,
            final String subject,
            final Long expTime,
            final boolean moderator) {

        return Result.tryCatch(() -> {

            final String host = UriComponentsBuilder.fromHttpUrl(url)
                    .build()
                    .getHost();

            final CharSequence decryptedSecret = this.cryptor.decrypt(appSecret);
            final String token = internalCreateAccessToken(
                    appKey,
                    decryptedSecret,
                    clientName,
                    clientKey,
                    roomName,
                    expTime,
                    host,
                    moderator);

            return new ProctoringRoomConnection(
                    proctoringServerType,
                    connectionToken,
                    host,
                    url,
                    roomName,
                    subject,
                    token);
        });
    }

    protected String internalCreateAccessToken(
            final String appKey,
            final CharSequence appSecret,
            final String clientName,
            final String clientKey,
            final String roomName,
            final Long expTime,
            final String host,
            final boolean moderator) throws NoSuchAlgorithmException, InvalidKeyException {

        final StringBuilder builder = new StringBuilder();
        final Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

        final String jwtHeaderPart = urlEncoder
                .encodeToString(JITSI_ACCESS_TOKEN_HEADER.getBytes(StandardCharsets.UTF_8));
        final String jwtPayload = createPayload(appKey, clientName, clientKey, roomName, expTime, host, moderator);
        final String jwtPayloadPart = urlEncoder
                .encodeToString(jwtPayload.getBytes(StandardCharsets.UTF_8));
        final String message = jwtHeaderPart + "." + jwtPayloadPart;

        final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        final SecretKeySpec secret_key =
                new SecretKeySpec(Utils.toByteArray(appSecret), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        final String hash = urlEncoder.encodeToString(sha256_HMAC.doFinal(Utils.toByteArray(message)));

        builder.append(message)
                .append(".")
                .append(hash);

        return builder.toString();
    }

    protected String createPayload(
            final String appKey,
            final String clientName,
            final String clientKey,
            final String roomName,
            final Long expTime,
            final String host,
            final boolean moderator) {

        final String jwtPayload = String.format(
                JITSI_ACCESS_TOKEN_PAYLOAD.replaceAll(" ", "").replaceAll("\n", ""),
                clientName,
                appKey,
                clientKey,
                host,
                roomName,
                (moderator)
                        ? ",\"moderator\":true"
                        : ",\"moderator\":false",
                (expTime != null)
                        ? String.format(",\"exp\":%s", String.valueOf(expTime))
                        : "");
        return jwtPayload;
    }

    private long forExam(final ProctoringServiceSettings examProctoring) {
        if (examProctoring.examId == null) {
            throw new IllegalStateException("Missing exam identifier from ExamProctoring data");
        }

        long expTime = System.currentTimeMillis() + Constants.DAY_IN_MILLIS;
        if (this.examSessionService.isExamRunning(examProctoring.examId)) {
            final Exam exam = this.examSessionService.getRunningExam(examProctoring.examId)
                    .getOrThrow();
            if (exam.endTime != null) {
                expTime = exam.endTime.getMillis();
            }
        }
        return expTime;
    }

}
