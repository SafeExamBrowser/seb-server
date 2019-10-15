package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class WebserviceServerInfoRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-10-15T08:50:46.762+02:00", comments="Source Table: webservice_server_info")
    public static final WebserviceServerInfoRecord webserviceServerInfoRecord = new WebserviceServerInfoRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-10-15T08:50:46.762+02:00", comments="Source field: webservice_server_info.id")
    public static final SqlColumn<Long> id = webserviceServerInfoRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-10-15T08:50:46.762+02:00", comments="Source field: webservice_server_info.uuid")
    public static final SqlColumn<String> uuid = webserviceServerInfoRecord.uuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-10-15T08:50:46.763+02:00", comments="Source field: webservice_server_info.service_address")
    public static final SqlColumn<String> serviceAddress = webserviceServerInfoRecord.serviceAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-10-15T08:50:46.762+02:00", comments="Source Table: webservice_server_info")
    public static final class WebserviceServerInfoRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> uuid = column("uuid", JDBCType.VARCHAR);

        public final SqlColumn<String> serviceAddress = column("service_address", JDBCType.VARCHAR);

        public WebserviceServerInfoRecord() {
            super("webservice_server_info");
        }
    }
}