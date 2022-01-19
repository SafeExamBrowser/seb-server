package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ExamRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.133+01:00", comments="Source Table: exam")
    public static final ExamRecord examRecord = new ExamRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.133+01:00", comments="Source field: exam.id")
    public static final SqlColumn<Long> id = examRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.133+01:00", comments="Source field: exam.institution_id")
    public static final SqlColumn<Long> institutionId = examRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.133+01:00", comments="Source field: exam.lms_setup_id")
    public static final SqlColumn<Long> lmsSetupId = examRecord.lmsSetupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.133+01:00", comments="Source field: exam.external_id")
    public static final SqlColumn<String> externalId = examRecord.externalId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.133+01:00", comments="Source field: exam.owner")
    public static final SqlColumn<String> owner = examRecord.owner;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.133+01:00", comments="Source field: exam.supporter")
    public static final SqlColumn<String> supporter = examRecord.supporter;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.133+01:00", comments="Source field: exam.type")
    public static final SqlColumn<String> type = examRecord.type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.133+01:00", comments="Source field: exam.quit_password")
    public static final SqlColumn<String> quitPassword = examRecord.quitPassword;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.134+01:00", comments="Source field: exam.browser_keys")
    public static final SqlColumn<String> browserKeys = examRecord.browserKeys;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.134+01:00", comments="Source field: exam.status")
    public static final SqlColumn<String> status = examRecord.status;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.134+01:00", comments="Source field: exam.lms_seb_restriction")
    public static final SqlColumn<Integer> lmsSebRestriction = examRecord.lmsSebRestriction;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.134+01:00", comments="Source field: exam.updating")
    public static final SqlColumn<Integer> updating = examRecord.updating;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.134+01:00", comments="Source field: exam.lastupdate")
    public static final SqlColumn<String> lastupdate = examRecord.lastupdate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.134+01:00", comments="Source field: exam.active")
    public static final SqlColumn<Integer> active = examRecord.active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.134+01:00", comments="Source field: exam.exam_template_id")
    public static final SqlColumn<Long> examTemplateId = examRecord.examTemplateId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.134+01:00", comments="Source field: exam.last_modified")
    public static final SqlColumn<Long> lastModified = examRecord.lastModified;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.133+01:00", comments="Source Table: exam")
    public static final class ExamRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<Long> lmsSetupId = column("lms_setup_id", JDBCType.BIGINT);

        public final SqlColumn<String> externalId = column("external_id", JDBCType.VARCHAR);

        public final SqlColumn<String> owner = column("owner", JDBCType.VARCHAR);

        public final SqlColumn<String> supporter = column("supporter", JDBCType.VARCHAR);

        public final SqlColumn<String> type = column("type", JDBCType.VARCHAR);

        public final SqlColumn<String> quitPassword = column("quit_password", JDBCType.VARCHAR);

        public final SqlColumn<String> browserKeys = column("browser_keys", JDBCType.VARCHAR);

        public final SqlColumn<String> status = column("status", JDBCType.VARCHAR);

        public final SqlColumn<Integer> lmsSebRestriction = column("lms_seb_restriction", JDBCType.INTEGER);

        public final SqlColumn<Integer> updating = column("updating", JDBCType.INTEGER);

        public final SqlColumn<String> lastupdate = column("lastupdate", JDBCType.VARCHAR);

        public final SqlColumn<Integer> active = column("active", JDBCType.INTEGER);

        public final SqlColumn<Long> examTemplateId = column("exam_template_id", JDBCType.BIGINT);

        public final SqlColumn<Long> lastModified = column("last_modified", JDBCType.BIGINT);

        public ExamRecord() {
            super("exam");
        }
    }
}