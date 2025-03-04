package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class EntityPrivilegeRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.863+01:00", comments="Source Table: entity_privilege")
    public static final EntityPrivilegeRecord entityPrivilegeRecord = new EntityPrivilegeRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.863+01:00", comments="Source field: entity_privilege.id")
    public static final SqlColumn<Long> id = entityPrivilegeRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.863+01:00", comments="Source field: entity_privilege.entity_type")
    public static final SqlColumn<String> entityType = entityPrivilegeRecord.entityType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.863+01:00", comments="Source field: entity_privilege.entity_id")
    public static final SqlColumn<Long> entityId = entityPrivilegeRecord.entityId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.863+01:00", comments="Source field: entity_privilege.user_uuid")
    public static final SqlColumn<String> userUuid = entityPrivilegeRecord.userUuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.863+01:00", comments="Source field: entity_privilege.privilege_type")
    public static final SqlColumn<Byte> privilegeType = entityPrivilegeRecord.privilegeType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.863+01:00", comments="Source Table: entity_privilege")
    public static final class EntityPrivilegeRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> entityType = column("entity_type", JDBCType.VARCHAR);

        public final SqlColumn<Long> entityId = column("entity_id", JDBCType.BIGINT);

        public final SqlColumn<String> userUuid = column("user_uuid", JDBCType.VARCHAR);

        public final SqlColumn<Byte> privilegeType = column("privilege_type", JDBCType.TINYINT);

        public EntityPrivilegeRecord() {
            super("entity_privilege");
        }
    }
}