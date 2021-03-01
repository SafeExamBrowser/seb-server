/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
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
import java.util.Map;

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

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
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
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;

@Lazy
@Service
@WebServiceProfile
public class ExamZOOMProctoringService implements ExamProctoringService {

    private static final Logger log = LoggerFactory.getLogger(ExamZOOMProctoringService.class);

    private static final String API_TEST_ENDPOINT = "v2/users?status=active&page_size=30&page_number=1&data_type=Json";
    private static final String TOKEN_ENCODE_ALG = "HmacSHA256";

    private static final String ZOOM_ACCESS_TOKEN_HEADER =
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String ZOOM_ACCESS_TOKEN_PAYLOAD =
            "{\"iss\":\"%s\",\"exp\":%s}";

    private final ExamSessionService examSessionService;
    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    private final Cryptor cryptor;
    private final AsyncService asyncService;
    private final JSONMapper jsonMapper;

    public ExamZOOMProctoringService(
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

                final String url = proctoringSettings.serverURL.endsWith(Constants.SLASH.toString())
                        ? proctoringSettings.serverURL + API_TEST_ENDPOINT
                        : proctoringSettings.serverURL + "/" + API_TEST_ENDPOINT;

                final ResponseEntity<String> result = new ZoomRestCallTemplate(proctoringSettings)
                        .callGET(url);

                if (result.getStatusCode() != HttpStatus.OK) {
                    throw new APIMessageException(
                            APIMessage.ErrorMessage.BINDING_ERROR,
                            String.valueOf(result.getStatusCode()));
                } else {
                    System.out.println(result.getBody());
                }
            } catch (final Exception e) {
                throw new APIMessageException(APIMessage.ErrorMessage.BINDING_ERROR, e.getMessage());
            }

            return true;
        });
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
    public Result<ProctoringRoomConnection> sendJoinRoomToClients(
            final ProctoringServiceSettings proctoringSettings,
            final Collection<String> clientConnectionTokens,
            final String roomName, final String subject) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Void> sendJoinCollectingRoomToClients(
            final ProctoringServiceSettings proctoringSettings,
            final Collection<String> clientConnectionTokens) {

        // TODO Auto-generated method stub
        return null;
    }

    protected String createJWT(
            final String appKey,
            final CharSequence appSecret,
            final Long expTime) throws NoSuchAlgorithmException, InvalidKeyException {

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
    }

    protected String createPayload(
            final String clientKey,
            final Long expTime) {

        return String.format(
                ZOOM_ACCESS_TOKEN_PAYLOAD.replaceAll(" ", "").replaceAll("\n", ""),
                clientKey,
                expTime);
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

    private class ZoomRestCallTemplate {

        final ProctoringServiceSettings proctoringSettings;
        final RestTemplate restTemplate;
        final HttpHeaders httpHeaders;

        private final CircuitBreaker<ResponseEntity<String>> circuitBreaker;

        public ZoomRestCallTemplate(final ProctoringServiceSettings proctoringSettings)
                throws InvalidKeyException, NoSuchAlgorithmException {

            this.proctoringSettings = proctoringSettings;
            this.restTemplate = new RestTemplate(ExamZOOMProctoringService.this.clientHttpRequestFactoryService
                    .getClientHttpRequestFactory()
                    .getOrThrow());

            final String jwt = createJWT(
                    proctoringSettings.appKey,
                    proctoringSettings.appSecret,
                    System.currentTimeMillis() + Constants.MINUTE_IN_MILLIS);

            this.httpHeaders = new HttpHeaders();
            this.httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
            this.httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            this.circuitBreaker = ExamZOOMProctoringService.this.asyncService.createCircuitBreaker(
                    2,
                    10 * Constants.SECOND_IN_MILLIS,
                    10 * Constants.SECOND_IN_MILLIS);
        }

        public ResponseEntity<String> callGET(final String url) {
            return exchange(null, url, null);
        }

        public ResponseEntity<String> callGET(final String url, final Map<String, ?> uriVariables) {
            return exchange(null, url, uriVariables);
        }

        private ResponseEntity<String> exchange(
                final Object body,
                final String url,
                final Map<String, ?> uriVariables) {

            final Result<ResponseEntity<String>> protectedRunResult = this.circuitBreaker.protectedRun(() -> {
                final HttpEntity<Object> httpEntity = (body != null)
                        ? new HttpEntity<>(body, this.httpHeaders)
                        : new HttpEntity<>(this.httpHeaders);

                final ResponseEntity<String> result = (uriVariables != null)
                        ? this.restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                httpEntity,
                                String.class,
                                uriVariables)
                        : this.restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                httpEntity,
                                String.class);

                if (result.getStatusCode() != HttpStatus.OK) {
                    log.warn("Zoom API call to {} respond not 200 -> {}", url, result.getStatusCode());
                }

                return result;
            });
            return protectedRunResult.getOrThrow();
        }
    }

}
