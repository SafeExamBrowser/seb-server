package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ExamConfigurationMapRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-08-24T13:24:15.739+02:00", comments="Source Table: exam_configuration_map")
    public static final ExamConfigurationMapRecord examConfigurationMapRecord = new ExamConfigurationMapRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-08-24T13:24:15.740+02:00", comments="Source field: exam_configuration_map.id")
    public static final SqlColumn<Long> id = examConfigurationMapRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-08-24T13:24:15.740+02:00", comments="Source field: exam_configuration_map.institution_id")
    public static final SqlColumn<Long> institutionId = examConfigurationMapRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-08-24T13:24:15.740+02:00", comments="Source field: exam_configuration_map.exam_id")
    public static final SqlColumn<Long> examId = examConfigurationMapRecord.examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-08-24T13:24:15.740+02:00", comments="Source field: exam_configuration_map.configuration_node_id")
    public static final SqlColumn<Long> configurationNodeId = examConfigurationMapRecord.configurationNodeId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-08-24T13:24:15.740+02:00", comments="Source field: exam_configuration_map.encrypt_secret")
    public static final SqlColumn<String> encryptSecret = examConfigurationMapRecord.encryptSecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-08-24T13:24:15.740+02:00", comments="Source field: exam_configuration_map.client_group_id")
    public static final SqlColumn<Long> clientGroupId = examConfigurationMapRecord.clientGroupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-08-24T13:24:15.740+02:00", comments="Source Table: exam_configuration_map")
    public static final class ExamConfigurationMapRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<Long> examId = column("exam_id", JDBCType.BIGINT);

        public final SqlColumn<Long> configurationNodeId = column("configuration_node_id", JDBCType.BIGINT);

        public final SqlColumn<String> encryptSecret = column("encrypt_secret", JDBCType.VARCHAR);

        public final SqlColumn<Long> clientGroupId = column("client_group_id", JDBCType.BIGINT);

        public ExamConfigurationMapRecord() {
            super("exam_configuration_map");
        }
    }
}