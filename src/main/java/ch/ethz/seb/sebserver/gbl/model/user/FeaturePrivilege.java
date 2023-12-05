package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.Objects;

import ch.ethz.seb.sebserver.gbl.model.Domain.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FeaturePrivilege {

    @JsonProperty(FEATURE_PRIVILEGE.ATTR_ID)
    public final Long id;
    @JsonProperty(FEATURE_PRIVILEGE.ATTR_FEATURE_ID)
    public final Long featureId;

    @JsonProperty(FEATURE_PRIVILEGE.ATTR_USER_UUID)
    public final String userUUID;

    public FeaturePrivilege(
            @JsonProperty(FEATURE_PRIVILEGE.ATTR_ID) final Long id,
            @JsonProperty(FEATURE_PRIVILEGE.ATTR_FEATURE_ID) final Long featureId,
            @JsonProperty(FEATURE_PRIVILEGE.ATTR_USER_UUID) final String userUUID) {

        this.id = id;
        this.featureId = featureId;
        this.userUUID = userUUID;
    }
    public Long getId() {
        return id;
    }

    public Long getFeatureId() {
        return featureId;
    }

    public String getUserUUID() {
        return userUUID;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FeaturePrivilege that = (FeaturePrivilege) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FeaturePrivilege{" +
                "id=" + id +
                ", featureId=" + featureId +
                ", userUUID='" + userUUID + '\'' +
                '}';
    }
}
