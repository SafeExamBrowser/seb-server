/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringFeature;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.RemoteProctoringRoomDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ExamProctoringServiceFactory;

@Lazy
@Service
@WebServiceProfile
public class ExamAdminServiceImpl implements ExamAdminService {

    private static final Logger log = LoggerFactory.getLogger(ExamAdminServiceImpl.class);

    private final ExamDAO examDAO;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final LmsAPIService lmsAPIService;
    private final Cryptor cryptor;
    private final ExamProctoringServiceFactory examProctoringServiceFactory;
    private final RemoteProctoringRoomDAO remoteProctoringRoomDAO;

    protected ExamAdminServiceImpl(
            final ExamDAO examDAO,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final LmsAPIService lmsAPIService,
            final Cryptor cryptor,
            final ExamProctoringServiceFactory examProctoringServiceFactory,
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO) {

        this.examDAO = examDAO;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.lmsAPIService = lmsAPIService;
        this.cryptor = cryptor;
        this.examProctoringServiceFactory = examProctoringServiceFactory;
        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
    }

    @Override
    public Result<Exam> examForPK(final Long examId) {
        return this.examDAO.byPK(examId);
    }

    @Override
    public Result<Exam> applyAdditionalSEBRestrictions(final Exam exam) {
        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Apply additional SEB restrictions for exam: {}",
                        exam.externalId);
            }

            final LmsSetup lmsSetup = this.lmsAPIService
                    .getLmsSetup(exam.lmsSetupId)
                    .getOrThrow();

            if (lmsSetup.lmsType == LmsType.OPEN_EDX) {
                final List<String> permissions = Arrays.asList(
                        OpenEdxSEBRestriction.PermissionComponent.ALWAYS_ALLOW_STAFF.key,
                        OpenEdxSEBRestriction.PermissionComponent.CHECK_CONFIG_KEY.key);

                this.additionalAttributesDAO.saveAdditionalAttribute(
                        EntityType.EXAM,
                        exam.id,
                        SEBRestrictionService.SEB_RESTRICTION_ADDITIONAL_PROPERTY_NAME_PREFIX +
                                OpenEdxSEBRestriction.ATTR_PERMISSION_COMPONENTS,
                        StringUtils.join(permissions, Constants.LIST_SEPARATOR_CHAR))
                        .getOrThrow();
            }

            return this.examDAO
                    .byPK(exam.id)
                    .getOrThrow();
        });
    }

    @Override
    public Result<Exam> saveLMSAttributes(final Exam exam) {
        return saveAdditionalAttributesForMoodleExams(exam);
    }

    @Override
    public Result<Boolean> isRestricted(final Exam exam) {
        if (exam == null) {
            return Result.of(false);
        }

        return this.lmsAPIService
                .getLmsAPITemplate(exam.lmsSetupId)
                .map(lmsAPI -> !lmsAPI.getSEBClientRestriction(exam).hasError());
    }

    @Override
    public Result<ProctoringServiceSettings> getProctoringServiceSettings(final Long examId) {
        return this.additionalAttributesDAO.getAdditionalAttributes(EntityType.EXAM, examId)
                .map(attrs -> attrs.stream()
                        .collect(Collectors.toMap(
                                attr -> attr.getName(),
                                Function.identity())))
                .map(mapping -> {
                    return new ProctoringServiceSettings(
                            examId,
                            getEnabled(mapping),
                            getServerType(mapping),
                            getString(mapping, ProctoringServiceSettings.ATTR_SERVER_URL),
                            getCollectingRoomSize(mapping),
                            getEnabledFeatures(mapping),
                            this.remoteProctoringRoomDAO.isServiceInUse(examId).getOr(true),
                            getString(mapping, ProctoringServiceSettings.ATTR_APP_KEY),
                            getString(mapping, ProctoringServiceSettings.ATTR_APP_SECRET),
                            getString(mapping, ProctoringServiceSettings.ATTR_SDK_KEY),
                            getString(mapping, ProctoringServiceSettings.ATTR_SDK_SECRET),
                            getBoolean(mapping, ProctoringServiceSettings.ATTR_USE_ZOOM_APP_CLIENT_COLLECTING_ROOM));
                });
    }

    @Override
    @Transactional
    public Result<ProctoringServiceSettings> saveProctoringServiceSettings(
            final Long examId,
            final ProctoringServiceSettings proctoringServiceSettings) {

        return Result.tryCatch(() -> {

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    ProctoringServiceSettings.ATTR_ENABLE_PROCTORING,
                    String.valueOf(proctoringServiceSettings.enableProctoring));

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    ProctoringServiceSettings.ATTR_SERVER_TYPE,
                    proctoringServiceSettings.serverType.name());

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    ProctoringServiceSettings.ATTR_SERVER_URL,
                    StringUtils.trim(proctoringServiceSettings.serverURL));

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    ProctoringServiceSettings.ATTR_COLLECTING_ROOM_SIZE,
                    String.valueOf(proctoringServiceSettings.collectingRoomSize));

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    ProctoringServiceSettings.ATTR_APP_KEY,
                    StringUtils.trim(proctoringServiceSettings.appKey));

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    ProctoringServiceSettings.ATTR_APP_SECRET,
                    this.cryptor.encrypt(Utils.trim(proctoringServiceSettings.appSecret))
                            .getOrThrow()
                            .toString());

            if (StringUtils.isNotBlank(proctoringServiceSettings.sdkKey)) {
                this.additionalAttributesDAO.saveAdditionalAttribute(
                        EntityType.EXAM,
                        examId,
                        ProctoringServiceSettings.ATTR_SDK_KEY,
                        StringUtils.trim(proctoringServiceSettings.sdkKey));

                this.additionalAttributesDAO.saveAdditionalAttribute(
                        EntityType.EXAM,
                        examId,
                        ProctoringServiceSettings.ATTR_SDK_SECRET,
                        this.cryptor.encrypt(Utils.trim(proctoringServiceSettings.sdkSecret))
                                .getOrThrow()
                                .toString());
            }

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    ProctoringServiceSettings.ATTR_ENABLED_FEATURES,
                    StringUtils.join(proctoringServiceSettings.enabledFeatures, Constants.LIST_SEPARATOR));

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    ProctoringServiceSettings.ATTR_USE_ZOOM_APP_CLIENT_COLLECTING_ROOM,
                    String.valueOf(proctoringServiceSettings.useZoomAppClientForCollectingRoom));

            // Mark the involved exam as updated to notify changes
            this.examDAO.setModified(examId);

            return proctoringServiceSettings;
        });
    }

    @Override
    public Result<Boolean> isProctoringEnabled(final Long examId) {
        return this.additionalAttributesDAO.getAdditionalAttribute(
                EntityType.EXAM,
                examId,
                ProctoringServiceSettings.ATTR_ENABLE_PROCTORING)
                .map(rec -> BooleanUtils.toBoolean(rec.getValue()))
                .onErrorDo(error -> {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to verify proctoring enabled for exam: {}, {}",
                                examId,
                                error.getMessage());
                    }
                    return false;
                });
    }

    @Override
    public Result<ExamProctoringService> getExamProctoringService(final ProctoringServerType type) {
        return this.examProctoringServiceFactory
                .getExamProctoringService(type);
    }

    private Boolean getEnabled(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING)) {
            return BooleanUtils.toBoolean(mapping.get(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING).getValue());
        } else {
            return false;
        }
    }

    private ProctoringServerType getServerType(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_SERVER_TYPE)) {
            return ProctoringServerType.valueOf(mapping.get(ProctoringServiceSettings.ATTR_SERVER_TYPE).getValue());
        } else {
            return ProctoringServerType.JITSI_MEET;
        }
    }

    private String getString(final Map<String, AdditionalAttributeRecord> mapping, final String name) {
        if (mapping.containsKey(name)) {
            return mapping.get(name).getValue();
        } else {
            return null;
        }
    }

    private Boolean getBoolean(final Map<String, AdditionalAttributeRecord> mapping, final String name) {
        if (mapping.containsKey(name)) {
            return BooleanUtils.toBooleanObject(mapping.get(name).getValue());
        } else {
            return false;
        }
    }

    private Integer getCollectingRoomSize(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_COLLECTING_ROOM_SIZE)) {
            return Integer.valueOf(mapping.get(ProctoringServiceSettings.ATTR_COLLECTING_ROOM_SIZE).getValue());
        } else {
            return 20;
        }
    }

    private EnumSet<ProctoringFeature> getEnabledFeatures(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_ENABLED_FEATURES)) {
            try {
                final String value = mapping.get(ProctoringServiceSettings.ATTR_ENABLED_FEATURES).getValue();
                return StringUtils.isNotBlank(value)
                        ? EnumSet.copyOf(Arrays.asList(StringUtils.split(value, Constants.LIST_SEPARATOR))
                                .stream()
                                .map(str -> {
                                    try {
                                        return ProctoringFeature.valueOf(str);
                                    } catch (final Exception e) {
                                        log.error(
                                                "Failed to enabled single features for proctoring settings. Skipping. {}",
                                                e.getMessage());
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                        : EnumSet.noneOf(ProctoringFeature.class);
            } catch (final Exception e) {
                log.error("Failed to get enabled features for proctoring settings. Enable all. {}", e.getMessage());
                return EnumSet.allOf(ProctoringFeature.class);
            }
        } else {
            return EnumSet.allOf(ProctoringFeature.class);
        }
    }

    private Result<Exam> saveAdditionalAttributesForMoodleExams(final Exam exam) {
        return Result.tryCatch(() -> {
            final LmsAPITemplate lmsTemplate = this.lmsAPIService
                    .getLmsAPITemplate(exam.lmsSetupId)
                    .getOrThrow();

            if (lmsTemplate.lmsSetup().lmsType == LmsType.MOODLE) {
                lmsTemplate.getQuiz(exam.externalId)
                        .flatMap(quizData -> this.additionalAttributesDAO.saveAdditionalAttribute(
                                EntityType.EXAM,
                                exam.id,
                                QuizData.QUIZ_ATTR_NAME,
                                quizData.name))
                        .getOrThrow();
            }

            return exam;
        });
    }

}
