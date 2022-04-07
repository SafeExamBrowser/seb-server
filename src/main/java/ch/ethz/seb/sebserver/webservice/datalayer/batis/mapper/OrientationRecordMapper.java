package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.OrientationRecord;
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
public interface OrientationRecordMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.951+02:00", comments="Source Table: orientation")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.951+02:00", comments="Source Table: orientation")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.952+02:00", comments="Source Table: orientation")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="record.id", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<OrientationRecord> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.952+02:00", comments="Source Table: orientation")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="config_attribute_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="template_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="view_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="group_id", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="x_position", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="y_position", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="width", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="height", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="title", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    OrientationRecord selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.952+02:00", comments="Source Table: orientation")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ConstructorArgs({
        @Arg(column="id", javaType=Long.class, jdbcType=JdbcType.BIGINT, id=true),
        @Arg(column="config_attribute_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="template_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="view_id", javaType=Long.class, jdbcType=JdbcType.BIGINT),
        @Arg(column="group_id", javaType=String.class, jdbcType=JdbcType.VARCHAR),
        @Arg(column="x_position", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="y_position", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="width", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="height", javaType=Integer.class, jdbcType=JdbcType.INTEGER),
        @Arg(column="title", javaType=String.class, jdbcType=JdbcType.VARCHAR)
    })
    List<OrientationRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.952+02:00", comments="Source Table: orientation")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.952+02:00", comments="Source Table: orientation")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> countByExample() {
        return SelectDSL.selectWithMapper(this::count, SqlBuilder.count())
                .from(orientationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.952+02:00", comments="Source Table: orientation")
    default DeleteDSL<MyBatis3DeleteModelAdapter<Integer>> deleteByExample() {
        return DeleteDSL.deleteFromWithMapper(this::delete, orientationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.952+02:00", comments="Source Table: orientation")
    default int deleteByPrimaryKey(Long id_) {
        return DeleteDSL.deleteFromWithMapper(this::delete, orientationRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.952+02:00", comments="Source Table: orientation")
    default int insert(OrientationRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(orientationRecord)
                .map(configAttributeId).toProperty("configAttributeId")
                .map(templateId).toProperty("templateId")
                .map(viewId).toProperty("viewId")
                .map(groupId).toProperty("groupId")
                .map(xPosition).toProperty("xPosition")
                .map(yPosition).toProperty("yPosition")
                .map(width).toProperty("width")
                .map(height).toProperty("height")
                .map(title).toProperty("title")
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.952+02:00", comments="Source Table: orientation")
    default int insertSelective(OrientationRecord record) {
        return insert(SqlBuilder.insert(record)
                .into(orientationRecord)
                .map(configAttributeId).toPropertyWhenPresent("configAttributeId", record::getConfigAttributeId)
                .map(templateId).toPropertyWhenPresent("templateId", record::getTemplateId)
                .map(viewId).toPropertyWhenPresent("viewId", record::getViewId)
                .map(groupId).toPropertyWhenPresent("groupId", record::getGroupId)
                .map(xPosition).toPropertyWhenPresent("xPosition", record::getxPosition)
                .map(yPosition).toPropertyWhenPresent("yPosition", record::getyPosition)
                .map(width).toPropertyWhenPresent("width", record::getWidth)
                .map(height).toPropertyWhenPresent("height", record::getHeight)
                .map(title).toPropertyWhenPresent("title", record::getTitle)
                .build()
                .render(RenderingStrategy.MYBATIS3));
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.953+02:00", comments="Source Table: orientation")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<OrientationRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(this::selectMany, id, configAttributeId, templateId, viewId, groupId, xPosition, yPosition, width, height, title)
                .from(orientationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.953+02:00", comments="Source Table: orientation")
    default QueryExpressionDSL<MyBatis3SelectModelAdapter<List<OrientationRecord>>> selectDistinctByExample() {
        return SelectDSL.selectDistinctWithMapper(this::selectMany, id, configAttributeId, templateId, viewId, groupId, xPosition, yPosition, width, height, title)
                .from(orientationRecord);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.953+02:00", comments="Source Table: orientation")
    default OrientationRecord selectByPrimaryKey(Long id_) {
        return SelectDSL.selectWithMapper(this::selectOne, id, configAttributeId, templateId, viewId, groupId, xPosition, yPosition, width, height, title)
                .from(orientationRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.953+02:00", comments="Source Table: orientation")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExample(OrientationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, orientationRecord)
                .set(configAttributeId).equalTo(record::getConfigAttributeId)
                .set(templateId).equalTo(record::getTemplateId)
                .set(viewId).equalTo(record::getViewId)
                .set(groupId).equalTo(record::getGroupId)
                .set(xPosition).equalTo(record::getxPosition)
                .set(yPosition).equalTo(record::getyPosition)
                .set(width).equalTo(record::getWidth)
                .set(height).equalTo(record::getHeight)
                .set(title).equalTo(record::getTitle);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.953+02:00", comments="Source Table: orientation")
    default UpdateDSL<MyBatis3UpdateModelAdapter<Integer>> updateByExampleSelective(OrientationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, orientationRecord)
                .set(configAttributeId).equalToWhenPresent(record::getConfigAttributeId)
                .set(templateId).equalToWhenPresent(record::getTemplateId)
                .set(viewId).equalToWhenPresent(record::getViewId)
                .set(groupId).equalToWhenPresent(record::getGroupId)
                .set(xPosition).equalToWhenPresent(record::getxPosition)
                .set(yPosition).equalToWhenPresent(record::getyPosition)
                .set(width).equalToWhenPresent(record::getWidth)
                .set(height).equalToWhenPresent(record::getHeight)
                .set(title).equalToWhenPresent(record::getTitle);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.953+02:00", comments="Source Table: orientation")
    default int updateByPrimaryKey(OrientationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, orientationRecord)
                .set(configAttributeId).equalTo(record::getConfigAttributeId)
                .set(templateId).equalTo(record::getTemplateId)
                .set(viewId).equalTo(record::getViewId)
                .set(groupId).equalTo(record::getGroupId)
                .set(xPosition).equalTo(record::getxPosition)
                .set(yPosition).equalTo(record::getyPosition)
                .set(width).equalTo(record::getWidth)
                .set(height).equalTo(record::getHeight)
                .set(title).equalTo(record::getTitle)
                .where(id, isEqualTo(record::getId))
                .build()
                .execute();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.953+02:00", comments="Source Table: orientation")
    default int updateByPrimaryKeySelective(OrientationRecord record) {
        return UpdateDSL.updateWithMapper(this::update, orientationRecord)
                .set(configAttributeId).equalToWhenPresent(record::getConfigAttributeId)
                .set(templateId).equalToWhenPresent(record::getTemplateId)
                .set(viewId).equalToWhenPresent(record::getViewId)
                .set(groupId).equalToWhenPresent(record::getGroupId)
                .set(xPosition).equalToWhenPresent(record::getxPosition)
                .set(yPosition).equalToWhenPresent(record::getyPosition)
                .set(width).equalToWhenPresent(record::getWidth)
                .set(height).equalToWhenPresent(record::getHeight)
                .set(title).equalToWhenPresent(record::getTitle)
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
                        .from(orientationRecord);
    }
}