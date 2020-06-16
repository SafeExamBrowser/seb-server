/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.EventHandlingStrategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.PingHandlingStrategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientConnectionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBInstructionService;
import ch.ethz.seb.sebserver.webservice.weblayer.api.APIConstraintViolationException;

@Lazy
@Service
@WebServiceProfile
public class SEBClientConnectionServiceImpl implements SEBClientConnectionService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientConnectionServiceImpl.class);

    private final ExamSessionService examSessionService;
    private final ExamSessionCacheService examSessionCacheService;
    private final CacheManager cacheManager;
    private final EventHandlingStrategy eventHandlingStrategy;
    private final ClientConnectionDAO clientConnectionDAO;
    private final PingHandlingStrategy pingHandlingStrategy;
    private final SEBClientConfigDAO sebClientConfigDAO;
    private final SEBInstructionService sebInstructionService;
    private final WebserviceInfo webserviceInfo;

    protected SEBClientConnectionServiceImpl(
            final ExamSessionService examSessionService,
            final EventHandlingStrategyFactory eventHandlingStrategyFactory,
            final PingHandlingStrategyFactory pingHandlingStrategyFactory,
            final SEBClientConfigDAO sebClientConfigDAO,
            final SEBInstructionService sebInstructionService) {

        this.examSessionService = examSessionService;
        this.examSessionCacheService = examSessionService.getExamSessionCacheService();
        this.cacheManager = examSessionService.getCacheManager();
        this.clientConnectionDAO = examSessionService.getClientConnectionDAO();
        this.pingHandlingStrategy = pingHandlingStrategyFactory.get();
        this.eventHandlingStrategy = eventHandlingStrategyFactory.get();
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.sebInstructionService = sebInstructionService;
        this.webserviceInfo = sebInstructionService.getWebserviceInfo();
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
            final Long examId) {

        return Result.tryCatch(() -> {

            final Long clientsInstitution = getInstitutionId(principal);
            if (!clientsInstitution.equals(institutionId)) {
                log.error("Institutional integrity violation: requested institution: {} authenticated institution: {}",
                        institutionId,
                        clientsInstitution);
                throw new APIConstraintViolationException("Institutional integrity violation");
            }

            if (log.isDebugEnabled()) {
                log.debug("Request received on Exam Client Connection create endpoint: "
                        + "institution: {} "
                        + "exam: {} "
                        + "client-address: {}",
                        institutionId,
                        examId,
                        clientAddress);
            }

            if (log.isDebugEnabled()) {
                log.debug("SEB client connection attempt, create ClientConnection for "
                        + "institution {} "
                        + "exam: {} "
                        + "client address: {}",
                        institutionId,
                        examId,
                        clientAddress);
            }

            if (examId != null) {
                checkExamIntegrity(examId);
            }

            // Create ClientConnection in status CONNECTION_REQUESTED for further processing
            final String connectionToken = createToken();
            final ClientConnection clientConnection = this.clientConnectionDAO.createNew(new ClientConnection(
                    null,
                    institutionId,
                    examId,
                    ConnectionStatus.CONNECTION_REQUESTED,
                    connectionToken,
                    null,
                    clientAddress,
                    null,
                    Utils.getMillisecondsNow()))
                    .getOrThrow();

            // load client connection data into cache
            final ClientConnectionDataInternal activeClientConnection = this.examSessionCacheService
                    .getActiveClientConnection(connectionToken);

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
            final String userSessionId) {

        return Result.tryCatch(() -> {
            if (log.isDebugEnabled()) {
                log.debug(
                        "SEB client connection, update ClientConnection for "
                                + "connectionToken {} "
                                + "institutionId: {}"
                                + "exam: {} "
                                + "client address: {} "
                                + "userSessionId: {}",
                        connectionToken,
                        institutionId,
                        examId,
                        clientAddress,
                        userSessionId);
            }

            final ClientConnection clientConnection = getClientConnection(connectionToken);

            checkInstitutionalIntegrity(institutionId, clientConnection);
            checkExamIntegrity(examId, clientConnection);

            // connection integrity check
            if (clientConnection.status != ConnectionStatus.CONNECTION_REQUESTED) {
                log.error("ClientConnection integrity violation: client connection is not in expected state: {}",
                        clientConnection);
                throw new IllegalArgumentException(
                        "ClientConnection integrity violation: client connection is not in expected state");
            }

            if (examId != null) {
                checkExamIntegrity(examId);
            }

            // userSessionId integrity check
            if (userSessionId != null &&
                    clientConnection.userSessionId != null &&
                    !userSessionId.equals(clientConnection.userSessionId)) {

                log.error(
                        "User session identifier integrity violation: another User session identifier is already set for the connection: {}",
                        clientConnection);
                throw new IllegalArgumentException(
                        "User session identifier integrity violation: another User session identifier is already set for the connection");
            }

            final String virtualClientAddress = getVirtualClientAddress(
                    (examId != null) ? examId : clientConnection.examId,
                    clientAddress,
                    clientConnection.clientAddress);

            final ClientConnection updatedClientConnection = this.clientConnectionDAO
                    .save(new ClientConnection(
                            clientConnection.id,
                            null,
                            examId,
                            (userSessionId != null) ? ConnectionStatus.AUTHENTICATED : null,
                            null,
                            userSessionId,
                            null,
                            virtualClientAddress,
                            null))
                    .getOrThrow();

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
            final String userSessionId) {

        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug(
                        "SEB client connection, establish ClientConnection for "
                                + "connectionToken {} "
                                + "institutionId: {}"
                                + "exam: {} "
                                + "client address: {} "
                                + "userSessionId: {}",
                        connectionToken,
                        institutionId,
                        examId,
                        clientAddress,
                        userSessionId);
            }

            ClientConnection clientConnection = getClientConnection(connectionToken);
            checkInstitutionalIntegrity(institutionId, clientConnection);
            checkExamIntegrity(examId, clientConnection);
            clientConnection = updateUserSessionId(userSessionId, clientConnection);

            // connection integrity check
            if (clientConnection.status == ConnectionStatus.CONNECTION_REQUESTED) {
                // TODO discuss if we need a flag on exam domain level that indicates whether unauthenticated connection
                //      are allowed or not
                log.warn("ClientConnection integrity warning: client connection is not authenticated: {}",
                        clientConnection);
            } else if (clientConnection.status != ConnectionStatus.AUTHENTICATED) {
                log.error("ClientConnection integrity violation: client connection is not in expected state: {}",
                        clientConnection);
                throw new IllegalArgumentException(
                        "ClientConnection integrity violation: client connection is not in expected state");
            }

            final String virtualClientAddress = getVirtualClientAddress(
                    (examId != null) ? examId : clientConnection.examId,
                    clientAddress,
                    clientConnection.clientAddress);

            // create new ClientConnection for update
            final ClientConnection establishedClientConnection = new ClientConnection(
                    clientConnection.id,
                    null,
                    (examId != null) ? examId : clientConnection.examId,
                    ConnectionStatus.ACTIVE,
                    null,
                    userSessionId,
                    null,
                    virtualClientAddress,
                    null);

            // ClientConnection integrity
            if (clientConnection.institutionId == null ||
                    clientConnection.connectionToken == null ||
                    establishedClientConnection.examId == null ||
                    clientConnection.clientAddress == null ||
                    establishedClientConnection.status != ConnectionStatus.ACTIVE) {

                log.error("ClientConnection integrity violation, clientConnection: {}, establishedClientConnection: {}",
                        clientConnection,
                        establishedClientConnection);
                throw new IllegalStateException("ClientConnection integrity violation");
            }

            final ClientConnection updatedClientConnection = this.clientConnectionDAO
                    .save(establishedClientConnection)
                    .getOrThrow();

            checkExamIntegrity(updatedClientConnection.examId);

            final ClientConnectionDataInternal activeClientConnection =
                    reloadConnectionCache(connectionToken);

            if (activeClientConnection == null) {
                log.warn("Failed to load ClientConnectionDataInternal into cache on update");
            } else if (log.isDebugEnabled()) {
                log.debug("SEB client connection, successfully established ClientConnection: {}",
                        updatedClientConnection);
            }

            // notify ping handler about established connection
            this.pingHandlingStrategy.initForConnection(
                    updatedClientConnection.id,
                    connectionToken);

            return updatedClientConnection;
        });
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
                            clientConnection);
                }
            } else {
                log.warn("SEB client connection is already closed: {}", clientConnection);
                updatedClientConnection = clientConnection;
            }

            reloadConnectionCache(connectionToken);
            return updatedClientConnection;
        });
    }

    private static final Predicate<ClientConnection> DISABLE_STATE_PREDICATE = ClientConnection
            .getStatusPredicate(
                    ConnectionStatus.UNDEFINED,
                    ConnectionStatus.CONNECTION_REQUESTED,
                    ConnectionStatus.AUTHENTICATED,
                    ConnectionStatus.CLOSED);

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

            reloadConnectionCache(connectionToken);
            return updatedClientConnection;
        });
    }

    @Override
    public void updatePingEvents() {
        try {

            final Cache cache = this.cacheManager.getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
            this.examSessionService
                    .getExamDAO()
                    .allRunningExamIds()
                    .getOrThrow()
                    .stream()
                    .flatMap(examId -> this.clientConnectionDAO
                            .getConnectionTokens(examId)
                            .getOrThrow()
                            .stream())
                    .map(token -> cache.get(token, ClientConnectionDataInternal.class))
                    .filter(Objects::nonNull)
                    .filter(connection -> connection.pingIndicator != null &&
                            connection.clientConnection.status.establishedStatus)
                    .map(connection -> connection.pingIndicator.updateLogEvent())
                    .filter(Objects::nonNull)
                    .forEach(this.eventHandlingStrategy);

        } catch (final Exception e) {
            log.error("Failed to update ping events: ", e);
        }
    }

    @Override
    public String notifyPing(
            final String connectionToken,
            final long timestamp,
            final int pingNumber) {

        this.pingHandlingStrategy.notifyPing(connectionToken, timestamp, pingNumber);
        return this.sebInstructionService.getInstructionJSON(connectionToken);
    }

    @Override
    public void notifyClientEvent(
            final String connectionToken,
            final ClientEvent event) {

        final ClientConnectionDataInternal activeClientConnection =
                this.examSessionCacheService.getActiveClientConnection(connectionToken);

        if (activeClientConnection != null) {

            // store event
            this.eventHandlingStrategy.accept(ClientEvent.toRecord(
                    event,
                    activeClientConnection.getConnectionId()));

            // update indicators
            activeClientConnection.getIndicatorMapping(event.eventType)
                    .forEach(indicator -> indicator.notifyValueChange(event));
        } else {
            log.warn("No active ClientConnection found for connectionToken: {}", connectionToken);
        }
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

    private String getVirtualClientAddress(
            final Long examId,
            final String requestClientAddress,
            final String existingClientAddress) {

        if (examId == null ||
                requestClientAddress == null ||
                requestClientAddress.equals(existingClientAddress) ||
                !isVDI(examId)) {
            return null;
        }

        return requestClientAddress;
    }

    private boolean isVDI(final Long examId) {
        return this.examSessionService.getRunningExam(examId)
                .getOrThrow()
                .getType() == ExamType.VDI;
    }

    private Long getInstitutionId(final Principal principal) {
        final String clientId = principal.getName();
        return this.sebClientConfigDAO.byClientName(clientId)
                .getOrThrow().institutionId;
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

    private ClientConnection updateUserSessionId(final String userSessionId, ClientConnection clientConnection) {
        if (StringUtils.isNoneBlank(userSessionId)) {
            if (StringUtils.isNoneBlank(clientConnection.userSessionId)) {
                log.error(
                        "ClientConnection integrity violation: clientConnection has already a userSessionId: {} : {}",
                        userSessionId, clientConnection);
                throw new IllegalArgumentException(
                        "ClientConnection integrity violation: clientConnection has already a userSessionId");
            }

            // create new ClientConnection for update
            final ClientConnection authenticatedClientConnection = new ClientConnection(
                    clientConnection.id,
                    null,
                    null,
                    ConnectionStatus.AUTHENTICATED,
                    null,
                    userSessionId,
                    null,
                    null,
                    null);

            clientConnection = this.clientConnectionDAO
                    .save(authenticatedClientConnection)
                    .getOrThrow();
        }
        return clientConnection;
    }

    private void checkExamIntegrity(final Long examId) {
        if (this.webserviceInfo.isDistributed()) {
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

        // check Exam has an default SEB Exam configuration attached
        if (!this.examSessionService.hasDefaultConfigurationAttached(examId)) {
            throw new APIConstraintViolationException(
                    "Exam is currently has no default SEB Exam configuration attached");
        }
    }

    private ClientConnection saveInState(final ClientConnection clientConnection, final ConnectionStatus status) {
        return this.clientConnectionDAO.save(new ClientConnection(
                clientConnection.id, null, null,
                status, null, null, null, null, null))
                .getOrThrow();
    }

    private ClientConnectionDataInternal reloadConnectionCache(final String connectionToken) {
        // evict cached ClientConnection
        this.examSessionCacheService.evictClientConnection(connectionToken);
        // evict also cached ping record
        this.examSessionCacheService.evictPingRecord(connectionToken);
        // and load updated ClientConnection into cache
        return this.examSessionCacheService.getActiveClientConnection(connectionToken);
    }

}
