package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.UserActivityLogRecord;
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
public interface UserActivityLogRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<UserActivityLogRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="user_uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="timestamp", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="activity_type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="entity_type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="entity_id", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="message", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    UserActivityLogRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="user_uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="timestamp", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="activity_type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="entity_type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="entity_id", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="message", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<UserActivityLogRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(userActivityLogRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, userActivityLogRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, userActivityLogRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    default int insert(UserActivityLogRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(userActivityLogRecord)
                .map(userUuid).toProperty("userUuid")
                .map(timestamp).toProperty("timestamp")
                .map(activityType).toProperty("activityType")
                .map(entityType).toProperty("entityType")
                .map(entityId).toProperty("entityId")
                .map(message).toProperty("message")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    default int insertSelective(UserActivityLogRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(userActivityLogRecord)
                .map(userUuid).toPropertyWhenPresent("userUuid", record::getUserUuid)
                .map(timestamp).toPropertyWhenPresent("timestamp", record::getTimestamp)
                .map(activityType).toPropertyWhenPresent("activityType", record::getActivityType)
                .map(entityType).toPropertyWhenPresent("entityType", record::getEntityType)
                .map(entityId).toPropertyWhenPresent("entityId", record::getEntityId)
                .map(message).toPropertyWhenPresent("message", record::getMessage)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<UserActivityLogRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, userUuid, timestamp, activityType, entityType, entityId, message)
                .from(userActivityLogRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<UserActivityLogRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, userUuid, timestamp, activityType, entityType, entityId, message)
                .from(userActivityLogRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    default UserActivityLogRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, userUuid, timestamp, activityType, entityType, entityId, message)
                .from(userActivityLogRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(UserActivityLogRecord record) {
        return UpdateDSL.updateWithMapper(this::update, userActivityLogRecord)
                .set(userUuid).equalTo(record::getUserUuid)
                .set(timestamp).equalTo(record::getTimestamp)
                .set(activityType).equalTo(record::getActivityType)
                .set(entityType).equalTo(record::getEntityType)
                .set(entityId).equalTo(record::getEntityId)
                .set(message).equalTo(record::getMessage);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(UserActivityLogRecord record) {
        return UpdateDSL.updateWithMapper(this::update, userActivityLogRecord)
                .set(userUuid).equalToWhenPresent(record::getUserUuid)
                .set(timestamp).equalToWhenPresent(record::getTimestamp)
                .set(activityType).equalToWhenPresent(record::getActivityType)
                .set(entityType).equalToWhenPresent(record::getEntityType)
                .set(entityId).equalToWhenPresent(record::getEntityId)
                .set(message).equalToWhenPresent(record::getMessage);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source Table: user_activity_log")
    default int updateByPrimaryKey(UserActivityLogRecord record) {
        return UpdateDSL.updateWithMapper(this::update, userActivityLogRecord)
                .set(userUuid).equalTo(record::getUserUuid)
                .set(timestamp).equalTo(record::getTimestamp)
                .set(activityType).equalTo(record::getActivityType)
                .set(entityType).equalTo(record::getEntityType)
                .set(entityId).equalTo(record::getEntityId)
                .set(message).equalTo(record::getMessage)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.060+02:00", comments="Source Table: user_activity_log")
    default int updateByPrimaryKeySelective(UserActivityLogRecord record) {
        return UpdateDSL.updateWithMapper(this::update, userActivityLogRecord)
                .set(userUuid).equalToWhenPresent(record::getUserUuid)
                .set(timestamp).equalToWhenPresent(record::getTimestamp)
                .set(activityType).equalToWhenPresent(record::getActivityType)
                .set(entityType).equalToWhenPresent(record::getEntityType)
                .set(entityId).equalToWhenPresent(record::getEntityId)
                .set(message).equalToWhenPresent(record::getMessage)
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
                        .from(userActivityLogRecord);
    }
}