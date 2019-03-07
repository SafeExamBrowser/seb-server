package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import java.util.Date;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class SebClientCredentialsRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-03-07T15:53:05.823+01:00", comments="Source Table: seb_client_credentials")
    public static final SebClientCredentialsRecord sebClientCredentialsRecord = new SebClientCredentialsRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-03-07T15:53:05.823+01:00", comments="Source field: seb_client_credentials.id")
    public static final SqlColumn<Long> id = sebClientCredentialsRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-03-07T15:53:05.823+01:00", comments="Source field: seb_client_credentials.institution_id")
    public static final SqlColumn<Long> institutionId = sebClientCredentialsRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-03-07T15:53:05.824+01:00", comments="Source field: seb_client_credentials.name")
    public static final SqlColumn<String> name = sebClientCredentialsRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-03-07T15:53:05.824+01:00", comments="Source field: seb_client_credentials.date")
    public static final SqlColumn<Date> date = sebClientCredentialsRecord.date;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-03-07T15:53:05.824+01:00", comments="Source field: seb_client_credentials.client_name")
    public static final SqlColumn<String> clientName = sebClientCredentialsRecord.clientName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-03-07T15:53:05.824+01:00", comments="Source field: seb_client_credentials.client_secret")
    public static final SqlColumn<String> clientSecret = sebClientCredentialsRecord.clientSecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-03-07T15:53:05.824+01:00", comments="Source field: seb_client_credentials.active")
    public static final SqlColumn<Integer> active = sebClientCredentialsRecord.active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-03-07T15:53:05.823+01:00", comments="Source Table: seb_client_credentials")
    public static final class SebClientCredentialsRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<Date> date = column("date", JDBCType.TIMESTAMP);

        public final SqlColumn<String> clientName = column("client_name", JDBCType.VARCHAR);

        public final SqlColumn<String> clientSecret = column("client_secret", JDBCType.VARCHAR);

        public final SqlColumn<Integer> active = column("active", JDBCType.INTEGER);

        public SebClientCredentialsRecord() {
            super("seb_client_credentials");
        }
    }
}