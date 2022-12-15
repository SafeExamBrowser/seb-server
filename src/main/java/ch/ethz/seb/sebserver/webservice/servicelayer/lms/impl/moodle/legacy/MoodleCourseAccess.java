/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.CourseAccessAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate.Warning;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy.MoodleCourseDataAsyncLoader.CourseDataShort;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy.MoodleCourseDataAsyncLoader.CourseQuizShort;

/** Implements the LmsAPITemplate for Open edX LMS Course API access.
 *
 * See also: https://docs.moodle.org/dev/Web_service_API_functions
 *
 * NOTE: Because of the missing integration on Moodle side so far the MoodleCourseAccess
 * needs to deal with Moodle's standard API functions that don't allow to filter and page course/quiz data
 * in an easy and proper way. Therefore we have to fetch all course and quiz data from Moodle before
 * filtering and paging can be applied. Since there are possibly thousands of active courses and quizzes
 * this moodle course access implements an synchronous fetch as well as an asynchronous fetch strategy.
 * The asynchronous fetch strategy is started within a background task that batches the course and quiz
 * requests to Moodle and fill up a shared cache. A SEB Server LMS API request will start the
 * background task if needed and return immediately to do not block the request.
 * The planed Moodle integration on moodle side also defines an improved course access API. This will
 * possibly make this synchronous fetch strategy obsolete in the future. */
public class MoodleCourseAccess implements CourseAccessAPI {

    private static final long INITIAL_WAIT_TIME = 3 * Constants.SECOND_IN_MILLIS;

    private static final Logger log = LoggerFactory.getLogger(MoodleCourseAccess.class);

    private static final String MOODLE_QUIZ_START_URL_PATH = "mod/quiz/view.php?id=";

    static final String MOODLE_COURSE_API_FUNCTION_NAME = "core_course_get_courses";
    static final String MOODLE_USER_PROFILE_API_FUNCTION_NAME = "core_user_get_users_by_field";
    static final String MOODLE_QUIZ_API_FUNCTION_NAME = "mod_quiz_get_quizzes_by_courses";
    static final String MOODLE_COURSE_API_COURSE_IDS = "courseids";
    static final String MOODLE_COURSE_API_IDS = "ids";
    static final String MOODLE_COURSE_SEARCH_API_FUNCTION_NAME = "core_course_search_courses";
    static final String MOODLE_COURSE_BY_FIELD_API_FUNCTION_NAME = "core_course_get_courses_by_field";
    static final String MOODLE_COURSE_API_FIELD_NAME = "field";
    static final String MOODLE_COURSE_API_FIELD_VALUE = "value";
    static final String MOODLE_COURSE_API_SEARCH_CRITERIA_NAME = "criterianame";
    static final String MOODLE_COURSE_API_SEARCH_CRITERIA_VALUE = "criteriavalue";
    static final String MOODLE_COURSE_API_SEARCH_PAGE = "page";
    static final String MOODLE_COURSE_API_SEARCH_PAGE_SIZE = "perpage";

    private final JSONMapper jsonMapper;
    private final MoodleRestTemplateFactory moodleRestTemplateFactory;
    private final MoodleCourseDataAsyncLoader moodleCourseDataAsyncLoader;
    private final boolean prependShortCourseName;
    private final CircuitBreaker<String> protectedMoodlePageCall;
    private final int pageSize;
    private final int maxSize;

    private MoodleAPIRestTemplate restTemplate;

