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

public interface SebExamConfigTemplateService {

    Result<List<TemplateAttribute>> getTemplateAttributes(
            final Long institutionId,
            final Long templateId,
            final String sort,
            final FilterMap filterMap);

    Result<TemplateAttribute> getAttribute(
            final Long institutionId,
            final Long templateId,
            final Long attributeId);

    Result<TemplateAttribute> setDefaultValues(
            final Long institutionId,
            final Long templateId,
            final Long attributeId);

    Result<TemplateAttribute> removeOrientation(
            final Long institutionId,
            final Long templateId,
            final Long attributeId);

    Result<TemplateAttribute> attachDefaultOrientation(
            final Long institutionId,
            final Long templateId,
            final Long attributeId,
            Long viewId);

}
