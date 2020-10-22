package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class RemoteProctoringRoomRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-22T14:34:22.437+02:00", comments="Source Table: remote_proctoring_room")
    public static final RemoteProctoringRoomRecord remoteProctoringRoomRecord = new RemoteProctoringRoomRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-22T14:34:22.437+02:00", comments="Source field: remote_proctoring_room.id")
    public static final SqlColumn<Long> id = remoteProctoringRoomRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-22T14:34:22.437+02:00", comments="Source field: remote_proctoring_room.exam_id")
    public static final SqlColumn<Long> examId = remoteProctoringRoomRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-22T14:34:22.438+02:00", comments="Source field: remote_proctoring_room.name")
    public static final SqlColumn<String> name = remoteProctoringRoomRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-22T14:34:22.438+02:00", comments="Source field: remote_proctoring_room.size")
    public static final SqlColumn<Integer> size = remoteProctoringRoomRecord.size;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-22T14:34:22.438+02:00", comments="Source field: remote_proctoring_room.subject")
    public static final SqlColumn<String> subject = remoteProctoringRoomRecord.subject;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-22T14:34:22.437+02:00", comments="Source Table: remote_proctoring_room")
    public static final class RemoteProctoringRoomRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<Integer> size = column("size", JDBCType.INTEGER);

        public final SqlColumn<String> subject = column("subject", JDBCType.VARCHAR);

        public RemoteProctoringRoomRecord() {
            super("remote_proctoring_room");
        }
    }
}