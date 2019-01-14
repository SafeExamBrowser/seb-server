/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;

@Service
@WebServiceProfile
public class LmsAPIServiceImpl implements LmsAPIService {

    private final LmsSetupDAO lmsSetupDAO;

    public LmsAPIServiceImpl(final LmsSetupDAO lmsSetupDAO) {
        this.lmsSetupDAO = lmsSetupDAO;
    }

    @Override
    public Result<LmsAPITemplate> createConnectionTemplate(final Long lmsSetupId) {
        return this.lmsSetupDAO
                .byId(lmsSetupId)
                .flatMap(this::createConnectionTemplate);
    }

    @Override
    public Result<LmsAPITemplate> createConnectionTemplate(final LmsSetup lmsSetup) {
        switch (lmsSetup.lmsType) {
            case MOCKUP:
                return Result.of(new MockupLmsAPITemplate(lmsSetup));
            default:
                return Result.ofError(
                        new UnsupportedOperationException("No support for LMS Type: " + lmsSetup.lmsType));
        }
    }

}
