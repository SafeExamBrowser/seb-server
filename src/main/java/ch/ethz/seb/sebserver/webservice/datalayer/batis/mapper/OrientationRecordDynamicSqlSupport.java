package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class OrientationRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.105+01:00", comments="Source Table: orientation")
    public static final OrientationRecord orientationRecord = new OrientationRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.105+01:00", comments="Source field: orientation.id")
    public static final SqlColumn<Long> id = orientationRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.105+01:00", comments="Source field: orientation.config_attribute_id")
    public static final SqlColumn<Long> configAttributeId = orientationRecord.configAttributeId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.105+01:00", comments="Source field: orientation.template_id")
    public static final SqlColumn<Long> templateId = orientationRecord.templateId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.106+01:00", comments="Source field: orientation.view_id")
    public static final SqlColumn<Long> viewId = orientationRecord.viewId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.106+01:00", comments="Source field: orientation.group_id")
    public static final SqlColumn<String> groupId = orientationRecord.groupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.106+01:00", comments="Source field: orientation.x_position")
    public static final SqlColumn<Integer> xPosition = orientationRecord.xPosition;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.106+01:00", comments="Source field: orientation.y_position")
    public static final SqlColumn<Integer> yPosition = orientationRecord.yPosition;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.107+01:00", comments="Source field: orientation.width")
    public static final SqlColumn<Integer> width = orientationRecord.width;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.107+01:00", comments="Source field: orientation.height")
    public static final SqlColumn<Integer> height = orientationRecord.height;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.107+01:00", comments="Source field: orientation.title")
    public static final SqlColumn<String> title = orientationRecord.title;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.105+01:00", comments="Source Table: orientation")
    public static final class OrientationRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> configAttributeId = column("config_attribute_id", JDBCType.BIGINT);

        public final SqlColumn<Long> templateId = column("template_id", JDBCType.BIGINT);

        public final SqlColumn<Long> viewId = column("view_id", JDBCType.BIGINT);

        public final SqlColumn<String> groupId = column("group_id", JDBCType.VARCHAR);

        public final SqlColumn<Integer> xPosition = column("x_position", JDBCType.INTEGER);

        public final SqlColumn<Integer> yPosition = column("y_position", JDBCType.INTEGER);

        public final SqlColumn<Integer> width = column("width", JDBCType.INTEGER);

        public final SqlColumn<Integer> height = column("height", JDBCType.INTEGER);

        public final SqlColumn<String> title = column("title", JDBCType.VARCHAR);

        public OrientationRecord() {
            super("orientation");
        }
    }
}