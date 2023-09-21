/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ScreenProctoringGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ProctoringSettingsDAOImpl;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ScreenProctoringGroupDAOImpl.AllGroupsFullException;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;

@Lazy
@Service
@WebServiceProfile
public class ScreenProctoringServiceImpl implements ScreenProctoringService {

    private static final Logger log = LoggerFactory.getLogger(ScreenProctoringServiceImpl.class);

    private final Cryptor cryptor;
    private final ScreenProctoringAPIBinding screenProctoringAPIBinding;
    private final ScreenProctoringGroupDAO screenProctoringGroupDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ExamDAO examDAO;
    private final ProctoringSettingsDAOImpl proctoringSettingsSupplier;
    private final SEBClientInstructionService sebInstructionService;

    public ScreenProctoringServiceImpl(
            final Cryptor cryptor,
            final ProctoringSettingsDAOImpl proctoringSettingsSupplier,
            final AsyncService asyncService,
            final JSONMapper jsonMapper,
            final UserDAO userDAO,
            final ExamDAO examDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ScreenProctoringGroupDAO screenProctoringGroupDAO,
            final SEBClientInstructionService sebInstructionService) {

        this.cryptor = cryptor;
        this.examDAO = examDAO;
        this.proctoringSettingsSupplier = proctoringSettingsSupplier;
        this.screenProctoringGroupDAO = screenProctoringGroupDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.sebInstructionService = sebInstructionService;

        this.screenProctoringAPIBinding = new ScreenProctoringAPIBinding(
                userDAO,
                cryptor,
                asyncService,
                jsonMapper,
                proctoringSettingsSupplier,
                additionalAttributesDAO);
    }

