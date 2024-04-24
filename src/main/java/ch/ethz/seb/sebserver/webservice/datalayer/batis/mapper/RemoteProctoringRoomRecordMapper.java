package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.RemoteProctoringRoomRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.RemoteProctoringRoomRecord;
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
public interface RemoteProctoringRoomRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<RemoteProctoringRoomRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="size", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="subject", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="townhall_room", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="break_out_connections", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="join_key", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="room_data", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    RemoteProctoringRoomRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="size", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="subject", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="townhall_room", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="break_out_connections", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="join_key", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="room_data", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<RemoteProctoringRoomRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(remoteProctoringRoomRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, remoteProctoringRoomRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, remoteProctoringRoomRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default int insert(RemoteProctoringRoomRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(remoteProctoringRoomRecord)
                .map(examId).toProperty("examId")
                .map(name).toProperty("name")
                .map(size).toProperty("size")
                .map(subject).toProperty("subject")
                .map(townhallRoom).toProperty("townhallRoom")
                .map(breakOutConnections).toProperty("breakOutConnections")
                .map(joinKey).toProperty("joinKey")
                .map(roomData).toProperty("roomData")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default int insertSelective(RemoteProctoringRoomRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(remoteProctoringRoomRecord)
                .map(examId).toPropertyWhenPresent("examId", record::getExamId)
                .map(name).toPropertyWhenPresent("name", record::getName)
                .map(size).toPropertyWhenPresent("size", record::getSize)
                .map(subject).toPropertyWhenPresent("subject", record::getSubject)
                .map(townhallRoom).toPropertyWhenPresent("townhallRoom", record::getTownhallRoom)
                .map(breakOutConnections).toPropertyWhenPresent("breakOutConnections", record::getBreakOutConnections)
                .map(joinKey).toPropertyWhenPresent("joinKey", record::getJoinKey)
                .map(roomData).toPropertyWhenPresent("roomData", record::getRoomData)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<RemoteProctoringRoomRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, examId, name, size, subject, townhallRoom, breakOutConnections, joinKey, roomData)
                .from(remoteProctoringRoomRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<RemoteProctoringRoomRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, examId, name, size, subject, townhallRoom, breakOutConnections, joinKey, roomData)
                .from(remoteProctoringRoomRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default RemoteProctoringRoomRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, examId, name, size, subject, townhallRoom, breakOutConnections, joinKey, roomData)
                .from(remoteProctoringRoomRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(RemoteProctoringRoomRecord record) {
        return UpdateDSL.updateWithMapper(this::update, remoteProctoringRoomRecord)
                .set(examId).equalTo(record::getExamId)
                .set(name).equalTo(record::getName)
                .set(size).equalTo(record::getSize)
                .set(subject).equalTo(record::getSubject)
                .set(townhallRoom).equalTo(record::getTownhallRoom)
                .set(breakOutConnections).equalTo(record::getBreakOutConnections)
                .set(joinKey).equalTo(record::getJoinKey)
                .set(roomData).equalTo(record::getRoomData);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(RemoteProctoringRoomRecord record) {
        return UpdateDSL.updateWithMapper(this::update, remoteProctoringRoomRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(name).equalToWhenPresent(record::getName)
                .set(size).equalToWhenPresent(record::getSize)
                .set(subject).equalToWhenPresent(record::getSubject)
                .set(townhallRoom).equalToWhenPresent(record::getTownhallRoom)
                .set(breakOutConnections).equalToWhenPresent(record::getBreakOutConnections)
                .set(joinKey).equalToWhenPresent(record::getJoinKey)
                .set(roomData).equalToWhenPresent(record::getRoomData);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default int updateByPrimaryKey(RemoteProctoringRoomRecord record) {
        return UpdateDSL.updateWithMapper(this::update, remoteProctoringRoomRecord)
                .set(examId).equalTo(record::getExamId)
                .set(name).equalTo(record::getName)
                .set(size).equalTo(record::getSize)
                .set(subject).equalTo(record::getSubject)
                .set(townhallRoom).equalTo(record::getTownhallRoom)
                .set(breakOutConnections).equalTo(record::getBreakOutConnections)
                .set(joinKey).equalTo(record::getJoinKey)
                .set(roomData).equalTo(record::getRoomData)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.956+02:00", comments="Source Table: remote_proctoring_room")
    default int updateByPrimaryKeySelective(RemoteProctoringRoomRecord record) {
        return UpdateDSL.updateWithMapper(this::update, remoteProctoringRoomRecord)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(name).equalToWhenPresent(record::getName)
                .set(size).equalToWhenPresent(record::getSize)
                .set(subject).equalToWhenPresent(record::getSubject)
                .set(townhallRoom).equalToWhenPresent(record::getTownhallRoom)
                .set(breakOutConnections).equalToWhenPresent(record::getBreakOutConnections)
                .set(joinKey).equalToWhenPresent(record::getJoinKey)
                .set(roomData).equalToWhenPresent(record::getRoomData)
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
                        .from(remoteProctoringRoomRecord);
    }
}