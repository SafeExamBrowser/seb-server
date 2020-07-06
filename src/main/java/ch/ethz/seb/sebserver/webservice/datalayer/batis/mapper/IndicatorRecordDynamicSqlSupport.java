package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class IndicatorRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-07-06T10:44:01.023+02:00", comments="Source Table: indicator")
    public static final IndicatorRecord indicatorRecord = new IndicatorRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-07-06T10:44:01.023+02:00", comments="Source field: indicator.id")
    public static final SqlColumn<Long> id = indicatorRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-07-06T10:44:01.023+02:00", comments="Source field: indicator.exam_id")
    public static final SqlColumn<Long> examId = indicatorRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-07-06T10:44:01.023+02:00", comments="Source field: indicator.type")
    public static final SqlColumn<String> type = indicatorRecord.type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-07-06T10:44:01.023+02:00", comments="Source field: indicator.name")
    public static final SqlColumn<String> name = indicatorRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-07-06T10:44:01.024+02:00", comments="Source field: indicator.color")
    public static final SqlColumn<String> color = indicatorRecord.color;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-07-06T10:44:01.023+02:00", comments="Source Table: indicator")
    public static final class IndicatorRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<String> type = column("type", JDBCType.VARCHAR);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> color = column("color", JDBCType.VARCHAR);

        public IndicatorRecord() {
            super("indicator");
        }
    }
}