/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.ArrayList;
import java.util.Arrays;
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
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator.DistributedIndicatorValueService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator.PingIntervalClientIndicator;

@Lazy
@Component
@WebServiceProfile
public class ClientIndicatorFactory {

    private static final Logger log = LoggerFactory.getLogger(ClientIndicatorFactory.class);

    private final ApplicationContext applicationContext;
    private final IndicatorDAO indicatorDAO;
    private final DistributedIndicatorValueService distributedPingCache;
    private final boolean distributedSetup;
    private final boolean enableCaching;

    @Autowired
    public ClientIndicatorFactory(
            final ApplicationContext applicationContext,
            final IndicatorDAO indicatorDAO,
            final DistributedIndicatorValueService distributedPingCache,
            @Value("${sebserver.webservice.distributed:false}") final boolean distributedSetup,
            @Value("${sebserver.webservice.api.exam.enable-indicator-cache:true}") final boolean enableCaching) {

        this.applicationContext = applicationContext;
        this.indicatorDAO = indicatorDAO;
        this.distributedPingCache = distributedPingCache;
        this.distributedSetup = distributedSetup;
        this.enableCaching = distributedSetup ? false : enableCaching;
    }

    public void initializeDistributedCaches(final ClientConnection clientConnection) {
        try {

            if (!this.distributedSetup || clientConnection.examId == null) {
                return;
            }

            final Collection<Indicator> examIndicators = this.indicatorDAO
                    .allForExam(clientConnection.examId)
                    .getOrThrow();

            boolean pingIndicatorAvailable = false;
            for (final Indicator indicatorDef : examIndicators) {

                this.distributedPingCache.createIndicatorForConnection(
                        clientConnection.id,
                        indicatorDef.type,
                        indicatorDef.type == IndicatorType.LAST_PING ? Utils.getMillisecondsNow() : 0L);

                if (!pingIndicatorAvailable) {
                    pingIndicatorAvailable = indicatorDef.type == IndicatorType.LAST_PING;
                }
            }

            // If there is no ping interval indicator set from the exam, we add a hidden one
            // to at least create missing ping events and track missing state
            if (!pingIndicatorAvailable) {
                this.distributedPingCache.createIndicatorForConnection(
                        clientConnection.id,
                        IndicatorType.LAST_PING,
                        Utils.getMillisecondsNow());
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to initialize distributed indicator value cache for: {}",
                    clientConnection,
                    e);
        }
    }

    public List<? extends IndicatorValue> getIndicatorValues(final ClientConnection clientConnection) {

        final List<ClientIndicator> result = new ArrayList<>();
        if (clientConnection.examId == null) {
            return result;
        }

        try {

            final Collection<Indicator> examIndicators = this.indicatorDAO
                    .allForExam(clientConnection.examId)
                    .getOrThrow();

            for (final Indicator indicatorDef : examIndicators) {
                try {

                    final ClientIndicator indicator = this.applicationContext
                            .getBean(indicatorDef.type.name(), ClientIndicator.class);

                    indicator.init(
                            indicatorDef,
                            clientConnection.id,
                            clientConnection.status.clientActiveStatus,
                            true);

                    result.add(indicator);
                } catch (final Exception e) {
                    log.error("Failed to create IndicatorValue for indicator: {}", indicatorDef, e);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to create IndicatorValues for ClientConnection: {}", clientConnection);
        }

        return Collections.unmodifiableList(result);
    }

    public List<ClientIndicator> createFor(final ClientConnection clientConnection) {

        final List<ClientIndicator> result = new ArrayList<>();
        if (clientConnection.examId == null) {
            return result;
        }

        try {

            final Collection<Indicator> examIndicators = this.indicatorDAO
                    .allForExam(clientConnection.examId)
                    .getOrThrow();

            boolean pingIndicatorAvailable = false;
            for (final Indicator indicatorDef : examIndicators) {
                try {

                    final ClientIndicator indicator = this.applicationContext
                            .getBean(indicatorDef.type.name(), ClientIndicator.class);

                    if (!pingIndicatorAvailable) {
                        pingIndicatorAvailable = indicatorDef.type == IndicatorType.LAST_PING;
                    }

                    indicator.init(
                            indicatorDef,
                            clientConnection.id,
                            clientConnection.status.clientActiveStatus,
                            this.enableCaching);

                    result.add(indicator);
                } catch (final Exception e) {
                    log.warn("No Indicator with type: {} found as registered bean. Ignore this one.",
                            indicatorDef.type,
                            e);
                }
            }

            // If there is no ping interval indicator set from the exam, we add a hidden one
            // to at least create missing ping events and track missing state
            if (!pingIndicatorAvailable) {
                final PingIntervalClientIndicator pingIndicator = this.applicationContext
                        .getBean(PingIntervalClientIndicator.class);
                pingIndicator.setHidden();
                final Indicator indicator = new Indicator(
                        -1L,
                        clientConnection.examId,
                        "hidden_ping_indicator",
                        IndicatorType.LAST_PING,
                        null,
                        null,
                        null,
                        Arrays.asList(new Indicator.Threshold(5000d, null, null)));

                pingIndicator.init(
                        indicator,
                        clientConnection.id,
                        clientConnection.status.clientActiveStatus,
                        this.enableCaching);
                result.add(pingIndicator);
            }

        } catch (final RuntimeException e) {
            log.error("Failed to create ClientIndicator for ClientConnection: {}", clientConnection);
            throw e;
        } catch (final Exception e) {
            log.error("Failed to create ClientIndicator for ClientConnection: {}", clientConnection);
        }

        return Collections.unmodifiableList(result);
    }

}
