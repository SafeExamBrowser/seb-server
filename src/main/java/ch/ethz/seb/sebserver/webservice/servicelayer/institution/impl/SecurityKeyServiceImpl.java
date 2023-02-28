/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.institution.impl;

import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.institution.AppSignatureKeyInfo;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityCheckResult;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey.KeyType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SecurityKeyRegistryDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.institution.SecurityKeyService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;

@Lazy
@Service
@WebServiceProfile
public class SecurityKeyServiceImpl implements SecurityKeyService {

    private static final Logger log = LoggerFactory.getLogger(SecurityKeyServiceImpl.class);

    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamSessionCacheService examSessionCacheService;
    private final SecurityKeyRegistryDAO securityKeyRegistryDAO;
    private final AdditionalAttributesDAO additionalAttributesDAO;

    public SecurityKeyServiceImpl(
            final ClientConnectionDAO clientConnectionDAO,
            final ExamSessionCacheService examSessionCacheService,
            final SecurityKeyRegistryDAO securityKeyRegistryDAO,
            final AdditionalAttributesDAO additionalAttributesDAO) {

        this.clientConnectionDAO = clientConnectionDAO;
        this.examSessionCacheService = examSessionCacheService;
        this.securityKeyRegistryDAO = securityKeyRegistryDAO;
        this.additionalAttributesDAO = additionalAttributesDAO;
    }

    @Override
    public Result<SecurityKey> getAppSignatureKey(final Long institutionId, final Long connectionId) {
        return this.clientConnectionDAO.byPK(connectionId)
                .map(connection -> new SecurityKey(
                        null,
                        institutionId,
                        KeyType.APP_SIGNATURE_KEY,
                        connection.ask,
                        connection.sebVersion,
                        null, null));
    }

