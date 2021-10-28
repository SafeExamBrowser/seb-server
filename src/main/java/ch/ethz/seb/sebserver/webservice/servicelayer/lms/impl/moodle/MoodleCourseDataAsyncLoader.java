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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncRunner;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleCourseAccess.Warning;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory.MoodleAPIRestTemplate;

@Lazy
@Component
@WebServiceProfile
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
/** This implements the (temporary) asynchronous fetch strategy to fetch
 * course and quiz data within a background task and fill up a shared cache. */
public class MoodleCourseDataAsyncLoader {

    private static final Logger log = LoggerFactory.getLogger(MoodleCourseDataAsyncLoader.class);

    private final JSONMapper jsonMapper;
    private final AsyncRunner asyncRunner;
    private final CircuitBreaker<String> moodleRestCall;
    private final int maxSize;
    private final int pageSize;

    private final Map<String, CourseDataShort> cachedCourseData = new HashMap<>();
    private final Set<String> newIds = new HashSet<>();

    private String lmsSetup = Constants.EMPTY_NOTE;
    private long lastRunTime = 0;
    private long lastLoadTime = 0;
    private boolean running = false;

    private long fromCutTime;

    public MoodleCourseDataAsyncLoader(
            final JSONMapper jsonMapper,
            final AsyncService asyncService,
            final AsyncRunner asyncRunner,
            final Environment environment) {

        this.jsonMapper = jsonMapper;
        this.fromCutTime = Utils.toUnixTimeInSeconds(DateTime.now(DateTimeZone.UTC).minusYears(3));
        this.asyncRunner = asyncRunner;

        this.moodleRestCall = asyncService.createCircuitBreaker(
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

    public void init(final String lmsSetupName) {
        if (Constants.EMPTY_NOTE.equals(this.lmsSetup)) {
            this.lmsSetup = lmsSetupName;
        } else {
            throw new IllegalStateException(
                    "Invalid initialization of MoodleCourseDataAsyncLoader. It has already been initialized yet");
        }
    }

    public long getFromCutTime() {
        return this.fromCutTime;
    }

    public void setFromCutTime(final long fromCutTime) {
        this.fromCutTime = fromCutTime;
    }

    public Map<String, CourseDataShort> getCachedCourseData() {
        return new HashMap<>(this.cachedCourseData);
    }

    public long getLastRunTime() {
        return this.lastRunTime;
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isLongRunningTask() {
        return this.lastLoadTime > 3 * Constants.SECOND_IN_MILLIS;
    }

    public Map<String, CourseDataShort> loadSync(final MoodleAPIRestTemplate restTemplate) {
        if (this.running) {
            throw new IllegalStateException("Is already running asynchronously");
        }

        this.running = true;
        loadAndCache(restTemplate).run();
        this.lastRunTime = Utils.getMillisecondsNow();

        log.info("LMS Setup: {} loaded {} courses synchronously",
                this.lmsSetup,
                this.cachedCourseData.size());

        return this.cachedCourseData;
    }

    public void loadAsync(final MoodleAPIRestTemplate restTemplate) {
        if (this.running) {
            return;
        }
        this.running = true;
        this.asyncRunner.runAsync(loadAndCache(restTemplate));
        this.lastRunTime = Utils.getMillisecondsNow();

        log.info("LMS Setup: {} loaded {} courses asynchronously",
                this.lmsSetup,
                this.cachedCourseData.size());

    }

    public void clearCache() {
        if (!isRunning()) {
            this.cachedCourseData.clear();
            this.newIds.clear();
        }
    }

    private Runnable loadAndCache(final MoodleAPIRestTemplate restTemplate) {
        return () -> {
            this.newIds.clear();
            final long startTime = Utils.getMillisecondsNow();

            loadAllQuizzes(restTemplate);
            this.syncCache();

            this.lastLoadTime = Utils.getMillisecondsNow() - startTime;
            this.running = false;
        };
    }

    private void loadAllQuizzes(final MoodleAPIRestTemplate restTemplate) {
        int page = 0;
        while (getQuizzesBatch(restTemplate, page)) {
            page++;
        }
    }

    private boolean getQuizzesBatch(
            final MoodleAPIRestTemplate restTemplate,
            final int page) {

        try {

            // first get courses from Moodle for page
            final Map<String, CourseDataShort> courseData = new HashMap<>();
            final Collection<CourseDataShort> coursesPage = getCoursesPage(restTemplate, page, this.pageSize);

            if (coursesPage == null || coursesPage.isEmpty()) {
                return false;
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

            final String quizzesJSON = callMoodleRestAPI(
                    restTemplate,
                    MoodleCourseAccess.MOODLE_QUIZ_API_FUNCTION_NAME,
                    attributes);

            final CourseQuizData courseQuizData = this.jsonMapper.readValue(
                    quizzesJSON,
                    CourseQuizData.class);

            if (courseQuizData == null) {
                return false;
            }

            if (courseQuizData.warnings != null && !courseQuizData.warnings.isEmpty()) {
                log.warn(
                        "There are warnings from Moodle response: Moodle: {} request: {} warnings: {} warning sample: {}",
                        this.lmsSetup,
                        MoodleCourseAccess.MOODLE_QUIZ_API_FUNCTION_NAME,
                        courseQuizData.warnings.size(),
                        courseQuizData.warnings.iterator().next().toString());
                if (log.isTraceEnabled()) {
                    log.trace("All warnings from Moodle: {}", courseQuizData.warnings.toString());
                }
            }

            if (courseQuizData.quizzes == null || courseQuizData.quizzes.isEmpty()) {
                // no quizzes on this page
                return true;
            }

            if (courseQuizData.quizzes != null) {
                courseQuizData.quizzes
                        .stream()
                        .filter(getQuizFilter())
                        .forEach(quiz -> {
                            final CourseDataShort data = courseData.get(quiz.course);
                            if (data != null) {
                                data.quizzes.add(quiz);
                            }
                        });

                courseData.values().stream()
                        .filter(c -> !c.quizzes.isEmpty())
                        .forEach(c -> {
                            if (this.cachedCourseData.size() >= this.maxSize) {
                                log.error(
                                        "LMS Setup: {} Cache is full and has reached its maximal size. Skip data: -> {}",
                                        this.lmsSetup,
                                        c);
                            } else {
                                this.cachedCourseData.put(c.id, c);
                                this.newIds.add(c.id);
                            }
                        });

                return true;
            } else {
                return false;
            }
        } catch (final Exception e) {
            log.error("LMS Setup: {} Unexpected exception while trying to get course data: ", this.lmsSetup, e);
            return false;
        }
    }

    private Collection<CourseDataShort> getCoursesPage(
            final MoodleAPIRestTemplate restTemplate,
            final int page,
            final int size) throws JsonParseException, JsonMappingException, IOException {

        try {
            // get course ids per page
            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_SEARCH_CRITERIA_NAME, "search");
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_SEARCH_CRITERIA_VALUE, "");
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_SEARCH_PAGE, String.valueOf(page));
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_SEARCH_PAGE_SIZE, String.valueOf(size));

            final String courseKeyPageJSON = callMoodleRestAPI(
                    restTemplate,
                    MoodleCourseAccess.MOODLE_COURSE_SEARCH_API_FUNCTION_NAME,
                    attributes);

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
                        this.lmsSetup,
                        MoodleCourseAccess.MOODLE_COURSE_SEARCH_API_FUNCTION_NAME,
                        keysPage.warnings.size(),
                        keysPage.warnings.iterator().next().toString());
                if (log.isTraceEnabled()) {
                    log.trace("All warnings from Moodle: {}", keysPage.warnings.toString());
                }
            }

