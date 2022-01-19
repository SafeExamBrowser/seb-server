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
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.142+01:00", comments="Source Table: client_connection")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.142+01:00", comments="Source Table: client_connection")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.142+01:00", comments="Source Table: client_connection")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ClientConnectionRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.142+01:00", comments="Source Table: client_connection")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="status", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="connection_token", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="exam_user_session_id", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="client_address", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="virtual_client_address", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="vdi", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="vdi_pair_token", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="creation_time", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="update_time", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="remote_proctoring_room_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="remote_proctoring_room_update", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="client_machine_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="client_os_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="client_version", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    ClientConnectionRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.142+01:00", comments="Source Table: client_connection")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="status", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="connection_token", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="exam_user_session_id", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="client_address", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="virtual_client_address", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="vdi", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="vdi_pair_token", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="creation_time", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="update_time", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="remote_proctoring_room_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="remote_proctoring_room_update", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="client_machine_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="client_os_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="client_version", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<ClientConnectionRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.142+01:00", comments="Source Table: client_connection")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.142+01:00", comments="Source Table: client_connection")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(clientConnectionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.142+01:00", comments="Source Table: client_connection")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientConnectionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.142+01:00", comments="Source Table: client_connection")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientConnectionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.142+01:00", comments="Source Table: client_connection")
    default int insert(ClientConnectionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientConnectionRecord)
                .map(institutionId).toProperty("institutionId")
                .map(examId).toProperty("examId")
                .map(status).toProperty("status")
                .map(connectionToken).toProperty("connectionToken")
                .map(examUserSessionId).toProperty("examUserSessionId")
                .map(clientAddress).toProperty("clientAddress")
                .map(virtualClientAddress).toProperty("virtualClientAddress")
                .map(vdi).toProperty("vdi")
                .map(vdiPairToken).toProperty("vdiPairToken")
                .map(creationTime).toProperty("creationTime")
                .map(updateTime).toProperty("updateTime")
                .map(remoteProctoringRoomId).toProperty("remoteProctoringRoomId")
                .map(remoteProctoringRoomUpdate).toProperty("remoteProctoringRoomUpdate")
                .map(clientMachineName).toProperty("clientMachineName")
                .map(clientOsName).toProperty("clientOsName")
                .map(clientVersion).toProperty("clientVersion")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.143+01:00", comments="Source Table: client_connection")
    default int insertSelective(ClientConnectionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientConnectionRecord)
                .map(institutionId).toPropertyWhenPresent("institutionId", record::getInstitutionId)
                .map(examId).toPropertyWhenPresent("examId", record::getExamId)
                .map(status).toPropertyWhenPresent("status", record::getStatus)
                .map(connectionToken).toPropertyWhenPresent("connectionToken", record::getConnectionToken)
                .map(examUserSessionId).toPropertyWhenPresent("examUserSessionId", record::getExamUserSessionId)
                .map(clientAddress).toPropertyWhenPresent("clientAddress", record::getClientAddress)
                .map(virtualClientAddress).toPropertyWhenPresent("virtualClientAddress", record::getVirtualClientAddress)
                .map(vdi).toPropertyWhenPresent("vdi", record::getVdi)
                .map(vdiPairToken).toPropertyWhenPresent("vdiPairToken", record::getVdiPairToken)
                .map(creationTime).toPropertyWhenPresent("creationTime", record::getCreationTime)
                .map(updateTime).toPropertyWhenPresent("updateTime", record::getUpdateTime)
                .map(remoteProctoringRoomId).toPropertyWhenPresent("remoteProctoringRoomId", record::getRemoteProctoringRoomId)
                .map(remoteProctoringRoomUpdate).toPropertyWhenPresent("remoteProctoringRoomUpdate", record::getRemoteProctoringRoomUpdate)
                .map(clientMachineName).toPropertyWhenPresent("clientMachineName", record::getClientMachineName)
                .map(clientOsName).toPropertyWhenPresent("clientOsName", record::getClientOsName)
                .map(clientVersion).toPropertyWhenPresent("clientVersion", record::getClientVersion)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.143+01:00", comments="Source Table: client_connection")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientConnectionRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, institutionId, examId, status, connectionToken, examUserSessionId, clientAddress, virtualClientAddress, vdi, vdiPairToken, creationTime, updateTime, remoteProctoringRoomId, remoteProctoringRoomUpdate, clientMachineName, clientOsName, clientVersion)
                .from(clientConnectionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.143+01:00", comments="Source Table: client_connection")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientConnectionRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, institutionId, examId, status, connectionToken, examUserSessionId, clientAddress, virtualClientAddress, vdi, vdiPairToken, creationTime, updateTime, remoteProctoringRoomId, remoteProctoringRoomUpdate, clientMachineName, clientOsName, clientVersion)
                .from(clientConnectionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.143+01:00", comments="Source Table: client_connection")
    default ClientConnectionRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, institutionId, examId, status, connectionToken, examUserSessionId, clientAddress, virtualClientAddress, vdi, vdiPairToken, creationTime, updateTime, remoteProctoringRoomId, remoteProctoringRoomUpdate, clientMachineName, clientOsName, clientVersion)
                .from(clientConnectionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.143+01:00", comments="Source Table: client_connection")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ClientConnectionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientConnectionRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(examId).equalTo(record::getExamId)
                .set(status).equalTo(record::getStatus)
                .set(connectionToken).equalTo(record::getConnectionToken)
                .set(examUserSessionId).equalTo(record::getExamUserSessionId)
                .set(clientAddress).equalTo(record::getClientAddress)
                .set(virtualClientAddress).equalTo(record::getVirtualClientAddress)
                .set(vdi).equalTo(record::getVdi)
                .set(vdiPairToken).equalTo(record::getVdiPairToken)
                .set(creationTime).equalTo(record::getCreationTime)
                .set(updateTime).equalTo(record::getUpdateTime)
                .set(remoteProctoringRoomId).equalTo(record::getRemoteProctoringRoomId)
                .set(remoteProctoringRoomUpdate).equalTo(record::getRemoteProctoringRoomUpdate)
                .set(clientMachineName).equalTo(record::getClientMachineName)
                .set(clientOsName).equalTo(record::getClientOsName)
                .set(clientVersion).equalTo(record::getClientVersion);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.143+01:00", comments="Source Table: client_connection")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ClientConnectionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientConnectionRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(status).equalToWhenPresent(record::getStatus)
                .set(connectionToken).equalToWhenPresent(record::getConnectionToken)
                .set(examUserSessionId).equalToWhenPresent(record::getExamUserSessionId)
                .set(clientAddress).equalToWhenPresent(record::getClientAddress)
                .set(virtualClientAddress).equalToWhenPresent(record::getVirtualClientAddress)
                .set(vdi).equalToWhenPresent(record::getVdi)
                .set(vdiPairToken).equalToWhenPresent(record::getVdiPairToken)
                .set(creationTime).equalToWhenPresent(record::getCreationTime)
                .set(updateTime).equalToWhenPresent(record::getUpdateTime)
                .set(remoteProctoringRoomId).equalToWhenPresent(record::getRemoteProctoringRoomId)
                .set(remoteProctoringRoomUpdate).equalToWhenPresent(record::getRemoteProctoringRoomUpdate)
                .set(clientMachineName).equalToWhenPresent(record::getClientMachineName)
                .set(clientOsName).equalToWhenPresent(record::getClientOsName)
                .set(clientVersion).equalToWhenPresent(record::getClientVersion);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.143+01:00", comments="Source Table: client_connection")
    default int updateByPrimaryKey(ClientConnectionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientConnectionRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(examId).equalTo(record::getExamId)
                .set(status).equalTo(record::getStatus)
                .set(connectionToken).equalTo(record::getConnectionToken)
                .set(examUserSessionId).equalTo(record::getExamUserSessionId)
                .set(clientAddress).equalTo(record::getClientAddress)
                .set(virtualClientAddress).equalTo(record::getVirtualClientAddress)
                .set(vdi).equalTo(record::getVdi)
                .set(vdiPairToken).equalTo(record::getVdiPairToken)
                .set(creationTime).equalTo(record::getCreationTime)
                .set(updateTime).equalTo(record::getUpdateTime)
                .set(remoteProctoringRoomId).equalTo(record::getRemoteProctoringRoomId)
                .set(remoteProctoringRoomUpdate).equalTo(record::getRemoteProctoringRoomUpdate)
                .set(clientMachineName).equalTo(record::getClientMachineName)
                .set(clientOsName).equalTo(record::getClientOsName)
                .set(clientVersion).equalTo(record::getClientVersion)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.143+01:00", comments="Source Table: client_connection")
    default int updateByPrimaryKeySelective(ClientConnectionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientConnectionRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(status).equalToWhenPresent(record::getStatus)
                .set(connectionToken).equalToWhenPresent(record::getConnectionToken)
                .set(examUserSessionId).equalToWhenPresent(record::getExamUserSessionId)
                .set(clientAddress).equalToWhenPresent(record::getClientAddress)
                .set(virtualClientAddress).equalToWhenPresent(record::getVirtualClientAddress)
                .set(vdi).equalToWhenPresent(record::getVdi)
                .set(vdiPairToken).equalToWhenPresent(record::getVdiPairToken)
                .set(creationTime).equalToWhenPresent(record::getCreationTime)
                .set(updateTime).equalToWhenPresent(record::getUpdateTime)
                .set(remoteProctoringRoomId).equalToWhenPresent(record::getRemoteProctoringRoomId)
                .set(remoteProctoringRoomUpdate).equalToWhenPresent(record::getRemoteProctoringRoomUpdate)
                .set(clientMachineName).equalToWhenPresent(record::getClientMachineName)
                .set(clientOsName).equalToWhenPresent(record::getClientOsName)
                .set(clientVersion).equalToWhenPresent(record::getClientVersion)
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