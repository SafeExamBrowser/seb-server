package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.BatchActionRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.BatchActionRecord;
import java.util.List;
import javax.annotation.Generated;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectKey;
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
public interface BatchActionRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.219+01:00", comments="Source Table: batch_action")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.219+01:00", comments="Source Table: batch_action")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.219+01:00", comments="Source Table: batch_action")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<BatchActionRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.219+01:00", comments="Source Table: batch_action")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="action_type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="source_ids", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="successful", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="last_update", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="processor_id", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    BatchActionRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.219+01:00", comments="Source Table: batch_action")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="action_type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="source_ids", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="successful", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="last_update", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="processor_id", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<BatchActionRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(batchActionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, batchActionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, batchActionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default int insert(BatchActionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(batchActionRecord)
                .map(institutionId).toProperty("institutionId")
                .map(actionType).toProperty("actionType")
                .map(sourceIds).toProperty("sourceIds")
                .map(successful).toProperty("successful")
                .map(lastUpdate).toProperty("lastUpdate")
                .map(processorId).toProperty("processorId")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default int insertSelective(BatchActionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(batchActionRecord)
                .map(institutionId).toPropertyWhenPresent("institutionId", record::getInstitutionId)
                .map(actionType).toPropertyWhenPresent("actionType", record::getActionType)
                .map(sourceIds).toPropertyWhenPresent("sourceIds", record::getSourceIds)
                .map(successful).toPropertyWhenPresent("successful", record::getSuccessful)
                .map(lastUpdate).toPropertyWhenPresent("lastUpdate", record::getLastUpdate)
                .map(processorId).toPropertyWhenPresent("processorId", record::getProcessorId)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<BatchActionRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, institutionId, actionType, sourceIds, successful, lastUpdate, processorId)
                .from(batchActionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<BatchActionRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, institutionId, actionType, sourceIds, successful, lastUpdate, processorId)
                .from(batchActionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default BatchActionRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, institutionId, actionType, sourceIds, successful, lastUpdate, processorId)
                .from(batchActionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(BatchActionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, batchActionRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(actionType).equalTo(record::getActionType)
                .set(sourceIds).equalTo(record::getSourceIds)
                .set(successful).equalTo(record::getSuccessful)
                .set(lastUpdate).equalTo(record::getLastUpdate)
                .set(processorId).equalTo(record::getProcessorId);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(BatchActionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, batchActionRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(actionType).equalToWhenPresent(record::getActionType)
                .set(sourceIds).equalToWhenPresent(record::getSourceIds)
                .set(successful).equalToWhenPresent(record::getSuccessful)
                .set(lastUpdate).equalToWhenPresent(record::getLastUpdate)
                .set(processorId).equalToWhenPresent(record::getProcessorId);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default int updateByPrimaryKey(BatchActionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, batchActionRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(actionType).equalTo(record::getActionType)
                .set(sourceIds).equalTo(record::getSourceIds)
                .set(successful).equalTo(record::getSuccessful)
                .set(lastUpdate).equalTo(record::getLastUpdate)
                .set(processorId).equalTo(record::getProcessorId)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.220+01:00", comments="Source Table: batch_action")
    default int updateByPrimaryKeySelective(BatchActionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, batchActionRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(actionType).equalToWhenPresent(record::getActionType)
                .set(sourceIds).equalToWhenPresent(record::getSourceIds)
                .set(successful).equalToWhenPresent(record::getSuccessful)
                .set(lastUpdate).equalToWhenPresent(record::getLastUpdate)
                .set(processorId).equalToWhenPresent(record::getProcessorId)
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
                        .from(batchActionRecord);
    }
}