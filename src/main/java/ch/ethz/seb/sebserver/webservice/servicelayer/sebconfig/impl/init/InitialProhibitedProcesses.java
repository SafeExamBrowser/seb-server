/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.init;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@Lazy
@Component
@WebServiceProfile
public class InitialProhibitedProcesses implements AdditionalDefaultValueProvider {

    private final String configFile;
    private final XMLAttributeLoader xmlAttributeLoader;
    private Collection<ConfigurationValue> cache = null;

    protected InitialProhibitedProcesses(
            final XMLAttributeLoader xmlAttributeLoader,
            @Value("${sebserver.webservice.api.exam.config.init.prohibitedProcesses:config/initialProhibitedProcesses.xml}") final String configFile) {

        this.xmlAttributeLoader = xmlAttributeLoader;
        this.configFile = configFile;
    }

    @Override
    public Collection<ConfigurationValue> getAdditionalDefaultValues(
            final Long institutionId,
            final Long configurationId,
            final Function<String, ConfigurationAttribute> attributeResolver) {

        if (this.cache == null) {
            this.cache = this.xmlAttributeLoader.loadFromXML(
                    institutionId,
                    configurationId,
                    attributeResolver,
                    this.configFile);
        }

        if (this.cache == null) {
            return Collections.emptyList();
        }

        return this.cache
                .stream()
                .map(value -> value.copyOf(institutionId, configurationId))
                .collect(Collectors.toList());
    }

}
