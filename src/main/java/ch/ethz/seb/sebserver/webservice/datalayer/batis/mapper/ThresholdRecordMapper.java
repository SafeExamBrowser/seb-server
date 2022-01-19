package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ThresholdRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ThresholdRecord;
import java.math.BigDecimal;
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
public interface ThresholdRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.171+01:00", comments="Source Table: threshold")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.171+01:00", comments="Source Table: threshold")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.172+01:00", comments="Source Table: threshold")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ThresholdRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.172+01:00", comments="Source Table: threshold")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="indicator_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="value", javaType=BigDecimal.class, jdbcType=JdbcType.DECIMAL),
        @Arg(column="color", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="icon", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    ThresholdRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.172+01:00", comments="Source Table: threshold")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="indicator_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="value", javaType=BigDecimal.class, jdbcType=JdbcType.DECIMAL),
        @Arg(column="color", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="icon", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<ThresholdRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.172+01:00", comments="Source Table: threshold")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.172+01:00", comments="Source Table: threshold")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(thresholdRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.172+01:00", comments="Source Table: threshold")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, thresholdRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.172+01:00", comments="Source Table: threshold")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, thresholdRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.172+01:00", comments="Source Table: threshold")
    default int insert(ThresholdRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(thresholdRecord)
                .map(indicatorId).toProperty("indicatorId")
                .map(value).toProperty("value")
                .map(color).toProperty("color")
                .map(icon).toProperty("icon")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.172+01:00", comments="Source Table: threshold")
    default int insertSelective(ThresholdRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(thresholdRecord)
                .map(indicatorId).toPropertyWhenPresent("indicatorId", record::getIndicatorId)
                .map(value).toPropertyWhenPresent("value", record::getValue)
                .map(color).toPropertyWhenPresent("color", record::getColor)
                .map(icon).toPropertyWhenPresent("icon", record::getIcon)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.173+01:00", comments="Source Table: threshold")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ThresholdRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, indicatorId, value, color, icon)
                .from(thresholdRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.173+01:00", comments="Source Table: threshold")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ThresholdRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, indicatorId, value, color, icon)
                .from(thresholdRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.173+01:00", comments="Source Table: threshold")
    default ThresholdRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, indicatorId, value, color, icon)
                .from(thresholdRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.173+01:00", comments="Source Table: threshold")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ThresholdRecord record) {
        return UpdateDSL.updateWithMapper(this::update, thresholdRecord)
                .set(indicatorId).equalTo(record::getIndicatorId)
                .set(value).equalTo(record::getValue)
                .set(color).equalTo(record::getColor)
                .set(icon).equalTo(record::getIcon);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.173+01:00", comments="Source Table: threshold")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ThresholdRecord record) {
        return UpdateDSL.updateWithMapper(this::update, thresholdRecord)
                .set(indicatorId).equalToWhenPresent(record::getIndicatorId)
                .set(value).equalToWhenPresent(record::getValue)
                .set(color).equalToWhenPresent(record::getColor)
                .set(icon).equalToWhenPresent(record::getIcon);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.173+01:00", comments="Source Table: threshold")
    default int updateByPrimaryKey(ThresholdRecord record) {
        return UpdateDSL.updateWithMapper(this::update, thresholdRecord)
                .set(indicatorId).equalTo(record::getIndicatorId)
                .set(value).equalTo(record::getValue)
                .set(color).equalTo(record::getColor)
                .set(icon).equalTo(record::getIcon)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.173+01:00", comments="Source Table: threshold")
    default int updateByPrimaryKeySelective(ThresholdRecord record) {
        return UpdateDSL.updateWithMapper(this::update, thresholdRecord)
                .set(indicatorId).equalToWhenPresent(record::getIndicatorId)
                .set(value).equalToWhenPresent(record::getValue)
                .set(color).equalToWhenPresent(record::getColor)
                .set(icon).equalToWhenPresent(record::getIcon)
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
                        .from(thresholdRecord);
    }
}