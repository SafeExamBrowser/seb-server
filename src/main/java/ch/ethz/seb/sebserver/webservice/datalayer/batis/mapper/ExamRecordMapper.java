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
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.444+01:00", comments="Source Table: exam")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.444+01:00", comments="Source Table: exam")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.444+01:00", comments="Source Table: exam")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<ExamRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.444+01:00", comments="Source Table: exam")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="lms_setup_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="external_uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="owner", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="supporter", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    ExamRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.445+01:00", comments="Source Table: exam")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="lms_setup_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="external_uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="owner", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="supporter", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER)
    })
    List<ExamRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.445+01:00", comments="Source Table: exam")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.445+01:00", comments="Source Table: exam")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(examRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.445+01:00", comments="Source Table: exam")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, examRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.445+01:00", comments="Source Table: exam")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, examRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.445+01:00", comments="Source Table: exam")
    default int insert(ExamRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(examRecord)
                .map(lmsSetupId).toProperty("lmsSetupId")
                .map(externalUuid).toProperty("externalUuid")
                .map(owner).toProperty("owner")
                .map(supporter).toProperty("supporter")
                .map(type).toProperty("type")
                .map(active).toProperty("active")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.445+01:00", comments="Source Table: exam")
    default int insertSelective(ExamRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(examRecord)
                .map(lmsSetupId).toPropertyWhenPresent("lmsSetupId", record::getLmsSetupId)
                .map(externalUuid).toPropertyWhenPresent("externalUuid", record::getExternalUuid)
                .map(owner).toPropertyWhenPresent("owner", record::getOwner)
                .map(supporter).toPropertyWhenPresent("supporter", record::getSupporter)
                .map(type).toPropertyWhenPresent("type", record::getType)
                .map(active).toPropertyWhenPresent("active", record::getActive)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.446+01:00", comments="Source Table: exam")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ExamRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, lmsSetupId, externalUuid, owner, supporter, type, active)
                .from(examRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.446+01:00", comments="Source Table: exam")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<ExamRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, lmsSetupId, externalUuid, owner, supporter, type, active)
                .from(examRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.446+01:00", comments="Source Table: exam")
    default ExamRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, lmsSetupId, externalUuid, owner, supporter, type, active)
                .from(examRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.446+01:00", comments="Source Table: exam")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(ExamRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examRecord)
                .set(lmsSetupId).equalTo(record::getLmsSetupId)
                .set(externalUuid).equalTo(record::getExternalUuid)
                .set(owner).equalTo(record::getOwner)
                .set(supporter).equalTo(record::getSupporter)
                .set(type).equalTo(record::getType)
                .set(active).equalTo(record::getActive);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.446+01:00", comments="Source Table: exam")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(ExamRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examRecord)
                .set(lmsSetupId).equalToWhenPresent(record::getLmsSetupId)
                .set(externalUuid).equalToWhenPresent(record::getExternalUuid)
                .set(owner).equalToWhenPresent(record::getOwner)
                .set(supporter).equalToWhenPresent(record::getSupporter)
                .set(type).equalToWhenPresent(record::getType)
                .set(active).equalToWhenPresent(record::getActive);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.446+01:00", comments="Source Table: exam")
    default int updateByPrimaryKey(ExamRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examRecord)
                .set(lmsSetupId).equalTo(record::getLmsSetupId)
                .set(externalUuid).equalTo(record::getExternalUuid)
                .set(owner).equalTo(record::getOwner)
                .set(supporter).equalTo(record::getSupporter)
                .set(type).equalTo(record::getType)
                .set(active).equalTo(record::getActive)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.446+01:00", comments="Source Table: exam")
    default int updateByPrimaryKeySelective(ExamRecord record) {
        return UpdateDSL.updateWithMapper(this::update, examRecord)
                .set(lmsSetupId).equalToWhenPresent(record::getLmsSetupId)
                .set(externalUuid).equalToWhenPresent(record::getExternalUuid)
                .set(owner).equalToWhenPresent(record::getOwner)
                .set(supporter).equalToWhenPresent(record::getSupporter)
                .set(type).equalToWhenPresent(record::getType)
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
                        .from(examRecord);
    }
}