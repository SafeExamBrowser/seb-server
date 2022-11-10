/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey.EncryptionType;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey.KeyType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SecurityKeyRegistryRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SecurityKeyRegistryRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.SecurityKeyRegistryRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SecurityKeyRegistryDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class SecurityKeyRegistryDAOImpl implements SecurityKeyRegistryDAO {

    private final SecurityKeyRegistryRecordMapper securityKeyRegistryRecordMapper;

    public SecurityKeyRegistryDAOImpl(final SecurityKeyRegistryRecordMapper securityKeyRegistryRecordMapper) {
        this.securityKeyRegistryRecordMapper = securityKeyRegistryRecordMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.SEB_SECURITY_KEY_REGISTRY;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<SecurityKey> byPK(final Long id) {
        return recordByPK(id)
                .map(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<SecurityKey>> allMatching(
            final FilterMap filterMap,
            final Predicate<SecurityKey> predicate) {

        return Result.tryCatch(() -> this.securityKeyRegistryRecordMapper
                .selectByExample()
                .where(
                        SecurityKeyRegistryRecordDynamicSqlSupport.institutionId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getLong(SecurityKey.FILTER_ATTR_INSTITUTION)))
                .and(
                        SecurityKeyRegistryRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getLong(SecurityKey.FILTER_ATTR_EXAM_ID)))
                .and(
                        SecurityKeyRegistryRecordDynamicSqlSupport.examTemplateId,
                        SqlBuilder.isEqualToWhenPresent(
                                filterMap.getLong(SecurityKey.FILTER_ATTR_EXAM_TEMPLATE_ID)))
                .and(
                        SecurityKeyRegistryRecordDynamicSqlSupport.type,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getString(SecurityKey.FILTER_ATTR_KEY_TYPE)))
                .and(
                        SecurityKeyRegistryRecordDynamicSqlSupport.encryptionType,
                        SqlBuilder.isEqualToWhenPresent(
                                filterMap.getString(SecurityKey.FILTER_ATTR_ENCRYPTION_TYPE)))
                .and(
                        SecurityKeyRegistryRecordDynamicSqlSupport.tag,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getString(SecurityKey.FILTER_ATTR_TAG)))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModelStream)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<SecurityKey>> allOf(final Set<Long> pks) {

        return Result.tryCatch(() -> {

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.securityKeyRegistryRecordMapper
                    .selectByExample()
                    .where(SecurityKeyRegistryRecordDynamicSqlSupport.id, SqlBuilder.isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModelStream)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<SecurityKey> createNew(final SecurityKey data) {
        return Result.tryCatch(() -> {

            checkUniqueTag(data);

            final SecurityKeyRegistryRecord newRecord = new SecurityKeyRegistryRecord(
                    null,
                    data.institutionId,
                    Utils.getEnumName(data.keyType),
                    Utils.toString(data.key),
                    data.tag,
                    data.examId,
                    data.examTemplateId,
                    Utils.getEnumName(data.encryptionType));

            this.securityKeyRegistryRecordMapper.insert(newRecord);
            return newRecord;
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<SecurityKey> save(final SecurityKey data) {
        return Result.tryCatch(() -> {

            checkUniqueTag(data);

            final SecurityKeyRegistryRecord newRecord = new SecurityKeyRegistryRecord(
                    null,
                    data.institutionId,
                    Utils.getEnumName(data.keyType),
                    Utils.toString(data.key),
                    data.tag,
                    data.examId,
                    data.examTemplateId,
                    Utils.getEnumName(data.encryptionType));

            this.securityKeyRegistryRecordMapper.updateByPrimaryKeySelective(newRecord);
            return this.securityKeyRegistryRecordMapper.selectByPrimaryKey(data.id);
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            this.securityKeyRegistryRecordMapper.deleteByExample()
                    .where(SecurityKeyRegistryRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.SEB_SECURITY_KEY_REGISTRY))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional
    public Result<SecurityKey> registerCopyForExam(final Long keyId, final Long examId) {
        return recordByPK(keyId)
                .map(rec -> {

                    // first check if there is already an entry
                    final Long execute = this.securityKeyRegistryRecordMapper
                            .countByExample()
                            .where(SecurityKeyRegistryRecordDynamicSqlSupport.examId, isEqualTo(examId))
                            .and(SecurityKeyRegistryRecordDynamicSqlSupport.key, isEqualTo(rec.getKey()))
                            .build()
                            .execute();

                    if (execute != null && execute.longValue() > 0) {
                        return rec;
                    }

                    // create new entry
                    final SecurityKeyRegistryRecord newRecord = new SecurityKeyRegistryRecord(
                            null,
                            rec.getInstitutionId(),
                            rec.getType(),
                            rec.getKey(),
                            rec.getTag(),
                            examId,
                            null,
                            rec.getEncryptionType());

                    this.securityKeyRegistryRecordMapper.insert(newRecord);
                    return newRecord;
                })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<SecurityKey> registerCopyForExamTemplate(final Long keyId, final Long examTemplateId) {
        return recordByPK(keyId)
                .map(rec -> {

                    // first check if there is already an entry
                    final Long execute = this.securityKeyRegistryRecordMapper
                            .countByExample()
                            .where(SecurityKeyRegistryRecordDynamicSqlSupport.examTemplateId, isEqualTo(examTemplateId))
                            .and(SecurityKeyRegistryRecordDynamicSqlSupport.key, isEqualTo(rec.getKey()))
                            .build()
                            .execute();

                    if (execute != null && execute.longValue() > 0) {
                        return rec;
                    }

                    // create new entry
                    final SecurityKeyRegistryRecord newRecord = new SecurityKeyRegistryRecord(
                            null,
                            rec.getInstitutionId(),
                            rec.getType(),
                            rec.getKey(),
                            rec.getTag(),
                            null,
                            examTemplateId,
                            rec.getEncryptionType());

                    this.securityKeyRegistryRecordMapper.insert(newRecord);
                    return newRecord;
                })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<SecurityKey>> getAll(final Long institutionId, final Long examId, final KeyType type) {
        return Result.tryCatch(() -> {

            final List<SecurityKey> resultGlobal = this.securityKeyRegistryRecordMapper
                    .selectByExample()
                    .where(
                            SecurityKeyRegistryRecordDynamicSqlSupport.institutionId,
                            SqlBuilder.isEqualTo(institutionId))
                    .and(
                            SecurityKeyRegistryRecordDynamicSqlSupport.type,
                            isEqualToWhenPresent((type == null) ? null : type.name()))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModelStream)
                    .flatMap(DAOLoggingSupport::logAndSkipOnError)
                    .collect(Collectors.toList());

            if (examId != null) {
                final List<SecurityKey> ResultsForExam = this.securityKeyRegistryRecordMapper
                        .selectByExample()
                        .where(
                                SecurityKeyRegistryRecordDynamicSqlSupport.institutionId,
                                SqlBuilder.isEqualTo(institutionId))
                        .and(
                                SecurityKeyRegistryRecordDynamicSqlSupport.examId,
                                isEqualTo(examId))
                        .and(
                                SecurityKeyRegistryRecordDynamicSqlSupport.type,
                                isEqualToWhenPresent((type == null) ? null : type.name()))
                        .build()
                        .execute()
                        .stream()
                        .map(this::toDomainModelStream)
                        .flatMap(DAOLoggingSupport::logAndSkipOnError)
                        .collect(Collectors.toList());

                final ArrayList<SecurityKey> result = new ArrayList<>(resultGlobal);
                result.addAll(ResultsForExam);
                return result;
            }

            return resultGlobal;
        });
    }

    @Override
    public void notifyExamDeletion(final ExamDeletionEvent event) {
        try {

            final Integer deleted = this.securityKeyRegistryRecordMapper.deleteByExample()
                    .where(SecurityKeyRegistryRecordDynamicSqlSupport.examId, SqlBuilder.isIn(event.ids))
                    .build()
                    .execute();

            if (log.isDebugEnabled()) {
                log.debug("Deleted {} records for exams: {}", deleted, event.ids);
            }

        } catch (final Exception e) {
            log.error("Failed to delete all for exams: {}", event.ids, e);
        }
    }

    @Override
    public void notifyExamTemplateDeletion(final ExamTemplateDeletionEvent event) {
        try {

            final Integer deleted = this.securityKeyRegistryRecordMapper.deleteByExample()
                    .where(SecurityKeyRegistryRecordDynamicSqlSupport.examTemplateId, SqlBuilder.isIn(event.ids))
                    .build()
                    .execute();

            if (log.isDebugEnabled()) {
                log.debug("Deleted {} records for exam templates: {}", deleted, event.ids);
            }

        } catch (final Exception e) {
            log.error("Failed to delete all for exam templates: {}", event.ids, e);
        }
    }

    private Result<SecurityKeyRegistryRecord> recordByPK(final Long pk) {
        return Result.tryCatch(() -> this.securityKeyRegistryRecordMapper.selectByPrimaryKey(pk));
    }

    private Result<SecurityKey> toDomainModelStream(final SecurityKeyRegistryRecord rec) {
        return Result.tryCatch(() -> toDomainModel(rec));
    }

    private SecurityKey toDomainModel(final SecurityKeyRegistryRecord rec) {
        return new SecurityKey(
                rec.getId(),
                rec.getInstitutionId(),
                KeyType.byString(rec.getType()),
                rec.getKey(),
                rec.getTag(),
                rec.getExamId(),
                rec.getExamTemplateId(),
                EncryptionType.byString(rec.getEncryptionType()));
    }

    private void checkUniqueTag(final SecurityKey securityKeyRegistry) {
        final Long count = this.securityKeyRegistryRecordMapper
                .countByExample()
                .where(SecurityKeyRegistryRecordDynamicSqlSupport.tag, isEqualTo(securityKeyRegistry.tag))
                .and(SecurityKeyRegistryRecordDynamicSqlSupport.id, isNotEqualToWhenPresent(securityKeyRegistry.id))
                .build()
                .execute();

        if (count != null && count > 0) {
            throw new FieldValidationException(
                    Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_TAG,
                    "institution:name:tag.notunique");
        }
    }

}
