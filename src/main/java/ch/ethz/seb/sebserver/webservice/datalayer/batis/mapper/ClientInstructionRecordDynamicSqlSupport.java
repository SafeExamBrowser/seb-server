package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ClientInstructionRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.170+01:00", comments="Source Table: client_instruction")
    public static final ClientInstructionRecord clientInstructionRecord = new ClientInstructionRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.170+01:00", comments="Source field: client_instruction.id")
    public static final SqlColumn<Long> id = clientInstructionRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.170+01:00", comments="Source field: client_instruction.client_connection_id")
    public static final SqlColumn<Long> clientConnectionId = clientInstructionRecord.clientConnectionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.170+01:00", comments="Source field: client_instruction.exam_id")
    public static final SqlColumn<Long> examId = clientInstructionRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.170+01:00", comments="Source field: client_instruction.type")
    public static final SqlColumn<String> type = clientInstructionRecord.type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.171+01:00", comments="Source field: client_instruction.attributes")
    public static final SqlColumn<String> attributes = clientInstructionRecord.attributes;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.171+01:00", comments="Source field: client_instruction.active")
    public static final SqlColumn<Integer> active = clientInstructionRecord.active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.170+01:00", comments="Source Table: client_instruction")
    public static final class ClientInstructionRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> clientConnectionId = column("client_connection_id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<String> type = column("type", JDBCType.VARCHAR);

        public final SqlColumn<String> attributes = column("attributes", JDBCType.VARCHAR);

        public final SqlColumn<Integer> active = column("active", JDBCType.INTEGER);

        public ClientInstructionRecord() {
            super("client_instruction");
        }
    }
}