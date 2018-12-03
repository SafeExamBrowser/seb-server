package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.joda.time.DateTime;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ConfigurationRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.413+01:00", comments="Source Table: configuration")
    public static final ConfigurationRecord configurationRecord = new ConfigurationRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.413+01:00", comments="Source field: configuration.id")
    public static final SqlColumn<Long> id = configurationRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.414+01:00", comments="Source field: configuration.configuration_node_id")
    public static final SqlColumn<Long> configurationNodeId = configurationRecord.configurationNodeId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.414+01:00", comments="Source field: configuration.version")
    public static final SqlColumn<String> version = configurationRecord.version;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.414+01:00", comments="Source field: configuration.version_date")
    public static final SqlColumn<DateTime> versionDate = configurationRecord.versionDate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.417+01:00", comments="Source field: configuration.followup")
    public static final SqlColumn<Integer> followup = configurationRecord.followup;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T08:25:17.413+01:00", comments="Source Table: configuration")
    public static final class ConfigurationRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> configurationNodeId = column("configuration_node_id", JDBCType.BIGINT);

        public final SqlColumn<String> version = column("version", JDBCType.VARCHAR);

        public final SqlColumn<DateTime> versionDate = column("version_date", JDBCType.TIMESTAMP, "ch.ethz.seb.sebserver.webservice.datalayer.batis.JodaTimeTypeResolver");

        public final SqlColumn<Integer> followup = column("followup", JDBCType.INTEGER);

        public ConfigurationRecord() {
            super("configuration");
        }
    }
}