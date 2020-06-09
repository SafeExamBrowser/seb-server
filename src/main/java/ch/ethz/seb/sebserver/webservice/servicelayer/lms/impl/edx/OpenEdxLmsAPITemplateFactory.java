/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.edx;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;

@Lazy
@Service
@WebServiceProfile
public class OpenEdxLmsAPITemplateFactory {

    private final JSONMapper jsonMapper;
    private final WebserviceInfo webserviceInfo;
    private final AsyncService asyncService;
    private final ClientCredentialService clientCredentialService;
    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    private final String[] alternativeTokenRequestPaths;
    private final int restrictionAPIPushCount;

    protected OpenEdxLmsAPITemplateFactory(
            final JSONMapper jsonMapper,
            final WebserviceInfo webserviceInfo,
            final AsyncService asyncService,
            final ClientCredentialService clientCredentialService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            @Value("${sebserver.webservice.lms.openedx.api.token.request.paths}") final String alternativeTokenRequestPaths,
            @Value("${sebserver.webservice.lms.openedx.seb.restriction.push-count:0}") final int restrictionAPIPushCount) {

        this.jsonMapper = jsonMapper;
        this.webserviceInfo = webserviceInfo;
        this.asyncService = asyncService;
        this.clientCredentialService = clientCredentialService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        this.alternativeTokenRequestPaths = (alternativeTokenRequestPaths != null)
                ? StringUtils.split(alternativeTokenRequestPaths, Constants.LIST_SEPARATOR)
                : null;
        this.restrictionAPIPushCount = restrictionAPIPushCount;
    }

    public Result<OpenEdxLmsAPITemplate> create(
            final LmsSetup lmsSetup,
            final ClientCredentials credentials,
            final ProxyData proxyData) {

        return Result.tryCatch(() -> {

            final OpenEdxRestTemplateFactory openEdxRestTemplateFactory = new OpenEdxRestTemplateFactory(
                    lmsSetup,
                    credentials,
                    proxyData,
                    this.clientCredentialService,
                    this.clientHttpRequestFactoryService,
                    this.alternativeTokenRequestPaths);

            final OpenEdxCourseAccess openEdxCourseAccess = new OpenEdxCourseAccess(
                    lmsSetup,
                    openEdxRestTemplateFactory,
                    this.webserviceInfo,
                    this.asyncService);

            final OpenEdxCourseRestriction openEdxCourseRestriction = new OpenEdxCourseRestriction(
                    lmsSetup,
                    this.jsonMapper,
                    openEdxRestTemplateFactory,
                    this.restrictionAPIPushCount);

            return new OpenEdxLmsAPITemplate(
                    lmsSetup,
                    openEdxCourseAccess,
                    openEdxCourseRestriction);
        });
    }

}
