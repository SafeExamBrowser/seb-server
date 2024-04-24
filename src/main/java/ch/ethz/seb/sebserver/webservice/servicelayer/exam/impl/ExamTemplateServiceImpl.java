/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ProctoringAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import org.apache.commons.lang3.BooleanUtils;
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
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamTemplateDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateService;

@Lazy
@Service
@WebServiceProfile
public class ExamTemplateServiceImpl implements ExamTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ExamTemplateServiceImpl.class);

    private final AdditionalAttributesDAO additionalAttributesDAO;

    private final ExamTemplateDAO examTemplateDAO;

    private final ProctoringAdminService proctoringAdminService;
    private final ConfigurationNodeDAO configurationNodeDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final IndicatorDAO indicatorDAO;
    private final ClientGroupDAO clientGroupDAO;
    private final JSONMapper jsonMapper;

    private final String defaultIndicatorName;
    private final String defaultIndicatorType;
    private final String defaultIndicatorColor;
    private final String defaultIndicatorThresholds;
    private final String defaultExamConfigNameTemplate;
    private final String defaultExamConfigDescTemplate;

    public ExamTemplateServiceImpl(
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ExamTemplateDAO examTemplateDAO,
            final ProctoringAdminService proctoringAdminService,
            final ConfigurationNodeDAO configurationNodeDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final IndicatorDAO indicatorDAO,
            final ClientGroupDAO clientGroupDAO,
            final JSONMapper jsonMapper,

            @Value("${sebserver.webservice.api.exam.indicator.name:}") final String defaultIndicatorName,
            @Value("${sebserver.webservice.api.exam.indicator.type:}") final String defaultIndicatorType,
            @Value("${sebserver.webservice.api.exam.indicator.color:}") final String defaultIndicatorColor,
            @Value("${sebserver.webservice.api.exam.indicator.thresholds:}") final String defaultIndicatorThresholds,
            @Value("${sebserver.webservice.configtemplate.examconfig.default.name:}") final String defaultExamConfigNameTemplate,
            @Value("${sebserver.webservice.configtemplate.examconfig.default.description:}") final String defaultExamConfigDescTemplate) {

        this.examTemplateDAO = examTemplateDAO;
        this.configurationNodeDAO = configurationNodeDAO;
        this.proctoringAdminService = proctoringAdminService;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.indicatorDAO = indicatorDAO;
        this.clientGroupDAO = clientGroupDAO;
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
    public Result<Exam> addDefinedClientGroups(final Exam exam) {
        return Result.tryCatch(() -> {

            if (exam.examTemplateId != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Init client groups for exam: {} from template: {}", exam.externalId,
                            exam.examTemplateId);
                }

                final ExamTemplate examTemplate = this.examTemplateDAO
                        .byPK(exam.examTemplateId)
                        .onError(error -> log.warn("No exam template found for id: {} error: {}",
                                exam.examTemplateId,
                                error.getMessage()))
                        .getOr(null);

                if (examTemplate == null) {
                    return exam;
                }

                examTemplate.clientGroupTemplates
                        .forEach(it -> createClientGroupFromTemplate(it, exam));
            }

            return exam;
        }).onError(error -> log.error("Failed to create indicators defined by template for exam: ", error));
    }

    @Override
    public Result<Exam> initAdditionalTemplateAttributes(final Exam exam) {
        return Result.tryCatch(() -> {

            if (exam.examTemplateId != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Init exam: {} with additional attributes from exam template: {}",
                            exam.externalId,
                            exam.examTemplateId);
                }

                final ExamTemplate examTemplate = this.examTemplateDAO
                        .byPK(exam.examTemplateId)
                        .onError(error -> log.warn("No exam template found for id: {} error: {}",
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

                if (examTemplate.clientConfigurationId != null) {
                    additionalAttributesDAO.saveAdditionalAttribute(
                            EntityType.EXAM,
                            exam.id,
                            Exam.ADDITIONAL_ATTR_DEFAULT_CONNECTION_CONFIGURATION,
                            String.valueOf(examTemplate.clientConfigurationId))
                            .onError(error -> log.warn(
                                    "Failed to store default connection configuration id from template for exam: {} error: {}",
                                    exam,
                                    error.getMessage()));
                }
            }

            return exam;
        }).onError(error -> log.error(
                "Failed to create additional attributes defined by template for exam: ",
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
                        .onError(error -> log.warn("No exam template found for id: {} error: {}",
                                exam.examTemplateId,
                                error.getMessage()))
                        .getOr(null);

                if (examTemplate == null) {
                    return exam;
                }

                if (examTemplate.configTemplateId != null) {

                    final ConfigurationNode examConfig = createOrReuseConfig(exam, examTemplate);

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

    @Override
    public  Result<Exam> applyScreenProctoringSettingsForExam(final Exam exam) {
        if (exam.examTemplateId == null) {
            return Result.of(exam);
        }

        return proctoringAdminService
                .getScreenProctoringSettings(new EntityKey(exam.examTemplateId, EntityType.EXAM_TEMPLATE))
                .map(settings -> {
                    if (BooleanUtils.isTrue(settings.enableScreenProctoring)) {
                        proctoringAdminService
                                .saveScreenProctoringSettings(exam.getEntityKey(), settings)
                                .getOrThrow();
                    }
                    return settings;
                })

                .onError(error -> log.warn(
                        "Failed to apply screen proctoring settings from Exam Template {} to Exam {}",
                        exam.examTemplateId,
                        exam))
                .map(settings ->  exam);
    }

    private ConfigurationNode createOrReuseConfig(final Exam exam, final ExamTemplate examTemplate) {
        final String configName = replaceVars(this.defaultExamConfigNameTemplate, exam, examTemplate);
        final FilterMap filterMap = new FilterMap();
        filterMap.putIfAbsent(Entity.FILTER_ATTR_INSTITUTION, exam.institutionId.toString());
        filterMap.putIfAbsent(Entity.FILTER_ATTR_NAME, configName);

        // get existing config if available
        final ConfigurationNode examConfig = this.configurationNodeDAO
                .allMatching(filterMap)
                .getOrThrow()
                .stream()
                .filter(res -> res.name.equals(configName))
                .findFirst()
                .orElse(null);


        // create new configuration if we don't have an old config that is on READY_TO_USE or the template has changed
        if (examConfig == null ||
                examConfig.status != ConfigurationStatus.READY_TO_USE ||
                !Objects.equals(examConfig.templateId, examTemplate.configTemplateId)) {

            final String newName = (examConfig != null && examConfig.name.equals(configName))
                    ? examConfig.name + "_"
                    : configName;

            final ConfigurationNode config = new ConfigurationNode(
                    null,
                    exam.institutionId,
                    examTemplate.configTemplateId,
                    newName,
                    replaceVars(this.defaultExamConfigDescTemplate, exam, examTemplate),
                    ConfigurationType.EXAM_CONFIG,
                    exam.owner,
                    ConfigurationStatus.IN_USE,
                    null,
                    null);

            return this.configurationNodeDAO
                    .createNew(config)
                    .onError(error -> log.error(
                            "Failed to create exam configuration for exam: {} from template: {} examConfig: {}",
                            exam.name,
                            examTemplate.name,
                            config,
                            error))
                    .getOrThrow(error -> new APIMessageException(
                            ErrorMessage.EXAM_IMPORT_ERROR_AUTO_CONFIG,
                            error));
        } else {
            final ConfigurationNode config = new ConfigurationNode(
                    examConfig.id,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    ConfigurationStatus.IN_USE,
                    null,
                    null);

            return this.configurationNodeDAO
                    .save(config)
                    .onError(error -> log.error(
                            "Failed to save exam configuration for exam: {} from template: {} examConfig: {}",
                            exam.name,
                            examTemplate.name,
                            config,
                            error))
                    .getOrThrow(error -> new APIMessageException(
                            ErrorMessage.EXAM_IMPORT_ERROR_AUTO_CONFIG,
                            error));
        }
    }

    private Result<Exam> addIndicatorsFromTemplate(final Exam exam) {
        return Result.tryCatch(() -> {

            if (exam.examTemplateId != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Init exam: {} from template: {}", exam.externalId, exam.examTemplateId);
                }

                final ExamTemplate examTemplate = this.examTemplateDAO
                        .byPK(exam.examTemplateId)
                        .onError(error -> log.warn("No exam template found for id: {} error: {}",
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
        this.indicatorDAO
                .createNew(new Indicator(
                        null,
                        exam.id,
                        template.name,
                        template.type,
                        template.defaultColor,
                        template.defaultIcon,
                        template.tags,
                        template.thresholds))
                .onError(error -> log.error("Failed to automatically create indicator from template: {} for exam: {}",
                        template,
                        exam,
                        error));
    }

    private void createClientGroupFromTemplate(final ClientGroupTemplate template, final Exam exam) {
        this.clientGroupDAO
                .createNew(new ClientGroup(
                        null,
                        exam.id,
                        template.name,
                        template.type,
                        template.color,
                        template.icon,
                        template.ipRangeStart,
                        template.ipRangeEnd,
                        template.clientOS))
                .onError(
                        error -> log.error("Failed to automatically create client group from template: {} for exam: {}",
                                template,
                                exam,
                                error));
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
                log.debug("Initialized default indicator for exam: {}", exam.externalId);
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
