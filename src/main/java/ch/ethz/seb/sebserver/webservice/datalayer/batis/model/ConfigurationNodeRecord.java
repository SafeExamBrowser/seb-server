package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import javax.annotation.Generated;

public class ConfigurationNodeRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.institution_id")
    private Long institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.template_id")
    private Long templateId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.owner")
    private String owner;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.name")
    private String name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.description")
    private String description;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.type")
    private String type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.615+01:00", comments="Source field: configuration_node.status")
    private String status;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.615+01:00", comments="Source field: configuration_node.last_update_time")
    private Long lastUpdateTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.615+01:00", comments="Source field: configuration_node.last_update_user")
    private String lastUpdateUser;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source Table: configuration_node")
    public ConfigurationNodeRecord(Long id, Long institutionId, Long templateId, String owner, String name, String description, String type, String status, Long lastUpdateTime, String lastUpdateUser) {
        this.id = id;
        this.institutionId = institutionId;
        this.templateId = templateId;
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = status;
        this.lastUpdateTime = lastUpdateTime;
        this.lastUpdateUser = lastUpdateUser;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.institution_id")
    public Long getInstitutionId() {
        return institutionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.template_id")
    public Long getTemplateId() {
        return templateId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.owner")
    public String getOwner() {
        return owner;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.name")
    public String getName() {
        return name;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.description")
    public String getDescription() {
        return description;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.614+01:00", comments="Source field: configuration_node.type")
    public String getType() {
        return type;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.615+01:00", comments="Source field: configuration_node.status")
    public String getStatus() {
        return status;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.615+01:00", comments="Source field: configuration_node.last_update_time")
    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-10-30T11:40:34.615+01:00", comments="Source field: configuration_node.last_update_user")
    public String getLastUpdateUser() {
        return lastUpdateUser;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table configuration_node
     *
     * @mbg.generated Wed Oct 30 11:40:34 CET 2024
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", institutionId=").append(institutionId);
        sb.append(", templateId=").append(templateId);
        sb.append(", owner=").append(owner);
        sb.append(", name=").append(name);
        sb.append(", description=").append(description);
        sb.append(", type=").append(type);
        sb.append(", status=").append(status);
        sb.append(", lastUpdateTime=").append(lastUpdateTime);
        sb.append(", lastUpdateUser=").append(lastUpdateUser);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table configuration_node
     *
     * @mbg.generated Wed Oct 30 11:40:34 CET 2024
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
        ConfigurationNodeRecord other = (ConfigurationNodeRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getInstitutionId() == null ? other.getInstitutionId() == null : this.getInstitutionId().equals(other.getInstitutionId()))
            && (this.getTemplateId() == null ? other.getTemplateId() == null : this.getTemplateId().equals(other.getTemplateId()))
            && (this.getOwner() == null ? other.getOwner() == null : this.getOwner().equals(other.getOwner()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getLastUpdateTime() == null ? other.getLastUpdateTime() == null : this.getLastUpdateTime().equals(other.getLastUpdateTime()))
            && (this.getLastUpdateUser() == null ? other.getLastUpdateUser() == null : this.getLastUpdateUser().equals(other.getLastUpdateUser()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table configuration_node
     *
     * @mbg.generated Wed Oct 30 11:40:34 CET 2024
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getInstitutionId() == null) ? 0 : getInstitutionId().hashCode());
        result = prime * result + ((getTemplateId() == null) ? 0 : getTemplateId().hashCode());
        result = prime * result + ((getOwner() == null) ? 0 : getOwner().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getLastUpdateTime() == null) ? 0 : getLastUpdateTime().hashCode());
        result = prime * result + ((getLastUpdateUser() == null) ? 0 : getLastUpdateUser().hashCode());
        return result;
    }
}