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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@Service
@WebServiceProfile
public class BulkActionService {

    private final Map<EntityType, BulkActionSupport> supporter;
    private final UserActivityLogDAO userActivityLogDAO;

    public BulkActionService(
            final Collection<BulkActionSupport> supporter,
            final UserActivityLogDAO userActivityLogDAO) {

        this.supporter = new HashMap<>();
        for (final BulkActionSupport support : supporter) {
            this.supporter.put(support.entityType(), support);
        }
        this.userActivityLogDAO = userActivityLogDAO;
    }

    public void collectDependencies(final BulkAction action) {
        checkProcessing(action);
        for (final BulkActionSupport sup : this.supporter.values()) {
            action.dependencies.addAll(sup.getDependencies(action));
        }
        action.alreadyProcessed = true;
    }

    public void doBulkAction(final BulkAction action) {
        checkProcessing(action);

        final BulkActionSupport supportForSource = this.supporter.get(action.sourceType);
        if (supportForSource == null) {
            action.alreadyProcessed = true;
            return;
        }

        collectDependencies(action);

        if (!action.dependencies.isEmpty()) {
            // process dependencies first...
            final List<BulkActionSupport> dependancySupporter =
                    getDependancySupporter(action);

            for (final BulkActionSupport support : dependancySupporter) {
                action.result.addAll(support.processBulkAction(action));
            }
        }

        action.result.addAll(supportForSource.processBulkAction(action));

        processUserActivityLog(action);
        action.alreadyProcessed = true;
    }

    public EntityProcessingReport createReport(final BulkAction action) {
        if (!action.alreadyProcessed) {
            doBulkAction(action);
        }

        // TODO

        return new EntityProcessingReport(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap());
    }

    private void processUserActivityLog(final BulkAction action) {
        if (action.type.activityType == null) {
            return;
        }

        for (final EntityKey key : action.dependencies) {
            this.userActivityLogDAO.log(
                    action.type.activityType,
                    key.entityType,
                    key.entityId,
                    "bulk action dependency");
        }

        for (final EntityKey key : action.sources) {
            this.userActivityLogDAO.log(
                    action.type.activityType,
                    key.entityType,
                    key.entityId,
                    "bulk action source");
        }
    }

    private List<BulkActionSupport> getDependancySupporter(final BulkAction action) {
        switch (action.type) {
            case ACTIVATE:
            case DEACTIVATE:
            case HARD_DELETE: {
                final List<BulkActionSupport> dependantSupporterInHierarchicalOrder =
                        getDependantSupporterInHierarchicalOrder(action);
                Collections.reverse(dependantSupporterInHierarchicalOrder);
                return dependantSupporterInHierarchicalOrder;
            }
            default:
                return getDependantSupporterInHierarchicalOrder(action);
        }
    }

    private List<BulkActionSupport> getDependantSupporterInHierarchicalOrder(final BulkAction action) {
        switch (action.sourceType) {
            case INSTITUTION:
                return Arrays.asList(
                        this.supporter.get(EntityType.LMS_SETUP),
                        this.supporter.get(EntityType.USER),
                        this.supporter.get(EntityType.EXAM),
                        this.supporter.get(EntityType.CLIENT_CONNECTION),
                        this.supporter.get(EntityType.CONFIGURATION_NODE));
            case LMS_SETUP:
                return Arrays.asList(
                        this.supporter.get(EntityType.EXAM),
                        this.supporter.get(EntityType.CLIENT_CONNECTION));
            case USER:
                return Arrays.asList(
                        this.supporter.get(EntityType.EXAM),
                        this.supporter.get(EntityType.CLIENT_CONNECTION),
                        this.supporter.get(EntityType.CONFIGURATION_NODE));
            case EXAM:
                return Arrays.asList(
                        this.supporter.get(EntityType.EXAM),
                        this.supporter.get(EntityType.CLIENT_CONNECTION));
            case CONFIGURATION:
                return Arrays.asList(
                        this.supporter.get(EntityType.EXAM),
                        this.supporter.get(EntityType.CLIENT_CONNECTION));
            default:
                return Collections.emptyList();
        }
    }

    private void checkProcessing(final BulkAction action) {
        if (action.alreadyProcessed) {
            throw new IllegalStateException("Given BulkAction has already been processed. Use a new one");
        }
    }

}
