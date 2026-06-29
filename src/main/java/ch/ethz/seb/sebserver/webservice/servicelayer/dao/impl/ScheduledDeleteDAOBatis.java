package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;


import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDelete;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDeleteInfo;
import ch.ethz.seb.sebserver.gbl.util.Nullable;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ScheduledDeleteInfoRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ScheduledDeleteInfoRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ScheduledDeleteRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ScheduledDeleteRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ScheduledDeleteInfoRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ScheduledDeleteRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.NoResourceFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ScheduledDeleteDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.core.util.Constants;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

@Service
public class ScheduledDeleteDAOBatis implements ScheduledDeleteDAO {

    private static final Logger log = LoggerFactory.getLogger(ScheduledDeleteDAOBatis.class);

    private final ScheduledDeleteRecordMapper scheduledDeleteRecordMapper;
    private final ScheduledDeleteInfoRecordMapper scheduledDeleteInfoRecordMapper;
    private final JSONMapper jsonMapper;


    public ScheduledDeleteDAOBatis(
            final ScheduledDeleteRecordMapper scheduledDeleteRecordMapper,
            final ScheduledDeleteInfoRecordMapper scheduledDeleteInfoRecordMapper,
            final JSONMapper jsonMapper) {

        this.scheduledDeleteRecordMapper = scheduledDeleteRecordMapper;
        this.scheduledDeleteInfoRecordMapper = scheduledDeleteInfoRecordMapper;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.SCHEDULED_DELETE;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ScheduledDelete> byPK(final Long id) {
        return recordByPK(id)
                .map(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ScheduledDelete>> allOf(final Set<Long> pks) {
        return Result.tryCatch(() -> {

            if (pks == null || pks.isEmpty()) {
                return Collections.emptyList();
            }

            return this.scheduledDeleteRecordMapper
                    .selectByExample()
                    .where(ScheduledDeleteRecordDynamicSqlSupport.id, isIn(new ArrayList<>(pks)))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<ScheduledDelete>> allMatching(
            final FilterMap filterMap,
            final Predicate<ScheduledDelete> predicate) {

        return Result.tryCatch(() -> {
            QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ScheduledDeleteRecord>>>.QueryExpressionWhereBuilder query = this.scheduledDeleteRecordMapper
                    .selectByExample()
                    .where(
                            ScheduledDeleteRecordDynamicSqlSupport.institutionId,
                            SqlBuilder.isEqualToWhenPresent(filterMap.getInstitutionId()));

            String state = filterMap.getString(Domain.SCHEDULED_DELETE.ATTR_STATE);
            if (StringUtils.isNotBlank(state)) {
                List<String> states = new ArrayList<>(Arrays.stream(StringUtils.split(state, Constants.COMMA)).toList());
                if (states.contains(ScheduledDelete.State.RUNNING.name())) {
                    states.add(ScheduledDelete.State.SPS_RUNNING.name());
                }

                query = query.and(
                        ScheduledDeleteRecordDynamicSqlSupport.state,
                        SqlBuilder.isIn(states));
            }

            return query
                    .and(
                            ScheduledDeleteRecordDynamicSqlSupport.deleteDueTime,
                            SqlBuilder.isLessThanOrEqualToWhenPresent(filterMap.getLong(Domain.SCHEDULED_DELETE.ATTR_DELETE_DUE_TIME)))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .collect(Collectors.toList());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Nullable<ScheduledDelete>> getPendingScheduledDelete() {
        return Result.tryCatch(() -> {
            List<ScheduledDelete> allPending = this.scheduledDeleteRecordMapper
                    .selectByExample()
                    .where(
                            ScheduledDeleteRecordDynamicSqlSupport.state,
                            isIn(
                                    ScheduledDelete.State.PENDING.name(),
                                    ScheduledDelete.State.SPS_RUNNING.name()))
                    .build()
                    .execute()
                    .stream()
                    .map(this::toDomainModel)
                    .toList();

            // NOTE: there should only be one pending schedule or none
            if (allPending.isEmpty()) {
                return Nullable.ofNull();
            }

            if (allPending.size() > 1) {
                log.warn("There are more pending ScheduledDelete as expected maximum of one: {}", + allPending.size());
            }

            return new Nullable<>(allPending.getFirst());
        });
    }

    @Override
    @Transactional
    public Result<ScheduledDelete> createNew(ScheduledDelete data) {
        return Result.tryCatch(() -> {

            // first create the ScheduledDelete entry
            ScheduledDeleteRecord scheduledDeleteRecord = new ScheduledDeleteRecord(
                    null,
                    data.spsId(),
                    data.state().toString(),
                    data.deleteDueTime(),
                    data.scheduleTime(),
                    null,
                    null,
                    data.ownerUUID(),
                    data.institutionId()
            );
            this.scheduledDeleteRecordMapper.insert(scheduledDeleteRecord);

            // add collected infos
            Collection<ScheduledDeleteInfo> infos = data.info();
            if (infos != null) {
                infos.forEach( info -> {
                        this.scheduledDeleteInfoRecordMapper.insert(new ScheduledDeleteInfoRecord(
                                null,
                                scheduledDeleteRecord.getId(),
                                info.state() != null ? info.state().name() : ScheduledDeleteInfo.State.PENDING.name(),
                                info.examUUID(),
                                putDeletionInfo(info),
                                info.errorInfo()
                        ));
                });
            }

            return scheduledDeleteRecordMapper.selectByPrimaryKey(scheduledDeleteRecord.getId());

        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ScheduledDelete> addInfo(
            final Long scheduledDeleteId,
            final Collection<ScheduledDeleteInfo> info) {

        return Result.tryCatch(() -> {
            if (info != null && !info.isEmpty()) {
                info.forEach(infoData ->  {
                    this.scheduledDeleteInfoRecordMapper.insert(new ScheduledDeleteInfoRecord(
                            null,
                            scheduledDeleteId,
                            ScheduledDeleteInfo.State.PENDING.toString(),
                            putDeletionInfo(infoData),
                            infoData.examUUID(),
                            null
                    ));
                });
            }

            return scheduledDeleteRecordMapper.selectByPrimaryKey(scheduledDeleteId);
        })
                .map(this::toDomainModel)
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<ScheduledDelete> save(ScheduledDelete data) {
        return Result.ofRuntimeError("Unsupported Operation Save ScheduledDelete");
    }

    @Override
    @Transactional
    public boolean startProcessing(final Long deleteId) {
        try {

            final long now = Utils.getMillisecondsNow();
            UpdateDSL.updateWithMapper(this.scheduledDeleteRecordMapper::update, ScheduledDeleteRecordDynamicSqlSupport.scheduledDeleteRecord)
                    .set(ScheduledDeleteRecordDynamicSqlSupport.startTime).equalTo(now)
                    .set(ScheduledDeleteRecordDynamicSqlSupport.state).equalTo(ScheduledDelete.State.RUNNING.name())
                    .where(ScheduledDeleteRecordDynamicSqlSupport.id, isEqualTo(deleteId))
                    .build()
                    .execute();

            return true;
        } catch (Exception e) {
            log.error("Failed to mark scheduled delete as running: {} cause: {}", deleteId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean endProcessing(final Long deleteId) {
        try {

            final long now = Utils.getMillisecondsNow();
            UpdateDSL.updateWithMapper(this.scheduledDeleteRecordMapper::update, ScheduledDeleteRecordDynamicSqlSupport.scheduledDeleteRecord)
                    .set(ScheduledDeleteRecordDynamicSqlSupport.endTime).equalTo(now)
                    .set(ScheduledDeleteRecordDynamicSqlSupport.state).equalTo(ScheduledDelete.State.FINISHED.name())
                    .where(ScheduledDeleteRecordDynamicSqlSupport.id, isEqualTo(deleteId))
                    .build()
                    .execute();

            return true;
        } catch (Exception e) {
            log.error("Failed to mark scheduled delete as finished: {} cause: {}", deleteId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean markAs(Long deleteId, ScheduledDelete.State state) {
        try {

            final long now = Utils.getMillisecondsNow();
            UpdateDSL.updateWithMapper(this.scheduledDeleteRecordMapper::update, ScheduledDeleteRecordDynamicSqlSupport.scheduledDeleteRecord)
                    .set(ScheduledDeleteRecordDynamicSqlSupport.endTime).equalTo(now)
                    .set(ScheduledDeleteRecordDynamicSqlSupport.state).equalTo(state.name())
                    .where(ScheduledDeleteRecordDynamicSqlSupport.id, isEqualTo(deleteId))
                    .build()
                    .execute();

            return true;
        } catch (Exception e) {
            log.error("Failed to mark scheduled delete exam as {} cause: {}",state, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean startSingleDeletion(final Long infoId) {
        try {

            UpdateDSL.updateWithMapper(this.scheduledDeleteInfoRecordMapper::update, ScheduledDeleteInfoRecordDynamicSqlSupport.scheduledDeleteInfoRecord)
                    .set(ScheduledDeleteInfoRecordDynamicSqlSupport.state).equalTo(ScheduledDeleteInfo.State.RUNNING.name())
                    .where(ScheduledDeleteInfoRecordDynamicSqlSupport.id, isEqualTo(infoId))
                    .build()
                    .execute();

            return true;
        } catch (Exception e) {
            log.error("Failed to mark scheduled delete exam as running: {} cause: {}", infoId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean endSingleDeletion(final Long infoId, final String errorInfo) {
        try {

            final String error = Utils.truncateText(errorInfo, 4000);

            UpdateDSL.updateWithMapper(this.scheduledDeleteInfoRecordMapper::update, ScheduledDeleteInfoRecordDynamicSqlSupport.scheduledDeleteInfoRecord)
                    .set(ScheduledDeleteInfoRecordDynamicSqlSupport.state).equalTo(
                            StringUtils.isBlank(error)
                                ? ScheduledDeleteInfo.State.DELETED.name()
                                : ScheduledDeleteInfo.State.ERROR.name())
                    .set(ScheduledDeleteInfoRecordDynamicSqlSupport.errorInfo).equalToWhenPresent(error)
                    .where(ScheduledDeleteInfoRecordDynamicSqlSupport.id, isEqualTo(infoId))
                    .build()
                    .execute();

            return true;
        } catch (Exception e) {
            log.error("Failed to mark scheduled delete exam as finished: {} cause: {}", infoId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(Set<EntityKey> all) {
        return Result.tryCatch(() -> extractListOfPKs(all))
                .map(this::delete);
    }



    private Result<ScheduledDeleteRecord> recordByPK(final Long pk) {
        return Result.tryCatch(() -> {

            final ScheduledDeleteRecord selectByPrimaryKey = this.scheduledDeleteRecordMapper.selectByPrimaryKey(pk);

            if (selectByPrimaryKey == null) {
                throw new NoResourceFoundException(EntityType.SCHEDULED_DELETE, String.valueOf(pk));
            }

            return selectByPrimaryKey;
        });
    }

    private Result<Collection<ScheduledDeleteInfoRecord>> infoByPK(final Long pk) {
        return Result.tryCatch(() -> this.scheduledDeleteInfoRecordMapper
                .selectByExample()
                .where(ScheduledDeleteInfoRecordDynamicSqlSupport.scheduledDeleteId, SqlBuilder.isEqualTo(pk))
                .build()
                .execute());
    }

    private ScheduledDelete toDomainModel(final ScheduledDeleteRecord rec) {
        Collection<ScheduledDeleteInfo> info = infoByPK(rec.getId())
                .getOrThrow()
                .stream()
                .map(info_rec ->
                        new ScheduledDeleteInfo(
                                info_rec.getId(),
                                info_rec.getScheduledDeleteId(),
                                ScheduledDeleteInfo.State.valueOf(info_rec.getState()),
                                info_rec.getExamUuid(),
                                getDeletionInfo(info_rec),
                                info_rec.getErrorInfo()
                        ))
                .toList();

        return new ScheduledDelete(
                rec.getId(),
                rec.getSpsId(),
                ScheduledDelete.State.valueOf(rec.getState()),
                rec.getDeleteDueTime(),
                rec.getScheduleTime(),
                rec.getStartTime(),
                rec.getEndTime(),
                rec.getOwner(),
                rec.getInstitutionId(),
                info
        );
    }



    private Collection<EntityKey> delete(final List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // first delete all infos
        this.scheduledDeleteInfoRecordMapper
                .deleteByExample()
                .where(ScheduledDeleteInfoRecordDynamicSqlSupport.scheduledDeleteId, isIn(ids))
                .build()
                .execute();

        // then delete the nodes
        this.scheduledDeleteRecordMapper
                .deleteByExample()
                .where(ScheduledDeleteRecordDynamicSqlSupport.id, isIn(ids))
                .build()
                .execute();

        return ids.stream()
                .map(pk -> new EntityKey(pk, EntityType.SCHEDULED_DELETE))
                .collect(Collectors.toList());
    }

    private Map<String, String>  getDeletionInfo(final ScheduledDeleteInfoRecord rec) {
        try {
            String delInfo = rec.getDeletionInfo();
            if (StringUtils.isBlank(delInfo)) {
                return Collections.emptyMap();
            }
            return  jsonMapper.readValue(delInfo, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("Failed to parse deletion info from: {} cause: {}", rec.getDeletionInfo(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String putDeletionInfo(final ScheduledDeleteInfo info) {
        try {
            Map<String, String> delInfo = info.deletionInfo();
            if (delInfo == null || delInfo.isEmpty()) {
                return null;
            }

            return jsonMapper.writeValueAsString(delInfo);
        } catch (Exception e) {
            log.error("Failed to serialize deletion info for: {} cause: {}", info.deletionInfo(), e.getMessage());
            return null;
        }
    }
}
