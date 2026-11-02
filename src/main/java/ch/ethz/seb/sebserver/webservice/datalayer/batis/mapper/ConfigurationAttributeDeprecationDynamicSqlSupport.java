package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import jakarta.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ConfigurationAttributeDeprecationDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: configuration_attribute_deprecation")
    public static final ConfigurationAttributeDeprecation configurationAttributeDeprecation = new ConfigurationAttributeDeprecation();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source field: configuration_attribute_deprecation.id")
    public static final SqlColumn<Long> id = configurationAttributeDeprecation.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source field: configuration_attribute_deprecation.configuration_attribute_id")
    public static final SqlColumn<Long> configurationAttributeId = configurationAttributeDeprecation.configurationAttributeId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: configuration_attribute_deprecation")
    public static final class ConfigurationAttributeDeprecation extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> configurationAttributeId = column("configuration_attribute_id", JDBCType.BIGINT);

        public ConfigurationAttributeDeprecation() {
            super("configuration_attribute_deprecation");
        }
    }
}