    @Override
    public Result<Collection<AppSignatureKeyInfo>> getAppSignatureKeyInfo(final Long institutionId, final Long examId) {
        return Result.tryCatch(() -> {

            return this.clientConnectionDAO
                    .getsecurityKeyConnectionRecords(examId)
                    .getOrThrow()
                    .stream()
                    .reduce(
                            new HashMap<String, Map<Long, String>>(),
                            this::reduceAppSecKey,
                            Utils::<String, Map<Long, String>> mergeMap)
                    .entrySet()
                    .stream()
                    .map(m -> new AppSignatureKeyInfo(institutionId, examId, m.getKey(), m.getValue()))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public Result<Collection<SecurityKey>> getSecurityKeyEntries(
            final Long institutionId,
            final Long examId,
            final KeyType type) {

        return this.securityKeyRegistryDAO
                .getAll(institutionId, examId, type);
    }

    @Override
    public Result<SecurityKey> registerGlobalAppSignatureKey(
            final Long institutionId,
            final Long connectionId,
            final String tag) {

        if (StringUtils.isEmpty(tag)) {
            throw new FieldValidationException(
                    Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_TAG,
                    "securityKeyGrant:tag:mandatory");
        }

        if (log.isDebugEnabled()) {
            log.debug("Register app-signature-key global grant. ConnectionId: {} tag: {}",
                    connectionId,
                    tag);
        }

        return this.clientConnectionDAO.byPK(connectionId)
                .map(cc -> this.securityKeyRegistryDAO.createNew(new SecurityKey(
                        null,
                        institutionId,
                        KeyType.APP_SIGNATURE_KEY,
                        cc.ask,
                        tag, null, null)).getOrThrow());
    }

    @Override
    public Result<SecurityKey> grantAppSignatureKey(
            final Long institutionId,
            final Long examId,
            final Long connectionId,
            final String tag) {

        if (StringUtils.isEmpty(tag)) {
            throw new FieldValidationException(
                    Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_TAG,
                    "securityKeyGrant:tag:notNull");
        }

        if (log.isDebugEnabled()) {
            log.debug("Register app-signature-key exam grant. Exam: {} connectionId: {} tag: {}",
                    examId,
                    connectionId,
                    tag);
        }

        return this.clientConnectionDAO.byPK(connectionId)
                .map(cc -> this.securityKeyRegistryDAO.createNew(new SecurityKey(
                        null,
                        institutionId,
                        KeyType.APP_SIGNATURE_KEY,
                        cc.ask,
                        tag, examId, null)).getOrThrow());
    }

    @Override
    public Result<String> getAppSignatureKeyHash(
            final String appSignatureKey,
            final String connectionToken,
            final CharSequence salt) {

        if (StringUtils.isBlank(appSignatureKey)) {
            return Result.ofEmpty();
        }

        // TODO if certificate encryption is available check if exam has defined certificate for decryption

        return Cryptor
                .decrypt(appSignatureKey + salt, connectionToken)
                .onErrorDo(error -> {

                    log.warn(
                            "Failed to decrypt ASK with added salt value. Try to decrypt without added salt. Error: {}",
                            error.getMessage());

                    return Cryptor
                            .decrypt(appSignatureKey, connectionToken)
                            .getOrThrow();
                })
                .map(signature -> createSignatureHash(signature));

    }

    @Override
    public void updateAppSignatureKeyGrant(final ClientConnectionRecord record) {
        try {
            final Byte securityCheckGranted = record.getSecurityCheckGranted();
            if (securityCheckGranted == null || securityCheckGranted == Constants.BYTE_FALSE) {
                final String token = record.getConnectionToken();
                if (applyAppSignatureCheck(
                        record.getInstitutionId(),
                        record.getExamId(),
                        token,
                        record.getAsk())
                                .getOrThrow()
                                .hasAnyGrant()) {
                    // now granted, update ClientConnection on DB level
                    if (log.isDebugEnabled()) {
                        log.debug("Update app-signature-key grant for client connection: {}", token);
                    }

                    saveSecurityCheckState(record, true);
                } else if (securityCheckGranted == null) {
                    saveSecurityCheckState(record, false);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to updateAppSignatureKeyGrants for connection: {}", record, e);
        }
    }

    @Override
    public Result<EntityKey> deleteSecurityKeyGrant(final Long keyId) {
        return Result.tryCatch(() -> {
            final SecurityKey key = this.securityKeyRegistryDAO.byPK(keyId).getOrThrow();

            final String grantedKeyHash = String.valueOf(key);
            this.securityKeyRegistryDAO.delete(keyId).getOrThrow();
            this.clientConnectionDAO.getsecurityKeyConnectionRecords(key.examId)
                    .getOrThrow()
                    .stream()
                    .filter(rec -> ConnectionStatus.ACTIVE.name().equals(rec.getStatus()))
                    .forEach(rec -> {
                        try {
                            if (Utils.isEqualsWithEmptyCheck(grantedKeyHash, rec.getAsk())) {
                                // we have to re-check here
                                final boolean granted = this.applyAppSignatureCheck(
                                        rec.getInstitutionId(),
                                        rec.getExamId(),
                                        rec.getConnectionToken(),
                                        grantedKeyHash).getOrThrow().hasAnyGrant();
                                final Boolean grantedBefore = Utils.fromByte(rec.getSecurityCheckGranted());
                                if (granted != grantedBefore) {
                                    // update grant
                                    this.clientConnectionDAO
                                            .saveSecurityCheckStatus(rec.getId(), granted)
                                            .onError(error -> log.error(
                                                    "Failed to save security key grant for SEB connection: {}",
                                                    rec.getId(),
                                                    error));
                                    this.examSessionCacheService.evictClientConnection(rec.getConnectionToken());
                                }
                            }
                        } catch (final Exception e) {
                            log.error("Failed to update security key grant for connection on deletion -> {}", rec, e);
                        }
                    });

            return key.getEntityKey();
        });

    }

    private Result<SecurityCheckResult> applyAppSignatureCheck(
            final Long institutionId,
            final Long examId,
            final String connectionToken,
            final String hashedSignatureKey) {

        if (log.isDebugEnabled()) {
            log.debug("Apply app-signature-key check for connection: {}", connectionToken);
        }

        return this.securityKeyRegistryDAO
                .getAll(institutionId, examId, KeyType.APP_SIGNATURE_KEY)
                .map(all -> {
                    final List<SecurityKey> matches = all
                            .stream()
                            .filter(key -> Utils.isEqualsWithEmptyCheck(String.valueOf(key.key), hashedSignatureKey))
                            .collect(Collectors.toList());

                    if (matches == null || matches.isEmpty()) {
                        return numericalCheck(examId, hashedSignatureKey);
                    } else {
                        return new SecurityCheckResult(
                                matches.stream()
                                        .filter(key -> key.examId != null)
                                        .findFirst()
                                        .isPresent(),
                                matches.stream()
                                        .filter(key -> key.examId == null)
                                        .findFirst()
                                        .isPresent(),
                                false);
                    }
                });
    }

    private SecurityCheckResult numericalCheck(
            final Long examId,
            final String hashedSignature) {

        if (log.isDebugEnabled()) {
            log.debug("Apply numerical security check update for exam {}", examId);
        }

        // if there is no exam known yet, no numerical check can be applied
        if (examId == null) {
            return SecurityCheckResult.NO_GRANT;
        }

        try {

            final int numericalTrustThreshold = getNumericalTrustThreshold(examId);
            final Long matches = this.clientConnectionDAO
                    .countSignatureHashes(examId, hashedSignature)
                    .getOr(0L);

            if (matches <= 0) {
                return SecurityCheckResult.NO_GRANT;
            } else {
                return new SecurityCheckResult(false, false, matches > numericalTrustThreshold);
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to apply numerical app signature key check: ", e);
            return SecurityCheckResult.NO_GRANT;
        }
    }

    private int getNumericalTrustThreshold(final Long examId) {
        // try to ger from running exam.
        final Exam runningExam = this.examSessionCacheService.getRunningExam(examId);
        if (runningExam != null) {
            final String threshold = runningExam.getAdditionalAttribute(
                    Exam.ADDITIONAL_ATTR_NUMERICAL_TRUST_THRESHOLD);

            if (StringUtils.isNotBlank(threshold)) {
                try {
                    return Integer.parseInt(threshold);
                } catch (final Exception e) {
                    log.warn("Failed to parse numerical trust threshold");
                }
            }
        }

        // if not possible get it from storage
        return this.additionalAttributesDAO
                .getAdditionalAttribute(
                        EntityType.EXAM,
                        examId,
                        Exam.ADDITIONAL_ATTR_NUMERICAL_TRUST_THRESHOLD)
                .map(attr -> Integer.valueOf(attr.getValue()))
                .getOr(1);
    }

    private String createSignatureHash(final CharSequence signature) {
        try {
            final MessageDigest hasher = MessageDigest.getInstance(Constants.SHA_256);
            hasher.update(Utils.toByteArray(signature));
            final String signatureHash = Hex.toHexString(hasher.digest());
            return signatureHash;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Map<Long, String>> reduceAppSecKey(
            final Map<String, Map<Long, String>> m,
            final ClientConnectionRecord rec) {

        final Map<Long, String> mapping = m.computeIfAbsent(rec.getAsk(), s -> new HashMap<>());

        mapping.put(rec.getId(), rec.getExamUserSessionId());
        return m;
    }

    private void saveSecurityCheckState(final ClientConnectionRecord record, final Boolean checkStatus) {
        this.clientConnectionDAO
                .saveSecurityCheckStatus(record.getId(), checkStatus)
                .onError(error -> log.error("Failed to save ClientConnection grant: ",
                        error))
                .onSuccess(c -> this.examSessionCacheService.evictClientConnection(record.getConnectionToken()));
    }

}
