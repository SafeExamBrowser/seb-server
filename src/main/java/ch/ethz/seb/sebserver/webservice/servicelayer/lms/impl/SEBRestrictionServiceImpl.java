/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.MoodleSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.Features;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamFinishedEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamStartedEvent;

@Lazy
@Service
@WebServiceProfile
public class SEBRestrictionServiceImpl implements SEBRestrictionService {

    private static final Logger log = LoggerFactory.getLogger(SEBRestrictionServiceImpl.class);

    private final ExamDAO examDAO;
    private final LmsAPIService lmsAPIService;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final ExamConfigService examConfigService;

    protected SEBRestrictionServiceImpl(
            final ExamDAO examDAO,
            final LmsAPIService lmsAPIService,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ExamConfigService examConfigService) {

        this.examDAO = examDAO;
        this.lmsAPIService = lmsAPIService;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.examConfigService = examConfigService;
    }

    @Override
    public LmsAPIService getLmsAPIService() {
        return this.lmsAPIService;
    }

    @Override
    public boolean checkSebRestrictionSet(final Exam exam) {
        if (exam == null || exam.lmsSetupId == null) {
            return false;
        }

        final LmsSetup lmsSetup = this.lmsAPIService
                .getLmsSetup(exam.lmsSetupId)
                .getOr(null);

        // check only if SEB_RESTRICTION feature is on
        if (lmsSetup != null && lmsSetup.lmsType.features.contains(Features.SEB_RESTRICTION)) {
            return exam.sebRestriction;
        }

        return true;
    }

    @Override
    @Transactional
    public Result<SEBRestriction> getSEBRestrictionFromExam(final Exam exam) {
        if (exam == null || exam.lmsSetupId == null) {
            return Result.ofError(new NoSEBRestrictionException("exam or lms setup has null reference"));
        }

        return Result.tryCatch(() -> {
            // load the config keys from restriction and merge with new generated config keys
            final Collection<String> generatedKeys = this.examConfigService
                    .generateConfigKeys(exam.institutionId, exam.id)
                    .getOrThrow();

            final Set<String> configKeys = new HashSet<>(generatedKeys);
            if (!generatedKeys.isEmpty()) {
                configKeys.addAll(this.lmsAPIService
                        .getLmsAPITemplate(exam.lmsSetupId)
                        .flatMap(lmsTemplate -> lmsTemplate.getSEBClientRestriction(exam))
                        .map(r -> r.configKeys)
                        .getOr(Collections.emptyList()));
            }

            // get the browser exam keys from exam record
            final Collection<String> browserExamKeys = new ArrayList<>();
            final String browserExamKeysString = exam.getBrowserExamKeys();
            if (StringUtils.isNotBlank(browserExamKeysString)) {
                browserExamKeys.addAll(Arrays.asList(StringUtils.split(
                        browserExamKeysString,
                        Constants.LIST_SEPARATOR)));
            }

            // extract the additional restriction properties from the exams AdditionalAttributes
            final Map<String, String> additionalAttributes = new HashMap<>();
            try {
                additionalAttributes.putAll(this.additionalAttributesDAO
                        .getAdditionalAttributes(EntityType.EXAM, exam.id)
                        .getOrThrow()
                        .stream()
                        .filter(attr -> attr.getName().startsWith(SEB_RESTRICTION_ADDITIONAL_PROPERTY_NAME_PREFIX))
                        .collect(Collectors.toMap(
                                attr -> attr.getName().replace(
                                        SEB_RESTRICTION_ADDITIONAL_PROPERTY_NAME_PREFIX,
                                        StringUtils.EMPTY),
                                AdditionalAttributeRecord::getValue)));
            } catch (final Exception e) {
                log.error(
                        "Failed to load additional SEB restriction properties from AdditionalAttributes of the Exam: {}",
                        exam,
                        e);
            }

            // special Moodle plugin case for ADDITIONAL_ATTR_ALTERNATIVE_SEB_BEK
            this.lmsAPIService.getLmsSetup(exam.lmsSetupId).map(lms -> {
                if (lms.lmsType == LmsType.MOODLE_PLUGIN) {
                    final AdditionalAttributeRecord attr = this.additionalAttributesDAO.getAdditionalAttribute(
                            EntityType.EXAM,
                            exam.id,
                            ADDITIONAL_ATTR_ALTERNATIVE_SEB_BEK)
                            .getOrThrow();
                    additionalAttributes.put(MoodleSEBRestriction.ATTR_ALT_BEK, attr.getValue());
                }
                return lms;
            });

            return new SEBRestriction(
                    exam.id,
                    configKeys,
                    browserExamKeys,
                    additionalAttributes,
                    null);
        });
    }

