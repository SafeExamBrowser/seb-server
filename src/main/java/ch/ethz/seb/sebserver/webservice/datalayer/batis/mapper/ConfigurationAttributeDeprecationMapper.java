package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeDeprecationDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationAttributeDeprecation;
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
public interface ConfigurationAttributeDeprecationMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: configuration_attribute_deprecation")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: configuration_attribute_deprecation")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: configuration_attribute_deprecation")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @Options(useGeneratedKeys=true,keyProperty="record.id")
    int insert(InsertStatementProvider<ConfigurationAttributeDeprecation> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: configuration_attribute_deprecation")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="configuration_attribute_id", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    ConfigurationAttributeDeprecation selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="configuration_attribute_id", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    List<ConfigurationAttributeDeprecation> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(configurationAttributeDeprecation);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, configurationAttributeDeprecation);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, configurationAttributeDeprecation)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default int insert(ConfigurationAttributeDeprecation record) {
        return insert(SqlBuilder.insert(record)
                .into(configurationAttributeDeprecation)
                .map(configurationAttributeId).toProperty("configurationAttributeId")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default int insertSelective(ConfigurationAttributeDeprecation record) {
        return insert(SqlBuilder.insert(record)
                .into(configurationAttributeDeprecation)
                .map(configurationAttributeId).toPropertyWhenPresent("configurationAttributeId", record::getConfigurationAttributeId)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ConfigurationAttributeDeprecation>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, configurationAttributeId)
                .from(configurationAttributeDeprecation);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ConfigurationAttributeDeprecation>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, configurationAttributeId)
                .from(configurationAttributeDeprecation);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default ConfigurationAttributeDeprecation selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, configurationAttributeId)
                .from(configurationAttributeDeprecation)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ConfigurationAttributeDeprecation record) {
        return UpdateDSL.updateWithMapper(this::update, configurationAttributeDeprecation)
                .set(configurationAttributeId).equalTo(record::getConfigurationAttributeId);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ConfigurationAttributeDeprecation record) {
        return UpdateDSL.updateWithMapper(this::update, configurationAttributeDeprecation)
                .set(configurationAttributeId).equalToWhenPresent(record::getConfigurationAttributeId);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default int updateByPrimaryKey(ConfigurationAttributeDeprecation record) {
        return UpdateDSL.updateWithMapper(this::update, configurationAttributeDeprecation)
                .set(configurationAttributeId).equalTo(record::getConfigurationAttributeId)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.101+02:00", comments="Source Table: configuration_attribute_deprecation")
    default int updateByPrimaryKeySelective(ConfigurationAttributeDeprecation record) {
        return UpdateDSL.updateWithMapper(this::update, configurationAttributeDeprecation)
                .set(configurationAttributeId).equalToWhenPresent(record::getConfigurationAttributeId)
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
                        .from(configurationAttributeDeprecation);
    }
}