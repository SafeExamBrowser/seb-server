/*
 * Copyright (c) 2020 ETH Zürich, IT Services
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin.MooldePluginLmsAPITemplateFactory;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.CourseAccessAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.CourseData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.CoursePage;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.CourseQuiz;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.CourseQuizData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.Courses;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.MoodleUserDetails;

/** Implements the LmsAPITemplate for Open edX LMS Course API access.
 * <p>
 * See also: https://docs.moodle.org/dev/Web_service_API_functions
 * <p>
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

    //private static final long INITIAL_WAIT_TIME = 3 * Constants.SECOND_IN_MILLIS;

    private static final Logger log = LoggerFactory.getLogger(MoodleCourseAccess.class);

    private static final String MOODLE_QUIZ_START_URL_PATH = "mod/quiz/view.php?id=";

    static final String MOODLE_COURSE_API_FUNCTION_NAME = "core_course_get_courses";
    static final String MOODLE_USER_PROFILE_API_FUNCTION_NAME = "core_user_get_users_by_field";
    static final String MOODLE_QUIZ_API_FUNCTION_NAME = "mod_quiz_get_quizzes_by_courses";
    static final String MOODLE_COURSE_API_COURSE_IDS = "courseids";
    static final String MOODLE_COURSE_API_IDS = "ids";
    static final String MOODLE_COURSE_API_COURSE_SHORTNAME = "shortname";
    static final String MOODLE_COURSE_SEARCH_API_FUNCTION_NAME = "core_course_search_courses";
    static final String MOODLE_COURSE_BY_FIELD_API_FUNCTION_NAME = "core_course_get_courses_by_field";
    static final String MOODLE_COURSE_API_FIELD_NAME = "field";
    static final String MOODLE_COURSE_API_FIELD_VALUE = "value";
    static final String MOODLE_COURSE_API_SEARCH_CRITERIA_NAME = "criterianame";
    static final String MOODLE_COURSE_API_SEARCH_CRITERIA_VALUE = "criteriavalue";
    static final String MOODLE_COURSE_API_SEARCH_PAGE = "page";
    static final String MOODLE_COURSE_API_SEARCH_PAGE_SIZE = "perpage";

    private final JSONMapper jsonMapper;
    private final MoodleRestTemplateFactory restTemplateFactory;
    private final boolean prependShortCourseName;
    private final CircuitBreaker<String> protectedMoodlePageCall;
    private final int pageSize;
    private final int maxSize;


    public MoodleCourseAccess(
            final JSONMapper jsonMapper,
            final AsyncService asyncService,
            final MoodleRestTemplateFactory restTemplateFactory,
            final Environment environment) {

        this.jsonMapper = jsonMapper;
        this.restTemplateFactory = restTemplateFactory;

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
        return this.restTemplateFactory.getApiTemplateDataSupplier();
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
    public void fetchQuizzes(final FilterMap filterMap, final AsyncQuizFetchBuffer asyncQuizFetchBuffer) {
        try {
            int page = 0;
            final LmsSetup lmsSetup = getApiTemplateDataSupplier().getLmsSetup();
            final MoodleAPIRestTemplate restTemplate = getRestTemplate().getOrThrow();
            final String urlPrefix = (lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                    ? lmsSetup.lmsApiUrl + MOODLE_QUIZ_START_URL_PATH
                    : lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MOODLE_QUIZ_START_URL_PATH;

            while (!asyncQuizFetchBuffer.finished && !asyncQuizFetchBuffer.canceled) {
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
                    MoodleUtils.logMoodleWarning(
                            courseQuizData.warnings,
                            lmsSetup.name,
                            MoodleCourseAccess.MOODLE_QUIZ_API_FUNCTION_NAME);
                }

                if (courseQuizData.quizzes == null || courseQuizData.quizzes.isEmpty()) {
                    // no quizzes on this page
                    page++;
                    continue;
                }

                courseQuizData.quizzes
                        .stream()
                        .filter(MoodleUtils.getQuizFilter())
                        .forEach(quiz -> {
                            final CourseData data = courseData.get(quiz.course);
                            if (data != null) {
                                data.quizzes.add(quiz);
                            }
                        });

                courseData.values().stream()
                        .filter(c -> !c.quizzes.isEmpty())
                        .forEach(c -> asyncQuizFetchBuffer.buffer.addAll(
                                MoodleUtils.quizDataOf(lmsSetup, c, urlPrefix, this.prependShortCourseName, false)
                                        .stream()
                                        .filter(LmsAPIService.quizFilterPredicate(filterMap))
                                        .collect(Collectors.toList())));

                if (asyncQuizFetchBuffer.buffer.size() > this.maxSize) {
                    log.warn("Maximal moodle quiz fetch size of {} reached. Cancel fetch at this point.", this.maxSize);
                    asyncQuizFetchBuffer.finish();
                }

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
            // get from LMS in protected request
            final Set<String> ids = Stream.of(id).collect(Collectors.toSet());
            return getRestTemplate()
                    .map(template -> getQuizzesForIds(template, ids))
                    .getOr(Collections.emptyList())
                    .get(0);
        });
    }

    @Override
    public Result<QuizData> tryRecoverQuizForExam(final Exam exam) {
        return Result.tryCatch(() -> {

            final LmsSetup lmsSetup = getApiTemplateDataSupplier().getLmsSetup();
            final MoodleAPIRestTemplate restTemplate = getRestTemplate().getOrThrow();
            final String urlPrefix = (lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                    ? lmsSetup.lmsApiUrl + MOODLE_QUIZ_START_URL_PATH
                    : lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MOODLE_QUIZ_START_URL_PATH;

            // get the course name identifier for recovering
            final String shortname = MoodleUtils.getShortname(exam.externalId);

            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
            attributes.add(MOODLE_COURSE_API_FIELD_NAME, MOODLE_COURSE_API_COURSE_SHORTNAME);
            attributes.add(MOODLE_COURSE_API_FIELD_VALUE, shortname);
            final String coursePageJSON = restTemplate.callMoodleAPIFunction(
                    MOODLE_COURSE_BY_FIELD_API_FUNCTION_NAME,
                    attributes);

            final Map<String, CourseData> courses = this.jsonMapper.readValue(
                    coursePageJSON,
                    Courses.class).courses
                            .stream()
                            .collect(Collectors.toMap(c -> c.id, Function.identity()));

            // then get all quizzes of courses and filter
            final LinkedMultiValueMap<String, String> cAttributes = new LinkedMultiValueMap<>();
            final List<String> courseIds = new ArrayList<>(courses.keySet());
            if (courseIds.size() == 1) {
                // NOTE: This is a workaround because the Moodle API do not support lists with only one element.
                courseIds.add("0");
            }
            cAttributes.put(
                    MoodleCourseAccess.MOODLE_COURSE_API_COURSE_IDS,
                    courseIds);

            final String quizzesJSON = this.protectedMoodlePageCall
                    .protectedRun(() -> restTemplate.callMoodleAPIFunction(
                            MoodleCourseAccess.MOODLE_QUIZ_API_FUNCTION_NAME,
                            attributes))
                    .getOrThrow();

            this.jsonMapper.readValue(
                    quizzesJSON,
                    CourseQuizData.class).quizzes.stream().forEach(quiz -> {
                        final CourseData data = courses.get(quiz.course);
                        if (data != null) {
                            data.quizzes.add(quiz);
                        }
                    });

            return courses.values()
                    .stream()
                    .flatMap(c -> MoodleUtils.quizDataOf(
                            lmsSetup,
                            c,
                            urlPrefix,
                            this.prependShortCourseName, false).stream())
                    .filter(q -> exam.name.contains(q.name))
                    .findFirst()
                    .get();
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

            if (MoodleUtils.checkAccessDeniedError(userDetailsJSON)) {
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
                            .map(MoodleUtils::getCourseId)
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

            if (courseQuizData.warnings != null && !courseQuizData.warnings.isEmpty()) {
                MoodleUtils.logMoodleWarning(
                        courseQuizData.warnings,
                        lmsSetup.name,
                        MoodleCourseAccess.MOODLE_QUIZ_API_FUNCTION_NAME);
            }

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
                    .flatMap(cd -> MoodleUtils.quizDataOf(
                            lmsSetup,
                            cd,
                            urlPrefix,
                            this.prependShortCourseName,
                            false).stream())
                    .collect(Collectors.toList());

        } catch (final Exception e) {
            log.error("Unexpected error while trying to get quizzes for ids", e);
            return Collections.emptyList();
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

            if (courses.warnings != null && !courses.warnings.isEmpty()) {
                MoodleUtils.logMoodleWarning(
                        courses.warnings,
                        lmsSetup.name,
                        MoodleCourseAccess.MOODLE_QUIZ_API_FUNCTION_NAME);
            }

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

    private static final void fillSelectedQuizzes(
            final Set<String> quizIds,
            final Map<String, CourseData> finalCourseDataRef,
            final CourseQuiz quiz) {
        try {
            final CourseData course = finalCourseDataRef.get(quiz.course);
            if (course != null) {
                final String internalQuizId = MoodleUtils.getInternalQuizId(
                        quiz.course_module, // TODO this is wrong should be id. Create recovery task
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

    private Result<MoodleAPIRestTemplate> getRestTemplate() {
        final Result<MoodleAPIRestTemplate> result = this.restTemplateFactory.getRestTemplate();
        if (!result.hasError()) {
            return result;
        }

        return this.restTemplateFactory.createRestTemplate(MoodleLmsAPITemplateFactory.MOODLE_MOBILE_APP_SERVICE);
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
                    .filter(MoodleUtils.getCourseFilter())
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

}
