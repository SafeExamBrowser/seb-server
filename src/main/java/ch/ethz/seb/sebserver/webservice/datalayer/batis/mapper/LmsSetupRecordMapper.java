package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.LmsSetupRecord;
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
public interface LmsSetupRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source Table: lms_setup")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source Table: lms_setup")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source Table: lms_setup")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<LmsSetupRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source Table: lms_setup")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_url", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_clientname", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_clientsecret", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_rest_api_token", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_proxy_host", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_proxy_port", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="lms_proxy_auth_username", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_proxy_auth_secret", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="update_time", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    LmsSetupRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source Table: lms_setup")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_url", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_clientname", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_clientsecret", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_rest_api_token", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_proxy_host", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_proxy_port", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="lms_proxy_auth_username", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_proxy_auth_secret", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="update_time", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    List<LmsSetupRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source Table: lms_setup")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source Table: lms_setup")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(lmsSetupRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source Table: lms_setup")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, lmsSetupRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source Table: lms_setup")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, lmsSetupRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source Table: lms_setup")
    default int insert(LmsSetupRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(lmsSetupRecord)
                .map(institutionId).toProperty("institutionId")
                .map(name).toProperty("name")
                .map(lmsType).toProperty("lmsType")
                .map(lmsUrl).toProperty("lmsUrl")
                .map(lmsClientname).toProperty("lmsClientname")
                .map(lmsClientsecret).toProperty("lmsClientsecret")
                .map(lmsRestApiToken).toProperty("lmsRestApiToken")
                .map(lmsProxyHost).toProperty("lmsProxyHost")
                .map(lmsProxyPort).toProperty("lmsProxyPort")
                .map(lmsProxyAuthUsername).toProperty("lmsProxyAuthUsername")
                .map(lmsProxyAuthSecret).toProperty("lmsProxyAuthSecret")
                .map(updateTime).toProperty("updateTime")
                .map(active).toProperty("active")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.186+01:00", comments="Source Table: lms_setup")
    default int insertSelective(LmsSetupRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(lmsSetupRecord)
                .map(institutionId).toPropertyWhenPresent("institutionId", record::getInstitutionId)
                .map(name).toPropertyWhenPresent("name", record::getName)
                .map(lmsType).toPropertyWhenPresent("lmsType", record::getLmsType)
                .map(lmsUrl).toPropertyWhenPresent("lmsUrl", record::getLmsUrl)
                .map(lmsClientname).toPropertyWhenPresent("lmsClientname", record::getLmsClientname)
                .map(lmsClientsecret).toPropertyWhenPresent("lmsClientsecret", record::getLmsClientsecret)
                .map(lmsRestApiToken).toPropertyWhenPresent("lmsRestApiToken", record::getLmsRestApiToken)
                .map(lmsProxyHost).toPropertyWhenPresent("lmsProxyHost", record::getLmsProxyHost)
                .map(lmsProxyPort).toPropertyWhenPresent("lmsProxyPort", record::getLmsProxyPort)
                .map(lmsProxyAuthUsername).toPropertyWhenPresent("lmsProxyAuthUsername", record::getLmsProxyAuthUsername)
                .map(lmsProxyAuthSecret).toPropertyWhenPresent("lmsProxyAuthSecret", record::getLmsProxyAuthSecret)
                .map(updateTime).toPropertyWhenPresent("updateTime", record::getUpdateTime)
                .map(active).toPropertyWhenPresent("active", record::getActive)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.186+01:00", comments="Source Table: lms_setup")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<LmsSetupRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, institutionId, name, lmsType, lmsUrl, lmsClientname, lmsClientsecret, lmsRestApiToken, lmsProxyHost, lmsProxyPort, lmsProxyAuthUsername, lmsProxyAuthSecret, updateTime, active)
                .from(lmsSetupRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.186+01:00", comments="Source Table: lms_setup")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<LmsSetupRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, institutionId, name, lmsType, lmsUrl, lmsClientname, lmsClientsecret, lmsRestApiToken, lmsProxyHost, lmsProxyPort, lmsProxyAuthUsername, lmsProxyAuthSecret, updateTime, active)
                .from(lmsSetupRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.186+01:00", comments="Source Table: lms_setup")
    default LmsSetupRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, institutionId, name, lmsType, lmsUrl, lmsClientname, lmsClientsecret, lmsRestApiToken, lmsProxyHost, lmsProxyPort, lmsProxyAuthUsername, lmsProxyAuthSecret, updateTime, active)
                .from(lmsSetupRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.186+01:00", comments="Source Table: lms_setup")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(LmsSetupRecord record) {
        return UpdateDSL.updateWithMapper(this::update, lmsSetupRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(name).equalTo(record::getName)
                .set(lmsType).equalTo(record::getLmsType)
                .set(lmsUrl).equalTo(record::getLmsUrl)
                .set(lmsClientname).equalTo(record::getLmsClientname)
                .set(lmsClientsecret).equalTo(record::getLmsClientsecret)
                .set(lmsRestApiToken).equalTo(record::getLmsRestApiToken)
                .set(lmsProxyHost).equalTo(record::getLmsProxyHost)
                .set(lmsProxyPort).equalTo(record::getLmsProxyPort)
                .set(lmsProxyAuthUsername).equalTo(record::getLmsProxyAuthUsername)
                .set(lmsProxyAuthSecret).equalTo(record::getLmsProxyAuthSecret)
                .set(updateTime).equalTo(record::getUpdateTime)
                .set(active).equalTo(record::getActive);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.186+01:00", comments="Source Table: lms_setup")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(LmsSetupRecord record) {
        return UpdateDSL.updateWithMapper(this::update, lmsSetupRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(name).equalToWhenPresent(record::getName)
                .set(lmsType).equalToWhenPresent(record::getLmsType)
                .set(lmsUrl).equalToWhenPresent(record::getLmsUrl)
                .set(lmsClientname).equalToWhenPresent(record::getLmsClientname)
                .set(lmsClientsecret).equalToWhenPresent(record::getLmsClientsecret)
                .set(lmsRestApiToken).equalToWhenPresent(record::getLmsRestApiToken)
                .set(lmsProxyHost).equalToWhenPresent(record::getLmsProxyHost)
                .set(lmsProxyPort).equalToWhenPresent(record::getLmsProxyPort)
                .set(lmsProxyAuthUsername).equalToWhenPresent(record::getLmsProxyAuthUsername)
                .set(lmsProxyAuthSecret).equalToWhenPresent(record::getLmsProxyAuthSecret)
                .set(updateTime).equalToWhenPresent(record::getUpdateTime)
                .set(active).equalToWhenPresent(record::getActive);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.186+01:00", comments="Source Table: lms_setup")
    default int updateByPrimaryKey(LmsSetupRecord record) {
        return UpdateDSL.updateWithMapper(this::update, lmsSetupRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(name).equalTo(record::getName)
                .set(lmsType).equalTo(record::getLmsType)
                .set(lmsUrl).equalTo(record::getLmsUrl)
                .set(lmsClientname).equalTo(record::getLmsClientname)
                .set(lmsClientsecret).equalTo(record::getLmsClientsecret)
                .set(lmsRestApiToken).equalTo(record::getLmsRestApiToken)
                .set(lmsProxyHost).equalTo(record::getLmsProxyHost)
                .set(lmsProxyPort).equalTo(record::getLmsProxyPort)
                .set(lmsProxyAuthUsername).equalTo(record::getLmsProxyAuthUsername)
                .set(lmsProxyAuthSecret).equalTo(record::getLmsProxyAuthSecret)
                .set(updateTime).equalTo(record::getUpdateTime)
                .set(active).equalTo(record::getActive)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.186+01:00", comments="Source Table: lms_setup")
    default int updateByPrimaryKeySelective(LmsSetupRecord record) {
        return UpdateDSL.updateWithMapper(this::update, lmsSetupRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(name).equalToWhenPresent(record::getName)
                .set(lmsType).equalToWhenPresent(record::getLmsType)
                .set(lmsUrl).equalToWhenPresent(record::getLmsUrl)
                .set(lmsClientname).equalToWhenPresent(record::getLmsClientname)
                .set(lmsClientsecret).equalToWhenPresent(record::getLmsClientsecret)
                .set(lmsRestApiToken).equalToWhenPresent(record::getLmsRestApiToken)
                .set(lmsProxyHost).equalToWhenPresent(record::getLmsProxyHost)
                .set(lmsProxyPort).equalToWhenPresent(record::getLmsProxyPort)
                .set(lmsProxyAuthUsername).equalToWhenPresent(record::getLmsProxyAuthUsername)
                .set(lmsProxyAuthSecret).equalToWhenPresent(record::getLmsProxyAuthSecret)
                .set(updateTime).equalToWhenPresent(record::getUpdateTime)
                .set(active).equalToWhenPresent(record::getActive)
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
                        .from(lmsSetupRecord);
    }
}