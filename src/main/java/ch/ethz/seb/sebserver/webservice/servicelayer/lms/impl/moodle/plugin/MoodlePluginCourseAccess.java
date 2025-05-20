/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.BooleanUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.CourseAccessAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.AbstractCachedCourseAccess;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.CourseData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.CoursesPlugin;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.MoodleUserDetails;
import io.micrometer.core.instrument.util.StringUtils;

public class MoodlePluginCourseAccess extends AbstractCachedCourseAccess implements CourseAccessAPI {

    private static final Logger log = LoggerFactory.getLogger(MoodlePluginCourseAccess.class);

    public static final String MOODLE_QUIZ_START_URL_PATH = "mod/quiz/view.php?id=";
    public static final String COURSES_API_FUNCTION_NAME = "quizaccess_sebserver_get_exams";
    public static final String USERS_API_FUNCTION_NAME = "core_user_get_users_by_field";

    public static final String ATTR_FIELD = "field";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_VALUE_ARRAY = "values[]";
    public static final String ATTR_ID = "id";
    public static final String ATTR_SHORTNAME = "shortname";

    public static final String PARAM_COURSE_ID_ARRAY = "courseid[]";
    public static final String PARAM_SQL_CONDITIONS = "conditions";
    public static final String PARAM_PAGE_START = "startneedle";
    public static final String PARAM_PAGE_SIZE = "perpage";
    public static final String PARAM_FILTER_COURSES = "filtercourses";

    public static final String SQL_QUIZ_NAME = "m.name";
    public static final String SQL_COURSE_NAME = "shortname";

    public static final String SQL_CONDITION_TEMPLATE =
            "(startdate is null OR startdate = 0 OR startdate >= %s) AND (enddate is null or enddate = 0 OR enddate >= %s)";

    private final JSONMapper jsonMapper;
    private final MoodleRestTemplateFactory restTemplateFactory;
    private final CircuitBreaker<String> protectedMoodlePageCall;
    private final boolean prependShortCourseName;
    private final int pageSize;
    private final int maxSize;
    private final int cutoffTimeOffset;
    private final boolean applyNameCriteria;