    @Override
    public Result<ScreenProctoringSettings> testSettings(final ScreenProctoringSettings screenProctoringSettings) {
        return Result.tryCatch(() -> {

            if (screenProctoringSettings.spsServiceURL != null
                    && screenProctoringSettings.spsServiceURL.contains("?")) {
                throw new FieldValidationException(
                        "serverURL",
                        "screenProctoringSettings:spsServiceURL:invalidURL");
            }

            final Collection<APIMessage> fieldChecks = new ArrayList<>();
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
    public Result<ScreenProctoringSettings> applyScreenProctoingForExam(final ScreenProctoringSettings settings) {

        return this.examDAO
                .byPK(settings.examId)
                .map(exam -> {

                    if (BooleanUtils.toBoolean(settings.enableScreenProctoring)) {

                        this.screenProctoringAPIBinding
                                .startScreenProctoring(exam)
                                .getOrThrow()
                                .stream()
                                .forEach(newGroup -> createNewLocalGroup(exam, newGroup));
                    } else {

                        this.screenProctoringAPIBinding
                                .dispsoseScreenProctoring(exam)
                                .getOrThrow();
                    }

                    return settings;
                });
    }

    @Override
    public Result<Exam> updateExamOnScreenProctoingService(final Long examId) {
        return this.examDAO.byPK(examId)
                .map(exam -> {

                    if (log.isDebugEnabled()) {
                        log.debug("Update changed exam attributes for screen proctoring: {}", exam);
                    }

                    final String enabeld = exam.additionalAttributes
                            .get(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING);

                    if (!BooleanUtils.toBoolean(enabeld)) {
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

            final Map<Long, ExamInfo> examInfoCache = new HashMap<>();

            this.examDAO
                    .allIdsOfRunningWithScreenProctoringEnabled()
                    .flatMap(this.clientConnectionDAO::getAllForScreenProctoringUpdate)
                    .getOrThrow()
                    .stream()
                    .forEach(cc -> applyScreenProctoringSession(cc, examInfoCache));

        } catch (final Exception e) {
            log.error("Failed to update active SEB connections for screen proctoring");
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

    private void applyScreenProctoringSession(
            final ClientConnectionRecord ccRecord,
            final Map<Long, ExamInfo> examInfoCache) {

        try {

            final Long examId = ccRecord.getExamId();

            // process based caching...
            if (!examInfoCache.containsKey(examId)) {
                final ScreenProctoringSettings settings = this.proctoringSettingsSupplier
                        .getScreenProctoringSettings(new EntityKey(examId, EntityType.EXAM))
                        .getOrThrow();
                final ClientCredentials sebClientCredential = this.screenProctoringAPIBinding
                        .getSEBClientCredentials(examId)
                        .getOrThrow();

                examInfoCache.put(examId, new ExamInfo(settings, sebClientCredential));
            }

            final ExamInfo examInfo = examInfoCache.get(examId);

            // apply SEB connection to screen proctoring group
            final ScreenProctoringGroup group = applySEBConnectionToGroup(
                    ccRecord,
                    examInfo.screenProctoringSettings);

            // create screen proctoring session for SEB connection on SPS service
            final String spsSessionToken = this.screenProctoringAPIBinding
                    .createSEBSession(examId, group, ccRecord);

            // create instruction for SEB and add it to instruction queue for SEB connection
            registerJoinInstruction(ccRecord, spsSessionToken, group, examInfo);

        } catch (final Exception e) {
            log.error("Failed to apply screen proctoring session to SEB with connection: ", ccRecord, e);
        }

    }

    private void registerJoinInstruction(
            final ClientConnectionRecord ccRecord,
            final String spsSessionToken,
            final ScreenProctoringGroup group,
            final ExamInfo examInfo) {

        if (log.isDebugEnabled()) {
            log.debug("Register JOIN instruction for client ");
        }

        final Long examId = examInfo.screenProctoringSettings.examId;
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(SERVICE_TYPE, SERVICE_TYPE_NAME);
        attributes.put(METHOD, ClientInstruction.ProctoringInstructionMethod.JOIN.name());
        attributes.put(URL, examInfo.screenProctoringSettings.getSpsServiceURL());
        attributes.put(CLIENT_ID, examInfo.sebClientCredential.clientIdAsString());
        attributes.put(CLIENT_SECRET, this.cryptor.decrypt(examInfo.sebClientCredential.secret).toString());
        attributes.put(GROUP_ID, group.uuid);
        attributes.put(SESSION_ID, spsSessionToken);

        this.sebInstructionService
                .registerInstruction(
                        examId,
                        InstructionType.SEB_PROCTORING,
                        attributes,
                        ccRecord.getConnectionToken(),
                        true)
                .onError(error -> log.error(
                        "Failed to register screen proctoring join instruction for SEB connection: {}",
                        ccRecord,
                        error));

    }

    private ScreenProctoringGroup applySEBConnectionToGroup(
            final ClientConnectionRecord ccRecord,
            final ScreenProctoringSettings settings) {

        final Long examId = ccRecord.getExamId();
        switch (settings.collectingStrategy) {
            case SEB_GROUP: {
                // TODO
                throw new UnsupportedOperationException("SEB_GROUP based group collection is not supported yet");
            }
            case EXAM:
            case FIX_SIZE:
            default: {

                final ScreenProctoringGroup screenProctoringGroup = getProctoringGroup(settings);
                this.clientConnectionDAO.assignToScreenProctoringGroup(
                        examId,
                        ccRecord.getConnectionToken(),
                        screenProctoringGroup.id)
                        .getOrThrow();

                return screenProctoringGroup;
            }
        }
    }

    private ScreenProctoringGroup getProctoringGroup(final ScreenProctoringSettings settings) {

        final Result<ScreenProctoringGroup> reserve = this.screenProctoringGroupDAO
                .reservePlaceInCollectingGroup(
                        settings.examId,
                        settings.collectingGroupSize != null ? settings.collectingGroupSize : 0);

        ScreenProctoringGroup screenProctoringGroup = null;
        if (reserve.hasError()) {
            if (reserve.getError() instanceof AllGroupsFullException) {
                screenProctoringGroup = applyNewGroup(settings.examId, settings.collectingGroupSize);
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

    private ScreenProctoringGroup applyNewGroup(final Long examId, final Integer groupSize) {

        final Exam exam = this.examDAO.byPK(examId).getOrThrow();
        final String spsExamUUID = exam.getAdditionalAttribute(
                ScreenProctoringAPIBinding.SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_EXAM_UUID);

        return this.screenProctoringGroupDAO
                .getCollectingGroups(exam.id)
                .flatMap(count -> this.screenProctoringAPIBinding
                        .createGroup(spsExamUUID, count.size() + 1, "Created by SEB Server", exam))
                .flatMap(this.screenProctoringGroupDAO::createNewGroup)
                .flatMap(group -> this.screenProctoringGroupDAO
                        .reservePlaceInCollectingGroup(examId, groupSize != null ? groupSize : 0))
                .getOrThrow();
    }

    private Result<Exam> deleteForExam(final Long examId) {
        return this.examDAO.byPK(examId)
                .flatMap(this.screenProctoringAPIBinding::deleteScreenProctoring)
                .map(this::cleanupAllLocalGroups);
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

//    private Exam saveSettings(final Exam exam, final ScreenProctoringSettings settings) {
//
//        this.proctoringAdminService
//                .saveScreenProctoringSettings(exam.getEntityKey(), settings)
//                .getOrThrow();
//
//        return exam;
//    }

    private static final class ExamInfo {

        final ScreenProctoringSettings screenProctoringSettings;
        final ClientCredentials sebClientCredential;

        public ExamInfo(
                final ScreenProctoringSettings screenProctoringSettings,
                final ClientCredentials sebClientCredential) {

            this.screenProctoringSettings = screenProctoringSettings;
            this.sebClientCredential = sebClientCredential;
        }
    }

}
