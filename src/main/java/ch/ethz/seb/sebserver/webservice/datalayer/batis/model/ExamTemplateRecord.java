package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import javax.annotation.Generated;

public class ExamTemplateRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.institution_id")
    private Long institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.configuration_template_id")
    private Long configurationTemplateId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.name")
    private String name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.description")
    private String description;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.exam_type")
    private String examType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.supporter")
    private String supporter;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.indicator_templates")
    private String indicatorTemplates;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.institutional_default")
    private Integer institutionalDefault;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.lms_integration")
    private Integer lmsIntegration;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.client_configuration_id")
    private Long clientConfigurationId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source Table: exam_template")
    public ExamTemplateRecord(Long id, Long institutionId, Long configurationTemplateId, String name, String description, String examType, String supporter, String indicatorTemplates, Integer institutionalDefault, Integer lmsIntegration, Long clientConfigurationId) {
        this.id = id;
        this.institutionId = institutionId;
        this.configurationTemplateId = configurationTemplateId;
        this.name = name;
        this.description = description;
        this.examType = examType;
        this.supporter = supporter;
        this.indicatorTemplates = indicatorTemplates;
        this.institutionalDefault = institutionalDefault;
        this.lmsIntegration = lmsIntegration;
        this.clientConfigurationId = clientConfigurationId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.institution_id")
    public Long getInstitutionId() {
        return institutionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.configuration_template_id")
    public Long getConfigurationTemplateId() {
        return configurationTemplateId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.name")
    public String getName() {
        return name;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.description")
    public String getDescription() {
        return description;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.exam_type")
    public String getExamType() {
        return examType;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.supporter")
    public String getSupporter() {
        return supporter;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.indicator_templates")
    public String getIndicatorTemplates() {
        return indicatorTemplates;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.institutional_default")
    public Integer getInstitutionalDefault() {
        return institutionalDefault;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.lms_integration")
    public Integer getLmsIntegration() {
        return lmsIntegration;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.031+02:00", comments="Source field: exam_template.client_configuration_id")
    public Long getClientConfigurationId() {
        return clientConfigurationId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table exam_template
     *
     * @mbg.generated Tue Sep 03 11:17:27 CEST 2024
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", institutionId=").append(institutionId);
        sb.append(", configurationTemplateId=").append(configurationTemplateId);
        sb.append(", name=").append(name);
        sb.append(", description=").append(description);
        sb.append(", examType=").append(examType);
        sb.append(", supporter=").append(supporter);
        sb.append(", indicatorTemplates=").append(indicatorTemplates);
        sb.append(", institutionalDefault=").append(institutionalDefault);
        sb.append(", lmsIntegration=").append(lmsIntegration);
        sb.append(", clientConfigurationId=").append(clientConfigurationId);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table exam_template
     *
     * @mbg.generated Tue Sep 03 11:17:27 CEST 2024
     */
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        ExamTemplateRecord other = (ExamTemplateRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getInstitutionId() == null ? other.getInstitutionId() == null : this.getInstitutionId().equals(other.getInstitutionId()))
            && (this.getConfigurationTemplateId() == null ? other.getConfigurationTemplateId() == null : this.getConfigurationTemplateId().equals(other.getConfigurationTemplateId()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getExamType() == null ? other.getExamType() == null : this.getExamType().equals(other.getExamType()))
            && (this.getSupporter() == null ? other.getSupporter() == null : this.getSupporter().equals(other.getSupporter()))
            && (this.getIndicatorTemplates() == null ? other.getIndicatorTemplates() == null : this.getIndicatorTemplates().equals(other.getIndicatorTemplates()))
            && (this.getInstitutionalDefault() == null ? other.getInstitutionalDefault() == null : this.getInstitutionalDefault().equals(other.getInstitutionalDefault()))
            && (this.getLmsIntegration() == null ? other.getLmsIntegration() == null : this.getLmsIntegration().equals(other.getLmsIntegration()))
            && (this.getClientConfigurationId() == null ? other.getClientConfigurationId() == null : this.getClientConfigurationId().equals(other.getClientConfigurationId()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table exam_template
     *
     * @mbg.generated Tue Sep 03 11:17:27 CEST 2024
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getInstitutionId() == null) ? 0 : getInstitutionId().hashCode());
        result = prime * result + ((getConfigurationTemplateId() == null) ? 0 : getConfigurationTemplateId().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getExamType() == null) ? 0 : getExamType().hashCode());
        result = prime * result + ((getSupporter() == null) ? 0 : getSupporter().hashCode());
        result = prime * result + ((getIndicatorTemplates() == null) ? 0 : getIndicatorTemplates().hashCode());
        result = prime * result + ((getInstitutionalDefault() == null) ? 0 : getInstitutionalDefault().hashCode());
        result = prime * result + ((getLmsIntegration() == null) ? 0 : getLmsIntegration().hashCode());
        result = prime * result + ((getClientConfigurationId() == null) ? 0 : getClientConfigurationId().hashCode());
        return result;
    }
}