/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.util.List;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

/** The base interface and service for all SEB Exam Configuration Template related functionality. */
public interface ExamConfigTemplateService {

    /** Use this to get filtered and sorted TemplateAttribute for a specified exam configuration template.
     *
     * @param institutionId The institution identifier of the exam configuration template
     * @param templateId The exam configuration template identifier
     * @param sort The sort column name
     * @param filterMap The filter map containing all filter attributes
     * @return Result revers to a sorted list of given TemplateAttribute or to an error if happened. */
    Result<List<TemplateAttribute>> getTemplateAttributes(
            final Long institutionId,
            final Long templateId,
            final String sort,
            final FilterMap filterMap);

    /** Use this to get a specified TemplateAttribute for a specified exam configuration template
     *
     * @param institutionId The institution identifier of the exam configuration template
     * @param templateId The exam configuration template identifier
     * @param attributeId The TemplateAttribute identifier */
    Result<TemplateAttribute> getAttribute(
            final Long institutionId,
            final Long templateId,
            final Long attributeId);

    /** Sets the default value for a specific TemplateAttribute on a specified exam configuration template
     *
     * @param institutionId The institution identifier of the exam configuration template
     * @param templateId The exam configuration template identifier
     * @param attributeId The TemplateAttribute identifier */
    Result<TemplateAttribute> setDefaultValues(
            final Long institutionId,
            final Long templateId,
            final Long attributeId);

    /** Removes the orientation from the view of a specified exam configuration template for a
     * specific TemplateAttribute. This TemplateAttribute will then not be shown on the view anymore.
     *
     * @param institutionId The institution identifier of the exam configuration template
     * @param templateId The exam configuration template identifier
     * @param attributeId The TemplateAttribute identifier */
    Result<TemplateAttribute> removeOrientation(
            final Long institutionId,
            final Long templateId,
            final Long attributeId);

    /** Adds the orientation from the default view of a specified exam configuration template for a
     * specific TemplateAttribute. This TemplateAttribute will then be shown on the specified view
     * of the template.
     *
     * @param institutionId The institution identifier of the exam configuration template
     * @param templateId The exam configuration template identifier
     * @param attributeId The TemplateAttribute identifier
     * @param viewId The view identifier to attach the TemplateAttribute to */
    Result<TemplateAttribute> attachDefaultOrientation(
            final Long institutionId,
            final Long templateId,
            final Long attributeId,
            Long viewId);

}
