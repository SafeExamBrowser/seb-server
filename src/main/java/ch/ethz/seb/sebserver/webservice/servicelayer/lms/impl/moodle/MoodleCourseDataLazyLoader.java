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
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncRunner;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleCourseAccess.CourseData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleCourseAccess.CourseQuiz;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleCourseAccess.CourseQuizData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleCourseAccess.Courses;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory.MoodleAPIRestTemplate;

@Lazy
@Component
@WebServiceProfile
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MoodleCourseDataLazyLoader {

    private static final Logger log = LoggerFactory.getLogger(MoodleCourseDataLazyLoader.class);

    private final JSONMapper jsonMapper;
    private final AsyncRunner asyncRunner;

    private final Set<CourseData> preFilteredCourseIds = new HashSet<>();

    private long lastRunTime = 0;
    private long lastLoadTime = 0;
    private boolean running = false;

    private final long fromCutTime = DateTime.now(DateTimeZone.UTC).minusYears(3).getMillis() / 1000;

    public MoodleCourseDataLazyLoader(
            final JSONMapper jsonMapper,
            final AsyncRunner asyncRunner) {

        this.jsonMapper = jsonMapper;
        this.asyncRunner = asyncRunner;
    }

    public Set<CourseData> getPreFilteredCourseIds() {
        return this.preFilteredCourseIds;
    }

    public long getLastRunTime() {
        return this.lastRunTime;
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isLongRunningTask() {
        return this.lastLoadTime > 30 * Constants.SECOND_IN_MILLIS;
    }

    public Set<CourseData> loadSync(final MoodleAPIRestTemplate restTemplate) {
        if (this.running) {
            throw new IllegalStateException("Is already running asynchronously");
        }

        this.running = true;
        loadAndCache(restTemplate).run();
        this.lastRunTime = Utils.getMillisecondsNow();

        log.info("Loaded {} courses synchronously", this.preFilteredCourseIds.size());

        return this.preFilteredCourseIds;
    }

    public void loadAsync(final MoodleAPIRestTemplate restTemplate) {
        if (this.running) {
            return;
        }
        this.running = true;
        this.asyncRunner.runAsync(loadAndCache(restTemplate));
        this.lastRunTime = Utils.getMillisecondsNow();

    }

    private Runnable loadAndCache(final MoodleAPIRestTemplate restTemplate) {
        return () -> {
            final long startTime = Utils.getMillisecondsNow();

            loadAllQuizzes(restTemplate);

            this.lastLoadTime = Utils.getMillisecondsNow() - startTime;
            this.running = false;

            log.info("Loaded {} courses asynchronously", this.preFilteredCourseIds.size());
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
            final Map<String, CourseData> courseData = new HashMap<>();
            final Collection<CourseData> coursesPage = getCoursesPage(restTemplate, page, 1000);

            if (coursesPage == null || coursesPage.isEmpty()) {
                return false;
            }

            courseData.putAll(coursesPage
                    .stream()
                    .collect(Collectors.toMap(cd -> cd.id, Function.identity())));

            // then get all quizzes of courses and filter
            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
            attributes.put(
                    MoodleCourseAccess.MOODLE_COURSE_API_COURSE_IDS,
                    new ArrayList<>(courseData.keySet()));

            final String quizzesJSON = restTemplate.callMoodleAPIFunction(
                    MoodleCourseAccess.MOODLE_QUIZ_API_FUNCTION_NAME,
                    attributes);

            final CourseQuizData courseQuizData = this.jsonMapper.readValue(
                    quizzesJSON,
                    CourseQuizData.class);

            if (courseQuizData == null || courseQuizData.quizzes == null || courseQuizData.quizzes.isEmpty()) {
                return false;
            }

            if (courseQuizData.quizzes != null) {
                courseQuizData.quizzes
                        .stream()
                        .filter(getQuizFilter())
                        .forEach(quiz -> {
                            final CourseData data = courseData.get(quiz.course);
                            if (data != null) {
                                data.quizzes.add(quiz);
                            }
                        });

                this.preFilteredCourseIds.addAll(
                        courseData.values().stream()
                                .filter(c -> !c.quizzes.isEmpty())
                                .collect(Collectors.toList()));

                return true;
            } else {
                return false;
            }
        } catch (final Exception e) {
            log.error("Unexpected exception while trying to get course data: ", e);
            return false;
        }
    }

    private Collection<CourseData> getCoursesPage(
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

            final String courseKeyPageJSON = restTemplate.callMoodleAPIFunction(
                    MoodleCourseAccess.MOODLE_COURSE_SEARCH_API_FUNCTION_NAME,
                    attributes);

            final CoursePage keysPage = this.jsonMapper.readValue(
                    courseKeyPageJSON,
                    CoursePage.class);

            if (keysPage == null || keysPage.courseKeys == null || keysPage.courseKeys.isEmpty()) {
                log.info("No courses found on page: {}", page);
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

//            log.info("course page with {} courses, after filtering {} left", keysPage.courseKeys, result.size());

            return result;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to get courses page: ", e);
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
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_FIELD_NAME, MoodleCourseAccess.MOODLE_COURSE_API_IDS);
            attributes.add(MoodleCourseAccess.MOODLE_COURSE_API_FIELD_VALUE, joinedIds);
            final String coursePageJSON = restTemplate.callMoodleAPIFunction(
                    MoodleCourseAccess.MOODLE_COURSE_BY_FIELD_API_FUNCTION_NAME,
                    attributes);

            return this.jsonMapper.<Courses> readValue(
                    coursePageJSON,
                    Courses.class).courses;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to get courses for ids", e);
            return Collections.emptyList();
        }
    }

    private Predicate<CourseQuiz> getQuizFilter() {
        final long now = Utils.getSecondsNow();
        return quiz -> {
            if (quiz.time_close == null || quiz.time_close == 0 || quiz.time_close > now) {
                return true;
            }

            log.info("remove quiz {} end_time {} now {}", quiz.name, quiz.time_close, now);
            return false;
        };
    }

    private Predicate<CourseData> getCourseFilter() {
        final long now = Utils.getSecondsNow();
        return course -> {
            if (course.start_date < this.fromCutTime) {
                return false;
            }

            if (course.end_date == null || course.end_date == 0 || course.end_date > now) {
                return true;
            }

            log.info("remove course {} end_time {} now {}", course.short_name, course.end_date, now);
            return false;
        };
    }

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

//    @JsonIgnoreProperties(ignoreUnknown = true)
//    static final class CourseKeys {
//        final Collection<CourseDataKey> courses;
//
//        @JsonCreator
//        protected CourseKeys(
//                @JsonProperty(value = "courses") final Collection<CourseDataKey> courses) {
//            this.courses = courses;
//        }
//    }

//    /** Maps the Moodle course API course data */
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    static final class CourseDataKey {
//        final String id;
//        final String short_name;
//        final Long start_date; // unix-time seconds UTC
//        final Long end_date; // unix-time seconds UTC
//        final Long time_created; // unix-time seconds UTC
//        final Collection<CourseQuizKey> quizzes = new ArrayList<>();
//
//        @JsonCreator
//        protected CourseDataKey(
//                @JsonProperty(value = "id") final String id,
//                @JsonProperty(value = "shortname") final String short_name,
//                @JsonProperty(value = "startdate") final Long start_date,
//                @JsonProperty(value = "enddate") final Long end_date,
//                @JsonProperty(value = "timecreated") final Long time_created) {
//
//            this.id = id;
//            this.short_name = short_name;
//            this.start_date = start_date;
//            this.end_date = end_date;
//            this.time_created = time_created;
//        }
//
//    }

//    @JsonIgnoreProperties(ignoreUnknown = true)
//    static final class CourseQuizKeys {
//        final Collection<CourseQuizKey> quizzes;
//
//        @JsonCreator
//        protected CourseQuizKeys(
//                @JsonProperty(value = "quizzes") final Collection<CourseQuizKey> quizzes) {
//            this.quizzes = quizzes;
//        }
//    }
//
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    static final class CourseQuizKey {
//        final String id;
//        final String course;
//        final String name;
//        final Long time_open; // unix-time seconds UTC
//        final Long time_close; // unix-time seconds UTC
//
//        @JsonCreator
//        protected CourseQuizKey(
//                @JsonProperty(value = "id") final String id,
//                @JsonProperty(value = "course") final String course,
//                @JsonProperty(value = "name") final String name,
//                @JsonProperty(value = "timeopen") final Long time_open,
//                @JsonProperty(value = "timeclose") final Long time_close) {
//
//            this.id = id;
//            this.course = course;
//            this.name = name;
//            this.time_open = time_open;
//            this.time_close = time_close;
//        }
//    }

}
