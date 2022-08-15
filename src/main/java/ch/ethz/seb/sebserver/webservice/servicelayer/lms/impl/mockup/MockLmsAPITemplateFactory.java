/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.mockup;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.LmsAPITemplateAdapter;

@Lazy
@Service
@WebServiceProfile
public class MockLmsAPITemplateFactory implements LmsAPITemplateFactory {

    private final AsyncService asyncService;
    private final WebserviceInfo webserviceInfo;
    private final Environment environment;

    public MockLmsAPITemplateFactory(
            final AsyncService asyncService,
            final Environment environment,
            final WebserviceInfo webserviceInfo) {

        this.environment = environment;
        this.asyncService = asyncService;
        this.webserviceInfo = webserviceInfo;
    }

    @Override
    public LmsType lmsType() {
        return LmsType.MOCKUP;
    }

    @Override
    public Result<LmsAPITemplate> create(final APITemplateDataSupplier apiTemplateDataSupplier) {

        final MockCourseAccessAPI mockCourseAccessAPI = new MockCourseAccessAPI(
                apiTemplateDataSupplier,
                this.webserviceInfo);

        final MockSEBRestrictionAPI mockSEBRestrictionAPI = new MockSEBRestrictionAPI();

        return Result.tryCatch(() -> new LmsAPITemplateAdapter(
                this.asyncService,
                this.environment,
                apiTemplateDataSupplier,
                mockCourseAccessAPI,
                mockSEBRestrictionAPI));
    }

}
