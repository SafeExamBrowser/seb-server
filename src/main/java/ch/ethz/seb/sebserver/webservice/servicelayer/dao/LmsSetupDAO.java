/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;

public interface LmsSetupDAO extends ActivatableEntityDAO<LmsSetup>, BulkActionSupportDAO<LmsSetup> {

    @Transactional(readOnly = true)
    default Result<Collection<LmsSetup>> allOfInstitution(final Long institutionId, final Boolean active) {
        return allMatching(institutionId, null, null, active);
    }

    Result<Collection<LmsSetup>> allMatching(Long institutionId, String name, LmsType lmsType, Boolean active);

    Result<LmsSetup> save(LmsSetup lmsSetup);

}
