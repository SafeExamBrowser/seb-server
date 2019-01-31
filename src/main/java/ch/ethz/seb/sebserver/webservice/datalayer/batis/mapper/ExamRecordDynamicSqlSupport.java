package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ExamRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.576+01:00", comments="Source Table: exam")
    public static final ExamRecord examRecord = new ExamRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.576+01:00", comments="Source field: exam.id")
    public static final SqlColumn<Long> id = examRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.577+01:00", comments="Source field: exam.institution_id")
    public static final SqlColumn<Long> institutionId = examRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.577+01:00", comments="Source field: exam.lms_setup_id")
    public static final SqlColumn<Long> lmsSetupId = examRecord.lmsSetupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.577+01:00", comments="Source field: exam.external_id")
    public static final SqlColumn<String> externalId = examRecord.externalId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.577+01:00", comments="Source field: exam.owner")
    public static final SqlColumn<String> owner = examRecord.owner;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.577+01:00", comments="Source field: exam.supporter")
    public static final SqlColumn<String> supporter = examRecord.supporter;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.577+01:00", comments="Source field: exam.type")
    public static final SqlColumn<String> type = examRecord.type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.578+01:00", comments="Source field: exam.status")
    public static final SqlColumn<String> status = examRecord.status;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.578+01:00", comments="Source field: exam.quit_password")
    public static final SqlColumn<String> quitPassword = examRecord.quitPassword;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.578+01:00", comments="Source field: exam.active")
    public static final SqlColumn<Integer> active = examRecord.active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-01-31T09:18:13.576+01:00", comments="Source Table: exam")
    public static final class ExamRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<Long> lmsSetupId = column("lms_setup_id", JDBCType.BIGINT);

        public final SqlColumn<String> externalId = column("external_id", JDBCType.VARCHAR);

        public final SqlColumn<String> owner = column("owner", JDBCType.VARCHAR);

        public final SqlColumn<String> supporter = column("supporter", JDBCType.VARCHAR);

        public final SqlColumn<String> type = column("type", JDBCType.VARCHAR);

        public final SqlColumn<String> status = column("status", JDBCType.VARCHAR);

        public final SqlColumn<String> quitPassword = column("quit_password", JDBCType.VARCHAR);

        public final SqlColumn<Integer> active = column("active", JDBCType.INTEGER);

        public ExamRecord() {
            super("exam");
        }
    }
}