            if (keysPage.courseKeys == null || keysPage.courseKeys.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("LMS Setup: {} No courses found on page: {}", this.lmsSetup, page);
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

            final Collection<CourseDataShort> result = getCoursesForIds(restTemplate, ids)
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
            log.error("LMS Setup: {} Unexpected error while trying to get courses page: ", this.lmsSetup, e);
            return Collections.emptyList();
        }
    }

    private Collection<CourseDataShort> getCoursesForIds(
            final MoodleAPIRestTemplate restTemplate,
            final Set<String> ids) {

        try {

            if (log.isDebugEnabled()) {
                log.debug("LMS Setup: {} Get courses for ids: {}", this.lmsSetup, ids);
            }

            final String joinedIds = StringUtils.join(ids, Constants.COMMA);

            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_FIELD_NAME, MoodleCourseAccess.MOODLE_COURSE_API_IDS);
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_FIELD_VALUE, joinedIds);
            final String coursePageJSON = callMoodleRestAPI(
                    restTemplate,
                    MoodleCourseAccess.MOODLE_COURSE_BY_FIELD_API_FUNCTION_NAME,
                    attributes);

            final Courses courses = this.jsonMapper.readValue(
                    coursePageJSON,
                    Courses.class);

            if (courses == null) {
                log.error("No Courses response: LMS: {} API call: {}", this.lmsSetup,
                        MoodleCourseAccess.MOODLE_COURSE_BY_FIELD_API_FUNCTION_NAME);
                return Collections.emptyList();
            }

            if (courses.warnings != null && !courses.warnings.isEmpty()) {
                log.warn(
                        "There are warnings from Moodle response: Moodle: {} request: {} warnings: {} warning sample: {}",
                        this.lmsSetup,
                        MoodleCourseAccess.MOODLE_COURSE_BY_FIELD_API_FUNCTION_NAME,
                        courses.warnings.size(),
                        courses.warnings.iterator().next().toString());
                if (log.isTraceEnabled()) {
                    log.trace("All warnings from Moodle: {}", courses.warnings.toString());
                }
            }

            if (courses.courses == null || courses.courses.isEmpty()) {
                log.warn("No courses found for ids: {} on LMS {}", ids, this.lmsSetup);
                return Collections.emptyList();
            }

            return courses.courses;

        } catch (final Exception e) {
            log.error("LMS Setup: {} Unexpected error while trying to get courses for ids", this.lmsSetup, e);
            return Collections.emptyList();
        }
    }

    private String callMoodleRestAPI(
            final MoodleAPIRestTemplate restTemplate,
            final String function,
            final MultiValueMap<String, String> queryAttributes) {

        return this.moodleRestCall
                .protectedRun(() -> restTemplate.callMoodleAPIFunction(
                        function,
                        queryAttributes))
                .getOrThrow();

    }

    private Predicate<CourseQuizShort> getQuizFilter() {
        final long now = Utils.getSecondsNow();
        return quiz -> {
            if (quiz.time_close == null || quiz.time_close == 0 || quiz.time_close > now) {
                return true;
            }

            if (log.isDebugEnabled()) {
                log.debug("LMS Setup: {} remove quiz {} end_time {} now {}",
                        this.lmsSetup,
                        quiz.name,
                        quiz.time_close,
                        now);
            }
            return false;
        };
    }

    private Predicate<CourseDataShort> getCourseFilter() {
        final long now = Utils.getSecondsNow();
        return course -> {
            if (course.start_date != null && course.start_date < this.fromCutTime) {
                return false;
            }

            if (course.end_date == null || course.end_date == 0 || course.end_date > now) {
                return true;
            }

            if (log.isDebugEnabled()) {
                log.info("LMS Setup: {} remove course {} end_time {} now {}",
                        this.lmsSetup,
                        course.short_name,
                        course.end_date,
                        now);
            }
            return false;
        };
    }

    private void syncCache() {
        if (!this.cachedCourseData.isEmpty()) {

            final Set<String> oldData = this.cachedCourseData
                    .keySet()
                    .stream()
                    .filter(id -> !this.newIds.contains(id))
                    .collect(Collectors.toSet());

            synchronized (this.cachedCourseData) {
                oldData.stream().forEach(this.cachedCourseData::remove);
            }
        }
        this.newIds.clear();
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

    /** Maps the Moodle course API course data */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CourseDataShort {
        final String id;
        final String short_name;
        final String idnumber;
        final Long start_date; // unix-time seconds UTC
        final Long end_date; // unix-time seconds UTC
        final Long time_created; // unix-time seconds UTC
        final Collection<CourseQuizShort> quizzes = new ArrayList<>();

        @JsonCreator
        protected CourseDataShort(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "shortname") final String short_name,
                @JsonProperty(value = "idnumber") final String idnumber,
                @JsonProperty(value = "startdate") final Long start_date,
                @JsonProperty(value = "enddate") final Long end_date,
                @JsonProperty(value = "timecreated") final Long time_created) {

            this.id = id;
            this.short_name = short_name;
            this.idnumber = idnumber;
            this.start_date = start_date;
            this.end_date = end_date;
            this.time_created = time_created;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final CourseDataShort other = (CourseDataShort) obj;
            if (this.id == null) {
                if (other.id != null)
                    return false;
            } else if (!this.id.equals(other.id))
                return false;
            return true;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class Courses {
        final Collection<CourseDataShort> courses;
        final Collection<Warning> warnings;

        @JsonCreator
        protected Courses(
                @JsonProperty(value = "courses") final Collection<CourseDataShort> courses,
                @JsonProperty(value = "warnings") final Collection<Warning> warnings) {
            this.courses = courses;
            this.warnings = warnings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CourseQuizData {
        final Collection<CourseQuizShort> quizzes;
        final Collection<Warning> warnings;

        @JsonCreator
        protected CourseQuizData(
                @JsonProperty(value = "quizzes") final Collection<CourseQuizShort> quizzes,
                @JsonProperty(value = "warnings") final Collection<Warning> warnings) {
            this.quizzes = quizzes;
            this.warnings = warnings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CourseQuizShort {
        final String id;
        final String course;
        final String course_module;
        final String name;
        final Long time_open; // unix-time seconds UTC
        final Long time_close; // unix-time seconds UTC

        @JsonCreator
        protected CourseQuizShort(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "course") final String course,
                @JsonProperty(value = "coursemodule") final String course_module,
                @JsonProperty(value = "name") final String name,
                @JsonProperty(value = "timeopen") final Long time_open,
                @JsonProperty(value = "timeclose") final Long time_close) {

            this.id = id;
            this.course = course;
            this.course_module = course_module;
            this.name = name;
            this.time_open = time_open;
            this.time_close = time_close;
        }
    }

}
