package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class LmsSetupRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.203+01:00", comments="Source Table: lms_setup")
    public static final LmsSetupRecord lmsSetupRecord = new LmsSetupRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.203+01:00", comments="Source field: lms_setup.id")
    public static final SqlColumn<Long> id = lmsSetupRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.203+01:00", comments="Source field: lms_setup.institution_id")
    public static final SqlColumn<Long> institutionId = lmsSetupRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.203+01:00", comments="Source field: lms_setup.name")
    public static final SqlColumn<String> name = lmsSetupRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.204+01:00", comments="Source field: lms_setup.lms_type")
    public static final SqlColumn<String> lmsType = lmsSetupRecord.lmsType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.204+01:00", comments="Source field: lms_setup.lms_url")
    public static final SqlColumn<String> lmsUrl = lmsSetupRecord.lmsUrl;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.204+01:00", comments="Source field: lms_setup.lms_clientname")
    public static final SqlColumn<String> lmsClientname = lmsSetupRecord.lmsClientname;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.204+01:00", comments="Source field: lms_setup.lms_clientsecret")
    public static final SqlColumn<String> lmsClientsecret = lmsSetupRecord.lmsClientsecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.204+01:00", comments="Source field: lms_setup.lms_rest_api_token")
    public static final SqlColumn<String> lmsRestApiToken = lmsSetupRecord.lmsRestApiToken;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.204+01:00", comments="Source field: lms_setup.seb_clientname")
    public static final SqlColumn<String> sebClientname = lmsSetupRecord.sebClientname;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.204+01:00", comments="Source field: lms_setup.seb_clientsecret")
    public static final SqlColumn<String> sebClientsecret = lmsSetupRecord.sebClientsecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-26T12:04:08.203+01:00", comments="Source Table: lms_setup")
    public static final class LmsSetupRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsType = column("lms_type", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsUrl = column("lms_url", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsClientname = column("lms_clientname", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsClientsecret = column("lms_clientsecret", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsRestApiToken = column("lms_rest_api_token", JDBCType.VARCHAR);

        public final SqlColumn<String> sebClientname = column("seb_clientname", JDBCType.VARCHAR);

        public final SqlColumn<String> sebClientsecret = column("seb_clientsecret", JDBCType.VARCHAR);

        public LmsSetupRecord() {
            super("lms_setup");
        }
    }
}