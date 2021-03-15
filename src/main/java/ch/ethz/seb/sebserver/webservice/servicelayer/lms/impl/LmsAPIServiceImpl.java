/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplateFactory;

@Lazy
@Service
@WebServiceProfile
public class LmsAPIServiceImpl implements LmsAPIService {

    private static final Logger log = LoggerFactory.getLogger(LmsAPIServiceImpl.class);

    private final LmsSetupDAO lmsSetupDAO;
    private final ClientCredentialService clientCredentialService;
    private final EnumMap<LmsType, LmsAPITemplateFactory> templateFactories;

    private final Map<CacheKey, LmsAPITemplate> cache = new ConcurrentHashMap<>();

    public LmsAPIServiceImpl(
            final LmsSetupDAO lmsSetupDAO,
            final ClientCredentialService clientCredentialService,
            final Collection<LmsAPITemplateFactory> lmsAPITemplateFactories) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.clientCredentialService = clientCredentialService;

        final Map<LmsType, LmsAPITemplateFactory> factories = lmsAPITemplateFactories
                .stream()
                .collect(Collectors.toMap(
                        t -> t.lmsType(),
                        Function.identity()));
        this.templateFactories = new EnumMap<>(factories);
    }

    /** Listen to LmsSetupChangeEvent to release an affected LmsAPITemplate from cache
     *
     * @param event the event holding the changed LmsSetup */
    @EventListener
    public void notifyLmsSetupChange(final LmsSetupChangeEvent event) {
        final LmsSetup lmsSetup = event.getLmsSetup();
        if (lmsSetup == null) {
            return;
        }

        log.debug("LmsSetup changed. Update cache by removing eventually used references");

        this.cache.remove(new CacheKey(lmsSetup.getModelId(), 0));
    }

    @Override
    public Result<LmsSetup> getLmsSetup(final Long id) {
        return this.lmsSetupDAO.byPK(id);
    }

    @Override
    public Result<Page<QuizData>> requestQuizDataPage(
            final int pageNumber,
            final int pageSize,
            final String sort,
            final FilterMap filterMap) {

        return getAllQuizzesFromLMSSetups(filterMap)
                .map(LmsAPIService.quizzesSortFunction(sort))
                .map(LmsAPIService.quizzesToPageFunction(sort, pageNumber, pageSize));
    }

    @Override
    public Result<LmsAPITemplate> getLmsAPITemplate(final String lmsSetupId) {

        if (log.isDebugEnabled()) {
            log.debug("Get LmsAPITemplate for id: {}", lmsSetupId);
        }

        return Result.tryCatch(() -> this.lmsSetupDAO
                .byModelId(lmsSetupId)
                .getOrThrow())
                .flatMap(this::getLmsAPITemplate);
    }

    @Override
    public LmsSetupTestResult test(final LmsAPITemplate template) {
        final LmsSetupTestResult testCourseAccessAPI = template.testCourseAccessAPI();
        if (!testCourseAccessAPI.isOk()) {
            return testCourseAccessAPI;
        }

        if (template.lmsSetup().getLmsType().features.contains(LmsSetup.Features.SEB_RESTRICTION)) {
            return template.testCourseRestrictionAPI();
        }

        return LmsSetupTestResult.ofOkay();
    }

    @Override
    public LmsSetupTestResult testAdHoc(final LmsSetup lmsSetup) {
        final ClientCredentials lmsCredentials = this.clientCredentialService.encryptClientCredentials(
                lmsSetup.lmsAuthName,
                lmsSetup.lmsAuthSecret,
                lmsSetup.lmsRestApiToken);

        final ProxyData proxyData = (StringUtils.isNoneBlank(lmsSetup.proxyHost))
                ? new ProxyData(
                        lmsSetup.proxyHost,
                        lmsSetup.proxyPort,
                        this.clientCredentialService.encryptClientCredentials(
                                lmsSetup.proxyAuthUsername,
                                lmsSetup.proxyAuthSecret))
                : null;

        return test(createLmsSetupTemplate(lmsSetup, lmsCredentials, proxyData));
    }

    /** Collect all QuizData from all affecting LmsSetup.
     * If filterMap contains a LmsSetup identifier, only the QuizData from that LmsSetup is collected.
     * Otherwise QuizData from all active LmsSetup of the current institution are collected.
     *
     * @param filterMap the FilterMap containing either an LmsSetup identifier or an institution identifier
     * @return list of QuizData from all affecting LmsSetup */
    private Result<List<QuizData>> getAllQuizzesFromLMSSetups(final FilterMap filterMap) {

        return Result.tryCatch(() -> {
            // case 1. if lmsSetupId is available only get quizzes from specified LmsSetup
            final Long lmsSetupId = filterMap.getLmsSetupId();
            if (lmsSetupId != null) {
                try {
                    final Long institutionId = filterMap.getInstitutionId();

                    final LmsAPITemplate template = getLmsAPITemplate(lmsSetupId)
                            .getOrThrow();

                    if (institutionId != null && template.lmsSetup().institutionId != institutionId) {
                        return Collections.emptyList();
                    }
                    return template
                            .getQuizzes(filterMap)
                            .getOrThrow();
                } catch (final Exception e) {
                    log.error("Failed to get quizzes from LMS Setup: {}", lmsSetupId, e);
                    return Collections.emptyList();
                }
            }

            // case 2. get quizzes from all LmsSetups of specified institution
            final Long institutionId = filterMap.getInstitutionId();
            return this.lmsSetupDAO.all(institutionId, true)
                    .getOrThrow()
                    .parallelStream()
                    .map(this::getLmsAPITemplate)
                    .flatMap(Result::onErrorLogAndSkip)
                    .map(template -> template.getQuizzes(filterMap))
                    .flatMap(Result::onErrorLogAndSkip)
                    .flatMap(List::stream)
                    .distinct()
                    .collect(Collectors.toList());
        });
    }

    private Result<LmsAPITemplate> getLmsAPITemplate(final LmsSetup lmsSetup) {
        return Result.tryCatch(() -> {
            LmsAPITemplate lmsAPITemplate = getFromCache(lmsSetup);
            if (lmsAPITemplate == null) {
                lmsAPITemplate = createLmsSetupTemplate(lmsSetup);
                this.cache.put(new CacheKey(lmsSetup.getModelId(), System.currentTimeMillis()), lmsAPITemplate);
            }
            return lmsAPITemplate;
        });
    }

    private LmsAPITemplate getFromCache(final LmsSetup lmsSetup) {
        // first cleanup the cache by removing old instances
        final long currentTimeMillis = System.currentTimeMillis();
        new ArrayList<>(this.cache.keySet())
                .stream()
                .filter(key -> key.creationTimestamp - currentTimeMillis > Constants.DAY_IN_MILLIS)
                .forEach(this.cache::remove);
        // get from cache
        return this.cache.get(new CacheKey(lmsSetup.getModelId(), 0));

    }

    private LmsAPITemplate createLmsSetupTemplate(final LmsSetup lmsSetup) {

        if (log.isDebugEnabled()) {
            log.debug("Create new LmsAPITemplate for id: {}", lmsSetup.getModelId());
        }

        final ClientCredentials credentials = this.lmsSetupDAO
                .getLmsAPIAccessCredentials(lmsSetup.getModelId())
                .getOrThrow();

        final ProxyData proxyData = this.lmsSetupDAO
                .getLmsAPIAccessProxyData(lmsSetup.getModelId())
                .getOr(null);

        return createLmsSetupTemplate(lmsSetup, credentials, proxyData);
    }

    private LmsAPITemplate createLmsSetupTemplate(
            final LmsSetup lmsSetup,
            final ClientCredentials credentials,
            final ProxyData proxyData) {

        if (!this.templateFactories.containsKey(lmsSetup.lmsType)) {
            throw new UnsupportedOperationException("No support for LMS Type: " + lmsSetup.lmsType);
        }

        final LmsAPITemplateFactory lmsAPITemplateFactory = this.templateFactories.get(lmsSetup.lmsType);
        return lmsAPITemplateFactory.create(lmsSetup, credentials, proxyData)
                .getOrThrow();
    }

    private static final class CacheKey {
        final String lmsSetupId;
        final long creationTimestamp;
        final int hash;

        CacheKey(final String lmsSetupId, final long creationTimestamp) {
            this.lmsSetupId = lmsSetupId;
            this.creationTimestamp = creationTimestamp;
            final int prime = 31;
            int result = 1;
            result = prime * result + ((lmsSetupId == null) ? 0 : lmsSetupId.hashCode());
            this.hash = result;
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final CacheKey other = (CacheKey) obj;
            if (this.lmsSetupId == null) {
                if (other.lmsSetupId != null)
                    return false;
            } else if (!this.lmsSetupId.equals(other.lmsSetupId))
                return false;
            return true;
        }
    }

}
