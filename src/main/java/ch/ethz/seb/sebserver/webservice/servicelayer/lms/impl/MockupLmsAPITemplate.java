/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;

final class MockupLmsAPITemplate implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(MockupLmsAPITemplate.class);

    private final LmsSetup lmsSetup;
    private final ClientCredentials credentials;
    private final Collection<QuizData> mockups;
    private final WebserviceInfo webserviceInfo;

    MockupLmsAPITemplate(
            final LmsSetup lmsSetup,
            final ClientCredentials credentials,
            final WebserviceInfo webserviceInfo) {

        this.lmsSetup = lmsSetup;
        this.credentials = credentials;
        this.webserviceInfo = webserviceInfo;

        final Long lmsSetupId = lmsSetup.id;
        final Long institutionId = lmsSetup.getInstitutionId();
        final LmsType lmsType = lmsSetup.getLmsType();
        this.mockups = new ArrayList<>();
        this.mockups.add(new QuizData(
                "quiz1", institutionId, lmsSetupId, lmsType, "Demo Quiz 1", "Demo Quit Mockup",
                "2020-01-01T09:00:00Z", "2021-01-01T09:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz2", institutionId, lmsSetupId, lmsType, "Demo Quiz 2", "Demo Quit Mockup",
                "2020-01-01T09:00:00Z", "2021-01-01T09:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz3", institutionId, lmsSetupId, lmsType, "Demo Quiz 3", "Demo Quit Mockup",
                "2018-07-30T09:00:00Z", "2018-08-01T00:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz4", institutionId, lmsSetupId, lmsType, "Demo Quiz 4", "Demo Quit Mockup",
                "2018-01-01T00:00:00Z", "2019-01-01T00:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz5", institutionId, lmsSetupId, lmsType, "Demo Quiz 5", "Demo Quit Mockup",
                "2018-01-01T09:00:00Z", "2021-01-01T09:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz6", institutionId, lmsSetupId, lmsType, "Demo Quiz 6", "Demo Quit Mockup",
                "2019-01-01T09:00:00Z", "2021-01-01T09:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz7", institutionId, lmsSetupId, lmsType, "Demo Quiz 7", "Demo Quit Mockup",
                "2018-01-01T09:00:00Z", "2021-01-01T09:00:00Z", "http://lms.mockup.com/api/"));
    }

    @Override
    public LmsSetup lmsSetup() {
        return this.lmsSetup;
    }

    @Override
    public LmsSetupTestResult testLmsSetup() {

        log.info("Test Lms Binding for Mockup and LmsSetup: {}", this.lmsSetup);

        final List<APIMessage> missingAttrs = attributeValidation(this.credentials);
        if (!missingAttrs.isEmpty()) {
            return LmsSetupTestResult.ofMissingAttributes(missingAttrs);
        }

        if (authenticate()) {
            return LmsSetupTestResult.ofOkay();
        } else {
            return LmsSetupTestResult.ofTokenRequestError("Illegal access");
        }
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {

        return Result.tryCatch(() -> {
            if (!authenticate()) {
                throw new IllegalArgumentException("Wrong clientId or secret");
            }

            final List<QuizData> quizzes = this.mockups
                    .stream()
                    .map(this::getExternalAddressAlias)
                    .filter(LmsAPIService.quizFilterFunction(filterMap))
                    .collect(Collectors.toList());

            return quizzes;
        });
    }

    @Override
    public Collection<Result<QuizData>> getQuizzes(final Set<String> ids) {
        if (!authenticate()) {
            throw new IllegalArgumentException("Wrong clientId or secret");
        }

        return this.mockups
                .stream()
                .map(this::getExternalAddressAlias)
                .filter(mockup -> ids.contains(mockup.id))
                .map(mockup -> Result.of(mockup))
                .collect(Collectors.toList());
    }

    private QuizData getExternalAddressAlias(final QuizData quizData) {
        final String externalAddressAlias = this.webserviceInfo.getExternalAddressAlias("lms.mockup.com");
        if (StringUtils.isNoneBlank(externalAddressAlias)) {
            try {

                if (log.isDebugEnabled()) {
                    log.debug("Found external address alias: {}", externalAddressAlias);
                }

                final URL url = new URL(this.lmsSetup.lmsApiUrl);
                final int port = url.getPort();
                final String _externalStartURI =
                        this.webserviceInfo.getHttpScheme() + "://" + externalAddressAlias + ":" + port + "/api/";

                log.info("Use external address for course access: {}", _externalStartURI);

                return new QuizData(
                        quizData.id, quizData.institutionId, quizData.lmsSetupId, quizData.lmsType,
                        quizData.name, quizData.description, quizData.startTime,
                        quizData.endTime, _externalStartURI, quizData.additionalAttributes);
            } catch (final Exception e) {
                log.error("Failed to create external address from alias: ", e);
                return quizData;
            }
        } else {
            return quizData;
        }
    }

    private boolean authenticate() {
        try {

            final CharSequence plainClientId = this.credentials.clientId;
            if (plainClientId == null || plainClientId.length() <= 0) {
                throw new IllegalAccessException("Wrong client credential");
            }

            return true;
        } catch (final Exception e) {
            log.info("Authentication failed: ", e);
            return false;
        }
    }

}
