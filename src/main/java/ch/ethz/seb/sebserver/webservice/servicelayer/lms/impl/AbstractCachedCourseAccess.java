/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;

/** This implements an overall short time cache for QuizData objects for all implementing
 * instances. It uses EH-Cache with a short time to live about 1 - 2 minutes.
 * </p>
 * The QuizData are stored with a key composed from the id of the key
 * </p>
 * The EH-Cache can be configured in file ehcache.xml **/
public abstract class AbstractCachedCourseAccess {

    private static final Logger log = LoggerFactory.getLogger(AbstractCachedCourseAccess.class);

    /** The cache name of the overall short time EH-Cache */
    public static final String CACHE_NAME_QUIZ_DATA = "QUIZ_DATA_CACHE";

    private final Cache cache;

    protected AbstractCachedCourseAccess(final CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CACHE_NAME_QUIZ_DATA);
    }

    /** Used to clear the entire cache */
    public void clearCourseCache() {
        final Object nativeCache = this.cache.getNativeCache();
        if (nativeCache instanceof javax.cache.Cache) {
            try {
                final String suffix = Constants.UNDERLINE.toString() + getLmsSetupId();
                final Set<String> keysToRemove = new HashSet<>();
                @SuppressWarnings({ "unchecked" })
                final javax.cache.Cache<String, QuizData> _cache =
                        (javax.cache.Cache<String, QuizData>) this.cache.getNativeCache();
                for (final javax.cache.Cache.Entry<String, QuizData> entry : _cache) {
                    if (entry.getKey().endsWith(suffix)) {
                        keysToRemove.add(entry.getKey());
                    }
                }

                if (!keysToRemove.isEmpty()) {
                    synchronized (this.cache) {
                        _cache.removeAll(keysToRemove);
                    }
                }
            } catch (final Exception e) {
                log.error("Failed to clear particular LMS Setup cache: ", e);
                this.cache.clear();
            }
        } else {
            this.cache.clear();
        }
    }

    /** Get the for the given quiz id QuizData from cache .
     *
     * @param id The quiz id - this is the raw quiz id not the cache key. The cache key is composed internally
     * @return the QuizData corresponding the given id or null if there is no such data in cache */
    protected QuizData getFromCache(final String id) {
        return this.cache.get(createCacheKey(id), QuizData.class);
    }

    /** Puts the given QuizData to the cache.
     * </p>
     * The cache key with lms suffix is composed internally
     *
     * @param quizData */
    protected void putToCache(final QuizData quizData) {
        if (quizData == null) {
            return;
        }

        final String createCacheKey = createCacheKey(quizData.id);

        if (log.isTraceEnabled()) {
            log.trace("Put to cache: {} : {}", createCacheKey, quizData);
        }

        this.cache.put(createCacheKey, quizData);
    }

    /** Put all QuizData to short time cache.
     *
     * @param quizData Collection of QuizData */
    protected void putToCache(final Collection<QuizData> quizData) {
        quizData.stream().forEach(q -> this.cache.put(createCacheKey(q.id), q));
    }

    protected void evict(final String id) {

        final String createCacheKey = createCacheKey(id);

        if (log.isTraceEnabled()) {
            log.trace("Evict from cache: {}", createCacheKey);
        }

        this.cache.evict(createCacheKey);
    }

    /** Get the LMS setup identifier that is wrapped within the implementing template.
     * This is used to create the cache Key.
     *
     * @return */
    protected abstract Long getLmsSetupId();

    private final String createCacheKey(final String id) {
        return id + Constants.UNDERLINE + getLmsSetupId();
    }

}
