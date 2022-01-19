package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.joda.time.DateTime;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class UserRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source Table: user")
    public static final UserRecord userRecord = new UserRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.id")
    public static final SqlColumn<Long> id = userRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.institution_id")
    public static final SqlColumn<Long> institutionId = userRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.uuid")
    public static final SqlColumn<String> uuid = userRecord.uuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.creation_date")
    public static final SqlColumn<DateTime> creationDate = userRecord.creationDate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.name")
    public static final SqlColumn<String> name = userRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.surname")
    public static final SqlColumn<String> surname = userRecord.surname;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.username")
    public static final SqlColumn<String> username = userRecord.username;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.password")
    public static final SqlColumn<String> password = userRecord.password;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.email")
    public static final SqlColumn<String> email = userRecord.email;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.language")
    public static final SqlColumn<String> language = userRecord.language;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.timezone")
    public static final SqlColumn<String> timezone = userRecord.timezone;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source field: user.active")
    public static final SqlColumn<Integer> active = userRecord.active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.193+01:00", comments="Source Table: user")
    public static final class UserRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<String> uuid = column("uuid", JDBCType.VARCHAR);

        public final SqlColumn<DateTime> creationDate = column("creation_date", JDBCType.TIMESTAMP, "ch.ethz.seb.sebserver.webservice.datalayer.batis.JodaTimeTypeResolver");

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> surname = column("surname", JDBCType.VARCHAR);

        public final SqlColumn<String> username = column("username", JDBCType.VARCHAR);

        public final SqlColumn<String> password = column("password", JDBCType.VARCHAR);

        public final SqlColumn<String> email = column("email", JDBCType.VARCHAR);

        public final SqlColumn<String> language = column("language", JDBCType.VARCHAR);

        public final SqlColumn<String> timezone = column("timezone", JDBCType.VARCHAR);

        public final SqlColumn<Integer> active = column("active", JDBCType.INTEGER);

        public UserRecord() {
            super("user");
        }
    }
}