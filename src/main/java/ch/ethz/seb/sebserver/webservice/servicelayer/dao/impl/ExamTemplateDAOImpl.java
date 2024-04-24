/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroupTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.IndicatorTemplate;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.IndicatorRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamTemplateRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamTemplateDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class ExamTemplateDAOImpl implements ExamTemplateDAO {

    private final ExamTemplateRecordMapper examTemplateRecordMapper;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ExamDAO examDAO;
    private final JSONMapper jsonMapper;

    public ExamTemplateDAOImpl(
            final ExamTemplateRecordMapper examTemplateRecordMapper,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ApplicationEventPublisher applicationEventPublisher,
            final ExamDAO examDAO,
            final JSONMapper jsonMapper) {

        this.examTemplateRecordMapper = examTemplateRecordMapper;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.applicationEventPublisher = applicationEventPublisher;
        this.examDAO = examDAO;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.EXAM_TEMPLATE;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ExamTemplate> byPK(final Long id) {
        return recordById(id)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ExamTemplate> getInstitutionalDefault(final Long institutionId) {
        return Result.tryCatch(() -> {

            final List<ExamTemplateRecord> defaults = this.examTemplateRecordMapper.selectByExample()
                    .where(
                            ExamTemplateRecordDynamicSqlSupport.institutionId,
                            isEqualTo(institutionId))
                    .and(
                            ExamTemplateRecordDynamicSqlSupport.institutionalDefault,
                            isNotEqualTo(0))
                    .build()
                    .execute();

            if (defaults == null || defaults.isEmpty()) {
                throw new ResourceNotFoundException(EntityType.EXAM_TEMPLATE, String.valueOf(institutionId));
            }

            return defaults.get(0);
        })
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ExamTemplate>> getAllForLMSIntegration(final Long institutionId) {
        return Result.tryCatch(() -> this.examTemplateRecordMapper.selectByExample()
                .where(
                        ExamTemplateRecordDynamicSqlSupport.institutionId,
                        isEqualTo(institutionId))
                .and(
                        lmsIntegration,
                        isNotEqualTo(0))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ExamTemplate>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.examTemplateRecordMapper.selectByExample()
                    .where(IndicatorRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ExamTemplate>> allMatching(
            final FilterMap filterMap,
            final Predicate<ExamTemplate> predicate) {

        return Result.tryCatch(() -> {
            final QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ExamTemplateRecord>>>.QueryExpressionWhereBuilder whereClause =
                    (filterMap.getBoolean(FilterMap.ATTR_ADD_INSITUTION_JOIN))
                            ? this.examTemplateRecordMapper
                                    .selectByExample()
                                    .join(InstitutionRecordDynamicSqlSupport.institutionRecord)
                                    .on(InstitutionRecordDynamicSqlSupport.id,
                                            SqlBuilder.equalTo(ExamTemplateRecordDynamicSqlSupport.institutionId))
                                    .where(
                                            ExamTemplateRecordDynamicSqlSupport.institutionId,
                                            isEqualToWhenPresent(filterMap.getInstitutionId()))
                            : this.examTemplateRecordMapper
                                    .selectByExample()
                                    .where(
                                            ExamTemplateRecordDynamicSqlSupport.institutionId,
                                            isEqualToWhenPresent(filterMap.getInstitutionId()));

            return whereClause
                    .and(
                            ExamTemplateRecordDynamicSqlSupport.name,
                            isLikeWhenPresent(filterMap.getExamTemplateName()))
                    .and(
                            ExamTemplateRecordDynamicSqlSupport.examType,
                            isEqualToWhenPresent(filterMap.getString(ExamTemplate.FILTER_ATTR_EXAM_TYPE)))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .filter(predicate)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<ExamTemplate> createNew(final ExamTemplate data) {
        return Result.tryCatch(() -> {

            checkUniqueName(data);
            checkUniqueDefault(data);

            final Collection<IndicatorTemplate> indicatorTemplates = data.getIndicatorTemplates();
            final String indicatorsJSON = (indicatorTemplates != null && !indicatorTemplates.isEmpty())
                    ? this.jsonMapper.writeValueAsString(indicatorTemplates)
                    : null;

            final ExamTemplateRecord newRecord = new ExamTemplateRecord(
                    null,
                    data.institutionId,
                    data.configTemplateId,
                    data.name,
                    data.description,
                    (data.examType != null)
                            ? data.examType.name()
                            : null,
                    (data.supporter != null)
                            ? StringUtils.join(data.supporter, Constants.LIST_SEPARATOR_CHAR)
                            : null,
                    indicatorsJSON,
                    BooleanUtils.toInteger(data.institutionalDefault),
                    BooleanUtils.toInteger(data.lmsIntegration),
                    data.clientConfigurationId);

            final String quitPassword = data.getExamAttributes().get(ExamTemplate.ATTR_QUIT_PASSWORD);
            if (StringUtils.isNotBlank(quitPassword)) {
                this.additionalAttributesDAO.saveAdditionalAttribute(
                        EntityType.EXAM_TEMPLATE,
                        data.id,
                        ExamTemplate.ATTR_QUIT_PASSWORD,
                        quitPassword);
            }

            this.examTemplateRecordMapper.insert(newRecord);
            return newRecord;
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ExamTemplate> save(final ExamTemplate data) {
        return Result.tryCatch(() -> {

            checkUniqueName(data);
            checkUniqueDefault(data);

            final String supporter = (data.supporter != null)
                    ? StringUtils.join(data.supporter, Constants.LIST_SEPARATOR_CHAR)
                    : null;

            UpdateDSL.updateWithMapper(examTemplateRecordMapper::update, examTemplateRecord)
                .set(configurationTemplateId).equalTo(data.configTemplateId)
                .set(name).equalTo(data.name)
                .set(description).equalTo(data.description)
                .set(examType).equalToWhenPresent((data.examType != null) ? data.examType.name() : null)
                .set(ExamTemplateRecordDynamicSqlSupport.supporter).equalTo(supporter)
                .set(institutionalDefault).equalTo(BooleanUtils.toInteger(data.institutionalDefault))
                .set(lmsIntegration).equalTo(BooleanUtils.toInteger(data.lmsIntegration))
                .set(clientConfigurationId).equalTo(data.clientConfigurationId)
                .where(id, isEqualTo(data.id))
                .build()
                .execute();

            if (!data.examAttributes.isEmpty()) {
                data.examAttributes
                        .entrySet()
                        .forEach(entry -> this.additionalAttributesDAO.saveAdditionalAttribute(
                                EntityType.EXAM_TEMPLATE,
                                data.id,
                                entry.getKey(),
                                entry.getValue()));
            }

            return this.examTemplateRecordMapper.selectByPrimaryKey(data.id);
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<IndicatorTemplate> createNewIndicatorTemplate(final IndicatorTemplate indicatorTemplate) {
        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Create new indicator template: {}", indicatorTemplate);
            }

            final Long examTemplatePK = indicatorTemplate.examTemplateId;
            final Collection<IndicatorTemplate> indicators = extractIndicatorTemplates(examTemplatePK);

            checkUniqueIndicatorName(indicatorTemplate, indicators);

            final IndicatorTemplate newIndicatorTemplate = new IndicatorTemplate(
                    getNextIndicatorId(indicators),
                    indicatorTemplate);

            final List<IndicatorTemplate> newIndicators = new ArrayList<>(indicators);
            newIndicators.add(newIndicatorTemplate);

            storeIndicatorTemplates(examTemplatePK, newIndicators);

            return newIndicatorTemplate;
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<IndicatorTemplate> saveIndicatorTemplate(final IndicatorTemplate indicatorTemplate) {
        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Save indicator template: {}", indicatorTemplate);
            }

            final Long examTemplatePK = indicatorTemplate.examTemplateId;
            final Collection<IndicatorTemplate> indicators = extractIndicatorTemplates(examTemplatePK);

            checkUniqueIndicatorName(indicatorTemplate, indicators);

            final List<IndicatorTemplate> newIndicators = indicators
                    .stream()
                    .map(i -> indicatorTemplate.id.equals(i.id) ? indicatorTemplate : i)
                    .collect(Collectors.toList());

            storeIndicatorTemplates(examTemplatePK, newIndicators);

            return indicatorTemplate;
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<EntityKey> deleteIndicatorTemplate(
            final String examTemplateId,
            final String indicatorTemplateId) {

        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug(
                        "Delete indicator template for exam template: {} indicator template id {}",
                        examTemplateId,
                        indicatorTemplateId);
            }

            final Long examTemplatePK = Long.valueOf(examTemplateId);
            final Collection<IndicatorTemplate> indicators = extractIndicatorTemplates(examTemplatePK);

            final List<IndicatorTemplate> newIndicators = indicators.stream()
                    .filter(indicatorTemplate -> !indicatorTemplateId.equals(indicatorTemplate.getModelId()))
                    .collect(Collectors.toList());

            storeIndicatorTemplates(examTemplatePK, newIndicators);

            return new EntityKey(indicatorTemplateId, EntityType.INDICATOR);
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ClientGroupTemplate> createNewClientGroupTemplate(final ClientGroupTemplate clientGroupTemplate) {
        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Create new clientGroup template: {}", clientGroupTemplate);
            }

            final Long examTemplateId = clientGroupTemplate.examTemplateId;
            final Collection<ClientGroupTemplate> clientGroups =
                    loadClientGroupTemplates(examTemplateId);

            checkUniqueClientGroupName(clientGroupTemplate, clientGroups);

            final ClientGroupTemplate newClientGroupTemplate = new ClientGroupTemplate(
                    getNextClientGroupId(clientGroups),
                    clientGroupTemplate);

            final List<ClientGroupTemplate> newClientGroups = new ArrayList<>(clientGroups);
            newClientGroups.add(newClientGroupTemplate);

            storeClientGroupTemplates(examTemplateId, newClientGroups);

            return newClientGroupTemplate;
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ClientGroupTemplate> saveClientGroupTemplate(final ClientGroupTemplate clientGroupTemplate) {
        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Save client group template: {}", clientGroupTemplate);
            }

            final Long examTemplateId = clientGroupTemplate.examTemplateId;
            final Collection<ClientGroupTemplate> clientGroups =
                    loadClientGroupTemplates(examTemplateId);

            checkUniqueClientGroupName(clientGroupTemplate, clientGroups);

            final List<ClientGroupTemplate> newClientGroups = clientGroups
                    .stream()
                    .map(i -> clientGroupTemplate.id.equals(i.id) ? clientGroupTemplate : i)
                    .collect(Collectors.toList());

            storeClientGroupTemplates(examTemplateId, newClientGroups);

            return clientGroupTemplate;
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<EntityKey> deleteClientGroupTemplate(
            final String examTemplateId,
            final String clientGroupTemplateId) {

        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug(
                        "Delete client group template for exam template: {} indicator template id {}",
                        examTemplateId,
                        clientGroupTemplateId);
            }

            final Long examTemplatePK = Long.valueOf(examTemplateId);
            final Collection<ClientGroupTemplate> clientGroups =
                    loadClientGroupTemplates(examTemplatePK);

            final List<ClientGroupTemplate> newClientGroups = clientGroups.stream()
                    .filter(clientGroupTemplate -> !clientGroupTemplateId.equals(clientGroupTemplate.getModelId()))
                    .collect(Collectors.toList());

            storeClientGroupTemplates(examTemplatePK, newClientGroups);

            return new EntityKey(clientGroupTemplateId, EntityType.CLIENT_GROUP);
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    public Set<EntityDependency> getDependencies(final BulkAction bulkAction) {
        // all of institution
        if (bulkAction.sourceType == EntityType.INSTITUTION) {
            return getDependencies(bulkAction, this::allIdsOfInstitution);
        }

        return Collections.emptySet();
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Delete exam templates: {}", all);
            }

            final List<Long> ids = extractListOfPKs(all);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            // notify exam deletion listener about following deletion, to cleanup stuff before deletion
            this.applicationEventPublisher.publishEvent(new ExamTemplateDeletionEvent(ids));

            ids.forEach(id -> {
                final Collection<EntityKey> deletedReferences = this.examDAO
                        .deleteTemplateReferences(id)
                        .getOrThrow();

                if (deletedReferences != null && !deletedReferences.isEmpty()) {
                    log.info("Deleted template references for exams: {}", deletedReferences);
                }
            });

            // delete all additional attributes
            ids.forEach(id -> this.additionalAttributesDAO.deleteAll(EntityType.EXAM_TEMPLATE, id));

            this.examTemplateRecordMapper.deleteByExample()
                    .where(ExamTemplateRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.EXAM_TEMPLATE))
                    .collect(Collectors.toList());
        });
    }

    private Result<ExamTemplateRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {

            final ExamTemplateRecord record = this.examTemplateRecordMapper.selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        entityType(),
                        String.valueOf(id));
            }

            return record;
        });
    }

    private Result<ExamTemplate> toDomainModel(final ExamTemplateRecord record) {
        return Result.tryCatch(() -> {

            final String indicatorTemplatesString = record.getIndicatorTemplates();
            final Collection<IndicatorTemplate> indicators = (StringUtils.isNotBlank(indicatorTemplatesString))
                    ? this.jsonMapper.readValue(indicatorTemplatesString,
                            new TypeReference<Collection<IndicatorTemplate>>() {
                            })
                    : null;

            final Map<String, String> examAttributes = this.additionalAttributesDAO
                    .getAdditionalAttributes(EntityType.EXAM_TEMPLATE, record.getId())
                    .map(attrs -> attrs.stream()
                            .collect(Collectors.toMap(
                                    AdditionalAttributeRecord::getName,
                                    AdditionalAttributeRecord::getValue)))
                    .onError(error -> log.error("Failed to load exam attributes for template: {}", record, error))
                    .getOrElse(Collections::emptyMap);

            final Collection<String> supporter = (StringUtils.isNotBlank(record.getSupporter()))
                    ? Arrays.asList(StringUtils.split(record.getSupporter(), Constants.LIST_SEPARATOR_CHAR))
                    : null;

            final ExamType examType = (record.getExamType() != null)
                    ? ExamType.valueOf(record.getExamType())
                    : ExamType.UNDEFINED;

            final Collection<ClientGroupTemplate> clientGroupTemplates = loadClientGroupTemplates(record.getId());

            return new ExamTemplate(
                    record.getId(),
                    record.getInstitutionId(),
                    record.getName(),
                    record.getDescription(),
                    examType,
                    supporter,
                    record.getConfigurationTemplateId(),
                    BooleanUtils.toBooleanObject(record.getInstitutionalDefault()),
                    BooleanUtils.toBooleanObject(record.getLmsIntegration()),
                    record.getClientConfigurationId(),
                    indicators,
                    clientGroupTemplates,
                    examAttributes);
        });
    }

    private void checkUniqueName(final ExamTemplate examTemplate) {
        final Long count = this.examTemplateRecordMapper
                .countByExample()
                .where(ExamTemplateRecordDynamicSqlSupport.name, isEqualTo(examTemplate.name))
                .and(ExamTemplateRecordDynamicSqlSupport.institutionId, isEqualTo(examTemplate.institutionId))
                .and(ExamTemplateRecordDynamicSqlSupport.id, isNotEqualToWhenPresent(examTemplate.id))
                .build()
                .execute();

        if (count != null && count > 0) {
            throw new FieldValidationException(
                    "name",
                    "examTemplate:name:exists");
        }
    }

    private void checkUniqueDefault(final ExamTemplate data) {
        if (data.institutionalDefault) {
            try {

                this.examTemplateRecordMapper.selectByExample()
                        .where(
                                ExamTemplateRecordDynamicSqlSupport.institutionId,
                                isEqualTo(data.institutionId))
                        .and(
                                ExamTemplateRecordDynamicSqlSupport.institutionalDefault,
                                isNotEqualTo(0))
                        .build()
                        .execute()
                        .forEach(this::resetDefault);

            } catch (final Exception e) {
                log.error("Unexpected error while trying to reset institutional default", e);
            }
        }
    }

    private void resetDefault(final ExamTemplateRecord record) {
        try {

            UpdateDSL.updateWithMapper(examTemplateRecordMapper::update, examTemplateRecord)
                    .set(institutionalDefault).equalTo(0)
                    .where(id, isEqualTo(record::getId))
                    .build()
                    .execute();

        } catch (final Exception e) {
            log.error("Failed to reset institutional default for exam template: {}", record, e);
        }
    }

    private void checkUniqueIndicatorName(
            final IndicatorTemplate indicatorTemplate,
            final Collection<IndicatorTemplate> indicators) {

        // check unique name
        indicators.stream()
                .filter(it -> !Objects.equals(it, indicatorTemplate) && Objects.equals(it.name, indicatorTemplate.name))
                .findAny()
                .ifPresent(it -> {
                    throw new FieldValidationException(
                            "name",
                            "indicatorTemplate:name:exists");
                });
    }

    private void checkUniqueClientGroupName(
            final ClientGroupTemplate clientGroupTemplate,
            final Collection<ClientGroupTemplate> clientGroups) {

        // check unique name
        clientGroups.stream()
                .filter(it -> !Objects.equals(it.id, clientGroupTemplate.id)
                        && Objects.equals(it.name, clientGroupTemplate.name))
                .findAny()
                .ifPresent(it -> {
                    throw new FieldValidationException(
                            "name",
                            "clientGroupTemplate:name:exists");
                });
    }

    private long getNextIndicatorId(final Collection<IndicatorTemplate> indicators) {
        return indicators.stream()
                .map(IndicatorTemplate::getId)
                .max(Long::compare)
                .orElse(-1L) + 1;
    }

    private long getNextClientGroupId(final Collection<ClientGroupTemplate> clientGroups) {
        return clientGroups.stream()
                .map(ClientGroupTemplate::getId)
                .max(Long::compare)
                .orElse(-1L) + 1;
    }

    private Result<Collection<EntityDependency>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> this.examTemplateRecordMapper.selectByExample()
                .where(ExamTemplateRecordDynamicSqlSupport.institutionId,
                        isEqualTo(Long.valueOf(institutionKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        institutionKey,
                        new EntityKey(rec.getId(), EntityType.EXAM_TEMPLATE),
                        rec.getName(),
                        rec.getDescription()))
                .collect(Collectors.toList()));
    }

    private Collection<IndicatorTemplate> extractIndicatorTemplates(final Long examTemplatePK)
            throws JsonProcessingException, JsonMappingException {

        final ExamTemplateRecord examTemplateRec = this.examTemplateRecordMapper
                .selectByPrimaryKey(examTemplatePK);
        final String indicatorTemplatesJSON = examTemplateRec.getIndicatorTemplates();
        return (StringUtils.isNotBlank(indicatorTemplatesJSON))
                ? this.jsonMapper.readValue(
                        indicatorTemplatesJSON,
                        new TypeReference<Collection<IndicatorTemplate>>() {
                        })
                : Collections.emptyList();
    }

    private void storeIndicatorTemplates(final Long examTemplatePK, final List<IndicatorTemplate> newIndicators)
            throws JsonProcessingException {

        final String newIndicatorTemplatesJSON = newIndicators.isEmpty()
                ? StringUtils.EMPTY
                : this.jsonMapper.writeValueAsString(newIndicators);

        UpdateDSL.updateWithMapper(examTemplateRecordMapper::update, examTemplateRecord)
                .set(indicatorTemplates).equalTo(newIndicatorTemplatesJSON)
                .where(id, isEqualTo(examTemplatePK))
                .build()
                .execute();
    }

    private void storeClientGroupTemplates(final Long examTemplateId, final List<ClientGroupTemplate> newClientGroups)
            throws JsonProcessingException {

        final String newIndicatorTemplatesJSON = newClientGroups.isEmpty()
                ? StringUtils.EMPTY
                : this.jsonMapper.writeValueAsString(newClientGroups);

        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM_TEMPLATE,
                examTemplateId,
                ExamTemplate.ATTR_CLIENT_GROUP_TEMPLATES,
                newIndicatorTemplatesJSON)
                .getOrThrow();
    }

    private Collection<ClientGroupTemplate> loadClientGroupTemplates(final Long examTemplatePK)
            throws JsonProcessingException, JsonMappingException {

        final String clientGroupTemplatesJSON = this.additionalAttributesDAO
                .getAdditionalAttribute(
                        EntityType.EXAM_TEMPLATE,
                        examTemplatePK,
                        ExamTemplate.ATTR_CLIENT_GROUP_TEMPLATES)
                .map(AdditionalAttributeRecord::getValue)
                .getOr(StringUtils.EMPTY);

        return (StringUtils.isNotBlank(clientGroupTemplatesJSON))
                ? this.jsonMapper.readValue(
                        clientGroupTemplatesJSON,
                        new TypeReference<Collection<ClientGroupTemplate>>() {
                        })
                : Collections.emptyList();
    }

}
