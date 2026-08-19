/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordDynamicSqlSupport.configurationTemplateId;
import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordDynamicSqlSupport.examTemplateRecord;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCreationInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ViewRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ViewRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationNodeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOLoggingSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOUserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
public class ConfigurationNodeDAOImpl implements ConfigurationNodeDAO {

    private final ConfigurationRecordMapper configurationRecordMapper;
    private final ConfigurationNodeRecordMapper configurationNodeRecordMapper;
    private final ConfigurationValueRecordMapper configurationValueRecordMapper;
    private final ConfigurationDAOBatchService configurationDAOBatchService;
    private final ExamTemplateRecordMapper examTemplateRecordMapper;
    private final ViewRecordMapper viewRecordMapper;
    private final OrientationRecordMapper orientationRecordMapper;
    private final DAOUserService daoUserService;

    protected ConfigurationNodeDAOImpl(
            final ConfigurationRecordMapper configurationRecordMapper,
            final ConfigurationNodeRecordMapper configurationNodeRecordMapper,
            final ConfigurationValueRecordMapper configurationValueRecordMapper,
            final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper,
            final ConfigurationDAOBatchService ConfigurationDAOBatchService,
            final ExamTemplateRecordMapper examTemplateRecordMapper,
            final ViewRecordMapper viewRecordMapper,
            final OrientationRecordMapper orientationRecordMapper,
            final DAOUserService daoUserService) {

        this.configurationRecordMapper = configurationRecordMapper;
        this.configurationNodeRecordMapper = configurationNodeRecordMapper;
        this.configurationValueRecordMapper = configurationValueRecordMapper;
        this.configurationDAOBatchService = ConfigurationDAOBatchService;
        this.examTemplateRecordMapper = examTemplateRecordMapper;
        this.viewRecordMapper = viewRecordMapper;
        this.orientationRecordMapper = orientationRecordMapper;
        this.daoUserService = daoUserService;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION_NODE;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ConfigurationNode> byPK(final Long id) {
        return recordById(id)
                .flatMap(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ConfigurationNode>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.configurationNodeRecordMapper
                    .selectByExample()
                    .where(ConfigurationNodeRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
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
    public Result<Collection<ConfigurationNode>> allMatching(
            final FilterMap filterMap,
            final Predicate<ConfigurationNode> predicate) {

        return Result.tryCatch(() -> {
            final QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ConfigurationNodeRecord>>>.QueryExpressionWhereBuilder whereClause =
                    (filterMap.getBoolean(FilterMap.ATTR_ADD_INSITUTION_JOIN))
                            ? this.configurationNodeRecordMapper
                                    .selectByExample()
                                    .join(InstitutionRecordDynamicSqlSupport.institutionRecord)
                                    .on(InstitutionRecordDynamicSqlSupport.id,
                                            SqlBuilder.equalTo(ConfigurationNodeRecordDynamicSqlSupport.institutionId))
                                    .where(
                                            ConfigurationNodeRecordDynamicSqlSupport.institutionId,
                                            SqlBuilder.isEqualToWhenPresent(filterMap.getInstitutionId()))
                            : this.configurationNodeRecordMapper
                                    .selectByExample()
                                    .where(
                                            ConfigurationNodeRecordDynamicSqlSupport.institutionId,
                                            SqlBuilder.isEqualToWhenPresent(filterMap.getInstitutionId()));

            whereClause
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.name,
                            SqlBuilder.isLikeWhenPresent(filterMap.getName()))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.description,
                            SqlBuilder.isLikeWhenPresent(filterMap.getConfigNodeDesc()))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.type,
                            SqlBuilder.isEqualToWhenPresent(filterMap.getConfigNodeType()))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.templateId,
                            SqlBuilder.isEqualToWhenPresent(filterMap.getConfigNodeTemplateId()));

            final String status = filterMap.getConfigNodeStatus();
            if (StringUtils.isBlank(status)) {
                whereClause.and(
                        ConfigurationNodeRecordDynamicSqlSupport.status,
                        SqlBuilder.isNotEqualToWhenPresent(ConfigurationStatus.ARCHIVED.name()));
            } else {
                whereClause.and(
                        ConfigurationNodeRecordDynamicSqlSupport.status,
                        SqlBuilder.isEqualToWhenPresent(filterMap.getConfigNodeStatus()));
            }

            return whereClause
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
    @Transactional(readOnly = true)
    public Set<EntityDependency> getDependencies(final BulkAction bulkAction) {
        // only if included
        if (!bulkAction.includesDependencyType(EntityType.CONFIGURATION_NODE)) {
            return Collections.emptySet();
        }

        // define the select function in case of source type
        Function<EntityKey, Result<Collection<EntityDependency>>> selectionFunction =
                key -> Result.of(Collections.emptyList());

        if (bulkAction.sourceType == EntityType.INSTITUTION) {
            selectionFunction = this::allIdsOfInstitution;
        }

        // in case of user deletion with configuration dependency inclusion
        if (bulkAction.sourceType == EntityType.USER &&
                bulkAction.type == BulkActionType.HARD_DELETE) {
            selectionFunction = this::allIdsOfUser;
        }

        return getDependencies(bulkAction, selectionFunction);
    }

    @Override
    public Result<ConfigurationNode> createNew(final ConfigurationNode data) {
        return this.configurationDAOBatchService
                .createNewConfiguration(data, daoUserService.getCurrentUserUUIDOrAnonymousUser())
                .flatMap(this.configurationDAOBatchService::createInitialConfiguration)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ConfigurationNode> save(final ConfigurationNode data) {
        return checkUniqueName(data)
                .map(_d -> {

                    final ConfigurationNodeRecord newRecord = new ConfigurationNodeRecord(
                            data.id,
                            null,
                            null,
                            null,
                            data.name,
                            data.description,
                            null,
                            (data.status != null) ? data.status.name() : ConfigurationStatus.CONSTRUCTION.name(),
                            Utils.getMillisecondsNow(),
                            this.daoUserService.getCurrentUserUUID());

                    this.configurationNodeRecordMapper.updateByPrimaryKeySelective(newRecord);
                    return this.configurationNodeRecordMapper.selectByPrimaryKey(data.id);
                })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    public Result<ConfigurationNode> createCopy(
            final Long institutionId,
            final String newOwner,
            final ConfigCreationInfo copyInfo) {

        return this
                .checkCorrectCopyName(copyInfo)
                .flatMap(cInfo -> this.configurationDAOBatchService.createCopy(
                        institutionId,
                        newOwner,
                        cInfo,
                        daoUserService.getCurrentUserUUID())
                );
    }

    @Override
    @Transactional
    public Result<ConfigurationNode> updateConfigurationTemplate(
            final Long configTemplateId,
            final String name,
            final String description) {

        return Result.tryCatch(() -> {

            ConfigurationNodeRecord record = recordById(configTemplateId).getOrThrow();
            if (ConfigurationType.valueOf(record.getType()) != ConfigurationType.TEMPLATE) {
                throw new APIMessage.APIMessageException(
                        APIMessage.ErrorMessage.BAD_REQUEST.of(
                                "ConfigurationNode is not of expected type TEMPLATE"));
            }

            final ConfigurationNodeRecord newRecord = new ConfigurationNodeRecord(
                    record.getId(),
                    null,
                    null,
                    null,
                    name,
                    description,
                    null,
                    null,
                    Utils.getMillisecondsNow(),
                    this.daoUserService.getCurrentUserUUID());

            this.configurationNodeRecordMapper.updateByPrimaryKeySelective(newRecord);
            return this.configurationNodeRecordMapper.selectByPrimaryKey(newRecord.getId());

         })
                .flatMap(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ConfigurationNodeRecord>> getAllTemporary() {
        return Result.tryCatch(() -> this.configurationNodeRecordMapper
                .selectByExample()
                .where(ConfigurationNodeRecordDynamicSqlSupport.name, isLike(Utils.toSQLWildcard(TEMPORARY_TEMPLATE_PREFIX)))
                .build()
                .execute()
        );
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.<Collection<EntityKey>>tryCatch(() -> {

            final List<Long> ids = extractListOfPKs(all);

            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }

            // find all configurations for this configuration node
            final List<Long> configurationIds = this.configurationRecordMapper
                    .selectIdsByExample()
                    .where(ConfigurationRecordDynamicSqlSupport.configurationNodeId, isIn(ids))
                    .build()
                    .execute();

            if (!configurationIds.isEmpty()) {

                // delete all ConfigurationValue's that belongs to the Configuration's to delete
                this.configurationValueRecordMapper.deleteByExample()
                        .where(ConfigurationValueRecordDynamicSqlSupport.configurationId, isIn(configurationIds))
                        .build()
                        .execute();

                // delete all Configuration's
                this.configurationRecordMapper.deleteByExample()
                        .where(ConfigurationRecordDynamicSqlSupport.id, isIn(configurationIds))
                        .build()
                        .execute();
            }

            handleConfigTemplateDeletion(ids);

            // and finally delete the requested ConfigurationNode's
            this.configurationNodeRecordMapper.deleteByExample()
                    .where(ConfigurationNodeRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.CONFIGURATION_NODE))
                    .collect(Collectors.toList());
        }).onError(TransactionHandler::rollback);
    }

    private void handleConfigTemplateDeletion(final List<Long> configurationIds) {
        // get all config template node ids
        final List<Long> templatesIds = this.configurationNodeRecordMapper
                .selectIdsByExample()
                .where(ConfigurationNodeRecordDynamicSqlSupport.id, isIn(configurationIds))
                .and(ConfigurationNodeRecordDynamicSqlSupport.type, isEqualTo(ConfigurationType.TEMPLATE.name()))
                .build()
                .execute();

        if (templatesIds == null || templatesIds.isEmpty()) {
            return;
        }

        // delete all related views and orientations
        this.orientationRecordMapper.deleteByExample()
                .where(OrientationRecordDynamicSqlSupport.templateId, isIn(templatesIds))
                .build()
                .execute();
        this.viewRecordMapper.deleteByExample()
                .where(ViewRecordDynamicSqlSupport.templateId, isIn(templatesIds))
                .build()
                .execute();

        // update all config nodes that uses one of the templates
        this.configurationNodeRecordMapper.updateByExampleSelective(
                new ConfigurationNodeRecord(null, null, 0L, null, null, null, null, null,
                        Utils.getMillisecondsNow(),
                        this.daoUserService.getCurrentUserUUID()))
                .where(ConfigurationNodeRecordDynamicSqlSupport.templateId, isIn(configurationIds))
                .build()
                .execute();

        // update all examTemplates that uses one of the templates
        UpdateDSL.updateWithMapper(this.examTemplateRecordMapper::update, examTemplateRecord)
                .set(configurationTemplateId).equalToNull()
                .where(ExamTemplateRecordDynamicSqlSupport.configurationTemplateId, isIn(configurationIds))
                .build()
                .execute();
    }

    private Result<Collection<EntityDependency>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> this.configurationNodeRecordMapper.selectByExample()
                .where(
                        ConfigurationNodeRecordDynamicSqlSupport.institutionId,
                        isEqualTo(Long.valueOf(institutionKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        institutionKey,
                        new EntityKey(rec.getId(), EntityType.CONFIGURATION_NODE),
                        rec.getName(),
                        rec.getDescription()))
                .collect(Collectors.toList()));
    }

    private Result<Collection<EntityDependency>> allIdsOfUser(final EntityKey userKey) {
        return Result.tryCatch(() -> this.configurationNodeRecordMapper.selectByExample()
                .where(
                        ConfigurationNodeRecordDynamicSqlSupport.owner,
                        isEqualTo(userKey.modelId))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        userKey,
                        new EntityKey(rec.getId(), EntityType.CONFIGURATION_NODE),
                        rec.getName(),
                        rec.getDescription()))
                .collect(Collectors.toList()));
    }

    private Result<ConfigurationNodeRecord> recordById(final Long id) {
        return Result.tryCatch(() -> {
            final ConfigurationNodeRecord record = this.configurationNodeRecordMapper
                    .selectByPrimaryKey(id);
            if (record == null) {
                throw new ResourceNotFoundException(
                        EntityType.CONFIGURATION_NODE,
                        String.valueOf(id));
            }
            return record;
        });
    }

    private Result<ConfigurationNode> checkUniqueName(final ConfigurationNode data) {
        return Result.tryCatch(() -> {
            final Long count = this.configurationNodeRecordMapper.countByExample()
                    .where(
                            ConfigurationNodeRecordDynamicSqlSupport.name,
                            isEqualTo(data.name))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.type,
                            isEqualTo(data.type.name()))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.id,
                            isNotEqualTo(data.id))
                    .and(
                            ConfigurationNodeRecordDynamicSqlSupport.institutionId,
                            isEqualTo(data.institutionId))
                    .build()
                    .execute();

            if (count != null && count > 0) {
                throw new FieldValidationException("name", "configurationNode:name:exists");
            }

            return data;
        });
    }