    @Override
    public Result<Exam> saveSEBRestrictionToExam(final Exam exam, final SEBRestriction sebRestriction) {

        if (log.isDebugEnabled()) {
            log.debug("Save SEBRestriction: {} for Exam: {}", sebRestriction, exam);
        }

        return Result.tryCatch(() -> {
            // save Browser Exam Keys
            final Collection<String> browserExamKeys = sebRestriction.getBrowserExamKeys();
            final Exam newExam = new Exam(
                    exam.id,
                    null, null, null, null, null, null, null, null, null,
                    exam.supporter,
                    exam.status,
                    null,
                    null,
                    (browserExamKeys != null && !browserExamKeys.isEmpty())
                            ? StringUtils.join(browserExamKeys, Constants.LIST_SEPARATOR_CHAR)
                            : StringUtils.EMPTY,
                    null, null, null, null, null);

            this.examDAO.save(newExam)
                    .getOrThrow();

            // save additional restriction properties
            // remove old ones first by collecting its id's and then delete by id's
            this.additionalAttributesDAO
                    .getAdditionalAttributes(EntityType.EXAM, exam.id)
                    .getOrThrow()
                    .stream()
                    .filter(attr -> attr.getName().startsWith(SEB_RESTRICTION_ADDITIONAL_PROPERTY_NAME_PREFIX))
                    .forEach(attr -> this.additionalAttributesDAO.delete(attr.getId()));
            // create new ones if needed
            sebRestriction.additionalProperties
                    .entrySet()
                    .stream()
                    .forEach(entry -> this.additionalAttributesDAO.saveAdditionalAttribute(
                            EntityType.EXAM,
                            exam.id,
                            SEB_RESTRICTION_ADDITIONAL_PROPERTY_NAME_PREFIX + entry.getKey(),
                            entry.getValue()));

            return exam.id;
        })
                .flatMap(this.examDAO::byPK);
    }

    @EventListener(ExamStartedEvent.class)
    public void notifyExamStarted(final ExamStartedEvent event) {

        // This affects only exams with LMS binding...
        if (event.exam.lmsSetupId == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("ExamStartedEvent received, process applySEBClientRestriction...");
        }

        applySEBClientRestriction(event.exam)
                .flatMap(e -> this.examDAO.setSEBRestriction(e.id, true))
                .onError(error -> log.error(
                        "Failed to apply SEB restrictions for started exam: {}",
                        event.exam,
                        error));
    }

    @EventListener(ExamFinishedEvent.class)
    public void notifyExamFinished(final ExamFinishedEvent event) {

        // This affects only exams with LMS binding...
        if (event.exam.lmsSetupId == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("ExamFinishedEvent received, process releaseSEBClientRestriction...");
        }

        releaseSEBClientRestriction(event.exam)
                .onError(error -> log.error(
                        "Failed to release SEB restrictions for finished exam: {}",
                        event.exam,
                        error));
    }

    @EventListener(ExamDeletionEvent.class)
    public void notifyExamDeletion(final ExamDeletionEvent event) {

        if (log.isDebugEnabled()) {
            log.debug("ExamDeletionEvent received, process releaseSEBClientRestriction...");
        }

        event.ids.stream().forEach(this::processExamDeletion);
    }

    private Result<Exam> processExamDeletion(final Long examId) {
        return this.examDAO
                .byPK(examId)
                .whenDo(
                        exam -> exam.lmsSetupId != null,
                        exam -> releaseSEBClientRestriction(exam).getOrThrow()
                ).onError(error -> log.error(
                        "Failed to release SEB restrictions for finished exam: {}",
                        examId, error));
    }

    @Override
    public Result<Exam> applySEBClientRestriction(final Exam exam) {
        return Result.tryCatch(() -> {
            if (exam.lmsSetupId == null) {
                log.info("No LMS for Exam: {}", exam.name);
                return exam;
            }

            if (!this.lmsAPIService
                    .getLmsSetup(exam.lmsSetupId)
                    .getOrThrow().lmsType.features.contains(Features.SEB_RESTRICTION)) {

                return exam;
            }

            return this.getSEBRestrictionFromExam(exam)
                    .map(sebRestrictionData -> {

                        if (log.isDebugEnabled()) {
                            log.debug(" *** SEB Restriction *** Applying SEB Client restriction on LMS with: {}",
                                    sebRestrictionData);
                        }

                        return this.lmsAPIService
                                .getLmsAPITemplate(exam.lmsSetupId)
                                .flatMap(lmsTemplate -> lmsTemplate.applySEBClientRestriction(
                                        exam,
                                        sebRestrictionData))
                                .map(data -> {
                                    if (StringUtils.isNoneBlank(data.warningMessage)) {
                                        throw new APIMessage.APIMessageException(
                                                APIMessage.ErrorMessage.LMS_WARNINGS.of(data.warningMessage));
                                    }
                                    return exam;
                                })
                                .onError(error -> this.examDAO.setSEBRestriction(exam.id, false))
                                .getOrThrow();
                    })
                    .getOrThrow();
        });
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {

        return this.lmsAPIService
                .getLmsAPITemplate(exam.lmsSetupId)
                .map(template -> {
                    if (template.lmsSetup().lmsType.features.contains(Features.SEB_RESTRICTION)) {
                        if (log.isDebugEnabled()) {
                            log.debug(" *** SEB Restriction *** Release SEB Client restrictions from LMS for exam: {}",
                                    exam);
                        }
                        template
                                .releaseSEBClientRestriction(exam)
                                .getOrThrow();

                    }
                    return exam;
                });
    }

}
