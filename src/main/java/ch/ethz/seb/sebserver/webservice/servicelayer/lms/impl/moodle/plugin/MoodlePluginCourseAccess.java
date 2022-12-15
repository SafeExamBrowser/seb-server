/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.CourseAccessAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.AbstractCachedCourseAccess;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate.Warning;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory;

public class MoodlePluginCourseAccess extends AbstractCachedCourseAccess implements CourseAccessAPI {

    private static final Logger log = LoggerFactory.getLogger(MoodlePluginCourseAccess.class);

    static final String COURSES_API_FUNCTION_NAME = "local_sebserver_get_courses";
    static final String QUIZZES_BY_COURSES_API_FUNCTION_NAME = "local_sebserver_get_quizzes_by_courses";
    static final String USERS_API_FUNCTION_NAME = "local_sebserver_get_users";

    static final String CRITERIA_FROM_DATE = "from_date";
    static final String CRITERIA_TO_DATE = "to_date";
    static final String CRITERIA_LIMIT_FROM = "limitfrom";
    static final String CRITERIA_LIMIT_NUM = "limitnum";

    private final JSONMapper jsonMapper;
    private final MoodleRestTemplateFactory moodleRestTemplateFactory;

    private MoodleAPIRestTemplate restTemplate;

    public MoodlePluginCourseAccess(
            final JSONMapper jsonMapper,
            final MoodleRestTemplateFactory moodleRestTemplateFactory,
            final CacheManager cacheManager) {
        super(cacheManager);
        this.jsonMapper = jsonMapper;
        this.moodleRestTemplateFactory = moodleRestTemplateFactory;
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
            return LmsSetupTestResult.ofTokenRequestError(LmsType.MOODLE_PLUGIN, message);
        }

        final MoodleAPIRestTemplate restTemplate = restTemplateRequest.get();

//        try {
//            restTemplate.testAPIConnection(
//                    COURSES_API_FUNCTION_NAME,
//                    QUIZZES_BY_COURSES_API_FUNCTION_NAME,
//                    USERS_API_FUNCTION_NAME);
//        } catch (final RuntimeException e) {
//            log.error("Failed to access Moodle course API: ", e);
//            return LmsSetupTestResult.ofQuizAccessAPIError(LmsType.MOODLE_PLUGIN, e.getMessage());
//        }

        return LmsSetupTestResult.ofOkay(LmsType.MOODLE_PLUGIN);
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        System.out.println("***************** filterMap: " + filterMap);
        // TODO Auto-generated method stub
        return Result.of(Collections.emptyList());
    }

    @Override
    public Result<Collection<QuizData>> getQuizzes(final Set<String> ids) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void fetchQuizzes(final FilterMap filterMap, final AsyncQuizFetchBuffer asyncQuizFetchBuffer) {
        // TODO Auto-generated method stub

    }

    @Override
    public Result<QuizData> getQuiz(final String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getExamineeName(final String examineeUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Chapters> getCourseChapters(final String courseId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Long getLmsSetupId() {
        // TODO Auto-generated method stub
        return null;
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

    // ---- Mapping Classes ---

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

    /** Maps the Moodle course API course data */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class CourseData {
        final String id;
        final String short_name;
        final String idnumber;
        final String full_name;
        final String display_name;
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
                @JsonProperty(value = "startdate") final Long start_date,
                @JsonProperty(value = "enddate") final Long end_date,
                @JsonProperty(value = "timecreated") final Long time_created) {

            this.id = id;
            this.short_name = short_name;
            this.idnumber = idnumber;
            this.full_name = full_name;
            this.display_name = display_name;
            this.start_date = start_date;
            this.end_date = end_date;
            this.time_created = time_created;
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

        @JsonCreator
        protected CourseQuiz(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "course") final String course,
                @JsonProperty(value = "coursemodule") final String course_module,
                @JsonProperty(value = "name") final String name,
                @JsonProperty(value = "intro") final String intro,
                @JsonProperty(value = "timeopen") final Long time_open,
                @JsonProperty(value = "timeclose") final Long time_close) {

            this.id = id;
            this.course = course;
            this.course_module = course_module;
            this.name = name;
            this.intro = intro;
            this.time_open = time_open;
            this.time_close = time_close;
        }
    }

}
