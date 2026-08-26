/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;


import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.*;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateChangeEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamUtils;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ProctoringAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
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
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateService;

@Lazy
@Service
public class ExamTemplateServiceImpl implements ExamTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ExamTemplateServiceImpl.class);

    static final Predicate<IndicatorTemplate> NEW_UI_INDICATOR_FILTER = (IndicatorTemplate indicator) ->
            indicator.type == IndicatorType.BATTERY_STATUS ||
            indicator.type == IndicatorType.WLAN_STATUS;

    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final ExamTemplateDAO examTemplateDAO;
    private final ProctoringAdminService proctoringAdminService;
    private final ConfigurationNodeDAO configurationNodeDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final IndicatorDAO indicatorDAO;
    private final ClientGroupDAO clientGroupDAO;
    private final JSONMapper jsonMapper;
    private final WebserviceInfo webserviceInfo;
    private final ProctoringAdminService proctoringServiceSettingsService;
    private final BeanValidationService beanValidationService;
    private final DAOUserService daoUserServcie;

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
            final WebserviceInfo webserviceInfo,
            final ProctoringAdminService proctoringServiceSettingsService,
            final BeanValidationService beanValidationService,
            final DAOUserService daoUserServcie,

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
        this.webserviceInfo = webserviceInfo;
        this.proctoringServiceSettingsService = proctoringServiceSettingsService;
        this.beanValidationService = beanValidationService;
        this.daoUserServcie = daoUserServcie;

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
    public Result<Exam> addDefinedClientGroups(final Exam exam, final boolean applyAllGroups) {
        return Result.tryCatch(() -> {

            if (exam.examTemplateId != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Init client groups for exam: {} from template: {}", exam.externalId,
                            exam.examTemplateId);
                }

                final ExamTemplate examTemplate = this.examTemplateDAO
                        .byPK(exam.examTemplateId)
                        .map(this::applyExamTemplateAdditionalData)
                        .onError(error -> log.warn("No exam template found for id: {} error: {}",
                                exam.examTemplateId,
                                error.getMessage()))
                        .getOr(null);

                
                if (examTemplate == null) {
                    return exam;
                }
                
                final String idsString = exam.getAdditionalAttribute(API.EXAM_IMPORT_ATTR_CLIENT_GROUP_IDS);
                final Set<Long> selectedClientGroupIds = (StringUtils.isNotBlank(idsString))
                        ? Arrays.stream(idsString.split(Constants.LIST_SEPARATOR))
                                .map(Long::parseLong)
                                .collect(Collectors.toSet())
                        : null;

                examTemplate.clientGroupTemplates
                        .stream()
                        .filter( it -> applyAllGroups || (selectedClientGroupIds != null && selectedClientGroupIds.contains(it.id)))
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
                        .map(this::applyExamTemplateAdditionalData)
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
                        .map(this::applyExamTemplateAdditionalData)
                        .onError(error -> log.warn("No exam template found for id: {} error: {}",
                                exam.examTemplateId,
                                error.getMessage()))
                        .getOr(null);

                if (examTemplate == null) {
                    return exam;
                }

                mapConfigurationNodeToExam(exam,  createOrReuseConfig(exam, examTemplate));
            } else {
                log.info("No exam template defined for exam: {}, create Exam Configuration with default SEB Settings", exam.externalId);
                mapConfigurationNodeToExam(exam, createOrReuseConfig(exam, null));
            }

            return exam;
        }).onError(error -> log.error("Failed to create exam configuration defined by template for exam: ", error));
    }

    @Override
    public void repairExamConfiguration(final Exam exam) {
        try {

            final Collection<Long> configurationNodeIds = examConfigurationMapDAO
                    .getConfigurationNodeIds(exam.id)
                    .getOrThrow();

            if (configurationNodeIds == null || configurationNodeIds.isEmpty()) {
                log.info("--------> Repair Exam with missing Exam Configuration. Apply default SEB Settings to Exam --> {} ({})", exam.name, exam.id);
                mapConfigurationNodeToExam(exam, createOrReuseConfig(exam, null));
            }

        } catch (Exception e) {
            log.error("------> !!! Failed to apply default Exam Configuration for Exam: {} cause:", exam, e);
        }
    }

    @Override
    public  Result<Exam> applyScreenProctoringSettingsForExam(final Exam exam) {
        if (exam.examTemplateId == null) {
            return Result.of(exam);
        }

        return Result.tryCatch(() -> {
            final ExamTemplate examTemplate = this.examTemplateDAO
                    .byPK(exam.examTemplateId)
                    .map(this::applyExamTemplateAdditionalData)
                    .onError(error -> log.warn("No exam template found for id: {} error: {}",
                            exam.examTemplateId,
                            error.getMessage()))
                    .getOrThrow();


            final Result<ScreenProctoringSettings> screenProctoringSettings = proctoringAdminService
                    .getScreenProctoringSettings(new EntityKey(exam.examTemplateId, EntityType.EXAM_TEMPLATE));

            if (!screenProctoringSettings.hasError()) {
                return screenProctoringSettings
                        .map(settings -> convertSPSTemplateSettings(exam, examTemplate, settings))
                        .map(settings -> proctoringAdminService
                                .saveScreenProctoringSettings(exam.getEntityKey(), settings)
                                .getOrThrow())
                        .map(settings -> exam)
                        .onError(error -> log.warn(
                                "Failed to apply screen proctoring settings from Exam Template {} to Exam {} cause: {}",
                                exam.examTemplateId,
                                exam,
                                error.getMessage()))
                        .getOr(exam);
            } else {
                log.debug("No Screen Proctoring settings found for Exam Template: {}", examTemplate);
                return exam;
            }
        });
    }

    @Override
    public Result<ExamTemplate> createExamTemplateAdditionalData(
            final Long createdTemplateId,
            final ExamTemplate examTemplate) {

        return Result.tryCatch(() -> {

            // create group templates
            final List<String> groupIdsWithSPS = examTemplate
                    .getClientGroupTemplates()
                    .stream()
                    .map(clientGroupTemplate -> {
                        final ClientGroupTemplate newTemplate = new ClientGroupTemplate(
                                null,
                                createdTemplateId,
                                clientGroupTemplate.name,
                                clientGroupTemplate.type,
                                clientGroupTemplate.color,
                                clientGroupTemplate.icon,
                                clientGroupTemplate.ipRangeStart,
                                clientGroupTemplate.ipRangeEnd,
                                clientGroupTemplate.clientOS,
                                clientGroupTemplate.nameRangeStartLetter,
                                clientGroupTemplate.nameRangeEndLetter,
                                clientGroupTemplate.screenProctoringEnabled
                        );
                        final ClientGroupTemplate newGroup = this.beanValidationService
                                .validateBean(newTemplate)
                                .map(ExamUtils::checkClientGroupConsistency)
                                .flatMap(this.examTemplateDAO::createNewClientGroupTemplate)
                                .onError(error -> log.error("Failed to create ClientGroupTemplate: {}", clientGroupTemplate, error))
                                .getOr(null);

                        return BooleanUtils.isTrue(newGroup.screenProctoringEnabled) ? newGroup.getModelId() : null;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            // apply SPS settings
            if (examTemplate.examAttributes.containsKey(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING)) {
                try {
                    final WebserviceInfo.ScreenProctoringServiceBundle screenProctoringServiceBundle = webserviceInfo
                            .getScreenProctoringServiceBundle();

                    final boolean spsEnabled = BooleanUtils.toBoolean(examTemplate.examAttributes.get(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING));
                    final CollectingStrategy collectingStrategy = examTemplate.examAttributes.containsKey(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY)
                            ? CollectingStrategy.valueOf(examTemplate.examAttributes.get(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY))
                            : CollectingStrategy.APPLY_SEB_GROUPS;
                    String groupSelection = examTemplate.examAttributes.get(ScreenProctoringSettings.ATTR_SEB_GROUPS_SELECTION);
                    if (StringUtils.isBlank(groupSelection) && !groupIdsWithSPS.isEmpty()) {
                        groupSelection = StringUtils.join(groupIdsWithSPS, Constants.LIST_SEPARATOR);
                    }

                    final ScreenProctoringSettings screenProctoringSettings = new ScreenProctoringSettings(
                            null,
                            spsEnabled,
                            screenProctoringServiceBundle.serviceURL,
                            screenProctoringServiceBundle.clientId,
                            screenProctoringServiceBundle.clientSecret.toString(),
                            screenProctoringServiceBundle.apiAccountName,
                            screenProctoringServiceBundle.apiAccountPassword.toString(),
                            collectingStrategy,
                            examTemplate.examAttributes.get(ScreenProctoringSettings.ATTR_COLLECTING_GROUP_NAME),
                            null,
                            groupSelection,
                            true,
                            false);

                    this.proctoringServiceSettingsService
                            .saveScreenProctoringSettings(
                                    new EntityKey(createdTemplateId, EntityType.EXAM_TEMPLATE),
                                    screenProctoringSettings)
                            .getOrThrow();

                } catch (final Exception e) {
                    log.error("Failed to create SPS data for ExamTemplate: {}", examTemplate, e);
                }
            }

            // update the assigned Configuration Template with name and description from Exam Template
            this.updateConfigurationTemplate(examTemplate);

            return examTemplateDAO.byPK(createdTemplateId).getOr(examTemplate);
        });
    }

    @Override
    public Result<ExamTemplate> saveAdditionalData(final ExamTemplate examTemplate) {
        return Result.tryCatch(() -> {

            // apply and save SPS changes
            final ScreenProctoringSettings currentSPSSettings = this.proctoringServiceSettingsService
                    .getScreenProctoringSettings(new EntityKey(examTemplate.getModelId(), EntityType.EXAM_TEMPLATE))
                    .getOrThrow();

            // check if SPS enabled is available and has changed. Of not available, skip the update
            if (examTemplate.examAttributes.containsKey(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING)) {
                final boolean spsEnabled = BooleanUtils.toBoolean(
                        examTemplate.examAttributes.get(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING));
                final CollectingStrategy collectingStrategy = examTemplate.examAttributes.containsKey(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY)
                        ? CollectingStrategy.valueOf(examTemplate.examAttributes.get(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY))
                        : currentSPSSettings.collectingStrategy;

                // TODO test this and remove of not necessary: SEBSERV-977
                // if we change to APPLY_SEB_GROUPS strategy and there are no sebGroupsSelection we set all SEB groups
                // to avoid data inconsistency
                final String sebGroupsSelection = currentSPSSettings.sebGroupsSelection;
//                if (collectingStrategy == CollectingStrategy.APPLY_SEB_GROUPS && sebGroupsSelection == null) {
//                    sebGroupsSelection = examTemplateDAO
//                            .getClientGroupTemplates(examTemplate.id)
//                            .map(groups ->
//                                    StringUtils.join(groups.stream().map(
//                                            ClientGroupTemplate::getModelId).toList(),
//                                            Constants.LIST_SEPARATOR))
//                            .getOr(null);
//                }

                final ScreenProctoringSettings screenProctoringSettings = new ScreenProctoringSettings(
                        currentSPSSettings.examId,
                        spsEnabled,
                        currentSPSSettings.spsServiceURL,
                        currentSPSSettings.spsAPIKey,
                        currentSPSSettings.spsAPISecret,
                        currentSPSSettings.spsAccountId,
                        currentSPSSettings.spsAccountPassword,
                        collectingStrategy,
                        currentSPSSettings.collectingGroupName,
                        null,
                        sebGroupsSelection,
                        true,
                        false);

                this.proctoringServiceSettingsService
                        .saveScreenProctoringSettings(
                                new EntityKey(examTemplate.id, EntityType.EXAM_TEMPLATE),
                                screenProctoringSettings)
                        .getOrThrow();
            }

            // apply the name and description to the related Configuration Template
            this.updateConfigurationTemplate(examTemplate);

            return examTemplateDAO.byPK(examTemplate.id).getOrThrow();
        }).map(this::applyExamTemplateAdditionalData);
    }

    @Override
    public ExamTemplate applyExamTemplateAdditionalData(final ExamTemplate examTemplate) {

            // apply SPS Settings
            final ScreenProctoringSettings spsSettings = this.proctoringServiceSettingsService
                    .getScreenProctoringSettings(new EntityKey(examTemplate.getModelId(), EntityType.EXAM_TEMPLATE))
                    .getOrThrow();

            final Map<String, String> examAttributes = new HashMap<>(examTemplate.examAttributes);
            examAttributes.put(
                    ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING,
                    Boolean.toString(spsSettings.enableScreenProctoring));

            examAttributes.put(
                    ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY,
                    spsSettings.collectingStrategy.toString());

            examAttributes.put(
                    ScreenProctoringSettings.ATTR_COLLECTING_GROUP_NAME,
                    spsSettings.collectingGroupName);

            if (spsSettings.collectingGroupSize != null) {
                examAttributes.put(
                        ScreenProctoringSettings.ATTR_COLLECTING_GROUP_SIZE,
                        Integer.toString(spsSettings.collectingGroupSize));
            }

            examAttributes.put(
                    ScreenProctoringSettings.ATTR_SEB_GROUPS_SELECTION,
                    spsSettings.sebGroupsSelection);

            // apply SEB groups
            final String[] spsGroupIds = StringUtils.split(spsSettings.sebGroupsSelection, Constants.LIST_SEPARATOR_CHAR);
            final Set<Long> spsGIds = BooleanUtils.isTrue(spsSettings.enableScreenProctoring) && spsGroupIds != null
                    ? Arrays.stream(spsGroupIds).map(Long::parseLong).collect(Collectors.toSet())
                    : Collections.emptySet();

            final List<ClientGroupTemplate> clientGroupTemplates = examTemplateDAO
                    .getClientGroupTemplates(examTemplate.id)
                    .getOrThrow()
                    .stream()
                    .map(group -> new ClientGroupTemplate(spsGIds.contains(group.id), group))
                    .toList();

            // create and return full ExamTemplate
            return new ExamTemplate(
                    examTemplate.id,
                    examTemplate.institutionId,
                    examTemplate.name,
                    examTemplate.description,
                    examTemplate.examType,
                    examTemplate.supporter,
                    examTemplate.configTemplateId,
                    examTemplate.institutionalDefault,
                    examTemplate.lmsIntegration,
                    examTemplate.clientConfigurationId,
                    examTemplate.indicatorTemplates,
                    clientGroupTemplates,
                    examAttributes
            );
    }

    @Override
    public ExamTemplate filterLegacyData(final ExamTemplate examTemplate) {
            if (!examTemplate.indicatorTemplates.isEmpty()) {
                List<IndicatorTemplate> filteredIndicator = examTemplate.indicatorTemplates
                        .stream()
                        .filter(NEW_UI_INDICATOR_FILTER)
                        .toList();
                if (filteredIndicator.size() != examTemplate.indicatorTemplates.size()) {
                    return new ExamTemplate(
                            examTemplate.id,
                            examTemplate.institutionId,
                            examTemplate.name,
                            examTemplate.description,
                            examTemplate.examType,
                            examTemplate.supporter,
                            examTemplate.configTemplateId,
                            examTemplate.institutionalDefault,
                            examTemplate.lmsIntegration,
                            examTemplate.clientConfigurationId,
                            filteredIndicator,
                            examTemplate.clientGroupTemplates,
                            examTemplate.examAttributes
                    );
                }
            }

            return examTemplate;
    }

    @Override
    public ExamTemplate createConfigurationTemplateWhenMissing(ExamTemplate examTemplate) {
        try {

            if (examTemplate.configTemplateId != null) {
                return examTemplate;
            }

            log.info("Create new default ConfigurationTemplate for Exam Template: {}", examTemplate);

            final String currentUserUUID = this.daoUserServcie.getCurrentUserUUID();
            ConfigurationNode newConfigurationTemplate = configurationNodeDAO.createNew(new ConfigurationNode(
                    null,
                    examTemplate.institutionId,
                    null,
                    examTemplate.name,
                    examTemplate.description,
                    ConfigurationType.TEMPLATE,
                    currentUserUUID,
                    ConfigurationStatus.READY_TO_USE,
                    Utils.toDateTimeUTC(Utils.getMillisecondsNow()),
                    currentUserUUID,
                    null
            )).getOrThrow();

            ExamTemplate newExamTemplate = new ExamTemplate(
                    examTemplate.id,
                    examTemplate.institutionId ,
                    examTemplate.name,
                    examTemplate.description ,
                    examTemplate.examType,
                    examTemplate.supporter ,
                    newConfigurationTemplate.id,
                    examTemplate.institutionalDefault ,
                    examTemplate.lmsIntegration ,
                    examTemplate.clientConfigurationId,
                    examTemplate.indicatorTemplates ,
                    examTemplate.clientGroupTemplates ,
                    examTemplate.examAttributes
            );

            examTemplateDAO
                    .save(newExamTemplate)
                    .getOrThrow();

            return newExamTemplate;

        } catch (Exception e) {
            log.error("Failed to create missing Configuration Template for Exam Template: {} cause: {}",
                    examTemplate,
                    e.getMessage());

            return examTemplate;
        }
    }

    @Override
    public void fixForV3(final Long templateId) {
        try {

            final ExamTemplate examTemplate = examTemplateDAO
                    .byPK(templateId)
                    .map(this::applyExamTemplateAdditionalData)
                    .getOrThrow();

            final Map<String, String> examAttributes = examTemplate.getExamAttributes();
            final String strategy = examAttributes.get(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY);

            System.out.println("************** templateId: " + templateId + " strategy: " + strategy);

            if (Objects.equals(strategy, CollectingStrategy.EXAM.name())) {

                if (examTemplate.id == 0L) {
                    log.info("**** Change collecting strategy form 'EXAM' to 'APPLY_SEB_GROUPS' for: {}", examTemplate);

                    final EntityKey entityKey = examTemplate.getEntityKey();
                    final ScreenProctoringSettings screenProctoringSettings = proctoringAdminService
                            .getScreenProctoringSettings(entityKey)
                            .getOrThrow();

                    this.saveSPSSettings(
                            Collections.emptySet(),
                            entityKey,
                            CollectingStrategy.APPLY_SEB_GROUPS,
                            screenProctoringSettings);
                }
            }

        } catch (Exception e) {
            log.error("Failed to repair Exam Template for V3.0 and apply valid SPS collection strategy. ExamTemplate id: {}, cause: {}",
                    templateId,
                    e.getMessage());
        }
    }

    private void updateConfigurationTemplate(final ExamTemplate examTemplate) {
        if (examTemplate.configTemplateId == null) {
            return;
        }

        try {

            // update name and description of the related Configuration Template
            configurationNodeDAO.updateConfigurationTemplate(
                            examTemplate.configTemplateId,
                            examTemplate.name,
                            examTemplate.description)
                    .getOrThrow();

        } catch (Exception e) {
            log.error(
                    "Failed to update Configuration Template for saved Exam Template: {} cause: {}",
                    examTemplate,
                    e.getMessage());
        }
    }

    @Override
    public Result<ClientGroupTemplate> createNewClientGroupTemplate(final ClientGroupTemplate clientGroupTemplate) {
        return examTemplateDAO
                .createNewClientGroupTemplate(clientGroupTemplate)
                .map(this::updateSPSGroups);
    }

    @Override
    public Result<ClientGroupTemplate> saveClientGroupTemplate(ClientGroupTemplate clientGroupTemplate) {
        return examTemplateDAO
                .saveClientGroupTemplate(clientGroupTemplate)
                .map(this::updateSPSGroups);
    }

    @Override
    public Result<EntityKey> deleteClientGroupTemplate(String examTemplateId, String clientGroupTemplateId) {
        return examTemplateDAO
                .deleteClientGroupTemplate(examTemplateId, clientGroupTemplateId)
                .map(key -> deleteSPSGroup(examTemplateId, key));
    }

    @Override
    public void notifyExamTemplateChange(final ExamTemplateChangeEvent event) {
        if (event.changeState != ExamTemplateChangeEvent.ChangeState.DELETED) {
            return;
        }

        try {

            Long configTemplateId = event.getExamTemplate().configTemplateId;
            if (configTemplateId == null) {
                return;
            }

            if (!examTemplateDAO.hasAnyExamTemplateWithConfigTemplate(configTemplateId)) {
                configurationNodeDAO
                        .delete(Collections.singleton(new EntityKey(configTemplateId, EntityType.CONFIGURATION_NODE)))
                        .getOrThrow();
            }

        } catch (Exception e) {
            log.error("Failed to delete configuration template for exam template: {} cause: {}", event.getExamTemplate(), e.getMessage());
        }
    }

    @Override
    public Result<Collection<Long>> getAllIds() {
        return examTemplateDAO.getAllIds();
    }


    private ClientGroupTemplate updateSPSGroups(final ClientGroupTemplate clientGroupTemplate) {
        try {

            final EntityKey templateKey = new EntityKey(clientGroupTemplate.examTemplateId, EntityType.EXAM_TEMPLATE);

            final ScreenProctoringSettings screenProctoringSettings = proctoringAdminService
                    .getScreenProctoringSettings(templateKey)
                    .getOrThrow();

            // if sps is not enabled at all, ignore the group setting for SPS
            if (!BooleanUtils.isTrue(screenProctoringSettings.enableScreenProctoring)) {
                if (BooleanUtils.isTrue(clientGroupTemplate.screenProctoringEnabled)) {
                    return new ClientGroupTemplate(
                            clientGroupTemplate.id,
                            clientGroupTemplate.examTemplateId,
                            clientGroupTemplate.name,
                            clientGroupTemplate.type,
                            clientGroupTemplate.color,
                            clientGroupTemplate.icon,
                            clientGroupTemplate.ipRangeStart,
                            clientGroupTemplate.ipRangeEnd,
                            clientGroupTemplate.clientOS,
                            clientGroupTemplate.nameRangeStartLetter,
                            clientGroupTemplate.nameRangeEndLetter,
                            false
                    );
                }
                return clientGroupTemplate;
            }

            final Set<String> spsGroupIds = new HashSet<>();
            final String sebGroupsSelection = screenProctoringSettings.sebGroupsSelection;
            if (StringUtils.isNotBlank(sebGroupsSelection)) {
                spsGroupIds.addAll(Arrays.stream(StringUtils.split(sebGroupsSelection, Constants.LIST_SEPARATOR_CHAR)).toList());
            }

            // update SPS Group mapping
            if (BooleanUtils.isTrue(clientGroupTemplate.screenProctoringEnabled)) {
                spsGroupIds.add(String.valueOf(clientGroupTemplate.id));
            } else {
                spsGroupIds.remove(String.valueOf(clientGroupTemplate.id));
            }

            saveSPSSettings(spsGroupIds, templateKey, null, screenProctoringSettings);

        } catch (Exception e) {
            log.error(
                    "Failed to update SPS Groups from ClientGroupTemplate: {} cause: {}",
                    clientGroupTemplate,
                    e.getMessage());
        }

        return clientGroupTemplate;
    }

    private EntityKey deleteSPSGroup(final String examTemplateId, final EntityKey entityKey) {
        try {

            final EntityKey templateKey = new EntityKey(examTemplateId, EntityType.EXAM_TEMPLATE);

            final ScreenProctoringSettings screenProctoringSettings = proctoringAdminService
                    .getScreenProctoringSettings(templateKey)
                    .getOrThrow();

            final Set<String> spsGroupIds = new HashSet<>();
            final String sebGroupsSelection = screenProctoringSettings.sebGroupsSelection;
            if (StringUtils.isNotBlank(sebGroupsSelection)) {
                spsGroupIds.addAll(Arrays.stream(StringUtils.split(sebGroupsSelection, Constants.LIST_SEPARATOR_CHAR)).toList());
            }

            // update SPS Group mapping
            spsGroupIds.remove(entityKey.modelId);
            saveSPSSettings(spsGroupIds, templateKey, null, screenProctoringSettings);

        } catch (Exception e) {
            log.error(
                    "Failed to update SPS Groups from deleted ClientGroupTemplate: {} cause: {}",
                    entityKey,
                    e.getMessage());
        }

        return entityKey;
    }

    private void saveSPSSettings(
            final Set<String> spsGroupIds,
            final EntityKey templateKey,
            final CollectingStrategy newCollectingStrategy,
            final ScreenProctoringSettings screenProctoringSettings) {

        final CollectingStrategy collectingStrategy = (newCollectingStrategy != null)
                ? newCollectingStrategy
                : screenProctoringSettings.collectingStrategy;

        final String newSPSGroupIds = !spsGroupIds.isEmpty()
                ? StringUtils.join(spsGroupIds, Constants.LIST_SEPARATOR_CHAR)
                : null;

        // and save new group mapping
        proctoringAdminService.saveScreenProctoringSettings(
                        templateKey,
                        new ScreenProctoringSettings(
                                screenProctoringSettings.examId,
                                screenProctoringSettings.enableScreenProctoring,
                                screenProctoringSettings.spsServiceURL,
                                screenProctoringSettings.spsAPIKey,
                                screenProctoringSettings.spsAPISecret,
                                screenProctoringSettings.spsAccountId,
                                screenProctoringSettings.spsAccountPassword,
                                collectingStrategy,
                                screenProctoringSettings.collectingGroupName,
                                screenProctoringSettings.collectingGroupSize,
                                newSPSGroupIds,
                                screenProctoringSettings.bundled,
                                screenProctoringSettings.confirmChangeStrategy))
                .getOrThrow();
    }

    private ScreenProctoringSettings convertSPSTemplateSettings(
            final Exam exam,
            final ExamTemplate examTemplate,
            final ScreenProctoringSettings screenProctoringSettings) {

        if (screenProctoringSettings.collectingStrategy == CollectingStrategy.APPLY_SEB_GROUPS) {

            final Set<Long> selectedTemplateIds = new HashSet<>();
            if (!StringUtils.isBlank(screenProctoringSettings.sebGroupsSelection)) {
                // in this case we need to map the selected template client groups to the just created exam client groups
                selectedTemplateIds.addAll(Arrays.stream(StringUtils.split(
                                screenProctoringSettings.sebGroupsSelection,
                                Constants.LIST_SEPARATOR_CHAR))
                        .map(Long::valueOf)
                        .collect(Collectors.toSet()));
            }

            final List<String> selectedNames = examTemplate.clientGroupTemplates
                    .stream()
                    .filter(gt -> selectedTemplateIds.contains(gt.id))
                    .map(gt -> gt.name)
                    .toList();

            final List<String> selectedInstances = clientGroupDAO
                    .allForExam(exam.id)
                    .getOr(Collections.emptyList())
                    .stream()
                    .filter(g -> selectedNames.contains(g.name))
                    .map(g -> String.valueOf(g.id))
                    .toList();

            System.out.println("*********************** selectedTemplateIds: " + selectedTemplateIds);
            System.out.println("*********************** selectedInstances: " + selectedInstances);
            
            return new ScreenProctoringSettings(
                    exam.id,
                    screenProctoringSettings.enableScreenProctoring,
                    screenProctoringSettings.spsServiceURL,
                    screenProctoringSettings.spsAPIKey,
                    screenProctoringSettings.spsAPISecret,
                    screenProctoringSettings.spsAccountId,
                    screenProctoringSettings.spsAccountPassword,
                    screenProctoringSettings.collectingStrategy,
                    screenProctoringSettings.collectingGroupName,
                    screenProctoringSettings.collectingGroupSize,
                    StringUtils.join(selectedInstances, Constants.LIST_SEPARATOR),
                    screenProctoringSettings.bundled, 
                    false
            );
        }
        return new ScreenProctoringSettings(
                exam.id,
                screenProctoringSettings.enableScreenProctoring,
                screenProctoringSettings.spsServiceURL,
                screenProctoringSettings.spsAPIKey,
                screenProctoringSettings.spsAPISecret,
                screenProctoringSettings.spsAccountId,
                screenProctoringSettings.spsAccountPassword,
                screenProctoringSettings.collectingStrategy,
                screenProctoringSettings.collectingGroupName,
                screenProctoringSettings.collectingGroupSize,
                screenProctoringSettings.sebGroupsSelection,
                screenProctoringSettings.bundled,
                false
        );
    }

    private ConfigurationNode createOrReuseConfig(final Exam exam, final ExamTemplate examTemplate) {
        final String configName = replaceVars(this.defaultExamConfigNameTemplate, exam, examTemplate);
        final FilterMap filterMap = new FilterMap();
        filterMap.putIfAbsent(Entity.FILTER_ATTR_INSTITUTION, exam.institutionId.toString());
        filterMap.putIfAbsent(Entity.FILTER_ATTR_NAME, configName);

        // get existing config if available
        Collection<ConfigurationNode> allConfigs = this.configurationNodeDAO
                .allMatching(filterMap)
                .getOrThrow();
        final ConfigurationNode examConfig = allConfigs.stream()
                .filter(res -> res.name.equals(configName))
                .findFirst()
                .orElse(null);


        // create new configuration if we don't have an old config that is on READY_TO_USE or the template has changed
        if (examConfig == null ||
                examConfig.status != ConfigurationStatus.READY_TO_USE ||
                !(examTemplate != null && Objects.equals(examConfig.templateId, examTemplate.configTemplateId))) {

            final String newName = (examConfig != null && examConfig.name.equals(configName))
                    ? examConfig.name +
                        "(" + allConfigs.size() + ")"
                    : configName;

            final ConfigurationNode config = new ConfigurationNode(
                    null,
                    exam.institutionId,
                    (examTemplate != null) ? examTemplate.configTemplateId : null,
                    newName,
                    replaceVars(this.defaultExamConfigDescTemplate, exam, examTemplate),
                    ConfigurationType.EXAM_CONFIG,
                    exam.owner,
                    ConfigurationStatus.IN_USE,
                    null,
                    null, null);

            return this.configurationNodeDAO
                    .createNew(config)
                    .onError(error -> log.error(
                            "Failed to create exam configuration for exam: {} from template: {} examConfig: {} error: {}",
                            exam.name,
                            (examTemplate != null) ? examTemplate.name : "--",
                            config,
                            error.getMessage()))
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
                    null, null);

            return this.configurationNodeDAO
                    .save(config)
                    .onError(error -> log.error(
                            "Failed to save exam configuration for exam: {} from template: {} examConfig: {}",
                            exam.name,
                            (examTemplate != null) ? examTemplate.name : "--",
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
                        .stream()
                        // SEBSERV-947 - filter out and create only BATTERY_STATUS and WLAN_STATUS indicators since 3.0
                        .filter(indicatorTemplate ->
                                indicatorTemplate.type == IndicatorType.BATTERY_STATUS ||
                                indicatorTemplate.type == IndicatorType.WLAN_STATUS)
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
                        template.clientOS,
                        template.nameRangeStartLetter,
                        template.nameRangeEndLetter,
                        false))
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
        if (examTemplate != null) {
            vars.put(VAR_EXAM_TEMPLATE_NAME, examTemplate.name);
        }

        return Utils.replaceAll(template, vars);
    }

    @Scheduled(fixedRate = 12 * Constants.HOUR_IN_MILLIS, initialDelay = Constants.MINUTE_IN_MILLIS)
    private void cleanupTemporaryConfigurationTemplates() {
        try {

            log.info("Check for outdated temporary ConfigurationTemplate");

            final long now = Utils.getMillisecondsNow();

            configurationNodeDAO
                    .getAllTemporary()
                    .getOrThrow()
                    .forEach(c -> {
                        try {

                            if (c.getLastUpdateTime() == null || now - c.getLastUpdateTime() > 24 * Constants.HOUR_IN_MILLIS) {

                                log.info("Delete outdated temporary ConfigurationTemplate: {}", c);

                                configurationNodeDAO
                                        .delete(Collections.singleton(new EntityKey(c.getId(), EntityType.CONFIGURATION_NODE)))
                                        .getOrThrow();
                            }
                        } catch (Exception e) {
                            log.error("Failed to cleanup temporary ConfigurationTemplate: {} ", c, e);
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to cleanup temporary ConfigurationTemplates: ", e);
        }
    }

    private void mapConfigurationNodeToExam(final Exam exam, final ConfigurationNode examConfig) {
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

}
