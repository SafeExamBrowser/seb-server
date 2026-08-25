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

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.model.exam.*;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.*;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
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
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamTemplateRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;

@Lazy
@Component
public class ExamTemplateDAOImpl implements ExamTemplateDAO {

    private static final String COPY_NAME_TEMPLATE = "%s (copy%s)";

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

            return defaults.getFirst();
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

            QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ExamTemplateRecord>>>.QueryExpressionWhereBuilder whereClause =
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

            // Exam Type filter, also multiple selection allowed
            // NOTE "UNDEFINED" must find both, "UNDEFINED" and NULL entries
            String exam_type = filterMap.getString(ExamTemplate.FILTER_ATTR_EXAM_TYPE);
            if (StringUtils.isNotBlank(exam_type)) {
                if (exam_type.contains(Constants.LIST_SEPARATOR)) {
                    final List<String> examTypes = Arrays.asList(StringUtils.split(exam_type, Constants.LIST_SEPARATOR));
                    if (examTypes.contains(ExamType.UNDEFINED.name())) {
                        whereClause = whereClause
                                .and(
                                        ExamTemplateRecordDynamicSqlSupport.examType,
                                        isIn(examTypes),
                                        or(ExamTemplateRecordDynamicSqlSupport.examType, isNull()));
                    } else {
                        whereClause = whereClause
                                .and(
                                        ExamTemplateRecordDynamicSqlSupport.examType,
                                        isIn(examTypes));
                    }

                } else {
                    if (Objects.equals(exam_type, ExamType.UNDEFINED.name())) {
                        whereClause = whereClause
                                .and(
                                        ExamTemplateRecordDynamicSqlSupport.examType,
                                        isEqualTo(exam_type),
                                        or(ExamTemplateRecordDynamicSqlSupport.examType, isNull()));
                    } else {
                        whereClause = whereClause
                                .and(
                                        ExamTemplateRecordDynamicSqlSupport.examType,
                                        isEqualTo(exam_type));
                    }
                }
            }


            return whereClause
                    .and(
                            ExamTemplateRecordDynamicSqlSupport.name,
                            isLikeWhenPresent(filterMap.getExamTemplateName()))
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

            final ExamTemplateRecord newRecord = new ExamTemplateRecord(
                    null,
                    data.institutionId,
                    data.configTemplateId,
                    data.name,
                    data.description,
                    (data.examType != null)
                            ? data.examType.name()
                            : ExamType.UNDEFINED.name(),
                    (data.supporter != null)
                            ? StringUtils.join(data.supporter, Constants.LIST_SEPARATOR_CHAR)
                            : null,
                    null,
                    BooleanUtils.toInteger(data.institutionalDefault),
                    BooleanUtils.toInteger(data.lmsIntegration),
                    data.clientConfigurationId);

            this.examTemplateRecordMapper.insert(newRecord);

            final Collection<IndicatorTemplate> indicatorTemplates = data.getIndicatorTemplates();
            if (indicatorTemplates != null && !indicatorTemplates.isEmpty()) {
                indicatorTemplates.forEach( t -> {
                    createNewIndicatorTemplate(new IndicatorTemplate(null, newRecord.getId(), t.name, t.type, t.defaultColor, t.defaultIcon, t.tags, t.thresholds))
                            .onError(error -> log.error("Failed to create new IndicatorTemplate: {t}", error));
                });
            }
            
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
                .set(configurationTemplateId).equalToWhenPresent(data.configTemplateId)
                .set(name).equalTo(data.name)
                .set(description).equalTo(data.description)
                .set(examType).equalToWhenPresent((data.examType != null) ? data.examType.name() : null)
                .set(ExamTemplateRecordDynamicSqlSupport.supporter).equalTo(supporter)
                .set(institutionalDefault).equalTo(BooleanUtils.toInteger(data.institutionalDefault))
                .set(lmsIntegration).equalTo(BooleanUtils.toInteger(data.lmsIntegration))
                .set(clientConfigurationId).equalToWhenPresent(data.clientConfigurationId)
                .where(id, isEqualTo(data.id))
                .build()
                .execute();

            if (!data.examAttributes.isEmpty()) {
                data.examAttributes.forEach((key, value) -> this.additionalAttributesDAO.saveAdditionalAttribute(
                        EntityType.EXAM_TEMPLATE,
                        data.id,
                        key,
                        value));
            }

            return data;
        })
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
    @Transactional(readOnly = true)
    public Result<Collection<ClientGroupTemplate>> getClientGroupTemplates(final Long examTemplateId) {
        return Result.tryCatch(() ->  loadClientGroupTemplates(examTemplateId));
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


    @Override
    @Transactional(readOnly = true)
    public String getCopyName(ExamTemplate sourceExamTemplate) {
        try {

            int count = 1;
            Long number = null;
            String newName = String.format(COPY_NAME_TEMPLATE, sourceExamTemplate.name, String.valueOf(Utils.getSecondsNow()));

            while (number == null || number > 0 ) {
                newName = String.format(COPY_NAME_TEMPLATE, sourceExamTemplate.name, count > 1 ? " " + count : "");
                number = this.examTemplateRecordMapper.countByExample()
                        .where(
                                ExamTemplateRecordDynamicSqlSupport.name,
                                isEqualTo(newName))
                        .build()
                        .execute();
                count++;
            }

            return newName;

        } catch (Exception e) {
            log.error("Failed to find valid copy name for Exam Template: {}, cause: {}, now use timestamp", sourceExamTemplate.name, e.getMessage());
            return String.format(COPY_NAME_TEMPLATE, sourceExamTemplate.name, String.valueOf(Utils.getSecondsNow()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAnyExamTemplateWithConfigTemplate(final Long configTemplateId) {
        try {

            Long execute = this.examTemplateRecordMapper.countByExample()
                    .where(
                            configurationTemplateId,
                            isEqualTo(configTemplateId))
                    .build()
                    .execute();

            return execute != null && execute > 0;

        } catch (Exception e) {
            log.error(
                    "Failed to check if there is any Exam Template that uses the configuration template with the id: {} case: {}",
                    configTemplateId,
                    e.getMessage());
        }

        // return true for safety reasons
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Long>> getAllIds() {
        return Result.tryCatch(() ->  this.examTemplateRecordMapper
                .selectIdsByExample()
                .build()
                .execute()
        );
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

            final Collection<String> supporter = (StringUtils.isNotBlank(record.getSupporter()))
                    ? Arrays.asList(StringUtils.split(record.getSupporter(), Constants.LIST_SEPARATOR_CHAR))
                    : null;

            final ExamType examType = (record.getExamType() != null)
                    ? ExamType.valueOf(record.getExamType())
                    : ExamType.UNDEFINED;

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
                    // NOTE: this data is later added if needed. Not needed for lists
                    Collections.emptyList(),
                    Collections.emptyMap());
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
