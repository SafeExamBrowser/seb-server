package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationNodeRecord;
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
public interface ConfigurationNodeRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ConfigurationNodeRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="owner", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="description", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="template", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    ConfigurationNodeRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="owner", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="description", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="template", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    List<ConfigurationNodeRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(configurationNodeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, configurationNodeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, configurationNodeRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default int insert(ConfigurationNodeRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(configurationNodeRecord)
                .map(institutionId).toProperty("institutionId")
                .map(owner).toProperty("owner")
                .map(name).toProperty("name")
                .map(description).toProperty("description")
                .map(type).toProperty("type")
                .map(template).toProperty("template")
                .map(active).toProperty("active")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default int insertSelective(ConfigurationNodeRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(configurationNodeRecord)
                .map(institutionId).toPropertyWhenPresent("institutionId", record::getInstitutionId)
                .map(owner).toPropertyWhenPresent("owner", record::getOwner)
                .map(name).toPropertyWhenPresent("name", record::getName)
                .map(description).toPropertyWhenPresent("description", record::getDescription)
                .map(type).toPropertyWhenPresent("type", record::getType)
                .map(template).toPropertyWhenPresent("template", record::getTemplate)
                .map(active).toPropertyWhenPresent("active", record::getActive)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ConfigurationNodeRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, institutionId, owner, name, description, type, template, active)
                .from(configurationNodeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ConfigurationNodeRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, institutionId, owner, name, description, type, template, active)
                .from(configurationNodeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default ConfigurationNodeRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, institutionId, owner, name, description, type, template, active)
                .from(configurationNodeRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ConfigurationNodeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, configurationNodeRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(owner).equalTo(record::getOwner)
                .set(name).equalTo(record::getName)
                .set(description).equalTo(record::getDescription)
                .set(type).equalTo(record::getType)
                .set(template).equalTo(record::getTemplate)
                .set(active).equalTo(record::getActive);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ConfigurationNodeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, configurationNodeRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(owner).equalToWhenPresent(record::getOwner)
                .set(name).equalToWhenPresent(record::getName)
                .set(description).equalToWhenPresent(record::getDescription)
                .set(type).equalToWhenPresent(record::getType)
                .set(template).equalToWhenPresent(record::getTemplate)
                .set(active).equalToWhenPresent(record::getActive);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default int updateByPrimaryKey(ConfigurationNodeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, configurationNodeRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(owner).equalTo(record::getOwner)
                .set(name).equalTo(record::getName)
                .set(description).equalTo(record::getDescription)
                .set(type).equalTo(record::getType)
                .set(template).equalTo(record::getTemplate)
                .set(active).equalTo(record::getActive)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-21T16:09:17.046+01:00", comments="Source Table: configuration_node")
    default int updateByPrimaryKeySelective(ConfigurationNodeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, configurationNodeRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(owner).equalToWhenPresent(record::getOwner)
                .set(name).equalToWhenPresent(record::getName)
                .set(description).equalToWhenPresent(record::getDescription)
                .set(type).equalToWhenPresent(record::getType)
                .set(template).equalToWhenPresent(record::getTemplate)
                .set(active).equalToWhenPresent(record::getActive)
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
                        .from(configurationNodeRecord);
    }
}