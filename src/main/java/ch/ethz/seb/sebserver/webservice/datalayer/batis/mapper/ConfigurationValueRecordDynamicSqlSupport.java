package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ConfigurationValueRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-29T16:15:37.686+01:00", comments="Source Table: configuration_value")
    public static final ConfigurationValueRecord configurationValueRecord = new ConfigurationValueRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-29T16:15:37.686+01:00", comments="Source field: configuration_value.id")
    public static final SqlColumn<Long> id = configurationValueRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-29T16:15:37.686+01:00", comments="Source field: configuration_value.configuration_id")
    public static final SqlColumn<Long> configurationId = configurationValueRecord.configurationId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-29T16:15:37.686+01:00", comments="Source field: configuration_value.configuration_attribute_id")
    public static final SqlColumn<Long> configurationAttributeId = configurationValueRecord.configurationAttributeId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-29T16:15:37.686+01:00", comments="Source field: configuration_value.list_index")
    public static final SqlColumn<Integer> listIndex = configurationValueRecord.listIndex;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-29T16:15:37.687+01:00", comments="Source field: configuration_value.value")
    public static final SqlColumn<String> value = configurationValueRecord.value;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-29T16:15:37.687+01:00", comments="Source field: configuration_value.text")
    public static final SqlColumn<String> text = configurationValueRecord.text;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-29T16:15:37.686+01:00", comments="Source Table: configuration_value")
    public static final class ConfigurationValueRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> configurationId = column("configuration_id", JDBCType.BIGINT);

        public final SqlColumn<Long> configurationAttributeId = column("configuration_attribute_id", JDBCType.BIGINT);

        public final SqlColumn<Integer> listIndex = column("list_index", JDBCType.INTEGER);

        public final SqlColumn<String> value = column("value", JDBCType.VARCHAR);

        public final SqlColumn<String> text = column("text", JDBCType.CLOB);

        public ConfigurationValueRecord() {
            super("configuration_value");
        }
    }
}