package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import javax.annotation.Generated;

public class ClientConnectionRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.institution_id")
    private Long institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.exam_id")
    private Long examId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.status")
    private String status;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.connection_token")
    private String connectionToken;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.exam_user_session_id")
    private String examUserSessionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.client_address")
    private String clientAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.virtual_client_address")
    private String virtualClientAddress;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.vdi")
    private Integer vdi;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.vdi_pair_token")
    private String vdiPairToken;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.creation_time")
    private Long creationTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.update_time")
    private Long updateTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.screen_proctoring_group_id")
    private Long screenProctoringGroupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.screen_proctoring_group_update")
    private Byte screenProctoringGroupUpdate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.remote_proctoring_room_id")
    private Long remoteProctoringRoomId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.remote_proctoring_room_update")
    private Integer remoteProctoringRoomUpdate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.client_machine_name")
    private String clientMachineName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.client_os_name")
    private String clientOsName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.client_version")
    private String clientVersion;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.security_check_granted")
    private Byte securityCheckGranted;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.ask")
    private String ask;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.client_version_granted")
    private Byte clientVersionGranted;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source Table: client_connection")
    public ClientConnectionRecord(Long id, Long institutionId, Long examId, String status, String connectionToken, String examUserSessionId, String clientAddress, String virtualClientAddress, Integer vdi, String vdiPairToken, Long creationTime, Long updateTime, Long screenProctoringGroupId, Byte screenProctoringGroupUpdate, Long remoteProctoringRoomId, Integer remoteProctoringRoomUpdate, String clientMachineName, String clientOsName, String clientVersion, Byte securityCheckGranted, String ask, Byte clientVersionGranted) {
        this.id = id;
        this.institutionId = institutionId;
        this.examId = examId;
        this.status = status;
        this.connectionToken = connectionToken;
        this.examUserSessionId = examUserSessionId;
        this.clientAddress = clientAddress;
        this.virtualClientAddress = virtualClientAddress;
        this.vdi = vdi;
        this.vdiPairToken = vdiPairToken;
        this.creationTime = creationTime;
        this.updateTime = updateTime;
        this.screenProctoringGroupId = screenProctoringGroupId;
        this.screenProctoringGroupUpdate = screenProctoringGroupUpdate;
        this.remoteProctoringRoomId = remoteProctoringRoomId;
        this.remoteProctoringRoomUpdate = remoteProctoringRoomUpdate;
        this.clientMachineName = clientMachineName;
        this.clientOsName = clientOsName;
        this.clientVersion = clientVersion;
        this.securityCheckGranted = securityCheckGranted;
        this.ask = ask;
        this.clientVersionGranted = clientVersionGranted;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.institution_id")
    public Long getInstitutionId() {
        return institutionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.exam_id")
    public Long getExamId() {
        return examId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.status")
    public String getStatus() {
        return status;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.connection_token")
    public String getConnectionToken() {
        return connectionToken;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.exam_user_session_id")
    public String getExamUserSessionId() {
        return examUserSessionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.client_address")
    public String getClientAddress() {
        return clientAddress;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.virtual_client_address")
    public String getVirtualClientAddress() {
        return virtualClientAddress;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.vdi")
    public Integer getVdi() {
        return vdi;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.vdi_pair_token")
    public String getVdiPairToken() {
        return vdiPairToken;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.154+02:00", comments="Source field: client_connection.creation_time")
    public Long getCreationTime() {
        return creationTime;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.update_time")
    public Long getUpdateTime() {
        return updateTime;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.screen_proctoring_group_id")
    public Long getScreenProctoringGroupId() {
        return screenProctoringGroupId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.screen_proctoring_group_update")
    public Byte getScreenProctoringGroupUpdate() {
        return screenProctoringGroupUpdate;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.remote_proctoring_room_id")
    public Long getRemoteProctoringRoomId() {
        return remoteProctoringRoomId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.remote_proctoring_room_update")
    public Integer getRemoteProctoringRoomUpdate() {
        return remoteProctoringRoomUpdate;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.client_machine_name")
    public String getClientMachineName() {
        return clientMachineName;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.client_os_name")
    public String getClientOsName() {
        return clientOsName;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.client_version")
    public String getClientVersion() {
        return clientVersion;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.security_check_granted")
    public Byte getSecurityCheckGranted() {
        return securityCheckGranted;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.ask")
    public String getAsk() {
        return ask;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2024-04-18T08:15:10.155+02:00", comments="Source field: client_connection.client_version_granted")
    public Byte getClientVersionGranted() {
        return clientVersionGranted;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_connection
     *
     * @mbg.generated Thu Apr 18 08:15:10 CEST 2024
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
        sb.append(", vdi=").append(vdi);
        sb.append(", vdiPairToken=").append(vdiPairToken);
        sb.append(", creationTime=").append(creationTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", screenProctoringGroupId=").append(screenProctoringGroupId);
        sb.append(", screenProctoringGroupUpdate=").append(screenProctoringGroupUpdate);
        sb.append(", remoteProctoringRoomId=").append(remoteProctoringRoomId);
        sb.append(", remoteProctoringRoomUpdate=").append(remoteProctoringRoomUpdate);
        sb.append(", clientMachineName=").append(clientMachineName);
        sb.append(", clientOsName=").append(clientOsName);
        sb.append(", clientVersion=").append(clientVersion);
        sb.append(", securityCheckGranted=").append(securityCheckGranted);
        sb.append(", ask=").append(ask);
        sb.append(", clientVersionGranted=").append(clientVersionGranted);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_connection
     *
     * @mbg.generated Thu Apr 18 08:15:10 CEST 2024
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
            && (this.getVdi() == null ? other.getVdi() == null : this.getVdi().equals(other.getVdi()))
            && (this.getVdiPairToken() == null ? other.getVdiPairToken() == null : this.getVdiPairToken().equals(other.getVdiPairToken()))
            && (this.getCreationTime() == null ? other.getCreationTime() == null : this.getCreationTime().equals(other.getCreationTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getScreenProctoringGroupId() == null ? other.getScreenProctoringGroupId() == null : this.getScreenProctoringGroupId().equals(other.getScreenProctoringGroupId()))
            && (this.getScreenProctoringGroupUpdate() == null ? other.getScreenProctoringGroupUpdate() == null : this.getScreenProctoringGroupUpdate().equals(other.getScreenProctoringGroupUpdate()))
            && (this.getRemoteProctoringRoomId() == null ? other.getRemoteProctoringRoomId() == null : this.getRemoteProctoringRoomId().equals(other.getRemoteProctoringRoomId()))
            && (this.getRemoteProctoringRoomUpdate() == null ? other.getRemoteProctoringRoomUpdate() == null : this.getRemoteProctoringRoomUpdate().equals(other.getRemoteProctoringRoomUpdate()))
            && (this.getClientMachineName() == null ? other.getClientMachineName() == null : this.getClientMachineName().equals(other.getClientMachineName()))
            && (this.getClientOsName() == null ? other.getClientOsName() == null : this.getClientOsName().equals(other.getClientOsName()))
            && (this.getClientVersion() == null ? other.getClientVersion() == null : this.getClientVersion().equals(other.getClientVersion()))
            && (this.getSecurityCheckGranted() == null ? other.getSecurityCheckGranted() == null : this.getSecurityCheckGranted().equals(other.getSecurityCheckGranted()))
            && (this.getAsk() == null ? other.getAsk() == null : this.getAsk().equals(other.getAsk()))
            && (this.getClientVersionGranted() == null ? other.getClientVersionGranted() == null : this.getClientVersionGranted().equals(other.getClientVersionGranted()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table client_connection
     *
     * @mbg.generated Thu Apr 18 08:15:10 CEST 2024
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
        result = prime * result + ((getVdi() == null) ? 0 : getVdi().hashCode());
        result = prime * result + ((getVdiPairToken() == null) ? 0 : getVdiPairToken().hashCode());
        result = prime * result + ((getCreationTime() == null) ? 0 : getCreationTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getScreenProctoringGroupId() == null) ? 0 : getScreenProctoringGroupId().hashCode());
        result = prime * result + ((getScreenProctoringGroupUpdate() == null) ? 0 : getScreenProctoringGroupUpdate().hashCode());
        result = prime * result + ((getRemoteProctoringRoomId() == null) ? 0 : getRemoteProctoringRoomId().hashCode());
        result = prime * result + ((getRemoteProctoringRoomUpdate() == null) ? 0 : getRemoteProctoringRoomUpdate().hashCode());
        result = prime * result + ((getClientMachineName() == null) ? 0 : getClientMachineName().hashCode());
        result = prime * result + ((getClientOsName() == null) ? 0 : getClientOsName().hashCode());
        result = prime * result + ((getClientVersion() == null) ? 0 : getClientVersion().hashCode());
        result = prime * result + ((getSecurityCheckGranted() == null) ? 0 : getSecurityCheckGranted().hashCode());
        result = prime * result + ((getAsk() == null) ? 0 : getAsk().hashCode());
        result = prime * result + ((getClientVersionGranted() == null) ? 0 : getClientVersionGranted().hashCode());
        return result;
    }
}