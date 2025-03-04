package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ScreenProctoringGroopRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source Table: screen_proctoring_group")
    public static final ScreenProctoringGroopRecord screenProctoringGroopRecord = new ScreenProctoringGroopRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.id")
    public static final SqlColumn<Long> id = screenProctoringGroopRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.exam_id")
    public static final SqlColumn<Long> examId = screenProctoringGroopRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.uuid")
    public static final SqlColumn<String> uuid = screenProctoringGroopRecord.uuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.name")
    public static final SqlColumn<String> name = screenProctoringGroopRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.size")
    public static final SqlColumn<Integer> size = screenProctoringGroopRecord.size;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.data")
    public static final SqlColumn<String> data = screenProctoringGroopRecord.data;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.is_fallback")
    public static final SqlColumn<Integer> isFallback = screenProctoringGroopRecord.isFallback;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.seb_group_id")
    public static final SqlColumn<Long> sebGroupId = screenProctoringGroopRecord.sebGroupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source Table: screen_proctoring_group")
    public static final class ScreenProctoringGroopRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<String> uuid = column("uuid", JDBCType.VARCHAR);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<Integer> size = column("size", JDBCType.INTEGER);

        public final SqlColumn<String> data = column("data", JDBCType.VARCHAR);

        public final SqlColumn<Integer> isFallback = column("is_fallback", JDBCType.INTEGER);

        public final SqlColumn<Long> sebGroupId = column("seb_group_id", JDBCType.BIGINT);

        public ScreenProctoringGroopRecord() {
            super("screen_proctoring_group");
        }
    }
}