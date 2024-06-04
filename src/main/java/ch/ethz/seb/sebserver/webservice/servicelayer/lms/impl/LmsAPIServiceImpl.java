/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.webservice.servicelayer.lms.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult.ErrorType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;

@Lazy
@Service
@WebServiceProfile
public class LmsAPIServiceImpl implements LmsAPIService {

    private static final Logger log = LoggerFactory.getLogger(LmsAPIServiceImpl.class);

    private final WebserviceInfo webserviceInfo;
    private final LmsSetupDAO lmsSetupDAO;
    private final ClientCredentialService clientCredentialService;
    private final QuizLookupService quizLookupService;
    private final EnumMap<LmsType, LmsAPITemplateFactory> templateFactories;

    private final Map<CacheKey, LmsAPITemplate> cache = new ConcurrentHashMap<>();

    public LmsAPIServiceImpl(
            final WebserviceInfo webserviceInfo,
            final LmsSetupDAO lmsSetupDAO,
            final ClientCredentialService clientCredentialService,
            final QuizLookupService quizLookupService,
            final Collection<LmsAPITemplateFactory> lmsAPITemplateFactories) {

        this.webserviceInfo = webserviceInfo;
        this.lmsSetupDAO = lmsSetupDAO;
        this.clientCredentialService = clientCredentialService;
        this.quizLookupService = quizLookupService;

        final Map<LmsType, LmsAPITemplateFactory> factories = lmsAPITemplateFactories
                .stream()
                .collect(Collectors.toMap(
                        LmsAPITemplateFactory::lmsType,
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

        final LmsAPITemplate removedTemplate = this.cache.remove(
                new CacheKey(lmsSetup.getModelId(), 0));

        if (removedTemplate != null) {
            removedTemplate.clearCourseCache();
        }
        this.quizLookupService.clear(lmsSetup.institutionId);
    }

    @Override
    public void cleanup() {
        this.cache.values().forEach(LmsAPITemplate::dispose);
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

        return this.quizLookupService.requestQuizDataPage(
                pageNumber,
                pageSize,
                sort,
                filterMap,
                this::getLmsAPITemplate);
    }

    @Override
    public Result<LmsAPITemplate> getLmsAPITemplate(final String lmsSetupId) {
        return Result.tryCatch(() -> {
            synchronized (this) {
                final LmsAPITemplate lmsAPITemplate = getFromCache(lmsSetupId);
                if (lmsAPITemplate == null) {
                    return createLmsSetupTemplate(lmsSetupId)
                            .onError(error -> log.error("Failed to create LMSSetup: ", error))
                            .onSuccess(t -> this.cache.put(new CacheKey(lmsSetupId, System.currentTimeMillis()), t))
                            .getOrThrow();
                }
                return lmsAPITemplate;
            }
        });
    }

    @Override
    public LmsSetupTestResult test(final LmsAPITemplate template) {
        final LmsSetupTestResult testCourseAccessAPI = template.testCourseAccessAPI();
        if (!testCourseAccessAPI.isOk()) {
            this.cache.remove(new CacheKey(template.lmsSetup().getModelId(), 0));
            return testCourseAccessAPI;
        }

        if (template.lmsSetup().getLmsType().features.contains(LmsSetup.Features.SEB_RESTRICTION)) {
            final LmsSetupTestResult lmsSetupTestResult = template.testCourseRestrictionAPI();
            if (!lmsSetupTestResult.isOk()) {
                this.cache.remove(new CacheKey(template.lmsSetup().getModelId(), 0));
                return lmsSetupTestResult;
            }
        }

        if (template.lmsSetup().getLmsType().features.contains(LmsSetup.Features.LMS_FULL_INTEGRATION)) {
            final Long lmsSetupId = template.lmsSetup().id;
            final LmsSetupTestResult lmsSetupTestResult = template.testFullIntegrationAPI();
            if (!lmsSetupTestResult.isOk()) {
                this.cache.remove(new CacheKey(template.lmsSetup().getModelId(), 0));
                this.lmsSetupDAO
                        .setIntegrationActive(lmsSetupId, false)
                        .onError(er -> log.error("Failed to mark LMS integration inactive", er));
                return lmsSetupTestResult;
            } else {
// TODO
//                final Result<FullLmsIntegrationService.IntegrationData> integrationDataResult = fullLmsIntegrationService
//                        .applyFullLmsIntegration(template.lmsSetup().id);
//
//                if (integrationDataResult.hasError()) {
//                    return LmsSetupTestResult.ofFullIntegrationAPIError(
//                            template.lmsSetup().lmsType,
//                            "Failed to apply full LMS integration");
//                }
            }
        }

        return LmsSetupTestResult.ofOkay(template.lmsSetup().getLmsType());
    }

    @Override
    public LmsSetupTestResult testAdHoc(final LmsSetup lmsSetup) {
        final AdHocAPITemplateDataSupplier apiTemplateDataSupplier = new AdHocAPITemplateDataSupplier(
                lmsSetup,
                this.clientCredentialService);

        final Result<LmsAPITemplate> createLmsSetupTemplate = createLmsSetupTemplate(apiTemplateDataSupplier);
        if (createLmsSetupTemplate.hasError()) {
            return new LmsSetupTestResult(
                    lmsSetup.lmsType,
                    new LmsSetupTestResult.Error(ErrorType.TEMPLATE_CREATION,
                            createLmsSetupTemplate.getError().getMessage()));

        }
        final LmsAPITemplate lmsSetupTemplate = createLmsSetupTemplate.get();

        final LmsSetupTestResult testCourseAccessAPI = lmsSetupTemplate.testCourseAccessAPI();
        if (!testCourseAccessAPI.isOk()) {
            return testCourseAccessAPI;
        }

        final LmsType lmsType = lmsSetupTemplate.lmsSetup().getLmsType();
        if (lmsType.features.contains(LmsSetup.Features.SEB_RESTRICTION)) {
            final LmsSetupTestResult lmsSetupTestResult = lmsSetupTemplate.testCourseRestrictionAPI();
            if (!lmsSetupTestResult.isOk()) {
                return lmsSetupTestResult;
            }
        }

        if (lmsType.features.contains(LmsSetup.Features.LMS_FULL_INTEGRATION)) {
            final LmsSetupTestResult lmsSetupTestResult = lmsSetupTemplate.testFullIntegrationAPI();
            if (!lmsSetupTestResult.isOk()) {
                return lmsSetupTestResult;
            }
        }

        return LmsSetupTestResult.ofOkay(lmsSetupTemplate.lmsSetup().getLmsType());
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
                this.quizLookupService.clear(lmsSetup.institutionId);
                return null;
            }
        }

        return lmsAPITemplate;
    }

    private Result<LmsAPITemplate> createLmsSetupTemplate(final String lmsSetupId) {

        if (log.isDebugEnabled()) {
            log.debug("Create new LmsAPITemplate for id: {}", lmsSetupId);
        }

        return createLmsSetupTemplate(new PersistentAPITemplateDataSupplier(
                lmsSetupId,
                this.lmsSetupDAO));
    }

    private Result<LmsAPITemplate> createLmsSetupTemplate(final APITemplateDataSupplier apiTemplateDataSupplier) {

        final LmsType lmsType = apiTemplateDataSupplier.getLmsSetup().lmsType;

        if (!this.templateFactories.containsKey(lmsType)) {
            throw new UnsupportedOperationException("No support for LMS Type: " + lmsType);
        }

        final LmsAPITemplateFactory lmsAPITemplateFactory = this.templateFactories
                .get(lmsType);

        return lmsAPITemplateFactory
                .create(apiTemplateDataSupplier);
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
                    this.lmsSetup.getLmsAuthSecret(),
                    this.lmsSetup.lmsRestApiToken)
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
                                    this.lmsSetup.proxyAuthSecret,
                                    this.lmsSetup.lmsRestApiToken)
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
