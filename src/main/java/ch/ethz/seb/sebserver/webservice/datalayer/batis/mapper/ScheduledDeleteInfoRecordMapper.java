package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ScheduledDeleteInfoRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ScheduledDeleteInfoRecord;
import java.util.List;
import jakarta.annotation.Generated;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.delete.DeleteDSL;
import org.mybatis.dynamic.sql.delete.MyBatis3DeleteModelAdapter;
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.render.RenderingStrategy;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectDSL;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.MyBatis3UpdateModelAdapter;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

@Mapper
public interface ScheduledDeleteInfoRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @Options(useGeneratedKeys=true,keyProperty="record.id")
    int insert(InsertStatementProvider<ScheduledDeleteInfoRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="scheduled_delete_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="state", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="exam_uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="deletion_info", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="error_info", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    ScheduledDeleteInfoRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="scheduled_delete_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="state", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="exam_uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="deletion_info", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="error_info", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<ScheduledDeleteInfoRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(scheduledDeleteInfoRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, scheduledDeleteInfoRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, scheduledDeleteInfoRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default int insert(ScheduledDeleteInfoRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(scheduledDeleteInfoRecord)
                .map(scheduledDeleteId).toProperty("scheduledDeleteId")
                .map(state).toProperty("state")
                .map(examUuid).toProperty("examUuid")
                .map(deletionInfo).toProperty("deletionInfo")
                .map(errorInfo).toProperty("errorInfo")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default int insertSelective(ScheduledDeleteInfoRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(scheduledDeleteInfoRecord)
                .map(scheduledDeleteId).toPropertyWhenPresent("scheduledDeleteId", record::getScheduledDeleteId)
                .map(state).toPropertyWhenPresent("state", record::getState)
                .map(examUuid).toPropertyWhenPresent("examUuid", record::getExamUuid)
                .map(deletionInfo).toPropertyWhenPresent("deletionInfo", record::getDeletionInfo)
                .map(errorInfo).toPropertyWhenPresent("errorInfo", record::getErrorInfo)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ScheduledDeleteInfoRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, scheduledDeleteId, state, examUuid, deletionInfo, errorInfo)
                .from(scheduledDeleteInfoRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ScheduledDeleteInfoRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, scheduledDeleteId, state, examUuid, deletionInfo, errorInfo)
                .from(scheduledDeleteInfoRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default ScheduledDeleteInfoRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, scheduledDeleteId, state, examUuid, deletionInfo, errorInfo)
                .from(scheduledDeleteInfoRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ScheduledDeleteInfoRecord record) {
        return UpdateDSL.updateWithMapper(this::update, scheduledDeleteInfoRecord)
                .set(scheduledDeleteId).equalTo(record::getScheduledDeleteId)
                .set(state).equalTo(record::getState)
                .set(examUuid).equalTo(record::getExamUuid)
                .set(deletionInfo).equalTo(record::getDeletionInfo)
                .set(errorInfo).equalTo(record::getErrorInfo);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ScheduledDeleteInfoRecord record) {
        return UpdateDSL.updateWithMapper(this::update, scheduledDeleteInfoRecord)
                .set(scheduledDeleteId).equalToWhenPresent(record::getScheduledDeleteId)
                .set(state).equalToWhenPresent(record::getState)
                .set(examUuid).equalToWhenPresent(record::getExamUuid)
                .set(deletionInfo).equalToWhenPresent(record::getDeletionInfo)
                .set(errorInfo).equalToWhenPresent(record::getErrorInfo);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default int updateByPrimaryKey(ScheduledDeleteInfoRecord record) {
        return UpdateDSL.updateWithMapper(this::update, scheduledDeleteInfoRecord)
                .set(scheduledDeleteId).equalTo(record::getScheduledDeleteId)
                .set(state).equalTo(record::getState)
                .set(examUuid).equalTo(record::getExamUuid)
                .set(deletionInfo).equalTo(record::getDeletionInfo)
                .set(errorInfo).equalTo(record::getErrorInfo)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    default int updateByPrimaryKeySelective(ScheduledDeleteInfoRecord record) {
        return UpdateDSL.updateWithMapper(this::update, scheduledDeleteInfoRecord)
                .set(scheduledDeleteId).equalToWhenPresent(record::getScheduledDeleteId)
                .set(state).equalToWhenPresent(record::getState)
                .set(examUuid).equalToWhenPresent(record::getExamUuid)
                .set(deletionInfo).equalToWhenPresent(record::getDeletionInfo)
                .set(errorInfo).equalToWhenPresent(record::getErrorInfo)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator",comments="Source Table: exam")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({@Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true)})
    List<Long> selectIds(SelectStatementProvider select);

    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<Long>>> selectIdsByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectIds, id)
                        .from(scheduledDeleteInfoRecord);
    }
}