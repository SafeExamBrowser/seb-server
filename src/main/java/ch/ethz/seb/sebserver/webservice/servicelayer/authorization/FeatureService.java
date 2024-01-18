package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.user.UserFeatures;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface FeatureService {

    public static final String FEATURE_CONFIG_PREFIX = "sebserver.feature.";

    /** Get all feature flags for current user.
     *
     * @return UserFeatures all feature flags for current user */
    Result<UserFeatures> getCurrentUserFeatures();

    Map<UserFeatures.Feature, Boolean> getUserRoleDefaults(UserRole role);

    boolean isEnabledByConfig(final UserFeatures.Feature feature);
}
