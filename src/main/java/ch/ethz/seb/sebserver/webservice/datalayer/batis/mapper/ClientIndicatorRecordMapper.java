package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientIndicatorRecord;
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
public interface ClientIndicatorRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.079+02:00", comments="Source Table: client_indicator")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.079+02:00", comments="Source Table: client_indicator")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.079+02:00", comments="Source Table: client_indicator")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ClientIndicatorRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.079+02:00", comments="Source Table: client_indicator")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="client_connection_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="type", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="value", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    ClientIndicatorRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.079+02:00", comments="Source Table: client_indicator")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="client_connection_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="type", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="value", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    List<ClientIndicatorRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.080+02:00", comments="Source Table: client_indicator")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.080+02:00", comments="Source Table: client_indicator")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(clientIndicatorRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.080+02:00", comments="Source Table: client_indicator")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientIndicatorRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.080+02:00", comments="Source Table: client_indicator")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, clientIndicatorRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.080+02:00", comments="Source Table: client_indicator")
    default int insert(ClientIndicatorRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientIndicatorRecord)
                .map(clientConnectionId).toProperty("clientConnectionId")
                .map(type).toProperty("type")
                .map(value).toProperty("value")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.080+02:00", comments="Source Table: client_indicator")
    default int insertSelective(ClientIndicatorRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(clientIndicatorRecord)
                .map(clientConnectionId).toPropertyWhenPresent("clientConnectionId", record::getClientConnectionId)
                .map(type).toPropertyWhenPresent("type", record::getType)
                .map(value).toPropertyWhenPresent("value", record::getValue)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.080+02:00", comments="Source Table: client_indicator")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientIndicatorRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, clientConnectionId, type, value)
                .from(clientIndicatorRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.080+02:00", comments="Source Table: client_indicator")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ClientIndicatorRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, clientConnectionId, type, value)
                .from(clientIndicatorRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.080+02:00", comments="Source Table: client_indicator")
    default ClientIndicatorRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, clientConnectionId, type, value)
                .from(clientIndicatorRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.081+02:00", comments="Source Table: client_indicator")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ClientIndicatorRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientIndicatorRecord)
                .set(clientConnectionId).equalTo(record::getClientConnectionId)
                .set(type).equalTo(record::getType)
                .set(value).equalTo(record::getValue);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.081+02:00", comments="Source Table: client_indicator")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ClientIndicatorRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientIndicatorRecord)
                .set(clientConnectionId).equalToWhenPresent(record::getClientConnectionId)
                .set(type).equalToWhenPresent(record::getType)
                .set(value).equalToWhenPresent(record::getValue);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.081+02:00", comments="Source Table: client_indicator")
    default int updateByPrimaryKey(ClientIndicatorRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientIndicatorRecord)
                .set(clientConnectionId).equalTo(record::getClientConnectionId)
                .set(type).equalTo(record::getType)
                .set(value).equalTo(record::getValue)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.082+02:00", comments="Source Table: client_indicator")
    default int updateByPrimaryKeySelective(ClientIndicatorRecord record) {
        return UpdateDSL.updateWithMapper(this::update, clientIndicatorRecord)
                .set(clientConnectionId).equalToWhenPresent(record::getClientConnectionId)
                .set(type).equalToWhenPresent(record::getType)
                .set(value).equalToWhenPresent(record::getValue)
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
                        .from(clientIndicatorRecord);
    }
}