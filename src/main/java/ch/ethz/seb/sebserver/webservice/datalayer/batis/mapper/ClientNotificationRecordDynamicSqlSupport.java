package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ClientNotificationRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source Table: client_notification")
    public static final ClientNotificationRecord clientNotificationRecord = new ClientNotificationRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source field: client_notification.id")
    public static final SqlColumn<Long> id = clientNotificationRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source field: client_notification.client_connection_id")
    public static final SqlColumn<Long> clientConnectionId = clientNotificationRecord.clientConnectionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source field: client_notification.event_type")
    public static final SqlColumn<Integer> eventType = clientNotificationRecord.eventType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source field: client_notification.notification_type")
    public static final SqlColumn<Integer> notificationType = clientNotificationRecord.notificationType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source field: client_notification.value")
    public static final SqlColumn<Long> value = clientNotificationRecord.value;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source field: client_notification.text")
    public static final SqlColumn<String> text = clientNotificationRecord.text;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.223+01:00", comments="Source Table: client_notification")
    public static final class ClientNotificationRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> clientConnectionId = column("client_connection_id", JDBCType.BIGINT);

        public final SqlColumn<Integer> eventType = column("event_type", JDBCType.INTEGER);

        public final SqlColumn<Integer> notificationType = column("notification_type", JDBCType.INTEGER);

        public final SqlColumn<Long> value = column("value", JDBCType.BIGINT);

        public final SqlColumn<String> text = column("text", JDBCType.VARCHAR);

        public ClientNotificationRecord() {
            super("client_notification");
        }
    }
}