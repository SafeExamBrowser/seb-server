package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.IndicatorRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.IndicatorRecord;
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
public interface IndicatorRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.027+02:00", comments="Source Table: indicator")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.027+02:00", comments="Source Table: indicator")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.027+02:00", comments="Source Table: indicator")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<IndicatorRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.027+02:00", comments="Source Table: indicator")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="color", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="icon", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="tags", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    IndicatorRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.028+02:00", comments="Source Table: indicator")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="color", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="icon", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="tags", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<IndicatorRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.028+02:00", comments="Source Table: indicator")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.028+02:00", comments="Source Table: indicator")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(indicatorRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.028+02:00", comments="Source Table: indicator")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, indicatorRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.028+02:00", comments="Source Table: indicator")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, indicatorRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.028+02:00", comments="Source Table: indicator")
    default int insert(IndicatorRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(indicatorRecord)
                .map(examId).toProperty("examId")
                .map(type).toProperty("type")
                .map(name).toProperty("name")
                .map(color).toProperty("color")
                .map(icon).toProperty("icon")
                .map(tags).toProperty("tags")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.028+02:00", comments="Source Table: indicator")
    default int insertSelective(IndicatorRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(indicatorRecord)
                .map(examId).toPropertyWhenPresent("examId", record::getExamId)
                .map(type).toPropertyWhenPresent("type", record::getType)
                .map(name).toPropertyWhenPresent("name", record::getName)
                .map(color).toPropertyWhenPresent("color", record::getColor)
                .map(icon).toPropertyWhenPresent("icon", record::getIcon)
                .map(tags).toPropertyWhenPresent("tags", record::getTags)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.028+02:00", comments="Source Table: indicator")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<IndicatorRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, examId, type, name, color, icon, tags)
                .from(indicatorRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.029+02:00", comments="Source Table: indicator")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<IndicatorRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, examId, type, name, color, icon, tags)
                .from(indicatorRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.029+02:00", comments="Source Table: indicator")
    default IndicatorRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, examId, type, name, color, icon, tags)
                .from(indicatorRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.029+02:00", comments="Source Table: indicator")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(IndicatorRecord record) {
        return UpdateDSL.updateWithMapper(this::update, indicatorRecord)
                .set(examId).equalTo(record::getExamId)
                .set(type).equalTo(record::getType)
                .set(name).equalTo(record::getName)
                .set(color).equalTo(record::getColor)
                .set(icon).equalTo(record::getIcon)
                .set(tags).equalTo(record::getTags);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.029+02:00", comments="Source Table: indicator")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(IndicatorRecord record) {
        return UpdateDSL.updateWithMapper(this::update, indicatorRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(type).equalToWhenPresent(record::getType)
                .set(name).equalToWhenPresent(record::getName)
                .set(color).equalToWhenPresent(record::getColor)
                .set(icon).equalToWhenPresent(record::getIcon)
                .set(tags).equalToWhenPresent(record::getTags);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.029+02:00", comments="Source Table: indicator")
    default int updateByPrimaryKey(IndicatorRecord record) {
        return UpdateDSL.updateWithMapper(this::update, indicatorRecord)
                .set(examId).equalTo(record::getExamId)
                .set(type).equalTo(record::getType)
                .set(name).equalTo(record::getName)
                .set(color).equalTo(record::getColor)
                .set(icon).equalTo(record::getIcon)
                .set(tags).equalTo(record::getTags)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.029+02:00", comments="Source Table: indicator")
    default int updateByPrimaryKeySelective(IndicatorRecord record) {
        return UpdateDSL.updateWithMapper(this::update, indicatorRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(type).equalToWhenPresent(record::getType)
                .set(name).equalToWhenPresent(record::getName)
                .set(color).equalToWhenPresent(record::getColor)
                .set(icon).equalToWhenPresent(record::getIcon)
                .set(tags).equalToWhenPresent(record::getTags)
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
                        .from(indicatorRecord);
    }
}