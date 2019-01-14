/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@Service
@WebServiceProfile
public class BulkActionService {

    private final Collection<BulkActionSupport> supporter;

    public BulkActionService(final Collection<BulkActionSupport> supporter) {
        this.supporter = supporter;
    }

    public void collectDependencies(final BulkAction action) {
        checkProcessing(action);
        for (final BulkActionSupport sup : this.supporter) {
            action.dependencies.addAll(sup.getDependencies(action));
        }
        action.alreadyProcessed = true;
    }

    public void doBulkAction(final BulkAction action) {
        checkProcessing(action);

        final BulkActionSupport supportForSource = getSupporterForSource(action);
        if (supportForSource == null) {
            action.alreadyProcessed = true;
            return;
        }

        collectDependencies(action);

        if (!action.dependencies.isEmpty()) {
            // process dependencies first...
            final List<BulkActionSupport> dependantSupporterInHierarchicalOrder =
                    getDependantSupporterInHierarchicalOrder(action);

            for (final BulkActionSupport support : dependantSupporterInHierarchicalOrder) {
                action.result.addAll(support.processBulkAction(action));
            }
        }

        // process bulk action
        action.result.addAll(supportForSource.processBulkAction(action));
        action.alreadyProcessed = true;
    }

    public EntityProcessingReport createReport(final BulkAction action) {
        if (!action.alreadyProcessed) {
            doBulkAction(action);
        }

        final EntityProcessingReport report = new EntityProcessingReport();

        // TODO

        return report;
    }

    private BulkActionSupport getSupporterForSource(final BulkAction action) {
        for (final BulkActionSupport support : this.supporter) {
            if (support.entityType() == action.sourceType) {
                return support;
            }
        }

        return null;
    }

    private List<BulkActionSupport> getDependantSupporterInHierarchicalOrder(final BulkAction action) {

        // TODO

        return null;
    }

    private void checkProcessing(final BulkAction action) {
        if (action.alreadyProcessed) {
            throw new IllegalStateException("Given BulkAction has already been processed. Use a new one");
        }
    }

}
