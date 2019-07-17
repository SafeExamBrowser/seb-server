package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.WebserviceServerInfoDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.WebserviceServerInfo;
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
public interface WebserviceServerInfoMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source Table: webservice_server_info")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source Table: webservice_server_info")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source Table: webservice_server_info")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<WebserviceServerInfo> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source Table: webservice_server_info")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="service_address", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    WebserviceServerInfo selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source Table: webservice_server_info")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="service_address", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<WebserviceServerInfo> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source Table: webservice_server_info")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source Table: webservice_server_info")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(webserviceServerInfo);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source Table: webservice_server_info")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, webserviceServerInfo);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source Table: webservice_server_info")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, webserviceServerInfo)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.839+02:00", comments="Source Table: webservice_server_info")
    default int insert(WebserviceServerInfo record) {
        return insert(SqlBuilder.insert(record)
                .into(webserviceServerInfo)
                .map(uuid).toProperty("uuid")
                .map(serviceAddress).toProperty("serviceAddress")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.839+02:00", comments="Source Table: webservice_server_info")
    default int insertSelective(WebserviceServerInfo record) {
        return insert(SqlBuilder.insert(record)
                .into(webserviceServerInfo)
                .map(uuid).toPropertyWhenPresent("uuid", record::getUuid)
                .map(serviceAddress).toPropertyWhenPresent("serviceAddress", record::getServiceAddress)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.839+02:00", comments="Source Table: webservice_server_info")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<WebserviceServerInfo>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, uuid, serviceAddress)
                .from(webserviceServerInfo);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.839+02:00", comments="Source Table: webservice_server_info")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<WebserviceServerInfo>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, uuid, serviceAddress)
                .from(webserviceServerInfo);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.839+02:00", comments="Source Table: webservice_server_info")
    default WebserviceServerInfo selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, uuid, serviceAddress)
                .from(webserviceServerInfo)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.839+02:00", comments="Source Table: webservice_server_info")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(WebserviceServerInfo record) {
        return UpdateDSL.updateWithMapper(this::update, webserviceServerInfo)
                .set(uuid).equalTo(record::getUuid)
                .set(serviceAddress).equalTo(record::getServiceAddress);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.839+02:00", comments="Source Table: webservice_server_info")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(WebserviceServerInfo record) {
        return UpdateDSL.updateWithMapper(this::update, webserviceServerInfo)
                .set(uuid).equalToWhenPresent(record::getUuid)
                .set(serviceAddress).equalToWhenPresent(record::getServiceAddress);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.839+02:00", comments="Source Table: webservice_server_info")
    default int updateByPrimaryKey(WebserviceServerInfo record) {
        return UpdateDSL.updateWithMapper(this::update, webserviceServerInfo)
                .set(uuid).equalTo(record::getUuid)
                .set(serviceAddress).equalTo(record::getServiceAddress)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.839+02:00", comments="Source Table: webservice_server_info")
    default int updateByPrimaryKeySelective(WebserviceServerInfo record) {
        return UpdateDSL.updateWithMapper(this::update, webserviceServerInfo)
                .set(uuid).equalToWhenPresent(record::getUuid)
                .set(serviceAddress).equalToWhenPresent(record::getServiceAddress)
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
                        .from(webserviceServerInfo);
    }
}