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
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    int insert(InsertStatementProvider<ClientEventRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="client_connection_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="type", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="client_time", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="server_time", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="numeric_value", javaType=BigDecimal.class, jdbcType=JdbcType.DECIMAL),
        @Arg(column="text", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    ClientEventRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="client_connection_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="type", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="client_time", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="server_time", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="numeric_value", javaType=BigDecimal.class, jdbcType=JdbcType.DECIMAL),
        @Arg(column="text", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<ClientEventRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(clientEventRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientEventRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientEventRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    default int insert(ClientEventRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientEventRecord)
                .map(id).toProperty("id")
                .map(clientConnectionId).toProperty("clientConnectionId")
                .map(type).toProperty("type")
                .map(clientTime).toProperty("clientTime")
                .map(serverTime).toProperty("serverTime")
                .map(numericValue).toProperty("numericValue")
                .map(text).toProperty("text")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    default int insertSelective(ClientEventRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientEventRecord)
                .map(id).toPropertyWhenPresent("id", record::getId)
                .map(clientConnectionId).toPropertyWhenPresent("clientConnectionId", record::getClientConnectionId)
                .map(type).toPropertyWhenPresent("type", record::getType)
                .map(clientTime).toPropertyWhenPresent("clientTime", record::getClientTime)
                .map(serverTime).toPropertyWhenPresent("serverTime", record::getServerTime)
                .map(numericValue).toPropertyWhenPresent("numericValue", record::getNumericValue)
                .map(text).toPropertyWhenPresent("text", record::getText)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientEventRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, clientConnectionId, type, clientTime, serverTime, numericValue, text)
                .from(clientEventRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientEventRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, clientConnectionId, type, clientTime, serverTime, numericValue, text)
                .from(clientEventRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    default ClientEventRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, clientConnectionId, type, clientTime, serverTime, numericValue, text)
                .from(clientEventRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ClientEventRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientEventRecord)
                .set(id).equalTo(record::getId)
                .set(clientConnectionId).equalTo(record::getClientConnectionId)
                .set(type).equalTo(record::getType)
                .set(clientTime).equalTo(record::getClientTime)
                .set(serverTime).equalTo(record::getServerTime)
                .set(numericValue).equalTo(record::getNumericValue)
                .set(text).equalTo(record::getText);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ClientEventRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientEventRecord)
                .set(id).equalToWhenPresent(record::getId)
                .set(clientConnectionId).equalToWhenPresent(record::getClientConnectionId)
                .set(type).equalToWhenPresent(record::getType)
                .set(clientTime).equalToWhenPresent(record::getClientTime)
                .set(serverTime).equalToWhenPresent(record::getServerTime)
                .set(numericValue).equalToWhenPresent(record::getNumericValue)
                .set(text).equalToWhenPresent(record::getText);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.012+02:00", comments="Source Table: client_event")
    default int updateByPrimaryKey(ClientEventRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientEventRecord)
                .set(clientConnectionId).equalTo(record::getClientConnectionId)
                .set(type).equalTo(record::getType)
                .set(clientTime).equalTo(record::getClientTime)
                .set(serverTime).equalTo(record::getServerTime)
                .set(numericValue).equalTo(record::getNumericValue)
                .set(text).equalTo(record::getText)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.013+02:00", comments="Source Table: client_event")
    default int updateByPrimaryKeySelective(ClientEventRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientEventRecord)
                .set(clientConnectionId).equalToWhenPresent(record::getClientConnectionId)
                .set(type).equalToWhenPresent(record::getType)
                .set(clientTime).equalToWhenPresent(record::getClientTime)
                .set(serverTime).equalToWhenPresent(record::getServerTime)
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