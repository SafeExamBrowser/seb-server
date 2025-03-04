package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamConfigurationMapRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamConfigurationMapRecord;
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
public interface ExamConfigurationMapRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ExamConfigurationMapRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="configuration_node_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="encrypt_secret", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="client_group_id", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    ExamConfigurationMapRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="exam_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="configuration_node_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="encrypt_secret", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="client_group_id", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    List<ExamConfigurationMapRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(examConfigurationMapRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, examConfigurationMapRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, examConfigurationMapRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default int insert(ExamConfigurationMapRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(examConfigurationMapRecord)
                .map(institutionId).toProperty("institutionId")
                .map(examId).toProperty("examId")
                .map(configurationNodeId).toProperty("configurationNodeId")
                .map(encryptSecret).toProperty("encryptSecret")
                .map(clientGroupId).toProperty("clientGroupId")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default int insertSelective(ExamConfigurationMapRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(examConfigurationMapRecord)
                .map(institutionId).toPropertyWhenPresent("institutionId", record::getInstitutionId)
                .map(examId).toPropertyWhenPresent("examId", record::getExamId)
                .map(configurationNodeId).toPropertyWhenPresent("configurationNodeId", record::getConfigurationNodeId)
                .map(encryptSecret).toPropertyWhenPresent("encryptSecret", record::getEncryptSecret)
                .map(clientGroupId).toPropertyWhenPresent("clientGroupId", record::getClientGroupId)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ExamConfigurationMapRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, institutionId, examId, configurationNodeId, encryptSecret, clientGroupId)
                .from(examConfigurationMapRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ExamConfigurationMapRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, institutionId, examId, configurationNodeId, encryptSecret, clientGroupId)
                .from(examConfigurationMapRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default ExamConfigurationMapRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, institutionId, examId, configurationNodeId, encryptSecret, clientGroupId)
                .from(examConfigurationMapRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ExamConfigurationMapRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examConfigurationMapRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(examId).equalTo(record::getExamId)
                .set(configurationNodeId).equalTo(record::getConfigurationNodeId)
                .set(encryptSecret).equalTo(record::getEncryptSecret)
                .set(clientGroupId).equalTo(record::getClientGroupId);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ExamConfigurationMapRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examConfigurationMapRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(configurationNodeId).equalToWhenPresent(record::getConfigurationNodeId)
                .set(encryptSecret).equalToWhenPresent(record::getEncryptSecret)
                .set(clientGroupId).equalToWhenPresent(record::getClientGroupId);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default int updateByPrimaryKey(ExamConfigurationMapRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examConfigurationMapRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(examId).equalTo(record::getExamId)
                .set(configurationNodeId).equalTo(record::getConfigurationNodeId)
                .set(encryptSecret).equalTo(record::getEncryptSecret)
                .set(clientGroupId).equalTo(record::getClientGroupId)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.834+01:00", comments="Source Table: exam_configuration_map")
    default int updateByPrimaryKeySelective(ExamConfigurationMapRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examConfigurationMapRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(examId).equalToWhenPresent(record::getExamId)
                .set(configurationNodeId).equalToWhenPresent(record::getConfigurationNodeId)
                .set(encryptSecret).equalToWhenPresent(record::getEncryptSecret)
                .set(clientGroupId).equalToWhenPresent(record::getClientGroupId)
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
                        .from(examConfigurationMapRecord);
    }
}