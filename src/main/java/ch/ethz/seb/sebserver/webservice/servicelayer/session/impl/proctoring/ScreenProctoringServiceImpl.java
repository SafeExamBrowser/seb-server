/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import static ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_SCREEN_PROCTORING.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ProctoringGroupMonitoringData;
import ch.ethz.seb.sebserver.gbl.monitoring.ClientGroupMatcherService;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ClientConnectionDAOImpl;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.LmsSetupChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.model.exam.CollectingStrategy;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.SPS_API.SPSData;

@Lazy
@Service
@WebServiceProfile
public class ScreenProctoringServiceImpl implements ScreenProctoringService {

    private static final Logger log = LoggerFactory.getLogger(ScreenProctoringServiceImpl.class);
    public static final Logger INIT_LOGGER = LoggerFactory.getLogger("ch.ethz.seb.SEB_SERVER_INIT");
    
    private final ClientGroupMatcherService clientGroupMatcherService;
    private final ClientGroupDAO clientGroupDAO;
    private final Cryptor cryptor;
    private final ScreenProctoringAPIBinding screenProctoringAPIBinding;
    private final ScreenProctoringGroupDAO screenProctoringGroupDAO;
    private final ProctoringSettingsDAO proctoringSettingsDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamDAO examDAO;
    private final SEBClientInstructionService sebInstructionService;
    private final ExamSessionCacheService examSessionCacheService;
    private final WebserviceInfo webserviceInfo;
    private final WebserviceInfo.ScreenProctoringServiceBundle screenProctoringServiceBundle;

    public ScreenProctoringServiceImpl(
            final ClientGroupMatcherService clientGroupMatcherService,
            final ClientGroupDAO clientGroupDAO1,
            final Cryptor cryptor,
            final ProctoringSettingsDAO proctoringSettingsDAO,
            final AsyncService asyncService,
            final JSONMapper jsonMapper,
            final UserDAO userDAO,
            final ExamDAO examDAO,
            final ClientGroupDAO clientGroupDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ScreenProctoringGroupDAO screenProctoringGroupDAO,
            final SEBClientInstructionService sebInstructionService,
            final ExamSessionCacheService examSessionCacheService,
            final WebserviceInfo webserviceInfo,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService) {
        
        this.clientGroupMatcherService = clientGroupMatcherService;
        this.clientGroupDAO = clientGroupDAO1;
        this.cryptor = cryptor;
        this.examDAO = examDAO;
        this.screenProctoringGroupDAO = screenProctoringGroupDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.sebInstructionService = sebInstructionService;
        this.examSessionCacheService = examSessionCacheService;
        this.proctoringSettingsDAO = proctoringSettingsDAO;
        this.webserviceInfo = webserviceInfo;
        this.screenProctoringServiceBundle = webserviceInfo.getScreenProctoringServiceBundle();

        this.screenProctoringAPIBinding = new ScreenProctoringAPIBinding(
                userDAO,
                clientGroupDAO,
                cryptor,
                asyncService,
                jsonMapper,
                proctoringSettingsDAO,
                additionalAttributesDAO,
                screenProctoringGroupDAO,
                clientHttpRequestFactoryService,
                webserviceInfo);
    }

    @Override
    public boolean isScreenProctoringEnabled(final Long examId) {
        return this.proctoringSettingsDAO.isScreenProctoringEnabled(examId);
    }

