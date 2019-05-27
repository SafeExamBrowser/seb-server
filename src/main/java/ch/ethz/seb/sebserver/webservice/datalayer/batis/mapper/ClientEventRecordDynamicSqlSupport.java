package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.math.BigDecimal;
import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ClientEventRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.438+02:00", comments="Source Table: client_event")
    public static final ClientEventRecord clientEventRecord = new ClientEventRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.438+02:00", comments="Source field: client_event.id")
    public static final SqlColumn<Long> id = clientEventRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.438+02:00", comments="Source field: client_event.connection_id")
    public static final SqlColumn<Long> connectionId = clientEventRecord.connectionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.438+02:00", comments="Source field: client_event.user_identifier")
    public static final SqlColumn<String> userIdentifier = clientEventRecord.userIdentifier;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.438+02:00", comments="Source field: client_event.type")
    public static final SqlColumn<Integer> type = clientEventRecord.type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.438+02:00", comments="Source field: client_event.timestamp")
    public static final SqlColumn<Long> timestamp = clientEventRecord.timestamp;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.438+02:00", comments="Source field: client_event.numeric_value")
    public static final SqlColumn<BigDecimal> numericValue = clientEventRecord.numericValue;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.438+02:00", comments="Source field: client_event.text")
    public static final SqlColumn<String> text = clientEventRecord.text;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-05-27T08:45:56.438+02:00", comments="Source Table: client_event")
    public static final class ClientEventRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> connectionId = column("connection_id", JDBCType.BIGINT);

        public final SqlColumn<String> userIdentifier = column("user_identifier", JDBCType.VARCHAR);

        public final SqlColumn<Integer> type = column("type", JDBCType.INTEGER);

        public final SqlColumn<Long> timestamp = column("timestamp", JDBCType.BIGINT);

        public final SqlColumn<BigDecimal> numericValue = column("numeric_value", JDBCType.DECIMAL);

        public final SqlColumn<String> text = column("text", JDBCType.VARCHAR);

        public ClientEventRecord() {
            super("client_event");
        }
    }
}