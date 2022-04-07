package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ClientInstructionRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.016+02:00", comments="Source Table: client_instruction")
    public static final ClientInstructionRecord clientInstructionRecord = new ClientInstructionRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.016+02:00", comments="Source field: client_instruction.id")
    public static final SqlColumn<Long> id = clientInstructionRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.016+02:00", comments="Source field: client_instruction.exam_id")
    public static final SqlColumn<Long> examId = clientInstructionRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.016+02:00", comments="Source field: client_instruction.connection_token")
    public static final SqlColumn<String> connectionToken = clientInstructionRecord.connectionToken;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.016+02:00", comments="Source field: client_instruction.type")
    public static final SqlColumn<String> type = clientInstructionRecord.type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.016+02:00", comments="Source field: client_instruction.attributes")
    public static final SqlColumn<String> attributes = clientInstructionRecord.attributes;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.016+02:00", comments="Source field: client_instruction.needs_confirmation")
    public static final SqlColumn<Integer> needsConfirmation = clientInstructionRecord.needsConfirmation;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.016+02:00", comments="Source field: client_instruction.timestamp")
    public static final SqlColumn<Long> timestamp = clientInstructionRecord.timestamp;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.016+02:00", comments="Source Table: client_instruction")
    public static final class ClientInstructionRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<String> connectionToken = column("connection_token", JDBCType.VARCHAR);

        public final SqlColumn<String> type = column("type", JDBCType.VARCHAR);

        public final SqlColumn<String> attributes = column("attributes", JDBCType.VARCHAR);

        public final SqlColumn<Integer> needsConfirmation = column("needs_confirmation", JDBCType.INTEGER);

        public final SqlColumn<Long> timestamp = column("timestamp", JDBCType.BIGINT);

        public ClientInstructionRecord() {
            super("client_instruction");
        }
    }
}