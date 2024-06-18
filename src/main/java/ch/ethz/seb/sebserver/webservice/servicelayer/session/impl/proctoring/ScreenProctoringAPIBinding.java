/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import java.util.*;

import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.SPSAPIAccessData;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.CollectingStrategy;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ProctoringSettingsDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ScreenProctoringAPIBinding.SPS_API.ExamUpdate;

class ScreenProctoringAPIBinding {

    private static final Logger log = LoggerFactory.getLogger(ScreenProctoringAPIBinding.class);

    private static final String SEB_SERVER_SCREEN_PROCTORING_SEB_ACCESS_PREFIX = "SEBServer_SEB_Access_";

    static interface SPS_API {

        enum SPSUserRole {
            ADMIN,
            PROCTOR
        }

        String TOKEN_ENDPOINT = "/oauth/token";
        String TEST_ENDPOINT = "/admin-api/v1/proctoring/group";

        String USER_ACCOUNT_ENDPOINT = "/admin-api/v1/useraccount/";
        String USERSYNC_SEBSERVER_ENDPOINT = USER_ACCOUNT_ENDPOINT + "usersync/sebserver";
        String ENTITY_PRIVILEGES_ENDPOINT =  USER_ACCOUNT_ENDPOINT + "entityprivilege";
        String EXAM_ENDPOINT = "/admin-api/v1/exam";
        String EXAM_DELETE_REQUEST_ENDPOINT = "/request";
        String SEB_ACCESS_ENDPOINT = "/admin-api/v1/clientaccess";
        String GROUP_ENDPOINT = "/admin-api/v1/group";
        String SESSION_ENDPOINT = "/admin-api/v1/session";
        String ACTIVE_PATH_SEGMENT = "/active";
        String INACTIVE_PATH_SEGMENT = "/inactive";


        interface ENTITY_PRIVILEGE {
            String ATTR_ENTITY_TYPE = "entityType";
            String ATTR_ENTITY_ID = "entityId";
            String ATTR_USER_UUID = "userUuid";
            String ATTR_USERNAME = "username";
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
            String ATTR_ID = "id";
            String ATTR_UUID = "uuid";
            String ATTR_SEB_SERVER_ID = "sebserverId";
            String ATTR_NAME = "name";
            String ATTR_DESCRIPTION = "description";
            String ATTR_URL = "url";
            String ATTR_TYPE = "type";
            String ATTR_START_TIME = "startTime";
            String ATTR_END_TIME = "endTime";
            String ATTR_USER_IDS = "userUUIDs";
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

        @JsonIgnoreProperties(ignoreUnknown = true)
        static final class ExamUpdate {
            @JsonProperty(EXAM.ATTR_NAME)
            final String name;
            @JsonProperty(EXAM.ATTR_DESCRIPTION)
            final String description;
            @JsonProperty(EXAM.ATTR_URL)
            final String url;
            @JsonProperty(EXAM.ATTR_TYPE)
            final String type;
            @JsonProperty(EXAM.ATTR_START_TIME)
            final Long startTime;
            @JsonProperty(EXAM.ATTR_END_TIME)
            final Long endTime;
            @JsonProperty(EXAM.ATTR_USER_IDS)
            final Collection<String> userIds;

            public ExamUpdate(
                    final String name,
                    final String description,
                    final String url,
                    final String type,
                    final Long startTime,
                    final Long endTime,
                    final Collection<String> userIds) {

                this.name = name;
                this.description = description;
                this.url = url;
                this.type = type;
                this.startTime = startTime;
                this.endTime = endTime;
                this.userIds = userIds;
            }
        }
    }

    private final UserDAO userDAO;
    private final Cryptor cryptor;
    private final AsyncService asyncService;
    private final JSONMapper jsonMapper;
    private final ProctoringSettingsDAO proctoringSettingsDAO;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final WebserviceInfo webserviceInfo;

    ScreenProctoringAPIBinding(
            final UserDAO userDAO,
            final Cryptor cryptor,
            final AsyncService asyncService,
            final JSONMapper jsonMapper,
            final ProctoringSettingsDAO proctoringSettingsDAO,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final WebserviceInfo webserviceInfo) {

        this.userDAO = userDAO;
        this.cryptor = cryptor;
        this.asyncService = asyncService;
        this.jsonMapper = jsonMapper;
        this.proctoringSettingsDAO = proctoringSettingsDAO;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.webserviceInfo = webserviceInfo;
    }

