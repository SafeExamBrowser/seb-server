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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.RemoteProctoringRoomDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;

@Lazy
@Service
@WebServiceProfile
public class ExamJITSIProctoringService implements ExamProctoringService {

    private static final String JITSI_ACCESS_TOKEN_HEADER =
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private static final String JITSI_ACCESS_TOKEN_PAYLOAD =
            "{\"context\":{\"user\":{\"name\":\"%s\"}},\"iss\":\"%s\",\"aud\":\"%s\",\"sub\":\"%s\",\"room\":\"%s\"%s%s}";

    private final RemoteProctoringRoomDAO remoteProctoringRoomDAO;
    private final AuthorizationService authorizationService;
    private final ExamSessionService examSessionService;
    private final Cryptor cryptor;

    protected ExamJITSIProctoringService(
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final AuthorizationService authorizationService,
            final ExamSessionService examSessionService,
            final Cryptor cryptor) {

        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.authorizationService = authorizationService;
        this.examSessionService = examSessionService;
        this.cryptor = cryptor;
    }

    @Override
    public ProctoringServerType getType() {
        return ProctoringServerType.JITSI_MEET;
    }

    @Override
    public Result<Boolean> testExamProctoring(final ProctoringSettings examProctoring) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<SEBProctoringConnection> createProctorPublicRoomConnection(
            final ProctoringSettings proctoringSettings,
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

    @Override
    public Result<SEBProctoringConnection> getClientExamCollectingRoomConnection(
            final ProctoringSettings proctoringSettings,
            final ClientConnection connection) {

        return Result.tryCatch(() -> {

            final String roomName = this.remoteProctoringRoomDAO
                    .getRoomName(connection.getRemoteProctoringRoomId())
                    .getOrThrow();

            return createProctoringConnection(
                    proctoringSettings.serverType,
                    null,
                    proctoringSettings.serverURL,
                    proctoringSettings.appKey,
                    proctoringSettings.getAppSecret(),
                    connection.userSessionId,
                    "seb-client",
                    roomName,
                    connection.userSessionId,
                    forExam(proctoringSettings),
                    false)
                            .getOrThrow();
        });
    }

    @Override
    public Result<SEBProctoringConnection> getClientExamCollectingRoomConnection(
            final ProctoringSettings proctoringSettings,
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

    @Override
    public Result<SEBProctoringConnection> getClientRoomConnection(
            final ProctoringSettings proctoringSettings,
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

    @Override
    public Result<SEBProctoringConnection> createProctoringConnection(
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

            return new SEBProctoringConnection(
                    proctoringServerType,
                    connectionToken,
                    host,
                    url,
                    roomName,
                    subject,
                    token);
        });
    }

    @Override
    public Result<String> createClientAccessToken(
            final ProctoringSettings proctoringSettings,
            final String connectionToken,
            final String roomName) {

        return Result.tryCatch(() -> {

            final ClientConnectionData connectionData = this.examSessionService
                    .getConnectionData(connectionToken)
                    .getOrThrow();

            final String host = UriComponentsBuilder.fromHttpUrl(proctoringSettings.serverURL)
                    .build()
                    .getHost();
            final CharSequence decryptedSecret = this.cryptor.decrypt(proctoringSettings.appSecret);

            return internalCreateAccessToken(
                    proctoringSettings.appKey,
                    decryptedSecret,
                    connectionData.clientConnection.userSessionId,
                    "seb-client",
                    roomName,
                    forExam(proctoringSettings),
                    host,
                    false);
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

    private long forExam(final ProctoringSettings examProctoring) {
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
