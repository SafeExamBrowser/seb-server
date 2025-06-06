package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import javax.annotation.Generated;

public class ClientNotificationRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.client_connection_id")
    private Long clientConnectionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.event_type")
    private Integer eventType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.notification_type")
    private Integer notificationType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.value")
    private Long value;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.text")
    private String text;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source Table: client_notification")
    public ClientNotificationRecord(Long id, Long clientConnectionId, Integer eventType, Integer notificationType, Long value, String text) {
        this.id = id;
        this.clientConnectionId = clientConnectionId;
        this.eventType = eventType;
        this.notificationType = notificationType;
        this.value = value;
        this.text = text;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.client_connection_id")
    public Long getClientConnectionId() {
        return clientConnectionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.event_type")
    public Integer getEventType() {
        return eventType;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.notification_type")
    public Integer getNotificationType() {
        return notificationType;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.value")
    public Long getValue() {
        return value;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-11-04T15:08:40.860+01:00", comments="Source field: client_notification.text")
    public String getText() {
        return text;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_notification
     *
     * @mbg.generated Mon Nov 04 15:08:40 CET 2024
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", clientConnectionId=").append(clientConnectionId);
        sb.append(", eventType=").append(eventType);
        sb.append(", notificationType=").append(notificationType);
        sb.append(", value=").append(value);
        sb.append(", text=").append(text);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_notification
     *
     * @mbg.generated Mon Nov 04 15:08:40 CET 2024
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
        ClientNotificationRecord other = (ClientNotificationRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getClientConnectionId() == null ? other.getClientConnectionId() == null : this.getClientConnectionId().equals(other.getClientConnectionId()))
            && (this.getEventType() == null ? other.getEventType() == null : this.getEventType().equals(other.getEventType()))
            && (this.getNotificationType() == null ? other.getNotificationType() == null : this.getNotificationType().equals(other.getNotificationType()))
            && (this.getValue() == null ? other.getValue() == null : this.getValue().equals(other.getValue()))
            && (this.getText() == null ? other.getText() == null : this.getText().equals(other.getText()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_notification
     *
     * @mbg.generated Mon Nov 04 15:08:40 CET 2024
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getClientConnectionId() == null) ? 0 : getClientConnectionId().hashCode());
        result = prime * result + ((getEventType() == null) ? 0 : getEventType().hashCode());
        result = prime * result + ((getNotificationType() == null) ? 0 : getNotificationType().hashCode());
        result = prime * result + ((getValue() == null) ? 0 : getValue().hashCode());
        result = prime * result + ((getText() == null) ? 0 : getText().hashCode());
        return result;
    }
}