    Result<ConfigurationNode> toDomainModel(final ConfigurationNodeRecord record) {
        return Result.tryCatch(() -> {
            final String lastUpdateUser = record.getLastUpdateUser();
            final String lastUpdateUserName = this.daoUserService.getUserNameForUUID(lastUpdateUser);

            return new ConfigurationNode(
                    record.getId(),
                    record.getInstitutionId(),
                    record.getTemplateId(),
                    record.getName(),
                    record.getDescription(),
                    ConfigurationType.valueOf(record.getType()),
                    record.getOwner(),
                    ConfigurationStatus.valueOf(record.getStatus()),
                    Utils.toDateTimeUTC(record.getLastUpdateTime()),
                    lastUpdateUser,
                    lastUpdateUserName);

        });
    }

    private Result<ConfigCreationInfo> checkCorrectCopyName(final ConfigCreationInfo copyInfo) {
        return Result.tryCatch(() -> {
            final Long count = this.configurationNodeRecordMapper.countByExample()
                    .where(
                            ConfigurationNodeRecordDynamicSqlSupport.name,
                            isEqualTo(copyInfo.name))
                    .build()
                    .execute();

            if (count > 0) {
                return new ConfigCreationInfo(
                        copyInfo.configurationNodeId,
                        copyInfo.name + " " + Utils.formatDate(DateTime.now(DateTimeZone.UTC)),
                        copyInfo.description,
                        copyInfo.withHistory,
                        copyInfo.configurationType
                );
            }

            return copyInfo;
        });
    }

}
