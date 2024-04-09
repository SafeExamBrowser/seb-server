package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ClientGroupRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-09T13:46:22.422+02:00", comments="Source Table: client_group")
    public static final ClientGroupRecord clientGroupRecord = new ClientGroupRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-09T13:46:22.422+02:00", comments="Source field: client_group.id")
    public static final SqlColumn<Long> id = clientGroupRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-09T13:46:22.422+02:00", comments="Source field: client_group.exam_id")
    public static final SqlColumn<Long> examId = clientGroupRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-09T13:46:22.422+02:00", comments="Source field: client_group.name")
    public static final SqlColumn<String> name = clientGroupRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-09T13:46:22.422+02:00", comments="Source field: client_group.type")
    public static final SqlColumn<String> type = clientGroupRecord.type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-09T13:46:22.422+02:00", comments="Source field: client_group.color")
    public static final SqlColumn<String> color = clientGroupRecord.color;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-09T13:46:22.422+02:00", comments="Source field: client_group.icon")
    public static final SqlColumn<String> icon = clientGroupRecord.icon;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-09T13:46:22.422+02:00", comments="Source field: client_group.data")
    public static final SqlColumn<String> data = clientGroupRecord.data;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-09T13:46:22.422+02:00", comments="Source Table: client_group")
    public static final class ClientGroupRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> type = column("type", JDBCType.VARCHAR);

        public final SqlColumn<String> color = column("color", JDBCType.VARCHAR);

        public final SqlColumn<String> icon = column("icon", JDBCType.VARCHAR);

        public final SqlColumn<String> data = column("data", JDBCType.VARCHAR);

        public ClientGroupRecord() {
            super("client_group");
        }
    }
}