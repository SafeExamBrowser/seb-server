package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SebClientConfigRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.JodaTimeTypeResolver;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.SebClientConfigRecord;
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
import org.joda.time.DateTime;
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
public interface SebClientConfigRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.044+02:00", comments="Source Table: seb_client_configuration")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.044+02:00", comments="Source Table: seb_client_configuration")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.044+02:00", comments="Source Table: seb_client_configuration")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<SebClientConfigRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.044+02:00", comments="Source Table: seb_client_configuration")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="date", javaType=DateTime.class, typeHandler=JodaTimeTypeResolver.class, jdbcType=JdbcType.TIMESTAMP),
        @Arg(column="client_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="client_secret", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="encrypt_secret", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    SebClientConfigRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.044+02:00", comments="Source Table: seb_client_configuration")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="date", javaType=DateTime.class, typeHandler=JodaTimeTypeResolver.class, jdbcType=JdbcType.TIMESTAMP),
        @Arg(column="client_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="client_secret", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="encrypt_secret", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    List<SebClientConfigRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.044+02:00", comments="Source Table: seb_client_configuration")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.044+02:00", comments="Source Table: seb_client_configuration")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(sebClientConfigRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.045+02:00", comments="Source Table: seb_client_configuration")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, sebClientConfigRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.045+02:00", comments="Source Table: seb_client_configuration")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, sebClientConfigRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.045+02:00", comments="Source Table: seb_client_configuration")
    default int insert(SebClientConfigRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(sebClientConfigRecord)
                .map(institutionId).toProperty("institutionId")
                .map(name).toProperty("name")
                .map(date).toProperty("date")
                .map(clientName).toProperty("clientName")
                .map(clientSecret).toProperty("clientSecret")
                .map(encryptSecret).toProperty("encryptSecret")
                .map(active).toProperty("active")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.045+02:00", comments="Source Table: seb_client_configuration")
    default int insertSelective(SebClientConfigRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(sebClientConfigRecord)
                .map(institutionId).toPropertyWhenPresent("institutionId", record::getInstitutionId)
                .map(name).toPropertyWhenPresent("name", record::getName)
                .map(date).toPropertyWhenPresent("date", record::getDate)
                .map(clientName).toPropertyWhenPresent("clientName", record::getClientName)
                .map(clientSecret).toPropertyWhenPresent("clientSecret", record::getClientSecret)
                .map(encryptSecret).toPropertyWhenPresent("encryptSecret", record::getEncryptSecret)
                .map(active).toPropertyWhenPresent("active", record::getActive)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.045+02:00", comments="Source Table: seb_client_configuration")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<SebClientConfigRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, institutionId, name, date, clientName, clientSecret, encryptSecret, active)
                .from(sebClientConfigRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.045+02:00", comments="Source Table: seb_client_configuration")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<SebClientConfigRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, institutionId, name, date, clientName, clientSecret, encryptSecret, active)
                .from(sebClientConfigRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.045+02:00", comments="Source Table: seb_client_configuration")
    default SebClientConfigRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, institutionId, name, date, clientName, clientSecret, encryptSecret, active)
                .from(sebClientConfigRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.045+02:00", comments="Source Table: seb_client_configuration")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(SebClientConfigRecord record) {
        return UpdateDSL.updateWithMapper(this::update, sebClientConfigRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(name).equalTo(record::getName)
                .set(date).equalTo(record::getDate)
                .set(clientName).equalTo(record::getClientName)
                .set(clientSecret).equalTo(record::getClientSecret)
                .set(encryptSecret).equalTo(record::getEncryptSecret)
                .set(active).equalTo(record::getActive);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.045+02:00", comments="Source Table: seb_client_configuration")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(SebClientConfigRecord record) {
        return UpdateDSL.updateWithMapper(this::update, sebClientConfigRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(name).equalToWhenPresent(record::getName)
                .set(date).equalToWhenPresent(record::getDate)
                .set(clientName).equalToWhenPresent(record::getClientName)
                .set(clientSecret).equalToWhenPresent(record::getClientSecret)
                .set(encryptSecret).equalToWhenPresent(record::getEncryptSecret)
                .set(active).equalToWhenPresent(record::getActive);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.045+02:00", comments="Source Table: seb_client_configuration")
    default int updateByPrimaryKey(SebClientConfigRecord record) {
        return UpdateDSL.updateWithMapper(this::update, sebClientConfigRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(name).equalTo(record::getName)
                .set(date).equalTo(record::getDate)
                .set(clientName).equalTo(record::getClientName)
                .set(clientSecret).equalTo(record::getClientSecret)
                .set(encryptSecret).equalTo(record::getEncryptSecret)
                .set(active).equalTo(record::getActive)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.045+02:00", comments="Source Table: seb_client_configuration")
    default int updateByPrimaryKeySelective(SebClientConfigRecord record) {
        return UpdateDSL.updateWithMapper(this::update, sebClientConfigRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(name).equalToWhenPresent(record::getName)
                .set(date).equalToWhenPresent(record::getDate)
                .set(clientName).equalToWhenPresent(record::getClientName)
                .set(clientSecret).equalToWhenPresent(record::getClientSecret)
                .set(encryptSecret).equalToWhenPresent(record::getEncryptSecret)
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
                        .from(sebClientConfigRecord);
    }
}