    @Override
    public Result<ScreenProctoringSettings> testSettings(
            final ScreenProctoringSettings screenProctoringSettings,
            final EntityKey parentKey) {
        
        return Result.tryCatch(() -> {
            
            if (!BooleanUtils.isTrue(screenProctoringSettings.enableScreenProctoring)) {
                log.debug("Screen Proctoring is not enabled --> not test is applied");
                return screenProctoringSettings;
            }

            final Collection<APIMessage> fieldChecks = new ArrayList<>();
            if (StringUtils.isBlank(screenProctoringSettings.spsServiceURL)) {
                fieldChecks.add(APIMessage.fieldValidationError(
                        "appKey",
                        "screenProctoringSettings:spsServiceURL:notNull"));
            }
            if (screenProctoringSettings.spsServiceURL != null
                    && screenProctoringSettings.spsServiceURL.contains("?")) {
                fieldChecks.add(APIMessage.fieldValidationError(
                        "appKey",
                        "screenProctoringSettings:spsServiceURL:invalidURL"));
            }

            if (StringUtils.isBlank(screenProctoringSettings.spsAPIKey)) {
                fieldChecks.add(APIMessage.fieldValidationError(
                        "appKey",
                        "screenProctoringSettings:spsAPIKey:notNull"));
            }
            if (StringUtils.isBlank(screenProctoringSettings.spsAPISecret)) {
                fieldChecks.add(APIMessage.fieldValidationError(
                        "appSecret",
                        "screenProctoringSettings:spsAPISecret:notNull"));
            }
            if (StringUtils.isBlank(screenProctoringSettings.spsAccountId)) {
                fieldChecks.add(APIMessage.fieldValidationError(
                        "clientId",
                        "screenProctoringSettings:spsAccountId:notNull"));
            }
            if (StringUtils.isBlank(screenProctoringSettings.spsAccountPassword)) {
                fieldChecks.add(APIMessage.fieldValidationError(
                        "clientSecret",
                        "screenProctoringSettings:spsAccountPassword:notNull"));
            }
            
            if (screenProctoringSettings.collectingStrategy == CollectingStrategy.APPLY_SEB_GROUPS &&
            StringUtils.isBlank(screenProctoringSettings.sebGroupsSelection)) {
                fieldChecks.add(APIMessage.fieldValidationError(
                        "clientSecret",
                        "screenProctoringSettings:spsSEBGroupsSelection:notNull"));
            }
            
            if (parentKey.entityType == EntityType.EXAM) {

                if (this.clientConnectionDAO.hasActiveSEBConnections(screenProctoringSettings.examId)) {
                    throw new APIMessageException(APIMessage.ErrorMessage.CLIENT_CONNECTION_INTEGRITY_VIOLATION.of());
                }

                // get existing groups 
                final Collection<ScreenProctoringGroup> existingGroups = this
                        .getCollectingGroups(screenProctoringSettings.examId)
                        .getOr(Collections.emptyList());

                if (!existingGroups.isEmpty()) {
                    // check for when there are already existing groups
                    final ScreenProctoringSettings oldSettings = this.proctoringSettingsDAO
                            .getScreenProctoringSettings(parentKey)
                            .getOr(null);
                    
                    if (oldSettings != null && oldSettings.collectingStrategy != screenProctoringSettings.collectingStrategy) {
                        // not possible to change grouping strategy when it has already groups
                        fieldChecks.add(APIMessage.fieldValidationError(
                                "spsCollectingStrategy",
                                "screenProctoringSettings:spsCollectingStrategy:collecting-strategy-not-changeable"));
                    }
                    
                    // find deletion and check possibility (only deletable if no sessions)
                    if (screenProctoringSettings.collectingStrategy == CollectingStrategy.APPLY_SEB_GROUPS) {
                        final Map<Long, ScreenProctoringGroup> existing = existingGroups.stream()
                                .filter(g -> !g.isFallback)
                                .collect(Collectors.toMap( g -> g.sebGroupId, Function.identity()));

                        Arrays.stream(StringUtils.split(
                                screenProctoringSettings.sebGroupsSelection, 
                                        Constants.LIST_SEPARATOR_CHAR))
                                .map(Long::valueOf)
                                .forEach(existing::remove);

                        existing.values().forEach( g -> {
                            if (g.size != null && g.size > 0) {
                                fieldChecks.add(APIMessage.fieldValidationError(
                                        "clientSecret",
                                        "screenProctoringSettings:spsSEBGroupsSelection:group-not-deletable"));
                            }
                        });
                    }
                }
            }
            
            if (!fieldChecks.isEmpty()) {
                throw new APIMessageException(fieldChecks);
            }

            final ScreenProctoringSettings proctoringServiceSettings = new ScreenProctoringSettings(
                    screenProctoringSettings.examId,
                    screenProctoringSettings.enableScreenProctoring,
                    screenProctoringSettings.spsServiceURL,
                    screenProctoringSettings.spsAPIKey,
                    this.cryptor.encrypt(screenProctoringSettings.spsAPISecret).getOrThrow(),
                    screenProctoringSettings.spsAccountId,
                    this.cryptor.encrypt(screenProctoringSettings.spsAccountPassword).getOrThrow(),
                    screenProctoringSettings.collectingStrategy,
                    screenProctoringSettings.collectingGroupName,
                    screenProctoringSettings.collectingGroupSize,
                    screenProctoringSettings.sebGroupsSelection);

            this.screenProctoringAPIBinding
                    .testConnection(proctoringServiceSettings)
                    .getOrThrow();

            return screenProctoringSettings;
        });
    }

