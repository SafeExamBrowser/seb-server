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
import ch.ethz.seb.sebserver.gbl.api.EntityType;
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
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplateFactory;

@Lazy
@Service
@WebServiceProfile
public class LmsAPIServiceImpl implements LmsAPIService {

    private static final Logger log = LoggerFactory.getLogger(LmsAPIServiceImpl.class);

    private final WebserviceInfo webserviceInfo;
    private final LmsSetupDAO lmsSetupDAO;
    private final ClientCredentialService clientCredentialService;
    private final EnumMap<LmsType, LmsAPITemplateFactory> templateFactories;

    private final Map<CacheKey, LmsAPITemplate> cache = new ConcurrentHashMap<>();

    public LmsAPIServiceImpl(
            final WebserviceInfo webserviceInfo,
            final LmsSetupDAO lmsSetupDAO,
            final ClientCredentialService clientCredentialService,
            final Collection<LmsAPITemplateFactory> lmsAPITemplateFactories) {

        this.webserviceInfo = webserviceInfo;
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

        if (log.isDebugEnabled()) {
            log.debug("LmsSetup changed. Update cache by removing eventually used references");
        }

        final LmsAPITemplate removedTemplate = this.cache
                .remove(new CacheKey(lmsSetup.getModelId(), 0));
        if (removedTemplate != null) {
            removedTemplate.clearCache();
        }
    }

    @Override
    public void cleanup() {
        this.cache.clear();
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
        return Result.tryCatch(() -> {
            synchronized (this) {
                LmsAPITemplate lmsAPITemplate = getFromCache(lmsSetupId);
                if (lmsAPITemplate == null) {
                    lmsAPITemplate = createLmsSetupTemplate(lmsSetupId);
                    if (lmsAPITemplate != null) {
                        this.cache.put(new CacheKey(lmsSetupId, System.currentTimeMillis()), lmsAPITemplate);
                    }
                }
                if (lmsAPITemplate == null) {
                    throw new ResourceNotFoundException(EntityType.LMS_SETUP, lmsSetupId);
                }

                return lmsAPITemplate;
            }
        });
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

        return LmsSetupTestResult.ofOkay(template.lmsSetup().getLmsType());
    }

