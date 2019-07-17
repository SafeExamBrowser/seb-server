package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class WebserviceServerInfoDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source Table: webservice_server_info")
    public static final WebserviceServerInfo webserviceServerInfo = new WebserviceServerInfo();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source field: webservice_server_info.id")
    public static final SqlColumn<Long> id = webserviceServerInfo.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source field: webservice_server_info.uuid")
    public static final SqlColumn<String> uuid = webserviceServerInfo.uuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source field: webservice_server_info.service_address")
    public static final SqlColumn<String> serviceAddress = webserviceServerInfo.serviceAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-07-15T11:57:33.838+02:00", comments="Source Table: webservice_server_info")
    public static final class WebserviceServerInfo extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> uuid = column("uuid", JDBCType.VARCHAR);

        public final SqlColumn<String> serviceAddress = column("service_address", JDBCType.VARCHAR);

        public WebserviceServerInfo() {
            super("webservice_server_info");
        }
    }
}