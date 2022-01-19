package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class RoleRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.197+01:00", comments="Source Table: user_role")
    public static final RoleRecord roleRecord = new RoleRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.197+01:00", comments="Source field: user_role.id")
    public static final SqlColumn<Long> id = roleRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.198+01:00", comments="Source field: user_role.user_id")
    public static final SqlColumn<Long> userId = roleRecord.userId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.198+01:00", comments="Source field: user_role.role_name")
    public static final SqlColumn<String> roleName = roleRecord.roleName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.197+01:00", comments="Source Table: user_role")
    public static final class RoleRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> userId = column("user_id", JDBCType.BIGINT);

        public final SqlColumn<String> roleName = column("role_name", JDBCType.VARCHAR);

        public RoleRecord() {
            super("user_role");
        }
    }
}