    public MoodlePluginCourseAccess(
            final JSONMapper jsonMapper,
            final AsyncService asyncService,
            final MoodleRestTemplateFactory restTemplateFactory,
            final CacheManager cacheManager,
            final Environment environment,
            final boolean applyNameCriteria) {

        super(cacheManager);

        this.jsonMapper = jsonMapper;
        this.restTemplateFactory = restTemplateFactory;
        this.applyNameCriteria = applyNameCriteria;

        this.prependShortCourseName = BooleanUtils.toBoolean(environment.getProperty(
                "sebserver.webservice.lms.moodle.prependShortCourseName",
                Constants.TRUE_STRING));

        this.protectedMoodlePageCall = asyncService.createCircuitBreaker(
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.moodleRestCall.attempts",
                        Integer.class,
                        2),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.moodleRestCall.blockingTime",
                        Long.class,
                        Constants.SECOND_IN_MILLIS * 30),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.moodleRestCall.timeToRecover",
                        Long.class,
                        Constants.MINUTE_IN_MILLIS));
        this.maxSize =
                environment.getProperty("sebserver.webservice.cache.moodle.course.maxSize", Integer.class, 10000);
        this.pageSize =
                environment.getProperty("sebserver.webservice.cache.moodle.course.pageSize", Integer.class, 500);

        this.cutoffTimeOffset = environment.getProperty(
                "sebserver.webservice.lms.moodle.fetch.cutoffdate.yearsBeforeNow",
                Integer.class, 3);
    }

    @Override
    protected Long getLmsSetupId() {
        return this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup().id;
    }

    @Override
    public String getCourseIdFromExam(final Exam exam) {
        return MoodleUtils.getCourseId(exam.externalId);
    }

    @Override
    public String getQuizIdFromExam(final Exam exam) {
        return MoodleUtils.getQuizId(exam.externalId);
    }

    @Override
    public LmsSetupTestResult testCourseAccessAPI() {
        final LmsSetupTestResult attributesCheck = this.restTemplateFactory.test();
        if (!attributesCheck.isOk()) {
            return attributesCheck;
        }

        final Result<MoodleAPIRestTemplate> restTemplateRequest = getRestTemplate();
        if (restTemplateRequest.hasError()) {
            final String message = "Failed to gain access token from Moodle Rest API:\n tried token endpoints: " +
                    this.restTemplateFactory.getKnownTokenAccessPaths();
            log.error(message + " cause: {}", restTemplateRequest.getError().getMessage());
            return LmsSetupTestResult.ofTokenRequestError(LmsType.MOODLE_PLUGIN, message);
        }

        final MoodleAPIRestTemplate restTemplate = restTemplateRequest.get();

        try {

            restTemplate.testAPIConnection(
                    COURSES_API_FUNCTION_NAME,
                    USERS_API_FUNCTION_NAME);

        } catch (final RuntimeException e) {
            return LmsSetupTestResult.ofQuizAccessAPIError(LmsType.MOODLE_PLUGIN, e.getMessage());
        }

        return LmsSetupTestResult.ofOkay(LmsType.MOODLE_PLUGIN);
    }

    @Override
    public void fetchQuizzes(final FilterMap filterMap, final AsyncQuizFetchBuffer asyncQuizFetchBuffer) {
        try {

            int page = 0;
            int failedAttempts = 0;
            DateTime quizFromTime = filterMap.getQuizFromTime();
            if (quizFromTime == null) {
                quizFromTime = DateTime.now(DateTimeZone.UTC).minusYears(this.cutoffTimeOffset);
            }
            final Predicate<QuizData> quizFilter = LmsAPIService.quizFilterPredicate(filterMap);
            final String quizName = filterMap.getQuizName();

            while (!asyncQuizFetchBuffer.finished && !asyncQuizFetchBuffer.canceled) {
                try {
                    fetchQuizzesPage(page, quizFromTime, quizName, asyncQuizFetchBuffer, quizFilter);
                    page++;
                } catch (final Exception e) {
                    log.error("Unexpected error while trying to fetch moodle quiz page: {}", page, e);
                    failedAttempts++;
                    if (failedAttempts > 3) {
                        asyncQuizFetchBuffer.finish(e);
                    }
                }
            }

        } catch (final Exception e) {
            asyncQuizFetchBuffer.finish(e);
        }
    }

    @Override
    public Result<Collection<QuizData>> getQuizzes(final Set<String> ids) {
        return Result.tryCatch(() -> {

            final Set<String> missingIds = new HashSet<>(ids);
            final Collection<QuizData> result = new ArrayList<>();
            final Set<String> fromCache = ids.stream()
                    .map(super::getFromCache)
                    .filter(Objects::nonNull)
                    .map(qd -> {
                        result.add(qd);
                        return qd.id;
                    }).collect(Collectors.toSet());
            missingIds.removeAll(fromCache);

            if (!missingIds.isEmpty()) {

                result.addAll(getRestTemplate()
                        .map(template -> getQuizzesForIds(template, ids))
                        .map(super::putToCache)
                        .onError(error -> log.error("Failed to get courses for: {}", ids, error))
                        .getOrElse(Collections::emptyList));
            }

            return result;
        });
    }

    @Override
    public Result<QuizData> getQuiz(final String id) {
        return Result.tryCatch(() -> {

            final QuizData fromCache = super.getFromCache(id);
            if (fromCache != null) {
                return fromCache;
            }

            final Set<String> ids = Stream.of(id).collect(Collectors.toSet());
            final Iterator<QuizData> iterator = getRestTemplate()
                    .map(template -> getQuizzesForIds(template, ids))
                    .map(super::putToCache)
                    .getOr(Collections.emptyList())
                    .iterator();

            if (!iterator.hasNext()) {
                throw new RuntimeException("Moodle Quiz for id " + id + " not found");
            }

            return iterator.next();
        });
    }

    @Override
    public Result<QuizData> tryRecoverQuizForExam(final Exam exam) {
        return Result.tryCatch(() -> {

            final LmsSetup lmsSetup = this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup();
            final MoodleAPIRestTemplate restTemplate = getRestTemplate().getOrThrow();
            final String urlPrefix = (lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                    ? lmsSetup.lmsApiUrl + MOODLE_QUIZ_START_URL_PATH
                    : lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MOODLE_QUIZ_START_URL_PATH;

            // get the course name identifier for recovering
            final String shortname = MoodleUtils.getShortname(exam.externalId);

            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
//            attributes.add(ATTR_FIELD, ATTR_SHORTNAME);
//            attributes.add(ATTR_VALUE, shortname);
            final String n = Utils.toSQLWildcard(shortname);
            final String condition = SQL_QUIZ_NAME + " LIKE '" + n + "' OR " + SQL_COURSE_NAME + " LIKE '" + n + "'";
            log.info("**************** moodle request condition: {}", condition);

//            final long start = Utils.toUnixTimeInSeconds(exam.getStartTime().minusDays(2));
//            final long end = Utils.toUnixTimeInSeconds(exam.getStartTime().plusDays(2));
//            //final String condition = "startdate >= " + start + " AND startdate <= " + end;
//            final String condition = "(startdate is null OR startdate = 0 OR startdate >= " + start + ") AND (startdate is null or startdate = 0 OR startdate >= " + end + ")";
            
            log.info("**************** moodle request condition: {}", condition);

            attributes.add(PARAM_COURSE_ID_ARRAY, "0");
            attributes.add(PARAM_FILTER_COURSES, "1");
            attributes.add(PARAM_SQL_CONDITIONS, condition);
            attributes.add(PARAM_PAGE_START, "0");
            attributes.add(PARAM_PAGE_SIZE, "10");
            final String courseJSON = this.protectedMoodlePageCall
                    .protectedRun(() -> restTemplate.callMoodleAPIFunction(
                            COURSES_API_FUNCTION_NAME,
                            attributes))
                    .getOrThrow();
            
            log.info("**************** moodle response: {}", courseJSON);

            MoodleUtils.checkJSONFormat(courseJSON);

            return this.jsonMapper.readValue(
                    courseJSON,
                            CoursesPlugin.class).results
                            .stream()
                            .flatMap(c -> MoodleUtils.quizDataOf(
                                    lmsSetup,
                                    c,
                                    urlPrefix,
                                    this.prependShortCourseName).stream())
                            .filter(q -> exam.name.contains(q.name))
                            .findFirst()
                            .get();
        });
    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeSessionId) {
        return Result.tryCatch(() -> {

            final MoodleAPIRestTemplate template = getRestTemplate()
                    .getOrThrow();

            final MultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();
            queryAttributes.add(ATTR_FIELD, ATTR_ID);
            queryAttributes.add(ATTR_VALUE_ARRAY, examineeSessionId);

            final String userDetailsJSON = template.callMoodleAPIFunction(
                    USERS_API_FUNCTION_NAME,
                    queryAttributes);

            if (MoodleUtils.checkAccessDeniedError(userDetailsJSON)) {
                final LmsSetup lmsSetup = this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup();
                log.error("Get access denied error from Moodle: {} for API call: {}, response: {}",
                        lmsSetup,
                        USERS_API_FUNCTION_NAME,
                        Utils.truncateText(userDetailsJSON, 2000));
                throw new RuntimeException("No user details on Moodle API request (access-denied)");
            }

            MoodleUtils.checkJSONFormat(userDetailsJSON);

            final MoodleUserDetails[] userDetails = this.jsonMapper.<MoodleUserDetails[]> readValue(
                    userDetailsJSON,
                    new TypeReference<MoodleUserDetails[]>() {
                    });

            if (userDetails == null || userDetails.length <= 0) {
                throw new RuntimeException("No user details on Moodle API request");
            }

            if (log.isDebugEnabled()) {
                log.debug("User details received from Moodle: {}", userDetails[0]);
            }

            final Map<String, String> additionalAttributes = MoodleUtils.getMoodleAccountDetails(userDetails);
            return new ExamineeAccountDetails(
                    userDetails[0].id,
                    MoodleUtils.getDisplayName(
                            userDetails[0].firstname,
                            userDetails[0].lastname,
                            userDetails[0].fullname,
                            examineeSessionId),
                    userDetails[0].username,
                    userDetails[0].email,
                    additionalAttributes);
        });
    }

    @Override
    public String getExamineeName(final String examineeUserId) {
        return getExamineeAccountDetails(examineeUserId)
                .map(ExamineeAccountDetails::getDisplayName)
                .onError(error -> log.warn("Failed to request user-name for ID: {}", error.getMessage(), error))
                .getOr(examineeUserId);
    }

    @Override
    public Result<Chapters> getCourseChapters(final String courseId) {
        return Result.of(new Chapters(Collections.emptyList()));
    }

    private String getLmsSetupName() {
        return this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup().name;
    }

    private void fetchQuizzesPage(
            final int page,
            final DateTime quizFromTime,
            final String nameCondition,
            final AsyncQuizFetchBuffer asyncQuizFetchBuffer,
            final Predicate<QuizData> quizFilter) throws JsonParseException, JsonMappingException, IOException {

        final MoodleAPIRestTemplate restTemplate = getRestTemplate().getOrThrow();

        final LmsSetup lmsSetup = this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup();
        final String urlPrefix = (lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                ? lmsSetup.lmsApiUrl + MOODLE_QUIZ_START_URL_PATH
                : lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MOODLE_QUIZ_START_URL_PATH;

        final Collection<CourseData> fetchCoursesPage =
                fetchCoursesPage(restTemplate, quizFromTime, nameCondition, page, this.pageSize);
        // finish if page is empty (no courses left
        if (fetchCoursesPage.isEmpty()) {
            asyncQuizFetchBuffer.finish();
            return;
        }

        // fetch and buffer quizzes
        fetchCoursesPage.stream()
                .filter(c -> !c.quizzes.isEmpty())
                .forEach(c -> asyncQuizFetchBuffer.buffer.addAll(
                        MoodleUtils.quizDataOf(lmsSetup, c, urlPrefix, this.prependShortCourseName)
                                .stream()
                                .filter(quizFilter)
                                .toList()));

        // check thresholds
        if (asyncQuizFetchBuffer.buffer.size() > this.maxSize) {
            log.warn("Maximal moodle quiz fetch size of {} reached. Cancel fetch at this point.", this.maxSize);
            asyncQuizFetchBuffer.finish();
        }
    }

    private Collection<CourseData> fetchCoursesPage(
            final MoodleAPIRestTemplate restTemplate,
            final DateTime quizFromTime,
            final String nameCondition,
            final int page,
            final int size) throws JsonParseException, JsonMappingException, IOException {

        if (log.isDebugEnabled()) {
            log.debug("Fetch course page: {}, size: {} quizFromTime: {}", page, size, quizFromTime);
        }

        final String lmsName = getLmsSetupName();
        try {
            // get course ids per page
            final long filterDate = Utils.toUnixTimeInSeconds(quizFromTime);
            final long defaultCutOff = Utils.toUnixTimeInSeconds(
                    DateTime.now(DateTimeZone.UTC).minusYears(this.cutoffTimeOffset));
            final long cutoffDate = Math.min(filterDate, defaultCutOff);
            String sqlCondition = String.format(
                    SQL_CONDITION_TEMPLATE, cutoffDate, filterDate);
            final String fromElement = String.valueOf(page * size);
            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();

            if (this.applyNameCriteria && StringUtils.isNotBlank(nameCondition)) {
                final String n = Utils.toSQLWildcard(nameCondition);
                sqlCondition = sqlCondition + " AND (" + SQL_QUIZ_NAME + " LIKE '" + n + "' OR " + SQL_COURSE_NAME + " LIKE '" + n + "')";
            }

            // Note: courseid[]=0 means all courses. Moodle don't like empty parameter
            attributes.add(PARAM_COURSE_ID_ARRAY, "0");
            attributes.add(PARAM_FILTER_COURSES, "1");
            attributes.add(PARAM_SQL_CONDITIONS, sqlCondition);
            attributes.add(PARAM_PAGE_START, fromElement);
            attributes.add(PARAM_PAGE_SIZE, String.valueOf(size));

            final String courseKeyPageJSON = this.protectedMoodlePageCall
                    .protectedRun(() -> restTemplate.callMoodleAPIFunction(
                            COURSES_API_FUNCTION_NAME,
                            attributes))
                    .getOrThrow();
            
            log.info("******** course page: {} condition: {}", page, sqlCondition);
            log.info("******** response: {}", courseKeyPageJSON);

            MoodleUtils.checkJSONFormat(courseKeyPageJSON);

            if (courseKeyPageJSON.startsWith("{\"exception\":")) {
                if (courseKeyPageJSON.contains("nocoursefound")) {
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "Got nocoursefound exception from Moodle for page: {}. "
                                        + "Assuming that there are no more courses and stop fetching.",
                                page);
                    }
                    return Collections.emptyList();
                }
                log.error("Moodle exception while page fetching on page: {}, response: {}", page, courseKeyPageJSON);
                log.info("Stop fetching because of Moodle error response");
                return Collections.emptyList();
            }

            final CoursesPlugin coursePage = this.jsonMapper.readValue(courseKeyPageJSON, CoursesPlugin.class);

            if (coursePage == null) {
                log.error("No CoursePage Response");
                return Collections.emptyList();
            }

            if (coursePage.warnings != null && !coursePage.warnings.isEmpty()) {
                MoodleUtils.logMoodleWarning(coursePage.warnings, lmsName, COURSES_API_FUNCTION_NAME);
            }

            final Collection<CourseData> result;
            if (coursePage.results == null || coursePage.results.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("LMS Setup: {} No courses found on page: {}", lmsName, page);
                    if (log.isTraceEnabled()) {
                        log.trace("Moodle response: {}", courseKeyPageJSON);
                    }
                }
                result = Collections.emptyList();
            } else {
                result = coursePage.results;
            }

            if (log.isDebugEnabled()) {
                log.debug("course page with {} courses", result.size());
            }

            return result;
        } catch (final Exception e) {
            log.error("LMS Setup: {} Unexpected error while trying to get courses page: ", lmsName, e);
            return Collections.emptyList();
        }
    }

    private List<QuizData> getQuizzesForIds(
            final MoodleAPIRestTemplate restTemplate,
            final Set<String> internalIds) {

        try {

            final LmsSetup lmsSetup = this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup();
            final String urlPrefix = (lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                    ? lmsSetup.lmsApiUrl + MOODLE_QUIZ_START_URL_PATH
                    : lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MOODLE_QUIZ_START_URL_PATH;
            final Set<String> moodleCourseIds = internalIds.stream()
                    .map(MoodleUtils::getCourseId)
                    .collect(Collectors.toSet());

            if (log.isDebugEnabled()) {
                log.debug("Get quizzes for internal ids: {}, Moodle courseI ids: {} and LMSSetup: {}",
                        internalIds,
                        moodleCourseIds,
                        lmsSetup);
            }

            final Set<String> qIdSet = internalIds.stream()
                    .map(MoodleUtils::getQuizId)
                    .collect(Collectors.toSet());

            return getCoursesForIds(restTemplate, moodleCourseIds)
                    .stream()
                    .filter(courseData -> !courseData.quizzes.isEmpty())
                    .flatMap(courseData -> MoodleUtils.quizDataOf(
                            lmsSetup,
                            courseData,
                            urlPrefix,
                            this.prependShortCourseName)
                            .stream()
                            .filter(q -> qIdSet.contains(MoodleUtils.getQuizId(q.id))))
                    .collect(Collectors.toList());

        } catch (final Exception e) {
            log.error("Unexpected error while trying to get quizzes for ids", e);
            return Collections.emptyList();
        }
    }

    private Collection<CourseData> getCoursesForIds(
            final MoodleAPIRestTemplate restTemplate,
            final Set<String> courseIds) {

        try {

            final LmsSetup lmsSetup = this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup();

            if (log.isDebugEnabled()) {
                log.debug("Get courses for ids: {} on LMS: {}", courseIds, lmsSetup);
            }

            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
            attributes.put(PARAM_COURSE_ID_ARRAY, new ArrayList<>(courseIds));
            final String coursePageJSON = restTemplate.callMoodleAPIFunction(
                    COURSES_API_FUNCTION_NAME,
                    attributes);

            MoodleUtils.checkJSONFormat(coursePageJSON);

            final CoursesPlugin courses = this.jsonMapper.readValue(
                    coursePageJSON,
                    CoursesPlugin.class);

            if (courses.results == null || courses.results.isEmpty()) {
                log.warn("No courses found for ids: {} on LMS: {}", courseIds, lmsSetup.name);

                if (courses.warnings != null && !courses.warnings.isEmpty()) {
                    MoodleUtils.logMoodleWarning(courses.warnings, lmsSetup.name, COURSES_API_FUNCTION_NAME);
                }
                return Collections.emptyList();
            }

            return courses.results;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to get courses for ids", e);
            return Collections.emptyList();
        }
    }

    private Result<MoodleAPIRestTemplate> getRestTemplate() {
        final Result<MoodleAPIRestTemplate> result = this.restTemplateFactory.getRestTemplate();
        if (!result.hasError()) {
            return result;
        }

        return this.restTemplateFactory.createRestTemplate(MooldePluginLmsAPITemplateFactory.SEB_SERVER_SERVICE_NAME);
    }

    protected String toTestString() {
        return "MoodlePluginCourseAccess [pageSize=" +
                this.pageSize +
                ", maxSize=" +
                this.maxSize +
                ", cutoffTimeOffset=" +
                this.cutoffTimeOffset +
                ", restTemplate=" +
                this.getRestTemplate().getOr(null) +
                "]";
    }

}
