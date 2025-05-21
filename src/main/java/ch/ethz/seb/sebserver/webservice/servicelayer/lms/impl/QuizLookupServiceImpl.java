/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.api.API;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncRunner;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.CourseAccessAPI.AsyncQuizFetchBuffer;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.QuizLookupService;

@Lazy
@Service
@WebServiceProfile
public class QuizLookupServiceImpl implements QuizLookupService {

    private static final Logger log = LoggerFactory.getLogger(QuizLookupServiceImpl.class);

    private final Map<String, AsyncLookup> lookups = new ConcurrentHashMap<>();

    private final UserService userService;
    private final LmsSetupDAO lmsSetupDAO;
    private final AsyncRunner asyncRunner;
    private final long fetchedDataValiditySeconds;

    public QuizLookupServiceImpl(
            final UserService userService,
            final LmsSetupDAO lmsSetupDAO,
            final AsyncService asyncService,
            final Environment environment,
            @Value("${sebserver.webservice.lms.datafetch.validity.seconds:600}") final long fetchedDataValiditySeconds) {

        this.userService = userService;
        this.lmsSetupDAO = lmsSetupDAO;
        this.asyncRunner = asyncService.getAsyncRunner();
        this.fetchedDataValiditySeconds = fetchedDataValiditySeconds;
    }

    @Override
    public void clear(final Long institutionId) {
        final Set<String> toRemove = this.lookups.values()
                .stream()
                .filter(al -> al.institutionId == institutionId.longValue() || !al.isUpToDate())
                .map(al -> al.userId)
                .collect(Collectors.toSet());

        if (log.isDebugEnabled()) {
            log.debug("Remove invalid async lookups: {}", toRemove);
        }

        toRemove.stream()
                .forEach(this::removeFromCache);
    }

    @Override
    public void clear() {
        final Set<String> toRemove = this.lookups.values()
                .stream()
                .filter(al -> !al.isUpToDate())
                .map(al -> al.userId)
                .collect(Collectors.toSet());

        if (log.isDebugEnabled()) {
            log.debug("Remove invalid async lookups: {}", toRemove);
        }

        toRemove.stream()
                .forEach(this::removeFromCache);
    }

    @Override
    public boolean isLookupRunning() {
        final String userId = this.userService.getCurrentUser().uuid();
        if (!this.lookups.containsKey(userId)) {
            return false;
        }
        return this.lookups.get(userId).isRunning();
    }

    @Override
    public Result<Page<QuizData>> requestQuizDataPage(
            final int pageNumber,
            final int pageSize,
            final String sort,
            final FilterMap filterMap,
            final Function<String, Result<LmsAPITemplate>> lmsAPITemplateSupplier) {

        return getAllQuizzesFromLMSSetups(filterMap, lmsAPITemplateSupplier)
                .map(QuizLookupService.quizzesSortFunction(sort))
                .map(QuizLookupService.quizzesToPageFunction(
                        sort,
                        pageNumber,
                        pageSize));
    }

    private Result<LookupResult> getAllQuizzesFromLMSSetups(
            final FilterMap filterMap,
            final Function<String, Result<LmsAPITemplate>> lmsAPITemplateSupplier) {

        return Result.tryCatch(() -> {

            final String userId = this.userService.getCurrentUser().uuid();

            if (log.isDebugEnabled()) {
                log.debug("Get all quizzes for user: {}", userId);
            }

            final AsyncLookup asyncLookup = getAsyncLookup(userId, filterMap, lmsAPITemplateSupplier);
            if (asyncLookup != null) {
                return asyncLookup.getAvailable();
            }

            return emptyLookupResult();
        });
    }

    private AsyncLookup getAsyncLookup(
            final String userId,
            final FilterMap filterMap,
            final Function<String, Result<LmsAPITemplate>> lmsAPITemplateSupplier) {

        if (!this.lookups.containsKey(userId)) {
            this.createNewAsyncLookup(userId, filterMap, lmsAPITemplateSupplier);
        }

        final AsyncLookup asyncLookup = this.lookups.get(userId);
        if (asyncLookup == null) {
            return null;
        }

        if (!asyncLookup.isValid(filterMap)) {
            final AsyncLookup removed = this.lookups.remove(userId);
            if (removed != null) {
                removed.cancel();
            }
            this.createNewAsyncLookup(userId, filterMap, lmsAPITemplateSupplier);
        }

        return this.lookups.get(userId);
    }

