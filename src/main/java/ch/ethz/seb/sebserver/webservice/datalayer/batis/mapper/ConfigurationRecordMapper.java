package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.JodaTimeTypeResolver;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationRecord;
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
import org.joda.time.DateTime;
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
public interface ConfigurationRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.113+01:00", comments="Source Table: configuration")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.113+01:00", comments="Source Table: configuration")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.113+01:00", comments="Source Table: configuration")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ConfigurationRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.113+01:00", comments="Source Table: configuration")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="configuration_node_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="version", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="version_date", javaType=DateTime.class, typeHandler=JodaTimeTypeResolver.class, jdbcType=JdbcType.TIMESTAMP),
        @Arg(column="followup", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    ConfigurationRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.113+01:00", comments="Source Table: configuration")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="configuration_node_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="version", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="version_date", javaType=DateTime.class, typeHandler=JodaTimeTypeResolver.class, jdbcType=JdbcType.TIMESTAMP),
        @Arg(column="followup", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    List<ConfigurationRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.114+01:00", comments="Source Table: configuration")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.114+01:00", comments="Source Table: configuration")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(configurationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.114+01:00", comments="Source Table: configuration")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, configurationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.114+01:00", comments="Source Table: configuration")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, configurationRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.114+01:00", comments="Source Table: configuration")
    default int insert(ConfigurationRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(configurationRecord)
                .map(institutionId).toProperty("institutionId")
                .map(configurationNodeId).toProperty("configurationNodeId")
                .map(version).toProperty("version")
                .map(versionDate).toProperty("versionDate")
                .map(followup).toProperty("followup")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.115+01:00", comments="Source Table: configuration")
    default int insertSelective(ConfigurationRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(configurationRecord)
                .map(institutionId).toPropertyWhenPresent("institutionId", record::getInstitutionId)
                .map(configurationNodeId).toPropertyWhenPresent("configurationNodeId", record::getConfigurationNodeId)
                .map(version).toPropertyWhenPresent("version", record::getVersion)
                .map(versionDate).toPropertyWhenPresent("versionDate", record::getVersionDate)
                .map(followup).toPropertyWhenPresent("followup", record::getFollowup)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.115+01:00", comments="Source Table: configuration")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ConfigurationRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, institutionId, configurationNodeId, version, versionDate, followup)
                .from(configurationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.115+01:00", comments="Source Table: configuration")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ConfigurationRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, institutionId, configurationNodeId, version, versionDate, followup)
                .from(configurationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.115+01:00", comments="Source Table: configuration")
    default ConfigurationRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, institutionId, configurationNodeId, version, versionDate, followup)
                .from(configurationRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.115+01:00", comments="Source Table: configuration")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ConfigurationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, configurationRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(configurationNodeId).equalTo(record::getConfigurationNodeId)
                .set(version).equalTo(record::getVersion)
                .set(versionDate).equalTo(record::getVersionDate)
                .set(followup).equalTo(record::getFollowup);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.115+01:00", comments="Source Table: configuration")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ConfigurationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, configurationRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(configurationNodeId).equalToWhenPresent(record::getConfigurationNodeId)
                .set(version).equalToWhenPresent(record::getVersion)
                .set(versionDate).equalToWhenPresent(record::getVersionDate)
                .set(followup).equalToWhenPresent(record::getFollowup);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.115+01:00", comments="Source Table: configuration")
    default int updateByPrimaryKey(ConfigurationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, configurationRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(configurationNodeId).equalTo(record::getConfigurationNodeId)
                .set(version).equalTo(record::getVersion)
                .set(versionDate).equalTo(record::getVersionDate)
                .set(followup).equalTo(record::getFollowup)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.115+01:00", comments="Source Table: configuration")
    default int updateByPrimaryKeySelective(ConfigurationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, configurationRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(configurationNodeId).equalToWhenPresent(record::getConfigurationNodeId)
                .set(version).equalToWhenPresent(record::getVersion)
                .set(versionDate).equalToWhenPresent(record::getVersionDate)
                .set(followup).equalToWhenPresent(record::getFollowup)
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
                        .from(configurationRecord);
    }
}