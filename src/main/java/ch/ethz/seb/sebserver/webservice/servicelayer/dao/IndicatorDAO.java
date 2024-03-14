/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;

/** Concrete EntityDAO interface of Indicator entities */
public interface IndicatorDAO extends EntityDAO<Indicator, Indicator>, BulkActionSupportDAO<Indicator> {

    /** Get a collection of all Indicator entities for a specified exam.
     *
     * @param examId the Exam identifier to get the Indicators for
     * @return Result referring to the collection of Indicators of an Exam or to an error if happened */
    Result<Collection<Indicator>> allForExam(Long examId);

    /** Delete all indicators for a particular exam.
     *
     * @param examId the exam identifier
     * @return Result refer to the list of deleted indicators or to an error when happened */
    Result<Collection<EntityKey>> deleteAllForExam(Long examId);

}
