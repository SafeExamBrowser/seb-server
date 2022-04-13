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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
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
    private final LmsAPIService lmsAPIService;

    protected ExamAdminServiceImpl(
            final ExamDAO examDAO,
            final ProctoringAdminService proctoringServiceSettingsService,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final LmsAPIService lmsAPIService) {

        this.examDAO = examDAO;
        this.proctoringServiceSettingsService = proctoringServiceSettingsService;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.lmsAPIService = lmsAPIService;
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

}
