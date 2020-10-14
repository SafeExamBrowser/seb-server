package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import javax.annotation.Generated;

public class ClientConnectionRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.755+02:00", comments="Source field: client_connection.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.institution_id")
    private Long institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.exam_id")
    private Long examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.status")
    private String status;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.connection_token")
    private String connectionToken;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.exam_user_session_id")
    private String examUserSessionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.client_address")
    private String clientAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.virtual_client_address")
    private String virtualClientAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.creation_time")
    private Long creationTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.remote_proctoring_room_id")
    private Long remoteProctoringRoomId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.remote_proctoring_room_update")
    private Integer remoteProctoringRoomUpdate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.755+02:00", comments="Source Table: client_connection")
    public ClientConnectionRecord(Long id, Long institutionId, Long examId, String status, String connectionToken, String examUserSessionId, String clientAddress, String virtualClientAddress, Long creationTime, Long remoteProctoringRoomId, Integer remoteProctoringRoomUpdate) {
        this.id = id;
        this.institutionId = institutionId;
        this.examId = examId;
        this.status = status;
        this.connectionToken = connectionToken;
        this.examUserSessionId = examUserSessionId;
        this.clientAddress = clientAddress;
        this.virtualClientAddress = virtualClientAddress;
        this.creationTime = creationTime;
        this.remoteProctoringRoomId = remoteProctoringRoomId;
        this.remoteProctoringRoomUpdate = remoteProctoringRoomUpdate;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.institution_id")
    public Long getInstitutionId() {
        return institutionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.exam_id")
    public Long getExamId() {
        return examId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.status")
    public String getStatus() {
        return status;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.connection_token")
    public String getConnectionToken() {
        return connectionToken;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.exam_user_session_id")
    public String getExamUserSessionId() {
        return examUserSessionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.client_address")
    public String getClientAddress() {
        return clientAddress;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.virtual_client_address")
    public String getVirtualClientAddress() {
        return virtualClientAddress;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.creation_time")
    public Long getCreationTime() {
        return creationTime;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.remote_proctoring_room_id")
    public Long getRemoteProctoringRoomId() {
        return remoteProctoringRoomId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2020-10-12T13:53:04.756+02:00", comments="Source field: client_connection.remote_proctoring_room_update")
    public Integer getRemoteProctoringRoomUpdate() {
        return remoteProctoringRoomUpdate;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_connection
     *
     * @mbg.generated Mon Oct 12 13:53:04 CEST 2020
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", institutionId=").append(institutionId);
        sb.append(", examId=").append(examId);
        sb.append(", status=").append(status);
        sb.append(", connectionToken=").append(connectionToken);
        sb.append(", examUserSessionId=").append(examUserSessionId);
        sb.append(", clientAddress=").append(clientAddress);
        sb.append(", virtualClientAddress=").append(virtualClientAddress);
        sb.append(", creationTime=").append(creationTime);
        sb.append(", remoteProctoringRoomId=").append(remoteProctoringRoomId);
        sb.append(", remoteProctoringRoomUpdate=").append(remoteProctoringRoomUpdate);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_connection
     *
     * @mbg.generated Mon Oct 12 13:53:04 CEST 2020
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
        ClientConnectionRecord other = (ClientConnectionRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getInstitutionId() == null ? other.getInstitutionId() == null : this.getInstitutionId().equals(other.getInstitutionId()))
            && (this.getExamId() == null ? other.getExamId() == null : this.getExamId().equals(other.getExamId()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getConnectionToken() == null ? other.getConnectionToken() == null : this.getConnectionToken().equals(other.getConnectionToken()))
            && (this.getExamUserSessionId() == null ? other.getExamUserSessionId() == null : this.getExamUserSessionId().equals(other.getExamUserSessionId()))
            && (this.getClientAddress() == null ? other.getClientAddress() == null : this.getClientAddress().equals(other.getClientAddress()))
            && (this.getVirtualClientAddress() == null ? other.getVirtualClientAddress() == null : this.getVirtualClientAddress().equals(other.getVirtualClientAddress()))
            && (this.getCreationTime() == null ? other.getCreationTime() == null : this.getCreationTime().equals(other.getCreationTime()))
            && (this.getRemoteProctoringRoomId() == null ? other.getRemoteProctoringRoomId() == null : this.getRemoteProctoringRoomId().equals(other.getRemoteProctoringRoomId()))
            && (this.getRemoteProctoringRoomUpdate() == null ? other.getRemoteProctoringRoomUpdate() == null : this.getRemoteProctoringRoomUpdate().equals(other.getRemoteProctoringRoomUpdate()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_connection
     *
     * @mbg.generated Mon Oct 12 13:53:04 CEST 2020
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getInstitutionId() == null) ? 0 : getInstitutionId().hashCode());
        result = prime * result + ((getExamId() == null) ? 0 : getExamId().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getConnectionToken() == null) ? 0 : getConnectionToken().hashCode());
        result = prime * result + ((getExamUserSessionId() == null) ? 0 : getExamUserSessionId().hashCode());
        result = prime * result + ((getClientAddress() == null) ? 0 : getClientAddress().hashCode());
        result = prime * result + ((getVirtualClientAddress() == null) ? 0 : getVirtualClientAddress().hashCode());
        result = prime * result + ((getCreationTime() == null) ? 0 : getCreationTime().hashCode());
        result = prime * result + ((getRemoteProctoringRoomId() == null) ? 0 : getRemoteProctoringRoomId().hashCode());
        result = prime * result + ((getRemoteProctoringRoomUpdate() == null) ? 0 : getRemoteProctoringRoomUpdate().hashCode());
        return result;
    }
}