    Result<Void> testConnection(final SPSAPIAccessData spsAPIAccessData) {
        return Result.tryCatch(() -> {
            try {

                final ScreenProctoringServiceOAuthTemplate newRestTemplate =
                        new ScreenProctoringServiceOAuthTemplate(this, spsAPIAccessData);

                final ResponseEntity<String> result = newRestTemplate.testServiceConnection();

                if (result.getStatusCode() != HttpStatus.OK) {
                    if (result.getStatusCode().is4xxClientError()) {
                        log.warn(
                                "Failed to establish REST connection to: {}. status: {}",
                                spsAPIAccessData.getSpsServiceURL(), result.getStatusCode());

                        throw new FieldValidationException(
                                "serverURL",
                                "screenProctoringSettings:spsServiceURL:url.noAccess");
                    }
                    throw new RuntimeException("Invalid SEB Screen Proctoring Service response: " + result);
                }
            } catch (final FieldValidationException fe) {
                throw fe;
            } catch (final Exception e) {

                log.error("Failed to access SEB Screen Proctoring service at: {}",
                        spsAPIAccessData.getSpsServiceURL(), e);
                throw new FieldValidationException(
                        "serverURL",
                        "proctoringSettings:serverURL:url.noservice");
            }
        });
    }

    boolean isSPSActive(final Exam exam) {
        try {
            final String active = this.additionalAttributesDAO
                    .getAdditionalAttribute(
                            EntityType.EXAM,
                            exam.id,
                            SPSData.ATTR_SPS_ACTIVE)
                    .getOrThrow()
                    .getValue();
            return BooleanUtils.toBoolean(active);
        } catch (final Exception e) {
            return false;
        }
    }

    SPSData getSPSData(final Long examId) {
        try {

            final String dataEncrypted = this.additionalAttributesDAO
                    .getAdditionalAttribute(
                            EntityType.EXAM,
                            examId,
                            SPSData.ATTR_SPS_ACCESS_DATA)
                    .getOrThrow()
                    .getValue();

            return this.jsonMapper.readValue(
                    this.cryptor.decrypt(dataEncrypted).getOrThrow().toString(),
                    SPSData.class);

        } catch (final Exception e) {
            log.warn("Failed to get local SPSData for exam: {}", examId);
            return null;
        }
    }

    /** This is called when an exam goes in running state
     * If the needed resources on SPS side has been already created before, this just reactivates
     * all resources on SPS side.
     * If this is the fist initial run of the given exam and there are no resources on SPS side,
     * this creates all needed resources on SPS side like Exam, SEB Access and ad-hoc User-Account.
     *
     * @param exam The exam
     * @return Result refer to the exam or to an error when happened */
    Result<Collection<ScreenProctoringGroup>> startScreenProctoring(final Exam exam) {
        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Start screen proctoring and initialize or re-activate all needed objects on SPS side");
            }

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);

            // if we have an exam where SPS was initialized before but deactivated meanwhile
            // reactivate on SPS site and synchronize
            if (exam.additionalAttributes.containsKey(SPSData.ATTR_SPS_ACTIVE)) {

                log.info("SPS Exam for SEB Server Exam: {} already exists. Try to re-activate", exam.externalId);

                final SPSData spsData = this.getSPSData(exam.id);
                // re-activate all needed entities on SPS side
                if (exam.status == Exam.ExamStatus.RUNNING) {
                    activateScreenProctoring(exam).getOrThrow();
                }

                synchronizeUserAccounts(exam);
                return Collections.emptyList();
            }

            // if we have a new Exam but Exam on SPS site for ExamUUID exists, reinitialize the exam and synchronize
            if (existsExamOnSPS(exam)) {
                return reinitializeScreenProctoring(exam);
            }

