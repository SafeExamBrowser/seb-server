package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class FeaturePrivilegeRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.992+02:00", comments="Source Table: feature_privilege")
    public static final FeaturePrivilegeRecord featurePrivilegeRecord = new FeaturePrivilegeRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.992+02:00", comments="Source field: feature_privilege.id")
    public static final SqlColumn<Long> id = featurePrivilegeRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.992+02:00", comments="Source field: feature_privilege.feature_id")
    public static final SqlColumn<Long> featureId = featurePrivilegeRecord.featureId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.992+02:00", comments="Source field: feature_privilege.user_uuid")
    public static final SqlColumn<String> userUuid = featurePrivilegeRecord.userUuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-22T15:58:16.992+02:00", comments="Source Table: feature_privilege")
    public static final class FeaturePrivilegeRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> featureId = column("feature_id", JDBCType.BIGINT);

        public final SqlColumn<String> userUuid = column("user_uuid", JDBCType.VARCHAR);

        public FeaturePrivilegeRecord() {
            super("feature_privilege");
        }
    }
}