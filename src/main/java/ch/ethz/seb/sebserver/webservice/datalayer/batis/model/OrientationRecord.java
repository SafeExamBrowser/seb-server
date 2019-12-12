package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import javax.annotation.Generated;

public class OrientationRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.136+01:00", comments="Source field: orientation.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.136+01:00", comments="Source field: orientation.config_attribute_id")
    private Long configAttributeId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.136+01:00", comments="Source field: orientation.template_id")
    private Long templateId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.136+01:00", comments="Source field: orientation.view_id")
    private Long viewId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.136+01:00", comments="Source field: orientation.group_id")
    private String groupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.137+01:00", comments="Source field: orientation.x_position")
    private Integer xPosition;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.137+01:00", comments="Source field: orientation.y_position")
    private Integer yPosition;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.137+01:00", comments="Source field: orientation.width")
    private Integer width;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.137+01:00", comments="Source field: orientation.height")
    private Integer height;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.137+01:00", comments="Source field: orientation.title")
    private String title;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.136+01:00", comments="Source Table: orientation")
    public OrientationRecord(Long id, Long configAttributeId, Long templateId, Long viewId, String groupId, Integer xPosition, Integer yPosition, Integer width, Integer height, String title) {
        this.id = id;
        this.configAttributeId = configAttributeId;
        this.templateId = templateId;
        this.viewId = viewId;
        this.groupId = groupId;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.width = width;
        this.height = height;
        this.title = title;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.136+01:00", comments="Source field: orientation.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.136+01:00", comments="Source field: orientation.config_attribute_id")
    public Long getConfigAttributeId() {
        return configAttributeId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.136+01:00", comments="Source field: orientation.template_id")
    public Long getTemplateId() {
        return templateId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.136+01:00", comments="Source field: orientation.view_id")
    public Long getViewId() {
        return viewId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.137+01:00", comments="Source field: orientation.group_id")
    public String getGroupId() {
        return groupId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.137+01:00", comments="Source field: orientation.x_position")
    public Integer getxPosition() {
        return xPosition;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.137+01:00", comments="Source field: orientation.y_position")
    public Integer getyPosition() {
        return yPosition;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.137+01:00", comments="Source field: orientation.width")
    public Integer getWidth() {
        return width;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.137+01:00", comments="Source field: orientation.height")
    public Integer getHeight() {
        return height;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-12-12T11:08:43.137+01:00", comments="Source field: orientation.title")
    public String getTitle() {
        return title;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table orientation
     *
     * @mbg.generated Thu Dec 12 11:08:43 CET 2019
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", configAttributeId=").append(configAttributeId);
        sb.append(", templateId=").append(templateId);
        sb.append(", viewId=").append(viewId);
        sb.append(", groupId=").append(groupId);
        sb.append(", xPosition=").append(xPosition);
        sb.append(", yPosition=").append(yPosition);
        sb.append(", width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", title=").append(title);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table orientation
     *
     * @mbg.generated Thu Dec 12 11:08:43 CET 2019
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
        OrientationRecord other = (OrientationRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getConfigAttributeId() == null ? other.getConfigAttributeId() == null : this.getConfigAttributeId().equals(other.getConfigAttributeId()))
            && (this.getTemplateId() == null ? other.getTemplateId() == null : this.getTemplateId().equals(other.getTemplateId()))
            && (this.getViewId() == null ? other.getViewId() == null : this.getViewId().equals(other.getViewId()))
            && (this.getGroupId() == null ? other.getGroupId() == null : this.getGroupId().equals(other.getGroupId()))
            && (this.getxPosition() == null ? other.getxPosition() == null : this.getxPosition().equals(other.getxPosition()))
            && (this.getyPosition() == null ? other.getyPosition() == null : this.getyPosition().equals(other.getyPosition()))
            && (this.getWidth() == null ? other.getWidth() == null : this.getWidth().equals(other.getWidth()))
            && (this.getHeight() == null ? other.getHeight() == null : this.getHeight().equals(other.getHeight()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table orientation
     *
     * @mbg.generated Thu Dec 12 11:08:43 CET 2019
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getConfigAttributeId() == null) ? 0 : getConfigAttributeId().hashCode());
        result = prime * result + ((getTemplateId() == null) ? 0 : getTemplateId().hashCode());
        result = prime * result + ((getViewId() == null) ? 0 : getViewId().hashCode());
        result = prime * result + ((getGroupId() == null) ? 0 : getGroupId().hashCode());
        result = prime * result + ((getxPosition() == null) ? 0 : getxPosition().hashCode());
        result = prime * result + ((getyPosition() == null) ? 0 : getyPosition().hashCode());
        result = prime * result + ((getWidth() == null) ? 0 : getWidth().hashCode());
        result = prime * result + ((getHeight() == null) ? 0 : getHeight().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        return result;
    }
}