package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ViewRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ViewRecord;
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
public interface ViewRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.101+01:00", comments="Source Table: view")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ViewRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="columns", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="position", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="template_id", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    ViewRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="columns", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="position", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="template_id", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    List<ViewRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(viewRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, viewRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, viewRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    default int insert(ViewRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(viewRecord)
                .map(name).toProperty("name")
                .map(columns).toProperty("columns")
                .map(position).toProperty("position")
                .map(templateId).toProperty("templateId")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    default int insertSelective(ViewRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(viewRecord)
                .map(name).toPropertyWhenPresent("name", record::getName)
                .map(columns).toPropertyWhenPresent("columns", record::getColumns)
                .map(position).toPropertyWhenPresent("position", record::getPosition)
                .map(templateId).toPropertyWhenPresent("templateId", record::getTemplateId)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ViewRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, name, columns, position, templateId)
                .from(viewRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ViewRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, name, columns, position, templateId)
                .from(viewRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    default ViewRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, name, columns, position, templateId)
                .from(viewRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.102+01:00", comments="Source Table: view")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ViewRecord record) {
        return UpdateDSL.updateWithMapper(this::update, viewRecord)
                .set(name).equalTo(record::getName)
                .set(columns).equalTo(record::getColumns)
                .set(position).equalTo(record::getPosition)
                .set(templateId).equalTo(record::getTemplateId);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.103+01:00", comments="Source Table: view")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ViewRecord record) {
        return UpdateDSL.updateWithMapper(this::update, viewRecord)
                .set(name).equalToWhenPresent(record::getName)
                .set(columns).equalToWhenPresent(record::getColumns)
                .set(position).equalToWhenPresent(record::getPosition)
                .set(templateId).equalToWhenPresent(record::getTemplateId);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.103+01:00", comments="Source Table: view")
    default int updateByPrimaryKey(ViewRecord record) {
        return UpdateDSL.updateWithMapper(this::update, viewRecord)
                .set(name).equalTo(record::getName)
                .set(columns).equalTo(record::getColumns)
                .set(position).equalTo(record::getPosition)
                .set(templateId).equalTo(record::getTemplateId)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.103+01:00", comments="Source Table: view")
    default int updateByPrimaryKeySelective(ViewRecord record) {
        return UpdateDSL.updateWithMapper(this::update, viewRecord)
                .set(name).equalToWhenPresent(record::getName)
                .set(columns).equalToWhenPresent(record::getColumns)
                .set(position).equalToWhenPresent(record::getPosition)
                .set(templateId).equalToWhenPresent(record::getTemplateId)
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
                        .from(viewRecord);
    }
}