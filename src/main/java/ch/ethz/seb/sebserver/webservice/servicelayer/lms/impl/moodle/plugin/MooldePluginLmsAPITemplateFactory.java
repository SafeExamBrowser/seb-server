/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamConfigurationValueService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.LmsAPITemplateAdapter;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactoryImpl;

@Lazy
@Service
@WebServiceProfile
public class MooldePluginLmsAPITemplateFactory implements LmsAPITemplateFactory {

    public static final String SEB_SERVER_SERVICE_NAME = "SEB-Server-Webservice";

    private final JSONMapper jsonMapper;
    private final CacheManager cacheManager;
    private final AsyncService asyncService;
    private final Environment environment;
    private final ClientCredentialService clientCredentialService;
    private final ExamConfigurationValueService examConfigurationValueService;
    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    private final String[] alternativeTokenRequestPaths;
    private final boolean applyNameCriteria;

    protected MooldePluginLmsAPITemplateFactory(
            final JSONMapper jsonMapper,
            final CacheManager cacheManager,
            final AsyncService asyncService,
            final Environment environment,
            final ClientCredentialService clientCredentialService,
            final ExamConfigurationValueService examConfigurationValueService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            @Value("${sebserver.webservice.lms.moodle.api.token.request.paths:}") final String alternativeTokenRequestPaths,
            @Value("${sebserver.webservice.lms.moodle.fetch.applyNameCriteria:true}") final boolean applyNameCriteria) {

        this.jsonMapper = jsonMapper;
        this.cacheManager = cacheManager;
        this.asyncService = asyncService;
        this.environment = environment;
        this.clientCredentialService = clientCredentialService;
        this.examConfigurationValueService = examConfigurationValueService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        this.alternativeTokenRequestPaths = (alternativeTokenRequestPaths != null)
                ? StringUtils.split(alternativeTokenRequestPaths, Constants.LIST_SEPARATOR)
                : null;
        this.applyNameCriteria = applyNameCriteria;
    }

    @Override
    public LmsType lmsType() {
        return LmsType.MOODLE_PLUGIN;
    }

    @Override
    public Result<LmsAPITemplate> create(final APITemplateDataSupplier apiTemplateDataSupplier) {
        return Result.tryCatch(() -> {

            final MoodleRestTemplateFactory moodleRestTemplateFactory = new MoodleRestTemplateFactoryImpl(
                    this.jsonMapper,
                    apiTemplateDataSupplier,
                    this.clientCredentialService,
                    this.clientHttpRequestFactoryService,
                    this.alternativeTokenRequestPaths);

            final MoodlePluginCourseAccess moodlePluginCourseAccess = new MoodlePluginCourseAccess(
                    this.jsonMapper,
                    this.asyncService,
                    moodleRestTemplateFactory,
                    this.cacheManager,
                    this.environment,
                    this.applyNameCriteria);

            final MoodlePluginCourseRestriction moodlePluginCourseRestriction = new MoodlePluginCourseRestriction(
                    this.jsonMapper,
                    moodleRestTemplateFactory,
                    this.examConfigurationValueService);

            final MoodlePluginFullIntegration moodlePluginFullIntegration = new MoodlePluginFullIntegration(
                    this.jsonMapper,
                    moodleRestTemplateFactory
            );

            return new LmsAPITemplateAdapter(
                    this.asyncService,
                    this.environment,
                    apiTemplateDataSupplier,
                    moodlePluginCourseAccess,
                    moodlePluginCourseRestriction,
                    moodlePluginFullIntegration);
        });
    }

}
