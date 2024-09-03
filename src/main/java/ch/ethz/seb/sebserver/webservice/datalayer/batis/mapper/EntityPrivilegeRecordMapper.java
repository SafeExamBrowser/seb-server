package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.EntityPrivilegeRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.EntityPrivilegeRecord;
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
public interface EntityPrivilegeRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<EntityPrivilegeRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="entity_type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="entity_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="user_uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="privilege_type", javaType=Byte.class, jdbcType=JdbcType.TINYINT)
    })
    EntityPrivilegeRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="entity_type", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="entity_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="user_uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="privilege_type", javaType=Byte.class, jdbcType=JdbcType.TINYINT)
    })
    List<EntityPrivilegeRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(entityPrivilegeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, entityPrivilegeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, entityPrivilegeRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    default int insert(EntityPrivilegeRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(entityPrivilegeRecord)
                .map(entityType).toProperty("entityType")
                .map(entityId).toProperty("entityId")
                .map(userUuid).toProperty("userUuid")
                .map(privilegeType).toProperty("privilegeType")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    default int insertSelective(EntityPrivilegeRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(entityPrivilegeRecord)
                .map(entityType).toPropertyWhenPresent("entityType", record::getEntityType)
                .map(entityId).toPropertyWhenPresent("entityId", record::getEntityId)
                .map(userUuid).toPropertyWhenPresent("userUuid", record::getUserUuid)
                .map(privilegeType).toPropertyWhenPresent("privilegeType", record::getPrivilegeType)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<EntityPrivilegeRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, entityType, entityId, userUuid, privilegeType)
                .from(entityPrivilegeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<EntityPrivilegeRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, entityType, entityId, userUuid, privilegeType)
                .from(entityPrivilegeRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    default EntityPrivilegeRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, entityType, entityId, userUuid, privilegeType)
                .from(entityPrivilegeRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(EntityPrivilegeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, entityPrivilegeRecord)
                .set(entityType).equalTo(record::getEntityType)
                .set(entityId).equalTo(record::getEntityId)
                .set(userUuid).equalTo(record::getUserUuid)
                .set(privilegeType).equalTo(record::getPrivilegeType);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(EntityPrivilegeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, entityPrivilegeRecord)
                .set(entityType).equalToWhenPresent(record::getEntityType)
                .set(entityId).equalToWhenPresent(record::getEntityId)
                .set(userUuid).equalToWhenPresent(record::getUserUuid)
                .set(privilegeType).equalToWhenPresent(record::getPrivilegeType);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.043+02:00", comments="Source Table: entity_privilege")
    default int updateByPrimaryKey(EntityPrivilegeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, entityPrivilegeRecord)
                .set(entityType).equalTo(record::getEntityType)
                .set(entityId).equalTo(record::getEntityId)
                .set(userUuid).equalTo(record::getUserUuid)
                .set(privilegeType).equalTo(record::getPrivilegeType)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.044+02:00", comments="Source Table: entity_privilege")
    default int updateByPrimaryKeySelective(EntityPrivilegeRecord record) {
        return UpdateDSL.updateWithMapper(this::update, entityPrivilegeRecord)
                .set(entityType).equalToWhenPresent(record::getEntityType)
                .set(entityId).equalToWhenPresent(record::getEntityId)
                .set(userUuid).equalToWhenPresent(record::getUserUuid)
                .set(privilegeType).equalToWhenPresent(record::getPrivilegeType)
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
                        .from(entityPrivilegeRecord);
    }
}