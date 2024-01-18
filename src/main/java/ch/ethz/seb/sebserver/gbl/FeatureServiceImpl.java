/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.CollectingStrategy;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@Lazy
@Service
@WebServiceProfile
@Deprecated // we need another more flexible feature service that also take new User Role and Privileges into account
// SEBSERV-497
public class FeatureServiceImpl implements FeatureService {

    private final Environment environment;

    public FeatureServiceImpl(final Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean isEnabled(final LmsType LmsType) {
        return this.environment.getProperty(toConfigName(
                FEATURE_SETTINGS_PREFIX + LmsType.class.getSimpleName() + "."
                        + LmsType.name()),
                Boolean.class,
                Boolean.TRUE);
    }

    @Override
    public boolean isEnabled(final CollectingStrategy collectingRoomStrategy) {
        return this.environment.getProperty(toConfigName(
                FEATURE_SETTINGS_PREFIX + CollectingStrategy.class.getSimpleName() + "."
                        + collectingRoomStrategy.name()),
                Boolean.class,
                Boolean.TRUE);
    }

    @Override
    public boolean isEnabled(final ProctoringServerType proctoringServerType) {
        return this.environment.getProperty(toConfigName(
                FEATURE_SETTINGS_PREFIX + ProctoringServerType.class.getSimpleName() + "."
                        + proctoringServerType.name()),
                Boolean.class,
                Boolean.TRUE);
    }

    @Override
    public boolean isEnabled(final ConfigurableFeature feature) {
        return this.environment.getProperty(toConfigName(
                        FEATURE_SETTINGS_PREFIX + feature.namespace + ".enabled"),
                Boolean.class,
                Boolean.FALSE);
    }

    private String toConfigName(final String key) {
        return key.replaceAll("_", "-");
    }

}