    @Override
    public Result<Exam> applyScreenProctoringForExam(final EntityKey entityKey) {

        return this.examDAO
                .byModelId(entityKey.modelId)
                .map(exam -> {

                    final boolean isSPSActive = this.screenProctoringAPIBinding.isSPSActive(exam);
                    final boolean isEnabling = this.isScreenProctoringEnabled(exam.id);

                    if (isEnabling && !isSPSActive) {
                        // if screen proctoring has been enabled
                        this.screenProctoringAPIBinding
                                .startScreenProctoring(exam)
                                .onError(error -> log.error("Failed to apply screen proctoring for exam: {}", exam, error))
                                .getOrThrow();

                        this.examDAO.markUpdate(exam.id);

                    } else if (!isEnabling && isSPSActive) {
                        // if screen proctoring has been disabled...
                        this.screenProctoringAPIBinding
                                .deactivateScreenProctoring(exam)
                                .onError(error -> log.error("Failed to dispose screen proctoring for exam: {}", exam, error))
                                .getOrThrow();

                        this.examDAO.markUpdate(exam.id);
                    } else if (isEnabling) {
                        this.screenProctoringAPIBinding.synchronizeGroups(exam);
                    }
                    
                    return exam;
                });
    }

    @Override
    public Result<Collection<ProctoringGroupMonitoringData>> getCollectingGroupsMonitoringData(final Long examId) {
        return this.examSessionCacheService.getScreenProctoringGroups(examId);
    }

    @Override
    public Result<Collection<ScreenProctoringGroup>> getCollectingGroups(final Long examId) {
        return screenProctoringGroupDAO
                .getCollectingGroups(examId)
                .onError(error -> log.error(
                        "Failed to screen proctoring groups for exam: {}, cause: {}",
                        examId,
                        error.getMessage()));
    }

    @Override
    public Result<Exam> updateExamOnScreenProctoringService(final Long examId) {
        return this.examDAO.byPK(examId)
                .map(exam -> {

                    if (!this.isScreenProctoringEnabled(exam.id)) {
                        return exam;
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Update exam data on screen proctoring service for exam: {}", exam);
                    }

                    this.notifyExamSaved(exam);
                    return exam;
                });
    }

    @Override
    public void updateClientConnections() {
        try {

            this.examDAO
                    .allIdsOfRunningWithScreenProctoringEnabled()
                    .flatMap(this.clientConnectionDAO::getAllForScreenProctoringUpdate)
                    .getOrThrow()
                    .forEach(this::applyScreenProctoringSession);

        } catch (final Exception e) {
            log.error("Failed to update active SEB connections for screen proctoring");
        }
    }

