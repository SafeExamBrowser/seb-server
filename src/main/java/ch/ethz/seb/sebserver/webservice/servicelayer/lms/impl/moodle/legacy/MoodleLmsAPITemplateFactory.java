/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.LmsAPITemplateAdapter;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactoryImpl;

@Lazy
@Service
@WebServiceProfile
public class MoodleLmsAPITemplateFactory implements LmsAPITemplateFactory {

    static final String MOODLE_MOBILE_APP_SERVICE = "moodle_mobile_app";

    private final JSONMapper jsonMapper;
    private final AsyncService asyncService;
    private final Environment environment;
    private final ClientCredentialService clientCredentialService;
    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    private final String[] alternativeTokenRequestPaths;

    protected MoodleLmsAPITemplateFactory(
            final JSONMapper jsonMapper,
            final AsyncService asyncService,
            final Environment environment,
            final ClientCredentialService clientCredentialService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final ApplicationContext applicationContext,
            @Value("${sebserver.webservice.lms.moodle.api.token.request.paths:}") final String alternativeTokenRequestPaths) {

        this.jsonMapper = jsonMapper;
        this.asyncService = asyncService;
        this.environment = environment;
        this.clientCredentialService = clientCredentialService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        this.alternativeTokenRequestPaths = (alternativeTokenRequestPaths != null)
                ? StringUtils.split(alternativeTokenRequestPaths, Constants.LIST_SEPARATOR)
                : null;
    }

    @Override
    public LmsType lmsType() {
        return LmsType.MOODLE;
    }

    @Override
    public Result<LmsAPITemplate> create(final APITemplateDataSupplier apiTemplateDataSupplier) {

        return Result.tryCatch(() -> {

            final MoodleRestTemplateFactory restTemplateFactory = new MoodleRestTemplateFactoryImpl(
                    this.jsonMapper,
                    apiTemplateDataSupplier,
                    this.clientCredentialService,
                    this.clientHttpRequestFactoryService,
                    this.alternativeTokenRequestPaths);

            final MoodleCourseAccess moodleCourseAccess = new MoodleCourseAccess(
                    this.jsonMapper,
                    this.asyncService,
                    restTemplateFactory,
                    this.environment);

            return new LmsAPITemplateAdapter(
                    this.asyncService,
                    this.environment,
                    apiTemplateDataSupplier,
                    moodleCourseAccess,
                    new MoodleCourseRestriction(),
                    null);

        });
    }

}
