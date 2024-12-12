/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig.VDIType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.institution.SecurityKeyService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConnectionConfigurationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientConnectionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator.DistributedIndicatorValueService;
import ch.ethz.seb.sebserver.webservice.weblayer.api.APIConstraintViolationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Lazy
@Service
@WebServiceProfile
public class SEBClientConnectionServiceImpl implements SEBClientConnectionService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientConnectionServiceImpl.class);

    private static final Predicate<ClientConnection> DISABLE_STATE_PREDICATE = ClientConnection
            .getStatusPredicate(
                    ConnectionStatus.UNDEFINED,
                    ConnectionStatus.CONNECTION_REQUESTED,
                    ConnectionStatus.READY,
                    ConnectionStatus.ACTIVE,
                    ConnectionStatus.CLOSED);

    private final ExamSessionService examSessionService;
    private final ExamSessionCacheService examSessionCacheService;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ClientIndicatorFactory clientIndicatorFactory;
    private final SEBClientConfigDAO sebClientConfigDAO;
    private final ExamAdminService examAdminService;
    private final DistributedIndicatorValueService distributedPingCache;
    private final SecurityKeyService securityKeyService;
    private final SEBClientEventBatchService sebClientEventBatchService;
    private final SEBClientInstructionService sebClientInstructionService;
    private final ConnectionConfigurationService connectionConfigurationService;
    private final JSONMapper jsonMapper;
    private final boolean isDistributedSetup;

    protected SEBClientConnectionServiceImpl(
            final ExamSessionService examSessionService,
            final SEBClientConfigDAO sebClientConfigDAO,
            final ExamAdminService examAdminService,
            final DistributedIndicatorValueService distributedPingCache,
            final ClientIndicatorFactory clientIndicatorFactory,
            final SecurityKeyService securityKeyService,
            final WebserviceInfo webserviceInfo,
            final SEBClientEventBatchService sebClientEventBatchService,
            final SEBClientInstructionService sebClientInstructionService,
            final ConnectionConfigurationService connectionConfigurationService,
            final JSONMapper jsonMapper) {

        this.examSessionService = examSessionService;
        this.examSessionCacheService = examSessionService.getExamSessionCacheService();
        this.clientConnectionDAO = examSessionService.getClientConnectionDAO();
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.examAdminService = examAdminService;
        this.clientIndicatorFactory = clientIndicatorFactory;
        this.distributedPingCache = distributedPingCache;
        this.securityKeyService = securityKeyService;
        this.isDistributedSetup = webserviceInfo.isDistributed();
        this.sebClientEventBatchService = sebClientEventBatchService;
        this.sebClientInstructionService = sebClientInstructionService;
        this.connectionConfigurationService = connectionConfigurationService;
        this.jsonMapper = jsonMapper;
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

            final SEBClientConfig clientConfig = principal == null ? null : this.sebClientConfigDAO
                    .byClientName(principal.getName())
                    .getOr(null);

            if (clientConfig == null) {
                log.error("Illegal client connection request: requested connection config name: {}",
                        (principal != null) ? principal.getName() : clientAddress);
                throw new AccessDeniedException("Unknown or illegal client access");
            }

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

            final String connectionToken = createToken();

            if (examId != null) {
                checkExamIntegrity(
                        examId,
                        null,
                        institutionId,
                        connectionToken,
                        clientAddress);
            }

            final String updateUserSessionId = updateUserSessionId(
                    examId,
                    null,
                    clientId,
                    sebMachineName,
                    null);

            // Create ClientConnection in status CONNECTION_REQUESTED for further processing
            final ClientConnection clientConnection = this.clientConnectionDAO.createNew(new ClientConnection(
                    null,
                    institutionId,
                    examId,
                    ConnectionStatus.CONNECTION_REQUESTED,
                    connectionToken,
                    updateUserSessionId,
                    (clientAddress != null) ? clientAddress : Constants.EMPTY_NOTE,
                    sebOsName,
                    sebMachineName,
                    sebVersion,
                    clientId,
                    clientConfig.vdiType != VDIType.NO,
                    null,
                    null,
                    null,
                    null,
                    null,
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

            if (examId != null) {
                this.clientConnectionDAO.evictConnectionTokenCache(examId);
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
            final String clientId,
            final String appSignatureKey) {

        return Result.tryCatch(() -> {

            final ClientConnection clientConnection = getClientConnection(connectionToken);
            final Long _examId = clientConnection.examId != null ? clientConnection.examId : examId;

            checkInstitutionalIntegrity(institutionId, clientConnection);
            connectionStatusIntegrityCheck(clientConnection, clientAddress);
            checkExamIntegrity(examId, clientConnection.examId, institutionId, clientConnection.connectionToken, clientConnection.info);

            if (log.isDebugEnabled()) {
                log.debug(
                    "SEB client connection, update ClientConnection for connectionToken {} institutionId: {} exam: {} client address: {} userSessionId: {} clientId: {}",
                    connectionToken, institutionId, examId, clientAddress, userSessionId, clientId);
            }

            final String updateUserSessionId = updateUserSessionId(
                    _examId,
                    userSessionId,
                    clientId,
                    sebMachineName,
                    clientConnection);

            final ConnectionStatus currentStatus = clientConnection.getStatus();
            final ConnectionStatus newStatus = (StringUtils.isNotBlank(userSessionId) && currentStatus == ConnectionStatus.READY)
                    ? ConnectionStatus.ACTIVE
                    : currentStatus;
            final String signatureHash = StringUtils.isNotBlank(appSignatureKey)
                    ? getSignatureHash(appSignatureKey, connectionToken, _examId)
                    : null;

            final ClientConnection updateConnection = new ClientConnection(
                    clientConnection.id,
                    null,
                    examId,
                    newStatus,
                    null,
                    updateUserSessionId,
                    StringUtils.isNotBlank(clientAddress) ? clientAddress : null,
                    StringUtils.isNotBlank(sebOsName) && clientConnection.sebOSName == null ? sebOsName : null,
                    StringUtils.isNotBlank(sebMachineName) && clientConnection.sebMachineName == null ? sebMachineName : null,
                    StringUtils.isNotBlank(sebVersion) && clientConnection.sebVersion == null ? sebVersion : null,
                    StringUtils.isNotBlank(clientId) && clientConnection.sebClientUserId == null ? clientId : null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    applyScreenProctoring(_examId, newStatus),
                    null,
                    applyProctoring(_examId, newStatus),
                    null,
                    signatureHash,
                    null);

            final ClientConnection updatedClientConnection = this.clientConnectionDAO
                    .save(updateConnection)
                    .getOrThrow();

            // initialize distributed indicator value caches if possible and needed
            if (examId != null && this.isDistributedSetup) {
                this.clientIndicatorFactory.initializeDistributedCaches(clientConnection);
            }

            final ClientConnectionDataInternal activeClientConnection = reloadConnectionCache(
                    connectionToken,
                    examId);

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
            final String clientId,
            final String appSignatureKey) {

        return Result.tryCatch(() -> {

            final ClientConnection clientConnection = getClientConnection(connectionToken);
            final Long _examId = clientConnection.examId != null ? clientConnection.examId : examId;

            connectionStatusIntegrityCheck(clientConnection, clientAddress);
            checkInstitutionalIntegrity(institutionId, clientConnection);
            checkExamIntegrity(examId, clientConnection.examId, institutionId, clientConnection.connectionToken, clientConnection.info);

            if (log.isDebugEnabled()) {
                log.debug(
                        "SEB client connection, establish ClientConnection for connectionToken {} institutionId: {} exam: {} client address: {} userSessionId: {} clientId: {}",
                        connectionToken, institutionId, examId, clientAddress, userSessionId, clientId);
            }

            final String updateUserSessionId = updateUserSessionId(
                    _examId,
                    userSessionId,
                    clientId,
                    sebMachineName,
                    clientConnection);

            final ConnectionStatus newStatus = StringUtils.isNotBlank(userSessionId) || alreadyAuthenticated(clientConnection)
                    ? ConnectionStatus.ACTIVE
                    : ConnectionStatus.READY;

            // create new ClientConnection for update
            final ClientConnection establishedClientConnection = new ClientConnection(
                    clientConnection.id,
                    null,
                    _examId,
                    newStatus,
                    null,
                    updateUserSessionId,
                    StringUtils.isNotBlank(clientAddress) ? clientAddress : null,
                    StringUtils.isNotBlank(sebOsName) ? sebOsName : null,
                    StringUtils.isNotBlank(sebMachineName) ? sebMachineName : null,
                    StringUtils.isNotBlank(sebVersion) ? sebVersion : null,
                    StringUtils.isNotBlank(clientId) ? clientId : null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    applyScreenProctoring(_examId, newStatus),
                    null,
                    applyProctoring(_examId, newStatus),
                    null,
                    getSignatureHash(appSignatureKey, connectionToken, _examId),
                    null);

            // ClientConnection integrity check
            if (clientConnection.institutionId == null ||
                    clientConnection.connectionToken == null ||
                    _examId == null ||
                    (clientConnection.clientAddress == null && clientAddress == null)) {

                log.error("ClientConnection integrity violation, clientConnection: {}, updatedClientConnection: {}",
                        clientConnection,
                        establishedClientConnection);

                throw new APIConstraintViolationException("ClientConnection integrity violation");
            }

            final ClientConnection updatedClientConnection = this.clientConnectionDAO
                    .save(establishedClientConnection)
                    .getOrThrow();

            // initialize distributed indicator value caches if possible and needed
            if (examId != null && this.isDistributedSetup) {
                this.clientIndicatorFactory.initializeDistributedCaches(clientConnection);
            }

            // flush and reload caches to work with actual connection data
            final ClientConnectionDataInternal activeClientConnection = reloadConnectionCache(
                    connectionToken,
                    examId);

            if (activeClientConnection == null) {
                log.warn("Failed to load ClientConnectionDataInternal into cache on update");
            } else if (log.isDebugEnabled()) {
                log.debug("SEB client connection, successfully established ClientConnection: {}",
                        establishedClientConnection);
            }

            return establishedClientConnection;
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

            final ClientConnection updatedClientConnection;
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

            // if screen proctoring is enabled, close the client session also on SPS site
            // usually it should have been closed by SEB but if not, SEB Server closes it
            if (BooleanUtils.isTrue(this.examAdminService.isScreenProctoringEnabled(clientConnection.examId).getOr(false))) {
                this.clientConnectionDAO.markForScreenProctoringUpdate(updatedClientConnection.id);
            }

            // delete stored ping if this is a distributed setup
            if (this.isDistributedSetup) {
                this.distributedPingCache
                        .deleteIndicatorValues(updatedClientConnection.id);
            }

            reloadConnectionCache(connectionToken, clientConnection.examId);
            return updatedClientConnection;
        });
    }

    @Override
    public Result<ClientConnection> disableConnection(final String connectionToken, final Long institutionId) {
        return Result.tryCatch(() -> {
            final ClientConnectionData connectionData = getExamSessionService()
                    .getConnectionData(connectionToken)
                    .getOrThrow();

            // A connection can only be disabled if we have a missing ping or for closed connections
            if (connectionData.clientConnection.status != ConnectionStatus.CLOSED &&
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

            final ClientConnection updatedClientConnection;
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
            
            // if screen proctoring is enabled, close the client session also on SPS site
            if (BooleanUtils.isTrue(this.examAdminService.isScreenProctoringEnabled(clientConnection.examId).getOr(false))) {
                this.clientConnectionDAO.markForScreenProctoringUpdate(updatedClientConnection.id);
            }

            // delete stored ping if this is a distributed setup
            if (this.isDistributedSetup) {
                this.distributedPingCache
                        .deleteIndicatorValues(updatedClientConnection.id);
            }

            reloadConnectionCache(connectionToken, clientConnection.examId);
            return updatedClientConnection;
        });
    }

    @Override
    public Result<Collection<EntityKey>> disableConnections(final String[] connectionTokens, final Long institutionId) {
        return Result.tryCatch(() -> Stream.of(connectionTokens)
                .map(token -> disableConnection(token, institutionId)
                        .onError(error -> log.error("Failed to disable SEB client connection: {}", token))
                        .getOr(null))
                .filter(Objects::nonNull)
                .map(Entity::getEntityKey)
                .collect(Collectors.toList()));
    }

    public void streamExamConfig(
            final Long institutionId,
            final Long examId,
            final String connectionToken,
            final String ipAddress,
            final HttpServletResponse response) {

        try {

            // if an examId is provided with the request, update the connection first
            if (examId != null) {
                final ClientConnection connection = this.updateClientConnection(
                                connectionToken,
                                institutionId,
                                examId,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null)
                        .getOrThrow();

                if (log.isDebugEnabled()) {
                    log.debug("Updated connection: {}", connection);
                }
            } else {

                // check connection status
                final ClientConnectionDataInternal cc = this.examSessionCacheService
                        .getClientConnection(connectionToken);
                if (cc != null) {
                    connectionStatusIntegrityCheck(cc.clientConnection, ipAddress);
                }
            }

            final ServletOutputStream outputStream = response.getOutputStream();

            try {

                this.examSessionService
                        .streamDefaultExamConfig(
                                institutionId,
                                connectionToken,
                                outputStream);

                response.setStatus(HttpStatus.OK.value());

            } catch (final Exception e) {
                final APIMessage errorMessage = APIMessage.ErrorMessage.GENERIC.of(e.getMessage());
                outputStream.write(Utils.toByteArray(this.jsonMapper.writeValueAsString(errorMessage)));
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            } finally {
                outputStream.flush();
                outputStream.close();
            }

        } catch (final IllegalArgumentException e) {
            final Collection<APIMessage> errorMessages = Arrays.asList(
                    APIMessage.ErrorMessage.CLIENT_CONNECTION_INTEGRITY_VIOLATION.of(e.getMessage()));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            writeSEBClientErrors(response, errorMessages);
        } catch (final Exception e) {
            log.error(
                    "Unexpected error while trying to stream SEB Exam Configuration to client with connection: {}",
                    connectionToken, e);

            final Collection<APIMessage> errorMessages = Arrays.asList(
                    APIMessage.ErrorMessage.GENERIC.of(e.getMessage()));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            writeSEBClientErrors(response, errorMessages);
        }
    }

    public void streamLightExamConfig(final String modelId, final HttpServletResponse response) throws IOException{

        final ServletOutputStream outputStream = response.getOutputStream();
        PipedOutputStream pout = null;
        PipedInputStream pin= null;

        try {
            pout = new PipedOutputStream();
            pin = new PipedInputStream(pout);

            this.connectionConfigurationService.exportSEBClientConfiguration(
                    pout,
                    modelId,
                    null);

            IOUtils.copyLarge(pin, outputStream);

            response.setStatus(HttpStatus.OK.value());

        }catch(Exception e){
            final APIMessage errorMessage = APIMessage.ErrorMessage.GENERIC.of(e.getMessage());
            outputStream.write(Utils.toByteArray(this.jsonMapper.writeValueAsString(errorMessage)));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        } finally {

            try {
                outputStream.flush();
                outputStream.close();

            } catch (IOException e) {
                 log.error("error while flushing / closing output stream", e);
            }
        }
    }

    private void writeSEBClientErrors(
            final HttpServletResponse response,
            final Collection<APIMessage> errorMessages) {

        try {
            response.getOutputStream().write(Utils.toByteArray(this.jsonMapper.writeValueAsString(errorMessages)));
        } catch (final Exception e1) {
            log.error("Failed to write error to response: ", e1);
        }
    }

    private void connectionStatusIntegrityCheck(
            final ClientConnection clientConnection,
            final String clientAddress) {

        // overall connection status integrity check
        if (!clientConnection.status.clientActiveStatus) {
            log.warn(
                    "ClientConnection integrity violation: client connection is not in expected state: {}",
                    clientConnection);

            // SEBSERV-440 send quit instruction to SEB
            sebClientInstructionService.registerInstruction(
                    clientConnection.examId,
                    ClientInstruction.InstructionType.SEB_QUIT,
                    Collections.emptyMap(),
                    clientConnection.connectionToken,
                    false,
                    false
            );

            throw new APIConstraintViolationException(
                    "ClientConnection integrity violation: client connection is not in expected state");
        }

        // SEBSERV-475 IP address change during handshake is possible but is logged within SEB logs
        if (StringUtils.isNoneBlank(clientAddress) &&
                StringUtils.isNotBlank(clientConnection.clientAddress) &&
                !clientAddress.equals(clientConnection.clientAddress)) {

            // log SEB client IP address change
            log.warn(
                    "ClientConnection integrity violation: client address mismatch: {}, {} connection: {}",
                    clientAddress,
                    clientConnection.clientAddress,
                    clientConnection.connectionToken);

            try {
                final long now = Utils.getMillisecondsNow();
                this.sebClientEventBatchService.accept(new SEBClientEventBatchService.EventData(
                        clientConnection.connectionToken,
                        now,
                        new ClientEvent(
                                null,
                                clientConnection.id,
                                ClientEvent.EventType.WARN_LOG,
                                now, now, null,
                                "SEB Client IP address changed: " +
                                        clientConnection.clientAddress +
                                        " -> " +
                                        clientAddress
                        )));
            } catch (final Exception e) {
                log.error("Failed to log SEB client IP address change: ", e);
            }
        }
    }



    private void checkExamRunning(final Long examId, final String ccToken, final String ccInfo) {
        if (examId != null && !this.examSessionService.isExamRunning(examId)) {
            log.warn("The exam {} is not running. Called by: {} info {}", examId, ccToken, ccInfo);
            throw new APIConstraintViolationException(
                    "The exam " + examId + " is not running");
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


    private String updateUserSessionId(
            final Long examId,
            final String userSessionId,
            final String clientId,
            final String sebMachineName,
            final ClientConnection clientConnection) {

        try {

            if (clientConnection == null) {
                if (StringUtils.isNotBlank(clientId)) {
                    return clientId;
                }
                if (StringUtils.isNotBlank(sebMachineName)) {
                    return sebMachineName;
                }
                return null;
            }

            // we don't have real userSessionId yet, so we use a placeholder of available
            if (StringUtils.isBlank(userSessionId)) {
                if (clientConnection.userSessionId == null) {
                    if (clientConnection.sebClientUserId != null) {
                        return clientConnection.sebClientUserId;
                    }
                    if (StringUtils.isNotBlank(clientId)) {
                        return clientId;
                    }
                    if (clientConnection.sebMachineName != null) {
                        return clientConnection.sebMachineName;
                    }
                    if (StringUtils.isNotBlank(sebMachineName)) {
                        return sebMachineName;
                    }
                    if (clientConnection.clientAddress != null) {
                        return clientConnection.clientAddress;
                    }
                    return null;
                } else if (clientConnection.userSessionId.equals(clientConnection.clientAddress)) {
                    if (clientConnection.sebClientUserId != null) {
                        return clientConnection.sebClientUserId;
                    }
                    if (StringUtils.isNotBlank(clientId)) {
                        return clientId;
                    }
                    if (clientConnection.sebMachineName != null) {
                        return clientConnection.sebMachineName;
                    }
                    if (StringUtils.isNotBlank(sebMachineName)) {
                        return sebMachineName;
                    }
                }

                return null;
            }

            if (examId == null) {
                return null;
            }

            // we got a userSessionId, so we want to resolve it via LMS binding
            final String accountId = this.examSessionService
                    .getRunningExam(examId)
                    .flatMap(exam -> this.examSessionService
                            .getLmsAPIService()
                            .getLmsAPITemplate(exam.lmsSetupId))
                    .map(template -> template.getExamineeName(userSessionId))
                    .getOr(userSessionId);

            // if userSessionId is not set yet or a placeholder is set, just use the new account name
            if (clientConnection.userSessionId == null ||
                    clientConnection.userSessionId.equals(clientConnection.sebClientUserId) ||
                    clientConnection.userSessionId.equals(clientConnection.sebMachineName) ||
                    clientConnection.userSessionId.equals(clientConnection.clientAddress)) {
                return accountId;
            }

            // otherwise apply new name
            return accountId +
                    Constants.SPACE +
                    Constants.EMBEDDED_LIST_SEPARATOR +
                    Constants.SPACE +
                    clientConnection.userSessionId;
        } catch (final Exception e) {
            log.error("Unexpected error while try to update userSessionId for connection: {}", clientConnection, e);
            return null;
        }
    }

    private void checkExamIntegrity(
            final Long examId,
            final Long currentExamId,
            final Long institutionId,
            final String ccToken,
            final String ccInfo) {

        if (examId == null) {
            return;
        }

        if (this.isDistributedSetup) {
            // if the cached Exam is not up-to-date anymore, we have to update the cache first
            this.examSessionService.updateExamCache(examId);
        }

        if (currentExamId != null && !examId.equals(currentExamId)) {
            log.error("Exam integrity violation: another examId is already set for the connection: {}", ccToken);
            throw new APIConstraintViolationException(
                    "Exam integrity violation: another examId is already set for the connection");
        }

        // check Exam is running and not locked
        checkExamRunning(examId, ccToken, ccInfo);
        if (this.examSessionService.isExamLocked(examId)) {
            throw new APIConstraintViolationException(
                    "Exam is currently on update and locked for new SEB Client connections");
        }

        // check Exam is within the correct institution
        if (this.examSessionService.getRunningExam(examId)
                .map(e -> !e.institutionId.equals(institutionId))
                .onError(error -> log.error("Failed to get running exam: ", error))
                .getOr(true)) {

            throw new APIConstraintViolationException(
                    "Exam institution mismatch. The requested exam is not within the expected institution");
        }

        // check Exam has a default SEB Exam configuration attached
        if (!this.examSessionService.hasDefaultConfigurationAttached(examId)) {
            throw new APIConstraintViolationException(
                    "Exam is currently running but has no default SEB Exam configuration attached");
        }
    }

    private ClientConnection saveInState(final ClientConnection clientConnection, final ConnectionStatus status) {
        return this.clientConnectionDAO.save(new ClientConnection(
                clientConnection.id, null, null, status,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, null, null, null))
                .getOrThrow();
    }

    private ClientConnectionDataInternal reloadConnectionCache(
            final String connectionToken,
            final Long examId) {

        if (examId != null) {
            // evict connection tokens for exam
            this.clientConnectionDAO.evictConnectionTokenCache(examId);
        }
        // evict cached ClientConnection
        this.examSessionCacheService.evictClientConnection(connectionToken);
        // and load updated ClientConnection into cache
        return this.examSessionService.getConnectionDataInternal(connectionToken);
    }

    private String getSignatureHash(
            final String appSignatureKey,
            final String connectionToken,
            final Long examId) {

        if (examId == null) {
            return null;
        }

        final String salt = this.examSessionService
                .getAppSignatureKeySalt(examId)
                .getOr(null);

        return this.securityKeyService
                .getAppSignatureKeyHash(appSignatureKey, connectionToken, salt)
                .onError(error -> log.error("Failed to get hash signature from sent app signature key: ", error))
                .getOr(null);
    }

    private boolean alreadyAuthenticated(final ClientConnection clientConnection) {
        return clientConnection.userSessionId != null &&
                !clientConnection.userSessionId.equals(clientConnection.clientAddress) &&
                !clientConnection.userSessionId.equals(clientConnection.sebClientUserId) &&
                !clientConnection.userSessionId.equals(clientConnection.sebMachineName);
    }

    private boolean applyProctoring(final Long examId, final ConnectionStatus status) {
        if (examId == null) {
            return false;
        }
        final Exam exam = this.examSessionCacheService.getRunningExam(examId);
        final boolean proctoringEnabled = exam != null && BooleanUtils.toBoolean(
                exam.getAdditionalAttribute(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING));

        return isApplyProctoring(status, exam) && proctoringEnabled;
    }

    private boolean applyScreenProctoring(final Long examId, final ConnectionStatus status) {
        if (examId == null) {
            return false;
        }
        final Exam exam = this.examSessionCacheService.getRunningExam(examId);
        final boolean screenProctoringEnabled = exam != null && BooleanUtils.toBoolean(
                exam.getAdditionalAttribute(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING));

        return isApplyProctoring(status, exam) && screenProctoringEnabled;
    }

    private static boolean isApplyProctoring(final ConnectionStatus status, final Exam exam) {
        return (exam != null && exam.lmsSetupId == null && status == ConnectionStatus.READY) ||
                status == ConnectionStatus.ACTIVE;
    }
}