package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import javax.annotation.Generated;

public class ExamRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.institution_id")
    private Long institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.lms_setup_id")
    private Long lmsSetupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.external_id")
    private String externalId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.owner")
    private String owner;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.supporter")
    private String supporter;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.type")
    private String type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.quit_password")
    private String quitPassword;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.browser_keys")
    private String browserKeys;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.status")
    private String status;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.lms_seb_restriction")
    private Integer lmsSebRestriction;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.updating")
    private Integer updating;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.lastupdate")
    private String lastupdate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.active")
    private Integer active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.132+01:00", comments="Source field: exam.exam_template_id")
    private Long examTemplateId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.132+01:00", comments="Source field: exam.last_modified")
    private Long lastModified;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.128+01:00", comments="Source Table: exam")
    public ExamRecord(Long id, Long institutionId, Long lmsSetupId, String externalId, String owner, String supporter, String type, String quitPassword, String browserKeys, String status, Integer lmsSebRestriction, Integer updating, String lastupdate, Integer active, Long examTemplateId, Long lastModified) {
        this.id = id;
        this.institutionId = institutionId;
        this.lmsSetupId = lmsSetupId;
        this.externalId = externalId;
        this.owner = owner;
        this.supporter = supporter;
        this.type = type;
        this.quitPassword = quitPassword;
        this.browserKeys = browserKeys;
        this.status = status;
        this.lmsSebRestriction = lmsSebRestriction;
        this.updating = updating;
        this.lastupdate = lastupdate;
        this.active = active;
        this.examTemplateId = examTemplateId;
        this.lastModified = lastModified;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.institution_id")
    public Long getInstitutionId() {
        return institutionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.lms_setup_id")
    public Long getLmsSetupId() {
        return lmsSetupId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.external_id")
    public String getExternalId() {
        return externalId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.owner")
    public String getOwner() {
        return owner;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.supporter")
    public String getSupporter() {
        return supporter;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.130+01:00", comments="Source field: exam.type")
    public String getType() {
        return type;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.quit_password")
    public String getQuitPassword() {
        return quitPassword;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.browser_keys")
    public String getBrowserKeys() {
        return browserKeys;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.status")
    public String getStatus() {
        return status;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.lms_seb_restriction")
    public Integer getLmsSebRestriction() {
        return lmsSebRestriction;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.updating")
    public Integer getUpdating() {
        return updating;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.131+01:00", comments="Source field: exam.lastupdate")
    public String getLastupdate() {
        return lastupdate;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.132+01:00", comments="Source field: exam.active")
    public Integer getActive() {
        return active;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.132+01:00", comments="Source field: exam.exam_template_id")
    public Long getExamTemplateId() {
        return examTemplateId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.132+01:00", comments="Source field: exam.last_modified")
    public Long getLastModified() {
        return lastModified;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table exam
     *
     * @mbg.generated Tue Jan 18 17:36:21 CET 2022
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", institutionId=").append(institutionId);
        sb.append(", lmsSetupId=").append(lmsSetupId);
        sb.append(", externalId=").append(externalId);
        sb.append(", owner=").append(owner);
        sb.append(", supporter=").append(supporter);
        sb.append(", type=").append(type);
        sb.append(", quitPassword=").append(quitPassword);
        sb.append(", browserKeys=").append(browserKeys);
        sb.append(", status=").append(status);
        sb.append(", lmsSebRestriction=").append(lmsSebRestriction);
        sb.append(", updating=").append(updating);
        sb.append(", lastupdate=").append(lastupdate);
        sb.append(", active=").append(active);
        sb.append(", examTemplateId=").append(examTemplateId);
        sb.append(", lastModified=").append(lastModified);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table exam
     *
     * @mbg.generated Tue Jan 18 17:36:21 CET 2022
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
        ExamRecord other = (ExamRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getInstitutionId() == null ? other.getInstitutionId() == null : this.getInstitutionId().equals(other.getInstitutionId()))
            && (this.getLmsSetupId() == null ? other.getLmsSetupId() == null : this.getLmsSetupId().equals(other.getLmsSetupId()))
            && (this.getExternalId() == null ? other.getExternalId() == null : this.getExternalId().equals(other.getExternalId()))
            && (this.getOwner() == null ? other.getOwner() == null : this.getOwner().equals(other.getOwner()))
            && (this.getSupporter() == null ? other.getSupporter() == null : this.getSupporter().equals(other.getSupporter()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getQuitPassword() == null ? other.getQuitPassword() == null : this.getQuitPassword().equals(other.getQuitPassword()))
            && (this.getBrowserKeys() == null ? other.getBrowserKeys() == null : this.getBrowserKeys().equals(other.getBrowserKeys()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getLmsSebRestriction() == null ? other.getLmsSebRestriction() == null : this.getLmsSebRestriction().equals(other.getLmsSebRestriction()))
            && (this.getUpdating() == null ? other.getUpdating() == null : this.getUpdating().equals(other.getUpdating()))
            && (this.getLastupdate() == null ? other.getLastupdate() == null : this.getLastupdate().equals(other.getLastupdate()))
            && (this.getActive() == null ? other.getActive() == null : this.getActive().equals(other.getActive()))
            && (this.getExamTemplateId() == null ? other.getExamTemplateId() == null : this.getExamTemplateId().equals(other.getExamTemplateId()))
            && (this.getLastModified() == null ? other.getLastModified() == null : this.getLastModified().equals(other.getLastModified()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table exam
     *
     * @mbg.generated Tue Jan 18 17:36:21 CET 2022
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getInstitutionId() == null) ? 0 : getInstitutionId().hashCode());
        result = prime * result + ((getLmsSetupId() == null) ? 0 : getLmsSetupId().hashCode());
        result = prime * result + ((getExternalId() == null) ? 0 : getExternalId().hashCode());
        result = prime * result + ((getOwner() == null) ? 0 : getOwner().hashCode());
        result = prime * result + ((getSupporter() == null) ? 0 : getSupporter().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getQuitPassword() == null) ? 0 : getQuitPassword().hashCode());
        result = prime * result + ((getBrowserKeys() == null) ? 0 : getBrowserKeys().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getLmsSebRestriction() == null) ? 0 : getLmsSebRestriction().hashCode());
        result = prime * result + ((getUpdating() == null) ? 0 : getUpdating().hashCode());
        result = prime * result + ((getLastupdate() == null) ? 0 : getLastupdate().hashCode());
        result = prime * result + ((getActive() == null) ? 0 : getActive().hashCode());
        result = prime * result + ((getExamTemplateId() == null) ? 0 : getExamTemplateId().hashCode());
        result = prime * result + ((getLastModified() == null) ? 0 : getLastModified().hashCode());
        return result;
    }
}