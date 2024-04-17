package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.FeaturePrivilegeRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.FeaturePrivilegeRecord;
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
public interface FeaturePrivilegeRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<FeaturePrivilegeRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="feature_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="user_uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    FeaturePrivilegeRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="feature_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="user_uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<FeaturePrivilegeRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(featurePrivilegeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, featurePrivilegeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, featurePrivilegeRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    default int insert(FeaturePrivilegeRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(featurePrivilegeRecord)
                .map(featureId).toProperty("featureId")
                .map(userUuid).toProperty("userUuid")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    default int insertSelective(FeaturePrivilegeRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(featurePrivilegeRecord)
                .map(featureId).toPropertyWhenPresent("featureId", record::getFeatureId)
                .map(userUuid).toPropertyWhenPresent("userUuid", record::getUserUuid)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<FeaturePrivilegeRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, featureId, userUuid)
                .from(featurePrivilegeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<FeaturePrivilegeRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, featureId, userUuid)
                .from(featurePrivilegeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    default FeaturePrivilegeRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, featureId, userUuid)
                .from(featurePrivilegeRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(FeaturePrivilegeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, featurePrivilegeRecord)
                .set(featureId).equalTo(record::getFeatureId)
                .set(userUuid).equalTo(record::getUserUuid);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.141+02:00", comments="Source Table: feature_privilege")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(FeaturePrivilegeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, featurePrivilegeRecord)
                .set(featureId).equalToWhenPresent(record::getFeatureId)
                .set(userUuid).equalToWhenPresent(record::getUserUuid);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.142+02:00", comments="Source Table: feature_privilege")
    default int updateByPrimaryKey(FeaturePrivilegeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, featurePrivilegeRecord)
                .set(featureId).equalTo(record::getFeatureId)
                .set(userUuid).equalTo(record::getUserUuid)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-16T08:47:55.142+02:00", comments="Source Table: feature_privilege")
    default int updateByPrimaryKeySelective(FeaturePrivilegeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, featurePrivilegeRecord)
                .set(featureId).equalToWhenPresent(record::getFeatureId)
                .set(userUuid).equalToWhenPresent(record::getUserUuid)
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
                        .from(featurePrivilegeRecord);
    }
}