/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialServiceImpl;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.CollectingStrategy;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ProctoringSettingsDAOImpl;

class ScreenProctoringAPIBinding {

    private static final Logger log = LoggerFactory.getLogger(ScreenProctoringAPIBinding.class);

    private static final String SEB_SERVER_SCREEN_PROCTORING_USER_PREFIX = "SEBServer_User_";
    private static final String SEB_SERVER_SCREEN_PROCTORING_SEB_ACCESS_PREFIX = "SEBServer_SEB_Access_";

    static interface SPS_API {

        String TOKEN_ENDPOINT = "/oauth/token";
        String TEST_ENDPOINT = "/admin-api/v1/proctoring/group";

        String USER_ENDPOINT = "/admin-api/v1/useraccount";
        String ENTIY_PRIVILEGES_ENDPOINT = USER_ENDPOINT + "/entityprivilege";
        String EXAM_ENDPOINT = "/admin-api/v1/exam";
        String SEB_ACCESS_ENDPOINT = "/admin-api/v1/clientaccess";
        String GROUP_ENDPOINT = "/admin-api/v1/group";
        String SESSION_ENDPOINT = "/admin-api/v1/session";
        String ACTIVE_PATH_SEGMENT = "/active";
        String INACTIVE_PATH_SEGMENT = "/inactive";

        interface SEB_SERVER_EXAM_SETTINGS {
            String ATTR_SPS_USER_UUID = "spsUserUUID";
            String ATTR_SPS_USER_NAME = "spsUserName";
            String ATTR_SPS_USER_PWD = "spsUserPWD";
            String ATTR_SPS_SEB_ACCESS_UUID = "spsSEBAccesUUID";
            String ATTR_SPS_SEB_ACCESS_NAME = "spsSEBAccessName";
            String ATTR_SPS_SEB_ACCESS_PWD = "spsSEBAccessPWD";
            String ATTR_SPS_EXAM_UUID = "spsExamUUID";
        }

        interface PRIVILEGE_FLAGS {
            String READ = "r";
            String MODIFY = "m";
            String WRITE = "w";
        }

        /** The screen proctoring service user-account API attribute names */
        interface USER {
            String ATTR_UUID = "uuid";
            String ATTR_NAME = "name";
            String ATTR_SURNAME = "surname";
            String ATTR_USERNAME = "username";
            String ATTR_PASSWORD = "password";
            String ATTR_LANGUAGE = "language";
            String ATTR_TIMEZONE = "timeZone";
            String ATTR_ROLES = "roles";
        }

        interface ENTITY_PRIVILEGE {
            String ATTR_ENTITY_TYPE = "entityType";
            String ATTR_ENTITY_ID = "entityId";
            String ATTR_USER_UUID = "userUuid";
            String ATTR_PRIVILEGES = "privileges";
        }

        /** The screen proctoring service client-access API attribute names */
        interface SEB_ACCESS {
            String ATTR_UUID = "uuid";
            String ATTR_NAME = "name";
            String ATTR_DESCRIPTION = "description";
            String ATTR_CLIENT_NAME = "clientName";
            String ATTR_CLIENT_SECRET = "clientSecret";
        }

        /** The screen proctoring service group API attribute names */
        interface EXAM {
            String ATTR_UUID = "uuid";
            String ATTR_NAME = "name";
            String ATTR_DESCRIPTION = "description";
            String ATTR_URL = "url";
            String ATTR_TYPE = "type";
            String ATTR_START_TIME = "startTime";
            String ATTR_END_TIME = "endTime";
        }

        /** The screen proctoring service seb-group API attribute names */
        interface GROUP {
            String ATTR_UUID = "uuid";
            String ATTR_EXAM_ID = "examId";
            String ATTR_NAME = "name";
            String ATTR_DESCRIPTION = "description";
        }

