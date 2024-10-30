package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ScreenProctoringGroopRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ScreenProctoringGroopRecord;
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
public interface ScreenProctoringGroopRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ScreenProctoringGroopRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="size", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="data", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="collecting_strategy", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="seb_group_id", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    ScreenProctoringGroopRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="size", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="data", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="collecting_strategy", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="seb_group_id", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    List<ScreenProctoringGroopRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(screenProctoringGroopRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, screenProctoringGroopRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, screenProctoringGroopRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default int insert(ScreenProctoringGroopRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(screenProctoringGroopRecord)
                .map(examId).toProperty("examId")
                .map(uuid).toProperty("uuid")
                .map(name).toProperty("name")
                .map(size).toProperty("size")
                .map(data).toProperty("data")
                .map(collectingStrategy).toProperty("collectingStrategy")
                .map(sebGroupId).toProperty("sebGroupId")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default int insertSelective(ScreenProctoringGroopRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(screenProctoringGroopRecord)
                .map(examId).toPropertyWhenPresent("examId", record::getExamId)
                .map(uuid).toPropertyWhenPresent("uuid", record::getUuid)
                .map(name).toPropertyWhenPresent("name", record::getName)
                .map(size).toPropertyWhenPresent("size", record::getSize)
                .map(data).toPropertyWhenPresent("data", record::getData)
                .map(collectingStrategy).toPropertyWhenPresent("collectingStrategy", record::getCollectingStrategy)
                .map(sebGroupId).toPropertyWhenPresent("sebGroupId", record::getSebGroupId)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ScreenProctoringGroopRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, examId, uuid, name, size, data, collectingStrategy, sebGroupId)
                .from(screenProctoringGroopRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ScreenProctoringGroopRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, examId, uuid, name, size, data, collectingStrategy, sebGroupId)
                .from(screenProctoringGroopRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default ScreenProctoringGroopRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, examId, uuid, name, size, data, collectingStrategy, sebGroupId)
                .from(screenProctoringGroopRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ScreenProctoringGroopRecord record) {
        return UpdateDSL.updateWithMapper(this::update, screenProctoringGroopRecord)
                .set(examId).equalTo(record::getExamId)
                .set(uuid).equalTo(record::getUuid)
                .set(name).equalTo(record::getName)
                .set(size).equalTo(record::getSize)
                .set(data).equalTo(record::getData)
                .set(collectingStrategy).equalTo(record::getCollectingStrategy)
                .set(sebGroupId).equalTo(record::getSebGroupId);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ScreenProctoringGroopRecord record) {
        return UpdateDSL.updateWithMapper(this::update, screenProctoringGroopRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(uuid).equalToWhenPresent(record::getUuid)
                .set(name).equalToWhenPresent(record::getName)
                .set(size).equalToWhenPresent(record::getSize)
                .set(data).equalToWhenPresent(record::getData)
                .set(collectingStrategy).equalToWhenPresent(record::getCollectingStrategy)
                .set(sebGroupId).equalToWhenPresent(record::getSebGroupId);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default int updateByPrimaryKey(ScreenProctoringGroopRecord record) {
        return UpdateDSL.updateWithMapper(this::update, screenProctoringGroopRecord)
                .set(examId).equalTo(record::getExamId)
                .set(uuid).equalTo(record::getUuid)
                .set(name).equalTo(record::getName)
                .set(size).equalTo(record::getSize)
                .set(data).equalTo(record::getData)
                .set(collectingStrategy).equalTo(record::getCollectingStrategy)
                .set(sebGroupId).equalTo(record::getSebGroupId)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.627+01:00", comments="Source Table: screen_proctoring_group")
    default int updateByPrimaryKeySelective(ScreenProctoringGroopRecord record) {
        return UpdateDSL.updateWithMapper(this::update, screenProctoringGroopRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(uuid).equalToWhenPresent(record::getUuid)
                .set(name).equalToWhenPresent(record::getName)
                .set(size).equalToWhenPresent(record::getSize)
                .set(data).equalToWhenPresent(record::getData)
                .set(collectingStrategy).equalToWhenPresent(record::getCollectingStrategy)
                .set(sebGroupId).equalToWhenPresent(record::getSebGroupId)
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
                        .from(screenProctoringGroopRecord);
    }
}