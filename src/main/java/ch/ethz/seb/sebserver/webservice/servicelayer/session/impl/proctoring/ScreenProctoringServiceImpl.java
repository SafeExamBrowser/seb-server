/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import static ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_SCREEN_PROCTORING.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ProctoringSettingsDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ScreenProctoringGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ScreenProctoringGroupDAOImpl.AllGroupsFullException;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamFinishedEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamStartedEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ScreenProctoringAPIBinding.SPSData;

@Lazy
@Service
@WebServiceProfile
public class ScreenProctoringServiceImpl implements ScreenProctoringService {

    private static final Logger log = LoggerFactory.getLogger(ScreenProctoringServiceImpl.class);

    private final Cryptor cryptor;
    private final ScreenProctoringAPIBinding screenProctoringAPIBinding;
    private final ScreenProctoringGroupDAO screenProctoringGroupDAO;
    private final ProctoringSettingsDAO proctoringSettingsDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamDAO examDAO;
    private final SEBClientInstructionService sebInstructionService;
    private final ExamSessionCacheService examSessionCacheService;
    private final WebserviceInfo webserviceInfo;

    public ScreenProctoringServiceImpl(
            final Cryptor cryptor,
            final ProctoringSettingsDAO proctoringSettingsDAO,
            final AsyncService asyncService,
            final JSONMapper jsonMapper,
            final UserDAO userDAO,
            final ExamDAO examDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ScreenProctoringGroupDAO screenProctoringGroupDAO,
            final SEBClientInstructionService sebInstructionService,
            final ExamSessionCacheService examSessionCacheService,
            final WebserviceInfo webserviceInfo) {

        this.cryptor = cryptor;
        this.examDAO = examDAO;
        this.screenProctoringGroupDAO = screenProctoringGroupDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.sebInstructionService = sebInstructionService;
        this.examSessionCacheService = examSessionCacheService;
        this.proctoringSettingsDAO = proctoringSettingsDAO;
        this.webserviceInfo = webserviceInfo;
        this.screenProctoringAPIBinding = new ScreenProctoringAPIBinding(
                userDAO,
                cryptor,
                asyncService,
                jsonMapper,
                proctoringSettingsDAO,
                additionalAttributesDAO,
                webserviceInfo);
    }

