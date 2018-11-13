package ch.ethz.seb.sebserver.ws.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class SebLmsSetupRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.548+01:00", comments="Source Table: seb_lms_setup")
    public static final SebLmsSetupRecord sebLmsSetupRecord = new SebLmsSetupRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.548+01:00", comments="Source field: seb_lms_setup.id")
    public static final SqlColumn<Long> id = sebLmsSetupRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.548+01:00", comments="Source field: seb_lms_setup.institution_id")
    public static final SqlColumn<Long> institutionId = sebLmsSetupRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.548+01:00", comments="Source field: seb_lms_setup.name")
    public static final SqlColumn<String> name = sebLmsSetupRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.549+01:00", comments="Source field: seb_lms_setup.lms_type")
    public static final SqlColumn<String> lmsType = sebLmsSetupRecord.lmsType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.549+01:00", comments="Source field: seb_lms_setup.lms_url")
    public static final SqlColumn<String> lmsUrl = sebLmsSetupRecord.lmsUrl;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.549+01:00", comments="Source field: seb_lms_setup.lms_clientname")
    public static final SqlColumn<String> lmsClientname = sebLmsSetupRecord.lmsClientname;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.549+01:00", comments="Source field: seb_lms_setup.lms_clientsecret")
    public static final SqlColumn<String> lmsClientsecret = sebLmsSetupRecord.lmsClientsecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.549+01:00", comments="Source field: seb_lms_setup.lms_rest_api_token")
    public static final SqlColumn<String> lmsRestApiToken = sebLmsSetupRecord.lmsRestApiToken;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.549+01:00", comments="Source field: seb_lms_setup.seb_clientname")
    public static final SqlColumn<String> sebClientname = sebLmsSetupRecord.sebClientname;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.549+01:00", comments="Source field: seb_lms_setup.seb_clientsecret")
    public static final SqlColumn<String> sebClientsecret = sebLmsSetupRecord.sebClientsecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.548+01:00", comments="Source Table: seb_lms_setup")
    public static final class SebLmsSetupRecord extends SqlTable {
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

        public SebLmsSetupRecord() {
            super("seb_lms_setup");
        }
    }
}