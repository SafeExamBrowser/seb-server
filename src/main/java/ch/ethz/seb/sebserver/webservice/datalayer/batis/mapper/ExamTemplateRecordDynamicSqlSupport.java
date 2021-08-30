package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ExamTemplateRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-08-30T15:10:38+02:00", comments="Source Table: exam_template")
    public static final ExamTemplateRecord examTemplateRecord = new ExamTemplateRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-08-30T15:10:38.001+02:00", comments="Source field: exam_template.id")
    public static final SqlColumn<Long> id = examTemplateRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-08-30T15:10:38.001+02:00", comments="Source field: exam_template.institution_id")
    public static final SqlColumn<Long> institutionId = examTemplateRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-08-30T15:10:38.001+02:00", comments="Source field: exam_template.configuration_template_id")
    public static final SqlColumn<Long> configurationTemplateId = examTemplateRecord.configurationTemplateId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-08-30T15:10:38.001+02:00", comments="Source field: exam_template.name")
    public static final SqlColumn<String> name = examTemplateRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-08-30T15:10:38.001+02:00", comments="Source field: exam_template.description")
    public static final SqlColumn<String> description = examTemplateRecord.description;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-08-30T15:10:38.001+02:00", comments="Source field: exam_template.indicators_json")
    public static final SqlColumn<String> indicatorsJson = examTemplateRecord.indicatorsJson;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-08-30T15:10:38.001+02:00", comments="Source field: exam_template.other_exam_attributes_json")
    public static final SqlColumn<String> otherExamAttributesJson = examTemplateRecord.otherExamAttributesJson;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-08-30T15:10:38+02:00", comments="Source Table: exam_template")
    public static final class ExamTemplateRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<Long> configurationTemplateId = column("configuration_template_id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> description = column("description", JDBCType.VARCHAR);

        public final SqlColumn<String> indicatorsJson = column("indicators_json", JDBCType.VARCHAR);

        public final SqlColumn<String> otherExamAttributesJson = column("other_exam_attributes_json", JDBCType.VARCHAR);

        public ExamTemplateRecord() {
            super("exam_template");
        }
    }
}