package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ClientConnectionRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-08T14:34:59.319+01:00", comments="Source Table: client_connection")
    public static final ClientConnectionRecord clientConnectionRecord = new ClientConnectionRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-08T14:34:59.320+01:00", comments="Source field: client_connection.id")
    public static final SqlColumn<Long> id = clientConnectionRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-08T14:34:59.320+01:00", comments="Source field: client_connection.exam_id")
    public static final SqlColumn<Long> examId = clientConnectionRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-08T14:34:59.320+01:00", comments="Source field: client_connection.status")
    public static final SqlColumn<String> status = clientConnectionRecord.status;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-08T14:34:59.320+01:00", comments="Source field: client_connection.connection_token")
    public static final SqlColumn<String> connectionToken = clientConnectionRecord.connectionToken;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-08T14:34:59.320+01:00", comments="Source field: client_connection.user_name")
    public static final SqlColumn<String> userName = clientConnectionRecord.userName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-08T14:34:59.320+01:00", comments="Source field: client_connection.vdi")
    public static final SqlColumn<Boolean> vdi = clientConnectionRecord.vdi;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-08T14:34:59.325+01:00", comments="Source field: client_connection.client_address")
    public static final SqlColumn<String> clientAddress = clientConnectionRecord.clientAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-08T14:34:59.325+01:00", comments="Source field: client_connection.virtual_client_address")
    public static final SqlColumn<String> virtualClientAddress = clientConnectionRecord.virtualClientAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-08T14:34:59.320+01:00", comments="Source Table: client_connection")
    public static final class ClientConnectionRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<String> status = column("status", JDBCType.VARCHAR);

        public final SqlColumn<String> connectionToken = column("connection_token", JDBCType.VARCHAR);

        public final SqlColumn<String> userName = column("user_name", JDBCType.VARCHAR);

        public final SqlColumn<Boolean> vdi = column("vdi", JDBCType.BOOLEAN);

        public final SqlColumn<String> clientAddress = column("client_address", JDBCType.VARCHAR);

        public final SqlColumn<String> virtualClientAddress = column("virtual_client_address", JDBCType.VARCHAR);

        public ClientConnectionRecord() {
            super("client_connection");
        }
    }
}