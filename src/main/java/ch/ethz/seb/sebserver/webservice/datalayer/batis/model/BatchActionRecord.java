package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import javax.annotation.Generated;

public class BatchActionRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.141+02:00", comments="Source field: batch_action.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.institution_id")
    private Long institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.owner")
    private String owner;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.action_type")
    private String actionType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.attributes")
    private String attributes;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.source_ids")
    private String sourceIds;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.successful")
    private String successful;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.last_update")
    private Long lastUpdate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.processor_id")
    private String processorId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.141+02:00", comments="Source Table: batch_action")
    public BatchActionRecord(Long id, Long institutionId, String owner, String actionType, String attributes, String sourceIds, String successful, Long lastUpdate, String processorId) {
        this.id = id;
        this.institutionId = institutionId;
        this.owner = owner;
        this.actionType = actionType;
        this.attributes = attributes;
        this.sourceIds = sourceIds;
        this.successful = successful;
        this.lastUpdate = lastUpdate;
        this.processorId = processorId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.141+02:00", comments="Source field: batch_action.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.institution_id")
    public Long getInstitutionId() {
        return institutionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.owner")
    public String getOwner() {
        return owner;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.action_type")
    public String getActionType() {
        return actionType;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.attributes")
    public String getAttributes() {
        return attributes;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.source_ids")
    public String getSourceIds() {
        return sourceIds;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.successful")
    public String getSuccessful() {
        return successful;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.last_update")
    public Long getLastUpdate() {
        return lastUpdate;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-08-17T15:55:00.142+02:00", comments="Source field: batch_action.processor_id")
    public String getProcessorId() {
        return processorId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table batch_action
     *
     * @mbg.generated Wed Aug 17 15:55:00 CEST 2022
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", institutionId=").append(institutionId);
        sb.append(", owner=").append(owner);
        sb.append(", actionType=").append(actionType);
        sb.append(", attributes=").append(attributes);
        sb.append(", sourceIds=").append(sourceIds);
        sb.append(", successful=").append(successful);
        sb.append(", lastUpdate=").append(lastUpdate);
        sb.append(", processorId=").append(processorId);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table batch_action
     *
     * @mbg.generated Wed Aug 17 15:55:00 CEST 2022
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
        BatchActionRecord other = (BatchActionRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getInstitutionId() == null ? other.getInstitutionId() == null : this.getInstitutionId().equals(other.getInstitutionId()))
            && (this.getOwner() == null ? other.getOwner() == null : this.getOwner().equals(other.getOwner()))
            && (this.getActionType() == null ? other.getActionType() == null : this.getActionType().equals(other.getActionType()))
            && (this.getAttributes() == null ? other.getAttributes() == null : this.getAttributes().equals(other.getAttributes()))
            && (this.getSourceIds() == null ? other.getSourceIds() == null : this.getSourceIds().equals(other.getSourceIds()))
            && (this.getSuccessful() == null ? other.getSuccessful() == null : this.getSuccessful().equals(other.getSuccessful()))
            && (this.getLastUpdate() == null ? other.getLastUpdate() == null : this.getLastUpdate().equals(other.getLastUpdate()))
            && (this.getProcessorId() == null ? other.getProcessorId() == null : this.getProcessorId().equals(other.getProcessorId()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table batch_action
     *
     * @mbg.generated Wed Aug 17 15:55:00 CEST 2022
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getInstitutionId() == null) ? 0 : getInstitutionId().hashCode());
        result = prime * result + ((getOwner() == null) ? 0 : getOwner().hashCode());
        result = prime * result + ((getActionType() == null) ? 0 : getActionType().hashCode());
        result = prime * result + ((getAttributes() == null) ? 0 : getAttributes().hashCode());
        result = prime * result + ((getSourceIds() == null) ? 0 : getSourceIds().hashCode());
        result = prime * result + ((getSuccessful() == null) ? 0 : getSuccessful().hashCode());
        result = prime * result + ((getLastUpdate() == null) ? 0 : getLastUpdate().hashCode());
        result = prime * result + ((getProcessorId() == null) ? 0 : getProcessorId().hashCode());
        return result;
    }
}