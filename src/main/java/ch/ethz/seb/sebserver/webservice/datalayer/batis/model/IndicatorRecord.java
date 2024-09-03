package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import javax.annotation.Generated;

public class IndicatorRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.exam_id")
    private Long examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.type")
    private String type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.name")
    private String name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.color")
    private String color;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.icon")
    private String icon;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.tags")
    private String tags;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source Table: indicator")
    public IndicatorRecord(Long id, Long examId, String type, String name, String color, String icon, String tags) {
        this.id = id;
        this.examId = examId;
        this.type = type;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.tags = tags;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.exam_id")
    public Long getExamId() {
        return examId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.type")
    public String getType() {
        return type;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.name")
    public String getName() {
        return name;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.color")
    public String getColor() {
        return color;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.icon")
    public String getIcon() {
        return icon;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-09-03T11:17:27.019+02:00", comments="Source field: indicator.tags")
    public String getTags() {
        return tags;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table indicator
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
        sb.append(", examId=").append(examId);
        sb.append(", type=").append(type);
        sb.append(", name=").append(name);
        sb.append(", color=").append(color);
        sb.append(", icon=").append(icon);
        sb.append(", tags=").append(tags);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table indicator
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
        IndicatorRecord other = (IndicatorRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getExamId() == null ? other.getExamId() == null : this.getExamId().equals(other.getExamId()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getColor() == null ? other.getColor() == null : this.getColor().equals(other.getColor()))
            && (this.getIcon() == null ? other.getIcon() == null : this.getIcon().equals(other.getIcon()))
            && (this.getTags() == null ? other.getTags() == null : this.getTags().equals(other.getTags()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table indicator
     *
     * @mbg.generated Tue Sep 03 11:17:27 CEST 2024
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getExamId() == null) ? 0 : getExamId().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getColor() == null) ? 0 : getColor().hashCode());
        result = prime * result + ((getIcon() == null) ? 0 : getIcon().hashCode());
        result = prime * result + ((getTags() == null) ? 0 : getTags().hashCode());
        return result;
    }
}