/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;

@Lazy
@Component
@WebServiceProfile
public class ClientIndicatorFactory {

    private static final Logger log = LoggerFactory.getLogger(ClientIndicatorFactory.class);

    private final ApplicationContext applicationContext;
    private final IndicatorDAO indicatorDAO;
    private final boolean enableCaching;

    @Autowired
    public ClientIndicatorFactory(
            final ApplicationContext applicationContext,
            final IndicatorDAO indicatorDAO,
            @Value("${sebserver.indicator.caching}") final boolean enableCaching) {

        this.applicationContext = applicationContext;
        this.indicatorDAO = indicatorDAO;
        this.enableCaching = enableCaching;
    }

    public Collection<ClientIndicator> createFor(final ClientConnection clientConnection) {
        final List<ClientIndicator> result = new ArrayList<>();

        try {

            final Collection<Indicator> examIndicators = this.indicatorDAO
                    .allForExam(clientConnection.examId)
                    .getOrThrow();

            for (final Indicator indicatorDef : examIndicators) {
                try {
                    final ClientIndicator indicator = this.applicationContext
                            .getBean(indicatorDef.type.name(), ClientIndicator.class);
                    indicator.init(indicatorDef, clientConnection.id, this.enableCaching);
                    result.add(indicator);
                } catch (final Exception e) {
                    log.warn("No Indicator with type: {} found as registered bean. Ignore this one.", indicatorDef.type,
                            e);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to create ClientIndicator for ClientConnection: {}", clientConnection);
        }

        return Collections.unmodifiableList(result);
    }

}
