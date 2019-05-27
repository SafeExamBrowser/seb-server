package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class AdditionalAttributesDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.473+02:00", comments="Source Table: additional_attributes")
    public static final AdditionalAttributes additionalAttributes = new AdditionalAttributes();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.473+02:00", comments="Source field: additional_attributes.id")
    public static final SqlColumn<Long> id = additionalAttributes.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.473+02:00", comments="Source field: additional_attributes.entity_type")
    public static final SqlColumn<String> entityType = additionalAttributes.entityType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.473+02:00", comments="Source field: additional_attributes.entity_id")
    public static final SqlColumn<Long> entityId = additionalAttributes.entityId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.473+02:00", comments="Source field: additional_attributes.name")
    public static final SqlColumn<String> name = additionalAttributes.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.473+02:00", comments="Source field: additional_attributes.value")
    public static final SqlColumn<String> value = additionalAttributes.value;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.473+02:00", comments="Source Table: additional_attributes")
    public static final class AdditionalAttributes extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> entityType = column("entity_type", JDBCType.VARCHAR);

        public final SqlColumn<Long> entityId = column("entity_id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> value = column("value", JDBCType.VARCHAR);

        public AdditionalAttributes() {
            super("additional_attributes");
        }
    }
}