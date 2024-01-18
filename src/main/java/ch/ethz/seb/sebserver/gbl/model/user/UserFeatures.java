package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserFeatures {

    public static final String ATTR_DEFAULT = "missingFeatureDefault";
    public static final String ATTR_FEATURE_PRIVILEGES = "featurePrivileges";

    public enum Feature {
        INSTITUTION("admin.institution"),
        SCREEN_PROCTORING("seb.screenProctoring"),

        LIVE_PROCTORING("seb.liveProctoring"),
        TEST_LMS("lms.type.MOCKUP"),
        EXAM_NO_LMS("exam.noLMS"),

        ;

        public final String featureName;

        Feature(final String featureName) {
            this.featureName = featureName;
        }
    }

    @JsonProperty(Domain.USER.ATTR_ID)
    public final String userId;
    @JsonProperty(ATTR_DEFAULT)
    public final Boolean missingFeatureDefault;
    @JsonProperty(ATTR_FEATURE_PRIVILEGES)
    public final Map<String, Boolean> featurePrivileges;

    @JsonCreator
    public UserFeatures(
            @JsonProperty(Domain.USER.ATTR_ID) final String userId,
            @JsonProperty(ATTR_DEFAULT) final Boolean missingFeatureDefault,
            @JsonProperty(ATTR_FEATURE_PRIVILEGES) final Map<String, Boolean> featurePrivileges) {

        this.userId = userId;
        this.missingFeatureDefault = missingFeatureDefault;
        this.featurePrivileges = Utils.immutableMapOf(featurePrivileges);
    }

    public String getUserId() {
        return userId;
    }

    public Boolean getMissingFeatureDefault() {
        return missingFeatureDefault;
    }

    public Map<String, Boolean> getFeaturePrivileges() {
        return featurePrivileges;
    }

    public boolean isFeatureEnabled(final Feature feature) {
        return featurePrivileges.getOrDefault(feature.featureName, missingFeatureDefault);
    }

    public boolean isFeatureEnabled(final String featureName) {
        return featurePrivileges.getOrDefault(featureName, missingFeatureDefault);
    }
}
