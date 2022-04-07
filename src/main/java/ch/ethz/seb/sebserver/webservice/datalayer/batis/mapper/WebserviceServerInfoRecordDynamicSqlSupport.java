package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class WebserviceServerInfoRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.067+02:00", comments="Source Table: webservice_server_info")
    public static final WebserviceServerInfoRecord webserviceServerInfoRecord = new WebserviceServerInfoRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.067+02:00", comments="Source field: webservice_server_info.id")
    public static final SqlColumn<Long> id = webserviceServerInfoRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.067+02:00", comments="Source field: webservice_server_info.uuid")
    public static final SqlColumn<String> uuid = webserviceServerInfoRecord.uuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.067+02:00", comments="Source field: webservice_server_info.service_address")
    public static final SqlColumn<String> serviceAddress = webserviceServerInfoRecord.serviceAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.067+02:00", comments="Source field: webservice_server_info.master")
    public static final SqlColumn<Integer> master = webserviceServerInfoRecord.master;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.067+02:00", comments="Source field: webservice_server_info.update_time")
    public static final SqlColumn<Long> updateTime = webserviceServerInfoRecord.updateTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.067+02:00", comments="Source Table: webservice_server_info")
    public static final class WebserviceServerInfoRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> uuid = column("uuid", JDBCType.VARCHAR);

        public final SqlColumn<String> serviceAddress = column("service_address", JDBCType.VARCHAR);

        public final SqlColumn<Integer> master = column("master", JDBCType.INTEGER);

        public final SqlColumn<Long> updateTime = column("update_time", JDBCType.BIGINT);

        public WebserviceServerInfoRecord() {
            super("webservice_server_info");
        }
    }
}