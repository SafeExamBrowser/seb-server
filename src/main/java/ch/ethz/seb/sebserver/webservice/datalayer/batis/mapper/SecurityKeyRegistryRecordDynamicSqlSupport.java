package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class SecurityKeyRegistryRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-03T16:28:08.514+01:00", comments="Source Table: seb_security_key_registry")
    public static final SecurityKeyRegistryRecord securityKeyRegistryRecord = new SecurityKeyRegistryRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-03T16:28:08.514+01:00", comments="Source field: seb_security_key_registry.id")
    public static final SqlColumn<Long> id = securityKeyRegistryRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-03T16:28:08.514+01:00", comments="Source field: seb_security_key_registry.institution_id")
    public static final SqlColumn<Long> institutionId = securityKeyRegistryRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-03T16:28:08.514+01:00", comments="Source field: seb_security_key_registry.type")
    public static final SqlColumn<String> type = securityKeyRegistryRecord.type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-03T16:28:08.514+01:00", comments="Source field: seb_security_key_registry.key")
    public static final SqlColumn<String> key = securityKeyRegistryRecord.key;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-03T16:28:08.514+01:00", comments="Source field: seb_security_key_registry.tag")
    public static final SqlColumn<String> tag = securityKeyRegistryRecord.tag;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-03T16:28:08.514+01:00", comments="Source field: seb_security_key_registry.exam_id")
    public static final SqlColumn<Long> examId = securityKeyRegistryRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-03T16:28:08.514+01:00", comments="Source field: seb_security_key_registry.exam_template_id")
    public static final SqlColumn<Long> examTemplateId = securityKeyRegistryRecord.examTemplateId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-03T16:28:08.514+01:00", comments="Source field: seb_security_key_registry.encryption_type")
    public static final SqlColumn<String> encryptionType = securityKeyRegistryRecord.encryptionType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-11-03T16:28:08.514+01:00", comments="Source Table: seb_security_key_registry")
    public static final class SecurityKeyRegistryRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<String> type = column("type", JDBCType.VARCHAR);

        public final SqlColumn<String> key = column("key", JDBCType.VARCHAR);

        public final SqlColumn<String> tag = column("tag", JDBCType.VARCHAR);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<Long> examTemplateId = column("exam_template_id", JDBCType.BIGINT);

        public final SqlColumn<String> encryptionType = column("encryption_type", JDBCType.VARCHAR);

        public SecurityKeyRegistryRecord() {
            super("seb_security_key_registry");
        }
    }
}