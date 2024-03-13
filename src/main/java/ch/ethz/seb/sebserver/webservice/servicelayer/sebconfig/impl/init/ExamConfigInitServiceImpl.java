/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.init;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigInitService;

@Lazy
@Service
@WebServiceProfile
public class ExamConfigInitServiceImpl implements ExamConfigInitService {

    private final Collection<AdditionalDefaultValueProvider> defaultValueProvider;

    public ExamConfigInitServiceImpl(
            final Collection<AdditionalDefaultValueProvider> defaultValueProvider) {

        this.defaultValueProvider = Utils.immutableCollectionOf(defaultValueProvider);
    }

    @Override
    public Collection<ConfigurationValue> getAdditionalDefaultValues(
            final Long institutionId,
            final Long configurationId,
            final Function<String, ConfigurationAttribute> attributeResolver) {

        return this.defaultValueProvider
                .stream()
                .flatMap(provider -> provider.getAdditionalDefaultValues(
                        institutionId,
                        configurationId,
                        attributeResolver)
                        .stream())
                .collect(Collectors.toList());
    }

}