    @Override
    public void updateActiveGroups() {
        try {

            // only 
            if (!webserviceInfo.isMaster()) {
                return;
            }

            // TODO make this more performant (batch update, check if size has changed, caching...
            if (screenProctoringGroupDAO.hasActiveGroups()) {
                screenProctoringAPIBinding
                        .getActiveGroupSessionCounts()
                        .forEach(groupCount -> screenProctoringGroupDAO.updateGroupSize(
                                groupCount.groupUUID,
                                groupCount.activeCount,
                                groupCount.totalCount));
            }
        } catch (final Exception e) {
            log.warn("Failed to update actual group session counts.");
        }
    }

    @Override
    public void notifyExamSaved(final Exam exam) {
        if (!this.isScreenProctoringEnabled(exam.id)) {
            return;
        }

        this.screenProctoringAPIBinding.synchronizeUserAccounts(exam);
        this.screenProctoringAPIBinding.updateExam(exam);
        this.screenProctoringAPIBinding.synchronizeGroups(exam);
    }

    @Override
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    public void synchronizeSPSUser(final String userUUID) {

        if (!webserviceInfo.getScreenProctoringServiceBundle().bundled) {
            return;
        }

        this.screenProctoringAPIBinding.synchronizeUserAccount(userUUID);
    }

    @Override
    public void synchronizeSPSUserForExam(final Long examId) {
        this.examDAO
                .byPK(examId)
                .onSuccess(this.screenProctoringAPIBinding::synchronizeUserAccounts)
                .onError(error -> log.error("Failed to synchronize SPS user accounts for exam: {}", examId, error));
    }

    @Override
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    public void deleteSPSUser(final String userUUID) {
        if (!webserviceInfo.getScreenProctoringServiceBundle().bundled) {
            return;
        }

        this.screenProctoringAPIBinding.deleteSPSUser(userUUID);
    }

    @Override
    public void notifyExamStarted(final ExamStartedEvent event) {
        final Exam exam = event.exam;
        if (!this.isScreenProctoringEnabled(event.exam.id)) {
            return;
        }

        this.screenProctoringAPIBinding.activateScreenProctoring(exam);
    }

    @Override
    public void notifyExamFinished(final ExamFinishedEvent event) {
        if (!this.isScreenProctoringEnabled(event.exam.id)) {
            return;
        }

        if (event.exam.status != Exam.ExamStatus.UP_COMING) {
            this.screenProctoringAPIBinding.deactivateScreenProctoring(event.exam);
            this.screenProctoringGroupDAO.resetAllForExam(event.exam.id);
        }
    }

    @Override
    public void notifyExamReset(final ExamResetEvent event) {
        if (!this.isScreenProctoringEnabled(event.exam.id)) {
            return;
        }

        if (event.exam.status != Exam.ExamStatus.UP_COMING) {
            this.screenProctoringAPIBinding.activateScreenProctoring(event.exam);
        }
    }

