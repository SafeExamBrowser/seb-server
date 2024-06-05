/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class LmsAPITemplateCacheServiceImpl implements LmsAPITemplateCacheService {

    private static final Logger log = LoggerFactory.getLogger(LmsAPITemplateCacheServiceImpl.class);
    private final WebserviceInfo webserviceInfo;

    private final LmsSetupDAO lmsSetupDAO;
    private final QuizLookupService quizLookupService;
    private final ClientCredentialService clientCredentialService;
    private final EnumMap<LmsSetup.LmsType, LmsAPITemplateFactory> templateFactories;

    private final Map<CacheKey, LmsAPITemplate> cache = new ConcurrentHashMap<>();

    public LmsAPITemplateCacheServiceImpl(
            final WebserviceInfo webserviceInfo,
            final LmsSetupDAO lmsSetupDAO,
            final QuizLookupService quizLookupService,
            final ClientCredentialService clientCredentialService,
            final Collection<LmsAPITemplateFactory> lmsAPITemplateFactories) {

        this.webserviceInfo = webserviceInfo;
        this.lmsSetupDAO = lmsSetupDAO;
        this.quizLookupService = quizLookupService;
        this.clientCredentialService = clientCredentialService;
        final Map<LmsSetup.LmsType, LmsAPITemplateFactory> factories = lmsAPITemplateFactories
                .stream()
                .collect(Collectors.toMap(
                        LmsAPITemplateFactory::lmsType,
                        Function.identity()));
        this.templateFactories = new EnumMap<>(factories);
    }

    @Override
    public void cleanup() {
        this.cache.values().forEach(LmsAPITemplate::dispose);
        this.cache.clear();
    }

    @Override
    public Result<LmsAPITemplate> getLmsAPITemplate(final Long lmsSetupId) {
        return getLmsAPITemplate(String.valueOf(lmsSetupId));
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
    public Result<LmsAPITemplate> getLmsAPITemplateForTesting(final String lmsSetupId) {
        return lmsSetupDAO.byModelId(lmsSetupId)
                .map(lmsSetup -> new AdHocAPITemplateDataSupplier(
                        lmsSetup,
                        this.clientCredentialService))
                .flatMap(this::createLmsSetupTemplate);
    }

    @Override
    public void clearCache(final String lmsSetupId) {
        final LmsAPITemplate removedTemplate = this.cache.remove(
                new CacheKey(lmsSetupId, 0));

        if (removedTemplate != null) {
            log.info("Removed LmsAPITemplate from cache: {}", removedTemplate);
            removedTemplate.clearCourseCache();
        }
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

        final LmsSetup.LmsType lmsType = apiTemplateDataSupplier.getLmsSetup().lmsType;

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
