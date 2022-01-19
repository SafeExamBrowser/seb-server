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
public interface ClientInstructionRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ClientInstructionRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="connection_token", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="attributes", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="needs_confirmation", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="timestamp", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    ClientInstructionRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="connection_token", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="attributes", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="needs_confirmation", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="timestamp", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    List<ClientInstructionRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(clientInstructionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientInstructionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientInstructionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    default int insert(ClientInstructionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientInstructionRecord)
                .map(examId).toProperty("examId")
                .map(connectionToken).toProperty("connectionToken")
                .map(type).toProperty("type")
                .map(attributes).toProperty("attributes")
                .map(needsConfirmation).toProperty("needsConfirmation")
                .map(timestamp).toProperty("timestamp")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    default int insertSelective(ClientInstructionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientInstructionRecord)
                .map(examId).toPropertyWhenPresent("examId", record::getExamId)
                .map(connectionToken).toPropertyWhenPresent("connectionToken", record::getConnectionToken)
                .map(type).toPropertyWhenPresent("type", record::getType)
                .map(attributes).toPropertyWhenPresent("attributes", record::getAttributes)
                .map(needsConfirmation).toPropertyWhenPresent("needsConfirmation", record::getNeedsConfirmation)
                .map(timestamp).toPropertyWhenPresent("timestamp", record::getTimestamp)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientInstructionRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, examId, connectionToken, type, attributes, needsConfirmation, timestamp)
                .from(clientInstructionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientInstructionRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, examId, connectionToken, type, attributes, needsConfirmation, timestamp)
                .from(clientInstructionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    default ClientInstructionRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, examId, connectionToken, type, attributes, needsConfirmation, timestamp)
                .from(clientInstructionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ClientInstructionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientInstructionRecord)
                .set(examId).equalTo(record::getExamId)
                .set(connectionToken).equalTo(record::getConnectionToken)
                .set(type).equalTo(record::getType)
                .set(attributes).equalTo(record::getAttributes)
                .set(needsConfirmation).equalTo(record::getNeedsConfirmation)
                .set(timestamp).equalTo(record::getTimestamp);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ClientInstructionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientInstructionRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(connectionToken).equalToWhenPresent(record::getConnectionToken)
                .set(type).equalToWhenPresent(record::getType)
                .set(attributes).equalToWhenPresent(record::getAttributes)
                .set(needsConfirmation).equalToWhenPresent(record::getNeedsConfirmation)
                .set(timestamp).equalToWhenPresent(record::getTimestamp);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.162+01:00", comments="Source Table: client_instruction")
    default int updateByPrimaryKey(ClientInstructionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientInstructionRecord)
                .set(examId).equalTo(record::getExamId)
                .set(connectionToken).equalTo(record::getConnectionToken)
                .set(type).equalTo(record::getType)
                .set(attributes).equalTo(record::getAttributes)
                .set(needsConfirmation).equalTo(record::getNeedsConfirmation)
                .set(timestamp).equalTo(record::getTimestamp)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.165+01:00", comments="Source Table: client_instruction")
    default int updateByPrimaryKeySelective(ClientInstructionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientInstructionRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(connectionToken).equalToWhenPresent(record::getConnectionToken)
                .set(type).equalToWhenPresent(record::getType)
                .set(attributes).equalToWhenPresent(record::getAttributes)
                .set(needsConfirmation).equalToWhenPresent(record::getNeedsConfirmation)
                .set(timestamp).equalToWhenPresent(record::getTimestamp)
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