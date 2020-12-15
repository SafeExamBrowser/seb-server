/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle;

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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.CourseAccess;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory.MoodleAPIRestTemplate;

/** Implements the LmsAPITemplate for Open edX LMS Course API access.
 *
 * See also: https://docs.moodle.org/dev/Web_service_API_functions */
public class MoodleCourseAccess extends CourseAccess {

    private static final Logger log = LoggerFactory.getLogger(MoodleCourseAccess.class);

    private static final String MOODLE_QUIZ_START_URL_PATH = "mod/quiz/view.php?id=";

    private static final String MOODLE_COURSE_API_FUNCTION_NAME = "core_course_get_courses";
    private static final String MOODLE_USER_PROFILE_API_FUNCTION_NAME = "core_user_get_users_by_field";
    private static final String MOODLE_QUIZ_API_FUNCTION_NAME = "mod_quiz_get_quizzes_by_courses";
    private static final String MOODLE_COURSE_API_COURSE_IDS = "courseids";
    private static final String MOODLE_COURSE_API_IDS = "ids";
    private static final String MOODLE_COURSE_SEARCH_API_FUNCTION_NAME = "core_course_search_courses";
    private static final String MOODLE_COURSE_BY_FIELD_API_FUNCTION_NAME = "core_course_get_courses_by_field";
    private static final String MOODLE_COURSE_API_FIELD_NAME = "field";
    private static final String MOODLE_COURSE_API_FIELD_VALUE = "value";
    private static final String MOODLE_COURSE_API_SEARCH_CRITERIA_NAME = "criterianame";
    private static final String MOODLE_COURSE_API_SEARCH_CRITERIA_VALUE = "criteriavalue";
    private static final String MOODLE_COURSE_API_SEARCH_PAGE = "page";
    private static final String MOODLE_COURSE_API_SEARCH_PAGE_SIZE = "perpage";

    private static final int DEFAULT_FROM_YEARS = 3;

    private final JSONMapper jsonMapper;
    private final LmsSetup lmsSetup;
    private final MoodleRestTemplateFactory moodleRestTemplateFactory;

    private MoodleAPIRestTemplate restTemplate;

