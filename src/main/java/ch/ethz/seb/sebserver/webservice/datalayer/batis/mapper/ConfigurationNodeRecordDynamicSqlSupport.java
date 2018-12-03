package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ConfigurationNodeRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.423+01:00", comments="Source Table: configuration_node")
    public static final ConfigurationNodeRecord configurationNodeRecord = new ConfigurationNodeRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.424+01:00", comments="Source field: configuration_node.id")
    public static final SqlColumn<Long> id = configurationNodeRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.424+01:00", comments="Source field: configuration_node.institution_id")
    public static final SqlColumn<Long> institutionId = configurationNodeRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.427+01:00", comments="Source field: configuration_node.owner")
    public static final SqlColumn<String> owner = configurationNodeRecord.owner;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.427+01:00", comments="Source field: configuration_node.name")
    public static final SqlColumn<String> name = configurationNodeRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.428+01:00", comments="Source field: configuration_node.description")
    public static final SqlColumn<String> description = configurationNodeRecord.description;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.428+01:00", comments="Source field: configuration_node.type")
    public static final SqlColumn<String> type = configurationNodeRecord.type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.429+01:00", comments="Source field: configuration_node.template")
    public static final SqlColumn<String> template = configurationNodeRecord.template;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.429+01:00", comments="Source field: configuration_node.active")
    public static final SqlColumn<Integer> active = configurationNodeRecord.active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.424+01:00", comments="Source Table: configuration_node")
    public static final class ConfigurationNodeRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<String> owner = column("owner", JDBCType.VARCHAR);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> description = column("description", JDBCType.VARCHAR);

        public final SqlColumn<String> type = column("type", JDBCType.VARCHAR);

        public final SqlColumn<String> template = column("template", JDBCType.VARCHAR);

        public final SqlColumn<Integer> active = column("active", JDBCType.INTEGER);

        public ConfigurationNodeRecord() {
            super("configuration_node");
        }
    }
}