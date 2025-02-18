/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ScreenProctoringGroopRecordDynamicSqlSupport.sebGroupId;
import static ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.SPS_API.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.model.exam.*;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;

@Lazy
@Service
@WebServiceProfile
public class ScreenProctoringAPIBinding {

    private static final Logger log = LoggerFactory.getLogger(ScreenProctoringAPIBinding.class);

    private static final String SEB_SERVER_SCREEN_PROCTORING_SEB_ACCESS_PREFIX = "SEBServer_SEB_Access_";

    private final UserDAO userDAO;
    private final ClientGroupDAO clientGroupDAO;
    protected final Cryptor cryptor;
    protected final AsyncService asyncService;
    private final JSONMapper jsonMapper;
    private final ProctoringSettingsDAO proctoringSettingsDAO;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final ScreenProctoringGroupDAO screenProctoringGroupDAO;
    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    private final WebserviceInfo webserviceInfo;

    ScreenProctoringAPIBinding(
            final UserDAO userDAO,
            final ClientGroupDAO clientGroupDAO,
            final Cryptor cryptor,
            final AsyncService asyncService,
            final JSONMapper jsonMapper,
            final ProctoringSettingsDAO proctoringSettingsDAO,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final ScreenProctoringGroupDAO screenProctoringGroupDAO,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final WebserviceInfo webserviceInfo) {

        this.userDAO = userDAO;
        this.clientGroupDAO = clientGroupDAO;
        this.cryptor = cryptor;
        this.asyncService = asyncService;
        this.jsonMapper = jsonMapper;
        this.proctoringSettingsDAO = proctoringSettingsDAO;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.screenProctoringGroupDAO = screenProctoringGroupDAO;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        this.webserviceInfo = webserviceInfo;
    }

    Result<Void> testConnection(final SPSAPIAccessData spsAPIAccessData) {
        return Result.tryCatch(() -> {
            final ScreenProctoringServiceOAuthTemplate newRestTemplate =
                    new ScreenProctoringServiceOAuthTemplate(this, spsAPIAccessData);

            final ResponseEntity<String> result = newRestTemplate.testServiceConnection();

            if (result.getStatusCode() != HttpStatus.OK) {
                if (result.getStatusCode().is4xxClientError()) {
                    log.warn(
                            "Failed to establish REST connection to: {}. status: {}",
                            spsAPIAccessData.getSpsServiceURL(), result.getStatusCode());
                    
                    throw new RuntimeException("Failed to establish REST connection to: " + spsAPIAccessData.getSpsServiceURL() + ". status: " + result.getStatusCode());
                }
                throw new RuntimeException("Invalid SEB Screen Proctoring Service response: " + result);
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

    ScreenProctoringSettings getSettingsForExam(final Exam exam) {
        if (exam.additionalAttributes.containsKey(ScreenProctoringSettings.ATTR_ADDITIONAL_ATTRIBUTE_STORE_NAME)) {
            try {
                final String encrypted = exam.additionalAttributes.get(ScreenProctoringSettings.ATTR_ADDITIONAL_ATTRIBUTE_STORE_NAME);
                return jsonMapper.readValue(cryptor.decrypt(encrypted).getOrThrow().toString(), ScreenProctoringSettings.class);
            } catch (final Exception e) {
                log.warn("Failed to parse ScreenProctoringSettings from Exam additional attributes: {}", e.getMessage());
            }
        }
        
        // load it from DB
        return this.proctoringSettingsDAO
                .getScreenProctoringSettings(new EntityKey(exam.id, EntityType.EXAM))
                .getOrThrow();
    }

    /** This is called when the Screen Proctoring is been enabled for an Exam
     * If the needed resources on SPS side has been already created before, this just reactivates
     * all resources on SPS side.
     * If this is the fist initial run of the given exam and there are no resources on SPS side,
     * this creates all needed resources on SPS side like Exam, SEB Access and ad-hoc User-Account.
     *
     * @param exam The exam
     * @return Result refer to the exam or to an error when happened */
    Result<Exam> startScreenProctoring(final Exam exam) {
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
                
                if (!existsExamOnSPS(exam)) {
                    log.warn("Exam does not exist on SPS but has local data. Try to reinitialize Screen Proctoring for exam: {}", exam.name);
                    initializeScreenProctoring(exam, apiTemplate);
                    return exam;
                }
                
                // re-activate all needed entities on SPS side
                if (exam.status == Exam.ExamStatus.RUNNING) {
                    activateScreenProctoring(exam).getOrThrow();
                }

                synchronizeUserAccounts(exam);
                synchronizeGroups(exam, spsData);
                
                return exam;
            }

            // if we have a new Exam but Exam on SPS site for ExamUUID exists, reinitialize the exam and synchronize
            if (existsExamOnSPS(exam)) {
                reinitializeScreenProctoring(exam);
                return exam;
            }

            // This is a completely new exam with new SPS binding, initialize it
            initializeScreenProctoring(exam, apiTemplate);

            return exam;
        });
    }

    boolean existsExamOnSPS(final Exam exam) {
        try {

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);
            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(EXAM_ENDPOINT)
                    .pathSegment(createExamUUID(exam))
                    .build().toUriString();

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.GET);

            if (exchange.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("Exam not exists on SPS service, crate new one for exam: {}", exam)
                ;
                return false;
            } else if (exchange.getStatusCode() == HttpStatus.OK) {
                log.info("Exam already exists on SPS, reuse it: {}", exchange.getBody());
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
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(USER_ACCOUNT_ENDPOINT + userUUID)
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
            final ScreenProctoringServiceOAuthTemplate apiTemplate = getAPITemplate(null);
            exam.supporter.forEach(userUUID -> synchronizeUserAccount(userUUID, apiTemplate));
            if (exam.owner != null) {
                synchronizeUserAccount(exam.owner, apiTemplate);
            }

        } catch (final Exception e) {
            log.error("Failed to synchronize user accounts with SPS for exam: {}", exam, e);
        }
    }
    
