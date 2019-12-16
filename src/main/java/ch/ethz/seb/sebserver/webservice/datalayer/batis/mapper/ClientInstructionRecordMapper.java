package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientInstructionRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientInstructionRecord;
import java.util.List;
import javax.annotation.Generated;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
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
public interface ClientInstructionRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    int insert(InsertStatementProvider<ClientInstructionRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="connections", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="attributes", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    ClientInstructionRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="connections", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="attributes", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    List<ClientInstructionRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(clientInstructionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientInstructionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientInstructionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    default int insert(ClientInstructionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientInstructionRecord)
                .map(id).toProperty("id")
                .map(examId).toProperty("examId")
                .map(type).toProperty("type")
                .map(connections).toProperty("connections")
                .map(attributes).toProperty("attributes")
                .map(active).toProperty("active")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    default int insertSelective(ClientInstructionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientInstructionRecord)
                .map(id).toPropertyWhenPresent("id", record::getId)
                .map(examId).toPropertyWhenPresent("examId", record::getExamId)
                .map(type).toPropertyWhenPresent("type", record::getType)
                .map(connections).toPropertyWhenPresent("connections", record::getConnections)
                .map(attributes).toPropertyWhenPresent("attributes", record::getAttributes)
                .map(active).toPropertyWhenPresent("active", record::getActive)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientInstructionRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, examId, type, connections, attributes, active)
                .from(clientInstructionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientInstructionRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, examId, type, connections, attributes, active)
                .from(clientInstructionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    default ClientInstructionRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, examId, type, connections, attributes, active)
                .from(clientInstructionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ClientInstructionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientInstructionRecord)
                .set(id).equalTo(record::getId)
                .set(examId).equalTo(record::getExamId)
                .set(type).equalTo(record::getType)
                .set(connections).equalTo(record::getConnections)
                .set(attributes).equalTo(record::getAttributes)
                .set(active).equalTo(record::getActive);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.847+01:00", comments="Source Table: client_instruction")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ClientInstructionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientInstructionRecord)
                .set(id).equalToWhenPresent(record::getId)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(type).equalToWhenPresent(record::getType)
                .set(connections).equalToWhenPresent(record::getConnections)
                .set(attributes).equalToWhenPresent(record::getAttributes)
                .set(active).equalToWhenPresent(record::getActive);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.848+01:00", comments="Source Table: client_instruction")
    default int updateByPrimaryKey(ClientInstructionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientInstructionRecord)
                .set(examId).equalTo(record::getExamId)
                .set(type).equalTo(record::getType)
                .set(connections).equalTo(record::getConnections)
                .set(attributes).equalTo(record::getAttributes)
                .set(active).equalTo(record::getActive)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-13T19:17:48.848+01:00", comments="Source Table: client_instruction")
    default int updateByPrimaryKeySelective(ClientInstructionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientInstructionRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(type).equalToWhenPresent(record::getType)
                .set(connections).equalToWhenPresent(record::getConnections)
                .set(attributes).equalToWhenPresent(record::getAttributes)
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
                        .from(clientInstructionRecord);
    }
}