    protected MoodleCourseAccess(
            final JSONMapper jsonMapper,
            final LmsSetup lmsSetup,
            final MoodleRestTemplateFactory moodleRestTemplateFactory,
            final AsyncService asyncService) {

        super(asyncService);
        this.jsonMapper = jsonMapper;
        this.lmsSetup = lmsSetup;
        this.moodleRestTemplateFactory = moodleRestTemplateFactory;
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

    LmsSetupTestResult initAPIAccess() {

        final LmsSetupTestResult attributesCheck = this.moodleRestTemplateFactory.test();
        if (!attributesCheck.isOk()) {
            return attributesCheck;
        }

        final Result<MoodleAPIRestTemplate> restTemplateRequest = getRestTemplate();
        if (restTemplateRequest.hasError()) {
            final String message = "Failed to gain access token from Moodle Rest API:\n tried token endpoints: " +
                    this.moodleRestTemplateFactory.knownTokenAccessPaths;
            log.error(message + " cause: ", restTemplateRequest.getError());
            return LmsSetupTestResult.ofTokenRequestError(message);
        }

        final MoodleAPIRestTemplate restTemplate = restTemplateRequest.get();

        try {
            restTemplate.testAPIConnection(
                    MOODLE_COURSE_API_FUNCTION_NAME,
                    MOODLE_QUIZ_API_FUNCTION_NAME);
        } catch (final RuntimeException e) {
            log.error("Failed to access Moodle course API: ", e);
            return LmsSetupTestResult.ofQuizAccessAPIError(e.getMessage());
        }

        return LmsSetupTestResult.ofOkay();
    }

    @Override
    protected Supplier<List<QuizData>> quizzesSupplier(final Set<String> ids) {
        return () -> getRestTemplate()
                .map(template -> getQuizzesForIds(template, ids))
                .getOrThrow();

    }

    @Override
    protected Supplier<List<QuizData>> allQuizzesSupplier(final FilterMap filterMap) {
        return () -> getRestTemplate()
                .map(template -> collectAllQuizzes(template, filterMap))
                .getOrThrow();
    }

    @Override
    protected Supplier<Chapters> getCourseChaptersSupplier(final String courseId) {
        throw new UnsupportedOperationException("not available yet");
    }

    private ArrayList<QuizData> collectAllQuizzes(
            final MoodleAPIRestTemplate restTemplate,
            final FilterMap filterMap) {

        final String urlPrefix = (this.lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                ? this.lmsSetup.lmsApiUrl + MOODLE_QUIZ_START_URL_PATH
                : this.lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MOODLE_QUIZ_START_URL_PATH;
        return getAllQuizzes(restTemplate, filterMap)
                .stream()
                .reduce(
                        new ArrayList<>(),
                        (list, courseData) -> {
                            list.addAll(quizDataOf(
                                    this.lmsSetup,
                                    courseData,
                                    urlPrefix));
                            return list;
                        },
                        (list1, list2) -> {
                            list1.addAll(list2);
                            return list1;
                        });
    }

    private List<CourseData> getAllQuizzes(
            final MoodleAPIRestTemplate restTemplate,
            final FilterMap filterMap) {

        final List<CourseData> result = new ArrayList<>();

        int page = 0;
        List<CourseData> quizzesBatch = getQuizzesBatch(restTemplate, filterMap, page);
        result.addAll(quizzesBatch);

        log.info("Got quiz page batch for page {} with {} items", page, quizzesBatch.size());

        while (!quizzesBatch.isEmpty()) {
            page++;
            quizzesBatch = getQuizzesBatch(restTemplate, filterMap, page);
            result.addAll(quizzesBatch);

            log.info("Got quiz page batch for page {} with {} items", page, quizzesBatch.size());
        }
        return result;
    }

    private List<CourseData> getQuizzesBatch(
            final MoodleAPIRestTemplate restTemplate,
            final FilterMap filterMap,
            final int page) {

        try {

            final long fromTime = (filterMap != null && filterMap.getQuizFromTime() != null)
                    ? Utils.toUnixTimeInSeconds(filterMap.getQuizFromTime())
                    : Utils.toUnixTimeInSeconds(DateTime.now(DateTimeZone.UTC).minusYears(DEFAULT_FROM_YEARS));

            System.out.println("******************** fromTime=" + fromTime);

            // first get courses from Moodle for page
            final Map<String, CourseData> courseData = new HashMap<>();
            final Collection<CourseData> coursesPage = getCoursesPage(restTemplate, fromTime, page, 100);

            if (coursesPage.isEmpty()) {
                return Collections.emptyList();
            }

            courseData.putAll(coursesPage
                    .stream()
                    .collect(Collectors.toMap(cd -> cd.id, Function.identity())));

            // then get all quizzes of courses and filter
            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
            attributes.put(MOODLE_COURSE_API_COURSE_IDS, new ArrayList<>(courseData.keySet()));

            final String quizzesJSON = restTemplate.callMoodleAPIFunction(
                    MOODLE_QUIZ_API_FUNCTION_NAME,
                    attributes);

            final CourseQuizData courseQuizData = this.jsonMapper.readValue(
                    quizzesJSON,
                    CourseQuizData.class);

            final Map<String, CourseData> finalCourseDataRef = courseData;
            if (courseQuizData.quizzes != null) {
                courseQuizData.quizzes
                        .forEach(quiz -> {
                            final CourseData course = finalCourseDataRef.get(quiz.course);
                            if (course != null) {
                                course.quizzes.add(quiz);
                            }
                        });
            }

            return courseData.values()
                    .stream()
                    .filter(c -> !c.quizzes.isEmpty())
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            log.error("Unexpected exception while trying to get course data: ", e);
            return Collections.emptyList();
        }
    }

    private Predicate<CourseData> getCourseFilter(final long from) {
        final long now = DateTime.now(DateTimeZone.UTC).getMillis() / 1000;
        return course -> {
            if (course.end_date != null && course.end_date > now) {
                return false;
            }
            if (course.time_created != null && course.time_created.longValue() < from) {
                return false;
            }

            return true;
        };
    }

    private Collection<CourseData> getCoursesPage(
            final MoodleAPIRestTemplate restTemplate,
            final long startDate,
            final int page,
            final int size) throws JsonParseException, JsonMappingException, IOException {

        try {
            // get course ids per page
            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
            attributes.add(MOODLE_COURSE_API_SEARCH_CRITERIA_NAME, "search");
            attributes.add(MOODLE_COURSE_API_SEARCH_CRITERIA_VALUE, "");
            attributes.add(MOODLE_COURSE_API_SEARCH_PAGE, String.valueOf(page));
            attributes.add(MOODLE_COURSE_API_SEARCH_PAGE_SIZE, String.valueOf(size));

            final String courseKeyPageJSON = restTemplate.callMoodleAPIFunction(
                    MOODLE_COURSE_SEARCH_API_FUNCTION_NAME,
                    attributes);

            final CoursePage keysPage = this.jsonMapper.readValue(
                    courseKeyPageJSON,
                    CoursePage.class);

            if (keysPage == null || keysPage.courseKeys == null || keysPage.courseKeys.isEmpty()) {
                log.info("No courses found on page: {}", page);
                return Collections.emptyList();
            }

            log.info("Got course page with: {} items", keysPage.courseKeys.size());
            log.info("course items:\n{} items", keysPage.courseKeys);

            // get courses
            final Set<String> ids = keysPage.courseKeys
                    .stream()
                    .map(key -> key.id)
                    .collect(Collectors.toSet());

            final Collection<CourseData> result = getCoursesForIds(restTemplate, ids)
                    .stream()
                    .filter(getCourseFilter(startDate))
                    .collect(Collectors.toList());

            log.info("After filtering {} left", result.size());

            return result;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to get courses page: ", e);
            return Collections.emptyList();
        }
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

            final Map<String, CourseData> finalCourseDataRef = courseData;
            courseQuizData.quizzes
                    .forEach(quiz -> {
                        final CourseData course = finalCourseDataRef.get(quiz.course);
                        if (course != null) {
                            course.quizzes.add(quiz);
                        }
                    });

            final String urlPrefix = (this.lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                    ? this.lmsSetup.lmsApiUrl + MOODLE_QUIZ_START_URL_PATH
                    : this.lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MOODLE_QUIZ_START_URL_PATH;

            return courseData.values()
                    .stream()
                    .filter(c -> !c.quizzes.isEmpty())
                    .reduce(
                            new ArrayList<>(),
                            (list, cd) -> {
                                list.addAll(quizDataOf(
                                        this.lmsSetup,
                                        cd,
                                        urlPrefix));
                                return list;
                            },
                            (list1, list2) -> {
                                list1.addAll(list2);
                                return list1;
                            });

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

            return this.jsonMapper.<Courses> readValue(
                    coursePageJSON,
                    Courses.class).courses;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to get courses for ids", e);
            return Collections.emptyList();
        }
    }

    static Map<String, String> additionalAttrs = new HashMap<>();

    private List<QuizData> quizDataOf(
            final LmsSetup lmsSetup,
            final CourseData courseData,
            final String uriPrefix) {

        additionalAttrs.clear();
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
                            courseQuizData.name,
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

    // ---- Mapping Classes ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CoursePage {
        final Collection<CourseKey> courseKeys;

        public CoursePage(
                @JsonProperty(value = "courses") final Collection<CourseKey> courseKeys) {

            this.courseKeys = courseKeys;
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

    /** Maps the Moodle course API course data */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CourseData {
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
    static final class Courses {
        final Collection<CourseData> courses;

        @JsonCreator
        protected Courses(
                @JsonProperty(value = "courses") final Collection<CourseData> courses) {
            this.courses = courses;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CourseQuizData {
        final Collection<CourseQuiz> quizzes;

        @JsonCreator
        protected CourseQuizData(
                @JsonProperty(value = "quizzes") final Collection<CourseQuiz> quizzes) {
            this.quizzes = quizzes;
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

}
