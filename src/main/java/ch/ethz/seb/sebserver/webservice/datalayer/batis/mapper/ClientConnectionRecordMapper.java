package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
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
public interface ClientConnectionRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.962+02:00", comments="Source Table: client_connection")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.962+02:00", comments="Source Table: client_connection")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.962+02:00", comments="Source Table: client_connection")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ClientConnectionRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.962+02:00", comments="Source Table: client_connection")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="status", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="connection_token", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="user_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="vdi", javaType=Boolean.class, jdbcType=JdbcType.BOOLEAN),
        @Arg(column="client_address", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="virtual_client_address", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    ClientConnectionRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.962+02:00", comments="Source Table: client_connection")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="status", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="connection_token", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="user_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="vdi", javaType=Boolean.class, jdbcType=JdbcType.BOOLEAN),
        @Arg(column="client_address", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="virtual_client_address", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<ClientConnectionRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.962+02:00", comments="Source Table: client_connection")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.962+02:00", comments="Source Table: client_connection")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(clientConnectionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.962+02:00", comments="Source Table: client_connection")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientConnectionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.963+02:00", comments="Source Table: client_connection")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientConnectionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.963+02:00", comments="Source Table: client_connection")
    default int insert(ClientConnectionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientConnectionRecord)
                .map(examId).toProperty("examId")
                .map(status).toProperty("status")
                .map(connectionToken).toProperty("connectionToken")
                .map(userName).toProperty("userName")
                .map(vdi).toProperty("vdi")
                .map(clientAddress).toProperty("clientAddress")
                .map(virtualClientAddress).toProperty("virtualClientAddress")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.963+02:00", comments="Source Table: client_connection")
    default int insertSelective(ClientConnectionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientConnectionRecord)
                .map(examId).toPropertyWhenPresent("examId", record::getExamId)
                .map(status).toPropertyWhenPresent("status", record::getStatus)
                .map(connectionToken).toPropertyWhenPresent("connectionToken", record::getConnectionToken)
                .map(userName).toPropertyWhenPresent("userName", record::getUserName)
                .map(vdi).toPropertyWhenPresent("vdi", record::getVdi)
                .map(clientAddress).toPropertyWhenPresent("clientAddress", record::getClientAddress)
                .map(virtualClientAddress).toPropertyWhenPresent("virtualClientAddress", record::getVirtualClientAddress)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.963+02:00", comments="Source Table: client_connection")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientConnectionRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, examId, status, connectionToken, userName, vdi, clientAddress, virtualClientAddress)
                .from(clientConnectionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.963+02:00", comments="Source Table: client_connection")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientConnectionRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, examId, status, connectionToken, userName, vdi, clientAddress, virtualClientAddress)
                .from(clientConnectionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.963+02:00", comments="Source Table: client_connection")
    default ClientConnectionRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, examId, status, connectionToken, userName, vdi, clientAddress, virtualClientAddress)
                .from(clientConnectionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.963+02:00", comments="Source Table: client_connection")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ClientConnectionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientConnectionRecord)
                .set(examId).equalTo(record::getExamId)
                .set(status).equalTo(record::getStatus)
                .set(connectionToken).equalTo(record::getConnectionToken)
                .set(userName).equalTo(record::getUserName)
                .set(vdi).equalTo(record::getVdi)
                .set(clientAddress).equalTo(record::getClientAddress)
                .set(virtualClientAddress).equalTo(record::getVirtualClientAddress);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.963+02:00", comments="Source Table: client_connection")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ClientConnectionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientConnectionRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(status).equalToWhenPresent(record::getStatus)
                .set(connectionToken).equalToWhenPresent(record::getConnectionToken)
                .set(userName).equalToWhenPresent(record::getUserName)
                .set(vdi).equalToWhenPresent(record::getVdi)
                .set(clientAddress).equalToWhenPresent(record::getClientAddress)
                .set(virtualClientAddress).equalToWhenPresent(record::getVirtualClientAddress);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.963+02:00", comments="Source Table: client_connection")
    default int updateByPrimaryKey(ClientConnectionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientConnectionRecord)
                .set(examId).equalTo(record::getExamId)
                .set(status).equalTo(record::getStatus)
                .set(connectionToken).equalTo(record::getConnectionToken)
                .set(userName).equalTo(record::getUserName)
                .set(vdi).equalTo(record::getVdi)
                .set(clientAddress).equalTo(record::getClientAddress)
                .set(virtualClientAddress).equalTo(record::getVirtualClientAddress)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-16T09:31:18.963+02:00", comments="Source Table: client_connection")
    default int updateByPrimaryKeySelective(ClientConnectionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientConnectionRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(status).equalToWhenPresent(record::getStatus)
                .set(connectionToken).equalToWhenPresent(record::getConnectionToken)
                .set(userName).equalToWhenPresent(record::getUserName)
                .set(vdi).equalToWhenPresent(record::getVdi)
                .set(clientAddress).equalToWhenPresent(record::getClientAddress)
                .set(virtualClientAddress).equalToWhenPresent(record::getVirtualClientAddress)
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
                        .from(clientConnectionRecord);
    }
}