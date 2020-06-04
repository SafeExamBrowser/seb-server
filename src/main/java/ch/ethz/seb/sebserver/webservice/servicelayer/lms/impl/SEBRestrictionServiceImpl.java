/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.Features;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigService;

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
    @Transactional
    public Result<SEBRestriction> getSEBRestrictionFromExam(final Exam exam) {
        return Result.tryCatch(() -> {
            // load the config keys from restriction and merge with new generated config keys
            final Set<String> configKeys = new HashSet<>();
            final Collection<String> generatedKeys = this.examConfigService
                    .generateConfigKeys(exam.institutionId, exam.id)
                    .getOrThrow();

            configKeys.addAll(generatedKeys);
            if (generatedKeys != null && !generatedKeys.isEmpty()) {
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
                                        ""),
                                attr -> attr.getValue())));
            } catch (final Exception e) {
                log.error(
                        "Failed to load additional SEB restriction properties from AdditionalAttributes of the Exam: {}",
                        exam,
                        e);
            }

            return new SEBRestriction(
                    exam.id,
                    configKeys,
                    browserExamKeys,
                    additionalAttributes);
        });
    }

    @Override
    @Transactional
    public Result<Exam> saveSEBRestrictionToExam(final Exam exam, final SEBRestriction sebRestriction) {

        if (log.isDebugEnabled()) {
            log.debug("Save SEBRestriction: {} for Exam: {}", sebRestriction, exam);
        }

        return Result.tryCatch(() -> {
            // save Browser Exam Keys
            final Collection<String> browserExamKeys = sebRestriction.getBrowserExamKeys();
            final Exam newExam = new Exam(
                    exam.id,
                    null, null, null, null, null, null, null, null, null, null,
                    exam.supporter,
                    exam.status,
                    (browserExamKeys != null && !browserExamKeys.isEmpty())
                            ? StringUtils.join(browserExamKeys, Constants.LIST_SEPARATOR_CHAR)
                            : StringUtils.EMPTY,
                    null, null);

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

    @Override
    public Result<Exam> applySEBClientRestriction(final Exam exam) {
        if (!this.lmsAPIService
                .getLmsSetup(exam.lmsSetupId)
                .getOrThrow().lmsType.features.contains(Features.SEB_RESTRICTION)) {

            return Result.of(exam);
        }

        return this.getSEBRestrictionFromExam(exam)
                .map(sebRestrictionData -> {

                    if (log.isDebugEnabled()) {
                        log.debug("Applying SEB Client restriction on LMS with: {}", sebRestrictionData);
                    }

                    return this.lmsAPIService
                            .getLmsAPITemplate(exam.lmsSetupId)
                            .flatMap(lmsTemplate -> lmsTemplate.applySEBClientRestriction(
                                    exam.externalId,
                                    sebRestrictionData))
                            .map(data -> exam)
                            .getOrThrow();
                });
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {

        if (log.isDebugEnabled()) {
            log.debug("Release SEB Client restrictions for exam: {}", exam);
        }

        return this.lmsAPIService
                .getLmsAPITemplate(exam.lmsSetupId)
                .flatMap(template -> template.releaseSEBClientRestriction(exam));
    }

}
