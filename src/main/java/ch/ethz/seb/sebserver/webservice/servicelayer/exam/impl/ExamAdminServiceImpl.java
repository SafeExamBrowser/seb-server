/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.NoResourceFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ProctoringAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;

@Lazy
@Service
@WebServiceProfile
public class ExamAdminServiceImpl implements ExamAdminService {

    private static final Logger log = LoggerFactory.getLogger(ExamAdminServiceImpl.class);

    private final ExamDAO examDAO;
    private final ProctoringAdminService proctoringServiceSettingsService;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final ConfigurationNodeDAO configurationNodeDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final LmsAPIService lmsAPIService;

    protected ExamAdminServiceImpl(
            final ExamDAO examDAO,
            final ProctoringAdminService proctoringServiceSettingsService,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ConfigurationNodeDAO configurationNodeDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final LmsAPIService lmsAPIService) {

        this.examDAO = examDAO;
        this.proctoringServiceSettingsService = proctoringServiceSettingsService;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.configurationNodeDAO = configurationNodeDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.lmsAPIService = lmsAPIService;
    }

    @Override
    public Result<Exam> examForPK(final Long examId) {
        return this.examDAO.byPK(examId);
    }

    @Override
    public Result<Exam> saveSecurityKeySettings(
            final Long institutionId,
            final Long examId,
            final Boolean enabled,
            final Integer statThreshold) {

        return Result.tryCatch(() -> {
            if (enabled != null) {
                this.additionalAttributesDAO.saveAdditionalAttribute(
                        EntityType.EXAM,
                        examId,
                        Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED,
                        String.valueOf(enabled))
                        .onError(error -> log.error("Failed to store ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED: ",
                                error));
            }
            if (statThreshold != null) {
                this.additionalAttributesDAO.saveAdditionalAttribute(
                        EntityType.EXAM,
                        examId,
                        Exam.ADDITIONAL_ATTR_STATISTICAL_GRANT_COUNT_THRESHOLD,
                        String.valueOf(statThreshold))
                        .onError(error -> log
                                .error("Failed to store ADDITIONAL_ATTR_STATISTICAL_GRANT_COUNT_THRESHOLD: ", error));
            }

            this.examDAO.setModified(examId);

        }).flatMap(v -> this.examDAO.byPK(examId));
    }

    @Override
    public Result<String> getAppSignatureKeySalt(final Long institutionId, final Long examId) {
        return this.additionalAttributesDAO.getAdditionalAttribute(
                EntityType.EXAM,
                examId,
                Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_SALT)
                .onErrorDo(error -> {
                    if (error instanceof NoResourceFoundException) {
                        final CharSequence salt = KeyGenerators.string().generateKey();
                        return this.additionalAttributesDAO.saveAdditionalAttribute(
                                EntityType.EXAM,
                                examId,
                                Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_SALT, salt.toString()).getOrThrow();
                    } else {
                        throw new RuntimeException(
                                "Unexpected error while trying to get AppSigKey Salt for Exam: " + examId, error);
                    }
                })
                .map(AdditionalAttributeRecord::getValue);
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
                .map(lmsAPI -> lmsAPI.hasSEBClientRestriction(exam))
                .onError(error -> log.error("Failed to check SEB restriction: ", error));
    }

    @Override
    public Result<ProctoringServiceSettings> getProctoringServiceSettings(final Long examId) {
        return this.proctoringServiceSettingsService.getProctoringSettings(new EntityKey(examId, EntityType.EXAM));
    }

    @Override
    @Transactional
    public Result<ProctoringServiceSettings> saveProctoringServiceSettings(
            final Long examId,
            final ProctoringServiceSettings proctoringServiceSettings) {

        return this.proctoringServiceSettingsService
                .saveProctoringServiceSettings(
                        new EntityKey(examId, EntityType.EXAM),
                        proctoringServiceSettings)
                .map(settings -> {
                    this.examDAO.setModified(examId);
                    return settings;
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
    public Result<ExamProctoringService> getExamProctoringService(final Long examId) {
        return getProctoringServiceSettings(examId)
                .flatMap(settings -> this.proctoringServiceSettingsService
                        .getExamProctoringService(settings.serverType));
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

    @Override
    public Result<Exam> archiveExam(final Exam exam) {
        return Result.tryCatch(() -> {

            if (exam.status != ExamStatus.FINISHED) {
                throw new APIMessageException(
                        APIMessage.ErrorMessage.INTEGRITY_VALIDATION.of("Exam is in wrong status to archive."));
            }

            if (log.isDebugEnabled()) {
                log.debug("Archiving exam: {}", exam);
            }

            if (this.isRestricted(exam).getOr(false)) {

                if (log.isDebugEnabled()) {
                    log.debug("Archiving exam, SEB restriction still active, try to release: {}", exam);
                }

                this.lmsAPIService
                        .getLmsAPITemplate(exam.lmsSetupId)
                        .flatMap(lms -> lms.releaseSEBClientRestriction(exam))
                        .onError(error -> log.error(
                                "Failed to release SEB client restriction for archiving exam: ",
                                error));
            }

            final Exam result = this.examDAO
                    .updateState(exam.id, ExamStatus.ARCHIVED, null)
                    .getOrThrow();

            this.examConfigurationMapDAO
                    .getConfigurationNodeIds(result.id)
                    .getOrThrow()
                    .stream()
                    .forEach(configNodeId -> {
                        if (this.examConfigurationMapDAO.checkNoActiveExamReferences(configNodeId).getOr(false)) {
                            log.debug("Also set exam configuration to archived: ", configNodeId);
                            this.configurationNodeDAO.save(
                                    new ConfigurationNode(
                                            configNodeId, null, null, null, null, null,
                                            null, ConfigurationStatus.ARCHIVED, null, null))
                                    .onError(error -> log.error("Failed to set exam configuration to archived state: ",
                                            error));
                        }
                    });

            return result;
        });
    }

}
