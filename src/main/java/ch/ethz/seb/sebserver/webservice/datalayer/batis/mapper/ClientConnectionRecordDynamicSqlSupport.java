package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ClientConnectionRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-08-11T12:03:38.712+02:00", comments="Source Table: client_connection")
    public static final ClientConnectionRecord clientConnectionRecord = new ClientConnectionRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-08-11T12:03:38.712+02:00", comments="Source field: client_connection.id")
    public static final SqlColumn<Long> id = clientConnectionRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-08-11T12:03:38.712+02:00", comments="Source field: client_connection.institution_id")
    public static final SqlColumn<Long> institutionId = clientConnectionRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-08-11T12:03:38.712+02:00", comments="Source field: client_connection.exam_id")
    public static final SqlColumn<Long> examId = clientConnectionRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-08-11T12:03:38.713+02:00", comments="Source field: client_connection.status")
    public static final SqlColumn<String> status = clientConnectionRecord.status;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-08-11T12:03:38.713+02:00", comments="Source field: client_connection.connection_token")
    public static final SqlColumn<String> connectionToken = clientConnectionRecord.connectionToken;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-08-11T12:03:38.713+02:00", comments="Source field: client_connection.exam_user_session_id")
    public static final SqlColumn<String> examUserSessionId = clientConnectionRecord.examUserSessionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-08-11T12:03:38.713+02:00", comments="Source field: client_connection.client_address")
    public static final SqlColumn<String> clientAddress = clientConnectionRecord.clientAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-08-11T12:03:38.713+02:00", comments="Source field: client_connection.virtual_client_address")
    public static final SqlColumn<String> virtualClientAddress = clientConnectionRecord.virtualClientAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-08-11T12:03:38.713+02:00", comments="Source field: client_connection.creation_time")
    public static final SqlColumn<Long> creationTime = clientConnectionRecord.creationTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-08-11T12:03:38.712+02:00", comments="Source Table: client_connection")
    public static final class ClientConnectionRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<String> status = column("status", JDBCType.VARCHAR);

        public final SqlColumn<String> connectionToken = column("connection_token", JDBCType.VARCHAR);

        public final SqlColumn<String> examUserSessionId = column("exam_user_session_id", JDBCType.VARCHAR);

        public final SqlColumn<String> clientAddress = column("client_address", JDBCType.VARCHAR);

        public final SqlColumn<String> virtualClientAddress = column("virtual_client_address", JDBCType.VARCHAR);

        public final SqlColumn<Long> creationTime = column("creation_time", JDBCType.BIGINT);

        public ClientConnectionRecord() {
            super("client_connection");
        }
    }
}