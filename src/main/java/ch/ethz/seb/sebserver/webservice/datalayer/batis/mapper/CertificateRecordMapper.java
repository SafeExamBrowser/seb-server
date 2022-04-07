package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.CertificateRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.CertificateRecord;
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
public interface CertificateRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.070+02:00", comments="Source Table: certificate")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<CertificateRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="aliases", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="cert_store", javaType=byte[].class, jdbcType=JdbcType.BLOB)
    })
    CertificateRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="aliases", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="cert_store", javaType=byte[].class, jdbcType=JdbcType.BLOB)
    })
    List<CertificateRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(certificateRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, certificateRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, certificateRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default int insert(CertificateRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(certificateRecord)
                .map(institutionId).toProperty("institutionId")
                .map(aliases).toProperty("aliases")
                .map(certStore).toProperty("certStore")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default int insertSelective(CertificateRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(certificateRecord)
                .map(institutionId).toPropertyWhenPresent("institutionId", record::getInstitutionId)
                .map(aliases).toPropertyWhenPresent("aliases", record::getAliases)
                .map(certStore).toPropertyWhenPresent("certStore", record::getCertStore)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<CertificateRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, institutionId, aliases, certStore)
                .from(certificateRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<CertificateRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, institutionId, aliases, certStore)
                .from(certificateRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default CertificateRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, institutionId, aliases, certStore)
                .from(certificateRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(CertificateRecord record) {
        return UpdateDSL.updateWithMapper(this::update, certificateRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(aliases).equalTo(record::getAliases)
                .set(certStore).equalTo(record::getCertStore);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(CertificateRecord record) {
        return UpdateDSL.updateWithMapper(this::update, certificateRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(aliases).equalToWhenPresent(record::getAliases)
                .set(certStore).equalToWhenPresent(record::getCertStore);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default int updateByPrimaryKey(CertificateRecord record) {
        return UpdateDSL.updateWithMapper(this::update, certificateRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(aliases).equalTo(record::getAliases)
                .set(certStore).equalTo(record::getCertStore)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.071+02:00", comments="Source Table: certificate")
    default int updateByPrimaryKeySelective(CertificateRecord record) {
        return UpdateDSL.updateWithMapper(this::update, certificateRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(aliases).equalToWhenPresent(record::getAliases)
                .set(certStore).equalToWhenPresent(record::getCertStore)
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
                        .from(certificateRecord);
    }
}