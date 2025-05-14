/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamConfigurationMapRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
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
        return Result.tryCatch(() -> {

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.examConfigurationMapRecordMapper.selectByExample()
                    .where(ExamConfigurationMapRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
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
                        ExamConfigurationMapRecordDynamicSqlSupport.clientGroupId,
                        SqlBuilder.isNull())
                .build()
                .execute()
                .stream()
                .map(ExamConfigurationMapRecord::getConfigurationNodeId)
                .collect(Utils.toSingleton()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Long> getConfigurationNodeIdForClientGroup(final Long examId, final Long clientGroupId) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper
                .selectByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualTo(examId))
                .and(
                        ExamConfigurationMapRecordDynamicSqlSupport.clientGroupId,
                        SqlBuilder.isEqualTo(clientGroupId))
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
                            getEncryptionPassword(data),
                            data.clientGroupId);

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

            final String p =  (StringUtils.isNotBlank(data.encryptSecret))
                    ? getEncryptionPassword(data)
                    : null;

            UpdateDSL.updateWithMapper(examConfigurationMapRecordMapper::update, examConfigurationMapRecord)
                    .set(encryptSecret).equalTo(p )
                    .set(clientGroupId).equalToWhenPresent(data.clientGroupId)
                    .where(id, isEqualTo(data.id))
                    .build()
                    .execute();

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
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            // get all involved configurations
            final List<Long> configIds = this.examConfigurationMapRecordMapper.selectByExample()
                    .where(ExamConfigurationMapRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute()
                    .stream()
                    .map(ExamConfigurationMapRecord::getConfigurationNodeId)
                    .collect(Collectors.toList());

            this.examConfigurationMapRecordMapper.deleteByExample()
                    .where(ExamConfigurationMapRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            updateConfigurationStates(configIds)
                    .onError(error -> log.error("Unexpected error while update exam configuration state: ", error));

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.EXAM_CONFIGURATION_MAP))
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityDependency> getDependencies(final BulkAction bulkAction) {
        // only deletion here
        if (bulkAction.type == BulkActionType.ACTIVATE || bulkAction.type == BulkActionType.DEACTIVATE) {
            return Collections.emptySet();
        }
        // only if included
        if (!bulkAction.includesDependencyType(EntityType.EXAM_CONFIGURATION_MAP)) {
            return Collections.emptySet();
        }

        // define the select function in case of source type
        final Function<EntityKey, Result<Collection<EntityDependency>>> selectionFunction = switch (bulkAction.sourceType) {
            case INSTITUTION -> this::allIdsOfInstitution;
            case USER -> this::allIdsOfUser;
            case LMS_SETUP -> this::allIdsOfLmsSetup;
            case EXAM -> this::allIdsOfExam;
            case CONFIGURATION_NODE -> this::allIdsOfConfig;
            default -> key -> Result.of(Collections.emptyList()); //empty select function
        };

        return getDependencies(bulkAction, selectionFunction);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<Long>> getExamIdsForConfigNodeId(final Long configurationNodeId) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper
                .selectByExample()
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

    @Override
    @Transactional(readOnly = true)
    public Result<Boolean> checkNoActiveExamReferences(final Long configurationNodeId) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper.selectByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.configurationNodeId,
                        isEqualTo(configurationNodeId))
                .build()
                .execute()
                .stream()
                .noneMatch(rec -> isExamActive(rec.getExamId())));
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> deleteAllForExam(final Long examId) {
        return Result.<Collection<EntityKey>> tryCatch(() -> {

            final List<Long> ids = this.examConfigurationMapRecordMapper.selectIdsByExample()
                    .where(ExamConfigurationMapRecordDynamicSqlSupport.examId, isEqualTo(examId))
                    .build()
                    .execute();

            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            // get all involved configurations
            final List<Long> configIds = this.examConfigurationMapRecordMapper.selectByExample()
                    .where(ExamConfigurationMapRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute()
                    .stream()
                    .map(ExamConfigurationMapRecord::getConfigurationNodeId)
                    .collect(Collectors.toList());

            this.examConfigurationMapRecordMapper.deleteByExample()
                    .where(ExamConfigurationMapRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            updateConfigurationStates(configIds)
                    .onError(error -> log.error("Unexpected error while update exam configuration state: ", error));

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.EXAM_CONFIGURATION_MAP))
                    .collect(Collectors.toList());
        })
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ExamConfigurationMap>> allOfExam(final Long examId) {
        return Result.tryCatch(() -> this.examConfigurationMapRecordMapper
                .selectByExample()
                .where(
                        ExamConfigurationMapRecordDynamicSqlSupport.examId,
                        SqlBuilder.isEqualTo(examId))
                .build()
                .execute())
                .flatMap(this::toDomainModel);
    }

    private boolean isExamActive(final Long examId) {
        try {
            return this.examRecordMapper.countByExample()
                    .where(ExamRecordDynamicSqlSupport.id, isEqualTo(examId))
                    .and(ExamRecordDynamicSqlSupport.status, isIn(Exam.ACTIVE_STATE_NAMES))
                    .build()
                    .execute() >= 1;
        } catch (final Exception e) {
            log.warn("Failed to check exam status for exam: {}", examId, e);
            return false;
        }
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

    private Result<Collection<ExamConfigurationMap>> toDomainModel(
            final Collection<ExamConfigurationMapRecord> records) {

        return Result.tryCatch(() -> records
                .stream()
                .map(model -> this.toDomainModel(model).getOrThrow())
                .collect(Collectors.toList()));
    }

    private Result<ExamConfigurationMap> toDomainModel(final ExamConfigurationMapRecord record) {
        return Result.tryCatch(() -> {

            final ConfigurationNodeRecord config = this.configurationNodeRecordMapper
                    .selectByPrimaryKey(record.getConfigurationNodeId());
            final String status = config.getStatus();

            final Exam exam = this.examDAO
                    .byPK(record.getExamId())
                    .getOr(null);

            return new ExamConfigurationMap(
                    record.getId(),
                    record.getInstitutionId(),
                    record.getExamId(),
                    (exam != null) ? exam.name : null,
                    (exam != null) ? exam.getDescription() : null,
                    (exam != null) ? exam.startTime : null,
                    (exam != null) ? exam.type : ExamType.UNDEFINED,
                    (exam != null) ? exam.status : null,
                    record.getConfigurationNodeId(),
                    record.getClientGroupId(),
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

    private Result<Collection<EntityDependency>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> toDependencies(
                this.examConfigurationMapRecordMapper.selectByExample()
                        .where(
                                ExamConfigurationMapRecordDynamicSqlSupport.institutionId,
                                isEqualTo(Long.valueOf(institutionKey.modelId)))
                        .build()
                        .execute(),
                institutionKey));
    }

    private Result<Collection<EntityDependency>> allIdsOfUser(final EntityKey userKey) {
        return Result.tryCatch(() -> {
            final List<Long> examsIds = this.examRecordMapper.selectIdsByExample()
                    .where(
                            ExamRecordDynamicSqlSupport.owner,
                            isEqualTo(userKey.modelId))
                    .build()
                    .execute();

            if (examsIds.isEmpty()) {
                return Collections.emptyList();
            }

            return toDependencies(
                    this.examConfigurationMapRecordMapper.selectByExample()
                            .where(
                                    ExamConfigurationMapRecordDynamicSqlSupport.examId,
                                    isIn(examsIds))
                            .build()
                            .execute(),
                    userKey);
        });
    }

    private Result<Collection<EntityDependency>> allIdsOfLmsSetup(final EntityKey lmsSetupKey) {
        return Result.tryCatch(() -> toDependencies(
                this.examConfigurationMapRecordMapper.selectByExample()
                        .leftJoin(ExamRecordDynamicSqlSupport.examRecord)
                        .on(
                                ExamRecordDynamicSqlSupport.id,
                                equalTo(ExamConfigurationMapRecordDynamicSqlSupport.examId))

                        .where(
                                ExamRecordDynamicSqlSupport.lmsSetupId,
                                isEqualTo(Long.valueOf(lmsSetupKey.modelId)))
                        .build()
                        .execute(),
                lmsSetupKey));
    }

    private Result<Collection<EntityDependency>> allIdsOfExam(final EntityKey examKey) {
        return Result.tryCatch(() -> toDependencies(
                this.examConfigurationMapRecordMapper.selectByExample()
                        .where(
                                ExamConfigurationMapRecordDynamicSqlSupport.examId,
                                isEqualTo(Long.valueOf(examKey.modelId)))
                        .build()
                        .execute(),
                examKey));
    }

    private Result<Collection<EntityDependency>> allIdsOfConfig(final EntityKey configKey) {
        return Result.tryCatch(() -> toDependencies(
                this.examConfigurationMapRecordMapper.selectByExample()
                        .where(
                                ExamConfigurationMapRecordDynamicSqlSupport.configurationNodeId,
                                isEqualTo(Long.valueOf(configKey.modelId)))
                        .build()
                        .execute(),
                configKey));
    }

    private Collection<EntityDependency> toDependencies(
            final List<ExamConfigurationMapRecord> records,
            final EntityKey parent) {

        return this.toDomainModel(records)
                .map(models -> models
                        .stream()
                        .map(model -> getDependency(model, parent))
                        .collect(Collectors.toList()))
                .getOrThrow();
    }

    private EntityDependency getDependency(final ExamConfigurationMap model, final EntityKey parent) {
        final String description = (Utils.valueOrEmptyNote(model.getExamDescription()) + " / "
                + Utils.valueOrEmptyNote(model.getConfigDescription())).replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*",
                        " ");
        return new EntityDependency(
                parent,
                new EntityKey(model.getId(), EntityType.EXAM_CONFIGURATION_MAP),
                Utils.valueOrEmptyNote(model.getExamName()) + " / " + Utils.valueOrEmptyNote(model.getConfigName()),
                description);
    }

    private String getEncryptionPassword(final ExamConfigurationMap examConfigurationMap) {
        if (examConfigurationMap.hasEncryptionSecret() &&
                !examConfigurationMap.encryptSecret.equals(examConfigurationMap.confirmEncryptSecret)) {
            throw new APIMessageException(ErrorMessage.PASSWORD_MISMATCH);
        }

        final CharSequence encrypted_encrypt_secret = examConfigurationMap.hasEncryptionSecret()
                ? this.clientCredentialService.encrypt(examConfigurationMap.encryptSecret)
                        .getOrThrow()
                : null;
        return (encrypted_encrypt_secret != null) ? encrypted_encrypt_secret.toString() : null;
    }

    private Result<Set<Long>> updateConfigurationStates(final Collection<Long> configIds) {
        return Result.tryCatch(() -> {
            return configIds
                    .stream()
                    .map(id -> {
                        final long assignments = this.examConfigurationMapRecordMapper.countByExample()
                                .where(ExamConfigurationMapRecordDynamicSqlSupport.configurationNodeId, isEqualTo(id))
                                .build()
                                .execute();
                        if (assignments <= 0) {
                            final ConfigurationNodeRecord newRecord = new ConfigurationNodeRecord(
                                    id,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    ConfigurationStatus.READY_TO_USE.name(),
                                    null,
                                    null);
                            this.configurationNodeRecordMapper.updateByPrimaryKeySelective(newRecord);
                            return id;
                        } else {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        });
    }

}