    @Override
    public LmsSetupTestResult testAdHoc(final LmsSetup lmsSetup) {
        final AdHocAPITemplateDataSupplier apiTemplateDataSupplier = new AdHocAPITemplateDataSupplier(
                lmsSetup,
                this.clientCredentialService);

        final LmsAPITemplate lmsSetupTemplate = createLmsSetupTemplate(apiTemplateDataSupplier);

        final LmsSetupTestResult testCourseAccessAPI = lmsSetupTemplate.testCourseAccessAPI();
        if (!testCourseAccessAPI.isOk()) {
            return testCourseAccessAPI;
        }

        if (lmsSetupTemplate.lmsSetup().getLmsType().features.contains(LmsSetup.Features.SEB_RESTRICTION)) {
            return lmsSetupTemplate.testCourseRestrictionAPI();
        }

        return LmsSetupTestResult.ofOkay(lmsSetupTemplate.lmsSetup().getLmsType());
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

                    if (institutionId != null && !institutionId.equals(template.lmsSetup().institutionId)) {
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
                    .map(LmsSetup::getModelId)
                    .map(this::getLmsAPITemplate)
                    .flatMap(Result::onErrorLogAndSkip)
                    .map(template -> template.getQuizzes(filterMap))
                    .flatMap(Result::onErrorLogAndSkip)
                    .flatMap(List::stream)
                    .distinct()
                    .collect(Collectors.toList());
        });
    }

    private LmsAPITemplate getFromCache(final String lmsSetupId) {
        // first cleanup the cache by removing old instances
        final long currentTimeMillis = System.currentTimeMillis();
        new ArrayList<>(this.cache.keySet())
                .stream()
                .filter(key -> key.creationTimestamp - currentTimeMillis > Constants.DAY_IN_MILLIS)
                .forEach(this.cache::remove);
        // get from cache
        final CacheKey cacheKey = new CacheKey(lmsSetupId, 0);
        final LmsAPITemplate lmsAPITemplate = this.cache.get(cacheKey);

        // in distributed setup, check if lmsSetup is up to date
        if (this.webserviceInfo.isDistributed()) {
            if (lmsAPITemplate == null) {
                return null;
            }

            final LmsSetup lmsSetup = lmsAPITemplate.lmsSetup();
            if (!this.lmsSetupDAO.isUpToDate(lmsSetup)) {
                this.cache.remove(cacheKey);
                return null;
            }
        }

        return lmsAPITemplate;
    }

    private LmsAPITemplate createLmsSetupTemplate(final String lmsSetupId) {

        if (log.isDebugEnabled()) {
            log.debug("Create new LmsAPITemplate for id: {}", lmsSetupId);
        }

        return createLmsSetupTemplate(new PersistentAPITemplateDataSupplier(
                lmsSetupId,
                this.lmsSetupDAO));
    }

    private LmsAPITemplate createLmsSetupTemplate(final APITemplateDataSupplier apiTemplateDataSupplier) {

        final LmsType lmsType = apiTemplateDataSupplier.getLmsSetup().lmsType;

        if (!this.templateFactories.containsKey(lmsType)) {
            throw new UnsupportedOperationException("No support for LMS Type: " + lmsType);
        }

        final LmsAPITemplateFactory lmsAPITemplateFactory = this.templateFactories
                .get(lmsType);

        return lmsAPITemplateFactory
                .create(apiTemplateDataSupplier)
                .getOrThrow();
    }

    /** Used to always get the actual LMS connection data from persistent */
    private static final class PersistentAPITemplateDataSupplier implements APITemplateDataSupplier {

        private final String lmsSetupId;
        private final LmsSetupDAO lmsSetupDAO;

        public PersistentAPITemplateDataSupplier(final String lmsSetupId, final LmsSetupDAO lmsSetupDAO) {
            this.lmsSetupId = lmsSetupId;
            this.lmsSetupDAO = lmsSetupDAO;
        }

        @Override
        public LmsSetup getLmsSetup() {
            return this.lmsSetupDAO.byModelId(this.lmsSetupId).getOrThrow();
        }

        @Override
        public ClientCredentials getLmsClientCredentials() {
            return this.lmsSetupDAO.getLmsAPIAccessCredentials(this.lmsSetupId).getOrThrow();
        }

        @Override
        public ProxyData getProxyData() {
            return this.lmsSetupDAO.getLmsAPIAccessProxyData(this.lmsSetupId).getOr(null);
        }
    }

    /** Used to test LMS connection data that are not yet persistently stored */
    private static final class AdHocAPITemplateDataSupplier implements APITemplateDataSupplier {

        private final LmsSetup lmsSetup;
        private final ClientCredentialService clientCredentialService;

        public AdHocAPITemplateDataSupplier(
                final LmsSetup lmsSetup,
                final ClientCredentialService clientCredentialService) {
            this.lmsSetup = lmsSetup;
            this.clientCredentialService = clientCredentialService;
        }

        @Override
        public LmsSetup getLmsSetup() {
            return this.lmsSetup;
        }

        @Override
        public ClientCredentials getLmsClientCredentials() {
            return this.clientCredentialService.encryptClientCredentials(
                    this.lmsSetup.getLmsAuthName(),
                    this.lmsSetup.getLmsAuthSecret())
                    .getOrThrow();
        }

        @Override
        public ProxyData getProxyData() {
            return (StringUtils.isNoneBlank(this.lmsSetup.proxyHost))
                    ? new ProxyData(
                            this.lmsSetup.proxyHost,
                            this.lmsSetup.proxyPort,
                            this.clientCredentialService.encryptClientCredentials(
                                    this.lmsSetup.proxyAuthUsername,
                                    this.lmsSetup.proxyAuthSecret)
                                    .getOrThrow())
                    : null;
        }
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
