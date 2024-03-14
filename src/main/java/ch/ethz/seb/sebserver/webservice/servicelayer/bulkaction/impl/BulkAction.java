/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;

/** Defines a bulk action with its type, source entities (and source-type) and dependent entities.
 * A BulkAction acts as a collector for entities (keys) that depends on the Bulk Action during the
 * dependency collection phase.
 * A BulkAction also acts as a result collector during the bulk-action process phase. */
public final class BulkAction {

    /** Defines the type of the BulkAction */
    public final BulkActionType type;
    /** Defines the EntityType of the source entities of the BulkAction */
    public final EntityType sourceType;
    /** A Set of EntityKey defining all source-entities of the BulkAction */
    public final Set<EntityKey> sources;
    /** A Set defining the types of dependencies to include into the bulk action
     * Null means all dependencies are included (ignore) and empty means no dependencies are included */
    public final EnumSet<EntityType> includeDependencies;
    /** A Set of EntityKey containing collected depending entities during dependency collection and processing phase */
    final Set<EntityDependency> dependencies;
    /** A Set of EntityKey containing collected bulk action processing results during processing phase */
    final Set<Result<EntityKey>> result;
    /** Indicates if this BulkAction has already been processed and is not valid anymore */
    boolean alreadyProcessed = false;

    public BulkAction(
            final BulkActionType type,
            final EntityType sourceType,
            final Collection<EntityKey> sources) {
        this(type, sourceType, sources, null);
    }

    public BulkAction(
            final BulkActionType type,
            final EntityType sourceType,
            final Collection<EntityKey> sources,
            final EnumSet<EntityType> includeDependencies) {

        this.type = type;
        this.sourceType = sourceType;
        this.sources = Utils.immutableSetOf(sources);
        this.includeDependencies = includeDependencies;
        this.dependencies = new LinkedHashSet<>();
        this.result = new HashSet<>();

        check();
    }

    public BulkAction(
            final BulkActionType type,
            final EntityType sourceType,
            final EntityKey... sources) {

        this(type, sourceType, (sources != null) ? Arrays.asList(sources) : Collections.emptyList());
    }

    public boolean includesDependencyType(final EntityType type) {
        return this.includeDependencies == null || this.includeDependencies.contains(type);
    }

    public Set<EntityDependency> getDependencies() {
        return Collections.unmodifiableSet(this.dependencies);
    }

    public Set<EntityKey> extractKeys(final EntityType type) {
        if (this.sourceType == type) {
            return Collections.unmodifiableSet(new HashSet<>(this.sources));
        }

        if (!this.dependencies.isEmpty()) {
            return Collections.unmodifiableSet(new HashSet<>(this.dependencies
                    .stream()
                    .filter(dependency -> dependency.self.entityType == type)
                    .map(dependency -> dependency.self)
                    .collect(Collectors.toList())));
        }

        return Collections.emptySet();
    }

    public UserLogActivityType getActivityType() {
        if (this.type == null) {
            return null;
        }

        switch (this.type) {
            case ACTIVATE:
                return UserLogActivityType.ACTIVATE;
            case DEACTIVATE:
                return UserLogActivityType.DEACTIVATE;
            case HARD_DELETE:
                return UserLogActivityType.DELETE;
            default:
                throw new IllegalStateException("There is no ActivityType mapped to the BulkActionType " + this.type);
        }
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
