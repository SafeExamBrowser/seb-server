/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;



import ch.ethz.seb.sebserver.gbl.model.exam.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamConfigurationValueService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ProctoringAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.RemoteProctoringService;

@Lazy
@Service
@WebServiceProfile
public class ExamAdminServiceImpl implements ExamAdminService {

    private static final Logger log = LoggerFactory.getLogger(ExamAdminServiceImpl.class);

    private final ExamDAO examDAO;
    private final ProctoringAdminService proctoringAdminService;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final ConfigurationNodeDAO configurationNodeDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final LmsAPIService lmsAPIService;
    private final ExamConfigurationValueService examConfigurationValueService;
    private final SEBRestrictionService sebRestrictionService;

    protected ExamAdminServiceImpl(
            final ExamDAO examDAO,
            final ProctoringAdminService proctoringAdminService,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ConfigurationNodeDAO configurationNodeDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final LmsAPIService lmsAPIService,
            final ExamConfigurationValueService examConfigurationValueService,
            final SEBRestrictionService sebRestrictionService) {

        this.examDAO = examDAO;
        this.proctoringAdminService = proctoringAdminService;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.configurationNodeDAO = configurationNodeDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.lmsAPIService = lmsAPIService;
        this.examConfigurationValueService = examConfigurationValueService;
        this.sebRestrictionService = sebRestrictionService;
    }

    @Override
    public ProctoringAdminService getProctoringAdminService() {
        return this.proctoringAdminService;
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
                        Exam.ADDITIONAL_ATTR_NUMERICAL_TRUST_THRESHOLD,
                        String.valueOf(statThreshold))
                        .onError(error -> log
                                .error("Failed to store ADDITIONAL_ATTR_NUMERICAL_TRUST_THRESHOLD: ", error));
            }

            this.examDAO.setModified(examId);

        }).flatMap(v -> this.examDAO.byPK(examId));
    }

    @Override
    public Result<Boolean> isRestricted(final Exam exam) {
        if (exam == null || exam.lmsSetupId == null) {
            return Result.of(false);
        }

        return this.lmsAPIService
                .getLmsAPITemplate(exam.lmsSetupId)
                .map(lmsAPI -> lmsAPI.hasSEBClientRestriction(exam))
                .onError(error -> log.warn("Failed to check SEB restriction: {}", error.getMessage()));
    }

    @Override
    public void updateAdditionalExamConfigAttributes(final Long examId) {
        try {
            final String allowedSEBVersion = this.examConfigurationValueService
                    .getAllowedSEBVersion(examId);

            if (StringUtils.isNotBlank(allowedSEBVersion)) {
                this.additionalAttributesDAO.saveAdditionalAttribute(
                        EntityType.EXAM,
                        examId, Exam.ADDITIONAL_ATTR_ALLOWED_SEB_VERSIONS,
                        allowedSEBVersion)
                        .getOrThrow();
            } else {
                this.additionalAttributesDAO.delete(
                        EntityType.EXAM,
                        examId, Exam.ADDITIONAL_ATTR_ALLOWED_SEB_VERSIONS);
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to save additional Exam Configuration settings for exam: {}",
                    examId, e);
        }
    }

    @Override
    public Result<ProctoringServiceSettings> getProctoringServiceSettings(final Long examId) {
        return this.proctoringAdminService
                .getProctoringSettings(new EntityKey(examId, EntityType.EXAM));
    }

    @Override
    public Result<ProctoringServiceSettings> saveProctoringServiceSettings(
            final Long examId,
            final ProctoringServiceSettings proctoringServiceSettings) {

        return this.proctoringAdminService
                .testProctoringSettings(proctoringServiceSettings)
                .flatMap(test -> this.proctoringAdminService
                        .saveProctoringServiceSettings(
                                new EntityKey(examId, EntityType.EXAM),
                                proctoringServiceSettings))
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
    public Result<Boolean> isScreenProctoringEnabled(final Long examId) {
        return this.additionalAttributesDAO.getAdditionalAttribute(
                EntityType.EXAM,
                examId,
                ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING)
                .map(rec -> BooleanUtils.toBoolean(rec.getValue()))
                .onErrorDo(error -> {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to verify screen proctoring enabled for exam: {}, {}",
                                examId,
                                error.getMessage());
                    }
                    return false;
                });
    }

    @Override
    public Result<RemoteProctoringService> getExamProctoringService(final Long examId) {
        return getProctoringServiceSettings(examId)
                .flatMap(settings -> this.proctoringAdminService
                        .getExamProctoringService(settings.serverType));
    }

    @Override
    public Result<Exam> resetProctoringSettings(final Exam exam) {

        // first delete all proctoring settings

        return getProctoringServiceSettings(exam.id)
                .map(settings -> {
                    final ProctoringServiceSettings resetSettings;
                    if (exam.examTemplateId != null) {
                        // get settings from origin template
                        resetSettings = this.proctoringAdminService
                                .getProctoringSettings(new EntityKey(exam.examTemplateId, EntityType.EXAM_TEMPLATE))
                                .map(template -> new ProctoringServiceSettings(exam.id, template))
                                .getOr(new ProctoringServiceSettings(exam.id));
                    } else {
                        // create new reseted settings
                        resetSettings = new ProctoringServiceSettings(exam.id);
                    }
                    return resetSettings;
                }).flatMap(settings -> saveProctoringServiceSettings(exam.id, settings))
                .map(settings -> exam);
    }

    @Override
    public Result<Exam>  notifyExamSaved(final Exam exam) {
        return Result.tryCatch(() -> {
            updateAdditionalExamConfigAttributes(exam.id);
            this.proctoringAdminService.notifyExamSaved(exam);
            return exam;
        });
    }

    @Override
    public Result<Exam> applyQuitPassword(final Exam exam) {
        return this.sebRestrictionService.applyQuitPassword(exam);
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
                            log.debug("Also set exam configuration to archived: {}", configNodeId);
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
