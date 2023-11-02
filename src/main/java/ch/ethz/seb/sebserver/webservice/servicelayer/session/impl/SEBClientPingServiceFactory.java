/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collection;
import java.util.EnumMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientPingService;

@Lazy
@Component
@WebServiceProfile
public class SEBClientPingServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(SEBClientPingServiceFactory.class);

    private final EnumMap<SEBClientPingService.PingServiceType, SEBClientPingService> serviceMapping =
            new EnumMap<>(SEBClientPingService.PingServiceType.class);
    private final SEBClientPingService.PingServiceType workingServiceType;

    public SEBClientPingServiceFactory(
            final Collection<SEBClientPingService> serviceBeans,
            @Value("${sebserver.webservice.api.exam.session.ping.service.strategy:BLOCKING}") final String serviceType) {

        SEBClientPingService.PingServiceType serviceTypeToSet = SEBClientPingService.PingServiceType.BLOCKING;
        try {
            serviceTypeToSet = SEBClientPingService.PingServiceType.valueOf(serviceType);
        } catch (final Exception e) {
            serviceTypeToSet = SEBClientPingService.PingServiceType.BLOCKING;
        }
        this.workingServiceType = serviceTypeToSet;

        serviceBeans.stream().forEach(service -> this.serviceMapping.putIfAbsent(service.pingServiceType(), service));
    }

    public SEBClientPingService.PingServiceType getWorkingServiceType() {
        return this.workingServiceType;
    }

    public SEBClientPingService getSEBClientPingService() {

        log.info("Work with SEBClientPingService of type: {}", this.workingServiceType);

        switch (this.workingServiceType) {
            case BATCH: {
                final SEBClientPingService service =
                        this.serviceMapping.get(SEBClientPingService.PingServiceType.BATCH);
                if (service != null) {
                    ((SEBClientPingBatchService) service).init();
                    return service;
                } else {
                    return this.serviceMapping.get(SEBClientPingService.PingServiceType.BLOCKING);
                }
            }
            default:
                return this.serviceMapping.get(SEBClientPingService.PingServiceType.BLOCKING);
        }
    }

}