        /** The screen proctoring service session API attribute names */
        interface SESSION {
            String ATTR_UUID = "uuid";
            String ATTR_GROUP_ID = "groupId";
            String ATTR_CLIENT_NAME = "clientName";
            String ATTR_CLIENT_IP = "clientIp";
            String ATTR_CLIENT_MACHINE_NAME = "clientMachineName";
            String ATTR_CLIENT_OS_NAME = "clientOsName";
            String ATTR_CLIENT_VERSION = "clientVersion";
        }
    }

    private final UserDAO userDAO;
    private final Cryptor cryptor;
    private final AsyncService asyncService;
    private final JSONMapper jsonMapper;
    private final ProctoringSettingsDAOImpl proctoringSettingsSupplier;
    private final AdditionalAttributesDAO additionalAttributesDAO;

    ScreenProctoringAPIBinding(
            final UserDAO userDAO,
            final Cryptor cryptor,
            final AsyncService asyncService,
            final JSONMapper jsonMapper,
            final ProctoringSettingsDAOImpl proctoringSettingsSupplier,
            final AdditionalAttributesDAO additionalAttributesDAO) {

        this.userDAO = userDAO;
        this.cryptor = cryptor;
        this.asyncService = asyncService;
        this.jsonMapper = jsonMapper;
        this.proctoringSettingsSupplier = proctoringSettingsSupplier;
        this.additionalAttributesDAO = additionalAttributesDAO;
    }

    public Result<Void> testConnection(final ScreenProctoringSettings screenProctoringSettings) {
        return Result.tryCatch(() -> {
            try {

                final ScreenProctoringServiceOAuthTemplate newRestTemplate =
                        new ScreenProctoringServiceOAuthTemplate(this, screenProctoringSettings);

                final ResponseEntity<String> result = newRestTemplate.testServiceConnection();

                if (result.getStatusCode() != HttpStatus.OK) {
                    if (result.getStatusCode().is4xxClientError()) {
                        throw new FieldValidationException(
                                "serverURL",
                                "proctoringSettings:serverURL:url.noAccess");
                    }
                    throw new RuntimeException("Invalid SEB Screen Proctoring Service response: " + result);
                }
            } catch (final FieldValidationException fe) {
                throw fe;
            } catch (final Exception e) {

                log.error("Failed to access SEB Screen Proctoring service at: {}",
                        screenProctoringSettings.spsServiceURL, e);
                throw new FieldValidationException(
                        "serverURL",
                        "proctoringSettings:serverURL:url.noservice");
            }
        });
    }

