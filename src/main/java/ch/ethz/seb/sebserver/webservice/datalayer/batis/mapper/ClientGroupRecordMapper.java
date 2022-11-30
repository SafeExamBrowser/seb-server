package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientGroupRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientGroupRecord;
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
public interface ClientGroupRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.077+01:00", comments="Source Table: client_group")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.077+01:00", comments="Source Table: client_group")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.077+01:00", comments="Source Table: client_group")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ClientGroupRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.077+01:00", comments="Source Table: client_group")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="color", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="icon", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="data", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    ClientGroupRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.077+01:00", comments="Source Table: client_group")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="color", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="icon", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="data", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<ClientGroupRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(clientGroupRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientGroupRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientGroupRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default int insert(ClientGroupRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientGroupRecord)
                .map(examId).toProperty("examId")
                .map(name).toProperty("name")
                .map(type).toProperty("type")
                .map(color).toProperty("color")
                .map(icon).toProperty("icon")
                .map(data).toProperty("data")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default int insertSelective(ClientGroupRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientGroupRecord)
                .map(examId).toPropertyWhenPresent("examId", record::getExamId)
                .map(name).toPropertyWhenPresent("name", record::getName)
                .map(type).toPropertyWhenPresent("type", record::getType)
                .map(color).toPropertyWhenPresent("color", record::getColor)
                .map(icon).toPropertyWhenPresent("icon", record::getIcon)
                .map(data).toPropertyWhenPresent("data", record::getData)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientGroupRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, examId, name, type, color, icon, data)
                .from(clientGroupRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientGroupRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, examId, name, type, color, icon, data)
                .from(clientGroupRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default ClientGroupRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, examId, name, type, color, icon, data)
                .from(clientGroupRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ClientGroupRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientGroupRecord)
                .set(examId).equalTo(record::getExamId)
                .set(name).equalTo(record::getName)
                .set(type).equalTo(record::getType)
                .set(color).equalTo(record::getColor)
                .set(icon).equalTo(record::getIcon)
                .set(data).equalTo(record::getData);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ClientGroupRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientGroupRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(name).equalToWhenPresent(record::getName)
                .set(type).equalToWhenPresent(record::getType)
                .set(color).equalToWhenPresent(record::getColor)
                .set(icon).equalToWhenPresent(record::getIcon)
                .set(data).equalToWhenPresent(record::getData);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default int updateByPrimaryKey(ClientGroupRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientGroupRecord)
                .set(examId).equalTo(record::getExamId)
                .set(name).equalTo(record::getName)
                .set(type).equalTo(record::getType)
                .set(color).equalTo(record::getColor)
                .set(icon).equalTo(record::getIcon)
                .set(data).equalTo(record::getData)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-29T10:28:50.078+01:00", comments="Source Table: client_group")
    default int updateByPrimaryKeySelective(ClientGroupRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientGroupRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(name).equalToWhenPresent(record::getName)
                .set(type).equalToWhenPresent(record::getType)
                .set(color).equalToWhenPresent(record::getColor)
                .set(icon).equalToWhenPresent(record::getIcon)
                .set(data).equalToWhenPresent(record::getData)
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
                        .from(clientGroupRecord);
    }
}