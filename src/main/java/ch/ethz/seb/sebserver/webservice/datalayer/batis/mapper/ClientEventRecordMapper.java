package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import java.math.BigDecimal;
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
public interface ClientEventRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.126+02:00", comments="Source Table: client_event")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.126+02:00", comments="Source Table: client_event")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.126+02:00", comments="Source Table: client_event")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ClientEventRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.126+02:00", comments="Source Table: client_event")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="connection_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="user_identifier", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="timestamp", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="numeric_value", javaType=BigDecimal.class, jdbcType=JdbcType.DECIMAL),
        @Arg(column="text", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    ClientEventRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.126+02:00", comments="Source Table: client_event")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="connection_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="user_identifier", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="timestamp", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="numeric_value", javaType=BigDecimal.class, jdbcType=JdbcType.DECIMAL),
        @Arg(column="text", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<ClientEventRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.126+02:00", comments="Source Table: client_event")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.126+02:00", comments="Source Table: client_event")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(clientEventRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.126+02:00", comments="Source Table: client_event")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientEventRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.126+02:00", comments="Source Table: client_event")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientEventRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.126+02:00", comments="Source Table: client_event")
    default int insert(ClientEventRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientEventRecord)
                .map(connectionId).toProperty("connectionId")
                .map(userIdentifier).toProperty("userIdentifier")
                .map(type).toProperty("type")
                .map(timestamp).toProperty("timestamp")
                .map(numericValue).toProperty("numericValue")
                .map(text).toProperty("text")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.127+02:00", comments="Source Table: client_event")
    default int insertSelective(ClientEventRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientEventRecord)
                .map(connectionId).toPropertyWhenPresent("connectionId", record::getConnectionId)
                .map(userIdentifier).toPropertyWhenPresent("userIdentifier", record::getUserIdentifier)
                .map(type).toPropertyWhenPresent("type", record::getType)
                .map(timestamp).toPropertyWhenPresent("timestamp", record::getTimestamp)
                .map(numericValue).toPropertyWhenPresent("numericValue", record::getNumericValue)
                .map(text).toPropertyWhenPresent("text", record::getText)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.127+02:00", comments="Source Table: client_event")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientEventRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, connectionId, userIdentifier, type, timestamp, numericValue, text)
                .from(clientEventRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.127+02:00", comments="Source Table: client_event")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientEventRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, connectionId, userIdentifier, type, timestamp, numericValue, text)
                .from(clientEventRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.127+02:00", comments="Source Table: client_event")
    default ClientEventRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, connectionId, userIdentifier, type, timestamp, numericValue, text)
                .from(clientEventRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.127+02:00", comments="Source Table: client_event")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ClientEventRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientEventRecord)
                .set(connectionId).equalTo(record::getConnectionId)
                .set(userIdentifier).equalTo(record::getUserIdentifier)
                .set(type).equalTo(record::getType)
                .set(timestamp).equalTo(record::getTimestamp)
                .set(numericValue).equalTo(record::getNumericValue)
                .set(text).equalTo(record::getText);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.127+02:00", comments="Source Table: client_event")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ClientEventRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientEventRecord)
                .set(connectionId).equalToWhenPresent(record::getConnectionId)
                .set(userIdentifier).equalToWhenPresent(record::getUserIdentifier)
                .set(type).equalToWhenPresent(record::getType)
                .set(timestamp).equalToWhenPresent(record::getTimestamp)
                .set(numericValue).equalToWhenPresent(record::getNumericValue)
                .set(text).equalToWhenPresent(record::getText);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.127+02:00", comments="Source Table: client_event")
    default int updateByPrimaryKey(ClientEventRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientEventRecord)
                .set(connectionId).equalTo(record::getConnectionId)
                .set(userIdentifier).equalTo(record::getUserIdentifier)
                .set(type).equalTo(record::getType)
                .set(timestamp).equalTo(record::getTimestamp)
                .set(numericValue).equalTo(record::getNumericValue)
                .set(text).equalTo(record::getText)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-04-17T16:13:34.127+02:00", comments="Source Table: client_event")
    default int updateByPrimaryKeySelective(ClientEventRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientEventRecord)
                .set(connectionId).equalToWhenPresent(record::getConnectionId)
                .set(userIdentifier).equalToWhenPresent(record::getUserIdentifier)
                .set(type).equalToWhenPresent(record::getType)
                .set(timestamp).equalToWhenPresent(record::getTimestamp)
                .set(numericValue).equalToWhenPresent(record::getNumericValue)
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
                        .from(clientEventRecord);
    }
}