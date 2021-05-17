/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** This implements an overall short time cache for QuizData objects for all implementing
 * instances. It uses EH-Cache with a short time to live about 1 - 2 minutes.
 * </p>
 * The QuizData are stored with a key composed from the id of the key **/
public abstract class AbstractCachedCourseAccess extends AbstractCourseAccess {

    public static final String CACHE_NAME_QUIZ_DATA = "QUIZ_DATA_CACHE";

    private final Cache cache;

    protected AbstractCachedCourseAccess(
            final AsyncService asyncService,
            final Environment environment,
            final CacheManager cacheManager) {

        super(asyncService, environment);
        this.cache = cacheManager.getCache(CACHE_NAME_QUIZ_DATA);
    }

    public void clearCache() {
        this.cache.clear();
    }

    protected QuizData getFromCache(final String id) {
        return this.cache.get(createCacheKey(id), QuizData.class);
    }

    protected void putToCache(final QuizData quizData) {
        this.cache.put(createCacheKey(quizData.id), quizData);
    }

    protected void putToCache(final Collection<QuizData> quizData) {
        quizData.stream().forEach(q -> this.cache.put(createCacheKey(q.id), q));
    }

    protected void evict(final String id) {
        this.cache.evict(createCacheKey(id));
    }

    @Override
    public Result<Collection<Result<QuizData>>> getQuizzesFromCache(final Set<String> ids) {
        return Result.of(ids.stream().map(this::getQuizFromCache).collect(Collectors.toList()));
    }

    protected abstract Long getLmsSetupId();

    private final String createCacheKey(final String id) {
        return id + Constants.UNDERLINE + getLmsSetupId();
    }

}
