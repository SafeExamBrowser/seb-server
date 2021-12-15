/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public abstract class AbstractLogIndicator extends AbstractClientIndicator {

    protected static final Long DISTRIBUTED_LOG_UPDATE_INTERVAL = 5 * Constants.SECOND_IN_MILLIS;

    protected final Set<EventType> observed;
    protected final List<Integer> eventTypeIds;
    protected String[] tags;

    protected AbstractLogIndicator(
            final DistributedIndicatorValueService distributedPingCache,
            final EventType... eventTypes) {

        super(distributedPingCache);
        this.observed = Collections.unmodifiableSet(EnumSet.of(eventTypes[0], eventTypes));
        this.eventTypeIds = Utils.immutableListOf(Arrays.stream(eventTypes)
                .map(et -> et.id)
                .collect(Collectors.toList()));

    }

    @Override
    public void init(
            final Indicator indicatorDefinition,
            final Long connectionId,
            final boolean active,
            final boolean cachingEnabled) {

        super.init(indicatorDefinition, connectionId, active, cachingEnabled);

        // init tags
        if (indicatorDefinition == null || StringUtils.isBlank(indicatorDefinition.tags)) {
            this.tags = null;
        } else {
            this.tags = StringUtils.split(indicatorDefinition.tags, Constants.COMMA);
            for (int i = 0; i < this.tags.length; i++) {
                this.tags[i] = Constants.ANGLE_BRACE_OPEN + this.tags[i] + Constants.ANGLE_BRACE_CLOSE;
            }
        }
    }

    protected boolean hasTag(final String text) {
        if (text == null) {
            return false;
        }

        for (int i = 0; i < this.tags.length; i++) {
            if (text.startsWith(this.tags[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<EventType> observedEvents() {
        return this.observed;
    }

}
