package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SecurityKeyRegistryRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.SecurityKeyRegistryRecord;
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
public interface SecurityKeyRegistryRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<SecurityKeyRegistryRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="key", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="tag", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="exam_template_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="encryption_type", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    SecurityKeyRegistryRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="key", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="tag", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="exam_template_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="encryption_type", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<SecurityKeyRegistryRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(securityKeyRegistryRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, securityKeyRegistryRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, securityKeyRegistryRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default int insert(SecurityKeyRegistryRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(securityKeyRegistryRecord)
                .map(institutionId).toProperty("institutionId")
                .map(type).toProperty("type")
                .map(key).toProperty("key")
                .map(tag).toProperty("tag")
                .map(examId).toProperty("examId")
                .map(examTemplateId).toProperty("examTemplateId")
                .map(encryptionType).toProperty("encryptionType")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default int insertSelective(SecurityKeyRegistryRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(securityKeyRegistryRecord)
                .map(institutionId).toPropertyWhenPresent("institutionId", record::getInstitutionId)
                .map(type).toPropertyWhenPresent("type", record::getType)
                .map(key).toPropertyWhenPresent("key", record::getKey)
                .map(tag).toPropertyWhenPresent("tag", record::getTag)
                .map(examId).toPropertyWhenPresent("examId", record::getExamId)
                .map(examTemplateId).toPropertyWhenPresent("examTemplateId", record::getExamTemplateId)
                .map(encryptionType).toPropertyWhenPresent("encryptionType", record::getEncryptionType)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<SecurityKeyRegistryRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, institutionId, type, key, tag, examId, examTemplateId, encryptionType)
                .from(securityKeyRegistryRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<SecurityKeyRegistryRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, institutionId, type, key, tag, examId, examTemplateId, encryptionType)
                .from(securityKeyRegistryRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default SecurityKeyRegistryRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, institutionId, type, key, tag, examId, examTemplateId, encryptionType)
                .from(securityKeyRegistryRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(SecurityKeyRegistryRecord record) {
        return UpdateDSL.updateWithMapper(this::update, securityKeyRegistryRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(type).equalTo(record::getType)
                .set(key).equalTo(record::getKey)
                .set(tag).equalTo(record::getTag)
                .set(examId).equalTo(record::getExamId)
                .set(examTemplateId).equalTo(record::getExamTemplateId)
                .set(encryptionType).equalTo(record::getEncryptionType);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(SecurityKeyRegistryRecord record) {
        return UpdateDSL.updateWithMapper(this::update, securityKeyRegistryRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(type).equalToWhenPresent(record::getType)
                .set(key).equalToWhenPresent(record::getKey)
                .set(tag).equalToWhenPresent(record::getTag)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(examTemplateId).equalToWhenPresent(record::getExamTemplateId)
                .set(encryptionType).equalToWhenPresent(record::getEncryptionType);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default int updateByPrimaryKey(SecurityKeyRegistryRecord record) {
        return UpdateDSL.updateWithMapper(this::update, securityKeyRegistryRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(type).equalTo(record::getType)
                .set(key).equalTo(record::getKey)
                .set(tag).equalTo(record::getTag)
                .set(examId).equalTo(record::getExamId)
                .set(examTemplateId).equalTo(record::getExamTemplateId)
                .set(encryptionType).equalTo(record::getEncryptionType)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-08T16:12:34.722+01:00", comments="Source Table: seb_security_key_registry")
    default int updateByPrimaryKeySelective(SecurityKeyRegistryRecord record) {
        return UpdateDSL.updateWithMapper(this::update, securityKeyRegistryRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(type).equalToWhenPresent(record::getType)
                .set(key).equalToWhenPresent(record::getKey)
                .set(tag).equalToWhenPresent(record::getTag)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(examTemplateId).equalToWhenPresent(record::getExamTemplateId)
                .set(encryptionType).equalToWhenPresent(record::getEncryptionType)
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
                        .from(securityKeyRegistryRecord);
    }
}