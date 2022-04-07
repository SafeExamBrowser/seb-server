package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ClientIndicatorRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.079+02:00", comments="Source Table: client_indicator")
    public static final ClientIndicatorRecord clientIndicatorRecord = new ClientIndicatorRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.079+02:00", comments="Source field: client_indicator.id")
    public static final SqlColumn<Long> id = clientIndicatorRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.079+02:00", comments="Source field: client_indicator.client_connection_id")
    public static final SqlColumn<Long> clientConnectionId = clientIndicatorRecord.clientConnectionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.079+02:00", comments="Source field: client_indicator.type")
    public static final SqlColumn<Integer> type = clientIndicatorRecord.type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.079+02:00", comments="Source field: client_indicator.value")
    public static final SqlColumn<Long> value = clientIndicatorRecord.value;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.079+02:00", comments="Source Table: client_indicator")
    public static final class ClientIndicatorRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> clientConnectionId = column("client_connection_id", JDBCType.BIGINT);

        public final SqlColumn<Integer> type = column("type", JDBCType.INTEGER);

        public final SqlColumn<Long> value = column("value", JDBCType.BIGINT);

        public ClientIndicatorRecord() {
            super("client_indicator");
        }
    }
}