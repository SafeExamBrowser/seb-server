/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamImportService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class ExamImportServiceImpl implements ExamImportService {

    private static final Logger log = LoggerFactory.getLogger(ExamImportServiceImpl.class);

    private final ExamDAO examDAO;
    private final ExamTemplateService examTemplateService;
    private final ExamAdminService examAdminService;
    private final SEBRestrictionService sebRestrictionService;
    private final boolean appSignatureKeyEnabled;
    private final int defaultNumericalTrustThreshold;

    public ExamImportServiceImpl(
            final ExamDAO examDAO,
            final ExamTemplateService examTemplateService,
            final SEBRestrictionService sebRestrictionService,
            final ExamAdminService examAdminService,
            final SEBRestrictionService sebRestrictionService1,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final LmsAPIService lmsAPIService,
            final @Value("${sebserver.webservice.api.admin.exam.app.signature.key.enabled:false}") boolean appSignatureKeyEnabled,
            final @Value("${sebserver.webservice.api.admin.exam.app.signature.key.numerical.threshold:2}") int defaultNumericalTrustThreshold) {

        this.examDAO = examDAO;
        this.examTemplateService = examTemplateService;
        this.examAdminService = examAdminService;
        this.sebRestrictionService = sebRestrictionService1;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.lmsAPIService = lmsAPIService;
        this.appSignatureKeyEnabled = appSignatureKeyEnabled;
        this.defaultNumericalTrustThreshold = defaultNumericalTrustThreshold;
    }

    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final LmsAPIService lmsAPIService;



    @Override
    public Result<Exam> applyExamImportInitialization(final Exam exam) {
        final List<APIMessage> errors = new ArrayList<>();

        this.initAdditionalAttributes(exam)
                .onError(error -> errors.add(APIMessage.ErrorMessage.EXAM_IMPORT_ERROR_AUTO_ATTRIBUTES.of(error)))
                .flatMap(this.examTemplateService::addDefinedIndicators)
                .onError(error -> errors.add(APIMessage.ErrorMessage.EXAM_IMPORT_ERROR_AUTO_INDICATOR.of(error)))
                .flatMap(this.examTemplateService::addDefinedClientGroups)
                .onError(error -> errors.add(APIMessage.ErrorMessage.EXAM_IMPORT_ERROR_AUTO_CLIENT_GROUPS.of(error)))
                .flatMap(this.examTemplateService::initAdditionalTemplateAttributes)
                .onError(error -> errors.add(APIMessage.ErrorMessage.EXAM_IMPORT_ERROR_AUTO_ATTRIBUTES.of(error)))
                .flatMap(this.examTemplateService::initExamConfiguration)
                .onError(error -> {
                    if (error instanceof APIMessage.APIMessageException) {
                        errors.addAll(((APIMessage.APIMessageException) error).getAPIMessages());
                    } else {
                        errors.add(APIMessage.ErrorMessage.EXAM_IMPORT_ERROR_AUTO_CONFIG.of(error));
                    }
                })
                .flatMap(this::applyAdditionalSEBRestrictionAttributes)
                .onError(error -> errors.add(APIMessage.ErrorMessage.EXAM_IMPORT_ERROR_AUTO_ATTRIBUTES.of(error)))
                .flatMap(examAdminService::applyQuitPassword)
                .onError(error -> errors.add(APIMessage.ErrorMessage.EXAM_IMPORT_ERROR_QUIT_PASSWORD.of(error)))
                .flatMap(examTemplateService::applyScreenProctoringSettingsForExam)
                .onError(error -> errors.add(APIMessage.ErrorMessage.EXAM_IMPORT_ERROR_SCREEN_PROCTORING_SETTINGS.of(error)))
                .flatMap(examAdminService::applySPSEnabled)
                .onError(error -> errors.add(APIMessage.ErrorMessage.EXAM_IMPORT_ERROR_SPS_ENABLED.of(error)))
                .flatMap( this::applySEBRestriction)
                .onError(error -> errors.add(APIMessage.ErrorMessage.EXAM_IMPORT_ERROR_AUTO_RESTRICTION.of(error)));


        if (!errors.isEmpty()) {
            errors.add(0, APIMessage.ErrorMessage.EXAM_IMPORT_ERROR_AUTO_SETUP.of(
                    exam.getModelId(),
                    API.PARAM_MODEL_ID + Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR + exam.getModelId()));

            log.warn("Exam successfully created but some initialization did go wrong: {}", errors);

            throw new APIMessage.APIMessageException(errors);
        } else {
            return this.examDAO.byPK(exam.id);
        }
    }

    private Result<Object> applySEBRestriction(final Exam exam) {
        return Result.tryCatch(() -> {
            this.sebRestrictionService
                    .applySEBClientRestriction(exam)
                    .onError(error -> log.error("Failed to apply SEB Restriction for exam: {}, cause: {}",
                            exam, 
                            error.getMessage()));

            return this.examDAO
                    .byPK(exam.id)
                    .getOrThrow();
        });
    }

    @Override
    public Result<Exam> initAdditionalAttributes(final Exam exam) {
        return Result.tryCatch(() -> {
            final Long examId = exam.getId();

            // initialize App-Signature-Key feature attributes
            this.additionalAttributesDAO.initAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED,
                    String.valueOf(this.appSignatureKeyEnabled));

            this.additionalAttributesDAO.initAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    Exam.ADDITIONAL_ATTR_NUMERICAL_TRUST_THRESHOLD,
                    String.valueOf(this.defaultNumericalTrustThreshold));

            this.additionalAttributesDAO.initAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_SALT,
                    KeyGenerators.string().generateKey());

            return exam;
        }).flatMap(this::initAdditionalAttributesForMoodleExams);
    }

    private Result<Exam> applyAdditionalSEBRestrictionAttributes(final Exam exam) {
        return Result.tryCatch(() -> {

            // this only applies to exams that are attached to an LMS
            if (exam.lmsSetupId == null) {
                return exam;
            }

            if (log.isDebugEnabled()) {
                log.debug("Apply additional SEB restrictions for exam: {}",
                        exam.externalId);
            }

            final LmsSetup lmsSetup = this.lmsAPIService
                    .getLmsSetup(exam.lmsSetupId)
                    .getOrThrow();

            if (lmsSetup.lmsType == LmsSetup.LmsType.OPEN_EDX) {
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

    private Result<Exam> initAdditionalAttributesForMoodleExams(final Exam exam) {
        return Result.tryCatch(() -> {

            if (exam.lmsSetupId == null) {
                return exam;
            }

            final LmsAPITemplate lmsTemplate = this.lmsAPIService
                    .getLmsAPITemplate(exam.lmsSetupId)
                    .getOrThrow();

            if (lmsTemplate.lmsSetup().lmsType == LmsSetup.LmsType.MOODLE) {
                lmsTemplate.getQuiz(exam.externalId)
                        .flatMap(quizData -> this.additionalAttributesDAO.saveAdditionalAttribute(
                                EntityType.EXAM,
                                exam.id,
                                QuizData.QUIZ_ATTR_NAME,
                                quizData.name))
                        .onError(error -> log.error("Failed to create additional moodle quiz name attribute: ", error));
            }

            if (lmsTemplate.lmsSetup().lmsType == LmsSetup.LmsType.MOODLE_PLUGIN) {
                // Save additional Browser Exam Key for Moodle plugin integration SEBSERV-372
                try {

                    final String moodleBEKUUID = UUID.randomUUID().toString();
                    final MessageDigest hasher = MessageDigest.getInstance(Constants.SHA_256);
                    hasher.update(Utils.toByteArray(moodleBEKUUID));
                    final String moodleBEK = Hex.toHexString(hasher.digest());

                    this.additionalAttributesDAO.saveAdditionalAttribute(
                                    EntityType.EXAM,
                                    exam.id,
                                    SEBRestrictionService.ADDITIONAL_ATTR_ALTERNATIVE_SEB_BEK,
                                    moodleBEK)
                            .getOrThrow();
                } catch (final Exception e) {
                    log.error("Failed to create additional moodle SEB BEK attribute: ", e);
                }
            }

            return exam;
        });
    }
}