    @Override
    public void notifyExamDeletion(final ExamDeletionEvent event) {
        event.ids
                .stream()
                .map(this::deleteForExam)
                .forEach(result -> {
                    if (result.hasError()) {
                        log.error("Failed to dispose SPS entities for exam: ", result.getError());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Successfully disposed SPS entities for exam: {}", result.get());
                        }
                    }
                });
    }

    @Override
    public void notifyLmsSetupChange(final LmsSetupChangeEvent event) {
        try {

            if (event.activation == Activatable.ActivationAction.NONE) {
                return;
            }

            examDAO.allActiveForLMSSetup(Arrays.asList(event.getLmsSetup().id))
                    .getOrThrow()
                    .forEach(exam -> {
                        if (screenProctoringAPIBinding.isSPSActive(exam)) {
                            if (event.activation == Activatable.ActivationAction.ACTIVATE) {
                                this.screenProctoringAPIBinding.activateScreenProctoring(exam)
                                        .onError(error -> log.warn("Failed to re-activate SPS for exam: {} error: {}",
                                                exam.name,
                                                error.getMessage()));
                            } else if (event.activation == Activatable.ActivationAction.DEACTIVATE) {
                                this.screenProctoringAPIBinding.deactivateScreenProctoring(exam)
                                        .onError(error -> log.warn("Failed to deactivate SPS for exam: {} error: {}",
                                                exam.name,
                                                error.getMessage()));
                            }
                        }
                    });

        } catch (final Exception e) {
            log.error("Failed to apply LMSSetup change activation/deactivation to Screen Proctoring: ", e);
        }
    }

    private void applyScreenProctoringSession(final ClientConnectionRecord ccRecord) {
        
        try {
            final Long examId = ccRecord.getExamId();
            final Exam runningExam = this.examSessionCacheService.getRunningExam(examId);
            final Long existingGroupId = ccRecord.getScreenProctoringGroupId();

            if (existingGroupId == null) {

                // apply SEB connection to screen proctoring group
                final ScreenProctoringGroup group = applySEBConnectionToGroup(
                        ccRecord,
                        runningExam);

                // create screen proctoring session for SEB connection on SPS service
                final String spsSessionToken = this.screenProctoringAPIBinding
                        .createSEBSession(examId, group, ccRecord);

                // create instruction for SEB and add it to instruction queue for SEB connection
                registerJoinInstruction(ccRecord, spsSessionToken, group, runningExam);
            } else {
                // just update session on SPS site
                this.screenProctoringGroupDAO
                        .getScreenProctoringGroup(existingGroupId)
                        .map(group -> this.screenProctoringAPIBinding.updateSEBSession(
                                group.id,
                                ccRecord))
                        .onError(error -> log.error("Failed to update SEB Session on SPS: {}", ccRecord, error));

            }

            this.clientConnectionDAO
                    .markScreenProctoringApplied(ccRecord.getId(), ccRecord.getConnectionToken())
                    .getOrThrow();

        } catch (final Exception e) {
            log.error("Failed to apply screen proctoring session to SEB with connection: {} cause: {}", ccRecord, e.getMessage());
        }
    }

    private ScreenProctoringGroup applySEBConnectionToGroup(
            final ClientConnectionRecord ccRecord,
            final Exam exam) {

        final ScreenProctoringSettings settingsForExam = this.screenProctoringAPIBinding.getSettingsForExam(exam);
        
        return switch (settingsForExam.collectingStrategy) {
            case APPLY_SEB_GROUPS -> applyToSEBClientGroup(ccRecord, exam);
            case EXAM -> applyToDefaultGroup(ccRecord.getId(), ccRecord.getConnectionToken(), exam);
        };
    }

    private ScreenProctoringGroup applyToDefaultGroup(
            final Long connectionId,
            final String connectionToken,
            final Exam exam) {

        final Collection<ScreenProctoringGroup> groups = screenProctoringGroupDAO
                .getCollectingGroups(exam.id)
                .getOrThrow();
        
        if (groups.isEmpty()) {
            throw new IllegalStateException("Exam has no local SPS Groups defined: " + exam);
        }

        ScreenProctoringGroup screenProctoringGroup = null;
        if (groups.size() == 1) {
            screenProctoringGroup = groups.iterator().next();
        } else {
            screenProctoringGroup = groups.stream()
                    .filter(group -> BooleanUtils.isTrue(group.isFallback))
                    .findFirst()
                    .orElseGet(null);
        }
        
        if (screenProctoringGroup == null) {
            throw new IllegalStateException("Exam has no local default or fallback SPS Groups defined: " + exam);
        }
        
        this.clientConnectionDAO.assignToScreenProctoringGroup(
                connectionId,
                connectionToken,
                screenProctoringGroup.id)
                .getOrThrow();

        return screenProctoringGroup;
    }

    private ScreenProctoringGroup applyToSEBClientGroup(
            final ClientConnectionRecord ccRecord,
            final Exam exam) {

        final ClientConnection clientConnection = ClientConnectionDAOImpl
                .toDomainModel(ccRecord)
                .getOrThrow();
        
        ScreenProctoringGroup defaultGroup = null;
        final Collection<ScreenProctoringGroup> groups = this.
                getCollectingGroups(exam.id)
                .getOrThrow();
        
        for (final ScreenProctoringGroup group : groups) {
            if (group.sebGroupId != null) {
                final ClientGroup clientGroup = clientGroupDAO
                        .byPK(group.sebGroupId)
                        .getOrThrow();
                if (this.clientGroupMatcherService.isInGroup(clientConnection, clientGroup)) {
                    this.clientConnectionDAO.assignToScreenProctoringGroup(
                                    clientConnection.id,
                                    clientConnection.connectionToken,
                                    group.id)
                            .getOrThrow();
                    return group;
                }
            } else {
                defaultGroup = group;
            }
        }
        
        if (defaultGroup != null) {
            this.clientConnectionDAO.assignToScreenProctoringGroup(
                            clientConnection.id,
                            clientConnection.connectionToken,
                            defaultGroup.id)
                    .getOrThrow();
            return defaultGroup;
        } else {
            return null;
        }
    }

    private Result<Exam> deleteForExam(final Long examId) {
        return Result.tryCatch(() -> {
            final Exam exam = this.examDAO.byPK(examId).getOrThrow();

            // Note: We delete the Exam on SPS site if there are no SEB client connection yet.
            //       Otherwise, the Exam on SPS site gets just closed

            final Collection<Long> sebConnections = clientConnectionDAO
                    .getAllConnectionIdsForExam(examId)
                    .getOr(Collections.emptyList());

            if (sebConnections == null || sebConnections.isEmpty()) {
                this.screenProctoringAPIBinding.deleteExamOnScreenProctoring(exam);
            } else {
                this.screenProctoringAPIBinding.deactivateScreenProctoring(exam)
                        .onError(error -> log.error("Failed to deactivate screen proctoring for exam: {}", exam.name, error));
            }

            return this.cleanupAllLocalGroups(exam);
        });
    }

    private Exam cleanupAllLocalGroups(final Exam exam) {
        return this.screenProctoringGroupDAO
                .deleteGroups(exam.id)
                .onSuccess(keys -> log.info("Deleted all screen proctoring groups for exam: {} groups: {}", exam, keys))
                .onError(error -> log.error("Failed to delete all groups for exam: {}", exam, error))
                .map(x -> exam)
                .getOrThrow();
    }

    private void registerJoinInstruction(
            final ClientConnectionRecord ccRecord,
            final String spsSessionToken,
            final ScreenProctoringGroup group,
            final Exam exam) {

        if (log.isDebugEnabled()) {
            log.debug("Register JOIN instruction for client ");
        }

        final boolean checkActive = exam.lmsSetupId != null;
        final SPSData spsData = this.screenProctoringAPIBinding.getSPSData(exam.id);
        final String url = screenProctoringServiceBundle.bundled
                ? screenProctoringServiceBundle.serviceURL
                : exam.additionalAttributes.get(ScreenProctoringSettings.ATTR_SPS_SERVICE_URL);
        final Map<String, String> attributes = new HashMap<>();

        attributes.put(SERVICE_TYPE, SERVICE_TYPE_NAME);
        attributes.put(METHOD, ClientInstruction.ProctoringInstructionMethod.JOIN.name());
        attributes.put(URL, url);
        attributes.put(CLIENT_ID, spsData.spsSEBAccessName);
        attributes.put(CLIENT_SECRET, spsData.spsSEBAccessPWD);
        attributes.put(GROUP_ID, group.uuid);
        attributes.put(SESSION_ID, spsSessionToken);

        this.sebInstructionService
                .registerInstruction(
                        exam.id,
                        InstructionType.SEB_PROCTORING,
                        attributes,
                        ccRecord.getConnectionToken(),
                        checkActive,
                        true)
                .onError(error -> log.error(
                        "Failed to register screen proctoring join instruction for SEB connection: {}",
                        ccRecord,
                        error));
    }

}
