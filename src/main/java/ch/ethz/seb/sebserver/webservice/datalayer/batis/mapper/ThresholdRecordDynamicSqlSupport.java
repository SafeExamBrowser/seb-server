package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.math.BigDecimal;
import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ThresholdRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.170+01:00", comments="Source Table: threshold")
    public static final ThresholdRecord thresholdRecord = new ThresholdRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.170+01:00", comments="Source field: threshold.id")
    public static final SqlColumn<Long> id = thresholdRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.170+01:00", comments="Source field: threshold.indicator_id")
    public static final SqlColumn<Long> indicatorId = thresholdRecord.indicatorId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.170+01:00", comments="Source field: threshold.value")
    public static final SqlColumn<BigDecimal> value = thresholdRecord.value;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.171+01:00", comments="Source field: threshold.color")
    public static final SqlColumn<String> color = thresholdRecord.color;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.171+01:00", comments="Source field: threshold.icon")
    public static final SqlColumn<String> icon = thresholdRecord.icon;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.170+01:00", comments="Source Table: threshold")
    public static final class ThresholdRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> indicatorId = column("indicator_id", JDBCType.BIGINT);

        public final SqlColumn<BigDecimal> value = column("value", JDBCType.DECIMAL);

        public final SqlColumn<String> color = column("color", JDBCType.VARCHAR);

        public final SqlColumn<String> icon = column("icon", JDBCType.VARCHAR);

        public ThresholdRecord() {
            super("threshold");
        }
    }
}