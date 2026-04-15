/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroupTemplate;
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

    Result<Collection<ExamTemplate>> getAllForLMSIntegration(Long institutionId);

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
     * @param examTemplateId the ExamTemplate id where the specified IndicatorTemplate shall be deleted from
     * @param indicatorTemplateId the id of the IndicatorTemplate to delete
     * @return Result refer to the EntityKey of the deleted IndicatorTemplate or to an error when happened */
    Result<EntityKey> deleteIndicatorTemplate(String examTemplateId, String indicatorTemplateId);

    Result<Collection<ClientGroupTemplate>> getClientGroupTemplates(Long examTemplateId);

    /** Creates a new client group template
     *
     * @param clientGroupTemplate The ClientGroupTemplate refer also to the exam template (examTemplateId)
     * @return Result refer to the created ClientGroupTemplate or to an error when happened */
    Result<ClientGroupTemplate> createNewClientGroupTemplate(ClientGroupTemplate clientGroupTemplate);

    /** Saves an already existing client group template
     *
     * @param clientGroupTemplate The ClientGroupTemplate refer also to the exam template (examTemplateId)
     * @return Result refer to the saved ClientGroupTemplate or to an error when happened */
    Result<ClientGroupTemplate> saveClientGroupTemplate(ClientGroupTemplate clientGroupTemplate);

    /** Deletes an already existing client group template
     *
     * @param examTemplateId the ExamTemplate id where the specified ClientGroupTemplate shall be deleted from
     * @param clientGroupTemplateId the id of the ClientGroupTemplate to delete
     * @return Result refer to the EntityKey of the deleted ClientGroupTemplate or to an error when happened */
    Result<EntityKey> deleteClientGroupTemplate(String examTemplateId, String clientGroupTemplateId);

    /** Gets a valid copy name for the given ExamTemplate.
     *  uses "[name] (copy)" if already exists --> "[name] (copy 2)" --> if already exists --> "[name] (copy 3)"...
     *
     * @param sourceExamTemplate The source ExamTemplate to get a copy name for
     * @return the new name for the copy.
     */
    String getCopyName(ExamTemplate sourceExamTemplate);

    /** Indicates if there exists any Exam Template that uses the Configuration Template with given id.
     *
     * @param configTemplateId The Configuration Template identifier
     * @return true if there exists any Exam Template that uses the Configuration Template with given id */
    boolean hasAnyExamTemplateWithConfigTemplate(Long configTemplateId);
}
