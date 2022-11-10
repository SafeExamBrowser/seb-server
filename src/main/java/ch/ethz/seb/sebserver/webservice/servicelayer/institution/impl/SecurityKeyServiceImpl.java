/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.institution.impl;

import java.security.cert.Certificate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityCheckResult;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey.EncryptionType;
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
    public Result<SecurityKey> registerGlobalAppSignatureKey(
            final Long institutionId,
            final Long connectionId,
            final String tag) {

        return this.clientConnectionDAO.byPK(connectionId)
                .map(this::decryptStoredSignatureForConnection)
                .map(appSignatureKey -> this.securityKeyRegistryDAO.createNew(new SecurityKey(
                        null,
                        institutionId,
                        KeyType.APP_SIGNATURE_KEY,
                        this.cryptor.encrypt(appSignatureKey).getOrThrow(),
                        tag, null, null, EncryptionType.PWD_INTERNAL)).getOrThrow());
    }

    @Override
    public Result<SecurityKey> registerExamAppSignatureKey(
            final Long institutionId,
            final Long examId,
            final Long connectionId,
            final String tag) {

        return this.clientConnectionDAO.byPK(connectionId)
                .map(this::decryptStoredSignatureForConnection)
                .map(appSignatureKey -> this.securityKeyRegistryDAO.createNew(new SecurityKey(
                        null,
                        institutionId,
                        KeyType.APP_SIGNATURE_KEY,
                        this.cryptor.encrypt(appSignatureKey).getOrThrow(),
                        tag, examId, null, EncryptionType.PWD_INTERNAL)).getOrThrow());
    }

    @Override
    public Result<SecurityCheckResult> applyAppSignatureCheck(
            final Long institutionId,
            final Long examId,
            final String connectionToken,
            final String appSignatureKey) {

        return this.securityKeyRegistryDAO
                .getAll(institutionId, examId, KeyType.APP_SIGNATURE_KEY)
                .map(all -> {
                    final String decryptedSignature = decryptSignature(examId, connectionToken, appSignatureKey);
                    final List<SecurityKey> matches = all.stream()
                            .map(this::decryptKey)
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

            // no signature, no check, no grant
            if (StringUtils.isBlank(signature)) {
                return false;
            }

            // apply check
            return applyAppSignatureCheck(
                    clientConnection.institutionId,
                    clientConnection.examId,
                    clientConnection.connectionToken,
                    appSignatureKey)
                            .map(SecurityCheckResult::hasAnyGrant)
                            .onError(error -> log.error("Failed to applyAppSignatureCheck: ", error))
                            .getOr(false);

        } catch (final Exception e) {
            log.error("Failed to apply App-Signature-Key check for clientConnection: {}", clientConnection, e);
            return false;
        }
    }

    private Pair<String, SecurityKey> decryptKey(final SecurityKey key) {
        if (key.encryptionType != EncryptionType.PWD_INTERNAL) {
            log.warn("Only internal encrypted keys can be decrypted here. Skip key: {}", key);
            return null;
        }

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
                    ADDITIONAL_ATTR_SIGNATURE_KEY_CERT_ALIAS)
                    .map(rec -> decryptSignatureWithCertificate(rec.getValue(), appSignatureKey))
                    .onErrorDo(error -> {
                        log.warn("Failed to decrypt with cert. Try with token: ", error);
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

        // if there is no exam known yet, no statistical check can be applied
        if (examId == null) {
            return SecurityCheckResult.NO_GRANT;
        }

        try {

            // TODO if cert encryption is available check if exam has defined cert for decryption
            final Certificate cert = null;

            final int matches = this.clientConnectionDAO.getAllActiveConnectionTokens(examId)
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
                        ADDITIONAL_ATTR_STATISTICAL_GRANT_COUNT_THRESHOLD)
                .map(attr -> Integer.valueOf(attr.getValue()))
                .getOr(1);

    }

    @Override
    public void updateAppSignatureKeyGrants(final Long examId) {
        // TODO Go through all client connections of the exam and update those that has no grant yet
        //      for all client connections that do not have a grant, make a check again and store if new granted.

    }

}