    void deleteSPSUser(final String userUUID) {
        try {

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(null);

            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(USER_ACCOUNT_ENDPOINT + userUUID)
                    .build()
                    .toUriString();

            final ResponseEntity<String> exchange = apiTemplate.exchange(
                    uri, HttpMethod.DELETE, null, apiTemplate.getHeaders());

            if (exchange.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully deleted User Account on SPS for user: {}", userUUID);
            } else if (exchange.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("SPS User with uuid {} not found on SPS site.", userUUID);
            } else {
                log.error("Failed to delete user account on SPS for user: {} response: {}", userUUID, exchange);
            }

        } catch (final Exception e) {
            log.error("Failed to delete user account on SPS for user: {}", userUUID);
        }
    }

    public void synchronizeGroups(final Exam exam) {
        synchronizeGroups(exam, this.getSPSData(exam.id));
    }

    public void synchronizeGroups(final Exam exam, final SPSData spsData) {
        try {
            
            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);
            final ScreenProctoringSettings settings = this.proctoringSettingsDAO
                    .getScreenProctoringSettings(new EntityKey(exam.id, EntityType.EXAM))
                    .getOrThrow();
            
            // Get existing groups from SPS and SEB Server mapped to UUID
            final Map<String, ScreenProctoringGroup> localGroups = this.screenProctoringGroupDAO
                    .getCollectingGroups(exam.id)
                    .getOrThrow()
                    .stream()
                    .collect(Collectors.toMap(g -> g.uuid, Function.identity()));
            final Map<String, SPSGroup> spsGroups = getSPSGroups(exam, spsData.spsExamUUID, apiTemplate)
                    .stream()
                    .collect(Collectors.toMap(SPSGroup::uuid, Function.identity()));
            
            // init
            if (localGroups.isEmpty() && spsGroups.isEmpty()) {
                // all clear, full new initialization
                switch (settings.collectingStrategy) {
                    case EXAM -> initDefaultGroup(exam, spsData, settings, apiTemplate);
                    case APPLY_SEB_GROUPS -> initFromSEBGroups(exam, spsData, settings, apiTemplate);
                }
                return;
            }
            
            // synchronize
            if (!localGroups.isEmpty()) {
                switch (settings.collectingStrategy) {
                    case EXAM -> synchronizeExamSingleGroup(exam, spsData, settings, localGroups, spsGroups, apiTemplate);
                    case APPLY_SEB_GROUPS -> synchronizeFromSEBGroups(exam, spsData, localGroups, spsGroups, settings, apiTemplate);
                }
            } else {
                mergeSPSGroupsToLocalGroups(exam, spsData, settings, spsGroups, apiTemplate);
            }
            
        } catch (final Exception e) {
            log.error("Failed to synchronize groups for exam: {} error: {}", exam.name, e.getMessage());
        }
    }
    
    private void mergeSPSGroupsToLocalGroups(
            final Exam exam,
            final SPSData spsData,
            final ScreenProctoringSettings settings,
            final Map<String, SPSGroup> spsGroups,
            final ScreenProctoringServiceOAuthTemplate apiTemplate
    ) {

        final Map<String, SPSGroup> spsGroupsByName = spsGroups.values().stream()
                .collect(Collectors.toMap(SPSGroup::name, Function.identity()));
        
        // merge selected groups
        if (settings.collectingStrategy == CollectingStrategy.APPLY_SEB_GROUPS) {
            getSelectedSEBClientGroups(exam, settings).forEach( sebGroup -> {
                try {
                    final SPSGroup spsGroup = spsGroupsByName.get(sebGroup.name);
                    if (spsGroup != null) {
                        final String json = jsonMapper.writeValueAsString(spsGroup);
                        createNewLocalGroup(
                                exam,
                                new ScreenProctoringGroup(null, exam.id, spsGroup.uuid(), sebGroup.name, 0, json, false, sebGroup.id));
                    } else {
                        createNewLocalGroup(
                                exam,
                                createGroupOnSPS(0, exam.id, sebGroup.name, spsData.spsExamUUID, false, sebGroup.id, apiTemplate));
                    }
                } catch (final Exception e) {
                    log.error("Failed to merge SPS group to local group for selected group: {} cause: {}", sebGroup, e.getMessage());
                }
            });
        }
        
        // merge the default group
        try {
            final SPSGroup spsGroup = spsGroupsByName.get(settings.collectingGroupName);
            if (spsGroup != null) {
                final String json = jsonMapper.writeValueAsString(spsGroup);
                createNewLocalGroup(
                        exam,
                        new ScreenProctoringGroup(null, exam.id, spsGroup.uuid(), settings.collectingGroupName, 0, json, true, null));
            } else {
                createNewLocalGroup(
                        exam,
                        createGroupOnSPS(0, exam.id, settings.collectingGroupName, spsData.spsExamUUID, true, null, apiTemplate));
            }
        } catch (final Exception e) {
            log.error("Failed to merge SPS group to local group for default group: {} cause: {}", settings.collectingGroupName, e.getMessage());
        }
        
    }
    
    private void synchronizeExamSingleGroup(
            final Exam exam,
            final SPSData spsData,
            final ScreenProctoringSettings settings,
            final Map<String, ScreenProctoringGroup> localGroups,
            final Map<String, SPSGroup> spsGroups,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {
        
        // delete every existing none default group first since this strategy has only one default group
        localGroups.entrySet()
                .stream()
                .filter(entry -> !BooleanUtils.isTrue(entry.getValue().isFallback))
                .forEach( entry -> deleteGroup(apiTemplate, entry.getValue()));

        synchronizeDefaultGroup(exam, spsData, settings, localGroups, spsGroups, apiTemplate);
    }

    private void synchronizeFromSEBGroups(
            final Exam exam,
            final SPSData spsData,
            final Map<String, ScreenProctoringGroup> localGroups,
            final Map<String, SPSGroup> spsGroups,
            final ScreenProctoringSettings settings,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {
        
        // map local groups to SEB client group ids
        final Map<Long, ScreenProctoringGroup> sebGroupIdMap = localGroups.values()
                .stream()
                .filter(g -> g.sebGroupId != null)
                .collect(Collectors.toMap(g -> g.sebGroupId, Function.identity()));
        
        // SEB group selection is reference
        getSelectedSEBClientGroups(exam, settings).forEach( sebGroup -> {
                // local is second reference
                final ScreenProctoringGroup existing = sebGroupIdMap.remove(sebGroup.id);
                if (existing == null) {
                    // create new group locally as well as on SPS
                    try {
                        createNewLocalGroup(
                                exam,
                                createGroupOnSPS(
                                        0,
                                        exam.id,
                                        sebGroup.name,
                                        spsData.spsExamUUID,
                                        false,
                                        sebGroup.id,
                                        apiTemplate));
                    } catch (final Exception e) {
                        log.error("Failed to create SPS group while synchronizing for exam: {} group: {} cause: {}", exam, sebGroup, e.getMessage());
                    }
                } else {
                    // update existing group if name has changed
                    if (!Objects.equals(existing.name, sebGroup.name)) {
                        this.screenProctoringGroupDAO.updateName(existing.id, sebGroup.name);
                    }
                    // if name has changed synchronize on SPS
                    final SPSGroup spsGroup = spsGroups.get(existing.uuid);
                    if (spsGroup != null) {
                        if (!Objects.equals(spsGroup.name(), settings.collectingGroupName)) {
                            updateGroupOnSPS(spsData, sebGroup.name, apiTemplate, spsGroup);
                        }
                    } else {
                        log.warn(
                                "Screen Proctoring group mismatch detected. No SPS group found for exam: {} and local group: {}", 
                                exam.name, 
                                existing);
                        log.info("Try to create new one on SPS");
                        try {
                            final ScreenProctoringGroup groupOnSPS = createGroupOnSPS(
                                    0,
                                    exam.id,
                                    sebGroup.name,
                                    spsData.spsExamUUID,
                                    existing.isFallback,
                                    sebGroup.id,
                                    apiTemplate);

                            this.screenProctoringGroupDAO
                                    .updateFromSPS(existing.id, groupOnSPS)
                                    .getOrThrow();
                            
                        } catch (final Exception e) {
                            log.error("Failed to synchronize SEB Group on SPS: {}", sebGroup, e);
                        }
                    }
                }
            }
        );
        
        // check if we had some deletions
        if (!sebGroupIdMap.isEmpty()) {
            sebGroupIdMap.values().forEach(g -> deleteGroup(apiTemplate, g));
        }
        
        // check also fallback group
        synchronizeDefaultGroup(exam, spsData, settings, localGroups, spsGroups, apiTemplate);
    }

    private void deleteGroup(
            final ScreenProctoringServiceOAuthTemplate apiTemplate,
            final ScreenProctoringGroup screenProctoringGroup) {
        
        log.info("Detected Screen Proctoring Group for deletion and try to delete it on SPS: {}", screenProctoringGroup);

        // group delete request on SPS
        final String uri = UriComponentsBuilder
                .fromUriString(apiTemplate.spsServiceURL)
                .path(GROUP_ENDPOINT)
                .pathSegment(screenProctoringGroup.uuid)
                .pathSegment(GROUP_DELETE_REQUEST_ENDPOINT)
                .build()
                .toUriString();

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.DELETE);
        if (exchange.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to request delete on SPS for Group: {} with response: {}", screenProctoringGroup, exchange);
        } else {
            // delete also locally
            this.screenProctoringGroupDAO
                    .deleteGroup(screenProctoringGroup.id)
                    .onError( error -> log.error(
                            "Failed to delete local ScreenProctoringGroup: {} cause: {}",
                            screenProctoringGroup, error.getMessage()));
        }
    }

    private void synchronizeDefaultGroup(
            final Exam exam,
            final SPSData spsData,
            final ScreenProctoringSettings settings,
            final Map<String, ScreenProctoringGroup> localGroups,
            final Map<String, SPSGroup> spsGroups,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {

        final ScreenProctoringGroup localGroup = localGroups.values()
                .stream()
                .filter(g -> BooleanUtils.isTrue(g.isFallback))
                .findFirst()
                .orElse(null);
        final SPSGroup spsGroup = spsGroups.get(localGroup.uuid);

        if (spsGroup == null) {
            // try re-create group on SPS
            log.warn(
                    "Screen Proctoring group mismatch detected. No SPS group found for exam: {} and local group: {}",
                    exam.name,
                    localGroup);
            log.info("Try to create new one on SPS");
            try {
                final ScreenProctoringGroup groupOnSPS = createGroupOnSPS(
                        0,
                        exam.id,
                        localGroup.name,
                        spsData.spsExamUUID,
                        localGroup.isFallback,
                        localGroup.id,
                        apiTemplate);

                this.screenProctoringGroupDAO
                        .updateFromSPS(localGroup.id, groupOnSPS)
                        .getOrThrow();

            } catch (final Exception e) {
                log.error("Failed to synchronize SEB Group on SPS: {}", localGroup, e);
            }
            return;
        }
        
        // if name has changed synchronize locally
        if (!Objects.equals(localGroup.name, settings.collectingGroupName)) {
            this.screenProctoringGroupDAO.updateName(localGroup.id, settings.collectingGroupName);
        }
        // if name has changed synchronize on SPS
        if (!Objects.equals(spsGroup.name(), settings.collectingGroupName)) {
            updateGroupOnSPS(spsData, settings.collectingGroupName, apiTemplate, spsGroup);
        }
    }

    private void updateGroupOnSPS(
            final SPSData spsData,
            final String name,
            final ScreenProctoringServiceOAuthTemplate apiTemplate,
            final SPSGroup spsGroup) {
        
        final String groupRequestURI = UriComponentsBuilder
                .fromUriString(apiTemplate.spsServiceURL)
                .path(GROUP_ENDPOINT)
                .pathSegment(spsGroup.uuid())
                .build().toUriString();
        final Map<String, String> values = Map.of(
                "name", name,
                "description", spsGroup.description()
        );
        try {
            final ResponseEntity<String> updateGroup = apiTemplate.exchangePUT(
                    groupRequestURI, jsonMapper.writeValueAsString(values));
            if (updateGroup.getStatusCode() != HttpStatus.OK) {
                log.warn("Failed to update SPS group: {}", spsGroup);
            }
        } catch (final JsonProcessingException e) {
            log.warn("Failed to update SPS group: {}, cause: {}", spsGroup, e.getMessage());
        }
    }

    private void initDefaultGroup(
            final Exam exam,
            final SPSData spsData,
            final ScreenProctoringSettings settings,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {
        
        final String name = StringUtils.isNotBlank(settings.collectingGroupName)
                ? settings.collectingGroupName
                : exam.getName();
        
        createNewLocalGroup(
                exam, 
                createGroupOnSPS(
                        0, exam.id, name, spsData.spsExamUUID, 
                        true, null, apiTemplate));
    }
    
    private void initFromSEBGroups(
            final Exam exam,
            final SPSData spsData,
            final ScreenProctoringSettings settings,
            final ScreenProctoringServiceOAuthTemplate apiTemplate)  {

        getSelectedSEBClientGroups(exam, settings).forEach( sebGroup ->
            createNewLocalGroup(
                exam,
                createGroupOnSPS(
                        0, exam.id, sebGroup.name, spsData.spsExamUUID,
                        false, sebGroup.id, apiTemplate))
        );

        // create default group to fit in all SEB clients that do not match the above SEB groups
        initDefaultGroup(exam, spsData, settings, apiTemplate);
    }

    private List<ClientGroup> getSelectedSEBClientGroups(
            final Exam exam,
            final ScreenProctoringSettings settings) {
        
        final List<String> selectedSEBGroups = Arrays.asList(StringUtils.split(
                settings.sebGroupsSelection, 
                Constants.LIST_SEPARATOR_CHAR));

        // create new groups for each selected SEB group
        return this.clientGroupDAO
                .allForExam(exam.id)
                .getOrThrow()
                .stream()
                .filter(g -> selectedSEBGroups.contains(g.getModelId()))
                .toList();
    }

    private Collection<SPSGroup> getSPSGroups(
            final Exam exam,
            final String spsExamUUID,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) throws JsonProcessingException {
        
        final String groupRequestURI = UriComponentsBuilder
                .fromUriString(apiTemplate.spsServiceURL)
                .path(GROUP_BY_EXAM_ENDPOINT)
                .pathSegment(spsExamUUID)
                .build()
                .toUriString();
        final ResponseEntity<String> exchangeGroups = apiTemplate.exchange(groupRequestURI, HttpMethod.GET);
        if (exchangeGroups.getStatusCode() == HttpStatus.NOT_FOUND)  {
            log.info("No SPS Groups found for exam: {} on SPS", exam);
            return Collections.emptyList();
        } else if (exchangeGroups.getStatusCode() != HttpStatus.OK)  {
            throw new RuntimeException("Failed to get groups for exam from SPS. Status: " + exchangeGroups.getStatusCode());
        }
        return jsonMapper.readValue(
                exchangeGroups.getBody(), 
                new TypeReference<Collection<SPSGroup>>() {});
    }

    /** This is called when an exam has changed its parameter and needs data update on SPS side
     *
     * @param exam The exam*/
    Result<Exam> updateExam(final Exam exam) {
        
        return Result.tryCatch(() -> {
            final SPSData spsData = this.getSPSData(exam.id);
            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);
            final ScreenProctoringSettings settings = this.proctoringSettingsDAO
                    .getScreenProctoringSettings(new EntityKey(exam.id, EntityType.EXAM))
                    .getOrThrow();

            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(EXAM_ENDPOINT)
                    .pathSegment(spsData.spsExamUUID)
                    .build()
                    .toUriString();

            final List<String> supporterIds = getSupporterIds(exam);
            final ExamUpdate examUpdate = new ExamUpdate(
                    exam.name,
                    exam.getDescription(),
                    exam.getStartURL(),
                    exam.getType().name(),
                    exam.startTime != null ? exam.startTime.getMillis() : null,
                    exam.endTime != null ? exam.endTime.getMillis() : null,
                    null,
                    supporterIds
            );

            final String jsonExamUpdate = this.jsonMapper.writeValueAsString(examUpdate);

            final ResponseEntity<String> exchange = apiTemplate.exchange(
                    uri,
                    HttpMethod.PUT,
                    jsonExamUpdate,
                    apiTemplate.getHeadersJSONRequest());
            
            if (exchange.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to update SPS exam data: " + exchange);
            }
            
            return exam;
        });
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
            activation(SEB_ACCESS_ENDPOINT, spsData.spsSEBAccessUUID, false, apiTemplate);
            activation(EXAM_ENDPOINT, spsData.spsExamUUID, false, apiTemplate);

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
            activation(SEB_ACCESS_ENDPOINT, spsData.spsSEBAccessUUID, true, apiTemplate);
            activation(EXAM_ENDPOINT, spsData.spsExamUUID, true, apiTemplate);

            // mark local for successfully activated on SPS side
            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    exam.id,
                    SPSData.ATTR_SPS_ACTIVE,
                    Constants.TRUE_STRING);

            return exam;
        });
    }

    private void initializeScreenProctoring(
            final Exam exam,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) throws JsonProcessingException {
        
        final SPSData spsData = new SPSData();
        log.info(
                "SPS Exam for SEB Server Exam: {} don't exists yet, create necessary structures on SPS",
                exam.externalId);

        synchronizeUserAccounts(exam);
        createSEBAccess(exam, apiTemplate, spsData);
        createExam(exam, apiTemplate, spsData);
        synchronizeGroups(exam, spsData);

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
        
    }

    void reinitializeScreenProctoring(final Exam exam) {
        try {

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);

            // get exam from SPS
            final String examUUID = createExamUUID(exam);
            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(EXAM_ENDPOINT)
                    .pathSegment(examUUID)
                    .build().toUriString();
            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.GET);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to get Exam from SPS. local exam uuid: " + examUUID);
            }
            final JsonNode requestJSON = this.jsonMapper.readTree(exchange.getBody());
            final String spsExamId = requestJSON.get(EXAM.ATTR_ID).asText();
            
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
            // synchronize groups
            this.synchronizeGroups(exam, spsData);
            
        } catch (final Exception e) {
            log.error("Failed to re-initialize Screen Proctoring: ", e);
        }
    }

    Tuple<String> createSEBSession(
            final Long examId,
            final ScreenProctoringGroup localGroup,
            final ClientConnectionRecord clientConnection) {


        final String token = clientConnection.getConnectionToken();
        final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(examId);
        final String uri = UriComponentsBuilder
                .fromUriString(apiTemplate.spsServiceURL)
                .path(SESSION_ENDPOINT)
                .build()
                .toUriString();

        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(SESSION.ATTR_UUID, token);
        params.add(SESSION.ATTR_GROUP_ID, localGroup.uuid);
        params.add(SESSION.ATTR_CLIENT_IP, clientConnection.getClientAddress());
        params.add(SESSION.ATTR_CLIENT_NAME, clientConnection.getExamUserSessionId());
        params.add(SESSION.ATTR_CLIENT_MACHINE_NAME, clientConnection.getClientMachineName());
        params.add(SESSION.ATTR_CLIENT_OS_NAME, clientConnection.getClientOsName());
        params.add(SESSION.ATTR_CLIENT_VERSION, clientConnection.getClientVersion());
        final String paramsFormEncoded = Utils.toAppFormUrlEncodedBodyForSPService(params);

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, paramsFormEncoded);
        if (exchange.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException(
                    "Failed to create SPS SEB session for SEB connection: " + token);
        }
        
        // get generated encryption key from SPS 
        final String getKeyRequestURI = UriComponentsBuilder
                .fromUriString(apiTemplate.spsServiceURL)
                .path(SESSION_ENCRYPTION_KEY_ENDPOINT)
                .pathSegment(token)
                .build()
                .toUriString();

        final ResponseEntity<String> exchange1 = apiTemplate.exchange(getKeyRequestURI, HttpMethod.GET);
        if (exchange1.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException(
                    "Failed to get SPS SEB session encryption key for SEB connection: " + token);
        }

        final String key = exchange1.getHeaders().getFirst(SESSION_ENCRYPTION_KEY_REQUEST_HEADER);
        if (StringUtils.isBlank(key)) {
            log.error("Failed to get SEB session encryption key from SPS");
        }

        return new Tuple<>(token, key);
    }

    String updateSEBSession(
            final Long groupId,
            final ClientConnectionRecord clientConnection) {

        final String token = clientConnection.getConnectionToken();
        final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(clientConnection.getExamId());

        final String uri = UriComponentsBuilder
                .fromUriString(apiTemplate.spsServiceURL)
                .path(SESSION_ENDPOINT)
                .pathSegment(token)
                .build()
                .toUriString();

        final Map<String, String> params = new HashMap<>();
        params.put(SESSION.ATTR_UUID, token);
        params.put(SESSION.ATTR_GROUP_ID, String.valueOf(groupId));
        params.put(SESSION.ATTR_CLIENT_IP, clientConnection.getClientAddress());
        params.put(SESSION.ATTR_CLIENT_NAME, clientConnection.getExamUserSessionId());
        params.put(SESSION.ATTR_CLIENT_MACHINE_NAME, clientConnection.getClientMachineName());
        params.put(SESSION.ATTR_CLIENT_OS_NAME, clientConnection.getClientOsName());
        params.put(SESSION.ATTR_CLIENT_VERSION, clientConnection.getClientVersion());

        ResponseEntity<String> exchange = null;
        try {
            final String jsonSession = jsonMapper.writeValueAsString(params);
            exchange = apiTemplate.exchangePUT(uri, jsonSession);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (exchange.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException(
                    "Failed to update SPS SEB session for SEB connection: " + token);
        }

        return token;
    }
    
    void closeSEBSession(final ClientConnectionRecord clientConnection) {
        if (clientConnection.getScreenProctoringGroupId() == null) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Skip closing SPS session because SEB Server connection seems not to have a SPS session before closing: {}", 
                        clientConnection);
            }
            return;
        }
        final String token = clientConnection.getConnectionToken();
        final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(clientConnection.getExamId());
        activation(SESSION_ENDPOINT, token, false, apiTemplate);
    }


    void deleteExamOnScreenProctoring(final Exam exam) {
        try {
            
            if (log.isDebugEnabled()) {
                log.info("Delete or deactivate exam and groups on SPS site and send deletion request for exam {}", exam);
            }

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(exam.id);
            final SPSData spsData = this.getSPSData(exam.id);
            
            if (spsData == null) {
                log.info("There os no SPS data for this exam");
                return;
            }
            
            deletion(SEB_ACCESS_ENDPOINT, spsData.spsSEBAccessUUID, apiTemplate);
            activation(EXAM_ENDPOINT, spsData.spsExamUUID, false, apiTemplate);

            // exam delete request on SPS
            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(EXAM_ENDPOINT)
                    .pathSegment(spsData.spsExamUUID)
                    .pathSegment(EXAM_DELETE_REQUEST_ENDPOINT)
                    .build()
                    .toUriString();

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.DELETE);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                log.error("Failed to request delete on SPS for Exam: {} with response: {}", exam, exchange);
            }

            // mark successfully dispose on SPS side
            this.additionalAttributesDAO.saveAdditionalAttribute(
                    EntityType.EXAM,
                    exam.id,
                    SPSData.ATTR_SPS_ACTIVE,
                    Constants.FALSE_STRING);


        } catch (final Exception e) {
            log.warn("Failed to apply SPS deletion of exam: {} error: {}", exam, e.getMessage());
        }
    }

    public Collection<GroupSessionCount> getActiveGroupSessionCounts() {
        try {

            final ScreenProctoringServiceOAuthTemplate apiTemplate = this.getAPITemplate(null);

            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(GROUP_COUNT_ENDPOINT)
                    .build()
                    .toUriString();


            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, HttpMethod.POST);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                log.warn("Failed to request active group session counts: {}", exchange);
                return Collections.emptyList();
            }

            return this.jsonMapper.readValue(
                    exchange.getBody(),
                    new TypeReference<>() {
                    });

        } catch (final Exception e) {
            if (!e.getMessage().contains("Open CircuitBreaker")) {
                log.warn("Failed to get active group session counts: {}", e.getMessage());
            }
            return Collections.emptyList();
        }
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
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(USERSYNC_SEBSERVER_ENDPOINT)
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
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(USER_ACCOUNT_ENDPOINT)
                    .pathSegment(userUUID)
                    .path(BooleanUtils.isTrue(userInfo.active) ? "/active" : "/inactive")
                    .build()
                    .toUriString();
            final ResponseEntity<String> activityRequest = apiTemplate.exchange(
                    activityURI, HttpMethod.POST, jsonBody, apiTemplate.getHeaders());

            if (activityRequest.getStatusCode() != HttpStatus.OK) {
                final String body = activityRequest.getBody();
                if (body != null && !body.contains("Activation argument mismatch")) {
                    log.warn("Failed to synchronize activity for user account on SPS: {}", activityRequest);
                }
            } else {
                log.info("Successfully synchronize activity for user account on SPS for user: {}", userUUID);
            }

        } catch (final Exception e) {
            log.error("Failed to synchronize user account with SPS for user: {}", userUUID, e);
        }
    }

    private static UserMod getUserModifications(final UserInfo userInfo, final SEBServerUser accountInfo) {
        final Set<String> spsUserRoles = new HashSet<>();
        spsUserRoles.add(SPSUserRole.PROCTOR.name());
        if (userInfo.roles.contains(UserRole.SEB_SERVER_ADMIN.name())) {
            spsUserRoles.add(SPSUserRole.ADMIN.name());
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

    private void createNewLocalGroup(final Exam exam, final ScreenProctoringGroup newGroup) {

        if (log.isDebugEnabled()) {
            log.debug(
                    "Create new local screen proctoring group for exam: {}, group: {}",
                    exam.externalId, newGroup);
        }

        this.screenProctoringGroupDAO
                .createNewGroup(newGroup)
                .onError(error -> log.error("Failed to create local screen proctoring group: {}",
                        newGroup, error));
    }

    private ScreenProctoringGroup createGroupOnSPS(
            final int size,
            final Long examId,
            final String name,
            final String spsExamUUID,
            final boolean isFallback,
            final Long sebGroupId,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {

        final String uri = UriComponentsBuilder
                .fromUriString(apiTemplate.spsServiceURL)
                .path(GROUP_ENDPOINT)
                .build()
                .toUriString();
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(GROUP.ATTR_NAME, name);
        params.add(GROUP.ATTR_DESCRIPTION, "Created by SEB Server");
        params.add(GROUP.ATTR_EXAM_ID, spsExamUUID);
        final String paramsFormEncoded = Utils.toAppFormUrlEncodedBodyForSPService(params);

        final ResponseEntity<String> exchange = apiTemplate.exchange(uri, paramsFormEncoded);
        if (exchange.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to create SPS SEB group for exam: " + spsExamUUID);
        }

        final Map<String, String> groupAttributes;
        try {
            groupAttributes = this.jsonMapper.readValue(
                    exchange.getBody(),
                    new TypeReference<Map<String, String>>() {
                    });
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        final String spsGroupUUID = groupAttributes.get(GROUP.ATTR_UUID);
        return new ScreenProctoringGroup(null, examId, spsGroupUUID, name, size, exchange.getBody(), isFallback, sebGroupId);
    }

    private void createExam(
            final Exam exam,
            final ScreenProctoringServiceOAuthTemplate apiTemplate,
            final SPSData spsData) {

        try {

            final List<String> supporterIds = getSupporterIds(exam);
            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(EXAM_ENDPOINT)
                    .build().toUriString();
            final ScreenProctoringSettings settings = this.proctoringSettingsDAO
                    .getScreenProctoringSettings(new EntityKey(exam.id, EntityType.EXAM))
                    .getOrThrow();

            final String uuid = createExamUUID(exam);
            final MultiValueMap<String, String> params = createExamCreationParams(
                    exam, 
                    uuid,
            /*settings.deletionTime */ null,
            supporterIds);
            final String paramsFormEncoded = Utils.toAppFormUrlEncodedBodyForSPService(params);

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, paramsFormEncoded);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Error response from Screen Proctoring Service: "
                        + exchange.getStatusCodeValue()
                        + " "
                        + exchange.getBody());
            }

            final JsonNode requestJSON = this.jsonMapper.readTree(exchange.getBody());
            final String respondedUUID = requestJSON.get(EXAM.ATTR_UUID).textValue();
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
            final DateTime deletionTime,
            final List<String> supporterIds) {

        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(EXAM.ATTR_UUID, uuid);
        params.add(EXAM.ATTR_NAME, exam.name);
        if (exam.getDescription() != null) {
            params.add(EXAM.ATTR_DESCRIPTION, exam.getDescription());
        }
        params.add(EXAM.ATTR_URL, exam.getStartURL());
        if (!supporterIds.isEmpty()) {
            params.add(EXAM.ATTR_SUPPORTER, StringUtils.join(supporterIds, Constants.LIST_SEPARATOR));
        }
        params.add(EXAM.ATTR_TYPE, exam.getType().name());
        params.add(EXAM.ATTR_START_TIME, java.lang.String.valueOf(exam.startTime.getMillis()));

        if (exam.endTime != null) {
            params.add(EXAM.ATTR_END_TIME, java.lang.String.valueOf(exam.endTime.getMillis()));
        }
        
        if (deletionTime != null) {
            params.add(EXAM.ATTR_DELETION_TIME, java.lang.String.valueOf(deletionTime.getMillis()));
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
            String name = SEB_SERVER_SCREEN_PROCTORING_SEB_ACCESS_PREFIX + exam.externalId;
            final String description = "This SEB access was auto-generated by SEB Server";

            // first try to get existing one by name and link it if available
            String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(SEB_ACCESS_ENDPOINT)
                    .queryParam(SEB_ACCESS.ATTR_NAME, name)
                    .build()
                    .toUriString();

            final ResponseEntity<String> getResponse = apiTemplate.exchange(uri, HttpMethod.GET);
            if (getResponse.getStatusCode() == HttpStatus.OK) {
                try {
                    final JsonNode requestJSON = this.jsonMapper.readTree(getResponse.getBody());
                    final JsonNode content = requestJSON.get("content");
                    if (content.isArray()) {

                        if (content.size() == 1) {
                            final JsonNode sebConnection = content.get(0);

                            // TODO remove when tested
                            final JsonNode uuidNode = sebConnection.get(SEB_ACCESS.ATTR_UUID);
                            final JsonNode sebClientNode = sebConnection.get(SEB_ACCESS.ATTR_CLIENT_NAME);
                            final JsonNode sebSecretNode = sebConnection.get(SEB_ACCESS.ATTR_CLIENT_SECRET);
                            log.info(" uuidNode: {}", uuidNode);
                            log.info(" sebClientNode: {}", sebClientNode);
                            log.info(" sebSecretNode: {}", sebSecretNode);

                            spsData.spsSEBAccessUUID = uuidNode.textValue();
                            spsData.spsSEBAccessName = sebClientNode.textValue();
                            spsData.spsSEBAccessPWD = sebSecretNode.textValue();
                            return;
                        } else if (content.size() > 1) {
                            log.warn("Got more then 1 SEB Client Access object for query, create new one with name suffix...");
                            name = name + "_(" + content.size() + ")";
                        }
                    }
                } catch (final Exception e) {
                    log.warn("Failed to extract existing SEB Account from JSON: {}", e.getMessage());
                }
            }

            // otherwise create new one and link it
            uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(SEB_ACCESS_ENDPOINT)
                    .build()
                    .toUriString();

            final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add(SEB_ACCESS.ATTR_NAME, name);
            params.add(SEB_ACCESS.ATTR_DESCRIPTION, description);
            final String paramsFormEncoded = Utils.toAppFormUrlEncodedBodyForSPService(params);

            final ResponseEntity<String> exchange = apiTemplate.exchange(uri, paramsFormEncoded);
            if (exchange.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to create SPS SEB access for exam: " + exam.externalId);
            }

            // store SEB access data for proctoring along with the exam
            final JsonNode requestJSON = this.jsonMapper.readTree(exchange.getBody());
            spsData.spsSEBAccessUUID = requestJSON.get(SEB_ACCESS.ATTR_UUID).textValue();
            spsData.spsSEBAccessName = requestJSON.get(SEB_ACCESS.ATTR_CLIENT_NAME).textValue();
            spsData.spsSEBAccessPWD = requestJSON.get(SEB_ACCESS.ATTR_CLIENT_SECRET).textValue();

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
            final String domainPath,
            final String uuid,
            final boolean activate,
            final ScreenProctoringServiceOAuthTemplate apiTemplate) {

        try {

            final String uri = UriComponentsBuilder
                    .fromUriString(apiTemplate.spsServiceURL)
                    .path(domainPath)
                    .pathSegment(uuid)
                    .pathSegment(activate ? ACTIVE_PATH_SEGMENT : INACTIVE_PATH_SEGMENT)
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
                    .fromUriString(apiTemplate.spsServiceURL)
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

            deletion(EXAM_ENDPOINT, spsData.spsExamUUID, apiTemplate);

        }

        if (StringUtils.isNotBlank(spsData.spsSEBAccessUUID)) {
            log.info(
                    "Try to rollback SPS SEB Access with UUID: {} for exam: {}",
                    spsData.spsSEBAccessUUID,
                    exam.externalId);

            deletion(SEB_ACCESS_ENDPOINT, spsData.spsSEBAccessUUID, apiTemplate);
        }
    }
    
    private ScreenProctoringServiceOAuthTemplate apiTemplate = null;
    private ScreenProctoringServiceOAuthTemplate getAPITemplate(final Long examId) {
        if (apiTemplate == null || !apiTemplate.isValid()) {

                if (log.isDebugEnabled()) {
                    log.debug("Create new ScreenProctoringServiceOAuthTemplate for bundle");
                }

                final WebserviceInfo.ScreenProctoringServiceBundle bundle = this.webserviceInfo
                        .getScreenProctoringServiceBundle();
                
                if (!bundle.bundled) {
                    throw new IllegalStateException("Only bundled SEB Server Screen Proctoring is supported yet");
                }

                this.testConnection(bundle).getOrThrow();
                this.apiTemplate = new ScreenProctoringServiceOAuthTemplate(this, bundle);
        }
        
        return apiTemplate;
    }

    private static List<String> getSupporterIds(final Exam exam) {
        final Set<String> supporterIds = new HashSet<>(exam.supporter);
        if (exam.owner != null && !UserService.LMS_INTEGRATION_CLIENT_UUID.equals(exam.owner)) {
            supporterIds.add(exam.owner);
        }
        return new ArrayList<>(supporterIds);
    }

    OAuth2RestTemplate getOAuth2RestTemplate(final ResourceOwnerPasswordResourceDetails resource) {

        final Result<ClientHttpRequestFactory> clientHttpRequestFactoryRequest = this.clientHttpRequestFactoryService
                .getClientHttpRequestFactory();
        ClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        if (!clientHttpRequestFactoryRequest.hasError()) {
            requestFactory = clientHttpRequestFactoryRequest.get();
        }

        final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resource);
        oAuth2RestTemplate.setRequestFactory(requestFactory);
        return oAuth2RestTemplate;
    }
}
