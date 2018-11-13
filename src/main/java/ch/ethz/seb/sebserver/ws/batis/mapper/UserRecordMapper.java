package ch.ethz.seb.sebserver.ws.batis.mapper;

import static ch.ethz.seb.sebserver.ws.batis.mapper.UserRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.ws.batis.JodaTimeTypeResolver;
import ch.ethz.seb.sebserver.ws.batis.model.UserRecord;
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
public interface UserRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.552+01:00", comments="Source Table: user")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.552+01:00", comments="Source Table: user")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.552+01:00", comments="Source Table: user")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<UserRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.552+01:00", comments="Source Table: user")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="user_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="password", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="email", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="creation_date", javaType=DateTime.class, typeHandler=JodaTimeTypeResolver.class, jdbcType=JdbcType.TIMESTAMP),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="locale", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="timezone", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    UserRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="institution_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="uuid", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="user_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="password", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="email", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="creation_date", javaType=DateTime.class, typeHandler=JodaTimeTypeResolver.class, jdbcType=JdbcType.TIMESTAMP),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="locale", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="timezone", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<UserRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(userRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, userRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, userRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default int insert(UserRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(userRecord)
                .map(institutionId).toProperty("institutionId")
                .map(uuid).toProperty("uuid")
                .map(name).toProperty("name")
                .map(userName).toProperty("userName")
                .map(password).toProperty("password")
                .map(email).toProperty("email")
                .map(creationDate).toProperty("creationDate")
                .map(active).toProperty("active")
                .map(locale).toProperty("locale")
                .map(timezone).toProperty("timezone")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default int insertSelective(UserRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(userRecord)
                .map(institutionId).toPropertyWhenPresent("institutionId", record::getInstitutionId)
                .map(uuid).toPropertyWhenPresent("uuid", record::getUuid)
                .map(name).toPropertyWhenPresent("name", record::getName)
                .map(userName).toPropertyWhenPresent("userName", record::getUserName)
                .map(password).toPropertyWhenPresent("password", record::getPassword)
                .map(email).toPropertyWhenPresent("email", record::getEmail)
                .map(creationDate).toPropertyWhenPresent("creationDate", record::getCreationDate)
                .map(active).toPropertyWhenPresent("active", record::getActive)
                .map(locale).toPropertyWhenPresent("locale", record::getLocale)
                .map(timezone).toPropertyWhenPresent("timezone", record::getTimezone)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<UserRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, institutionId, uuid, name, userName, password, email, creationDate, active, locale, timezone)
                .from(userRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<UserRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, institutionId, uuid, name, userName, password, email, creationDate, active, locale, timezone)
                .from(userRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default UserRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, institutionId, uuid, name, userName, password, email, creationDate, active, locale, timezone)
                .from(userRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(UserRecord record) {
        return UpdateDSL.updateWithMapper(this::update, userRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(uuid).equalTo(record::getUuid)
                .set(name).equalTo(record::getName)
                .set(userName).equalTo(record::getUserName)
                .set(password).equalTo(record::getPassword)
                .set(email).equalTo(record::getEmail)
                .set(creationDate).equalTo(record::getCreationDate)
                .set(active).equalTo(record::getActive)
                .set(locale).equalTo(record::getLocale)
                .set(timezone).equalTo(record::getTimezone);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(UserRecord record) {
        return UpdateDSL.updateWithMapper(this::update, userRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(uuid).equalToWhenPresent(record::getUuid)
                .set(name).equalToWhenPresent(record::getName)
                .set(userName).equalToWhenPresent(record::getUserName)
                .set(password).equalToWhenPresent(record::getPassword)
                .set(email).equalToWhenPresent(record::getEmail)
                .set(creationDate).equalToWhenPresent(record::getCreationDate)
                .set(active).equalToWhenPresent(record::getActive)
                .set(locale).equalToWhenPresent(record::getLocale)
                .set(timezone).equalToWhenPresent(record::getTimezone);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default int updateByPrimaryKey(UserRecord record) {
        return UpdateDSL.updateWithMapper(this::update, userRecord)
                .set(institutionId).equalTo(record::getInstitutionId)
                .set(uuid).equalTo(record::getUuid)
                .set(name).equalTo(record::getName)
                .set(userName).equalTo(record::getUserName)
                .set(password).equalTo(record::getPassword)
                .set(email).equalTo(record::getEmail)
                .set(creationDate).equalTo(record::getCreationDate)
                .set(active).equalTo(record::getActive)
                .set(locale).equalTo(record::getLocale)
                .set(timezone).equalTo(record::getTimezone)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.553+01:00", comments="Source Table: user")
    default int updateByPrimaryKeySelective(UserRecord record) {
        return UpdateDSL.updateWithMapper(this::update, userRecord)
                .set(institutionId).equalToWhenPresent(record::getInstitutionId)
                .set(uuid).equalToWhenPresent(record::getUuid)
                .set(name).equalToWhenPresent(record::getName)
                .set(userName).equalToWhenPresent(record::getUserName)
                .set(password).equalToWhenPresent(record::getPassword)
                .set(email).equalToWhenPresent(record::getEmail)
                .set(creationDate).equalToWhenPresent(record::getCreationDate)
                .set(active).equalToWhenPresent(record::getActive)
                .set(locale).equalToWhenPresent(record::getLocale)
                .set(timezone).equalToWhenPresent(record::getTimezone)
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
                        .from(userRecord);
    }
}