    public MoodleCourseAccess(
            final JSONMapper jsonMapper,
            final AsyncService asyncService,
            final MoodleRestTemplateFactory moodleRestTemplateFactory,
            final MoodleCourseDataAsyncLoader moodleCourseDataAsyncLoader,
            final Environment environment) {

        this.jsonMapper = jsonMapper;
        this.moodleCourseDataAsyncLoader = moodleCourseDataAsyncLoader;
        this.moodleRestTemplateFactory = moodleRestTemplateFactory;

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
                        Constants.SECOND_IN_MILLIS * 20),
                environment.getProperty(
                        "sebserver.webservice.circuitbreaker.moodleRestCall.timeToRecover",
                        Long.class,
                        Constants.MINUTE_IN_MILLIS));
        this.maxSize =
                environment.getProperty("sebserver.webservice.cache.moodle.course.maxSize", Integer.class, 10000);
        this.pageSize =
                environment.getProperty("sebserver.webservice.cache.moodle.course.pageSize", Integer.class, 500);
    }

    APITemplateDataSupplier getApiTemplateDataSupplier() {
        return this.moodleRestTemplateFactory.apiTemplateDataSupplier;
    }

    @Override
    public LmsSetupTestResult testCourseAccessAPI() {
        final LmsSetupTestResult attributesCheck = this.moodleRestTemplateFactory.test();
        if (!attributesCheck.isOk()) {
            return attributesCheck;
        }

        final Result<MoodleAPIRestTemplate> restTemplateRequest = getRestTemplate();
        if (restTemplateRequest.hasError()) {
            final String message = "Failed to gain access token from Moodle Rest API:\n tried token endpoints: " +
                    this.moodleRestTemplateFactory.knownTokenAccessPaths;
            log.error(message + " cause: {}", restTemplateRequest.getError().getMessage());
            return LmsSetupTestResult.ofTokenRequestError(LmsType.MOODLE, message);
        }

        final MoodleAPIRestTemplate restTemplate = restTemplateRequest.get();

        try {
            restTemplate.testAPIConnection(
                    MOODLE_COURSE_API_FUNCTION_NAME,
                    MOODLE_QUIZ_API_FUNCTION_NAME);
        } catch (final RuntimeException e) {
            log.error("Failed to access Moodle course API: ", e);
            return LmsSetupTestResult.ofQuizAccessAPIError(LmsType.MOODLE, e.getMessage());
        }

        return LmsSetupTestResult.ofOkay(LmsType.MOODLE);
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        return Result.tryCatch(() -> getRestTemplate()
                .map(template -> collectAllQuizzes(template, filterMap))
                .map(quizzes -> quizzes.stream()
                        .filter(LmsAPIService.quizFilterPredicate(filterMap))
                        .collect(Collectors.toList()))
                .getOr(Collections.emptyList()));
    }

    @Override
    public void fetchQuizzes(final FilterMap filterMap, final AsyncQuizFetchBuffer asyncQuizFetchBuffer) {
        try {
            int page = 0;
            final LmsSetup lmsSetup = getApiTemplateDataSupplier().getLmsSetup();
            final String urlPrefix = (lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                    ? lmsSetup.lmsApiUrl + MOODLE_QUIZ_START_URL_PATH
                    : lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MOODLE_QUIZ_START_URL_PATH;

            while (!asyncQuizFetchBuffer.finished && !asyncQuizFetchBuffer.canceled) {
                final MoodleAPIRestTemplate restTemplate = getRestTemplate().getOrThrow();
                // first get courses from Moodle for page
                final Map<String, CourseData> courseData = new HashMap<>();
                final Collection<CourseData> coursesPage = getCoursesPage(restTemplate, page, this.pageSize);

                if (coursesPage == null || coursesPage.isEmpty()) {
                    asyncQuizFetchBuffer.finish();
                    continue;
                }

                courseData.putAll(coursesPage
                        .stream()
                        .collect(Collectors.toMap(cd -> cd.id, Function.identity())));

                // then get all quizzes of courses and filter
                final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
                final List<String> courseIds = new ArrayList<>(courseData.keySet());
                if (courseIds.size() == 1) {
                    // NOTE: This is a workaround because the Moodle API do not support lists with only one element.
                    courseIds.add("0");
                }
                attributes.put(
                        MoodleCourseAccess.MOODLE_COURSE_API_COURSE_IDS,
                        courseIds);

                final String quizzesJSON = this.protectedMoodlePageCall
                        .protectedRun(() -> restTemplate.callMoodleAPIFunction(
                                MoodleCourseAccess.MOODLE_QUIZ_API_FUNCTION_NAME,
                                attributes))
                        .getOrThrow();

                final CourseQuizData courseQuizData = this.jsonMapper.readValue(
                        quizzesJSON,
                        CourseQuizData.class);

                if (courseQuizData == null) {
                    // return false;  SEBSERV-361
                    page++;
                    continue;
                }

                if (courseQuizData.warnings != null && !courseQuizData.warnings.isEmpty()) {
                    log.warn(
                            "There are warnings from Moodle response: Moodle: {} request: {} warnings: {} warning sample: {}",
                            lmsSetup.name,
                            MoodleCourseAccess.MOODLE_QUIZ_API_FUNCTION_NAME,
                            courseQuizData.warnings.size(),
                            courseQuizData.warnings.iterator().next().toString());
                    if (log.isTraceEnabled()) {
                        log.trace("All warnings from Moodle: {}", courseQuizData.warnings.toString());
                    }
                }

                if (courseQuizData.quizzes == null || courseQuizData.quizzes.isEmpty()) {
                    // no quizzes on this page
                    page++;
                    continue;
                }

                courseQuizData.quizzes
                        .stream()
                        .filter(getQuizFilter())
                        .forEach(quiz -> {
                            final CourseData data = courseData.get(quiz.course);
                            if (data != null) {
                                data.quizzes.add(quiz);
                            }
                        });

                courseData.values().stream()
                        .filter(c -> !c.quizzes.isEmpty())
                        .forEach(c -> asyncQuizFetchBuffer.buffer.addAll(
                                quizDataOf(lmsSetup, c, urlPrefix).stream()
                                        .filter(LmsAPIService.quizFilterPredicate(filterMap))
                                        .collect(Collectors.toList())));

                page++;
            }

            asyncQuizFetchBuffer.finish();

        } catch (final Exception e) {
            asyncQuizFetchBuffer.finish(e);
        }
    }

    @Override
    public Result<Collection<QuizData>> getQuizzes(final Set<String> ids) {
        return Result.tryCatch(() -> {

            return getRestTemplate()
                    .map(template -> getQuizzesForIds(template, ids))
                    .onError(error -> log.error("Failed to get courses for: {}", ids, error))
                    .getOrElse(() -> Collections.emptyList());
        });
    }

    @Override
    public Result<QuizData> getQuiz(final String id) {
        return Result.tryCatch(() -> {

            final Map<String, CourseDataShort> cachedCourseData = this.moodleCourseDataAsyncLoader
                    .getCachedCourseData();

            final String courseId = getCourseId(id);
            final String quizId = getQuizId(id);
            if (cachedCourseData.containsKey(courseId)) {
                final CourseDataShort courseData = cachedCourseData.get(courseId);
                final CourseQuizShort quiz = courseData.quizzes
                        .stream()
                        .filter(q -> q.id.equals(quizId))
                        .findFirst()
                        .orElse(null);

                if (quiz != null) {
                    final Map<String, String> additionalAttrs = new HashMap<>();
                    additionalAttrs.put(QuizData.ATTR_ADDITIONAL_CREATION_TIME,
                            String.valueOf(courseData.time_created));
                    additionalAttrs.put(QuizData.ATTR_ADDITIONAL_SHORT_NAME, courseData.short_name);
                    additionalAttrs.put(QuizData.ATTR_ADDITIONAL_ID_NUMBER, courseData.idnumber);
                    final LmsSetup lmsSetup = getApiTemplateDataSupplier().getLmsSetup();
                    final String urlPrefix = (lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                            ? lmsSetup.lmsApiUrl + MOODLE_QUIZ_START_URL_PATH
                            : lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MOODLE_QUIZ_START_URL_PATH;

                    return createQuizData(lmsSetup, courseData, urlPrefix, additionalAttrs, quiz);
                }
            }

            // get from LMS in protected request
            final Set<String> ids = Stream.of(id).collect(Collectors.toSet());
            return getRestTemplate()
                    .map(template -> getQuizzesForIds(template, ids))
                    .getOr(Collections.emptyList())
                    .get(0);
        });
    }

    @Override
    public void clearCourseCache() {

    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeSessionId) {
        return Result.tryCatch(() -> {

            final MoodleAPIRestTemplate template = getRestTemplate()
                    .getOrThrow();

            final MultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();
            queryAttributes.add("field", "id");
            queryAttributes.add("values[0]", examineeSessionId);

            final String userDetailsJSON = template.callMoodleAPIFunction(
                    MOODLE_USER_PROFILE_API_FUNCTION_NAME,
                    queryAttributes);

            if (checkAccessDeniedError(userDetailsJSON)) {
                final LmsSetup lmsSetup = getApiTemplateDataSupplier().getLmsSetup();
                log.error("Get access denied error from Moodle: {} for API call: {}, response: {}",
                        lmsSetup,
                        MOODLE_USER_PROFILE_API_FUNCTION_NAME,
                        Utils.truncateText(userDetailsJSON, 2000));
                throw new RuntimeException("No user details on Moodle API request (access-denied)");
            }

            final MoodleUserDetails[] userDetails = this.jsonMapper.<MoodleUserDetails[]> readValue(
                    userDetailsJSON,
                    new TypeReference<MoodleUserDetails[]>() {
                    });

            if (userDetails == null || userDetails.length <= 0) {
                throw new RuntimeException("No user details on Moodle API request");
            }

            final Map<String, String> additionalAttributes = new HashMap<>();
            additionalAttributes.put("firstname", userDetails[0].firstname);
            additionalAttributes.put("lastname", userDetails[0].lastname);
            additionalAttributes.put("department", userDetails[0].department);
            additionalAttributes.put("firstaccess", String.valueOf(userDetails[0].firstaccess));
            additionalAttributes.put("lastaccess", String.valueOf(userDetails[0].lastaccess));
            additionalAttributes.put("auth", userDetails[0].auth);
            additionalAttributes.put("suspended", String.valueOf(userDetails[0].suspended));
            additionalAttributes.put("confirmed", String.valueOf(userDetails[0].confirmed));
            additionalAttributes.put("lang", userDetails[0].lang);
            additionalAttributes.put("theme", userDetails[0].theme);
            additionalAttributes.put("timezone", userDetails[0].timezone);
            additionalAttributes.put("description", userDetails[0].description);
            additionalAttributes.put("mailformat", String.valueOf(userDetails[0].mailformat));
            additionalAttributes.put("descriptionformat", String.valueOf(userDetails[0].descriptionformat));
            return new ExamineeAccountDetails(
                    userDetails[0].id,
                    userDetails[0].fullname,
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
        return Result.ofError(new UnsupportedOperationException("not available yet"));
    }

    public void clearCache() {
        this.moodleCourseDataAsyncLoader.clearCache();
    }

    private List<QuizData> collectAllQuizzes(
            final MoodleAPIRestTemplate restTemplate,
            final FilterMap filterMap) {

        final LmsSetup lmsSetup = getApiTemplateDataSupplier().getLmsSetup();
        final String urlPrefix = (lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                ? lmsSetup.lmsApiUrl + MOODLE_QUIZ_START_URL_PATH
                : lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MOODLE_QUIZ_START_URL_PATH;

        final DateTime quizFromTime = (filterMap != null) ? filterMap.getQuizFromTime() : null;
        final long fromCutTime = (quizFromTime != null) ? Utils.toUnixTimeInSeconds(quizFromTime) : -1;

        // Verify and call the proper strategy to get the course and quiz data
        Collection<CourseDataShort> courseQuizData = Collections.emptyList();
        if (this.moodleCourseDataAsyncLoader.isRunning()) {
            courseQuizData = this.moodleCourseDataAsyncLoader.getCachedCourseData().values();
        } else if (this.moodleCourseDataAsyncLoader.getLastRunTime() <= 0) {
            // set cut time if available
            if (fromCutTime >= 0) {
                this.moodleCourseDataAsyncLoader.setFromCutTime(fromCutTime);
            }
            // first run async and wait some time, get what is there
            this.moodleCourseDataAsyncLoader.loadAsync(restTemplate);
            try {
                Thread.sleep(INITIAL_WAIT_TIME);
                courseQuizData = this.moodleCourseDataAsyncLoader.getCachedCourseData().values();
            } catch (final Exception e) {
                log.error("Failed to wait for first load run: ", e);
                return Collections.emptyList();
            }
        } else if (this.moodleCourseDataAsyncLoader.isLongRunningTask()) {
            // on long running tasks if we have a different fromCutTime as before
            // kick off the lazy loading task immediately with the new time filter
            courseQuizData = this.moodleCourseDataAsyncLoader.getCachedCourseData().values();
            if (fromCutTime > 0 && fromCutTime != this.moodleCourseDataAsyncLoader.getFromCutTime()) {
                this.moodleCourseDataAsyncLoader.setFromCutTime(fromCutTime);
                this.moodleCourseDataAsyncLoader.loadAsync(restTemplate);
                // otherwise kick off only if the last fetch task was then minutes ago
            } else if (Utils.getMillisecondsNow() - this.moodleCourseDataAsyncLoader.getLastRunTime() > 10
                    * Constants.MINUTE_IN_MILLIS) {
                this.moodleCourseDataAsyncLoader.loadAsync(restTemplate);
            }

        } else {
            // just run the task in sync
            if (fromCutTime >= 0) {
                this.moodleCourseDataAsyncLoader.setFromCutTime(fromCutTime);
            }
            this.moodleCourseDataAsyncLoader.loadSync(restTemplate);
            courseQuizData = this.moodleCourseDataAsyncLoader.getCachedCourseData().values();
        }

        if (courseQuizData.isEmpty()) {
            return Collections.emptyList();
        }

        return courseQuizData
                .stream()
                .flatMap(courseData -> quizDataOf(lmsSetup, courseData, urlPrefix).stream())
                .collect(Collectors.toList());
    }

    private List<QuizData> getQuizzesForIds(
            final MoodleAPIRestTemplate restTemplate,
            final Set<String> quizIds) {

        try {

            if (log.isDebugEnabled()) {
                log.debug("Get quizzes for ids: {}", quizIds);
            }

            final Map<String, CourseData> courseData = getCoursesForIds(
                    restTemplate,
                    quizIds.stream()
                            .map(MoodleCourseAccess::getCourseId)
                            .collect(Collectors.toSet()))
                                    .stream()
                                    .collect(Collectors.toMap(cd -> cd.id, Function.identity()));

            final List<String> courseIds = new ArrayList<>(courseData.keySet());
            if (courseIds.isEmpty()) {
                return Collections.emptyList();
            }
            if (courseIds.size() == 1) {
                // NOTE: This is a workaround because the Moodle API do not support lists with only one element.
                courseIds.add("0");
            }

            // then get all quizzes of courses and filter
            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
            attributes.put(MOODLE_COURSE_API_COURSE_IDS, courseIds);

            final String quizzesJSON = restTemplate.callMoodleAPIFunction(
                    MOODLE_QUIZ_API_FUNCTION_NAME,
                    attributes);

            final CourseQuizData courseQuizData = this.jsonMapper.readValue(
                    quizzesJSON,
                    CourseQuizData.class);
            final LmsSetup lmsSetup = getApiTemplateDataSupplier().getLmsSetup();

            if (courseQuizData == null) {
                log.error("No quizzes found for  ids: {} on LMS; {}", quizIds, lmsSetup.name);
                return Collections.emptyList();
            }

            logMoodleWarnings(courseQuizData.warnings);

            if (courseQuizData.quizzes == null || courseQuizData.quizzes.isEmpty()) {
                log.error("No quizzes found for  ids: {} on LMS; {}", quizIds, lmsSetup.name);
                return Collections.emptyList();
            }

            final Map<String, CourseData> finalCourseDataRef = courseData;
            courseQuizData.quizzes
                    .stream()
                    .forEach(quiz -> fillSelectedQuizzes(quizIds, finalCourseDataRef, quiz));

            final String urlPrefix = (lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                    ? lmsSetup.lmsApiUrl + MOODLE_QUIZ_START_URL_PATH
                    : lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MOODLE_QUIZ_START_URL_PATH;

            return courseData.values()
                    .stream()
                    .filter(c -> !c.quizzes.isEmpty())
                    .flatMap(cd -> quizDataOf(lmsSetup, cd, urlPrefix).stream())
                    .collect(Collectors.toList());

        } catch (final Exception e) {
            log.error("Unexpected error while trying to get quizzes for ids", e);
            return Collections.emptyList();
        }
    }

    private void fillSelectedQuizzes(
            final Set<String> quizIds,
            final Map<String, CourseData> finalCourseDataRef,
            final CourseQuiz quiz) {
        try {
            final CourseData course = finalCourseDataRef.get(quiz.course);
            if (course != null) {
                final String internalQuizId = getInternalQuizId(
                        quiz.course_module,
                        course.id,
                        course.short_name,
                        course.idnumber);
                if (quizIds.contains(internalQuizId)) {
                    course.quizzes.add(quiz);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to verify selected quiz for course: {}", e.getMessage());
        }
    }

    private Collection<CourseData> getCoursesForIds(
            final MoodleAPIRestTemplate restTemplate,
            final Set<String> ids) {

        try {

            if (log.isDebugEnabled()) {
                log.debug("Get courses for ids: {}", ids);
            }

            final String joinedIds = StringUtils.join(ids, Constants.COMMA);

            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
            attributes.add(MOODLE_COURSE_API_FIELD_NAME, MOODLE_COURSE_API_IDS);
            attributes.add(MOODLE_COURSE_API_FIELD_VALUE, joinedIds);
            final String coursePageJSON = restTemplate.callMoodleAPIFunction(
                    MOODLE_COURSE_BY_FIELD_API_FUNCTION_NAME,
                    attributes);

            final Courses courses = this.jsonMapper.readValue(
                    coursePageJSON,
                    Courses.class);
            final LmsSetup lmsSetup = getApiTemplateDataSupplier().getLmsSetup();

            if (courses == null) {
                log.error("No courses found for ids: {} on LMS: {}", ids, lmsSetup.name);
                return Collections.emptyList();
            }

            logMoodleWarnings(courses.warnings);

            if (courses.courses == null || courses.courses.isEmpty()) {
                log.error("No courses found for ids: {} on LMS: {}", ids, lmsSetup.name);
                return Collections.emptyList();
            }

            return courses.courses;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to get courses for ids", e);
            return Collections.emptyList();
        }
    }

    private List<QuizData> quizDataOf(
            final LmsSetup lmsSetup,
            final CourseData courseData,
            final String uriPrefix) {

        final Map<String, String> additionalAttrs = new HashMap<>();
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_CREATION_TIME, String.valueOf(courseData.time_created));
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_SHORT_NAME, courseData.short_name);
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_ID_NUMBER, courseData.idnumber);
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_FULL_NAME, courseData.full_name);
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_DISPLAY_NAME, courseData.display_name);
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_SUMMARY, courseData.summary);

        final List<QuizData> courseAndQuiz = courseData.quizzes
                .stream()
                .map(courseQuizData -> {
                    final String startURI = uriPrefix + courseQuizData.course_module;
                    additionalAttrs.put(QuizData.ATTR_ADDITIONAL_TIME_LIMIT, String.valueOf(courseQuizData.time_limit));
                    return new QuizData(
                            getInternalQuizId(
                                    courseQuizData.course_module,
                                    courseData.id,
                                    courseData.short_name,
                                    courseData.idnumber),
                            lmsSetup.getInstitutionId(),
                            lmsSetup.id,
                            lmsSetup.getLmsType(),
                            (this.prependShortCourseName)
                                    ? courseData.short_name + " : " + courseQuizData.name
                                    : courseQuizData.name,
                            courseQuizData.intro,
                            (courseQuizData.time_open != null && courseQuizData.time_open > 0)
                                    ? Utils.toDateTimeUTCUnix(courseQuizData.time_open)
                                    : Utils.toDateTimeUTCUnix(courseData.start_date),
                            (courseQuizData.time_close != null && courseQuizData.time_close > 0)
                                    ? Utils.toDateTimeUTCUnix(courseQuizData.time_close)
                                    : Utils.toDateTimeUTCUnix(courseData.end_date),
                            startURI,
                            additionalAttrs);
                })
                .collect(Collectors.toList());

        return courseAndQuiz;
    }

    private List<QuizData> quizDataOf(
            final LmsSetup lmsSetup,
            final CourseDataShort courseData,
            final String uriPrefix) {

        final Map<String, String> additionalAttrs = new HashMap<>();
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_CREATION_TIME, String.valueOf(courseData.time_created));
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_SHORT_NAME, courseData.short_name);
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_ID_NUMBER, courseData.idnumber);

        final List<QuizData> courseAndQuiz = courseData.quizzes
                .stream()
                .map(courseQuizData -> createQuizData(lmsSetup, courseData, uriPrefix, additionalAttrs, courseQuizData))
                .collect(Collectors.toList());

        return courseAndQuiz;
    }

    private QuizData createQuizData(
            final LmsSetup lmsSetup,
            final CourseDataShort courseData,
            final String uriPrefix,
            final Map<String, String> additionalAttrs,
            final CourseQuizShort courseQuizData) {

        final String startURI = uriPrefix + courseQuizData.course_module;
        return new QuizData(
                getInternalQuizId(
                        courseQuizData.course_module,
                        courseData.id,
                        courseData.short_name,
                        courseData.idnumber),
                lmsSetup.getInstitutionId(),
                lmsSetup.id,
                lmsSetup.getLmsType(),
                (this.prependShortCourseName)
                        ? courseData.short_name + " : " + courseQuizData.name
                        : courseQuizData.name,
                Constants.EMPTY_NOTE,
                (courseQuizData.time_open != null && courseQuizData.time_open > 0)
                        ? Utils.toDateTimeUTCUnix(courseQuizData.time_open)
                        : Utils.toDateTimeUTCUnix(courseData.start_date),
                (courseQuizData.time_close != null && courseQuizData.time_close > 0)
                        ? Utils.toDateTimeUTCUnix(courseQuizData.time_close)
                        : Utils.toDateTimeUTCUnix(courseData.end_date),
                startURI,
                additionalAttrs);
    }

    private Result<MoodleAPIRestTemplate> getRestTemplate() {
        if (this.restTemplate == null) {
            final Result<MoodleAPIRestTemplate> templateRequest = this.moodleRestTemplateFactory
                    .createRestTemplate();
            if (templateRequest.hasError()) {
                return templateRequest;
            } else {
                this.restTemplate = templateRequest.get();
            }
        }

        return Result.of(this.restTemplate);
    }

    public static final String getInternalQuizId(
            final String quizId,
            final String courseId,
            final String shortname,
            final String idnumber) {

        return StringUtils.join(
                new String[] {
                        quizId,
                        courseId,
                        StringUtils.isNotBlank(shortname) ? shortname : Constants.EMPTY_NOTE,
                        StringUtils.isNotBlank(idnumber) ? idnumber : Constants.EMPTY_NOTE
                },
                Constants.COLON);
    }

    public static final String getQuizId(final String internalQuizId) {
        if (StringUtils.isBlank(internalQuizId)) {
            return null;
        }

        return StringUtils.split(internalQuizId, Constants.COLON)[0];
    }

    public static final String getCourseId(final String internalQuizId) {
        if (StringUtils.isBlank(internalQuizId)) {
            return null;
        }

        return StringUtils.split(internalQuizId, Constants.COLON)[1];
    }

    public static final String getShortname(final String internalQuizId) {
        if (StringUtils.isBlank(internalQuizId)) {
            return null;
        }

        final String[] split = StringUtils.split(internalQuizId, Constants.COLON);
        if (split.length < 3) {
            return null;
        }

        final String shortName = split[2];
        return shortName.equals(Constants.EMPTY_NOTE) ? null : shortName;
    }

    public static final String getIdnumber(final String internalQuizId) {
        if (StringUtils.isBlank(internalQuizId)) {
            return null;
        }
        final String[] split = StringUtils.split(internalQuizId, Constants.COLON);
        if (split.length < 4) {
            return null;
        }

        final String idNumber = split[3];
        return idNumber.equals(Constants.EMPTY_NOTE) ? null : idNumber;
    }

    private void logMoodleWarnings(final Collection<Warning> warnings) {
        if (warnings != null && !warnings.isEmpty()) {
            if (log.isDebugEnabled()) {
                final LmsSetup lmsSetup = getApiTemplateDataSupplier().getLmsSetup();
                log.debug(
                        "There are warnings from Moodle response: Moodle: {} request: {} warnings: {} warning sample: {}",
                        lmsSetup,
                        MoodleCourseAccess.MOODLE_QUIZ_API_FUNCTION_NAME,
                        warnings.size(),
                        warnings.iterator().next().toString());
            } else if (log.isTraceEnabled()) {
                log.trace("All warnings from Moodle: {}", warnings.toString());
            }
        }
    }

    private static final Pattern ACCESS_DENIED_PATTERN_1 =
            Pattern.compile(Pattern.quote("No access rights"), Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCESS_DENIED_PATTERN_2 =
            Pattern.compile(Pattern.quote("access denied"), Pattern.CASE_INSENSITIVE);

    public static final boolean checkAccessDeniedError(final String courseKeyPageJSON) {
        return ACCESS_DENIED_PATTERN_1
                .matcher(courseKeyPageJSON)
                .find() ||
                ACCESS_DENIED_PATTERN_2
                        .matcher(courseKeyPageJSON)
                        .find();
    }

    private Collection<CourseData> getCoursesPage(
            final MoodleAPIRestTemplate restTemplate,
            final int page,
            final int size) throws JsonParseException, JsonMappingException, IOException {

        final String lmsName = getApiTemplateDataSupplier().getLmsSetup().name;
        try {
            // get course ids per page
            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_SEARCH_CRITERIA_NAME, "search");
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_SEARCH_CRITERIA_VALUE, "");
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_SEARCH_PAGE, String.valueOf(page));
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_SEARCH_PAGE_SIZE, String.valueOf(size));

            final String courseKeyPageJSON = this.protectedMoodlePageCall
                    .protectedRun(() -> restTemplate.callMoodleAPIFunction(
                            MoodleCourseAccess.MOODLE_COURSE_SEARCH_API_FUNCTION_NAME,
                            attributes))
                    .getOrThrow();

            final CoursePage keysPage = this.jsonMapper.readValue(
                    courseKeyPageJSON,
                    CoursePage.class);

            if (keysPage == null) {
                log.error("No CoursePage Response");
                return Collections.emptyList();
            }

            if (keysPage.warnings != null && !keysPage.warnings.isEmpty()) {
                log.warn(
                        "There are warnings from Moodle response: Moodle: {} request: {} warnings: {} warning sample: {}",
                        lmsName,
                        MoodleCourseAccess.MOODLE_COURSE_SEARCH_API_FUNCTION_NAME,
                        keysPage.warnings.size(),
                        keysPage.warnings.iterator().next().toString());
                if (log.isTraceEnabled()) {
                    log.trace("All warnings from Moodle: {}", keysPage.warnings.toString());
                }
            }

            if (keysPage.courseKeys == null || keysPage.courseKeys.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("LMS Setup: {} No courses found on page: {}", lmsName, page);
                    if (log.isTraceEnabled()) {
                        log.trace("Moodle response: {}", courseKeyPageJSON);
                    }
                }
                return Collections.emptyList();
            }

            // get courses
            final Set<String> ids = keysPage.courseKeys
                    .stream()
                    .map(key -> key.id)
                    .collect(Collectors.toSet());

            final Collection<CourseData> result = getCoursesForIds(restTemplate, ids)
                    .stream()
                    .filter(getCourseFilter())
                    .collect(Collectors.toList());

            if (log.isDebugEnabled()) {
                log.debug("course page with {} courses, after filtering {} left",
                        keysPage.courseKeys.size(),
                        result.size());
            }

            return result;
        } catch (final Exception e) {
            log.error("LMS Setup: {} Unexpected error while trying to get courses page: ", lmsName, e);
            return Collections.emptyList();
        }
    }

    private Predicate<CourseData> getCourseFilter() {
        final long now = Utils.getSecondsNow();
        return course -> {
            if (course.start_date != null
                    && course.start_date < Utils.toUnixTimeInSeconds(DateTime.now(DateTimeZone.UTC).minusYears(3))) {
                return false;
            }

            if (course.end_date == null || course.end_date == 0 || course.end_date > now) {
                return true;
            }

            if (log.isDebugEnabled()) {
                log.info("remove course {} end_time {} now {}",
                        course.short_name,
                        course.end_date,
                        now);
            }
            return false;
        };
    }

    private Predicate<CourseQuiz> getQuizFilter() {
        final long now = Utils.getSecondsNow();
        return quiz -> {
            if (quiz.time_close == null || quiz.time_close == 0 || quiz.time_close > now) {
                return true;
            }

            if (log.isDebugEnabled()) {
                log.debug("remove quiz {} end_time {} now {}",
                        quiz.name,
                        quiz.time_close,
                        now);
            }
            return false;
        };
    }

    // ---- Mapping Classes ---

    /** Maps the Moodle course API course data */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class CourseData {
        final String id;
        final String short_name;
        final String idnumber;
        final String full_name;
        final String display_name;
        final String summary;
        final Long start_date; // unix-time seconds UTC
        final Long end_date; // unix-time seconds UTC
        final Long time_created; // unix-time seconds UTC
        final Collection<CourseQuiz> quizzes = new ArrayList<>();

        @JsonCreator
        protected CourseData(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "shortname") final String short_name,
                @JsonProperty(value = "idnumber") final String idnumber,
                @JsonProperty(value = "fullname") final String full_name,
                @JsonProperty(value = "displayname") final String display_name,
                @JsonProperty(value = "summary") final String summary,
                @JsonProperty(value = "startdate") final Long start_date,
                @JsonProperty(value = "enddate") final Long end_date,
                @JsonProperty(value = "timecreated") final Long time_created) {

            this.id = id;
            this.short_name = short_name;
            this.idnumber = idnumber;
            this.full_name = full_name;
            this.display_name = display_name;
            this.summary = summary;
            this.start_date = start_date;
            this.end_date = end_date;
            this.time_created = time_created;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class Courses {
        final Collection<CourseData> courses;
        final Collection<Warning> warnings;

        @JsonCreator
        protected Courses(
                @JsonProperty(value = "courses") final Collection<CourseData> courses,
                @JsonProperty(value = "warnings") final Collection<Warning> warnings) {
            this.courses = courses;
            this.warnings = warnings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class CourseQuizData {
        final Collection<CourseQuiz> quizzes;
        final Collection<Warning> warnings;

        @JsonCreator
        protected CourseQuizData(
                @JsonProperty(value = "quizzes") final Collection<CourseQuiz> quizzes,
                @JsonProperty(value = "warnings") final Collection<Warning> warnings) {
            this.quizzes = quizzes;
            this.warnings = warnings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CourseQuiz {
        final String id;
        final String course;
        final String course_module;
        final String name;
        final String intro; // HTML
        final Long time_open; // unix-time seconds UTC
        final Long time_close; // unix-time seconds UTC
        final Long time_limit; // unix-time seconds UTC

        @JsonCreator
        protected CourseQuiz(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "course") final String course,
                @JsonProperty(value = "coursemodule") final String course_module,
                @JsonProperty(value = "name") final String name,
                @JsonProperty(value = "intro") final String intro,
                @JsonProperty(value = "timeopen") final Long time_open,
                @JsonProperty(value = "timeclose") final Long time_close,
                @JsonProperty(value = "timelimit") final Long time_limit) {

            this.id = id;
            this.course = course;
            this.course_module = course_module;
            this.name = name;
            this.intro = intro;
            this.time_open = time_open;
            this.time_close = time_close;
            this.time_limit = time_limit;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class MoodleUserDetails {
        final String id;
        final String username;
        final String firstname;
        final String lastname;
        final String fullname;
        final String email;
        final String department;
        final Long firstaccess;
        final Long lastaccess;
        final String auth;
        final Boolean suspended;
        final Boolean confirmed;
        final String lang;
        final String theme;
        final String timezone;
        final String description;
        final Integer mailformat;
        final Integer descriptionformat;

        @JsonCreator
        protected MoodleUserDetails(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "username") final String username,
                @JsonProperty(value = "firstname") final String firstname,
                @JsonProperty(value = "lastname") final String lastname,
                @JsonProperty(value = "fullname") final String fullname,
                @JsonProperty(value = "email") final String email,
                @JsonProperty(value = "department") final String department,
                @JsonProperty(value = "firstaccess") final Long firstaccess,
                @JsonProperty(value = "lastaccess") final Long lastaccess,
                @JsonProperty(value = "auth") final String auth,
                @JsonProperty(value = "suspended") final Boolean suspended,
                @JsonProperty(value = "confirmed") final Boolean confirmed,
                @JsonProperty(value = "lang") final String lang,
                @JsonProperty(value = "theme") final String theme,
                @JsonProperty(value = "timezone") final String timezone,
                @JsonProperty(value = "description") final String description,
                @JsonProperty(value = "mailformat") final Integer mailformat,
                @JsonProperty(value = "descriptionformat") final Integer descriptionformat) {

            this.id = id;
            this.username = username;
            this.firstname = firstname;
            this.lastname = lastname;
            this.fullname = fullname;
            this.email = email;
            this.department = department;
            this.firstaccess = firstaccess;
            this.lastaccess = lastaccess;
            this.auth = auth;
            this.suspended = suspended;
            this.confirmed = confirmed;
            this.lang = lang;
            this.theme = theme;
            this.timezone = timezone;
            this.description = description;
            this.mailformat = mailformat;
            this.descriptionformat = descriptionformat;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CoursePage {
        final Collection<CourseKey> courseKeys;
        final Collection<Warning> warnings;

        public CoursePage(
                @JsonProperty(value = "courses") final Collection<CourseKey> courseKeys,
                @JsonProperty(value = "warnings") final Collection<Warning> warnings) {

            this.courseKeys = courseKeys;
            this.warnings = warnings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CourseKey {
        final String id;
        final String short_name;
        final String category_name;
        final String sort_order;

        @JsonCreator
        protected CourseKey(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "shortname") final String short_name,
                @JsonProperty(value = "categoryname") final String category_name,
                @JsonProperty(value = "sortorder") final String sort_order) {

            this.id = id;
            this.short_name = short_name;
            this.category_name = category_name;
            this.sort_order = sort_order;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("CourseKey [id=");
            builder.append(this.id);
            builder.append(", short_name=");
            builder.append(this.short_name);
            builder.append(", category_name=");
            builder.append(this.category_name);
            builder.append(", sort_order=");
            builder.append(this.sort_order);
            builder.append("]");
            return builder.toString();
        }

    }

}