    private void createNewAsyncLookup(
            final String userId,
            final FilterMap filterMap,
            final Function<String, Result<LmsAPITemplate>> lmsAPITemplateSupplier) {

        try {
            final Long institutionId = filterMap.getInstitutionId();
            Long userInstitutionId = institutionId;
            if (userInstitutionId == null) {
                userInstitutionId = this.userService.getCurrentUser().getUserInfo().institutionId;
            }
            final Long lmsSetupId = filterMap.getLmsSetupId();
            final Result<List<AsyncQuizFetchBuffer>> tasks = this.lmsSetupDAO
                    .all(institutionId, true)
                    .map(lmsSetups -> lmsSetups
                            .stream()
                            .filter(lmsSetup -> lmsSetupId == null || lmsSetupId.longValue() == lmsSetup.id.longValue())
                            .map(lmsSetup -> lmsAPITemplateSupplier.apply(lmsSetup.getModelId()))
                            .flatMap(Result::onErrorLogAndSkip)
                            .map(lmsAPITemplate -> spawnTask(filterMap, lmsAPITemplate))
                            .collect(Collectors.toList()));

            if (tasks.hasError()) {
                log.error("Failed to spawn LMS quizzes lookup tasks: ", tasks.getError());
                return;
            }

            final List<AsyncQuizFetchBuffer> buffers = tasks.getOr(Collections.emptyList());
            if (buffers.isEmpty()) {
                return;
            }

            final LookupFilterCriteria criteria = new LookupFilterCriteria(filterMap);
            final AsyncLookup asyncLookup = new AsyncLookup(
                    userInstitutionId,
                    userId,
                    criteria,
                    buffers,
                    this.fetchedDataValiditySeconds);

            if (log.isDebugEnabled()) {
                log.debug("Create new AsyncLookup: user={} criteria={}", userId, criteria);
            }

            this.lookups.put(asyncLookup.userId, asyncLookup);

            // Note: wait for about max five second to finish up
            int tryWait = 0;
            while (asyncLookup.isRunning() && tryWait < 10) {
                tryWait++;
                Utils.sleep(500);
            }
        } catch (final Exception e) {
            log.error(
                    "Unexpected error while create new AsyncLookup for user: {}, filterMap: {}, error:",
                    userId,
                    filterMap,
                    e);
        }
    }

    private AsyncQuizFetchBuffer spawnTask(final FilterMap filterMap, final LmsAPITemplate lmsAPITemplate) {
        final AsyncQuizFetchBuffer asyncQuizFetchBuffer = new AsyncQuizFetchBuffer();
        this.asyncRunner.runAsync(() -> lmsAPITemplate.fetchQuizzes(filterMap, asyncQuizFetchBuffer));
        return asyncQuizFetchBuffer;
    }

    private void removeFromCache(final String userId) {
        final AsyncLookup removed = this.lookups.remove(userId);
        if (removed != null) {
            removed.cancel();
        }
    }

    private static final class LookupFilterCriteria {
        public final Long institutionId;
        public final Long lmsId;
        public final String name;
        public final DateTime startTime;
        public final Long startTimeMillis;

        public LookupFilterCriteria(final FilterMap filterMap) {

            this.institutionId = filterMap.getInstitutionId();
            this.lmsId = filterMap.getLmsSetupId();
            this.name = filterMap.getQuizName();
            this.startTime = filterMap.getQuizFromTime();
            this.startTimeMillis = filterMap.getQuizFromTimeMillis();
        }

        boolean equals(final FilterMap filterMap) {
            return Utils.isEqualsWithEmptyCheck(filterMap.getQuizName(), this.name) &&
                    Objects.equals(filterMap.getQuizFromTime(), this.startTime) &&
                    Objects.equals(filterMap.getQuizFromTimeMillis(), this.startTimeMillis) &&
                    Objects.equals(filterMap.getLmsSetupId(), this.lmsId) &&
                    Objects.equals(filterMap.getInstitutionId(), this.institutionId);
        }

        @Override
        public String toString() {
            return "LookupFilterCriteria [institutionId=" +
                    this.institutionId +
                    ", lmsId=" +
                    this.lmsId +
                    ", name=" +
                    this.name +
                    ", startTime=" +
                    this.startTime +
                    ", startTimeMillis=" +
                    this.startTimeMillis +
                    "]";
        }
    }

    private static final class AsyncLookup {
        final long institutionId;
        final String userId;
        final LookupFilterCriteria lookupFilterCriteria;
        final Collection<AsyncQuizFetchBuffer> asyncBuffers;
        final long timeCreated;
        long timeCompleted = Long.MAX_VALUE;
        private final long fetchedDataValiditySeconds;

        public AsyncLookup(
                final long institutionId,
                final String userId,
                final LookupFilterCriteria lookupFilterCriteria,
                final Collection<AsyncQuizFetchBuffer> asyncBuffers,
                final long fetchedDataValiditySeconds) {

            this.institutionId = institutionId;
            this.userId = userId;
            this.lookupFilterCriteria = lookupFilterCriteria;
            this.asyncBuffers = asyncBuffers;
            this.timeCreated = Utils.getMillisecondsNow();
            this.fetchedDataValiditySeconds = fetchedDataValiditySeconds;
        }

        LookupResult getAvailable() {
            boolean running = false;
            final List<QuizData> result = new ArrayList<>();
            for (final AsyncQuizFetchBuffer buffer : this.asyncBuffers) {
                running = running || !buffer.finished;
                result.addAll(buffer.buffer);
            }
            if (!running) {
                this.timeCompleted = Utils.getMillisecondsNow();
            }
            return new LookupResult(result, !running);
        }

        boolean isUpToDate() {
            final long now = Utils.getMillisecondsNow();
            if (now - this.timeCreated > this.fetchedDataValiditySeconds * Constants.SECOND_IN_MILLIS) {
                return false;
            }
            return true;
        }

        boolean isValid(final FilterMap filterMap) {
            if (filterMap.getBoolean(API.LMS_LOOKUP_NEW_SEARCH)) {
                return false;
            }
            
            if (!isUpToDate()) {
                return false;
            }

            return this.lookupFilterCriteria.equals(filterMap);
        }

        boolean isRunning() {
            if (this.timeCompleted < Long.MAX_VALUE) {
                return false;
            }
            final boolean running = this.asyncBuffers
                    .stream()
                    .anyMatch(b -> !b.finished);
            if (!running) {
                this.timeCompleted = Utils.getMillisecondsNow();
            }
            return running;
        }

        void cancel() {
            this.asyncBuffers.stream().forEach(AsyncQuizFetchBuffer::cancel);
        }
    }

}
