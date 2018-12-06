package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class UserLogRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-06T10:32:55.897+01:00", comments="Source Table: user_log")
    public static final UserLogRecord userLogRecord = new UserLogRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-06T10:32:55.897+01:00", comments="Source field: user_log.id")
    public static final SqlColumn<Long> id = userLogRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-06T10:32:55.897+01:00", comments="Source field: user_log.user_uuid")
    public static final SqlColumn<String> userUuid = userLogRecord.userUuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-06T10:32:55.898+01:00", comments="Source field: user_log.timestamp")
    public static final SqlColumn<Long> timestamp = userLogRecord.timestamp;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-06T10:32:55.898+01:00", comments="Source field: user_log.action_type")
    public static final SqlColumn<String> actionType = userLogRecord.actionType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-06T10:32:55.898+01:00", comments="Source field: user_log.entity_type")
    public static final SqlColumn<String> entityType = userLogRecord.entityType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-06T10:32:55.898+01:00", comments="Source field: user_log.entity_id")
    public static final SqlColumn<String> entityId = userLogRecord.entityId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-06T10:32:55.898+01:00", comments="Source field: user_log.message")
    public static final SqlColumn<String> message = userLogRecord.message;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-06T10:32:55.897+01:00", comments="Source Table: user_log")
    public static final class UserLogRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> userUuid = column("user_uuid", JDBCType.VARCHAR);

        public final SqlColumn<Long> timestamp = column("timestamp", JDBCType.BIGINT);

        public final SqlColumn<String> actionType = column("action_type", JDBCType.VARCHAR);

        public final SqlColumn<String> entityType = column("entity_type", JDBCType.VARCHAR);

        public final SqlColumn<String> entityId = column("entity_id", JDBCType.VARCHAR);

        public final SqlColumn<String> message = column("message", JDBCType.VARCHAR);

        public UserLogRecord() {
            super("user_log");
        }
    }
}