    @Override
    public Result<ScreenProctoringSettings> testSettings(final ScreenProctoringSettings screenProctoringSettings) {
        return Result.tryCatch(() -> {

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
                    screenProctoringSettings.collectingGroupSize);

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
                    final boolean isEnabling = this.proctoringSettingsDAO.isScreenProctoringEnabled(exam.id);

                    if (isEnabling && !isSPSActive) {

                        this.screenProctoringAPIBinding
                                .startScreenProctoring(exam)
                                .onError(error -> log.error(
                                        "Failed to apply screen proctoring for exam: {}",
                                        exam,
                                        error))
                                .getOrThrow()
                                .stream()
                                .forEach(newGroup -> createNewLocalGroup(exam, newGroup));

                        this.examDAO.markUpdate(exam.id);

                    } else if (!isEnabling && isSPSActive) {

                        this.screenProctoringAPIBinding
                                .disposeScreenProctoring(exam)
                                .onError(error -> log.error("Failed to dispose screen proctoring for exam: {}",
                                        exam,
                                        error))
                                .getOrThrow();

                        this.examDAO.markUpdate(exam.id);
                    }
                    return exam;
                });
    }

    @Override
    public Result<Collection<ScreenProctoringGroup>> getCollectingGroups(final Long examId) {
        return this.screenProctoringGroupDAO.getCollectingGroups(examId);
    }

    @Override
    public Result<Exam> updateExamOnScreenProctoringService(final Long examId) {
        return this.examDAO.byPK(examId)
                .map(exam -> {

                    final String enabled = exam.additionalAttributes
                            .get(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING);

                    if (!BooleanUtils.toBoolean(enabled)) {
                        return exam;
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Update exam data on screen proctoring service for exam: {}", exam);
                    }

                    this.screenProctoringAPIBinding.updateExam(exam);
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
                    .stream()
                    .forEach(this::applyScreenProctoringSession);

        } catch (final Exception e) {
            log.error("Failed to update active SEB connections for screen proctoring");
        }
    }

    @Override
    public void notifyExamSaved(final Exam exam) {
        final String enabled = exam.additionalAttributes
                .get(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING);

        if (!BooleanUtils.toBoolean(enabled)) {
            return;
        }

        this.screenProctoringAPIBinding.synchronizeUserAccounts(exam);
        this.screenProctoringAPIBinding.createExamReadPrivileges(exam);
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
        if (!BooleanUtils.toBoolean(exam.additionalAttributes.get(SPSData.ATTR_SPS_ACTIVE))) {
            return;
        }

        this.screenProctoringAPIBinding.activateSEBAccessOnSPS(exam, true);
    }

    @Override
    public void notifyExamFinished(final ExamFinishedEvent event) {
        final Exam exam = event.exam;
        if (!BooleanUtils.toBoolean(exam.additionalAttributes.get(SPSData.ATTR_SPS_ACTIVE))) {
            return;
        }

        this.screenProctoringAPIBinding.activateSEBAccessOnSPS(exam, false);
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

    private void applyScreenProctoringSession(final ClientConnectionRecord ccRecord) {

        Long placeReservedInGroup = null;

        try {
            final Long examId = ccRecord.getExamId();
            final Exam runningExam = this.examSessionCacheService.getRunningExam(examId);

            // apply SEB connection to screen proctoring group
            final ScreenProctoringGroup group = applySEBConnectionToGroup(
                    ccRecord,
                    runningExam);
            placeReservedInGroup = group.id;

            // create screen proctoring session for SEB connection on SPS service
            final String spsSessionToken = this.screenProctoringAPIBinding
                    .createSEBSession(examId, group, ccRecord);

            // create instruction for SEB and add it to instruction queue for SEB connection
            registerJoinInstruction(ccRecord, spsSessionToken, group, runningExam);

            this.clientConnectionDAO
                    .markScreenProctoringApplied(ccRecord.getId(), ccRecord.getConnectionToken())
                    .getOrThrow();

        } catch (final Exception e) {
            log.error("Failed to apply screen proctoring session to SEB with connection: {}", ccRecord, e);

            if (placeReservedInGroup != null) {
                // release reserved place in group
                this.screenProctoringGroupDAO.releasePlaceInCollectingGroup(
                        ccRecord.getExamId(),
                        placeReservedInGroup)
                        .onError(
                                error -> log.warn("Failed to release reserved place in group: {}", error.getMessage()));
            }
        }
    }

    private ScreenProctoringGroup applySEBConnectionToGroup(
            final ClientConnectionRecord ccRecord,
            final Exam exam) {

        if (!exam.additionalAttributes.containsKey(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY)) {
            log.warn("Can't verify collecting strategy for exam: {} use default group assignment.", exam.id);
            return applyToDefaultGroup(ccRecord.getId(), ccRecord.getConnectionToken(), exam);
        }

        final CollectingStrategy strategy = CollectingStrategy.valueOf(exam.additionalAttributes
                .get(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY));

        switch (strategy) {
            case SEB_GROUP: {
                // TODO
                throw new UnsupportedOperationException("SEB_GROUP based group collection is not supported yet");
            }
            case EXAM:
            case FIX_SIZE:
            default: {
                return applyToDefaultGroup(ccRecord.getId(), ccRecord.getConnectionToken(), exam);
            }
        }

    }

    private ScreenProctoringGroup applyToDefaultGroup(
            final Long connectionId,
            final String connectionToken,
            final Exam exam) {

        final ScreenProctoringGroup screenProctoringGroup = reservePlaceOnProctoringGroup(exam);
        this.clientConnectionDAO.assignToScreenProctoringGroup(
                connectionId,
                connectionToken,
                screenProctoringGroup.id)
                .getOrThrow();

        return screenProctoringGroup;
    }

    private ScreenProctoringGroup reservePlaceOnProctoringGroup(final Exam exam) {

        int collectingGroupSize = 0;
        if (exam.additionalAttributes.containsKey(ScreenProctoringSettings.ATTR_COLLECTING_GROUP_SIZE)) {
            collectingGroupSize = Integer.parseInt(exam.additionalAttributes
                    .get(ScreenProctoringSettings.ATTR_COLLECTING_GROUP_SIZE));
        }

        final Result<ScreenProctoringGroup> reserve = this.screenProctoringGroupDAO
                .reservePlaceInCollectingGroup(
                        exam.id,
                        collectingGroupSize);

        ScreenProctoringGroup screenProctoringGroup = null;
        if (reserve.hasError()) {
            if (reserve.getError() instanceof AllGroupsFullException) {
                screenProctoringGroup = applyNewGroup(exam, collectingGroupSize);
            } else {
                throw new RuntimeException(
                        "Failed to create new screen proctoring group: ",
                        reserve.getError());
            }
        } else {
            screenProctoringGroup = reserve.get();
        }
        return screenProctoringGroup;
    }

    private ScreenProctoringGroup applyNewGroup(final Exam exam, final Integer groupSize) {

        final String spsExamUUID = this.screenProctoringAPIBinding
                .getSPSData(exam.id)
                .spsExamUUID;

        return this.screenProctoringGroupDAO
                .getCollectingGroups(exam.id)
                .flatMap(count -> this.screenProctoringAPIBinding
                        .createGroup(spsExamUUID, count.size() + 1, "Created by SEB Server", exam))
                .flatMap(this.screenProctoringGroupDAO::createNewGroup)
                .flatMap(group -> this.screenProctoringGroupDAO
                        .reservePlaceInCollectingGroup(exam.id, groupSize != null ? groupSize : 0))
                .getOrThrow();
    }

    private Result<Exam> deleteForExam(final Long examId) {
        return this.examDAO
                .byPK(examId)
                .flatMap(this.screenProctoringAPIBinding::deleteScreenProctoring)
                .map(this::cleanupAllLocalGroups)
                .onError(error -> log.error("Failed to delete SPS integration for exam: {}", examId, error));
    }

    private Exam cleanupAllLocalGroups(final Exam exam) {
        return this.screenProctoringGroupDAO
                .deleteGroups(exam.id)
                .onSuccess(keys -> log.info("Deleted all screen proctoring groups for exam: {} groups: {}", exam, keys))
                .onError(error -> log.error("Failed to delete all groups for exam: {}", exam, error))
                .map(x -> exam)
                .getOrThrow();
    }

    private void createNewLocalGroup(final Exam exam, final ScreenProctoringGroup newGroup) {

        if (log.isDebugEnabled()) {
            log.debug(
                    "Create new local screen proctoring group for exam: {}, group: {}",
                    exam.externalId, newGroup);
        }

        this.screenProctoringGroupDAO
                .createNewGroup(newGroup)
                .onError(error -> log.error("Failed to create local screen proctoring group: {}",
                        newGroup, error));
    }

    private void registerJoinInstruction(
            final ClientConnectionRecord ccRecord,
            final String spsSessionToken,
            final ScreenProctoringGroup group,
            final Exam exam) {

        if (log.isDebugEnabled()) {
            log.debug("Register JOIN instruction for client ");
        }

        final SPSData spsData = this.screenProctoringAPIBinding.getSPSData(exam.id);
        final String url = exam.additionalAttributes.get(ScreenProctoringSettings.ATTR_SPS_SERVICE_URL);
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
                        true,
                        true)
                .onError(error -> log.error(
                        "Failed to register screen proctoring join instruction for SEB connection: {}",
                        ccRecord,
                        error));
    }
}
