/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction;

import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;

/** Service to address bulk actions like activation or deletion where the action
 * or state-change of one Entity has an effect on other entities that that has
 * a relation to the source entity.
 * <p>
 * A bulk action for a specified entity instance will be first applied to all its dependent
 * child-entities. For example if one is going to delete/deactivate a particular LMS Setup, all
 * Exams imported from this LMSSetup are first deactivated and all Exam Config Mapping and
 * all Client Connection are of all the Exams are deactivated first.
 * <p>
 * below is the relation-tree of known node-entities of the SEB Server application
 * <code>
 *                                  Institution
 *                        ____________ / | \________________________
 *                       /               |              \           \
 *                  LMS Setup            |          User-Account   Client Configuration
 *                      |                |
 *                      |  ______________+______________/
 *                      |/               |/
 *                    Exam       Exam Configuration
 *                      |\              /
 *                      | Exam Config Mapping
 *                      |
 *               Client Connection
 * </code> */
public interface BulkActionService {

    /** Use this to collect all EntityKey's of dependent entities for a given BulkAction.
     *
     * @param action the BulkAction defining the source entity keys and acts also as the
     *            dependency collector */
    void collectDependencies(BulkAction action);

    /** This executes a given BulkAction by first getting all dependencies and applying
     * the action to that first and then applying the action to the source entities of
     * the BulkAction.
     *
     * @param action the BulkAction that defines at least the type and the source entity keys
     * @return The BulkAction containing the result of the execution */
    Result<BulkAction> doBulkAction(BulkAction action);

    /** Creates a EntityProcessingReport from a given BulkAction result.
     * If the given BulkAction has not already been executed, it will be executed first
     *
     * @param action the BulkAction of a concrete type
     * @return EntityProcessingReport extracted form an executed BulkAction */
    Result<EntityProcessingReport> createReport(BulkAction action);

}