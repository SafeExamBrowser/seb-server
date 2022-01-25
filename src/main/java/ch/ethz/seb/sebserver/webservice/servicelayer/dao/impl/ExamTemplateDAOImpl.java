/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.IndicatorTemplate;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.IndicatorRecordDynamicSqlSupport;
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
    private final ExamDAO examDAO;
    private final JSONMapper jsonMapper;

    public ExamTemplateDAOImpl(
            final ExamTemplateRecordMapper examTemplateRecordMapper,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ExamDAO examDAO,
            final JSONMapper jsonMapper) {

        this.examTemplateRecordMapper = examTemplateRecordMapper;
        this.additionalAttributesDAO = additionalAttributesDAO;
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

            if (defaults.size() != 1) {
                throw new IllegalStateException("Expected one default but was: " + defaults.size());
            }

            return defaults.get(0);
        })
                .flatMap(this::toDomainModel);
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

        return Result.tryCatch(() -> this.examTemplateRecordMapper
                .selectByExample()
                .where(
                        ExamTemplateRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(filterMap.getInstitutionId()))
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
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<ExamTemplate> createNew(final ExamTemplate data) {
        return Result.tryCatch(() -> {

            checkUniqueName(data);

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
                    BooleanUtils.toInteger(data.institutionalDefault));

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

            final Collection<IndicatorTemplate> indicatorTemplates = data.getIndicatorTemplates();
            final String indicatorsJSON = (indicatorTemplates != null && !indicatorTemplates.isEmpty())
                    ? this.jsonMapper.writeValueAsString(indicatorTemplates)
                    : null;

            final ExamTemplateRecord newRecord = new ExamTemplateRecord(
                    data.id,
                    null,
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
                    BooleanUtils.toInteger(data.institutionalDefault));

            this.examTemplateRecordMapper.updateByPrimaryKeySelective(newRecord);

            if (!data.examAttributes.isEmpty()) {
                data.examAttributes
                        .entrySet()
                        .stream()
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
    public Set<EntityDependency> getDependencies(final BulkAction bulkAction) {
        return Collections.emptySet();
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            log.info("Delete exam templates: {}", all);

            final List<Long> ids = extractListOfPKs(all);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            ids.stream()
                    .forEach(id -> {
                        final Collection<EntityKey> deletedReferences = this.examDAO
                                .deleteTemplateReferences(id)
                                .getOrThrow();

                        if (deletedReferences != null && !deletedReferences.isEmpty()) {
                            log.info("Deleted template references for exams: {}", deletedReferences);
                        }
                    });

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
                                    attr -> attr.getName(),
                                    attr -> attr.getValue())))
                    .onError(error -> log.error("Failed to load exam attributes for template: {}", record, error))
                    .getOrElse(() -> Collections.emptyMap());

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
                    indicators,
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
                        .stream()
                        .forEach(this::resetDefault);

            } catch (final Exception e) {
                log.error("Unexpected error while trying to reset institutional default", e);
            }
        }
    }

    private void resetDefault(final ExamTemplateRecord record) {
        try {

            this.examTemplateRecordMapper
                    .updateByPrimaryKeySelective(new ExamTemplateRecord(
                            record.getId(),
                            null, null, null, null, null, null, null,
                            0));

        } catch (final Exception e) {
            log.error("Failed to reset institutional default for exam template: {}", record, e);
        }
    }

}
