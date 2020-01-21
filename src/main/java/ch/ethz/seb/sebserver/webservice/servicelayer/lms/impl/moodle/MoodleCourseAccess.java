/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.CourseAccess;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory.MoodleAPIRestTemplate;

/** Implements the LmsAPITemplate for Open edX LMS Course API access.
 *
 * See also: https://docs.moodle.org/dev/Web_service_API_functions */
public class MoodleCourseAccess extends CourseAccess {

    private static final Logger log = LoggerFactory.getLogger(MoodleCourseAccess.class);

    private static final String MOOLDE_QUIZ_START_URL_PATH = "/mod/quiz/view.php?id=";
    private static final String MOODLE_COURSE_API_FUNCTION_NAME = "core_course_get_courses";
    private static final String MOODLE_QUIZ_API_FUNCTION_NAME = "mod_quiz_get_quizzes_by_courses";

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

    LmsSetupTestResult initAPIAccess() {

        final LmsSetupTestResult attributesCheck = this.moodleRestTemplateFactory.test();
        if (!attributesCheck.isOk()) {
            return attributesCheck;
        }

        final Result<MoodleAPIRestTemplate> restTemplateRequest = getRestTemplate();
        if (restTemplateRequest.hasError()) {
            final String message = "Failed to gain access token from Moodle Rest API:\n tried token endpoints: " +
                    this.moodleRestTemplateFactory.knownTokenAccessPaths;
            log.error(message, restTemplateRequest.getError().getMessage());
            return LmsSetupTestResult.ofTokenRequestError(message);
        }

        final MoodleAPIRestTemplate restTemplate = restTemplateRequest.get();

        try {
            restTemplate.testAPIConnection(
                    MOODLE_COURSE_API_FUNCTION_NAME,
                    MOODLE_QUIZ_API_FUNCTION_NAME);
        } catch (final RuntimeException e) {
            log.error("Failed to access Open edX course API: ", e);
            return LmsSetupTestResult.ofQuizAccessAPIError(e.getMessage());
        }

        return LmsSetupTestResult.ofOkay();
    }

    @Override
    protected Supplier<List<QuizData>> allQuizzesSupplier() {
        return () -> {
            return getRestTemplate()
                    .map(this::collectAllQuizzes)
                    .getOrThrow();
        };
    }

    private ArrayList<QuizData> collectAllQuizzes(final MoodleAPIRestTemplate restTemplate) {
        final String urlPrefix = this.lmsSetup.lmsApiUrl + MOOLDE_QUIZ_START_URL_PATH;
        return collectAllCourses(
                restTemplate)
                        .stream()
                        .reduce(
                                new ArrayList<QuizData>(),
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

    private List<CourseData> collectAllCourses(final MoodleAPIRestTemplate restTemplate) {

        try {

            // first get courses form Moodle...
            final String coursesJSON = restTemplate.callMoodleAPIFunction(MOODLE_COURSE_API_FUNCTION_NAME);
            final Map<String, CourseData> courseData = this.jsonMapper.<Collection<CourseData>> readValue(
                    coursesJSON,
                    new TypeReference<Collection<CourseData>>() {
                    })
                    .stream()
                    .collect(Collectors.toMap(d -> d.id, Function.identity()));

            // then get all quizzes of courses and filter
            final LinkedMultiValueMap<String, String> attributes = new LinkedMultiValueMap<>();
            attributes.put("courseids", new ArrayList<>(courseData.keySet()));

            final String quizzesJSON = restTemplate.callMoodleAPIFunction(
                    MOODLE_QUIZ_API_FUNCTION_NAME,
                    attributes);

            final CourseQuizData courseQuizData = this.jsonMapper.readValue(
                    quizzesJSON,
                    CourseQuizData.class);

            courseQuizData.quizzes
                    .stream()
                    .forEach(quiz -> {
                        final CourseData course = courseData.get(quiz.course);
                        if (course != null) {
                            course.quizzes.add(quiz);
                        }
                    });

            return courseData.values()
                    .stream()
                    .filter(c -> !c.quizzes.isEmpty())
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected exception while trying to get course data: ", e);
        }
    }

    private static List<QuizData> quizDataOf(
            final LmsSetup lmsSetup,
            final CourseData courseData,
            final String uriPrefix) {

        final Map<String, String> additionalAttrs = new HashMap<>();
        additionalAttrs.clear();
        additionalAttrs.put("timecreated", String.valueOf(courseData.timecreated));
        additionalAttrs.put("course_shortname", courseData.shortname);
        additionalAttrs.put("course_fullname", courseData.fullname);
        additionalAttrs.put("course_displayname", courseData.displayname);
        additionalAttrs.put("course_summary", courseData.summary);

        return courseData.quizzes
                .stream()
                .map(courseQuizData -> {
                    final String startURI = uriPrefix + courseData.id;
                    additionalAttrs.put("coursemodule", courseQuizData.coursemodule);
                    additionalAttrs.put("timelimit", String.valueOf(courseQuizData.timelimit));
                    return new QuizData(
                            courseQuizData.id,
                            lmsSetup.getInstitutionId(),
                            lmsSetup.id,
                            lmsSetup.getLmsType(),
                            courseQuizData.name,
                            courseQuizData.intro,
                            Utils.toDateTimeUTCUnix(courseData.startdate),
                            Utils.toDateTimeUTCUnix(courseData.enddate),
                            startURI,
                            additionalAttrs);
                })
                .collect(Collectors.toList());

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

    /** Maps the Moodle course API course data */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CourseData {
        final String id;
        final String shortname;
        final String fullname;
        final String displayname;
        final String summary;
        final Long startdate; // unixtime milliseconds UTC
        final Long enddate; // unixtime milliseconds UTC
        final Long timecreated; // unixtime milliseconds UTC
        final Collection<CourseQuiz> quizzes = new ArrayList<>();

        @JsonCreator
        protected CourseData(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "shortname") final String shortname,
                @JsonProperty(value = "fullname") final String fullname,
                @JsonProperty(value = "displayname") final String displayname,
                @JsonProperty(value = "summary") final String summary,
                @JsonProperty(value = "startdate") final Long startdate,
                @JsonProperty(value = "enddate") final Long enddate,
                @JsonProperty(value = "timecreated") final Long timecreated) {

            this.id = id;
            this.shortname = shortname;
            this.fullname = fullname;
            this.displayname = displayname;
            this.summary = summary;
            this.startdate = startdate;
            this.enddate = enddate;
            this.timecreated = timecreated;
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
        final String coursemodule;
        final String name;
        final String intro; // HTML
        final Long timelimit; // unixtime milliseconds UTC

        @JsonCreator
        protected CourseQuiz(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "course") final String course,
                @JsonProperty(value = "coursemodule") final String coursemodule,
                @JsonProperty(value = "name") final String name,
                @JsonProperty(value = "intro") final String intro,
                @JsonProperty(value = "timelimit") final Long timelimit) {

            this.id = id;
            this.course = course;
            this.coursemodule = coursemodule;
            this.name = name;
            this.intro = intro;
            this.timelimit = timelimit;
        }

    }

}
