package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import java.math.BigDecimal;
import javax.annotation.Generated;

public class ClientEventRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.009+02:00", comments="Source field: client_event.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.client_connection_id")
    private Long clientConnectionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.type")
    private Integer type;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.client_time")
    private Long clientTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.server_time")
    private Long serverTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.numeric_value")
    private BigDecimal numericValue;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.text")
    private String text;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.009+02:00", comments="Source Table: client_event")
    public ClientEventRecord(Long id, Long clientConnectionId, Integer type, Long clientTime, Long serverTime, BigDecimal numericValue, String text) {
        this.id = id;
        this.clientConnectionId = clientConnectionId;
        this.type = type;
        this.clientTime = clientTime;
        this.serverTime = serverTime;
        this.numericValue = numericValue;
        this.text = text;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.009+02:00", comments="Source Table: client_event")
    public ClientEventRecord() {
        super();
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.id")
    public void setId(Long id) {
        this.id = id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.client_connection_id")
    public Long getClientConnectionId() {
        return clientConnectionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.client_connection_id")
    public void setClientConnectionId(Long clientConnectionId) {
        this.clientConnectionId = clientConnectionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.type")
    public Integer getType() {
        return type;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.type")
    public void setType(Integer type) {
        this.type = type;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.client_time")
    public Long getClientTime() {
        return clientTime;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.client_time")
    public void setClientTime(Long clientTime) {
        this.clientTime = clientTime;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.server_time")
    public Long getServerTime() {
        return serverTime;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.server_time")
    public void setServerTime(Long serverTime) {
        this.serverTime = serverTime;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.numeric_value")
    public BigDecimal getNumericValue() {
        return numericValue;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.numeric_value")
    public void setNumericValue(BigDecimal numericValue) {
        this.numericValue = numericValue;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.text")
    public String getText() {
        return text;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.010+02:00", comments="Source field: client_event.text")
    public void setText(String text) {
        this.text = text == null ? null : text.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_event
     *
     * @mbg.generated Wed Apr 06 16:51:31 CEST 2022
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", clientConnectionId=").append(clientConnectionId);
        sb.append(", type=").append(type);
        sb.append(", clientTime=").append(clientTime);
        sb.append(", serverTime=").append(serverTime);
        sb.append(", numericValue=").append(numericValue);
        sb.append(", text=").append(text);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_event
     *
     * @mbg.generated Wed Apr 06 16:51:31 CEST 2022
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
        ClientEventRecord other = (ClientEventRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getClientConnectionId() == null ? other.getClientConnectionId() == null : this.getClientConnectionId().equals(other.getClientConnectionId()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getClientTime() == null ? other.getClientTime() == null : this.getClientTime().equals(other.getClientTime()))
            && (this.getServerTime() == null ? other.getServerTime() == null : this.getServerTime().equals(other.getServerTime()))
            && (this.getNumericValue() == null ? other.getNumericValue() == null : this.getNumericValue().equals(other.getNumericValue()))
            && (this.getText() == null ? other.getText() == null : this.getText().equals(other.getText()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_event
     *
     * @mbg.generated Wed Apr 06 16:51:31 CEST 2022
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getClientConnectionId() == null) ? 0 : getClientConnectionId().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getClientTime() == null) ? 0 : getClientTime().hashCode());
        result = prime * result + ((getServerTime() == null) ? 0 : getServerTime().hashCode());
        result = prime * result + ((getNumericValue() == null) ? 0 : getNumericValue().hashCode());
        result = prime * result + ((getText() == null) ? 0 : getText().hashCode());
        return result;
    }
}