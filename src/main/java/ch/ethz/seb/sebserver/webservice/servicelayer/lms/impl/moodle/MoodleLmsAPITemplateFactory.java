/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle;

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
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.LmsAPITemplateAdapter;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy.MoodleCourseAccess;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy.MoodleCourseDataAsyncLoader;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy.MoodleCourseRestriction;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy.MoodleRestTemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin.MoodlePluginCheck;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin.MoodlePluginCourseAccess;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin.MoodlePluginCourseRestriction;

@Lazy
@Service
@WebServiceProfile
public class MoodleLmsAPITemplateFactory implements LmsAPITemplateFactory {

    private final MoodlePluginCheck moodlePluginCheck;
    private final JSONMapper jsonMapper;
    private final AsyncService asyncService;
    private final Environment environment;
    private final ClientCredentialService clientCredentialService;
    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    private final ApplicationContext applicationContext;
    private final String[] alternativeTokenRequestPaths;

    protected MoodleLmsAPITemplateFactory(
            final MoodlePluginCheck moodlePluginCheck,
            final JSONMapper jsonMapper,
            final AsyncService asyncService,
            final Environment environment,
            final ClientCredentialService clientCredentialService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final ApplicationContext applicationContext,
            @Value("${sebserver.webservice.lms.moodle.api.token.request.paths:}") final String alternativeTokenRequestPaths) {

        this.moodlePluginCheck = moodlePluginCheck;
        this.jsonMapper = jsonMapper;
        this.asyncService = asyncService;
        this.environment = environment;
        this.clientCredentialService = clientCredentialService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        this.applicationContext = applicationContext;
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

            final LmsSetup lmsSetup = apiTemplateDataSupplier.getLmsSetup();
            final MoodleCourseDataAsyncLoader asyncLoaderPrototype = this.applicationContext
                    .getBean(MoodleCourseDataAsyncLoader.class);
            asyncLoaderPrototype.init(lmsSetup.getModelId());

            if (this.moodlePluginCheck.checkPluginAvailable(lmsSetup)) {

                final MoodlePluginCourseAccess moodlePluginCourseAccess = new MoodlePluginCourseAccess();
                final MoodlePluginCourseRestriction moodlePluginCourseRestriction = new MoodlePluginCourseRestriction();

                return new LmsAPITemplateAdapter(
                        this.asyncService,
                        this.environment,
                        apiTemplateDataSupplier,
                        moodlePluginCourseAccess,
                        moodlePluginCourseRestriction);

            } else {

                final MoodleRestTemplateFactory moodleRestTemplateFactory = new MoodleRestTemplateFactory(
                        this.jsonMapper,
                        apiTemplateDataSupplier,
                        this.clientCredentialService,
                        this.clientHttpRequestFactoryService,
                        this.alternativeTokenRequestPaths);

                final MoodleCourseAccess moodleCourseAccess = new MoodleCourseAccess(
                        this.jsonMapper,
                        moodleRestTemplateFactory,
                        asyncLoaderPrototype,
                        this.environment);

                final MoodleCourseRestriction moodleCourseRestriction = new MoodleCourseRestriction(
                        this.jsonMapper,
                        moodleRestTemplateFactory);

                return new LmsAPITemplateAdapter(
                        this.asyncService,
                        this.environment,
                        apiTemplateDataSupplier,
                        moodleCourseAccess,
                        moodleCourseRestriction);
            }
        });
    }

}
