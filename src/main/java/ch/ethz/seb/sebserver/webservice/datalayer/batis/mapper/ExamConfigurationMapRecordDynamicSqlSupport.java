package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ExamConfigurationMapRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.125+01:00", comments="Source Table: exam_configuration_map")
    public static final ExamConfigurationMapRecord examConfigurationMapRecord = new ExamConfigurationMapRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.126+01:00", comments="Source field: exam_configuration_map.id")
    public static final SqlColumn<Long> id = examConfigurationMapRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.126+01:00", comments="Source field: exam_configuration_map.institution_id")
    public static final SqlColumn<Long> institutionId = examConfigurationMapRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.126+01:00", comments="Source field: exam_configuration_map.exam_id")
    public static final SqlColumn<Long> examId = examConfigurationMapRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.126+01:00", comments="Source field: exam_configuration_map.configuration_node_id")
    public static final SqlColumn<Long> configurationNodeId = examConfigurationMapRecord.configurationNodeId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.126+01:00", comments="Source field: exam_configuration_map.user_names")
    public static final SqlColumn<String> userNames = examConfigurationMapRecord.userNames;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.126+01:00", comments="Source field: exam_configuration_map.encrypt_secret")
    public static final SqlColumn<String> encryptSecret = examConfigurationMapRecord.encryptSecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.126+01:00", comments="Source Table: exam_configuration_map")
    public static final class ExamConfigurationMapRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<Long> configurationNodeId = column("configuration_node_id", JDBCType.BIGINT);

        public final SqlColumn<String> userNames = column("user_names", JDBCType.VARCHAR);

        public final SqlColumn<String> encryptSecret = column("encrypt_secret", JDBCType.VARCHAR);

        public ExamConfigurationMapRecord() {
            super("exam_configuration_map");
        }
    }
}