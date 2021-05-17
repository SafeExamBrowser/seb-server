/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.Features;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.AbstractCachedCourseAccess;

public class OlatLmsAPITemplate extends AbstractCachedCourseAccess implements LmsAPITemplate {

    // TODO add needed dependencies here
    private final APITemplateDataSupplier apiTemplateDataSupplier;
    private final Long lmsSetupId;

    protected OlatLmsAPITemplate(
            // TODO if you need more dependencies inject them here and set the reference
            final APITemplateDataSupplier apiTemplateDataSupplier,
            final AsyncService asyncService,
            final Environment environment,
            final CacheManager cacheManager) {

        super(asyncService, environment, cacheManager);

        this.apiTemplateDataSupplier = apiTemplateDataSupplier;
        this.lmsSetupId = apiTemplateDataSupplier.getLmsSetup().id;
    }

    @Override
    public LmsType getType() {
        return LmsType.OPEN_OLAT;
    }

    @Override
    public LmsSetup lmsSetup() {
        return this.apiTemplateDataSupplier.getLmsSetup();
    }

    @Override
    protected Long getLmsSetupId() {
        return this.lmsSetupId;
    }

    @Override
    public LmsSetupTestResult testCourseAccessAPI() {
        final LmsSetupTestResult testLmsSetupSettings = testLmsSetupSettings();
        if (testLmsSetupSettings.hasAnyError()) {
            return testLmsSetupSettings;
        }

        // TODO check if the course API of the remote LMS is available
        // if not, create corresponding LmsSetupTestResult error

        return LmsSetupTestResult.ofOkay(LmsType.OPEN_OLAT);
    }

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        final LmsSetupTestResult testLmsSetupSettings = testLmsSetupSettings();
        if (testLmsSetupSettings.hasAnyError()) {
            return testLmsSetupSettings;
        }

        if (LmsType.OPEN_OLAT.features.contains(Features.SEB_RESTRICTION)) {

            // TODO check if the course API of the remote LMS is available
            // if not, create corresponding LmsSetupTestResult error

        }

        return LmsSetupTestResult.ofOkay(LmsType.OPEN_OLAT);
    }

    private LmsSetupTestResult testLmsSetupSettings() {

        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final ClientCredentials lmsClientCredentials = this.apiTemplateDataSupplier.getLmsClientCredentials();
        final List<APIMessage> missingAttrs = new ArrayList<>();

        // Check given LMS URL
        if (StringUtils.isBlank(lmsSetup.lmsApiUrl)) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_URL,
                    "lmsSetup:lmsUrl:notNull"));
        } else {
            // Try to connect to the URL
            if (!Utils.pingHost(lmsSetup.lmsApiUrl)) {
                missingAttrs.add(APIMessage.fieldValidationError(
                        LMS_SETUP.ATTR_LMS_URL,
                        "lmsSetup:lmsUrl:url.invalid"));
            }
        }

        // Client id is mandatory
        if (!lmsClientCredentials.hasClientId()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTNAME,
                    "lmsSetup:lmsClientname:notNull"));
        }

        // Client secret is mandatory
        if (!lmsClientCredentials.hasSecret()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTSECRET,
                    "lmsSetup:lmsClientsecret:notNull"));
        }

        if (!missingAttrs.isEmpty()) {
            return LmsSetupTestResult.ofMissingAttributes(LmsType.OPEN_OLAT, missingAttrs);
        }

        return LmsSetupTestResult.ofOkay(LmsType.OPEN_OLAT);
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        return this
                .protectedQuizzesRequest(filterMap)
                .map(quizzes -> quizzes.stream()
                        .filter(LmsAPIService.quizFilterPredicate(filterMap))
                        .collect(Collectors.toList()));
    }

    @Override
    public Result<Collection<QuizData>> getQuizzes(final Set<String> ids) {
        return Result.tryCatch(() -> {
            final HashSet<String> leftIds = new HashSet<>(ids);
            final Collection<QuizData> result = new ArrayList<>();
            ids.stream()
                    .map(id -> super.getFromCache(id))
                    .forEach(q -> {
                        if (q != null) {
                            leftIds.remove(q.id);
                            result.add(q);
                        }
                    });

            if (!leftIds.isEmpty()) {
                result.addAll(super.protectedQuizzesRequest(leftIds).getOrThrow());
            }

            return result;
        });
    }

    @Override
    public Result<QuizData> getQuiz(final String id) {
        final QuizData fromCache = super.getFromCache(id);
        if (fromCache != null) {
            return Result.of(fromCache);
        }

        return super.protectedQuizRequest(id);
    }

    @Override
    protected Supplier<List<QuizData>> allQuizzesSupplier(final FilterMap filterMap) {

        final String quizName = filterMap.getString(QuizData.FILTER_ATTR_QUIZ_NAME);
        final DateTime quizFromTime = (filterMap != null) ? filterMap.getQuizFromTime() : null;

        // TODO get all course / quiz data from remote LMS that matches the filter criteria.
        //      put loaded QuizData to the cache: super.putToCache(quizDataCollection);
        //      before returning it.

        return () -> {
            throw new RuntimeException("TODO");
        };
    }

    @Override
    protected Supplier<Collection<QuizData>> quizzesSupplier(final Set<String> ids) {

        // TODO get all quiz / course data for specified identifiers from remote LMS
        //      and put it to the cache: super.putToCache(quizDataCollection);
        //      before returning it.

        return () -> {
            throw new RuntimeException("TODO");
        };
    }

    @Override
    protected Supplier<QuizData> quizSupplier(final String id) {

        // TODO get the specified quiz / course data for specified identifier from remote LMS
        //      and put it to the cache: super.putToCache(quizDataCollection);
        //      before returning it.

        return () -> {
            throw new RuntimeException("TODO");
        };
    }

    @Override
    protected Supplier<ExamineeAccountDetails> accountDetailsSupplier(final String examineeSessionId) {

        // TODO get the examinee's account details by the given examineeSessionId from remote LMS.
        //      Currently only the name is needed to display on monitoring view.

        return () -> {
            throw new RuntimeException("TODO");
        };
    }

    @Override
    protected Supplier<Chapters> getCourseChaptersSupplier(final String courseId) {
        return () -> {
            throw new UnsupportedOperationException("not available yet");
        };
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {

        final String quizId = exam.externalId;

        // TODO get the SEB client restrictions that are currently set on the remote LMS for
        //      the given quiz / course derived from the given exam

        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(
            final String externalExamId,
            final SEBRestriction sebRestrictionData) {

        // TODO apply the given sebRestrictionData settings as current SEB client restriction setting
        //      to the remote LMS for the given quiz / course.
        //      Mainly SEBRestriction.configKeys and SEBRestriction.browserExamKeys

        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {

        final String quizId = exam.externalId;

        // TODO Release respectively delete all SEB client restrictions for the given
        //      course / quize on the remote LMS.

        return Result.ofRuntimeError("TODO");
    }

}
