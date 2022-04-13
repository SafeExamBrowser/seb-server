/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.IndicatorTemplate;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;

/** Concrete EntityDAO interface of ExamTemplate entities */
public interface ExamTemplateDAO extends EntityDAO<ExamTemplate, ExamTemplate>, BulkActionSupportDAO<ExamTemplate> {

    /** Used to get the ExamTemplate that is set as default for a given institution.
     *
     * @param institutionId The institution identifier
     * @return Result refer to the ExamTemplate instance or to an error when happened */
    Result<ExamTemplate> getInstitutionalDefault(Long institutionId);

    /** Creates a new indicator template
     *
     * @param indicatorTemplate The IndicatorTemplate refer also to the exam template (examTemplateId)
     * @return Result refer to the created IndicatorTemplate or to an error when happened */
    Result<IndicatorTemplate> createNewIndicatorTemplate(IndicatorTemplate indicatorTemplate);

    /** Saves an already existing indicator template
     *
     * @param indicatorTemplate The IndicatorTemplate refer also to the exam template (examTemplateId)
     * @return Result refer to the saved IndicatorTemplate or to an error when happened */
    Result<IndicatorTemplate> saveIndicatorTemplate(IndicatorTemplate indicatorTemplate);

    /** Deletes an already existing indicator template
     *
     * @param indicatorTemplate The IndicatorTemplate refer also to the exam template (examTemplateId)
     * @return Result refer to the EntityKey of the deleted IndicatorTemplate or to an error when happened */
    Result<EntityKey> deleteIndicatorTemplate(String examTemplateId, String indicatorTemplateId);

}
