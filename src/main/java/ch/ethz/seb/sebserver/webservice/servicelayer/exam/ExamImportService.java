/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ExamImportService {

    /** Applies fill Exam import initialization
     *
     * @param exam The Exam to initialize import data. Also uses ExamTemplate if an examTemplateId is set for the Exam
     * @param applyAllGroups Indicates whether all SEB Groups should be created or not.
     * @return Result refer to the initialized Exam or to an error when happened */
    Result<Exam> applyExamImportInitialization(Exam exam, boolean applyAllGroups);

    /** Initializes initial additional attributes for a yet created exam.
     *
     * @param exam The exam that has been created
     * @return The exam with the initial additional attributes */
    Result<Exam> initAdditionalAttributes(Exam exam);
}
