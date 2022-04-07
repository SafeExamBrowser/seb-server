package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class InstitutionRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.034+02:00", comments="Source Table: institution")
    public static final InstitutionRecord institutionRecord = new InstitutionRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.034+02:00", comments="Source field: institution.id")
    public static final SqlColumn<Long> id = institutionRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.035+02:00", comments="Source field: institution.name")
    public static final SqlColumn<String> name = institutionRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.036+02:00", comments="Source field: institution.url_suffix")
    public static final SqlColumn<String> urlSuffix = institutionRecord.urlSuffix;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.037+02:00", comments="Source field: institution.theme_name")
    public static final SqlColumn<String> themeName = institutionRecord.themeName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.037+02:00", comments="Source field: institution.active")
    public static final SqlColumn<Integer> active = institutionRecord.active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.037+02:00", comments="Source field: institution.logo_image")
    public static final SqlColumn<String> logoImage = institutionRecord.logoImage;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.034+02:00", comments="Source Table: institution")
    public static final class InstitutionRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> urlSuffix = column("url_suffix", JDBCType.VARCHAR);

        public final SqlColumn<String> themeName = column("theme_name", JDBCType.VARCHAR);

        public final SqlColumn<Integer> active = column("active", JDBCType.INTEGER);

        public final SqlColumn<String> logoImage = column("logo_image", JDBCType.CLOB);

        public InstitutionRecord() {
            super("institution");
        }
    }
}