            // If this is a completely new exam with new SPS binding, initialize it
            return initializeScreenProctoring(exam, apiTemplate);
        });
    }

    boolean existsExamOnSPS(final Exam exam) {
        try {

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);
            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(SPS_API.EXAM_ENDPOINT)
                    .pathSegment(createExamUUID(exam))
                    .build().toUriString();

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.GET);

            if (exchange.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            } else if (exchange.getStatusCode() == HttpStatus.OK) {
                return true;
            } else {
                log.warn("Failed to verify if Exam on SPS already exists: {}", exchange.getBody());
                return false;
            }

        } catch (final Exception e) {
            log.error("Failed to verify if Exam exists already on SPS site: ", e);
            return false;
        }
    }

    void synchronizeUserAccount(final String userUUID) {
        if (UserService.LMS_INTEGRATION_CLIENT_UUID.equals(userUUID)) {
            return;
        }

        try {

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(null);
            // check if user exists on SPS
            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(SPS_API.USER_ACCOUNT_ENDPOINT + userUUID)
                    .build()
                    .toUriString();

            final ResponseEntity<String> exchange = apiTemplate.exchange(
                    uri, HttpMethod.GET, null, apiTemplate.getHeaders());

            if (exchange.getStatusCode() == HttpStatus.OK) {
                log.info("Synchronize SPS user account for SEB Server user account with id: {} ", userUUID);
                this.synchronizeUserAccount(userUUID, apiTemplate);
            }

        } catch (final Exception e) {
            log.error("Failed to synchronize user account with SPS for user: {}", userUUID);
        }
    }

    void synchronizeUserAccounts(final Exam exam) {
        try {
            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);
            final SPSData spsData = this.getSPSData(exam.id);

            exam.supporter.forEach(userUUID -> synchronizeUserAccount(userUUID, apiTemplate));
            if (exam.owner != null) {
                synchronizeUserAccount(exam.owner, apiTemplate);
            }

        } catch (final Exception e) {
            log.error("Failed to synchronize user accounts with SPS for exam: {}", exam);
        }
    }
    void deleteSPSUser(final String userUUID) {
        try {

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(null);

            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(SPS_API.USER_ACCOUNT_ENDPOINT + userUUID)
                    .build()
                    .toUriString();

            final ResponseEntity<String> exchange = apiTemplate.exchange(
                    uri, HttpMethod.DELETE, null, apiTemplate.getHeaders());

            if (exchange.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully deleted User Account on SPS for user: {}", userUUID);
            } else {
                log.error("Failed to delete user account on SPS for user: {} response: {}", userUUID, exchange);
            }

        } catch (final Exception e) {
            log.error("Failed to delete user account on SPS for user: {}", userUUID);
        }
    }

    /** This is called when an exam has changed its parameter and needs data update on SPS side
     *
     * @param exam The exam*/
    void updateExam(final Exam exam) {

        try {
            final SPSData spsData = this.getSPSData(exam.id);
            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);

            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(SPS_API.EXAM_ENDPOINT)
                    .pathSegment(spsData.spsExamUUID)
                    .build()
                    .toUriString();

            final List<String> userIds = new ArrayList<>(exam.supporter);
            if (exam.owner != null) {
                userIds.add(exam.owner);
            }

            final ExamUpdate examUpdate = new ExamUpdate(
                    exam.name,
                    exam.getDescription(),
                    exam.getStartURL(),
                    exam.getType().name(),
                    exam.startTime != null ? exam.startTime.getMillis() : null,
                    exam.endTime != null ? exam.endTime.getMillis() : null,
                    userIds);

            final String jsonExamUpdate = this.jsonMapper.writeValueAsString(examUpdate);

            final ResponseEntity<String> exchange = apiTemplate.exchange(
                    uri,
                    HttpMethod.PUT,
                    jsonExamUpdate,
                    apiTemplate.getHeadersJSONRequest());
            if (exchange.getStatusCode() != HttpStatus.OK) {
                log.error("Failed to update SPS exam data: {}", exchange);
            }

        } catch (final Exception e) {
            log.error("Failed to update exam on SPS service for exam: {}", exam, e);
        }
    }

    /** This is called when an exam finishes and deactivates the Exam, SEB Client Access on Screen Proctoring Service side.
     *
     * @param exam The exam
     * @return Result refer to the exam or to an error when happened */
    Result<Exam> deactivateScreenProctoring(final Exam exam) {

        return Result.tryCatch(() -> {

            if (!this.isSPSActive(exam)) {
                return exam;
            }

            if (log.isDebugEnabled()) {
                log.debug("Deactivate active screen proctoring exam, groups and access on SPS for exam: {}", exam.name);
            }

            final SPSData spsData = this.getSPSData(exam.id);
            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);
            activation(exam, SPS_API.SEB_ACCESS_ENDPOINT, spsData.spsSEBAccessUUID, false, apiTemplate);
            activation(exam, SPS_API.EXAM_ENDPOINT, spsData.spsExamUUID, false, apiTemplate);

            // mark local for successfully dispose on SPS side
            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    exam.id,
                    SPSData.ATTR_SPS_ACTIVE,
                    Constants.FALSE_STRING);

            return exam;
        });
    }

    Result<Exam> activateScreenProctoring(final Exam exam) {

        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Activate screen proctoring exam, groups and access on SPS for exam: {}", exam.name);
            }

            final SPSData spsData = this.getSPSData(exam.id);
            if (spsData == null) {
                return exam;
            }

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);
            activation(exam, SPS_API.SEB_ACCESS_ENDPOINT, spsData.spsSEBAccessUUID, true, apiTemplate);
            activation(exam, SPS_API.EXAM_ENDPOINT, spsData.spsExamUUID, true, apiTemplate);

            // mark local for successfully activated on SPS side
            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    exam.id,
                    SPSData.ATTR_SPS_ACTIVE,
                    Constants.TRUE_STRING);

            return exam;
        });
    }

    private Collection<ScreenProctoringGroup> initializeScreenProctoring(
            final Exam exam,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) throws JsonProcessingException {

        final SPSData spsData = new SPSData();
        log.info(
                "SPS Exam for SEB Server Exam: {} don't exists yet, create necessary structures on SPS",
                exam.externalId);

        synchronizeUserAccounts(exam);
        createSEBAccess(exam, apiTemplate, spsData);
        createExam(exam, apiTemplate, spsData);
        final Collection<ScreenProctoringGroup> initializeGroups = initializeGroups(exam, apiTemplate, spsData);

        // store encrypted spsData
        final String spsDataJSON = this.jsonMapper.writeValueAsString(spsData);
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                exam.id,
                SPSData.ATTR_SPS_ACCESS_DATA,
                this.cryptor.encrypt(spsDataJSON).getOrThrow().toString());

        // mark successfully activated on SPS side
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                exam.id,
                SPSData.ATTR_SPS_ACTIVE,
                Constants.TRUE_STRING);

        return initializeGroups;
    }

    Collection<ScreenProctoringGroup> reinitializeScreenProctoring(final Exam exam) {
        try {

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);

            // get exam from SPS
            final String examUUID = createExamUUID(exam);
            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(SPS_API.EXAM_ENDPOINT)
                    .pathSegment(examUUID)
                    .build().toUriString();
            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.GET);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to get Exam from SPS. local exam uuid: " + examUUID);
            }
            final JsonNode requestJSON = this.jsonMapper.readTree(exchange.getBody());
            final String spsExamId = requestJSON.get(SPS_API.EXAM.ATTR_ID).textValue();


            // check if Exam has SPSData, if not create and if check completeness
            SPSData spsData = this.getSPSData(exam.id);
            if (spsData == null) {
                spsData = new SPSData();
            }
            // create new SEB Account on SPS if needed
            if (spsData.spsSEBAccessUUID == null) {
                createSEBAccess(exam, apiTemplate, spsData);
            }

            spsData.spsExamUUID = examUUID;
            // store encrypted spsData
            final String spsDataJSON = this.jsonMapper.writeValueAsString(spsData);
            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    exam.id,
                    SPSData.ATTR_SPS_ACCESS_DATA,
                    this.cryptor.encrypt(spsDataJSON).getOrThrow().toString());

            // reactivate exam on SPS
            this.activateScreenProctoring(exam);

            // recreate groups on SEB Server if needed
            try {
                final Collection<ScreenProctoringGroup> groups = new ArrayList<>();
                final String groupRequestURI = UriComponentsBuilder
                        .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                        .path(SPS_API.GROUP_ENDPOINT)
                        .queryParam(Page.ATTR_PAGE_SIZE, 100)
                        .queryParam(SPS_API.GROUP.ATTR_EXAM_ID, spsExamId)
                        .build()
                        .toUriString();

                final JsonNode groupsJSON = this.jsonMapper.readTree(exchange.getBody());
                final JsonNode pageContent = groupsJSON.get("content");
                if (pageContent.isArray()) {
                   for (final JsonNode group : pageContent) {
                       groups.add(new ScreenProctoringGroup(
                               null,
                               exam.id,
                               group.get(SPS_API.GROUP.ATTR_UUID).textValue(),
                               group.get(SPS_API.GROUP.ATTR_NAME).textValue(),
                               0,
                               null
                       ));
                   }
                }

                return groups;
            } catch (final Exception e) {
                log.error("Failed to get exam groups from SPS due to reinitialization: ", e);
                return initializeGroups(exam, apiTemplate, spsData);
            }

        } catch (final Exception e) {
            log.error("Failed to re-initialize Screen Proctoring: ", e);
            return Collections.emptyList();
        }

    }

    Result<ScreenProctoringGroup> createGroup(
            final String spsExamUUID,
            final int groupNumber,
            final String description,
            final Exam exam) {

        return Result.tryCatch(() -> {
            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);
            final ScreenProctoringSettings settings = this.proctoringSettingsDAO
                    .getScreenProctoringSettings(new EntityKey(exam.id, EntityType.EXAM))
                    .getOrThrow();

            if (settings.collectingStrategy != CollectingStrategy.FIX_SIZE) {
                throw new IllegalStateException(
                        "Only FIX_SIZE collecting strategy is supposed to create additional rooms");
            }

            return createGroupOnSPS(
                    settings.collectingGroupSize,
                    exam.id,
                    "Proctoring Group " + groupNumber + " : " + exam.getName(),
                    description,
                    spsExamUUID,
                    apiTemplate);
        });
    }

    String createSEBSession(
            final Long examId,
            final ScreenProctoringGroup localGroup,
            final ClientConnectionRecord clientConnection) {

        // TODO check if the Session with token UUID already exists and if true, return the token

        final String token = clientConnection.getConnectionToken();
        final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(examId);
        final String uri = UriComponentsBuilder
                .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                .path(SPS_API.SESSION_ENDPOINT)

                .build()
                .toUriString();

        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(SPS_API.SESSION.ATTR_UUID, token);
        params.add(SPS_API.SESSION.ATTR_GROUP_ID, localGroup.uuid);
        params.add(SPS_API.SESSION.ATTR_CLIENT_IP, clientConnection.getClientAddress());
        params.add(SPS_API.SESSION.ATTR_CLIENT_NAME, Utils.encodeFormURL_UTF_8(clientConnection.getExamUserSessionId()));
        params.add(SPS_API.SESSION.ATTR_CLIENT_MACHINE_NAME, Utils.encodeFormURL_UTF_8(clientConnection.getClientMachineName()));
        params.add(SPS_API.SESSION.ATTR_CLIENT_OS_NAME, Utils.encodeFormURL_UTF_8(clientConnection.getClientOsName()));
        params.add(SPS_API.SESSION.ATTR_CLIENT_VERSION, Utils.encodeFormURL_UTF_8(clientConnection.getClientVersion()));
        final String paramsFormEncoded = Utils.toAppFormUrlEncodedBody(params);

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, paramsFormEncoded);
        if (exchange.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException(
                    "Failed to create SPS SEB session for SEB connection: " + token);
        }

        return token;
    }

    private void synchronizeUserAccount(
            final String userUUID,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {

        if (UserService.LMS_INTEGRATION_CLIENT_UUID.equals(userUUID)) {
            return;
        }

        try {

            final UserInfo userInfo = this.userDAO
                    .byModelId(userUUID)
                    .getOrThrow();
            final SEBServerUser accountInfo = this.userDAO
                    .sebServerUserByUsername(userInfo.username)
                    .getOrThrow();

            final UserMod userMod = getUserModifications(userInfo, accountInfo);
            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(SPS_API.USERSYNC_SEBSERVER_ENDPOINT)
                    .build()
                    .toUriString();

            final String jsonBody = this.jsonMapper.writeValueAsString(userMod);
            final ResponseEntity<String> exchange = apiTemplate.exchange(
                    uri, HttpMethod.POST, jsonBody, apiTemplate.getHeadersJSONRequest());

            if (exchange.getStatusCode() != HttpStatus.OK) {
                log.warn("Failed to synchronize user account on SPS: {}", exchange);
            } else {
                log.info("Successfully synchronize user account on SPS for user: {}", userUUID);
            }

            // sync activity
            final String activityURI = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(SPS_API.USER_ACCOUNT_ENDPOINT)
                    .pathSegment(userUUID)
                    .path(BooleanUtils.isTrue(userInfo.active) ? "/active" : "/inactive")
                    .build()
                    .toUriString();
            final ResponseEntity<String> activityRequest = apiTemplate.exchange(
                    activityURI, HttpMethod.POST, jsonBody, apiTemplate.getHeaders());

            if (activityRequest.getStatusCode() != HttpStatus.OK) {
                final String body = activityRequest.getBody();
                if (body != null && body.contains("Activation argument mismatch")) {
                    return;
                }

                log.warn("Failed to synchronize activity for user account on SPS: {}", activityRequest);
            } else {
                log.info("Successfully synchronize activity for user account on SPS for user: {}", userUUID);
            }

        } catch (final Exception e) {
            log.error("Failed to synchronize user account with SPS for user: {}", userUUID);
        }
    }

    private static UserMod getUserModifications(final UserInfo userInfo, final SEBServerUser accountInfo) {
        final Set<String> spsUserRoles = new HashSet<>();
        spsUserRoles.add(SPS_API.SPSUserRole.PROCTOR.name());
        if (userInfo.roles.contains(UserRole.SEB_SERVER_ADMIN.name()) ||
                userInfo.roles.contains(UserRole.INSTITUTIONAL_ADMIN.name())) {
            spsUserRoles.add(SPS_API.SPSUserRole.ADMIN.name());
        }

        return new UserMod(
                userInfo.uuid,
                -1L,
                userInfo.name,
                userInfo.surname,
                userInfo.username,
                accountInfo.getPassword(),
                accountInfo.getPassword(),
                userInfo.email,
                userInfo.language,
                userInfo.timeZone,
                true,
                true,
                spsUserRoles);
    }

    private Collection<ScreenProctoringGroup> initializeGroups(
            final Exam exam,
            final ScreenProctoringServiceOAuthTemplate apiTemplate,
            final SPSData spsData) {

        try {

            final ScreenProctoringSettings settings = this.proctoringSettingsDAO
                    .getScreenProctoringSettings(new EntityKey(exam.id, EntityType.EXAM))
                    .getOrThrow();
            final List<ScreenProctoringGroup> result = new ArrayList<>();

            switch (settings.collectingStrategy) {

                case FIX_SIZE: {
                    result.add(createGroupOnSPS(
                            settings.collectingGroupSize,
                            exam.id,
                            "Group 1 : " + exam.getName(),
                            "Created by SEB Server",
                            spsData.spsExamUUID,
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
                            spsData.spsExamUUID,
                            apiTemplate));
                    break;
                }
            }

            return result;

        } catch (final Exception e) {
            log.error(
                    "Failed to initialize SPS Groups for screen proctoring. perform rollback. exam: {} error: {}",
                    exam,
                    e.getMessage());
            rollbackOnSPS(exam, spsData, apiTemplate);
            throw new RuntimeException("Failed to apply screen proctoring:", e);
        }
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
                .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                .path(SPS_API.GROUP_ENDPOINT)
                .build()
                .toUriString();
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(SPS_API.GROUP.ATTR_NAME, name);
        params.add(SPS_API.GROUP.ATTR_DESCRIPTION, description);
        params.add(SPS_API.GROUP.ATTR_EXAM_ID, spsExamUUID);
        final String paramsFormEncoded = Utils.toAppFormUrlEncodedBody(params);

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, paramsFormEncoded);
        if (exchange.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to create SPS SEB group for exam: " + spsExamUUID);
        }

        final Map<String, String> groupAttributes = this.jsonMapper.readValue(
                exchange.getBody(),
                new TypeReference<Map<String, String>>() {
                });

        final String spsGroupUUID = groupAttributes.get(SPS_API.GROUP.ATTR_UUID);
        return new ScreenProctoringGroup(null, examId, spsGroupUUID, name, size, exchange.getBody());
    }

    private void createExam(
            final Exam exam,
            final ScreenProctoringServiceOAuthTemplate apiTemplate,
            final SPSData spsData) {

        try {

            final List<String> userIds = new ArrayList<>(exam.supporter);
            if (exam.owner != null) {
                userIds.add(exam.owner);
            }

            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(SPS_API.EXAM_ENDPOINT)
                    .build().toUriString();

            final String uuid = createExamUUID(exam);
            final MultiValueMap<String, String> params = createExamCreationParams(exam, uuid, userIds);
            final String paramsFormEncoded = Utils.toAppFormUrlEncodedBody(params);

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, paramsFormEncoded);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Error response from Screen Proctoring Service: "
                        + exchange.getStatusCodeValue()
                        + " "
                        + exchange.getBody());
            }

            final JsonNode requestJSON = this.jsonMapper.readTree(exchange.getBody());
            final String respondedUUID = requestJSON.get(SPS_API.EXAM.ATTR_UUID).textValue();
            if (!uuid.equals(respondedUUID)) {
                log.warn("Detected Exam ({}) generation UUID mismatch. propagated UUID: {} responded UUID: {}",
                        exam.name,
                        uuid,
                        respondedUUID);
            }
            spsData.spsExamUUID = respondedUUID;

        } catch (final Exception e) {
            log.error(
                    "Failed to create ad-hoc SPS Exam for screen proctoring. perform rollback. exam: {} error: {}",
                    exam,
                    e.getMessage());
            rollbackOnSPS(exam, spsData, apiTemplate);
            throw new RuntimeException("Failed to apply screen proctoring:", e);
        }
    }

    private static MultiValueMap<String, String> createExamCreationParams(
            final Exam exam,
            final String uuid,
            final List<String> userIds) {

        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(SPS_API.EXAM.ATTR_UUID, uuid);
        params.add(SPS_API.EXAM.ATTR_NAME, exam.name);
        params.add(SPS_API.EXAM.ATTR_DESCRIPTION, exam.getDescription());
        params.add(SPS_API.EXAM.ATTR_URL, exam.getStartURL());
        if (!userIds.isEmpty()) {
            params.add(SPS_API.EXAM.ATTR_USER_IDS, StringUtils.join(userIds, Constants.LIST_SEPARATOR));
        }
        params.add(SPS_API.EXAM.ATTR_TYPE, exam.getType().name());
        params.add(SPS_API.EXAM.ATTR_START_TIME, String.valueOf(exam.startTime.getMillis()));

        if (exam.endTime != null) {
            params.add(SPS_API.EXAM.ATTR_END_TIME, String.valueOf(exam.endTime.getMillis()));
        }
        return params;
    }

    private String createExamUUID(final Exam exam) {
        return exam.externalId;
    }

    private void createSEBAccess(
            final Exam exam,
            final ScreenProctoringServiceOAuthTemplate apiTemplate,
            final SPSData spsData) {

        try {
            final String name = SEB_SERVER_SCREEN_PROCTORING_SEB_ACCESS_PREFIX + exam.externalId;
            final String description = "This SEB access was auto-generated by SEB Server";

            // first try to get existing one by name and link it if available
            String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(SPS_API.SEB_ACCESS_ENDPOINT)
                    .queryParam(SPS_API.SEB_ACCESS.ATTR_NAME, name)
                    .build()
                    .toUriString();

            final ResponseEntity<String> getResponse = apiTemplate.exchange(uri, HttpMethod.GET);
            if (getResponse.getStatusCode() == HttpStatus.OK) {
                try {
                    final JsonNode requestJSON = this.jsonMapper.readTree(getResponse.getBody());
                    final JsonNode content = requestJSON.get("content");
                    if (content.isArray()) {
                        final JsonNode sebConnection = content.get(0);
                        spsData.spsSEBAccessUUID = sebConnection.get(SPS_API.SEB_ACCESS.ATTR_UUID).textValue();
                        spsData.spsSEBAccessName = sebConnection.get(SPS_API.SEB_ACCESS.ATTR_CLIENT_NAME).textValue();
                        spsData.spsSEBAccessPWD = sebConnection.get(SPS_API.SEB_ACCESS.ATTR_CLIENT_SECRET).textValue();
                        return;
                    }
                } catch (final Exception e) {
                    log.warn("Failed to extract existing SEB Account from JSON: {}", e.getMessage());
                }
            }

            // otherwise create new one and link it
            uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(SPS_API.SEB_ACCESS_ENDPOINT)
                    .build()
                    .toUriString();

            final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add(SPS_API.SEB_ACCESS.ATTR_NAME, name);
            params.add(SPS_API.SEB_ACCESS.ATTR_DESCRIPTION, description);
            final String paramsFormEncoded = Utils.toAppFormUrlEncodedBody(params);

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, paramsFormEncoded);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to create SPS SEB access for exam: " + exam.externalId);
            }

            // store SEB access data for proctoring along with the exam
            final JsonNode requestJSON = this.jsonMapper.readTree(exchange.getBody());
            spsData.spsSEBAccessUUID = requestJSON.get(SPS_API.SEB_ACCESS.ATTR_UUID).textValue();
            spsData.spsSEBAccessName = requestJSON.get(SPS_API.SEB_ACCESS.ATTR_CLIENT_NAME).textValue();
            spsData.spsSEBAccessPWD = requestJSON.get(SPS_API.SEB_ACCESS.ATTR_CLIENT_SECRET).textValue();

        } catch (final Exception e) {
            log.error(
                    "Failed to create ad-hoc SEB Access for screen proctoring. perform rollback. exam: {} error: {}",
                    exam,
                    e.getMessage());
            rollbackOnSPS(exam, spsData, apiTemplate);
            throw new RuntimeException("Failed to apply screen proctoring:", e);
        }
    }

    private void activation(
            final Exam exam,
            final String domainPath,
            final String uuid,
            final boolean activate,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {

        try {

            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(domainPath)
                    .pathSegment(uuid)
                    .pathSegment(activate ? SPS_API.ACTIVE_PATH_SEGMENT : SPS_API.INACTIVE_PATH_SEGMENT)
                    .build()
                    .toUriString();

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.POST);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                log.error("Failed to activate/deactivate on SPS: {} with response: {}", uri, exchange);
            }
        } catch (final Exception e) {
            log.error("Failed to activate/deactivate on SPS: {}, {}, {}", domainPath, uuid, activate, e);
        }
    }

    private void deletion(
            final String domainPath,
            final String uuid,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {

        try {

            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsAPIAccessData.getSpsServiceURL())
                    .path(domainPath)
                    .pathSegment(uuid)
                    .build()
                    .toUriString();

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.DELETE);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                log.error("Failed to delete on SPS: {} with response: {}", uri, exchange);
            }
        } catch (final Exception e) {
            log.error("Failed to delete on SPS: {}, {}, ", domainPath, uuid, e);
        }
    }

    private void rollbackOnSPS(
            final Exam exam,
            final SPSData spsData,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {

        log.info("Try to rollback SPS binding for exam: {}", exam.externalId);

        if (StringUtils.isNotBlank(spsData.spsExamUUID)) {

            // TODO delete entity privilege

            log.info(
                    "Try to rollback SPS Exam with UUID: {} for exam: {}",
                    spsData.spsExamUUID,
                    exam.externalId);

            deletion(SPS_API.EXAM_ENDPOINT, spsData.spsExamUUID, apiTemplate);

        }

        if (StringUtils.isNotBlank(spsData.spsSEBAccessUUID)) {
            log.info(
                    "Try to rollback SPS SEB Access with UUID: {} for exam: {}",
                    spsData.spsSEBAccessUUID,
                    exam.externalId);

            deletion(SPS_API.SEB_ACCESS_ENDPOINT, spsData.spsSEBAccessUUID, apiTemplate);
        }
    }

    private ScreenProctoringServiceOAuthTemplate apiTemplate = null;

    private ScreenProctoringServiceOAuthTemplate getAPITemplate(final Long examId) {
        if (this.apiTemplate == null || !this.apiTemplate.isValid(examId)) {
            if (examId != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Create new ScreenProctoringServiceOAuthTemplate for exam: {}", examId);
                }

                final ScreenProctoringSettings settings = this.proctoringSettingsDAO
                        .getScreenProctoringSettings(new EntityKey(examId, EntityType.EXAM))
                        .getOrThrow();
                this.testConnection(settings).getOrThrow();
                this.apiTemplate = new ScreenProctoringServiceOAuthTemplate(this, settings);

            } else if (this.webserviceInfo.getScreenProctoringServiceBundle().bundled) {

                if (log.isDebugEnabled()) {
                    log.debug("Create new ScreenProctoringServiceOAuthTemplate for exam: {}", examId);
                }

                final WebserviceInfo.ScreenProctoringServiceBundle bundle = this.webserviceInfo
                        .getScreenProctoringServiceBundle();

                this.testConnection(bundle).getOrThrow();
                this.apiTemplate = new ScreenProctoringServiceOAuthTemplate(this, bundle);


            } else {
                throw new IllegalStateException("No SPS API access information found!");
            }
        }

        return this.apiTemplate;
    }

    final static class ScreenProctoringServiceOAuthTemplate {

        private static final String GRANT_TYPE = "password";
        private static final List<String> SCOPES = Collections.unmodifiableList(
                Arrays.asList("read", "write"));

        private final SPSAPIAccessData spsAPIAccessData;
        private final CircuitBreaker<ResponseEntity<String>> circuitBreaker;
        private final OAuth2RestTemplate restTemplate;

        ScreenProctoringServiceOAuthTemplate(
                final ScreenProctoringAPIBinding sebScreenProctoringService,
                final SPSAPIAccessData spsAPIAccessData) {

            this.spsAPIAccessData = spsAPIAccessData;
            this.circuitBreaker = sebScreenProctoringService.asyncService.createCircuitBreaker(
                    2,
                    10 * Constants.SECOND_IN_MILLIS,
                    10 * Constants.SECOND_IN_MILLIS);

            final ClientCredentials clientCredentials = new ClientCredentials(
                    spsAPIAccessData.getSpsAPIKey(),
                    spsAPIAccessData.getSpsAPISecret());

            CharSequence decryptedSecret = sebScreenProctoringService.cryptor
                    .decrypt(clientCredentials.secret)
                    .getOrThrow();

            final ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
            resource.setAccessTokenUri(spsAPIAccessData.getSpsServiceURL() + SPS_API.TOKEN_ENDPOINT);
            resource.setClientId(clientCredentials.clientIdAsString());
            resource.setClientSecret(decryptedSecret.toString());
            resource.setGrantType(GRANT_TYPE);
            resource.setScope(SCOPES);
            final ClientCredentials userCredentials = new ClientCredentials(
                    spsAPIAccessData.getSpsAccountId(),
                    spsAPIAccessData.getSpsAccountPassword());

            decryptedSecret = sebScreenProctoringService.cryptor
                    .decrypt(userCredentials.secret)
                    .getOrThrow();

            resource.setUsername(userCredentials.clientIdAsString());
            resource.setPassword(decryptedSecret.toString());

            final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setOutputStreaming(false);
            final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resource);
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
                        .fromUriString(this.spsAPIAccessData.getSpsServiceURL())
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

            if (!Objects.equals(this.spsAPIAccessData.getExamId(), examId)) {
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

                return accessToken.getExpiresIn() >= 60;

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

        ResponseEntity<String> exchange(
                final String url,
                final String body) {

            return exchange(url, HttpMethod.POST, body, getHeaders());
        }

        HttpHeaders getHeadersJSONRequest() {
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            return httpHeaders;
        }

        HttpHeaders getHeaders() {
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class SPSData {

        public static final String ATTR_SPS_ACTIVE = "spsExamActive";
        public static final String ATTR_SPS_ACCESS_DATA = "spsAccessData";

        @JsonProperty("spsSEBAccessUUID")
        String spsSEBAccessUUID = null;
        @JsonProperty("spsSEBAccessName")
        String spsSEBAccessName = null;
        @JsonProperty("spsSEBAccessPWD")
        String spsSEBAccessPWD = null;
        @JsonProperty("psExamUUID")
        String spsExamUUID = null;

        private SPSData() {
        }

        @JsonCreator
        public SPSData(@JsonProperty("spsSEBAccessUUID") final String spsSEBAccessUUID,
                // NOTE: this is only for compatibility reasons, TODO as soon as possible
                @JsonProperty("spsSEBAccesUUID") final String spsSEBAccesUUID,
                @JsonProperty("spsSEBAccessName") final String spsSEBAccessName,
                @JsonProperty("spsSEBAccessPWD") final String spsSEBAccessPWD,
                @JsonProperty("psExamUUID") final String spsExamUUID) {

            this.spsSEBAccessUUID = StringUtils.isNotBlank(spsSEBAccesUUID) ? spsSEBAccesUUID : spsSEBAccessUUID;
            this.spsSEBAccessName = spsSEBAccessName;
            this.spsSEBAccessPWD = spsSEBAccessPWD;
            this.spsExamUUID = spsExamUUID;
        }
    }

}
