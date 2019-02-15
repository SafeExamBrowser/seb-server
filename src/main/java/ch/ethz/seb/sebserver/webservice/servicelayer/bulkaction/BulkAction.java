/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;

public final class BulkAction {

    public enum Type {
        HARD_DELETE(ActivityType.DELETE),
        DEACTIVATE(ActivityType.DEACTIVATE),
        ACTIVATE(ActivityType.ACTIVATE);

        public final ActivityType activityType;

        private Type(final ActivityType activityType) {
            this.activityType = activityType;
        }
    }

    public final Type type;
    public final EntityType sourceType;
    public final Set<EntityKey> sources;

    final Set<EntityKey> dependencies;
    final Set<Result<EntityKey>> result;

    boolean alreadyProcessed = false;

    public BulkAction(
            final Type type,
            final EntityType sourceType,
            final Collection<EntityKey> sources) {

        this.type = type;
        this.sourceType = sourceType;
        this.sources = Utils.immutableSetOf(sources);
        this.dependencies = new LinkedHashSet<>();
        this.result = new HashSet<>();

        check();
    }

    public BulkAction(
            final Type type,
            final EntityType sourceType,
            final EntityKey... sources) {

        this(type, sourceType, (sources != null) ? Arrays.asList(sources) : Collections.emptyList());
    }

    public Set<EntityKey> getDependencies() {
        return Collections.unmodifiableSet(this.dependencies);
    }

    public Set<EntityKey> extractKeys(final EntityType type) {
        if (this.sourceType == type) {
            return Collections.unmodifiableSet(new HashSet<>(this.sources));
        }

        if (!this.dependencies.isEmpty()) {
            return Collections.unmodifiableSet(new HashSet<>(this.dependencies
                    .stream()
                    .filter(key -> key.entityType == type)
                    .collect(Collectors.toList())));
        }

        return Collections.emptySet();
    }

    @Override
    public String toString() {
        return "BulkAction [type=" + this.type + ", sourceType=" + this.sourceType + ", sources=" + this.sources + "]";
    }

    private void check() {
        for (final EntityKey source : this.sources) {
            if (source.entityType != this.sourceType) {
                throw new IllegalArgumentException(
                        "At least one EntityType in sources list has not the expected EntityType");
            }
        }

    }

}
