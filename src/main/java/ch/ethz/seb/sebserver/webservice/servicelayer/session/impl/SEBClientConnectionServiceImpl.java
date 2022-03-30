/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig.VDIType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.EventHandlingStrategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientConnectionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator.DistributedIndicatorValueService;
import ch.ethz.seb.sebserver.webservice.weblayer.api.APIConstraintViolationException;

@Lazy
@Service
@WebServiceProfile
public class SEBClientConnectionServiceImpl implements SEBClientConnectionService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientConnectionServiceImpl.class);

    private static final Predicate<ClientConnection> DISABLE_STATE_PREDICATE = ClientConnection
            .getStatusPredicate(
                    ConnectionStatus.UNDEFINED,
                    ConnectionStatus.CONNECTION_REQUESTED,
                    ConnectionStatus.AUTHENTICATED,
                    ConnectionStatus.CLOSED);

    private final ExamSessionService examSessionService;
    private final ExamSessionCacheService examSessionCacheService;
    private final CacheManager cacheManager;
    private final EventHandlingStrategy eventHandlingStrategy;
    private final ClientConnectionDAO clientConnectionDAO;
    private final SEBClientConfigDAO sebClientConfigDAO;
    private final SEBClientInstructionService sebInstructionService;
    private final ExamAdminService examAdminService;
    private final DistributedIndicatorValueService distributedPingCache;
    private final ClientIndicatorFactory clientIndicatorFactory;
    private final boolean isDistributedSetup;

    protected SEBClientConnectionServiceImpl(
            final ExamSessionService examSessionService,
            final EventHandlingStrategyFactory eventHandlingStrategyFactory,
            final SEBClientConfigDAO sebClientConfigDAO,
            final SEBClientInstructionService sebInstructionService,
            final ExamAdminService examAdminService,
            final DistributedIndicatorValueService distributedPingCache,
            final ClientIndicatorFactory clientIndicatorFactory) {

        this.examSessionService = examSessionService;
        this.examSessionCacheService = examSessionService.getExamSessionCacheService();
        this.cacheManager = examSessionService.getCacheManager();
        this.clientConnectionDAO = examSessionService.getClientConnectionDAO();
        this.eventHandlingStrategy = eventHandlingStrategyFactory.get();
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.sebInstructionService = sebInstructionService;
        this.examAdminService = examAdminService;
        this.clientIndicatorFactory = clientIndicatorFactory;
        this.distributedPingCache = distributedPingCache;
        this.isDistributedSetup = sebInstructionService.getWebserviceInfo().isDistributed();
    }

    @Override
    public ExamSessionService getExamSessionService() {
        return this.examSessionService;
    }

    @Override
    public Result<ClientConnection> createClientConnection(
            final Principal principal,
            final Long institutionId,
            final String clientAddress,
            final String sebVersion,
            final String sebOsName,
            final String sebMachineName,
            final Long examId,
            final String clientId) {

        return Result.tryCatch(() -> {

            final SEBClientConfig clientConfig = this.sebClientConfigDAO
                    .byClientName(principal.getName())
                    .getOrThrow();

            if (!clientConfig.institutionId.equals(institutionId)) {
                log.error("Institutional integrity violation: requested institution: {} authenticated institution: {}",
                        institutionId,
                        clientConfig.institutionId);
                throw new APIConstraintViolationException("Institutional integrity violation");
            }

            if (log.isDebugEnabled()) {
                log.debug("Request received on Exam Client Connection create endpoint: "
                        + "institution: {} "
                        + "exam: {} "
                        + "client-address: {}"
                        + "clientId: {}",
                        institutionId,
                        examId,
                        clientAddress,
                        clientId);
            }

            if (examId != null) {
                checkExamIntegrity(examId);
            }

            // Create ClientConnection in status CONNECTION_REQUESTED for further processing
            final String connectionToken = createToken();
            final ClientConnection clientConnection = this.clientConnectionDAO.createNew(new ClientConnection(
                    null,
                    institutionId, // Set the institution identifier that was checked against integrity before
                    examId, // Set the exam identifier if available otherwise it is null
                    ConnectionStatus.CONNECTION_REQUESTED, // Initial state
                    connectionToken, // The generated connection token that identifies this connection
                    null,
                    (clientAddress != null) ? clientAddress : Constants.EMPTY_NOTE, // The IP address of the connecting client, verified on SEB Server side
                    sebOsName,
                    sebMachineName,
                    sebVersion,
                    clientId, // The client identifier sent by the SEB client if available
                    clientConfig.vdiType != VDIType.NO, // The VDI flag to indicate if this is a VDI prime connection
                    null,
                    null,
                    null,
                    null,
                    null))
                    .getOrThrow();

            // initialize distributed indicator value caches if possible and needed
            if (clientConnection.examId != null && this.isDistributedSetup) {
                this.clientIndicatorFactory.initializeDistributedCaches(clientConnection);
            }

            // load client connection data into cache
            final ClientConnectionDataInternal activeClientConnection = this.examSessionService
                    .getConnectionDataInternal(connectionToken);

            if (activeClientConnection == null) {
                log.warn("Failed to load ClientConnectionDataInternal into cache on update");
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("New ClientConnection created: {}", clientConnection);
                }
            }

            return clientConnection;
        });
    }

    @Override
    public Result<ClientConnection> updateClientConnection(
            final String connectionToken,
            final Long institutionId,
            final Long examId,
            final String clientAddress,
            final String sebVersion,
            final String sebOsName,
            final String sebMachineName,
            final String userSessionId,
            final String clientId) {

        return Result.tryCatch(() -> {

            ClientConnection clientConnection = getClientConnection(connectionToken);
            checkInstitutionalIntegrity(institutionId, clientConnection);
            checkExamIntegrity(examId, clientConnection);

            // connection integrity check
            if (!clientConnection.status.clientActiveStatus) {
                log.error(
                        "ClientConnection integrity violation: client connection is not in expected state: {}",
                        clientConnection);
                throw new IllegalArgumentException(
                        "ClientConnection integrity violation: client connection is not in expected state");
            }
            if (StringUtils.isNoneBlank(clientAddress) &&
                    StringUtils.isNotBlank(clientConnection.clientAddress) &&
                    !clientAddress.equals(clientConnection.clientAddress)) {
                log.error(
                        "ClientConnection integrity violation: client address mismatch: {}, {}",
                        clientAddress,
                        clientConnection.clientAddress);
                throw new IllegalArgumentException(
                        "ClientConnection integrity violation: client address mismatch");
            }

            if (examId != null) {
                checkExamIntegrity(examId);
            }

            if (log.isDebugEnabled()) {
                log.debug(
                        "SEB client connection, update ClientConnection for "
                                + "connectionToken {} "
                                + "institutionId: {}"
                                + "exam: {} "
                                + "client address: {} "
                                + "userSessionId: {}"
                                + "clientId: {}",
                        connectionToken,
                        institutionId,
                        examId,
                        clientAddress,
                        userSessionId,
                        clientId);
            }

            // userSessionId integrity check
            clientConnection = updateUserSessionId(userSessionId, clientConnection, examId);
            final ClientConnection updatedClientConnection = this.clientConnectionDAO
                    .save(new ClientConnection(
                            clientConnection.id,
                            null,
                            examId,
                            (userSessionId != null) ? ConnectionStatus.AUTHENTICATED : null,
                            null,
                            clientConnection.userSessionId,
                            StringUtils.isNoneBlank(clientAddress) ? clientAddress : null,
                            StringUtils.isNoneBlank(sebOsName) ? sebOsName : null,
                            StringUtils.isNoneBlank(sebMachineName) ? sebMachineName : null,
                            StringUtils.isNoneBlank(sebVersion) ? sebVersion : null,
                            StringUtils.isNoneBlank(clientId) ? clientId : null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null))
                    .getOrThrow();

            // initialize distributed indicator value caches if possible and needed
            if (examId != null && this.isDistributedSetup) {
                this.clientIndicatorFactory.initializeDistributedCaches(clientConnection);
            }

            final ClientConnectionDataInternal activeClientConnection =
                    reloadConnectionCache(connectionToken);

            if (activeClientConnection == null) {
                log.warn("Failed to load ClientConnectionDataInternal into cache on update");
            } else if (log.isDebugEnabled()) {
                log.debug("SEB client connection, successfully updated ClientConnection: {}",
                        updatedClientConnection);
            }

            return updatedClientConnection;
        });
    }

    @Override
    public Result<ClientConnection> establishClientConnection(
            final String connectionToken,
            final Long institutionId,
            final Long examId,
            final String clientAddress,
            final String sebVersion,
            final String sebOsName,
            final String sebMachineName,
            final String userSessionId,
            final String clientId) {

        return Result.tryCatch(() -> {

            ClientConnection clientConnection = getClientConnection(connectionToken);

            // connection integrity check
            if (clientConnection.status == ConnectionStatus.ACTIVE) {
                // connection already established. Check if IP is the same
                if (StringUtils.isNoneBlank(clientAddress) &&
                        StringUtils.isNotBlank(clientConnection.clientAddress) &&
                        !clientAddress.equals(clientConnection.clientAddress)) {
                    log.error(
                            "ClientConnection integrity violation: client address mismatch: {}, {}",
                            clientAddress,
                            clientConnection.clientAddress);
                    throw new IllegalArgumentException(
                            "ClientConnection integrity violation: client address mismatch");
                }
            } else if (!clientConnection.status.clientActiveStatus) {
                log.error("ClientConnection integrity violation: client connection is not in expected state: {}",
                        clientConnection);
                throw new IllegalArgumentException(
                        "ClientConnection integrity violation: client connection is not in expected state");
            }

            if (log.isDebugEnabled()) {
                log.debug(
                        "SEB client connection, establish ClientConnection for "
                                + "connectionToken {} "
                                + "institutionId: {}"
                                + "exam: {} "
                                + "client address: {} "
                                + "userSessionId: {}"
                                + "clientId: {}",
                        connectionToken,
                        institutionId,
                        examId,
                        clientAddress,
                        userSessionId,
                        clientId);
            }

            checkInstitutionalIntegrity(institutionId, clientConnection);
            checkExamIntegrity(examId, clientConnection);
            clientConnection = updateUserSessionId(userSessionId, clientConnection, examId);

            // connection integrity check
            if (clientConnection.status != ConnectionStatus.ACTIVE) {
                if (clientConnection.status == ConnectionStatus.CONNECTION_REQUESTED) {
                    log.warn("ClientConnection integrity warning: client connection is not authenticated: {}",
                            clientConnection);
                } else if (clientConnection.status != ConnectionStatus.AUTHENTICATED) {
                    log.error("ClientConnection integrity violation: client connection is not in expected state: {}",
                            clientConnection);
                    throw new IllegalArgumentException(
                            "ClientConnection integrity violation: client connection is not in expected state");
                }
            }

            final Boolean proctoringEnabled = this.examAdminService
                    .isProctoringEnabled(clientConnection.examId)
                    .getOr(false);
            final Long currentExamId = (examId != null) ? examId : clientConnection.examId;
            final String currentVdiConnectionId = (clientId != null)
                    ? clientId
                    : clientConnection.virtualClientId;

            // create new ClientConnection for update
            final ClientConnection establishedClientConnection = new ClientConnection(
                    clientConnection.id,
                    null,
                    currentExamId,
                    ConnectionStatus.ACTIVE,
                    null,
                    clientConnection.userSessionId,
                    StringUtils.isNoneBlank(clientAddress) ? clientAddress : null,
                    StringUtils.isNoneBlank(sebOsName) ? sebOsName : null,
                    StringUtils.isNoneBlank(sebMachineName) ? sebMachineName : null,
                    StringUtils.isNoneBlank(sebVersion) ? sebVersion : null,
                    StringUtils.isNoneBlank(clientId) ? clientId : null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    proctoringEnabled);

            // ClientConnection integrity check
            // institutionId, connectionToken and clientAddress must be set
            // The status ins not already active
            // and if this is not a VDI prime connection, the examId must also be set
            if (clientConnection.institutionId == null ||
                    clientConnection.connectionToken == null ||
                    establishedClientConnection.examId == null ||
                    clientConnection.clientAddress == null ||
                    establishedClientConnection.status != ConnectionStatus.ACTIVE ||
                    (!BooleanUtils.isTrue(clientConnection.vdi) && currentExamId == null)) {

                log.error("ClientConnection integrity violation, clientConnection: {}, establishedClientConnection: {}",
                        clientConnection,
                        establishedClientConnection);
                throw new IllegalStateException("ClientConnection integrity violation");
            }

            final ClientConnection connectionToSave = handleVDISetup(
                    currentVdiConnectionId,
                    establishedClientConnection);

            final ClientConnection updatedClientConnection = this.clientConnectionDAO
                    .save(connectionToSave)
                    .getOrThrow();

            // check exam integrity for established connection
            checkExamIntegrity(establishedClientConnection.examId);

            // initialize distributed indicator value caches if possible and needed
            if (examId != null && this.isDistributedSetup) {
                this.clientIndicatorFactory.initializeDistributedCaches(clientConnection);
            }

            // if proctoring is enabled for exam, mark for room update
            if (proctoringEnabled) {
                this.clientConnectionDAO.markForProctoringUpdate(updatedClientConnection.id);
            }

            // flush and reload caches to work with actual connection data
            final ClientConnectionDataInternal activeClientConnection =
                    reloadConnectionCache(connectionToken);

            if (activeClientConnection == null) {
                log.warn("Failed to load ClientConnectionDataInternal into cache on update");
            } else if (log.isDebugEnabled()) {
                log.debug("SEB client connection, successfully established ClientConnection: {}",
                        updatedClientConnection);
            }

            return updatedClientConnection;
        });
    }

    private ClientConnection handleVDISetup(
            final String currentVdiConnectionId,
            final ClientConnection establishedClientConnection) {

        if (currentVdiConnectionId == null) {
            return establishedClientConnection;
        }

        final Result<ClientConnectionRecord> vdiPairConnectionResult =
                this.clientConnectionDAO.getVDIPairCompanion(
                        establishedClientConnection.examId,
                        establishedClientConnection.virtualClientId);

        if (!vdiPairConnectionResult.hasValue()) {
            return establishedClientConnection;
        }

        final ClientConnectionRecord vdiPairCompanion = vdiPairConnectionResult.get();
        final Long vdiExamId = (establishedClientConnection.examId != null)
                ? establishedClientConnection.examId
                : vdiPairCompanion.getExamId();
        final ClientConnection updatedConnection = new ClientConnection(
                establishedClientConnection.id,
                null,
                vdiExamId,
                establishedClientConnection.status,
                null,
                establishedClientConnection.userSessionId,
                null,
                null,
                null,
                null,
                establishedClientConnection.virtualClientId,
                null,
                vdiPairCompanion.getConnectionToken(),
                null,
                null,
                null,
                establishedClientConnection.remoteProctoringRoomUpdate);

        // Update other connection with token and exam id
        this.clientConnectionDAO
                .save(new ClientConnection(
                        vdiPairCompanion.getId(), null,
                        vdiExamId, null, null, null, null, null, null,
                        establishedClientConnection.connectionToken, null, null, null, null, null, null, null))
                .getOrThrow();
        reloadConnectionCache(vdiPairCompanion.getConnectionToken());
        return updatedConnection;
    }

    @Override
    public Result<ClientConnection> closeConnection(
            final String connectionToken,
            final Long institutionId,
            final String clientAddress) {

        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("SEB client connection: regular close attempt for "
                        + "institution {} "
                        + "client address: {} "
                        + "connectionToken {} ",
                        institutionId,
                        clientAddress,
                        connectionToken);
            }

            final ClientConnection clientConnection = this.clientConnectionDAO
                    .byConnectionToken(connectionToken)
                    .getOrThrow();

            ClientConnection updatedClientConnection;
            if (clientConnection.status != ConnectionStatus.CLOSED) {
                updatedClientConnection = saveInState(
                        clientConnection,
                        ConnectionStatus.CLOSED);

                if (log.isDebugEnabled()) {
                    log.debug("SEB client connection: successfully closed ClientConnection: {}",
                            updatedClientConnection);
                }
            } else {
                log.warn("SEB client connection is already closed: {}", clientConnection);
                updatedClientConnection = clientConnection;
            }

            // if proctoring is enabled for exam, mark for room update
            final Boolean proctoringEnabled = this.examAdminService
                    .isProctoringEnabled(clientConnection.examId)
                    .getOr(false);
            if (proctoringEnabled) {
                this.clientConnectionDAO.markForProctoringUpdate(updatedClientConnection.id);
            }

            // delete stored ping if this is a distributed setup
            if (this.isDistributedSetup) {
                this.distributedPingCache
                        .deleteIndicatorValues(updatedClientConnection.id);
            }

            reloadConnectionCache(connectionToken);
            return updatedClientConnection;
        });
    }

    @Override
    public Result<ClientConnection> disableConnection(final String connectionToken, final Long institutionId) {
        return Result.tryCatch(() -> {
            final ClientConnectionData connectionData = getExamSessionService()
                    .getConnectionData(connectionToken)
                    .getOrThrow();

            // An active connection can only be disabled if we have a missing ping
            if (connectionData.clientConnection.status == ConnectionStatus.ACTIVE &&
                    !BooleanUtils.isTrue(connectionData.getMissingPing())) {

                return connectionData.clientConnection;
            }

            if (log.isDebugEnabled()) {
                log.debug("SEB client connection: SEB Server disable attempt for "
                        + "institution {} "
                        + "connectionToken {} ",
                        institutionId,
                        connectionToken);
            }

            final ClientConnection clientConnection = this.clientConnectionDAO
                    .byConnectionToken(connectionToken)
                    .getOrThrow();

            ClientConnection updatedClientConnection;
            if (DISABLE_STATE_PREDICATE.test(clientConnection)) {

                updatedClientConnection = saveInState(
                        clientConnection,
                        ConnectionStatus.DISABLED);

                if (log.isDebugEnabled()) {
                    log.debug("SEB client connection: successfully disabled ClientConnection: {}",
                            clientConnection);
                }

            } else {
                log.warn("SEB client connection in invalid state for disabling: {}", clientConnection);
                updatedClientConnection = clientConnection;
            }

            // if proctoring is enabled for exam, mark for room update
            final Boolean proctoringEnabled = this.examAdminService
                    .isProctoringEnabled(clientConnection.examId)
                    .getOr(false);
            if (proctoringEnabled) {
                this.clientConnectionDAO.markForProctoringUpdate(updatedClientConnection.id);
            }

            // delete stored ping if this is a distributed setup
            if (this.isDistributedSetup) {
                this.distributedPingCache
                        .deleteIndicatorValues(updatedClientConnection.id);
            }

            reloadConnectionCache(connectionToken);
            return updatedClientConnection;
        });
    }

    @Override
    public Result<Collection<EntityKey>> disableConnections(final String[] connectionTokens, final Long institutionId) {
        return Result.tryCatch(() -> {

            return Stream.of(connectionTokens)
                    .map(token -> disableConnection(token, institutionId)
                            .onError(error -> log.error("Failed to disable SEB client connection: {}", token))
                            .getOr(null))
                    .filter(clientConnection -> clientConnection != null)
                    .map(clientConnection -> clientConnection.getEntityKey())
                    .collect(Collectors.toList());
        });
    }

    @Override
    public void updatePingEvents() {
        try {

            final Cache cache = this.cacheManager.getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
            final long now = Utils.getMillisecondsNow();
            final Consumer<ClientConnectionDataInternal> missingPingUpdate = missingPingUpdate(now);
            this.examSessionService
                    .getExamDAO()
                    .allRunningExamIds()
                    .getOrThrow()
                    .stream()
                    .flatMap(examId -> this.isDistributedSetup
                            ? this.clientConnectionDAO
                                    .getConnectionTokensNoCache(examId)
                                    .getOrThrow()
                                    .stream()
                            : this.clientConnectionDAO
                                    .getConnectionTokens(examId)
                                    .getOrThrow()
                                    .stream())
                    .map(token -> cache.get(token, ClientConnectionDataInternal.class))
                    .filter(Objects::nonNull)
                    .filter(connection -> connection.pingIndicator != null &&
                            connection.clientConnection.status.clientActiveStatus)
                    .forEach(connection -> missingPingUpdate.accept(connection));

        } catch (final Exception e) {
            log.error("Failed to update ping events: ", e);
        }
    }

    @Override
    public void cleanupInstructions() {
        this.sebInstructionService.cleanupInstructions();
    }

    @Override
    public String notifyPing(
            final String connectionToken,
            final long timestamp,
            final int pingNumber,
            final String instructionConfirm) {

        processPing(connectionToken, timestamp, pingNumber);

        if (instructionConfirm != null) {
            this.sebInstructionService.confirmInstructionDone(connectionToken, instructionConfirm);
        }

        return this.sebInstructionService.getInstructionJSON(connectionToken);
    }

    private void processPing(final String connectionToken, final long timestamp, final int pingNumber) {

        final ClientConnectionDataInternal activeClientConnection =
                this.examSessionCacheService.getClientConnection(connectionToken);

        if (activeClientConnection != null) {
            activeClientConnection.notifyPing(timestamp, pingNumber);
        }
    }

    @Override
    public void notifyClientEvent(
            final String connectionToken,
            final ClientEvent event) {

        try {
            final ClientConnectionDataInternal activeClientConnection =
                    this.examSessionService.getConnectionDataInternal(connectionToken);

            if (activeClientConnection != null) {

                // store event
                this.eventHandlingStrategy.accept(ClientEvent.toRecord(
                        event,
                        activeClientConnection.getConnectionId()));

                // handle indicator update
                activeClientConnection
                        .getIndicatorMapping(event.eventType)
                        .forEach(indicator -> indicator.notifyValueChange(event));

            } else {
                log.warn("No active ClientConnection found for connectionToken: {}", connectionToken);
            }
        } catch (final Exception e) {
            log.error("Failed to process SEB client event: ", e);
        }
    }

    @Override
    public void confirmInstructionDone(final String connectionToken, final String instructionConfirm) {
        this.sebInstructionService.confirmInstructionDone(connectionToken, instructionConfirm);
    }

    @Override
    public Result<ClientConnectionData> getIndicatorValues(final ClientConnection clientConnection) {
        return Result.tryCatch(() -> new ClientConnectionData(
                clientConnection,
                this.clientIndicatorFactory.getIndicatorValues(clientConnection)));
    }

    private void checkExamRunning(final Long examId) {
        if (examId != null && !this.examSessionService.isExamRunning(examId)) {
            examNotRunningException(examId);
        }
    }

    private ClientConnection getClientConnection(final String connectionToken) {
        return this.clientConnectionDAO
                .byConnectionToken(connectionToken)
                .getOrThrow();
    }

    private void checkInstitutionalIntegrity(
            final Long institutionId,
            final ClientConnection clientConnection) throws IllegalAccessError {

        if (!institutionId.equals(clientConnection.institutionId)) {
            log.error("Institution integrity violation with institution: {} on clientConnection: {}",
                    institutionId,
                    clientConnection);
            throw new IllegalAccessError("Institution integrity violation");
        }
    }

    private String createToken() {
        return UUID.randomUUID().toString();
    }

    private void examNotRunningException(final Long examId) {
        log.error("The exam {} is not running", examId);
        throw new IllegalStateException("The exam " + examId + " is not running");
    }

    private void checkExamIntegrity(final Long examId, final ClientConnection clientConnection) {
        if (examId != null &&
                clientConnection.examId != null &&
                !examId.equals(clientConnection.examId)) {

            log.error("Exam integrity violation: another examId is already set for the connection: {}",
                    clientConnection);
            throw new IllegalArgumentException(
                    "Exam integrity violation: another examId is already set for the connection");
        }
        checkExamRunning(examId);
    }

    private ClientConnection updateUserSessionId(
            final String userSessionId,
            ClientConnection clientConnection,
            final Long examId) {

        if (StringUtils.isNoneBlank(userSessionId)) {
            if (StringUtils.isNoneBlank(clientConnection.userSessionId)) {
                if (clientConnection.userSessionId.contains(userSessionId)) {
                    if (log.isDebugEnabled()) {
                        log.debug("SEB sent LMS userSessionId but clientConnection has already a userSessionId");
                    }
                    return clientConnection;
                } else {
                    log.warn(
                            "Possible client integrity violation: clientConnection has already a userSessionId: {} : {}",
                            userSessionId, clientConnection.userSessionId);
                }
            }

            // try to get user account display name
            String accountId = userSessionId;
            try {
                final String newAccountId = this.examSessionService
                        .getRunningExam((clientConnection.examId != null)
                                ? clientConnection.examId
                                : examId)
                        .flatMap(exam -> this.examSessionService.getLmsAPIService().getLmsAPITemplate(exam.lmsSetupId))
                        .map(template -> template.getExamineeName(userSessionId))
                        .getOr(userSessionId);

                if (StringUtils.isNotBlank(clientConnection.userSessionId)) {
                    accountId = newAccountId +
                            Constants.SPACE +
                            Constants.EMBEDDED_LIST_SEPARATOR +
                            Constants.SPACE +
                            clientConnection.userSessionId;
                } else {
                    accountId = newAccountId;
                }
            } catch (final Exception e) {
                log.warn("Unexpected error while trying to get user account display name: {}", e.getMessage());
            }

            // create new ClientConnection for update
            final ClientConnection authenticatedClientConnection = new ClientConnection(
                    clientConnection.id, null, null,
                    ConnectionStatus.AUTHENTICATED, null,
                    accountId, null, null, null, null, null, null, null, null, null, null, null);

            clientConnection = this.clientConnectionDAO
                    .save(authenticatedClientConnection)
                    .getOrThrow();
        }
        return clientConnection;
    }

    private void checkExamIntegrity(final Long examId) {
        if (this.isDistributedSetup) {
            // if the cached Exam is not up to date anymore, we have to update the cache first
            final Result<Exam> updateExamCache = this.examSessionService.updateExamCache(examId);
            if (updateExamCache.hasError()) {
                log.warn("Failed to update Exam-Cache for Exam: {}", examId);
            }
        }

        // check Exam is running and not locked
        checkExamRunning(examId);
        if (this.examSessionService.isExamLocked(examId)) {
            throw new APIConstraintViolationException(
                    "Exam is currently on update and locked for new SEB Client connections");
        }

        // check Exam has a default SEB Exam configuration attached
        if (!this.examSessionService.hasDefaultConfigurationAttached(examId)) {
            throw new APIConstraintViolationException(
                    "Exam is currently running but has no default SEB Exam configuration attached");
        }
    }

    private ClientConnection saveInState(final ClientConnection clientConnection, final ConnectionStatus status) {
        final Boolean proctoringEnabled = this.examAdminService
                .isProctoringEnabled(clientConnection.examId)
                .getOr(false);

        return this.clientConnectionDAO.save(new ClientConnection(
                clientConnection.id, null, null, status,
                null, null, null, null, null, null, null, null, null, null, null, null,
                proctoringEnabled))
                .getOrThrow();
    }

    private ClientConnectionDataInternal reloadConnectionCache(final String connectionToken) {
        // evict cached ClientConnection
        this.examSessionCacheService.evictClientConnection(connectionToken);
        // and load updated ClientConnection into cache
        return this.examSessionService.getConnectionDataInternal(connectionToken);
    }

    private Consumer<ClientConnectionDataInternal> missingPingUpdate(final long now) {

        return connection -> {

            if (connection.pingIndicator.missingPingUpdate(now)) {
                final boolean missingPing = connection.getMissingPing();
                final ClientEventRecord clientEventRecord = new ClientEventRecord(
                        null,
                        connection.getConnectionId(),
                        (missingPing) ? EventType.ERROR_LOG.id : EventType.INFO_LOG.id,
                        now,
                        now,
                        new BigDecimal(connection.pingIndicator.getValue()),
                        (missingPing) ? "Missing Client Ping" : "Client Ping Back To Normal");

                // store event and and flush cache
                this.eventHandlingStrategy.accept(clientEventRecord);

                // update indicators
                if (clientEventRecord.getType() != null && EventType.ERROR_LOG.id == clientEventRecord.getType()) {
                    connection.getIndicatorMapping(EventType.ERROR_LOG)
                            .forEach(indicator -> indicator.notifyValueChange(clientEventRecord));
                }
            }
        };
    }

}
