package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientNotificationRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientNotificationRecord;
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
public interface ClientNotificationRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source Table: client_notification")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source Table: client_notification")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source Table: client_notification")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ClientNotificationRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source Table: client_notification")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="client_connection_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="event_type", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="notification_type", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="value", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="text", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    ClientNotificationRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source Table: client_notification")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="client_connection_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="event_type", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="notification_type", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="value", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="text", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<ClientNotificationRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source Table: client_notification")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source Table: client_notification")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(clientNotificationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source Table: client_notification")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientNotificationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.224+01:00", comments="Source Table: client_notification")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientNotificationRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.224+01:00", comments="Source Table: client_notification")
    default int insert(ClientNotificationRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientNotificationRecord)
                .map(clientConnectionId).toProperty("clientConnectionId")
                .map(eventType).toProperty("eventType")
                .map(notificationType).toProperty("notificationType")
                .map(value).toProperty("value")
                .map(text).toProperty("text")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.224+01:00", comments="Source Table: client_notification")
    default int insertSelective(ClientNotificationRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientNotificationRecord)
                .map(clientConnectionId).toPropertyWhenPresent("clientConnectionId", record::getClientConnectionId)
                .map(eventType).toPropertyWhenPresent("eventType", record::getEventType)
                .map(notificationType).toPropertyWhenPresent("notificationType", record::getNotificationType)
                .map(value).toPropertyWhenPresent("value", record::getValue)
                .map(text).toPropertyWhenPresent("text", record::getText)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.224+01:00", comments="Source Table: client_notification")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientNotificationRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, clientConnectionId, eventType, notificationType, value, text)
                .from(clientNotificationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.224+01:00", comments="Source Table: client_notification")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientNotificationRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, clientConnectionId, eventType, notificationType, value, text)
                .from(clientNotificationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.224+01:00", comments="Source Table: client_notification")
    default ClientNotificationRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, clientConnectionId, eventType, notificationType, value, text)
                .from(clientNotificationRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.224+01:00", comments="Source Table: client_notification")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ClientNotificationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientNotificationRecord)
                .set(clientConnectionId).equalTo(record::getClientConnectionId)
                .set(eventType).equalTo(record::getEventType)
                .set(notificationType).equalTo(record::getNotificationType)
                .set(value).equalTo(record::getValue)
                .set(text).equalTo(record::getText);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.224+01:00", comments="Source Table: client_notification")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ClientNotificationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientNotificationRecord)
                .set(clientConnectionId).equalToWhenPresent(record::getClientConnectionId)
                .set(eventType).equalToWhenPresent(record::getEventType)
                .set(notificationType).equalToWhenPresent(record::getNotificationType)
                .set(value).equalToWhenPresent(record::getValue)
                .set(text).equalToWhenPresent(record::getText);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.224+01:00", comments="Source Table: client_notification")
    default int updateByPrimaryKey(ClientNotificationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientNotificationRecord)
                .set(clientConnectionId).equalTo(record::getClientConnectionId)
                .set(eventType).equalTo(record::getEventType)
                .set(notificationType).equalTo(record::getNotificationType)
                .set(value).equalTo(record::getValue)
                .set(text).equalTo(record::getText)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.224+01:00", comments="Source Table: client_notification")
    default int updateByPrimaryKeySelective(ClientNotificationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientNotificationRecord)
                .set(clientConnectionId).equalToWhenPresent(record::getClientConnectionId)
                .set(eventType).equalToWhenPresent(record::getEventType)
                .set(notificationType).equalToWhenPresent(record::getNotificationType)
                .set(value).equalToWhenPresent(record::getValue)
                .set(text).equalToWhenPresent(record::getText)
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
                        .from(clientNotificationRecord);
    }
}