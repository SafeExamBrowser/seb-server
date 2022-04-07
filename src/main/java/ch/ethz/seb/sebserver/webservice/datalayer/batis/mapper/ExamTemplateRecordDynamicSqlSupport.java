package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ExamTemplateRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.073+02:00", comments="Source Table: exam_template")
    public static final ExamTemplateRecord examTemplateRecord = new ExamTemplateRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.073+02:00", comments="Source field: exam_template.id")
    public static final SqlColumn<Long> id = examTemplateRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.073+02:00", comments="Source field: exam_template.institution_id")
    public static final SqlColumn<Long> institutionId = examTemplateRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.073+02:00", comments="Source field: exam_template.configuration_template_id")
    public static final SqlColumn<Long> configurationTemplateId = examTemplateRecord.configurationTemplateId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.073+02:00", comments="Source field: exam_template.name")
    public static final SqlColumn<String> name = examTemplateRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.073+02:00", comments="Source field: exam_template.description")
    public static final SqlColumn<String> description = examTemplateRecord.description;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.073+02:00", comments="Source field: exam_template.exam_type")
    public static final SqlColumn<String> examType = examTemplateRecord.examType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.073+02:00", comments="Source field: exam_template.supporter")
    public static final SqlColumn<String> supporter = examTemplateRecord.supporter;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.073+02:00", comments="Source field: exam_template.indicator_templates")
    public static final SqlColumn<String> indicatorTemplates = examTemplateRecord.indicatorTemplates;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.073+02:00", comments="Source field: exam_template.institutional_default")
    public static final SqlColumn<Integer> institutionalDefault = examTemplateRecord.institutionalDefault;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.073+02:00", comments="Source Table: exam_template")
    public static final class ExamTemplateRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<Long> configurationTemplateId = column("configuration_template_id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<String> description = column("description", JDBCType.VARCHAR);

        public final SqlColumn<String> examType = column("exam_type", JDBCType.VARCHAR);

        public final SqlColumn<String> supporter = column("supporter", JDBCType.VARCHAR);

        public final SqlColumn<String> indicatorTemplates = column("indicator_templates", JDBCType.VARCHAR);

        public final SqlColumn<Integer> institutionalDefault = column("institutional_default", JDBCType.INTEGER);

        public ExamTemplateRecord() {
            super("exam_template");
        }
    }
}