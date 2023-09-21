package ch.ethz.seb.sebserver.webservice.datalayer.batis.model;

import javax.annotation.Generated;
import org.joda.time.DateTime;

public class SebClientConfigRecord {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.043+02:00", comments="Source field: seb_client_configuration.id")
    private Long id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.043+02:00", comments="Source field: seb_client_configuration.institution_id")
    private Long institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.043+02:00", comments="Source field: seb_client_configuration.name")
    private String name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.043+02:00", comments="Source field: seb_client_configuration.date")
    private DateTime date;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.043+02:00", comments="Source field: seb_client_configuration.client_name")
    private String clientName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.044+02:00", comments="Source field: seb_client_configuration.client_secret")
    private String clientSecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.044+02:00", comments="Source field: seb_client_configuration.encrypt_secret")
    private String encryptSecret;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.044+02:00", comments="Source field: seb_client_configuration.active")
    private Integer active;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.044+02:00", comments="Source field: seb_client_configuration.last_update_time")
    private Long lastUpdateTime;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.044+02:00", comments="Source field: seb_client_configuration.last_update_user")
    private String lastUpdateUser;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.043+02:00", comments="Source Table: seb_client_configuration")
    public SebClientConfigRecord(Long id, Long institutionId, String name, DateTime date, String clientName, String clientSecret, String encryptSecret, Integer active, Long lastUpdateTime, String lastUpdateUser) {
        this.id = id;
        this.institutionId = institutionId;
        this.name = name;
        this.date = date;
        this.clientName = clientName;
        this.clientSecret = clientSecret;
        this.encryptSecret = encryptSecret;
        this.active = active;
        this.lastUpdateTime = lastUpdateTime;
        this.lastUpdateUser = lastUpdateUser;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.043+02:00", comments="Source field: seb_client_configuration.id")
    public Long getId() {
        return id;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.043+02:00", comments="Source field: seb_client_configuration.institution_id")
    public Long getInstitutionId() {
        return institutionId;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.043+02:00", comments="Source field: seb_client_configuration.name")
    public String getName() {
        return name;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.043+02:00", comments="Source field: seb_client_configuration.date")
    public DateTime getDate() {
        return date;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.043+02:00", comments="Source field: seb_client_configuration.client_name")
    public String getClientName() {
        return clientName;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.044+02:00", comments="Source field: seb_client_configuration.client_secret")
    public String getClientSecret() {
        return clientSecret;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.044+02:00", comments="Source field: seb_client_configuration.encrypt_secret")
    public String getEncryptSecret() {
        return encryptSecret;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.044+02:00", comments="Source field: seb_client_configuration.active")
    public Integer getActive() {
        return active;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.044+02:00", comments="Source field: seb_client_configuration.last_update_time")
    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2023-09-14T09:19:48.044+02:00", comments="Source field: seb_client_configuration.last_update_user")
    public String getLastUpdateUser() {
        return lastUpdateUser;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table seb_client_configuration
     *
     * @mbg.generated Thu Sep 14 09:19:48 CEST 2023
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", institutionId=").append(institutionId);
        sb.append(", name=").append(name);
        sb.append(", date=").append(date);
        sb.append(", clientName=").append(clientName);
        sb.append(", clientSecret=").append(clientSecret);
        sb.append(", encryptSecret=").append(encryptSecret);
        sb.append(", active=").append(active);
        sb.append(", lastUpdateTime=").append(lastUpdateTime);
        sb.append(", lastUpdateUser=").append(lastUpdateUser);
        sb.append("]");
        return sb.toString();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table seb_client_configuration
     *
     * @mbg.generated Thu Sep 14 09:19:48 CEST 2023
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
        SebClientConfigRecord other = (SebClientConfigRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getInstitutionId() == null ? other.getInstitutionId() == null : this.getInstitutionId().equals(other.getInstitutionId()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getDate() == null ? other.getDate() == null : this.getDate().equals(other.getDate()))
            && (this.getClientName() == null ? other.getClientName() == null : this.getClientName().equals(other.getClientName()))
            && (this.getClientSecret() == null ? other.getClientSecret() == null : this.getClientSecret().equals(other.getClientSecret()))
            && (this.getEncryptSecret() == null ? other.getEncryptSecret() == null : this.getEncryptSecret().equals(other.getEncryptSecret()))
            && (this.getActive() == null ? other.getActive() == null : this.getActive().equals(other.getActive()))
            && (this.getLastUpdateTime() == null ? other.getLastUpdateTime() == null : this.getLastUpdateTime().equals(other.getLastUpdateTime()))
            && (this.getLastUpdateUser() == null ? other.getLastUpdateUser() == null : this.getLastUpdateUser().equals(other.getLastUpdateUser()));
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table seb_client_configuration
     *
     * @mbg.generated Thu Sep 14 09:19:48 CEST 2023
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getInstitutionId() == null) ? 0 : getInstitutionId().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getDate() == null) ? 0 : getDate().hashCode());
        result = prime * result + ((getClientName() == null) ? 0 : getClientName().hashCode());
        result = prime * result + ((getClientSecret() == null) ? 0 : getClientSecret().hashCode());
        result = prime * result + ((getEncryptSecret() == null) ? 0 : getEncryptSecret().hashCode());
        result = prime * result + ((getActive() == null) ? 0 : getActive().hashCode());
        result = prime * result + ((getLastUpdateTime() == null) ? 0 : getLastUpdateTime().hashCode());
        result = prime * result + ((getLastUpdateUser() == null) ? 0 : getLastUpdateUser().hashCode());
        return result;
    }
}