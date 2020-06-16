/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamConfigurationMapRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamConfigurationMapRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationNodeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamConfigurationMapRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class ExamConfigurationMapDAOImpl implements ExamConfigurationMapDAO {

    private final ExamRecordMapper examRecordMapper;
    private final ExamConfigurationMapRecordMapper examConfigurationMapRecordMapper;
    private final ConfigurationNodeRecordMapper configurationNodeRecordMapper;
    private final ClientCredentialService clientCredentialService;
    private final ExamDAO examDAO;

    protected ExamConfigurationMapDAOImpl(
            final ExamRecordMapper examRecordMapper,
            final ExamConfigurationMapRecordMapper examConfigurationMapRecordMapper,
            final ConfigurationNodeRecordMapper configurationNodeRecordMapper,
            final ClientCredentialService clientCredentialService,
            final ExamDAO examDAO) {

        this.examRecordMapper = examRecordMapper;
        this.examConfigurationMapRecordMapper = examConfigurationMapRecordMapper;
        this.configurationNodeRecordMapper = configurationNodeRecordMapper;
        this.clientCredentialService = clientCredentialService;
        this.examDAO = examDAO;
    }

    @Override
    public EntityType entityType() {
        return EntityType.EXAM_CONFIGURATION_MAP;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ExamConfigurationMap> byPK(final Long id) {
        return recordById(id)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ExamConfigurationMap>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper.selectByExample()
                .where(ExamConfigurationMapRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ExamConfigurationMap>> allMatching(
            final FilterMap filterMap,
            final Predicate<ExamConfigurationMap> predicate) {

        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper
                .selectByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.institutionId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        ExamConfigurationMapRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getExamConfigExamId()))
                .and(
                        ExamConfigurationMapRecordDynamicSqlSupport.configurationNodeId,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getExamConfigConfigId()))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ExamConfigurationMap> byMapping(final Long examId, final Long configurationNodeId) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper
                .selectByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualTo(examId))
                .and(
                        ExamConfigurationMapRecordDynamicSqlSupport.configurationNodeId,
                        SqlBuilder.isEqualTo(configurationNodeId))
                .build()
                .execute()
                .stream()
                .map(this::toDomainModel)
                .flatMap(DAOLoggingSupport::logAndSkipOnError)
                .collect(Utils.toSingleton()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<CharSequence> getConfigPasswordCipher(final Long examId, final Long configurationNodeId) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper
                .selectByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualTo(examId))
                .and(
                        ExamConfigurationMapRecordDynamicSqlSupport.configurationNodeId,
                        SqlBuilder.isEqualTo(configurationNodeId))
                .build()
                .execute()
                .stream()
                .collect(Utils.toSingleton()))
                .map(ExamConfigurationMapRecord::getEncryptSecret);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Long> getDefaultConfigurationNode(final Long examId) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper
                .selectByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualTo(examId))
                .and(
                        ExamConfigurationMapRecordDynamicSqlSupport.userNames,
                        SqlBuilder.isNull())
                .build()
                .execute()
                .stream()
                .map(ExamConfigurationMapRecord::getConfigurationNodeId)
                .collect(Utils.toSingleton()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Long> getUserConfigurationNodeId(final Long examId, final String userId) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper
                .selectByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualTo(examId))
                .and(
                        ExamConfigurationMapRecordDynamicSqlSupport.userNames,
                        SqlBuilder.isLike(Utils.toSQLWildcard(userId)))
                .build()
                .execute()
                .stream()
                .map(ExamConfigurationMapRecord::getConfigurationNodeId)
                .collect(Utils.toSingleton()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Long>> getConfigurationNodeIds(final Long examId) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper
                .selectByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualTo(examId))
                .build()
                .execute()
                .stream()
                .map(ExamConfigurationMapRecord::getConfigurationNodeId)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<ExamConfigurationMap> createNew(final ExamConfigurationMap data) {
        return checkMappingIntegrity(data)
                .map(config -> {
                    final ExamConfigurationMapRecord newRecord = new ExamConfigurationMapRecord(
                            null,
                            data.institutionId,
                            data.examId,
                            data.configurationNodeId,
                            data.userNames,
                            getEncryptionPassword(data));

                    this.examConfigurationMapRecordMapper.insert(newRecord);
                    return newRecord;
                })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ExamConfigurationMap> save(final ExamConfigurationMap data) {
        return Result.tryCatch(() -> {

            final ExamConfigurationMapRecord newRecord = new ExamConfigurationMapRecord(
                    data.id,
                    null,
                    null,
                    null,
                    data.userNames,
                    getEncryptionPassword(data));

            this.examConfigurationMapRecordMapper.updateByPrimaryKeySelective(newRecord);
            return this.examConfigurationMapRecordMapper.selectByPrimaryKey(data.id);
        })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);

            this.examConfigurationMapRecordMapper.deleteByExample()
                    .where(ExamConfigurationMapRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.EXAM_CONFIGURATION_MAP))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityKey> getDependencies(final BulkAction bulkAction) {
        if (bulkAction.type == BulkActionType.ACTIVATE || bulkAction.type == BulkActionType.DEACTIVATE) {
            return Collections.emptySet();
        }

        // define the select function in case of source type
        Function<EntityKey, Result<Collection<EntityKey>>> selectionFunction;
        switch (bulkAction.sourceType) {
            case INSTITUTION:
                selectionFunction = this::allIdsOfInstitution;
                break;
            case LMS_SETUP:
                selectionFunction = this::allIdsOfLmsSetup;
                break;
            case EXAM:
                selectionFunction = this::allIdsOfExam;
                break;
            case CONFIGURATION_NODE:
                selectionFunction = this::allIdsOfConfig;
                break;
            default:
                selectionFunction = key -> Result.of(Collections.emptyList()); //empty select function
                break;
        }

        return getDependencies(bulkAction, selectionFunction);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Long>> getExamIdsForConfigNodeId(final Long configurationNodeId) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper.selectByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.configurationNodeId,
                        isEqualTo(configurationNodeId))
                .build()
                .execute()
                .stream()
                .map(ExamConfigurationMapRecord::getExamId)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Long>> getExamIdsForConfigId(final Long configurationId) {
        return Result.tryCatch(() -> this.configurationNodeRecordMapper.selectIdsByExample()
                .leftJoin(ConfigurationRecordDynamicSqlSupport.configurationRecord)
                .on(
                        ConfigurationRecordDynamicSqlSupport.configurationNodeId,
                        equalTo(ConfigurationNodeRecordDynamicSqlSupport.id))
                .where(
                        ConfigurationRecordDynamicSqlSupport.id,
                        isEqualTo(configurationId))
                .build()
                .execute()
                .stream()
                .collect(Utils.toSingleton()))
                .flatMap(this::getExamIdsForConfigNodeId);
    }

    private Result<ExamConfigurationMapRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final ExamConfigurationMapRecord record = this.examConfigurationMapRecordMapper
                    .selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.EXAM_CONFIGURATION_MAP,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private Result<ExamConfigurationMap> toDomainModel(final ExamConfigurationMapRecord record) {
        return Result.tryCatch(() -> {

            final ConfigurationNodeRecord config = this.configurationNodeRecordMapper
                    .selectByPrimaryKey(record.getConfigurationNodeId());
            final String status = config.getStatus();

            final Exam exam = this.examDAO.byPK(record.getExamId())
                    .getOr(null);

            return new ExamConfigurationMap(
                    record.getId(),
                    record.getInstitutionId(),
                    record.getExamId(),
                    (exam != null) ? exam.name : null,
                    (exam != null) ? exam.description : null,
                    (exam != null) ? exam.startTime : null,
                    (exam != null) ? exam.type : ExamType.UNDEFINED,
                    record.getConfigurationNodeId(),
                    record.getUserNames(),
                    record.getEncryptSecret(),
                    null,
                    config.getName(),
                    config.getDescription(),
                    (StringUtils.isNotBlank(status)) ? ConfigurationStatus.valueOf(status) : null);
        });
    }

    private Result<ExamConfigurationMap> checkMappingIntegrity(final ExamConfigurationMap data) {
        return Result.tryCatch(() -> {
            final ConfigurationNodeRecord config =
                    this.configurationNodeRecordMapper.selectByPrimaryKey(data.configurationNodeId);

            if (config == null) {
                throw new ResourceNotFoundException(
                        EntityType.CONFIGURATION_NODE,
                        String.valueOf(data.configurationNodeId));
            }

            if (config.getInstitutionId().longValue() != data.institutionId.longValue()) {
                throw new IllegalArgumentException("Institutional integrity constraint violation");
            }

            final ExamRecord exam = this.examRecordMapper.selectByPrimaryKey(data.examId);

            if (exam == null) {
                throw new ResourceNotFoundException(
                        EntityType.EXAM,
                        String.valueOf(data.configurationNodeId));
            }

            if (exam.getInstitutionId().longValue() != data.institutionId.longValue()) {
                throw new IllegalArgumentException("Institutional integrity constraint violation");
            }

            return data;
        });
    }

    private Result<Collection<EntityKey>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper.selectIdsByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.institutionId,
                        isEqualTo(Long.valueOf(institutionKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(id -> new EntityKey(id, EntityType.EXAM_CONFIGURATION_MAP))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityKey>> allIdsOfLmsSetup(final EntityKey lmsSetupKey) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper.selectIdsByExample()
                .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                .on(
                        ExamRecordDynamicSqlSupport.id,
                        equalTo(ExamConfigurationMapRecordDynamicSqlSupport.examId))

                .where(
                        ExamRecordDynamicSqlSupport.lmsSetupId,
                        isEqualTo(Long.valueOf(lmsSetupKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(id -> new EntityKey(id, EntityType.EXAM_CONFIGURATION_MAP))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityKey>> allIdsOfExam(final EntityKey examKey) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper.selectIdsByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.examId,
                        isEqualTo(Long.valueOf(examKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(id -> new EntityKey(id, EntityType.EXAM_CONFIGURATION_MAP))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityKey>> allIdsOfConfig(final EntityKey configKey) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper.selectIdsByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.configurationNodeId,
                        isEqualTo(Long.valueOf(configKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(id -> new EntityKey(id, EntityType.EXAM_CONFIGURATION_MAP))
                .collect(Collectors.toList()));
    }

    private String getEncryptionPassword(final ExamConfigurationMap examConfigurationMap) {
        if (examConfigurationMap.hasEncryptionSecret() &&
                !examConfigurationMap.encryptSecret.equals(examConfigurationMap.confirmEncryptSecret)) {
            throw new APIMessageException(ErrorMessage.PASSWORD_MISMATCH);
        }

        final CharSequence encrypted_encrypt_secret = examConfigurationMap.hasEncryptionSecret()
                ? this.clientCredentialService.encrypt(examConfigurationMap.encryptSecret)
                : null;
        return (encrypted_encrypt_secret != null) ? encrypted_encrypt_secret.toString() : null;
    }

}
