package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ClientConnectionRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.996+02:00", comments="Source Table: client_connection")
    public static final ClientConnectionRecord clientConnectionRecord = new ClientConnectionRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.996+02:00", comments="Source field: client_connection.id")
    public static final SqlColumn<Long> id = clientConnectionRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.996+02:00", comments="Source field: client_connection.institution_id")
    public static final SqlColumn<Long> institutionId = clientConnectionRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.996+02:00", comments="Source field: client_connection.exam_id")
    public static final SqlColumn<Long> examId = clientConnectionRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.997+02:00", comments="Source field: client_connection.status")
    public static final SqlColumn<String> status = clientConnectionRecord.status;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.997+02:00", comments="Source field: client_connection.connection_token")
    public static final SqlColumn<String> connectionToken = clientConnectionRecord.connectionToken;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.997+02:00", comments="Source field: client_connection.exam_user_session_id")
    public static final SqlColumn<String> examUserSessionId = clientConnectionRecord.examUserSessionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.997+02:00", comments="Source field: client_connection.client_address")
    public static final SqlColumn<String> clientAddress = clientConnectionRecord.clientAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.997+02:00", comments="Source field: client_connection.virtual_client_address")
    public static final SqlColumn<String> virtualClientAddress = clientConnectionRecord.virtualClientAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.997+02:00", comments="Source field: client_connection.vdi")
    public static final SqlColumn<Integer> vdi = clientConnectionRecord.vdi;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.997+02:00", comments="Source field: client_connection.vdi_pair_token")
    public static final SqlColumn<String> vdiPairToken = clientConnectionRecord.vdiPairToken;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.997+02:00", comments="Source field: client_connection.creation_time")
    public static final SqlColumn<Long> creationTime = clientConnectionRecord.creationTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.998+02:00", comments="Source field: client_connection.update_time")
    public static final SqlColumn<Long> updateTime = clientConnectionRecord.updateTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.999+02:00", comments="Source field: client_connection.remote_proctoring_room_id")
    public static final SqlColumn<Long> remoteProctoringRoomId = clientConnectionRecord.remoteProctoringRoomId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.999+02:00", comments="Source field: client_connection.remote_proctoring_room_update")
    public static final SqlColumn<Integer> remoteProctoringRoomUpdate = clientConnectionRecord.remoteProctoringRoomUpdate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.999+02:00", comments="Source field: client_connection.client_machine_name")
    public static final SqlColumn<String> clientMachineName = clientConnectionRecord.clientMachineName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.999+02:00", comments="Source field: client_connection.client_os_name")
    public static final SqlColumn<String> clientOsName = clientConnectionRecord.clientOsName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.999+02:00", comments="Source field: client_connection.client_version")
    public static final SqlColumn<String> clientVersion = clientConnectionRecord.clientVersion;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:30.996+02:00", comments="Source Table: client_connection")
    public static final class ClientConnectionRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<String> status = column("status", JDBCType.VARCHAR);

        public final SqlColumn<String> connectionToken = column("connection_token", JDBCType.VARCHAR);

        public final SqlColumn<String> examUserSessionId = column("exam_user_session_id", JDBCType.VARCHAR);

        public final SqlColumn<String> clientAddress = column("client_address", JDBCType.VARCHAR);

        public final SqlColumn<String> virtualClientAddress = column("virtual_client_address", JDBCType.VARCHAR);

        public final SqlColumn<Integer> vdi = column("vdi", JDBCType.INTEGER);

        public final SqlColumn<String> vdiPairToken = column("vdi_pair_token", JDBCType.VARCHAR);

        public final SqlColumn<Long> creationTime = column("creation_time", JDBCType.BIGINT);

        public final SqlColumn<Long> updateTime = column("update_time", JDBCType.BIGINT);

        public final SqlColumn<Long> remoteProctoringRoomId = column("remote_proctoring_room_id", JDBCType.BIGINT);

        public final SqlColumn<Integer> remoteProctoringRoomUpdate = column("remote_proctoring_room_update", JDBCType.INTEGER);

        public final SqlColumn<String> clientMachineName = column("client_machine_name", JDBCType.VARCHAR);

        public final SqlColumn<String> clientOsName = column("client_os_name", JDBCType.VARCHAR);

        public final SqlColumn<String> clientVersion = column("client_version", JDBCType.VARCHAR);

        public ClientConnectionRecord() {
            super("client_connection");
        }
    }
}