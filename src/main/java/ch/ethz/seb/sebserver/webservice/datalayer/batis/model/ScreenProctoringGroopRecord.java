package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import javax.annotation.Generated;

public class ScreenProctoringGroopRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.exam_id")
    private Long examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.uuid")
    private String uuid;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.name")
    private String name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.size")
    private Integer size;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.data")
    private String data;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.is_fallback")
    private Integer isFallback;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.seb_group_id")
    private Long sebGroupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source Table: screen_proctoring_group")
    public ScreenProctoringGroopRecord(Long id, Long examId, String uuid, String name, Integer size, String data, Integer isFallback, Long sebGroupId) {
        this.id = id;
        this.examId = examId;
        this.uuid = uuid;
        this.name = name;
        this.size = size;
        this.data = data;
        this.isFallback = isFallback;
        this.sebGroupId = sebGroupId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.exam_id")
    public Long getExamId() {
        return examId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.uuid")
    public String getUuid() {
        return uuid;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.name")
    public String getName() {
        return name;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.size")
    public Integer getSize() {
        return size;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.data")
    public String getData() {
        return data;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.is_fallback")
    public Integer getIsFallback() {
        return isFallback;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2025-03-04T15:02:47.223+01:00", comments="Source field: screen_proctoring_group.seb_group_id")
    public Long getSebGroupId() {
        return sebGroupId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table screen_proctoring_group
     *
     * @mbg.generated Tue Mar 04 15:02:47 CET 2025
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", examId=").append(examId);
        sb.append(", uuid=").append(uuid);
        sb.append(", name=").append(name);
        sb.append(", size=").append(size);
        sb.append(", data=").append(data);
        sb.append(", isFallback=").append(isFallback);
        sb.append(", sebGroupId=").append(sebGroupId);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table screen_proctoring_group
     *
     * @mbg.generated Tue Mar 04 15:02:47 CET 2025
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
        ScreenProctoringGroopRecord other = (ScreenProctoringGroopRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getExamId() == null ? other.getExamId() == null : this.getExamId().equals(other.getExamId()))
            && (this.getUuid() == null ? other.getUuid() == null : this.getUuid().equals(other.getUuid()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getSize() == null ? other.getSize() == null : this.getSize().equals(other.getSize()))
            && (this.getData() == null ? other.getData() == null : this.getData().equals(other.getData()))
            && (this.getIsFallback() == null ? other.getIsFallback() == null : this.getIsFallback().equals(other.getIsFallback()))
            && (this.getSebGroupId() == null ? other.getSebGroupId() == null : this.getSebGroupId().equals(other.getSebGroupId()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table screen_proctoring_group
     *
     * @mbg.generated Tue Mar 04 15:02:47 CET 2025
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getExamId() == null) ? 0 : getExamId().hashCode());
        result = prime * result + ((getUuid() == null) ? 0 : getUuid().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getSize() == null) ? 0 : getSize().hashCode());
        result = prime * result + ((getData() == null) ? 0 : getData().hashCode());
        result = prime * result + ((getIsFallback() == null) ? 0 : getIsFallback().hashCode());
        result = prime * result + ((getSebGroupId() == null) ? 0 : getSebGroupId().hashCode());
        return result;
    }
}