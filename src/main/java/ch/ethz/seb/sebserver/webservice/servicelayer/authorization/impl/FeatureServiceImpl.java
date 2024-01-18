package ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl;

import java.util.*;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.model.user.UserFeatures;
import ch.ethz.seb.sebserver.gbl.model.user.UserFeatures.Feature;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.FeatureService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class FeatureServiceImpl implements FeatureService {

    private final Environment environment;
    private final UserService userService;
    private final UserDAO userDAO;

    public FeatureServiceImpl(
            final Environment environment,
            final UserService userService,
            final UserDAO userDAO) {

        this.environment = environment;
        this.userService = userService;
        this.userDAO = userDAO;
    }


    @Override
    public Result<UserFeatures> getCurrentUserFeatures() {
        return Result.tryCatch(() -> {
            final String userId = userService.getCurrentUser().getUserInfo().uuid;
            final EnumSet<Feature> userEnabledFeatures = getUserEnabledFeatures(userId);
            final Map<String, Boolean> features = Arrays.stream(Feature.values()).collect(Collectors.toMap(
                    f -> f.featureName,
                    f -> isEnabledByConfig(f) && userEnabledFeatures.contains(f)
            ));

            return new UserFeatures(userId, true, features);
        });
    }

    @Override
    public Map<Feature, Boolean> getUserRoleDefaults(final UserRole role) {
        // TODO implement this when user role based features are available
        return Collections.emptyMap();
    }

    public EnumSet<Feature> getUserEnabledFeatures(final String userId) {
        // TODO implement this when user role based features are available
        return EnumSet.allOf(Feature.class);
    }

    @Override
    public boolean isEnabledByConfig(final Feature feature) {
        final String configName = getConfigName(feature);
        try {
            return this.environment.getProperty(configName, Boolean.class, false)
                    || this.environment.getProperty(configName + ".enabled", Boolean.class);
        } catch (final Exception e) {
            // NOTE: for now if there is not explicitly disabled from config, the feature is considered enabled
            return true;
        }
    }

    private String getConfigName(final Feature feature) {
        return getConfigName(feature.featureName);
    }

    private String getConfigName(final String featureName) {
        return FEATURE_CONFIG_PREFIX + featureName;
    }
}
