/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;

/** Concrete EntityDAO interface of ClientGroup entities */
public interface ClientGroupDAO extends EntityDAO<ClientGroup, ClientGroup>, BulkActionSupportDAO<ClientGroup> {

    /** Get a collection of all ClientGroup entities for a specified exam.
     *
     * @param examId the Exam identifier to get the ClientGroups for
     * @return Result referring to the collection of ClientGroups of an Exam or to an error if happened */
    Result<Collection<ClientGroup>> allForExam(Long examId);

}
