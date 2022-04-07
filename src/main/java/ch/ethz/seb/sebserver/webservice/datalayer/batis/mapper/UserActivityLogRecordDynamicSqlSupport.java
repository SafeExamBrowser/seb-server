package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class UserActivityLogRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.058+02:00", comments="Source Table: user_activity_log")
    public static final UserActivityLogRecord userActivityLogRecord = new UserActivityLogRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.058+02:00", comments="Source field: user_activity_log.id")
    public static final SqlColumn<Long> id = userActivityLogRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.058+02:00", comments="Source field: user_activity_log.user_uuid")
    public static final SqlColumn<String> userUuid = userActivityLogRecord.userUuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.058+02:00", comments="Source field: user_activity_log.timestamp")
    public static final SqlColumn<Long> timestamp = userActivityLogRecord.timestamp;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.058+02:00", comments="Source field: user_activity_log.activity_type")
    public static final SqlColumn<String> activityType = userActivityLogRecord.activityType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.058+02:00", comments="Source field: user_activity_log.entity_type")
    public static final SqlColumn<String> entityType = userActivityLogRecord.entityType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.058+02:00", comments="Source field: user_activity_log.entity_id")
    public static final SqlColumn<String> entityId = userActivityLogRecord.entityId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.059+02:00", comments="Source field: user_activity_log.message")
    public static final SqlColumn<String> message = userActivityLogRecord.message;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.058+02:00", comments="Source Table: user_activity_log")
    public static final class UserActivityLogRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> userUuid = column("user_uuid", JDBCType.VARCHAR);

        public final SqlColumn<Long> timestamp = column("timestamp", JDBCType.BIGINT);

        public final SqlColumn<String> activityType = column("activity_type", JDBCType.VARCHAR);

        public final SqlColumn<String> entityType = column("entity_type", JDBCType.VARCHAR);

        public final SqlColumn<String> entityId = column("entity_id", JDBCType.VARCHAR);

        public final SqlColumn<String> message = column("message", JDBCType.VARCHAR);

        public UserActivityLogRecord() {
            super("user_activity_log");
        }
    }
}