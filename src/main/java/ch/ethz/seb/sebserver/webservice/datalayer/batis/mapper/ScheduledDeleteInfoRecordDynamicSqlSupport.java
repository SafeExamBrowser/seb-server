package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import jakarta.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ScheduledDeleteInfoRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.098+02:00", comments="Source Table: scheduled_delete_info")
    public static final ScheduledDeleteInfoRecord scheduledDeleteInfoRecord = new ScheduledDeleteInfoRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source field: scheduled_delete_info.id")
    public static final SqlColumn<Long> id = scheduledDeleteInfoRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source field: scheduled_delete_info.scheduled_delete_id")
    public static final SqlColumn<Long> scheduledDeleteId = scheduledDeleteInfoRecord.scheduledDeleteId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source field: scheduled_delete_info.state")
    public static final SqlColumn<String> state = scheduledDeleteInfoRecord.state;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source field: scheduled_delete_info.exam_uuid")
    public static final SqlColumn<String> examUuid = scheduledDeleteInfoRecord.examUuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source field: scheduled_delete_info.deletion_info")
    public static final SqlColumn<String> deletionInfo = scheduledDeleteInfoRecord.deletionInfo;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source field: scheduled_delete_info.error_info")
    public static final SqlColumn<String> errorInfo = scheduledDeleteInfoRecord.errorInfo;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2026-05-11T14:20:08.100+02:00", comments="Source Table: scheduled_delete_info")
    public static final class ScheduledDeleteInfoRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> scheduledDeleteId = column("scheduled_delete_id", JDBCType.BIGINT);

        public final SqlColumn<String> state = column("state", JDBCType.VARCHAR);

        public final SqlColumn<String> examUuid = column("exam_uuid", JDBCType.VARCHAR);

        public final SqlColumn<String> deletionInfo = column("deletion_info", JDBCType.VARCHAR);

        public final SqlColumn<String> errorInfo = column("error_info", JDBCType.VARCHAR);

        public ScheduledDeleteInfoRecord() {
            super("scheduled_delete_info");
        }
    }
}