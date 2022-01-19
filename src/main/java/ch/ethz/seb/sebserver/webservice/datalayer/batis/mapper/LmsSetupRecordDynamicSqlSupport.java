package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class LmsSetupRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.182+01:00", comments="Source Table: lms_setup")
    public static final LmsSetupRecord lmsSetupRecord = new LmsSetupRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.182+01:00", comments="Source field: lms_setup.id")
    public static final SqlColumn<Long> id = lmsSetupRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.183+01:00", comments="Source field: lms_setup.institution_id")
    public static final SqlColumn<Long> institutionId = lmsSetupRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.183+01:00", comments="Source field: lms_setup.name")
    public static final SqlColumn<String> name = lmsSetupRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.183+01:00", comments="Source field: lms_setup.lms_type")
    public static final SqlColumn<String> lmsType = lmsSetupRecord.lmsType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.183+01:00", comments="Source field: lms_setup.lms_url")
    public static final SqlColumn<String> lmsUrl = lmsSetupRecord.lmsUrl;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.183+01:00", comments="Source field: lms_setup.lms_clientname")
    public static final SqlColumn<String> lmsClientname = lmsSetupRecord.lmsClientname;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.183+01:00", comments="Source field: lms_setup.lms_clientsecret")
    public static final SqlColumn<String> lmsClientsecret = lmsSetupRecord.lmsClientsecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.184+01:00", comments="Source field: lms_setup.lms_rest_api_token")
    public static final SqlColumn<String> lmsRestApiToken = lmsSetupRecord.lmsRestApiToken;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source field: lms_setup.lms_proxy_host")
    public static final SqlColumn<String> lmsProxyHost = lmsSetupRecord.lmsProxyHost;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source field: lms_setup.lms_proxy_port")
    public static final SqlColumn<Integer> lmsProxyPort = lmsSetupRecord.lmsProxyPort;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source field: lms_setup.lms_proxy_auth_username")
    public static final SqlColumn<String> lmsProxyAuthUsername = lmsSetupRecord.lmsProxyAuthUsername;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source field: lms_setup.lms_proxy_auth_secret")
    public static final SqlColumn<String> lmsProxyAuthSecret = lmsSetupRecord.lmsProxyAuthSecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source field: lms_setup.update_time")
    public static final SqlColumn<Long> updateTime = lmsSetupRecord.updateTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.185+01:00", comments="Source field: lms_setup.active")
    public static final SqlColumn<Integer> active = lmsSetupRecord.active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.182+01:00", comments="Source Table: lms_setup")
    public static final class LmsSetupRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsType = column("lms_type", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsUrl = column("lms_url", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsClientname = column("lms_clientname", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsClientsecret = column("lms_clientsecret", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsRestApiToken = column("lms_rest_api_token", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsProxyHost = column("lms_proxy_host", JDBCType.VARCHAR);

        public final SqlColumn<Integer> lmsProxyPort = column("lms_proxy_port", JDBCType.INTEGER);

        public final SqlColumn<String> lmsProxyAuthUsername = column("lms_proxy_auth_username", JDBCType.VARCHAR);

        public final SqlColumn<String> lmsProxyAuthSecret = column("lms_proxy_auth_secret", JDBCType.VARCHAR);

        public final SqlColumn<Long> updateTime = column("update_time", JDBCType.BIGINT);

        public final SqlColumn<Integer> active = column("active", JDBCType.INTEGER);

        public LmsSetupRecord() {
            super("lms_setup");
        }
    }
}