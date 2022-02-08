/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.IndicatorTemplate;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamTemplateDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateService;

@Lazy
@Service
@WebServiceProfile
public class ExamTemplateServiceImpl implements ExamTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ExamTemplateServiceImpl.class);

    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final ExamAdminService examAdminService;
    private final ExamTemplateDAO examTemplateDAO;
    private final ConfigurationNodeDAO configurationNodeDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final IndicatorDAO indicatorDAO;
    private final JSONMapper jsonMapper;

    private final String defaultIndicatorName;
    private final String defaultIndicatorType;
    private final String defaultIndicatorColor;
    private final String defaultIndicatorThresholds;
    private final String defaultExamConfigNameTemplate;
    private final String defaultExamConfigDescTemplate;

    public ExamTemplateServiceImpl(
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ExamAdminService examAdminService,
            final ExamTemplateDAO examTemplateDAO,
            final ConfigurationNodeDAO configurationNodeDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final IndicatorDAO indicatorDAO,
            final JSONMapper jsonMapper,

            @Value("${sebserver.webservice.api.exam.indicator.name:}") final String defaultIndicatorName,
            @Value("${sebserver.webservice.api.exam.indicator.type:}") final String defaultIndicatorType,
            @Value("${sebserver.webservice.api.exam.indicator.color:}") final String defaultIndicatorColor,
            @Value("${sebserver.webservice.api.exam.indicator.thresholds:}") final String defaultIndicatorThresholds,
            @Value("${sebserver.webservice.configtemplate.examconfig.default.name:}") final String defaultExamConfigNameTemplate,
            @Value("${sebserver.webservice.configtemplate.examconfig.default.description:}") final String defaultExamConfigDescTemplate) {

        this.examTemplateDAO = examTemplateDAO;
        this.configurationNodeDAO = configurationNodeDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.examAdminService = examAdminService;
        this.indicatorDAO = indicatorDAO;
        this.jsonMapper = jsonMapper;

        this.defaultIndicatorName = defaultIndicatorName;
        this.defaultIndicatorType = defaultIndicatorType;
        this.defaultIndicatorColor = defaultIndicatorColor;
        this.defaultIndicatorThresholds = defaultIndicatorThresholds;

        this.defaultExamConfigNameTemplate = (StringUtils.isNotBlank(defaultExamConfigDescTemplate))
                ? defaultExamConfigNameTemplate
                : DEFAULT_EXAM_CONFIG_NAME_TEMPLATE;
        this.defaultExamConfigDescTemplate = (StringUtils.isNotBlank(defaultExamConfigDescTemplate))
                ? defaultExamConfigDescTemplate
                : DEFAULT_EXAM_CONFIG_DESC_TEMPLATE;
    }

    @Override
    public Result<Exam> addDefinedIndicators(final Exam exam) {
        if (exam.examTemplateId != null) {
            return addIndicatorsFromTemplate(exam);
        } else {
            return addDefaultIndicator(exam);
        }
    }

    @Override
    public Result<Exam> initAdditionalAttributes(final Exam exam) {
        return this.examAdminService.saveLMSAttributes(exam)
                .map(_exam -> {

                    if (exam.examTemplateId != null) {

                        if (log.isDebugEnabled()) {
                            log.debug("Init exam: {} with additional attributes form exam template: {}",
                                    exam.externalId,
                                    exam.examTemplateId);
                        }

                        final ExamTemplate examTemplate = this.examTemplateDAO
                                .byPK(exam.examTemplateId)
                                .onError(error -> log.warn("No exam template found for id: {}",
                                        exam.examTemplateId,
                                        error.getMessage()))
                                .getOr(null);

                        if (examTemplate == null) {
                            return exam;
                        }

                        if (examTemplate.examAttributes != null && !examTemplate.examAttributes.isEmpty()) {
                            this.additionalAttributesDAO.saveAdditionalAttributes(
                                    EntityType.EXAM,
                                    exam.getId(),
                                    examTemplate.examAttributes);
                        }
                    }
                    return _exam;
                }).onError(error -> log.error("Failed to create additional attributes defined by template for exam: ",
                        error));
    }

    @Override
    public Result<Exam> initExamConfiguration(final Exam exam) {
        return Result.tryCatch(() -> {

            if (exam.examTemplateId != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Init exam: {} from template: {}", exam.externalId, exam.examTemplateId);
                }

                final ExamTemplate examTemplate = this.examTemplateDAO
                        .byPK(exam.examTemplateId)
                        .onError(error -> log.warn("No exam template found for id: {}",
                                exam.examTemplateId,
                                error.getMessage()))
                        .getOr(null);

                if (examTemplate == null) {
                    return exam;
                }

                if (examTemplate.configTemplateId != null) {

                    // create new exam configuration for the exam
                    final ConfigurationNode configurationNode = new ConfigurationNode(
                            null,
                            exam.institutionId,
                            examTemplate.configTemplateId,
                            replaceVars(this.defaultExamConfigNameTemplate, exam, examTemplate),
                            replaceVars(this.defaultExamConfigDescTemplate, exam, examTemplate),
                            ConfigurationType.EXAM_CONFIG,
                            exam.owner,
                            ConfigurationStatus.IN_USE);

                    final ConfigurationNode examConfig = this.configurationNodeDAO
                            .createNew(configurationNode)
                            .onError(error -> log.error(
                                    "Failed to create exam configuration for exam: {} from template: {} examConfig: {}",
                                    exam.name,
                                    examTemplate.name,
                                    configurationNode,
                                    error))
                            .getOrThrow(error -> new APIMessageException(
                                    ErrorMessage.EXAM_IMPORT_ERROR_AUTO_CONFIG,
                                    error));

                    // map the exam configuration to the exam
                    this.examConfigurationMapDAO.createNew(new ExamConfigurationMap(
                            exam.institutionId,
                            exam.id,
                            examConfig.id,
                            null))
                            .onError(error -> log.error(
                                    "Failed to create exam configuration mapping for exam: {} for exam config: {}",
                                    exam,
                                    examConfig,
                                    error))
                            .getOrThrow(error -> new APIMessageException(
                                    ErrorMessage.EXAM_IMPORT_ERROR_AUTO_CONFIG_LINKING,
                                    error));

                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Not exam template defined for exam: {}", exam.externalId);
                }
            }

            return exam;
        }).onError(error -> log.error("Failed to create exam configuration defined by template for exam: ", error));
    }

    private Result<Exam> addIndicatorsFromTemplate(final Exam exam) {
        return Result.tryCatch(() -> {

            if (exam.examTemplateId != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Init exam: {} from template: {}", exam.externalId, exam.examTemplateId);
                }

                final ExamTemplate examTemplate = this.examTemplateDAO
                        .byPK(exam.examTemplateId)
                        .onError(error -> log.warn("No exam template found for id: {}",
                                exam.examTemplateId,
                                error.getMessage()))
                        .getOr(null);

                if (examTemplate == null) {
                    return exam;
                }

                examTemplate.indicatorTemplates
                        .forEach(it -> createIndicatorFromTemplate(it, exam));
            }

            return exam;
        }).onError(error -> log.error("Failed to create indicators defined by template for exam: ", error));
    }

    private void createIndicatorFromTemplate(final IndicatorTemplate template, final Exam exam) {
        try {

            this.indicatorDAO.createNew(
                    new Indicator(
                            null,
                            exam.id,
                            template.name,
                            template.type,
                            template.defaultColor,
                            template.defaultIcon,
                            template.tags,
                            template.thresholds))
                    .getOrThrow();

        } catch (final Exception e) {
            log.error("Failed to automatically create indicator from template: {} for exam: {}",
                    template,
                    exam,
                    e);
        }
    }

    private Result<Exam> addDefaultIndicator(final Exam exam) {
        return Result.tryCatch(() -> {

            if (StringUtils.isBlank(this.defaultIndicatorName)) {
                if (log.isDebugEnabled()) {
                    log.debug("No default indicator defined for exam: {}", exam.externalId);
                }
                return exam;
            }

            if (log.isDebugEnabled()) {
                log.debug("Init default indicator for exam: {}", exam.externalId);
            }

            final Collection<Indicator.Threshold> thresholds = this.jsonMapper.readValue(
                    this.defaultIndicatorThresholds,
                    new TypeReference<Collection<Indicator.Threshold>>() {
                    });

            this.indicatorDAO.createNew(
                    new Indicator(
                            null,
                            exam.id,
                            this.defaultIndicatorName,
                            IndicatorType.valueOf(this.defaultIndicatorType),
                            this.defaultIndicatorColor,
                            null,
                            null,
                            thresholds))
                    .getOrThrow();

            return exam;
        }).onError(error -> log.error("Failed to apply default indicators for exam: ", error));
    }

    private String replaceVars(final String template, final Exam exam, final ExamTemplate examTemplate) {
        final String currentDate = DateTime.now(DateTimeZone.UTC).toString(Constants.STANDARD_DATE_FORMATTER);
        final Map<String, String> vars = new HashMap<>();
        vars.put(VAR_CURRENT_DATE, currentDate);
        vars.put(
                VAR_START_DATE,
                (exam.startTime != null)
                        ? exam.startTime.toString(Constants.STANDARD_DATE_FORMATTER)
                        : currentDate);
        vars.put(VAR_EXAM_NAME, exam.name);
        vars.put(VAR_EXAM_TEMPLATE_NAME, examTemplate.name);

        return Utils.replaceAll(template, vars);
    }

}
