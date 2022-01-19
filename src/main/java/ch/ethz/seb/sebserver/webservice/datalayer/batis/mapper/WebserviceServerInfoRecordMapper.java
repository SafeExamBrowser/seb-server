package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.WebserviceServerInfoRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.WebserviceServerInfoRecord;
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
public interface WebserviceServerInfoRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.208+01:00", comments="Source Table: webservice_server_info")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.209+01:00", comments="Source Table: webservice_server_info")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.209+01:00", comments="Source Table: webservice_server_info")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<WebserviceServerInfoRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.209+01:00", comments="Source Table: webservice_server_info")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="service_address", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="master", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="update_time", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    WebserviceServerInfoRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.209+01:00", comments="Source Table: webservice_server_info")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="service_address", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="master", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="update_time", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    List<WebserviceServerInfoRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.210+01:00", comments="Source Table: webservice_server_info")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.210+01:00", comments="Source Table: webservice_server_info")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(webserviceServerInfoRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.210+01:00", comments="Source Table: webservice_server_info")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, webserviceServerInfoRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.210+01:00", comments="Source Table: webservice_server_info")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, webserviceServerInfoRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.210+01:00", comments="Source Table: webservice_server_info")
    default int insert(WebserviceServerInfoRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(webserviceServerInfoRecord)
                .map(uuid).toProperty("uuid")
                .map(serviceAddress).toProperty("serviceAddress")
                .map(master).toProperty("master")
                .map(updateTime).toProperty("updateTime")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.210+01:00", comments="Source Table: webservice_server_info")
    default int insertSelective(WebserviceServerInfoRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(webserviceServerInfoRecord)
                .map(uuid).toPropertyWhenPresent("uuid", record::getUuid)
                .map(serviceAddress).toPropertyWhenPresent("serviceAddress", record::getServiceAddress)
                .map(master).toPropertyWhenPresent("master", record::getMaster)
                .map(updateTime).toPropertyWhenPresent("updateTime", record::getUpdateTime)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.211+01:00", comments="Source Table: webservice_server_info")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<WebserviceServerInfoRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, uuid, serviceAddress, master, updateTime)
                .from(webserviceServerInfoRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.211+01:00", comments="Source Table: webservice_server_info")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<WebserviceServerInfoRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, uuid, serviceAddress, master, updateTime)
                .from(webserviceServerInfoRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.211+01:00", comments="Source Table: webservice_server_info")
    default WebserviceServerInfoRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, uuid, serviceAddress, master, updateTime)
                .from(webserviceServerInfoRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.211+01:00", comments="Source Table: webservice_server_info")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(WebserviceServerInfoRecord record) {
        return UpdateDSL.updateWithMapper(this::update, webserviceServerInfoRecord)
                .set(uuid).equalTo(record::getUuid)
                .set(serviceAddress).equalTo(record::getServiceAddress)
                .set(master).equalTo(record::getMaster)
                .set(updateTime).equalTo(record::getUpdateTime);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.211+01:00", comments="Source Table: webservice_server_info")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(WebserviceServerInfoRecord record) {
        return UpdateDSL.updateWithMapper(this::update, webserviceServerInfoRecord)
                .set(uuid).equalToWhenPresent(record::getUuid)
                .set(serviceAddress).equalToWhenPresent(record::getServiceAddress)
                .set(master).equalToWhenPresent(record::getMaster)
                .set(updateTime).equalToWhenPresent(record::getUpdateTime);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.211+01:00", comments="Source Table: webservice_server_info")
    default int updateByPrimaryKey(WebserviceServerInfoRecord record) {
        return UpdateDSL.updateWithMapper(this::update, webserviceServerInfoRecord)
                .set(uuid).equalTo(record::getUuid)
                .set(serviceAddress).equalTo(record::getServiceAddress)
                .set(master).equalTo(record::getMaster)
                .set(updateTime).equalTo(record::getUpdateTime)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.211+01:00", comments="Source Table: webservice_server_info")
    default int updateByPrimaryKeySelective(WebserviceServerInfoRecord record) {
        return UpdateDSL.updateWithMapper(this::update, webserviceServerInfoRecord)
                .set(uuid).equalToWhenPresent(record::getUuid)
                .set(serviceAddress).equalToWhenPresent(record::getServiceAddress)
                .set(master).equalToWhenPresent(record::getMaster)
                .set(updateTime).equalToWhenPresent(record::getUpdateTime)
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
                        .from(webserviceServerInfoRecord);
    }
}