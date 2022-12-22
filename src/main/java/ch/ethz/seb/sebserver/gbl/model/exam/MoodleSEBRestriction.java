/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MoodleSEBRestriction {

    public static final String ATTR_ALT_BEK = "ALTERNATIVE_SEB_BEK";
    public static final String ATTR_BROWSER_KEYS = "BROWSER_KEYS";
    public static final String ATTR_CONFIG_KEYS = "CONFIG_KEYS";

    @JsonProperty(ATTR_CONFIG_KEYS)
    public final Collection<String> configKeys;

    @JsonProperty(ATTR_BROWSER_KEYS)
    public final Collection<String> browserExamKeys;

    @JsonCreator
    protected MoodleSEBRestriction(
            @JsonProperty(ATTR_CONFIG_KEYS) final Collection<String> configKeys,
            @JsonProperty(ATTR_BROWSER_KEYS) final Collection<String> browserExamKeys) {

        this.configKeys = Utils.immutableCollectionOf(configKeys);
        this.browserExamKeys = Utils.immutableCollectionOf(browserExamKeys);
    }

    public Collection<String> getConfigKeys() {
        return this.configKeys;
    }

    public Collection<String> getBrowserExamKeys() {
        return this.browserExamKeys;
    }

    public static MoodleSEBRestriction from(final SEBRestriction sebRestrictionData) {
        return new MoodleSEBRestriction(
                sebRestrictionData.configKeys,
                sebRestrictionData.browserExamKeys);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("MoodleSEBRestriction [configKeys=");
        builder.append(this.configKeys);
        builder.append(", browserExamKeys=");
        builder.append(this.browserExamKeys);
        builder.append("]");
        return builder.toString();
    }

}
