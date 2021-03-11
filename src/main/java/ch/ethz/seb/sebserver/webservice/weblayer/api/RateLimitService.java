/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.time.Duration;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;

@Lazy
@Service
@WebServiceProfile
public class RateLimitService {

    private final int requestLimit;
    private final int requestLimitInterval;
    private final int requestLimitRefill;

    private final int createLimit;
    private final int createLimitInterval;
    private final int createLimitRefill;

    public RateLimitService(final Environment env) {
        this.requestLimit = env.getProperty(
                "sebserver.webservice.api.admin.request.limit", Integer.class, 10);
        this.requestLimitInterval =
                env.getProperty("sebserver.webservice.api.admin.request.limit.interval.min", Integer.class, 10);
        this.requestLimitRefill =
                env.getProperty("sebserver.webservice.api.admin.request.limit.refill", Integer.class, 2);

        this.createLimit = env.getProperty(
                "sebserver.webservice.api.admin.create.limit", Integer.class, 10);
        this.createLimitInterval =
                env.getProperty("sebserver.webservice.api.admin.create.limit.interval.min", Integer.class, 3600);
        this.createLimitRefill =
                env.getProperty("sebserver.webservice.api.admin.create.limit.refill", Integer.class, 10);
    }

    public LocalBucket createRequestLimitBucker() {
        final Bandwidth limit = Bandwidth.classic(
                this.requestLimit,
                Refill.intervally(this.requestLimitRefill, Duration.ofMinutes(this.requestLimitInterval)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    public LocalBucket createCreationLimitBucker() {
        final Bandwidth limit = Bandwidth.classic(
                this.createLimit,
                Refill.intervally(this.createLimitRefill, Duration.ofMinutes(this.createLimitInterval)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

}
