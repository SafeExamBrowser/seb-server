/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.institution.impl;

import java.security.cert.Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityCheckResult;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey.KeyType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SecurityKeyRegistryDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.institution.SecurityKeyService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ClientConnectionDataInternal;
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
    private final Cryptor cryptor;

    public SecurityKeyServiceImpl(
            final ClientConnectionDAO clientConnectionDAO,
            final ExamSessionCacheService examSessionCacheService,
            final SecurityKeyRegistryDAO securityKeyRegistryDAO,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final Cryptor cryptor) {

        this.clientConnectionDAO = clientConnectionDAO;
        this.examSessionCacheService = examSessionCacheService;
        this.securityKeyRegistryDAO = securityKeyRegistryDAO;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.cryptor = cryptor;
    }

    @Override
    public Result<Collection<SecurityKey>> getPlainGrants(final Long institutionId, final Long examId) {
        return this.securityKeyRegistryDAO
                .getAll(institutionId, examId, null)
                .map(this::decryptAll);
    }

    @Override
    public Result<Collection<SecurityKey>> getPlainAppSignatureKeyGrants(final Long institutionId, final Long examId) {
        return this.securityKeyRegistryDAO
                .getAll(institutionId, examId, KeyType.APP_SIGNATURE_KEY)
                .map(this::decryptAll);
    }

    @Override
    public Result<SecurityKey> registerGlobalAppSignatureKey(
            final Long institutionId,
            final Long connectionId,
            final String tag) {

        if (log.isDebugEnabled()) {
            log.debug("Register app-signature-key global grant. ConnectionId: {} tag: {}",
                    connectionId,
                    tag);
        }

        return this.clientConnectionDAO.byPK(connectionId)
                .map(this::decryptStoredSignatureForConnection)
                .map(appSignatureKey -> this.securityKeyRegistryDAO.createNew(new SecurityKey(
                        null,
                        institutionId,
                        KeyType.APP_SIGNATURE_KEY,
                        this.cryptor.encrypt(appSignatureKey).getOrThrow(),
                        tag, null, null)).getOrThrow());
    }

    @Override
    public Result<SecurityKey> registerExamAppSignatureKey(
            final Long institutionId,
            final Long examId,
            final Long connectionId,
            final String tag) {

        if (log.isDebugEnabled()) {
            log.debug("Register app-signature-key exam grant. Exam: {} connectionId: {} tag: {}",
                    examId,
                    connectionId,
                    tag);
        }

        return this.clientConnectionDAO.byPK(connectionId)
                .map(this::decryptStoredSignatureForConnection)
                .map(appSignatureKey -> this.securityKeyRegistryDAO.createNew(new SecurityKey(
                        null,
                        institutionId,
                        KeyType.APP_SIGNATURE_KEY,
                        this.cryptor.encrypt(appSignatureKey).getOrThrow(),
                        tag, examId, null)).getOrThrow());
    }

    @Override
    public Result<SecurityCheckResult> applyAppSignatureCheck(
            final Long institutionId,
            final Long examId,
            final String connectionToken,
            final String appSignatureKey) {

        if (log.isDebugEnabled()) {
            log.debug("Apply app-signature-key check for connection: {}", connectionToken);
        }

        return this.securityKeyRegistryDAO
                .getAll(institutionId, examId, KeyType.APP_SIGNATURE_KEY)
                .map(all -> {
                    final String decryptedSignature = decryptSignature(examId, connectionToken, appSignatureKey);
                    final List<SecurityKey> matches = all.stream()
                            .map(this::decryptGrantedKey)
                            .filter(pair -> pair != null && Objects.equals(decryptedSignature, pair.a))
                            .map(Pair::getB)
                            .collect(Collectors.toList());

                    if (matches == null || matches.isEmpty()) {
                        return statisticalCheck(examId, decryptedSignature);
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

    @Override
    public boolean checkAppSignatureKey(
            final ClientConnection clientConnection,
            final String appSignatureKey) {

        try {

            // if already granted, return true
            if (clientConnection.isSecurityCheckGranted()) {
                return true;
            }

            String signature = appSignatureKey;
            if (StringUtils.isBlank(appSignatureKey)) {
                signature = getSignatureKeyForConnection(clientConnection);
            } else {
                saveSignatureKeyForConnection(clientConnection, appSignatureKey);
            }

            // check can only be applied if exam is known, no signature, no check, no grant
            if (clientConnection.examId == null || StringUtils.isBlank(signature)) {
                return false;
            }

            // if signature check is not enabled, skip
            if (!this.additionalAttributesDAO.getAdditionalAttribute(
                    EntityType.EXAM,
                    clientConnection.examId,
                    Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED)
                    .map(attr -> BooleanUtils.toBoolean(attr.getValue()))
                    .getOr(false).booleanValue()) {

                return false;
            }

            // apply check
            final Boolean grant = applyAppSignatureCheck(
                    clientConnection.institutionId,
                    clientConnection.examId,
                    clientConnection.connectionToken,
                    signature)
                            .map(SecurityCheckResult::hasAnyGrant)
                            .onError(error -> log.error("Failed to applyAppSignatureCheck: ", error))
                            .getOr(false);

            return grant;

        } catch (final Exception e) {
            log.error("Failed to apply App-Signature-Key check for clientConnection: {}", clientConnection, e);
            return false;
        }
    }

    @Override
    public Result<SecurityKey> getDecrypted(final SecurityKey key) {
        return this.cryptor.decrypt(key.key)
                .map(dKey -> new SecurityKey(
                        key.id,
                        key.institutionId,
                        key.keyType,
                        dKey,
                        key.tag,
                        key.examId,
                        key.examTemplateId));
    }

    @Override
    public void updateAppSignatureKeyGrants(final Long examId) {
        if (examId == null) {
            return;
        }

        try {

            this.clientConnectionDAO
                    .getConnectionTokens(examId)
                    .getOrThrow()
                    .stream()
                    .forEach(token -> {
                        final ClientConnectionDataInternal clientConnection =
                                this.examSessionCacheService.getClientConnection(token);
                        if (!clientConnection.clientConnection.isSecurityCheckGranted()) {
                            if (this.checkAppSignatureKey(clientConnection.clientConnection, null)) {
                                // now granted, update ClientConnection on DB level

                                if (log.isDebugEnabled()) {
                                    log.debug("Update app-signature-key grant for client connection: {}", token);
                                }

                                this.clientConnectionDAO
                                        .save(new ClientConnection(
                                                clientConnection.clientConnection.id, null,
                                                null, null, null, null, null, null, null, null,
                                                null, null, null, null, null, null, null, true))
                                        .onError(error -> log.error("Failed to save ClientConnection grant: ", error))
                                        .onSuccess(c -> this.examSessionCacheService.evictClientConnection(token));
                            }
                        }
                    });

        } catch (final Exception e) {
            log.error("Unexpected error while trying to update app-signature-key grants: ", e);
        }
    }

    @Override
    public Result<SecurityKey> registerSecurityKey(final SecurityKey key) {
        return this.encryptInternal(key)
                .flatMap(this.securityKeyRegistryDAO::createNew);
    }

    @Override
    public Result<EntityKey> deleteSecurityKeyGrant(final String keyModelId) {
        return Result.tryCatch(() -> Long.parseLong(keyModelId))
                .flatMap(this.securityKeyRegistryDAO::delete);
    }

    private Pair<String, SecurityKey> decryptGrantedKey(final SecurityKey key) {
        final Result<CharSequence> decrypt = this.cryptor.decrypt(key.key);
        if (decrypt.hasError()) {
            log.error("Failed to decrypt security key with internal secret: ", decrypt.getError());
            return null;
        }
        return new Pair<>(Utils.toString(decrypt.get()), key);
    }

    private String decryptSignature(
            final Long examId,
            final String connectionToken,
            final String appSignatureKey) {

        if (examId != null) {
            // when exam is available check if signature is encrypted with certificate
            return this.additionalAttributesDAO.getAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CERT_ALIAS)
                    .map(rec -> decryptSignatureWithCertificate(rec.getValue(), appSignatureKey))
                    .onErrorDo(error -> {
                        if (log.isDebugEnabled()) {
                            log.debug(
                                    "No Cert for encryption found for exam: {} cause: {}",
                                    examId,
                                    error.getMessage());
                        }
                        return decryptSignatureWithConnectionToken(connectionToken, appSignatureKey);
                    })
                    .getOrThrow();
        } else {
            return decryptSignatureWithConnectionToken(connectionToken, appSignatureKey);
        }
    }

    private SecurityCheckResult statisticalCheck(
            final Long examId,
            final String decryptedSignature) {

        System.out.println("****************** statisticalCheck: " + decryptedSignature);

        // if there is no exam known yet, no statistical check can be applied
        if (examId == null) {
            return SecurityCheckResult.NO_GRANT;
        }

        try {

            // TODO if cert encryption is available check if exam has defined cert for decryption
            final Certificate cert = null;

            final int matches = this.clientConnectionDAO
                    .getAllActiveConnectionTokens(examId)
                    .map(tokens -> tokens.stream()
                            .map(this.examSessionCacheService::getClientConnection)
                            .filter(cc -> matchOtherClientConnection(cc.clientConnection, decryptedSignature, cert))
                            .count())
                    .getOr(0l)
                    .intValue();

            if (matches <= 0) {
                return SecurityCheckResult.NO_GRANT;
            } else {
                return new SecurityCheckResult(false, false, matches > getStatisticalGrantThreshold(examId));
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to apply statistical app signature key check: ", e);
            return SecurityCheckResult.NO_GRANT;
        }
    }

    private boolean matchOtherClientConnection(
            final ClientConnection cc,
            final String decryptedSignature,
            final Certificate cert) {

        try {

            if (cert != null) {
                return false; // NOTE: not supported yet
            }

            return Objects.equals(
                    decryptedSignature,
                    decryptStoredSignatureForConnection(cc));

        } catch (final Exception e) {
            log.warn("Failed to get and decrypt app signature key for client connection: {}", cc, e);
            return false;
        }
    }

    private String decryptStoredSignatureForConnection(final ClientConnection cc) {
        final String signatureKey = getSignatureKeyForConnection(cc);
        if (StringUtils.isBlank(signatureKey)) {
            return null;
        }

        return decryptSignatureWithConnectionToken(cc.connectionToken, signatureKey);
    }

    private void saveSignatureKeyForConnection(final ClientConnection clientConnection, final String appSignatureKey) {
        this.additionalAttributesDAO
                .saveAdditionalAttribute(
                        EntityType.CLIENT_CONNECTION,
                        clientConnection.id,
                        ADDITIONAL_ATTR_APP_SIGNATURE_KEY,
                        appSignatureKey)
                .onError(error -> log.error(
                        "Failed to store App-Signature-Key for clientConnection: {}",
                        clientConnection, error));
    }

    private String getSignatureKeyForConnection(final Long connectionId) {
        return this.additionalAttributesDAO
                .getAdditionalAttribute(
                        EntityType.CLIENT_CONNECTION,
                        connectionId,
                        ADDITIONAL_ATTR_APP_SIGNATURE_KEY)
                .map(AdditionalAttributeRecord::getValue)
                .getOr(null);
    }

    private String getSignatureKeyForConnection(final ClientConnection clientConnection) {
        return getSignatureKeyForConnection(clientConnection.id);
    }

    private String decryptSignatureWithConnectionToken(
            final String connectionToken,
            final String appSignatureKey) {
        return Cryptor.decrypt(appSignatureKey, connectionToken).get().toString();
    }

    private String decryptSignatureWithCertificate(
            final String alias,
            final String appSignatureKey) {

        throw new UnsupportedOperationException("Currently not supported");
    }

    private int getStatisticalGrantThreshold(final Long examId) {
        return this.additionalAttributesDAO
                .getAdditionalAttribute(
                        EntityType.EXAM,
                        examId,
                        Exam.ADDITIONAL_ATTR_STATISTICAL_GRANT_COUNT_THRESHOLD)
                .map(attr -> Integer.valueOf(attr.getValue()))
                .getOr(1);
    }

    private Collection<SecurityKey> decryptAll(final Collection<SecurityKey> all) {
        return all.stream()
                .map(this::getDecrypted)
                .filter(Result::hasValue)
                .map(Result::get)
                .collect(Collectors.toList());
    }

    private Result<SecurityKey> encryptInternal(final SecurityKey key) {
        return Result.tryCatch(() -> new SecurityKey(
                key.id,
                key.institutionId,
                key.keyType,
                Utils.toString(this.cryptor.encrypt(key.key).getOrThrow()),
                key.tag,
                key.examId,
                key.examTemplateId));
    }

}
