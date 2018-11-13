package ch.ethz.seb.sebserver.ws.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ExamRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.530+01:00", comments="Source Table: exam")
    public static final ExamRecord examRecord = new ExamRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.530+01:00", comments="Source field: exam.id")
    public static final SqlColumn<Long> id = examRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.531+01:00", comments="Source field: exam.lms_setup_id")
    public static final SqlColumn<Long> lmsSetupId = examRecord.lmsSetupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.531+01:00", comments="Source field: exam.external_uuid")
    public static final SqlColumn<String> externalUuid = examRecord.externalUuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.531+01:00", comments="Source field: exam.owner")
    public static final SqlColumn<String> owner = examRecord.owner;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.531+01:00", comments="Source field: exam.supporter")
    public static final SqlColumn<String> supporter = examRecord.supporter;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.531+01:00", comments="Source field: exam.type")
    public static final SqlColumn<String> type = examRecord.type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-11-12T16:16:23.530+01:00", comments="Source Table: exam")
    public static final class ExamRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> lmsSetupId = column("lms_setup_id", JDBCType.BIGINT);

        public final SqlColumn<String> externalUuid = column("external_uuid", JDBCType.VARCHAR);

        public final SqlColumn<String> owner = column("owner", JDBCType.VARCHAR);

        public final SqlColumn<String> supporter = column("supporter", JDBCType.VARCHAR);

        public final SqlColumn<String> type = column("type", JDBCType.VARCHAR);

        public ExamRecord() {
            super("exam");
        }
    }
}