    /** This is called when an exam goes in running state
     * If the needed resources on SPS side has been already created before, this just reactivates
     * all resources on SPS side.
     * If this is the fist initial run of the given exam and there are no resources on SPS side,
     * this creates all needed resources on SPS side like Exam, SEB Access and ad-hoc User-Account.
     *
     * @param exam The exam
     * @return Result refer to the exam or to an error when happened */
    public Result<Collection<ScreenProctoringGroup>> startScreenProctoring(final Exam exam) {
        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Start screen proctoring and initialize or re-activate all needed objects on SPS side");
            }

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);

            if (existsExamOnSPS(exam, apiTemplate)) {

                log.info("SPS Exam for SEB Server Exam: {} already exists. Try to re-activate", exam.externalId);

                // re-activate all needed entities on SPS side
                activation(exam, SPS_API.USER_ENDPOINT, getSPSUserUUID(exam), true, apiTemplate);
                activation(exam, SPS_API.SEB_ACCESS_ENDPOINT, getSPSSEBAccessUUID(exam), true, apiTemplate);
                activation(exam, SPS_API.EXAM_ENDPOINT, getSPSExamUUID(exam), true, apiTemplate);

                return Collections.emptyList();
            }

            final String spsUserUUID = createExamUser(exam, apiTemplate);
            createSEBAccess(exam, apiTemplate);
            final String examUUID = createExam(exam, apiTemplate);

            createExamReadPrivilege(apiTemplate, spsUserUUID, examUUID);

            return initializeGroups(exam, examUUID, apiTemplate);
        });
    }

    /** This is called when an exam has changed its parameter and needs data update on SPS side
     *
     * @param exam The exam
     * @return Result refer to the exam or to an error when happened */
    public Result<Exam> updateExam(final Exam exam) {
        return Result.tryCatch(() -> {

            final String spsExamUUID = getSPSExamUUID(exam);
            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);

            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromUriString(this.apiTemplate.screenProctoringSettings.spsServiceURL)
                    .path(SPS_API.EXAM_ENDPOINT)
                    .path(spsExamUUID)
                    .queryParam(SPS_API.EXAM.ATTR_NAME, exam.name)
                    .queryParam(SPS_API.EXAM.ATTR_DESCRIPTION, exam.getDescription())
                    .queryParam(SPS_API.EXAM.ATTR_URL, exam.getStartURL())
                    .queryParam(SPS_API.EXAM.ATTR_TYPE, exam.getType().name())
                    .queryParam(SPS_API.EXAM.ATTR_START_TIME, String.valueOf(exam.startTime.getMillis()));

            if (exam.endTime != null) {
                uriBuilder =
                        uriBuilder.queryParam(SPS_API.EXAM.ATTR_END_TIME, String.valueOf(exam.endTime.getMillis()));
            }

            final String uri = uriBuilder.build().toUriString();

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.PUT);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                log.error("Failed to update SPS exam data: {}", exchange);
            }

            return exam;
        });
    }

    /** This is called when an exam finishes and deactivates the Exam, SEB Client Access and the ad-hoc User-Account
     * on Screen Proctoring Service side.
     *
     * @param exam The exam
     * @return Result refer to the exam or to an error when happened */
    public Result<Exam> dispsoseScreenProctoring(final Exam exam) {

        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Dispose active screen proctoring exam, groups and access on SPS for exam: {}", exam);
            }

            activation(exam, SPS_API.EXAM_ENDPOINT, getSPSExamUUID(exam), false, this.apiTemplate);
            activation(exam, SPS_API.SEB_ACCESS_ENDPOINT, getSPSSEBAccessUUID(exam), false, this.apiTemplate);
            activation(exam, SPS_API.USER_ENDPOINT, getSPSUserUUID(exam), false, this.apiTemplate);

            return exam;
        });
    }

    /** This is called on exam delete and deletes the SEB Client Access and the ad-hoc User-Account
     * on Screen Proctoring Service side.
     *
     * @param exam The exam
     * @return Result refer to the exam or to an error when happened */
    public Result<Exam> deleteScreenProctoring(final Exam exam) {

        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Delete screen proctoring exam, groups and access on SPS for exam: {}", exam);
            }

            deletion(exam, SPS_API.SEB_ACCESS_ENDPOINT, getSPSSEBAccessUUID(exam), this.apiTemplate);
            deletion(exam, SPS_API.USER_ENDPOINT, getSPSUserUUID(exam), this.apiTemplate);

            return exam;
        });
    }

    public Result<ScreenProctoringGroup> createGroup(
            final String spsExamUUID,
            final int groupNumber,
            final String description,
            final Exam exam) {

        return Result.tryCatch(() -> {
            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);

            if (apiTemplate.screenProctoringSettings.collectingStrategy != CollectingStrategy.FIX_SIZE) {
                throw new IllegalStateException(
                        "Only FIX_SIZE collecting strategy is supposed to create additional rooms");
            }

            return createGroupOnSPS(
                    apiTemplate.screenProctoringSettings.collectingGroupSize,
                    exam.id,
                    "Proctoring Group " + groupNumber + " : " + exam.getName(),
                    description,
                    spsExamUUID,
                    apiTemplate);
        });
    }

    public Result<ClientCredentials> getSEBClientCredentials(final Long examId) {
        return Result.tryCatch(() -> {

            final String clientName = this.additionalAttributesDAO.getAdditionalAttribute(
                    EntityType.EXAM,
                    examId,
                    SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_SEB_ACCESS_NAME)
                    .getOrThrow()
                    .getValue();
            final String clientSecret = this.additionalAttributesDAO.getAdditionalAttribute(EntityType.EXAM,
                    examId,
                    SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_SEB_ACCESS_PWD)
                    .getOrThrow()
                    .getValue();

            return new ClientCredentials(clientName, clientSecret);
        });
    }

    public String createSEBSession(
            final Long examId,
            final ScreenProctoringGroup localGroup,
            final ClientConnectionRecord clientConnection) {

        final String token = clientConnection.getConnectionToken();
        final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(examId);

        final String uri = UriComponentsBuilder
                .fromUriString(this.apiTemplate.screenProctoringSettings.spsServiceURL)
                .path(SPS_API.SESSION_ENDPOINT)
                .queryParam(SPS_API.SESSION.ATTR_UUID, token)
                .queryParam(SPS_API.SESSION.ATTR_GROUP_ID, localGroup.uuid)

                .queryParam(SPS_API.SESSION.ATTR_CLIENT_IP, clientConnection.getClientAddress())
                .queryParam(SPS_API.SESSION.ATTR_CLIENT_NAME, clientConnection.getExamUserSessionId())
                .queryParam(SPS_API.SESSION.ATTR_CLIENT_MACHINE_NAME, clientConnection.getClientMachineName())
                .queryParam(SPS_API.SESSION.ATTR_CLIENT_OS_NAME, clientConnection.getClientOsName())
                .queryParam(SPS_API.SESSION.ATTR_CLIENT_VERSION, clientConnection.getClientVersion())

                .build()
                .toUriString();

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.POST);
        if (exchange.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException(
                    "Failed to create SPS SEB session for SEB connection: " + token);
        }

        return token;

    }

    private void createExamReadPrivilege(final ScreenProctoringServiceOAuthTemplate apiTemplate,
            final String spsUserUUID, final String examUUID) {
        final String uri = UriComponentsBuilder
                .fromUriString(this.apiTemplate.screenProctoringSettings.spsServiceURL)
                .path(SPS_API.ENTIY_PRIVILEGES_ENDPOINT)
                .queryParam(SPS_API.ENTITY_PRIVILEGE.ATTR_ENTITY_TYPE, EntityType.EXAM.name())
                .queryParam(SPS_API.ENTITY_PRIVILEGE.ATTR_ENTITY_ID, examUUID)
                .queryParam(SPS_API.ENTITY_PRIVILEGE.ATTR_USER_UUID, spsUserUUID)
                .queryParam(SPS_API.ENTITY_PRIVILEGE.ATTR_PRIVILEGES, SPS_API.PRIVILEGE_FLAGS.READ)
                .build()
                .toUriString();

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.POST);
        if (exchange.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to apply entity read privilege to SPS exam: " + examUUID);
        }
    }

    private Collection<ScreenProctoringGroup> initializeGroups(
            final Exam exam,
            final String spsExamUUID,
            final ScreenProctoringServiceOAuthTemplate apiTemplate)
            throws JsonMappingException, JsonProcessingException {

        final List<ScreenProctoringGroup> result = new ArrayList<>();

        switch (apiTemplate.screenProctoringSettings.collectingStrategy) {

            case FIX_SIZE: {
                result.add(createGroupOnSPS(
                        apiTemplate.screenProctoringSettings.collectingGroupSize,
                        exam.id,
                        "Group 1 : " + exam.getName(),
                        "Created by SEB Server",
                        spsExamUUID,
                        apiTemplate));
                break;
            }
            case SEB_GROUP: {
                // TODO
                throw new UnsupportedOperationException("SEB_GROUP based group collection is not supported yet");
            }
            case EXAM:
            default: {
                result.add(createGroupOnSPS(
                        0,
                        exam.id,
                        exam.getName(),
                        "Created by SEB Server",
                        spsExamUUID,
                        apiTemplate));
                break;
            }
        }

        return result;
    }

    private ScreenProctoringGroup createGroupOnSPS(
            final int size,
            final Long examId,
            final String name,
            final String description,
            final String spsExamUUID,
            final ScreenProctoringServiceOAuthTemplate apiTemplate)
            throws JsonMappingException, JsonProcessingException {

        final String uri = UriComponentsBuilder
                .fromUriString(this.apiTemplate.screenProctoringSettings.spsServiceURL)
                .path(SPS_API.GROUP_ENDPOINT)
                .queryParam(SPS_API.GROUP.ATTR_NAME, name)
                .queryParam(SPS_API.GROUP.ATTR_DESCRIPTION, description)
                .queryParam(SPS_API.GROUP.ATTR_EXAM_ID, spsExamUUID)
                .build()
                .toUriString();

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.POST);
        if (exchange.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to create SPS SEB group for exam: " + spsExamUUID);
        }

        final Map<String, String> userAttributes = this.jsonMapper.readValue(
                exchange.getBody(),
                new TypeReference<Map<String, String>>() {
                });

        final String spsGroupUUID = userAttributes.get(SPS_API.GROUP.ATTR_UUID);

        return new ScreenProctoringGroup(null, examId, spsGroupUUID, name, size, exchange.getBody());
    }

    private String createExam(
            final Exam exam,
            final ScreenProctoringServiceOAuthTemplate apiTemplate)
            throws JsonMappingException, JsonProcessingException {

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(this.apiTemplate.screenProctoringSettings.spsServiceURL)
                .path(SPS_API.EXAM_ENDPOINT)
                .queryParam(SPS_API.EXAM.ATTR_NAME, exam.name)
                .queryParam(SPS_API.EXAM.ATTR_DESCRIPTION, exam.getDescription())
                .queryParam(SPS_API.EXAM.ATTR_URL, exam.getStartURL())
                .queryParam(SPS_API.EXAM.ATTR_TYPE, exam.getType().name())
                .queryParam(SPS_API.EXAM.ATTR_START_TIME, String.valueOf(exam.startTime.getMillis()));

        if (exam.endTime != null) {
            uriBuilder =
                    uriBuilder.queryParam(SPS_API.EXAM.ATTR_END_TIME, String.valueOf(exam.endTime.getMillis()));
        }

        final String uri = uriBuilder.build().toUriString();

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.POST);
        if (exchange.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to update SPS exam data: {}", exchange);
        }

        final Map<String, String> userAttributes = this.jsonMapper.readValue(
                exchange.getBody(),
                new TypeReference<Map<String, String>>() {
                });

        final String spsExamUUID = userAttributes.get(SPS_API.EXAM.ATTR_UUID);
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                exam.id,
                SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_EXAM_UUID,
                spsExamUUID);

        return spsExamUUID;
    }

    private String createSEBAccess(
            final Exam exam,
            final ScreenProctoringServiceOAuthTemplate apiTemplate)
            throws JsonMappingException, JsonProcessingException {

        final String name = SEB_SERVER_SCREEN_PROCTORING_SEB_ACCESS_PREFIX + exam.id;
        final String description = "This SEB access was autogenerated by SEB Server";

        final String uri = UriComponentsBuilder
                .fromUriString(this.apiTemplate.screenProctoringSettings.spsServiceURL)
                .path(SPS_API.SEB_ACCESS_ENDPOINT)
                .queryParam(SPS_API.SEB_ACCESS.ATTR_NAME, name)
                .queryParam(SPS_API.SEB_ACCESS.ATTR_DESCRIPTION, description)
                .build()
                .toUriString();

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.POST);
        if (exchange.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to create SPS SEB access for exam: " + exam.externalId);
        }

        final Map<String, String> userAttributes = this.jsonMapper.readValue(
                exchange.getBody(),
                new TypeReference<Map<String, String>>() {
                });

        final String sebAccessUUID = userAttributes.get(SPS_API.SEB_ACCESS.ATTR_UUID);

        // store SEB access data for proctoring along with the exam
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                exam.id,
                SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_SEB_ACCESS_UUID,
                sebAccessUUID);
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                exam.id,
                SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_SEB_ACCESS_NAME,
                userAttributes.get(SPS_API.SEB_ACCESS.ATTR_CLIENT_NAME));
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                exam.id,
                SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_SEB_ACCESS_PWD,
                this.cryptor.encrypt(userAttributes.get(SPS_API.SEB_ACCESS.ATTR_CLIENT_SECRET)).toString());

        return sebAccessUUID;
    }

    private String createExamUser(
            final Exam exam,
            final ScreenProctoringServiceOAuthTemplate apiTemplate)
            throws JsonMappingException, JsonProcessingException {

        final UserInfo examOwner = this.userDAO.byModelId(exam.getOwnerId()).getOrThrow();
        final String userName = SEB_SERVER_SCREEN_PROCTORING_USER_PREFIX + exam.id;
        final CharSequence secret = ClientCredentialServiceImpl.generateClientSecret();

        final String uri = UriComponentsBuilder
                .fromUriString(this.apiTemplate.screenProctoringSettings.spsServiceURL)
                .path(SPS_API.USER_ENDPOINT)
                .queryParam(SPS_API.USER.ATTR_NAME, userName)
                .queryParam(SPS_API.USER.ATTR_SURNAME, userName)
                .queryParam(SPS_API.USER.ATTR_USERNAME, userName)
                .queryParam(SPS_API.USER.ATTR_PASSWORD, secret.toString())
                .queryParam(SPS_API.USER.ATTR_LANGUAGE, examOwner.language.toLanguageTag())
                .queryParam(SPS_API.USER.ATTR_TIMEZONE, examOwner.timeZone.getID())
                .queryParam(SPS_API.USER.ATTR_ROLES, "ADMIN") // TODO adapt role when known
                .build()
                .toUriString();

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.POST);
        if (exchange.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to create SPS user account for exam: " + exam.externalId);
        }

        final Map<String, String> userAttributes = this.jsonMapper.readValue(
                exchange.getBody(),
                new TypeReference<Map<String, String>>() {
                });

        final String userUUID = userAttributes.get(SPS_API.USER.ATTR_UUID);

        // store user data for proctoring along with the exam
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                exam.id,
                SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_USER_UUID,
                userUUID);
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                exam.id,
                SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_USER_NAME,
                userName);
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                exam.id,
                SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_USER_PWD,
                this.cryptor.encrypt(secret).toString());

        return userUUID;
    }

    private boolean existsExamOnSPS(
            final Exam exam,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {

        final String uri = UriComponentsBuilder
                .fromUriString(apiTemplate.screenProctoringSettings.spsServiceURL)
                .path(SPS_API.EXAM_ENDPOINT)
                .path(createSPSExamId(exam))
                .build()
                .toUriString();

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.GET);
        return exchange.getStatusCode() == HttpStatus.OK;
    }

    private void activation(
            final Exam exam,
            final String domainPath,
            final String uuid,
            final boolean activate,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {

        try {

            final String uri = UriComponentsBuilder
                    .fromUriString(this.apiTemplate.screenProctoringSettings.spsServiceURL)
                    .path(domainPath)
                    .path(uuid)
                    .path(activate ? SPS_API.ACTIVE_PATH_SEGMENT : SPS_API.INACTIVE_PATH_SEGMENT)
                    .build()
                    .toUriString();

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.POST);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                log.error("Failed to activate/deactivate on SPS: {} with response: ", uri, exchange);
            }
        } catch (final Exception e) {
            log.error("Failed to activate/deactivate on SPS: {}, {}, {}", domainPath, uuid, activate, e);
        }
    }

    private void deletion(
            final Exam exam,
            final String domainPath,
            final String uuid,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {

        try {

            final String uri = UriComponentsBuilder
                    .fromUriString(this.apiTemplate.screenProctoringSettings.spsServiceURL)
                    .path(domainPath)
                    .path(uuid)
                    .build()
                    .toUriString();

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.DELETE);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                log.error("Failed to delete on SPS: {} with response: ", uri, exchange);
            }
        } catch (final Exception e) {
            log.error("Failed to delete on SPS: {}, {}, {}", domainPath, uuid, e);
        }
    }

    private String createSPSExamId(final Exam exam) {
        return exam.getModelId();
    }

    private String getSPSExamUUID(final Exam exam) {
        final String spsExamUUID = this.additionalAttributesDAO
                .getAdditionalAttribute(
                        EntityType.EXAM,
                        exam.id,
                        SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_EXAM_UUID)
                .getOrThrow()
                .getValue();
        return spsExamUUID;
    }

    private String getSPSUserUUID(final Exam exam) {
        final String spsExamUUID = this.additionalAttributesDAO
                .getAdditionalAttribute(
                        EntityType.EXAM,
                        exam.id,
                        SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_USER_UUID)
                .getOrThrow()
                .getValue();
        return spsExamUUID;
    }

    private String getSPSSEBAccessUUID(final Exam exam) {
        final String spsExamUUID = this.additionalAttributesDAO
                .getAdditionalAttribute(
                        EntityType.EXAM,
                        exam.id,
                        SPS_API.SEB_SERVER_EXAM_SETTINGS.ATTR_SPS_SEB_ACCESS_UUID)
                .getOrThrow()
                .getValue();
        return spsExamUUID;
    }

    private ScreenProctoringServiceOAuthTemplate apiTemplate = null;

    private ScreenProctoringServiceOAuthTemplate getAPITemplate(final Long examId) {
        if (this.apiTemplate == null || !this.apiTemplate.isValid(examId)) {

            log.debug("Create new ScreenProctoringServiceOAuthTemplate for exam: {}", examId);

            final ScreenProctoringSettings settings = this.proctoringSettingsSupplier
                    .getScreenProctoringSettings(new EntityKey(examId, EntityType.EXAM))
                    .getOrThrow();

            this.testConnection(settings).getOrThrow();

            this.apiTemplate = new ScreenProctoringServiceOAuthTemplate(this, settings);
        }

        return this.apiTemplate;
    }

    final static class ScreenProctoringServiceOAuthTemplate {

        private static final String GRANT_TYPE = "password";
        private static final List<String> SCOPES = Collections.unmodifiableList(
                Arrays.asList("read", "write"));

        private final ScreenProctoringSettings screenProctoringSettings;
        private final CircuitBreaker<ResponseEntity<String>> circuitBreaker;

        private final ResourceOwnerPasswordResourceDetails resource;
        private final ClientCredentials clientCredentials;
        private final ClientCredentials userCredentials;
        private final OAuth2RestTemplate restTemplate;

        ScreenProctoringServiceOAuthTemplate(
                final ScreenProctoringAPIBinding sebScreenProctoringService,
                final ScreenProctoringSettings screenProctoringSettings) {

            this.screenProctoringSettings = screenProctoringSettings;
            this.circuitBreaker = sebScreenProctoringService.asyncService.createCircuitBreaker(
                    2,
                    10 * Constants.SECOND_IN_MILLIS,
                    10 * Constants.SECOND_IN_MILLIS);

            this.clientCredentials = new ClientCredentials(
                    this.screenProctoringSettings.spsAPIKey,
                    this.screenProctoringSettings.spsAPISecret);

            CharSequence decryptedSecret = sebScreenProctoringService.cryptor
                    .decrypt(this.clientCredentials.secret)
                    .getOrThrow();

            this.resource = new ResourceOwnerPasswordResourceDetails();
            this.resource.setAccessTokenUri(this.screenProctoringSettings.spsServiceURL + SPS_API.TOKEN_ENDPOINT);
            this.resource.setClientId(this.clientCredentials.clientIdAsString());
            this.resource.setClientSecret(decryptedSecret.toString());
            this.resource.setGrantType(GRANT_TYPE);
            this.resource.setScope(SCOPES);

            this.userCredentials = new ClientCredentials(
                    this.screenProctoringSettings.spsAccountId,
                    this.screenProctoringSettings.spsAccountPassword);

            decryptedSecret = sebScreenProctoringService.cryptor
                    .decrypt(this.userCredentials.secret)
                    .getOrThrow();

            this.resource.setUsername(this.userCredentials.clientIdAsString());
            this.resource.setPassword(decryptedSecret.toString());

            final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setOutputStreaming(false);
            final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(this.resource);
            oAuth2RestTemplate.setRequestFactory(requestFactory);
            this.restTemplate = oAuth2RestTemplate;
        }

        ResponseEntity<String> testServiceConnection() {

            try {
                this.restTemplate.getAccessToken();
            } catch (final Exception e) {
                log.info("Failed to get access token for SEB Screen Proctoring Service: {}", e.getMessage());
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            try {

                final String url = UriComponentsBuilder
                        .fromUriString(this.screenProctoringSettings.spsServiceURL)
                        .path(SPS_API.TEST_ENDPOINT)
                        .queryParam("pageSize", "1")
                        .queryParam("pageNumber", "1")

                        .build()
                        .toUriString();

                return exchange(url, HttpMethod.GET);

            } catch (final Exception e) {
                log.error("Failed to test SEB Screen Proctoring service connection: ", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        boolean isValid(final Long examId) {

            if (this.screenProctoringSettings.examId != examId) {
                return false;
            }

            try {

                final OAuth2AccessToken accessToken = this.restTemplate.getAccessToken();
                if (accessToken == null) {
                    return false;
                }

                final boolean expired = accessToken.isExpired();
                if (expired) {
                    return false;
                }

                final int expiresIn = accessToken.getExpiresIn();
                if (expiresIn < 60) {
                    return false;
                }

                return true;
            } catch (final Exception e) {
                log.error("Failed to verify SEB Screen Proctoring OAuth2RestTemplate status", e);
                return false;
            }
        }

        ResponseEntity<String> exchange(
                final String url,
                final HttpMethod method) {

            return exchange(url, method, null, getHeaders());
        }

        HttpHeaders getHeaders() {
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            return httpHeaders;
        }

        ResponseEntity<String> exchange(
                final String url,
                final HttpMethod method,
                final Object body,
                final HttpHeaders httpHeaders) {

            final Result<ResponseEntity<String>> protectedRunResult = this.circuitBreaker.protectedRun(() -> {
                final HttpEntity<Object> httpEntity = (body != null)
                        ? new HttpEntity<>(body, httpHeaders)
                        : new HttpEntity<>(httpHeaders);

                try {
                    final ResponseEntity<String> result = this.restTemplate.exchange(
                            url,
                            method,
                            httpEntity,
                            String.class);

                    if (result.getStatusCode().value() >= 400) {
                        log.warn("Error response on SEB Screen Proctoring Service API call to {} response status: {}",
                                url,
                                result.getStatusCode());
                    }

                    return result;
                } catch (final RestClientResponseException rce) {
                    return ResponseEntity
                            .status(rce.getRawStatusCode())
                            .body(rce.getResponseBodyAsString());
                }
            });
            return protectedRunResult.getOrThrow();
        }
    }

}
