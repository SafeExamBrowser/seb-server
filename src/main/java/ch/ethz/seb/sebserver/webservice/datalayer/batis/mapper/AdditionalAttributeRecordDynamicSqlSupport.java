package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class AdditionalAttributeRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.063+02:00", comments="Source Table: additional_attributes")
    public static final AdditionalAttributeRecord additionalAttributeRecord = new AdditionalAttributeRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.063+02:00", comments="Source field: additional_attributes.id")
    public static final SqlColumn<Long> id = additionalAttributeRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.063+02:00", comments="Source field: additional_attributes.entity_type")
    public static final SqlColumn<String> entityType = additionalAttributeRecord.entityType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.063+02:00", comments="Source field: additional_attributes.entity_id")
    public static final SqlColumn<Long> entityId = additionalAttributeRecord.entityId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.064+02:00", comments="Source field: additional_attributes.name")
    public static final SqlColumn<String> name = additionalAttributeRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.064+02:00", comments="Source field: additional_attributes.value")
    public static final SqlColumn<String> value = additionalAttributeRecord.value;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.063+02:00", comments="Source Table: additional_attributes")
    public static final class AdditionalAttributeRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> entityType = column("entity_type", JDBCType.VARCHAR);

        public final SqlColumn<Long> entityId = column("entity_id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> value = column("value", JDBCType.VARCHAR);

        public AdditionalAttributeRecord() {
            super("additional_attributes");
        }
    }
}