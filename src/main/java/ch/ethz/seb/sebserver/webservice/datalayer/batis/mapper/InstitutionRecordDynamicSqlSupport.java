package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class InstitutionRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T12:08:57.182+01:00", comments="Source Table: institution")
    public static final InstitutionRecord institutionRecord = new InstitutionRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T12:08:57.183+01:00", comments="Source field: institution.id")
    public static final SqlColumn<Long> id = institutionRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T12:08:57.183+01:00", comments="Source field: institution.name")
    public static final SqlColumn<String> name = institutionRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T12:08:57.183+01:00", comments="Source field: institution.authtype")
    public static final SqlColumn<String> authtype = institutionRecord.authtype;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T12:08:57.183+01:00", comments="Source field: institution.active")
    public static final SqlColumn<Integer> active = institutionRecord.active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2018-12-03T12:08:57.183+01:00", comments="Source Table: institution")
    public static final class InstitutionRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> authtype = column("authtype", JDBCType.VARCHAR);

        public final SqlColumn<Integer> active = column("active", JDBCType.INTEGER);

        public InstitutionRecord() {
            super("institution");
        }
    }
}