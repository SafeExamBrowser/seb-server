package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class RemoteProctoringRoomRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.145+01:00", comments="Source Table: remote_proctoring_room")
    public static final RemoteProctoringRoomRecord remoteProctoringRoomRecord = new RemoteProctoringRoomRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.145+01:00", comments="Source field: remote_proctoring_room.id")
    public static final SqlColumn<Long> id = remoteProctoringRoomRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.145+01:00", comments="Source field: remote_proctoring_room.exam_id")
    public static final SqlColumn<Long> examId = remoteProctoringRoomRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.145+01:00", comments="Source field: remote_proctoring_room.name")
    public static final SqlColumn<String> name = remoteProctoringRoomRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.145+01:00", comments="Source field: remote_proctoring_room.size")
    public static final SqlColumn<Integer> size = remoteProctoringRoomRecord.size;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.146+01:00", comments="Source field: remote_proctoring_room.subject")
    public static final SqlColumn<String> subject = remoteProctoringRoomRecord.subject;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.146+01:00", comments="Source field: remote_proctoring_room.townhall_room")
    public static final SqlColumn<Integer> townhallRoom = remoteProctoringRoomRecord.townhallRoom;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.147+01:00", comments="Source field: remote_proctoring_room.break_out_connections")
    public static final SqlColumn<String> breakOutConnections = remoteProctoringRoomRecord.breakOutConnections;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.148+01:00", comments="Source field: remote_proctoring_room.join_key")
    public static final SqlColumn<String> joinKey = remoteProctoringRoomRecord.joinKey;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.148+01:00", comments="Source field: remote_proctoring_room.room_data")
    public static final SqlColumn<String> roomData = remoteProctoringRoomRecord.roomData;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.145+01:00", comments="Source Table: remote_proctoring_room")
    public static final class RemoteProctoringRoomRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<Integer> size = column("size", JDBCType.INTEGER);

        public final SqlColumn<String> subject = column("subject", JDBCType.VARCHAR);

        public final SqlColumn<Integer> townhallRoom = column("townhall_room", JDBCType.INTEGER);

        public final SqlColumn<String> breakOutConnections = column("break_out_connections", JDBCType.VARCHAR);

        public final SqlColumn<String> joinKey = column("join_key", JDBCType.VARCHAR);

        public final SqlColumn<String> roomData = column("room_data", JDBCType.VARCHAR);

        public RemoteProctoringRoomRecord() {
            super("remote_proctoring_room");
        }
    }
}