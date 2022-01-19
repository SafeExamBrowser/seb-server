package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.joda.time.DateTime;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class SebClientConfigRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.177+01:00", comments="Source Table: seb_client_configuration")
    public static final SebClientConfigRecord sebClientConfigRecord = new SebClientConfigRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.178+01:00", comments="Source field: seb_client_configuration.id")
    public static final SqlColumn<Long> id = sebClientConfigRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.178+01:00", comments="Source field: seb_client_configuration.institution_id")
    public static final SqlColumn<Long> institutionId = sebClientConfigRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.178+01:00", comments="Source field: seb_client_configuration.name")
    public static final SqlColumn<String> name = sebClientConfigRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.178+01:00", comments="Source field: seb_client_configuration.date")
    public static final SqlColumn<DateTime> date = sebClientConfigRecord.date;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.178+01:00", comments="Source field: seb_client_configuration.client_name")
    public static final SqlColumn<String> clientName = sebClientConfigRecord.clientName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.178+01:00", comments="Source field: seb_client_configuration.client_secret")
    public static final SqlColumn<String> clientSecret = sebClientConfigRecord.clientSecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.178+01:00", comments="Source field: seb_client_configuration.encrypt_secret")
    public static final SqlColumn<String> encryptSecret = sebClientConfigRecord.encryptSecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.178+01:00", comments="Source field: seb_client_configuration.active")
    public static final SqlColumn<Integer> active = sebClientConfigRecord.active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.177+01:00", comments="Source Table: seb_client_configuration")
    public static final class SebClientConfigRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<DateTime> date = column("date", JDBCType.TIMESTAMP, "ch.ethz.seb.sebserver.webservice.datalayer.batis.JodaTimeTypeResolver");

        public final SqlColumn<String> clientName = column("client_name", JDBCType.VARCHAR);

        public final SqlColumn<String> clientSecret = column("client_secret", JDBCType.VARCHAR);

        public final SqlColumn<String> encryptSecret = column("encrypt_secret", JDBCType.VARCHAR);

        public final SqlColumn<Integer> active = column("active", JDBCType.INTEGER);

        public SebClientConfigRecord() {
            super("seb_client_configuration");
        }
    }
}