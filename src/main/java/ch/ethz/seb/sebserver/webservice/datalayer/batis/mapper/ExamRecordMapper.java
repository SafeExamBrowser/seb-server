package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ExamRecord;
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
public interface ExamRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.134+01:00", comments="Source Table: exam")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ExamRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="lms_setup_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="external_id", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="owner", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="supporter", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="quit_password", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="browser_keys", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="status", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_seb_restriction", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="updating", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="lastupdate", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="exam_template_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="last_modified", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    ExamRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="lms_setup_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="external_id", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="owner", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="supporter", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="quit_password", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="browser_keys", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="status", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="lms_seb_restriction", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="updating", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="lastupdate", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="exam_template_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="last_modified", javaType=Long.class, jdbcType=JdbcType.BIGINT)
    })
    List<ExamRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(examRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, examRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, examRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    default int insert(ExamRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(examRecord)
                .map(institutionId).toProperty("institutionId")
                .map(lmsSetupId).toProperty("lmsSetupId")
                .map(externalId).toProperty("externalId")
                .map(owner).toProperty("owner")
                .map(supporter).toProperty("supporter")
                .map(type).toProperty("type")
                .map(quitPassword).toProperty("quitPassword")
                .map(browserKeys).toProperty("browserKeys")
                .map(status).toProperty("status")
                .map(lmsSebRestriction).toProperty("lmsSebRestriction")
                .map(updating).toProperty("updating")
                .map(lastupdate).toProperty("lastupdate")
                .map(active).toProperty("active")
                .map(examTemplateId).toProperty("examTemplateId")
                .map(lastModified).toProperty("lastModified")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    default int insertSelective(ExamRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(examRecord)
                .map(institutionId).toPropertyWhenPresent("institutionId", record::getInstitutionId)
                .map(lmsSetupId).toPropertyWhenPresent("lmsSetupId", record::getLmsSetupId)
                .map(externalId).toPropertyWhenPresent("externalId", record::getExternalId)
                .map(owner).toPropertyWhenPresent("owner", record::getOwner)
                .map(supporter).toPropertyWhenPresent("supporter", record::getSupporter)
                .map(type).toPropertyWhenPresent("type", record::getType)
                .map(quitPassword).toPropertyWhenPresent("quitPassword", record::getQuitPassword)
                .map(browserKeys).toPropertyWhenPresent("browserKeys", record::getBrowserKeys)
                .map(status).toPropertyWhenPresent("status", record::getStatus)
                .map(lmsSebRestriction).toPropertyWhenPresent("lmsSebRestriction", record::getLmsSebRestriction)
                .map(updating).toPropertyWhenPresent("updating", record::getUpdating)
                .map(lastupdate).toPropertyWhenPresent("lastupdate", record::getLastupdate)
                .map(active).toPropertyWhenPresent("active", record::getActive)
                .map(examTemplateId).toPropertyWhenPresent("examTemplateId", record::getExamTemplateId)
                .map(lastModified).toPropertyWhenPresent("lastModified", record::getLastModified)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ExamRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, institutionId, lmsSetupId, externalId, owner, supporter, type, quitPassword, browserKeys, status, lmsSebRestriction, updating, lastupdate, active, examTemplateId, lastModified)
                .from(examRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ExamRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, institutionId, lmsSetupId, externalId, owner, supporter, type, quitPassword, browserKeys, status, lmsSebRestriction, updating, lastupdate, active, examTemplateId, lastModified)
                .from(examRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    default ExamRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, institutionId, lmsSetupId, externalId, owner, supporter, type, quitPassword, browserKeys, status, lmsSebRestriction, updating, lastupdate, active, examTemplateId, lastModified)
                .from(examRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ExamRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(lmsSetupId).equalTo(record::getLmsSetupId)
                .set(externalId).equalTo(record::getExternalId)
                .set(owner).equalTo(record::getOwner)
                .set(supporter).equalTo(record::getSupporter)
                .set(type).equalTo(record::getType)
                .set(quitPassword).equalTo(record::getQuitPassword)
                .set(browserKeys).equalTo(record::getBrowserKeys)
                .set(status).equalTo(record::getStatus)
                .set(lmsSebRestriction).equalTo(record::getLmsSebRestriction)
                .set(updating).equalTo(record::getUpdating)
                .set(lastupdate).equalTo(record::getLastupdate)
                .set(active).equalTo(record::getActive)
                .set(examTemplateId).equalTo(record::getExamTemplateId)
                .set(lastModified).equalTo(record::getLastModified);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.135+01:00", comments="Source Table: exam")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ExamRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(lmsSetupId).equalToWhenPresent(record::getLmsSetupId)
                .set(externalId).equalToWhenPresent(record::getExternalId)
                .set(owner).equalToWhenPresent(record::getOwner)
                .set(supporter).equalToWhenPresent(record::getSupporter)
                .set(type).equalToWhenPresent(record::getType)
                .set(quitPassword).equalToWhenPresent(record::getQuitPassword)
                .set(browserKeys).equalToWhenPresent(record::getBrowserKeys)
                .set(status).equalToWhenPresent(record::getStatus)
                .set(lmsSebRestriction).equalToWhenPresent(record::getLmsSebRestriction)
                .set(updating).equalToWhenPresent(record::getUpdating)
                .set(lastupdate).equalToWhenPresent(record::getLastupdate)
                .set(active).equalToWhenPresent(record::getActive)
                .set(examTemplateId).equalToWhenPresent(record::getExamTemplateId)
                .set(lastModified).equalToWhenPresent(record::getLastModified);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.136+01:00", comments="Source Table: exam")
    default int updateByPrimaryKey(ExamRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(lmsSetupId).equalTo(record::getLmsSetupId)
                .set(externalId).equalTo(record::getExternalId)
                .set(owner).equalTo(record::getOwner)
                .set(supporter).equalTo(record::getSupporter)
                .set(type).equalTo(record::getType)
                .set(quitPassword).equalTo(record::getQuitPassword)
                .set(browserKeys).equalTo(record::getBrowserKeys)
                .set(status).equalTo(record::getStatus)
                .set(lmsSebRestriction).equalTo(record::getLmsSebRestriction)
                .set(updating).equalTo(record::getUpdating)
                .set(lastupdate).equalTo(record::getLastupdate)
                .set(active).equalTo(record::getActive)
                .set(examTemplateId).equalTo(record::getExamTemplateId)
                .set(lastModified).equalTo(record::getLastModified)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.136+01:00", comments="Source Table: exam")
    default int updateByPrimaryKeySelective(ExamRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(lmsSetupId).equalToWhenPresent(record::getLmsSetupId)
                .set(externalId).equalToWhenPresent(record::getExternalId)
                .set(owner).equalToWhenPresent(record::getOwner)
                .set(supporter).equalToWhenPresent(record::getSupporter)
                .set(type).equalToWhenPresent(record::getType)
                .set(quitPassword).equalToWhenPresent(record::getQuitPassword)
                .set(browserKeys).equalToWhenPresent(record::getBrowserKeys)
                .set(status).equalToWhenPresent(record::getStatus)
                .set(lmsSebRestriction).equalToWhenPresent(record::getLmsSebRestriction)
                .set(updating).equalToWhenPresent(record::getUpdating)
                .set(lastupdate).equalToWhenPresent(record::getLastupdate)
                .set(active).equalToWhenPresent(record::getActive)
                .set(examTemplateId).equalToWhenPresent(record::getExamTemplateId)
                .set(lastModified).equalToWhenPresent(record::getLastModified)
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
                        .from(examRecord);
    }
}