package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ConfigurationValueRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.934+02:00", comments="Source Table: configuration_value")
    public static final ConfigurationValueRecord configurationValueRecord = new ConfigurationValueRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.934+02:00", comments="Source field: configuration_value.id")
    public static final SqlColumn<Long> id = configurationValueRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.934+02:00", comments="Source field: configuration_value.institution_id")
    public static final SqlColumn<Long> institutionId = configurationValueRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.934+02:00", comments="Source field: configuration_value.configuration_id")
    public static final SqlColumn<Long> configurationId = configurationValueRecord.configurationId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.934+02:00", comments="Source field: configuration_value.configuration_attribute_id")
    public static final SqlColumn<Long> configurationAttributeId = configurationValueRecord.configurationAttributeId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.934+02:00", comments="Source field: configuration_value.list_index")
    public static final SqlColumn<Integer> listIndex = configurationValueRecord.listIndex;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.935+02:00", comments="Source field: configuration_value.value")
    public static final SqlColumn<String> value = configurationValueRecord.value;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.934+02:00", comments="Source Table: configuration_value")
    public static final class ConfigurationValueRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<Long> configurationId = column("configuration_id", JDBCType.BIGINT);

        public final SqlColumn<Long> configurationAttributeId = column("configuration_attribute_id", JDBCType.BIGINT);

        public final SqlColumn<Integer> listIndex = column("list_index", JDBCType.INTEGER);

        public final SqlColumn<String> value = column("value", JDBCType.VARCHAR);

        public ConfigurationValueRecord() {
            super("configuration_value");
        }
    }
}