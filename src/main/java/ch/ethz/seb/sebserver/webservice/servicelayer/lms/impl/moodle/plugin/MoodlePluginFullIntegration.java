/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import java.util.*;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService.IntegrationData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService.ExamData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleResponseException;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class MoodlePluginFullIntegration implements FullLmsIntegrationAPI {

    private static final Logger log = LoggerFactory.getLogger(MoodlePluginFullIntegration.class);

    public static final String FUNCTION_NAME_SEBSERVER_CONNECTION = "quizaccess_sebserver_connection";
    private static final String FUNCTION_NAME_SEBSERVER_CONNECTION_DELETE = "quizaccess_sebserver_connection_delete";
    private static final String FUNCTION_NAME_SET_EXAM_DATA = "quizaccess_sebserver_set_exam_data";
    private static final String ATTRIBUTE_CONNECTION = "connection";
    private static final String ATTRIBUTE_EXAM_DATA = "data";

    private static final String UPLOAD_ENDPOINT = "/mod/quiz/accessrule/sebserver/uploadconfig.php";

    private final JSONMapper jsonMapper;
    private final MoodleRestTemplateFactory restTemplateFactory;

    private final boolean prependShortCourseName;

    public MoodlePluginFullIntegration(
            final JSONMapper jsonMapper,
            final MoodleRestTemplateFactory restTemplateFactory,
            final Environment environment) {

        this.jsonMapper = jsonMapper;
        this.restTemplateFactory = restTemplateFactory;

        this.prependShortCourseName = BooleanUtils.toBoolean(environment.getProperty(
                "sebserver.webservice.lms.moodle.prependShortCourseName",
                Constants.TRUE_STRING));
    }

    @Override
    public LmsSetupTestResult testFullIntegrationAPI() {
        final LmsSetupTestResult attributesCheck = this.restTemplateFactory.test();
        if (!attributesCheck.isOk()) {
            return attributesCheck;
        }

        final Result<MoodleAPIRestTemplate> restTemplateRequest = getRestTemplate();
        if (restTemplateRequest.hasError()) {
            final String message = "Failed to gain access token from Moodle Rest API:\n tried token endpoints: " +
                    this.restTemplateFactory.getKnownTokenAccessPaths();
            log.error(message + " cause: {}", restTemplateRequest.getError().getMessage());
            return LmsSetupTestResult.ofTokenRequestError(LmsSetup.LmsType.MOODLE_PLUGIN, message);
        }

        try {
            final MoodleAPIRestTemplate restTemplate = restTemplateRequest.get();
            restTemplate.testAPIConnection(
                    FUNCTION_NAME_SEBSERVER_CONNECTION,
                    FUNCTION_NAME_SEBSERVER_CONNECTION_DELETE,
                    FUNCTION_NAME_SET_EXAM_DATA);

        } catch (final RuntimeException e) {
            return LmsSetupTestResult.ofQuizAccessAPIError(LmsSetup.LmsType.MOODLE_PLUGIN, e.getMessage());
        }

        return LmsSetupTestResult.ofOkay(LmsSetup.LmsType.MOODLE_PLUGIN);
    }

    @Override
    public Result<IntegrationData> applyConnectionDetails(final IntegrationData data) {
        return Result.tryCatch(() -> {
            // validation
            if (StringUtils.isBlank( data.id)) {
                throw new APIMessage.FieldValidationException("lmsFullIntegration:id", "id is mandatory");
            }
            if (StringUtils.isBlank( data.url)) {
                throw new APIMessage.FieldValidationException("lmsFullIntegration:url", "url is mandatory");
            }
            if (StringUtils.isBlank( data.access_token)) {
                throw new APIMessage.FieldValidationException("lmsFullIntegration:access_token", "access_token is mandatory");
            }

            // apply
            final LmsSetup lmsSetup = this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup();
            final String connectionJSON = jsonMapper.writeValueAsString(data);
            final MoodleAPIRestTemplate rest = getRestTemplate().getOrThrow();

            if (log.isDebugEnabled()) {
                log.debug("Try to connect to Moodle Plugin 2.0 with: {}", connectionJSON);
            }

            final MultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();
            queryAttributes.add(ATTRIBUTE_CONNECTION, connectionJSON);

            final String response = rest.postToMoodleAPIFunction(
                    FUNCTION_NAME_SEBSERVER_CONNECTION,
                    queryAttributes,
                    null);

            if (response != null && response.startsWith("{\"exception\":")) {
                // Seems there was an error response from Moodle side.
                // How do we know now what is active on Moodle side?
                // Shall we mark the connection as invalid here?

                log.warn(
                        "Failed to apply SEB Server connection details to Moodle for full integration. Moodle error {}, lmsSetup: {} data: {}",
                        response,
                        lmsSetup.name,
                        data
                );

                throw new MoodleResponseException("Failed to apply SEB Server connection: " + lmsSetup, response);
            }

            try {
                final MoodleUtils.FullConnectionApplyResponse fullConnectionApplyResponse = jsonMapper.readValue(
                        response,
                        MoodleUtils.FullConnectionApplyResponse.class);

                if (!fullConnectionApplyResponse.success && !fullConnectionApplyResponse.warnings.isEmpty()) {
                    fullConnectionApplyResponse.warnings.stream()
                            .filter(w -> Objects.equals(w.warningcode, "connectiondoesntmatch"))
                            .findFirst()
                            .ifPresent(w -> {
                                throw new MoodleResponseException("Failed to apply SEB Server connection due to connection mismatch\n There seems to be another SEB Server already connected to this LMS instance", response);
                            });
                }

                if (log.isDebugEnabled()) {
                    log.debug("Got warnings from Moodle: {}", response);
                }
            } catch (final MoodleResponseException mre) {
                throw mre;
            } catch (final Exception e) {
                log.warn("Failed to parse Moodle warnings. Error: {}", e.getMessage());
                throw e;
            }

            if (log.isDebugEnabled()) {
                log.debug("Successfully applied SEB Server connection for Moodle. Connection data: {} LMS Setup: {}", data, lmsSetup);
            }

            return data;
        });
    }

    @Override
    public Result<ExamData> applyExamData(final ExamData examData) {
        return Result.tryCatch(() -> {
            // validation
            if (StringUtils.isBlank( examData.id)) {
                throw new APIMessage.FieldValidationException("ExamData:id", "id is mandatory");
            }
            if (StringUtils.isBlank( examData.course_id)) {
                throw new APIMessage.FieldValidationException("ExamData:course_id", "course_id is mandatory");
            }
            if (StringUtils.isBlank( examData.quiz_id)) {
                throw new APIMessage.FieldValidationException("ExamData:quiz_id", "quiz_id is mandatory");
            }

            final LmsSetup lmsSetup = this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup();
            final MoodleAPIRestTemplate rest = getRestTemplate().getOrThrow();

            final Map<String, Map<String, String>> attributes = new HashMap<>();
            final Map<String, String> data_mapping = new LinkedHashMap<>();
            attributes.put(ATTRIBUTE_EXAM_DATA, data_mapping);

            // data[quizid]= int
            // data[addordelete]= int
            // data[templateid]= int
            // data[showquitlink]= int
            // data[quitsecret]= string
            data_mapping.put("quizid", examData.quiz_id);
            if (BooleanUtils.isTrue(examData.exam_created)) {
                data_mapping.put("addordelete", "1");
                data_mapping.put("templateid", examData.template_id);
                data_mapping.put("showquitlink", BooleanUtils.isTrue(examData.show_quit_link) ? "1" : "0");
                data_mapping.put("quitsecret", examData.quit_password);
            } else {
                data_mapping.put("addordelete", "0");
            }

            final String response = rest.postToMoodleAPIFunction(
                    FUNCTION_NAME_SET_EXAM_DATA,
                    null,
                    attributes);

            if (response != null && (response.startsWith("{\"exception\":") || response.startsWith("0"))) {
                log.warn("Failed to apply Exam data to moodle: {}", examData);
            }

            if (response != null && (response.startsWith("{\"warnings\":"))) {
                log.info("Moodle warnings in response: {}", response);
            }

            return examData;
        });
    }

    @Override
    public Result<Exam> applyConnectionConfiguration(final Exam exam, final byte[] configData) {
        return Result.tryCatch(() -> {

            final String quizId = MoodleUtils.getQuizId(exam.externalId);
            final String fileName = getConnectionConfigFileName(exam);


            final MultiValueMap<String, Object> multiPartAttributes = new LinkedMultiValueMap<>();
            multiPartAttributes.add("quizid", quizId);
            multiPartAttributes.add("name", fileName);
            multiPartAttributes.add("filename", fileName);
            final ByteArrayResource contentsAsResource = new ByteArrayResource(configData) {
                @Override
                public String getFilename() {
                    return fileName; // Filename has to be returned in order to be able to post.
                }
            };

            multiPartAttributes.add("file", contentsAsResource);

            final MoodleAPIRestTemplate rest = getRestTemplate().getOrThrow();
            final String response = rest.uploadMultiPart(UPLOAD_ENDPOINT, multiPartAttributes);

            if (response != null && (response.startsWith("{\"exception\":") || response.startsWith("0"))) {
                log.warn("Failed to apply Connection Configuration to LMS for Exam: {}", exam.externalId);
            }

            if (response != null && (response.startsWith("{\"warnings\":"))) {
                log.info("Moodle warnings in response: {}", response);
            }

            return exam;
        });
    }

    private String getConnectionConfigFileName(final Exam exam) {
        return "SEBServerConnectionConfiguration-" + exam.id + ".seb";
    }


    @Override
    public Result<String> deleteConnectionDetails() {
        return Result.tryCatch(() -> {
            // get connection identifier
            final LmsSetup lmsSetup = this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup();
            final String connectionId = lmsSetup.getConnectionId();
            if (StringUtils.isBlank(connectionId)) {
                throw new RuntimeException("LMS Setup still has no SEB Server connection identifier: " + lmsSetup);
            }

            final MoodleAPIRestTemplate rest = getRestTemplate().getOrThrow();
            final MultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();
            queryAttributes.set("id", connectionId);
            final String response = rest.callMoodleAPIFunction(
                    FUNCTION_NAME_SEBSERVER_CONNECTION_DELETE,
                    null,
                    queryAttributes);

            if (response != null && response.startsWith("{\"exception\":")) {
                throw new MoodleResponseException("Failed to delete SEB Server connection: " + lmsSetup, response);
            }

            if (response != null && (response.startsWith("{\"warnings\":"))) {
                log.info("Moodle warnings in response: {}", response);
            }

            log.info("Successfully deleted SEB Server connection for Moodle. LMS Setup: {}", lmsSetup);
            return response;
        });
    }

    @Override
    public Result<QuizData> getQuizDataForRemoteImport(final String examData) {
        return Result.tryCatch(() -> {

             log.info("****** Try to parse import exam data sent by Moodle on Exam import: {}", examData);

             final LmsSetup lmsSetup = this.restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup();
             final String urlPrefix = (lmsSetup.lmsApiUrl.endsWith(Constants.URL_PATH_SEPARATOR))
                     ? lmsSetup.lmsApiUrl + MoodlePluginCourseAccess.MOODLE_QUIZ_START_URL_PATH
                     : lmsSetup.lmsApiUrl + Constants.URL_PATH_SEPARATOR + MoodlePluginCourseAccess.MOODLE_QUIZ_START_URL_PATH;
             MoodleUtils.checkJSONFormat(examData);
             final MoodleUtils.CoursesPlugin courses = this.jsonMapper.readValue(
                     examData,
                     MoodleUtils.CoursesPlugin.class);

             final MoodleUtils.CourseData courseData = courses.results.iterator().next();
             final List<QuizData> quizData = MoodleUtils.quizDataOf(
                     lmsSetup,
                     courseData,
                     urlPrefix,
                     prependShortCourseName
             );

             return quizData.get(0);
        });
    }

    private Result<MoodleAPIRestTemplate> getRestTemplate() {

        final Result<MoodleAPIRestTemplate> result = this.restTemplateFactory.getRestTemplate();
        if (!result.hasError()) {
            return result;
        }

        return this.restTemplateFactory.createRestTemplate(MooldePluginLmsAPITemplateFactory.SEB_SERVER_SERVICE_NAME);
    }
}
