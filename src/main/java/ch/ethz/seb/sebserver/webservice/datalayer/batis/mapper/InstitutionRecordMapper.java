package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.InstitutionRecord;
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
public interface InstitutionRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.037+02:00", comments="Source Table: institution")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.037+02:00", comments="Source Table: institution")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.037+02:00", comments="Source Table: institution")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<InstitutionRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.038+02:00", comments="Source Table: institution")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="url_suffix", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="theme_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="logo_image", javaType=String.class, jdbcType=JdbcType.CLOB)
    })
    InstitutionRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.039+02:00", comments="Source Table: institution")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="url_suffix", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="theme_name", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="active", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="logo_image", javaType=String.class, jdbcType=JdbcType.CLOB)
    })
    List<InstitutionRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(institutionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, institutionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, institutionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    default int insert(InstitutionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(institutionRecord)
                .map(name).toProperty("name")
                .map(urlSuffix).toProperty("urlSuffix")
                .map(themeName).toProperty("themeName")
                .map(active).toProperty("active")
                .map(logoImage).toProperty("logoImage")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    default int insertSelective(InstitutionRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(institutionRecord)
                .map(name).toPropertyWhenPresent("name", record::getName)
                .map(urlSuffix).toPropertyWhenPresent("urlSuffix", record::getUrlSuffix)
                .map(themeName).toPropertyWhenPresent("themeName", record::getThemeName)
                .map(active).toPropertyWhenPresent("active", record::getActive)
                .map(logoImage).toPropertyWhenPresent("logoImage", record::getLogoImage)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<InstitutionRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, name, urlSuffix, themeName, active, logoImage)
                .from(institutionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<InstitutionRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, name, urlSuffix, themeName, active, logoImage)
                .from(institutionRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    default InstitutionRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, name, urlSuffix, themeName, active, logoImage)
                .from(institutionRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(InstitutionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, institutionRecord)
                .set(name).equalTo(record::getName)
                .set(urlSuffix).equalTo(record::getUrlSuffix)
                .set(themeName).equalTo(record::getThemeName)
                .set(active).equalTo(record::getActive)
                .set(logoImage).equalTo(record::getLogoImage);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(InstitutionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, institutionRecord)
                .set(name).equalToWhenPresent(record::getName)
                .set(urlSuffix).equalToWhenPresent(record::getUrlSuffix)
                .set(themeName).equalToWhenPresent(record::getThemeName)
                .set(active).equalToWhenPresent(record::getActive)
                .set(logoImage).equalToWhenPresent(record::getLogoImage);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.040+02:00", comments="Source Table: institution")
    default int updateByPrimaryKey(InstitutionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, institutionRecord)
                .set(name).equalTo(record::getName)
                .set(urlSuffix).equalTo(record::getUrlSuffix)
                .set(themeName).equalTo(record::getThemeName)
                .set(active).equalTo(record::getActive)
                .set(logoImage).equalTo(record::getLogoImage)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.041+02:00", comments="Source Table: institution")
    default int updateByPrimaryKeySelective(InstitutionRecord record) {
        return UpdateDSL.updateWithMapper(this::update, institutionRecord)
                .set(name).equalToWhenPresent(record::getName)
                .set(urlSuffix).equalToWhenPresent(record::getUrlSuffix)
                .set(themeName).equalToWhenPresent(record::getThemeName)
                .set(active).equalToWhenPresent(record::getActive)
                .set(logoImage).equalToWhenPresent(record::getLogoImage)
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
                        .from(institutionRecord);
    }
}